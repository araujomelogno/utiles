package uy.com.bay.utiles.controllers;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.JobType;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.data.repository.TaskRepository;

@RestController
@RequestMapping("/api/webhook")
public class AlchemerController {

	private static final Logger log = LoggerFactory.getLogger(AlchemerController.class);

	@Autowired
	private AlchemerSurveyResponseRepository alchemerSurveyResponseRepository;

	@Autowired
	private FieldworkRepository fieldworkRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	// 1) application/x-www-form-urlencoded (Alchemer suele mandar esto)
	@PostMapping(path = "/survey-response", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveForm(@RequestParam org.springframework.util.MultiValueMap<String, String> form,
			HttpServletRequest req) {
		log.error("NO debería  invocar este servicio: webhook FORM ct={} fields={}", req.getContentType(), form);
		// TODO: tu lógica (form.getFirst("survey_id"), etc.)
		return ResponseEntity.ok("ok");
	}

	// 2) JSON
	@PostMapping(path = "/survey-response", consumes = { MediaType.APPLICATION_JSON_VALUE,
			"application/*+json" }, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveJson(@RequestBody AlchemerSurveyResponse response, HttpServletRequest req) {
		log.info("Received Alchemer survey response with response_id: {} and survey_id: {}",
				response.getData().getResponseId(), response.getData().getSurveyId());
		return this.process(response);
	}

	// 3) application/octet-stream (a veces mandan JSON pero con este CT)
	@PostMapping(path = "/survey-response", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveBytes(@RequestBody byte[] raw, HttpServletRequest req) {
		// Intento de mostrar como texto (si no es texto, verá binario)
		String preview = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
		log.info("este es el string:" + preview);
		ObjectMapper mapper = new ObjectMapper();

		AlchemerSurveyResponse response;

		try {
			response = mapper.readValue(preview, AlchemerSurveyResponse.class);
			return this.process(response);
		} catch (JsonMappingException e) {
			e.printStackTrace();
			log.error("errror al parsear respusta de alchemer" + e.getMessage());
			return ResponseEntity.internalServerError().build();
		} catch (JsonProcessingException e) {
			log.error("errror al parsear respusta de alchemer" + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping(path = "/survey-response", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveText(@RequestBody String raw, HttpServletRequest req) {
		log.error("NO debería  invocar este servicio TEXT ct={} len={} body={}", req.getContentType(),
				raw == null ? 0 : raw.length(), raw);
		// TODO: si realmente es JSON en texto, podés parsearlo:
		// var node = new ObjectMapper().readTree(raw);
		return ResponseEntity.ok("ok");
	}

	// 4) Fallback (por si llega cualquier otro Content-Type)
	@PostMapping(path = "/survey-response", consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> receiveFallback(@RequestBody(required = false) byte[] raw, HttpServletRequest req) {
		int len = raw == null ? 0 : raw.length;
		String preview = raw == null ? "null" : new String(raw, java.nio.charset.StandardCharsets.UTF_8);
		log.error("NO debería  invocar este servicio FALLBACK ct={} bytes={} preview={}", req.getContentType(), len,
				preview);
		return ResponseEntity.ok("ok");
	}

	private ResponseEntity<String> process(AlchemerSurveyResponse response) {
		log.info("Received Alchemer survey response with response_id: {} and survey_id: {}",
				response.getData().getResponseId(), response.getData().getSurveyId());

		List<AlchemerSurveyResponse> existingResponses = alchemerSurveyResponseRepository
				.findByDataResponseIdAndDataSurveyId((long) response.getData().getResponseId(),
						response.getData().getSurveyId());

		if (!existingResponses.isEmpty()) {
			log.warn("Duplicate Alchemer survey response received. response_id: {}, survey_id: {}. Ignoring.",
					response.getData().getResponseId(), response.getData().getSurveyId());
			return ResponseEntity.ok().build();
		}

		log.info("Processing new Alchemer survey response.");
		Optional<Fieldwork> optionalFieldwork = fieldworkRepository
				.findByAlchemerId(String.valueOf(response.getData().getSurveyId()));
		optionalFieldwork.ifPresent(response::setFieldwork);

		response.getData().setSurveyResponse(response);

		alchemerSurveyResponseRepository.save(response);
		log.info("Saved new AlchemerSurveyResponse with id: {}", response.getId());

		Task task = new Task();
		task.setJobType(JobType.ALCHEMERANSWERRETRIEVAL);
		task.setStatus(Status.PENDING);
		task.setCreated(new Date());
		task.setSurveyId(response.getData().getSurveyId());
		task.setResponseId(response.getData().getResponseId());
		taskRepository.save(task);
		log.info("Created new Task with id: {}", task.getId());

		String urlStudy = String.format("https://api.alchemer.com/v5/survey/%d?api_token=%s&api_token_secret=%s",
				task.getSurveyId(), apiToken, apiTokenSecret);

		RestTemplate restTemplate = new RestTemplate();
		String responsesTudy = restTemplate.getForObject(urlStudy, String.class);

		ObjectMapper mapperStudy = new ObjectMapper();
		JsonNode rootStudy;
		try {
			rootStudy = mapperStudy.readTree(responsesTudy);

			String surveyTitle = rootStudy.path("data").path("internal_title").asText();

			response.setStudyName(surveyTitle);

			alchemerSurveyResponseRepository.save(response);

		} catch (JsonMappingException e) {
			log.error(e.getLocalizedMessage());
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			log.error(e.getLocalizedMessage());
			e.printStackTrace();
		}

		return ResponseEntity.ok().build();
	}
}
