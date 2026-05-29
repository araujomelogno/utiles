package uy.com.bay.utiles.tasks;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import uy.com.bay.utiles.data.DoobloResponse;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.StudyRepository;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;
import uy.com.bay.utiles.data.repository.DoobloResponseRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
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
//				fw.setCompleted(fw.getCompleted() + 1);

				fieldworkRepository.save(fw);

//				// Se afecta elbudget si ya tiene asignado
//				if (fw != null && fw.getBudgetEntry() != null && fw.getUnitCost() != null) {
//					BudgetEntry budgetEntry = fw.getBudgetEntry();
//					budgetEntryService.save(budgetEntry);
//
//				}

			}
			doobloResponseRepository.save(doobloResponse);
			LOGGER.info("Successfully saved DoobloResponse for interview ID {}", interviewId);

		} catch (Exception e) {
			LOGGER.error("Failed to process XML and save DoobloResponse for interview ID {}", interviewId, e);
		}
	}

	public Map<Date, Integer> getCompletedSurveys(String surveyId, Date fromDate, Date toDate) {
		Map<Date, Integer> result = new LinkedHashMap<>();
		if (fromDate == null || toDate == null || fromDate.after(toDate)) {
			return result;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		HttpEntity<String> entity = createAuthHeaders();
		ObjectMapper mapper = new ObjectMapper();

		Calendar cursor = Calendar.getInstance();
		cursor.setTime(fromDate);
		cursor.set(Calendar.DAY_OF_MONTH, 1);
		cursor.set(Calendar.HOUR_OF_DAY, 0);
		cursor.set(Calendar.MINUTE, 0);
		cursor.set(Calendar.SECOND, 0);
		cursor.set(Calendar.MILLISECOND, 0);

		while (!cursor.getTime().after(toDate)) {
			Date monthStart = cursor.getTime();

			Calendar monthEndCal = (Calendar) cursor.clone();
			monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
			monthEndCal.set(Calendar.HOUR_OF_DAY, 23);
			monthEndCal.set(Calendar.MINUTE, 59);
			monthEndCal.set(Calendar.SECOND, 59);
			Date monthEnd = monthEndCal.getTime();

			try {
				Thread.sleep(2000);

				String fromStr = URLEncoder.encode(dateFormat.format(monthStart), StandardCharsets.UTF_8);
				String toStr = URLEncoder.encode(dateFormat.format(monthEnd), StandardCharsets.UTF_8);

				String interviewsUrl = String.format(
						"http://api.dooblo.net/newapi/SurveyInterviewIDs?surveyIDs=%s&testMode=False&completed=True&filtered=False&dateStart=%s&dateEnd=%s",
						surveyId, fromStr, toStr);

				ResponseEntity<String> interviewsResponse = restTemplate.exchange(interviewsUrl, HttpMethod.GET, entity,
						String.class);
				LOGGER.info("Successfully retrieved interview IDs for SurveyID {} between {} and {}. Response: {}",
						surveyId, monthStart, monthEnd, interviewsResponse.getBody());

				JsonNode interviewsRoot = mapper.readTree(interviewsResponse.getBody());
				int count = (interviewsRoot != null && interviewsRoot.isArray()) ? interviewsRoot.size() : 0;

				Thread.sleep(2000);
				String cancelledInterviewsUrl = String.format(
						"http://api.dooblo.net/newapi/SurveyInterviewIDs?surveyIDs=%s&testMode=False&completed=True&filtered=False&statuses=7&dateStart=%s&dateEnd=%s",
						surveyId, fromStr, toStr);

				ResponseEntity<String> cancelledInterviewsResponse = restTemplate.exchange(cancelledInterviewsUrl,
						HttpMethod.GET, entity, String.class);
				LOGGER.info("Successfully retrieved interview IDs for SurveyID {} between {} and {}. Response: {}",
						surveyId, monthStart, monthEnd, cancelledInterviewsResponse.getBody());

				JsonNode canceledInterviewsRoot = mapper.readTree(cancelledInterviewsResponse.getBody());
				int cancelledCount = (canceledInterviewsRoot != null && canceledInterviewsRoot.isArray())
						? canceledInterviewsRoot.size()
						: 0;

				result.put(monthStart, count - cancelledCount);
			} catch (Exception e) {
				LOGGER.error("Failed to retrieve completed surveys for SurveyID {} for month {}", surveyId, monthStart,
						e);
				Notification.show("Failed to retrieve completed surveys for SurveyID {} for month {}", 5000,
						Position.MIDDLE);
				result.put(monthStart, 0);
			}

			cursor.add(Calendar.MONTH, 1);
		}

		return result;
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
