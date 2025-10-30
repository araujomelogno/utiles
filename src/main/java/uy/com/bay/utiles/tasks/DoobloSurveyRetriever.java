package uy.com.bay.utiles.tasks;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import uy.com.bay.utiles.data.DoobloResponse;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.StudyRepository;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;
import uy.com.bay.utiles.data.repository.DoobloResponseRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetEntryService;

@Component
public class DoobloSurveyRetriever {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoobloSurveyRetriever.class);

	private final DoobloResponseRepository doobloResponseRepository;
	private final StudyRepository studyRepository;
	private final SurveyorRepository surveyorRepository;
	private final FieldworkRepository fieldworkRepository;
	private final RestTemplate restTemplate;

	@Value("${surveyToGo.username}")
	private String username;

	@Value("${surveyToGo.password}")
	private String password;

	@Value("${surveyToGo.activeSurveyDaysBack}")
	private int activeSurveyDaysBack;

	@Autowired
	private BudgetEntryService budgetEntryService;

	public DoobloSurveyRetriever(DoobloResponseRepository doobloResponseRepository, StudyRepository studyRepository,
			SurveyorRepository surveyorRepository, FieldworkRepository fieldworkRepository,
			BudgetEntryService budgetEntryService) {
		this.doobloResponseRepository = doobloResponseRepository;
		this.studyRepository = studyRepository;
		this.surveyorRepository = surveyorRepository;
		this.fieldworkRepository = fieldworkRepository;
		this.budgetEntryService = budgetEntryService;
		this.restTemplate = new RestTemplate();
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void retrieveDoobloSurveys() {
		LOGGER.info("Starting Dooblo Survey Retriever task...");
		try {
			String url = "http://api.dooblo.net/newapi/Surveys/GetActiveSurveys?daysBack=" + activeSurveyDaysBack;
			HttpEntity<String> entity = createAuthHeaders();
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			LOGGER.info("Successfully retrieved active surveys. Response: {}", response.getBody());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.getBody());

			if (root.isArray()) {
				LOGGER.info("Found {} active surveys.", root.size());
				for (JsonNode surveyNode : root) {
					String surveyId = surveyNode.path("SurveyID").asText();
					if (surveyId.isEmpty()) {
						LOGGER.warn("Found a survey object with no SurveyID. Skipping.");
						continue;
					}

					try {
						String interviewsUrl = String.format(
								"http://api.dooblo.net/newapi/SurveyInterviewIDs?surveyIDs=%s&completed=True",
								surveyId);
						ResponseEntity<String> interviewsResponse = restTemplate.exchange(interviewsUrl, HttpMethod.GET,
								entity, String.class);
						LOGGER.info("Successfully retrieved interview IDs for SurveyID {}. Response: {}", surveyId,
								interviewsResponse.getBody());

						JsonNode interviewsRoot = mapper.readTree(interviewsResponse.getBody());
						if (interviewsRoot.isArray()) {
							LOGGER.info("Found {} interviews for SurveyID {}.", interviewsRoot.size(), surveyId);
							for (JsonNode interviewIdNode : interviewsRoot) {
								String interviewId = interviewIdNode.asText();
								if (interviewId.isEmpty()) {
									LOGGER.warn("Found an empty interview ID for SurveyID {}. Skipping.", surveyId);
									continue;
								}

								if (doobloResponseRepository.existsByInterviewId(interviewId)) {
									LOGGER.info("Interview ID {} already exists in the database. Skipping.",
											interviewId);
									continue;
								}

								try {
									String dataUrl = String.format(
											"http://api.dooblo.net/newapi/SurveyInterviewData?subjectIDs=%s&surveyID=%s&onlyHeaders=false&includeNulls=false&noAnswerData=true",
											interviewId, surveyId);
									ResponseEntity<String> dataResponse = restTemplate.exchange(dataUrl, HttpMethod.GET,
											entity, String.class);
									LOGGER.info("Successfully retrieved interview data for InterviewID {}.",
											interviewId);
									processAndSaveSurveyData(dataResponse.getBody(), surveyId, interviewId);

								} catch (Exception e) {
									LOGGER.error("Failed to retrieve interview data for InterviewID: {}", interviewId,
											e);
								}
							}
						}

					} catch (Exception e) {
						LOGGER.error("Failed to retrieve interview IDs for SurveyID: {}", surveyId, e);
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error("Failed to retrieve active surveys from Dooblo API.", e);
		}
		LOGGER.info("Dooblo Survey Retriever task finished.");
	}

	private void processAndSaveSurveyData(String xmlData, String surveyId, String interviewId) {
		try {
			XmlMapper xmlMapper = new XmlMapper();
			JsonNode rootNode = xmlMapper.readTree(xmlData.getBytes(StandardCharsets.UTF_8));

			String surveyorName = rootNode.path("SurveyorName").asText();
			String dateStr = rootNode.path("Date").asText();
			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateStr);

			Optional<Surveyor> surveyorOpt = surveyorRepository.findByFirstName(surveyorName);
			Optional<Fieldwork> fieldworkOpt = fieldworkRepository.findByDoobloId(surveyId);
			DoobloResponse doobloResponse = new DoobloResponse();
			doobloResponse.setSurveyor(surveyorOpt.get());
			doobloResponse.setInterviewId(interviewId);
			doobloResponse.setDate(date);
			if (fieldworkOpt.get() != null) {
				Fieldwork fw = new Fieldwork();
				doobloResponse.setFieldwork(fw);
				fw.setCompleted(fw.getCompleted() + 1);
				fieldworkRepository.save(fw);

				// Se afecta elbudget si ya tiene asignado
				if (fw != null && fw.getBudgetEntry() != null && fw.getUnitCost() != null) {
					BudgetEntry budgetEntry = fw.getBudgetEntry();
					budgetEntry.setSpent(budgetEntry.getSpent() + fw.getUnitCost().doubleValue());
					budgetEntryService.save(budgetEntry);

				}

			}
			doobloResponseRepository.save(doobloResponse);
			LOGGER.info("Successfully saved DoobloResponse for interview ID {}", interviewId);

		} catch (Exception e) {
			LOGGER.error("Failed to process XML and save DoobloResponse for interview ID {}", interviewId, e);
		}
	}

	private HttpEntity<String> createAuthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
		String authHeader = "Basic " + new String(encodedAuth);
		headers.set("Authorization", authHeader);
		return new HttpEntity<>(headers);
	}
}
