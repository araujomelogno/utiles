package uy.com.bay.utiles.tasks;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

import uy.com.bay.utiles.data.DoobloResponse;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.StudyRepository;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;
import uy.com.bay.utiles.data.repository.DoobloResponseRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.dto.CompletedSurveysCount;
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

	public CompletedSurveysCount getCompletedSurveys(List<String> surveyIds, Date fromDate, Date toDate) {
		CompletedSurveysCount merged = new CompletedSurveysCount();
		if (surveyIds == null) {
			return merged;
		}
		for (String surveyId : surveyIds) {
			if (surveyId == null || surveyId.isBlank()) {
				continue;
			}
			merged.merge(getCompletedSurveys(surveyId, fromDate, toDate));
		}
		return merged;
	}

	/**
	 * Obtiene los completos (descontando los cancelados) mes a mes y, para los
	 * meses con completos, dia a dia (solo hasta el dia de hoy).
	 */
	public CompletedSurveysCount getCompletedSurveys(String surveyId, Date fromDate, Date toDate) {
		CompletedSurveysCount result = new CompletedSurveysCount();
		if (fromDate == null || toDate == null || fromDate.after(toDate)) {
			return result;
		}

		HttpEntity<String> entity = createAuthHeaders();
		Date now = new Date();

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

			int monthCount = 0;
			int monthCancelled = 0;
			try {
				monthCount = countInterviews(entity, surveyId, monthStart, monthEnd, false);
				monthCancelled = countInterviews(entity, surveyId, monthStart, monthEnd, true);
				result.getByMonth().put(monthStart, monthCount - monthCancelled);
			} catch (Exception e) {
				LOGGER.error("Failed to retrieve completed surveys for SurveyID {} for month {}", surveyId, monthStart,
						e);
				showNotification(String.format("Failed to retrieve completed surveys for SurveyID %s for month %s",
						surveyId, monthStart));
				result.getByMonth().put(monthStart, 0);
				monthCount = 0;
			}

			if (monthCount - monthCancelled > 0) {
				int daysSum = 0;
				Calendar dayCursor = (Calendar) cursor.clone();
				while (!dayCursor.getTime().after(monthEnd) && !dayCursor.getTime().after(now)) {
					Date day = dayCursor.getTime();
					try {
						int dayCount = countInterviews(entity, surveyId, day, day, false);
						// Solo se consultan los cancelados del dia si hubo cancelados en el mes.
						int dayCancelled = (dayCount > 0 && monthCancelled > 0)
								? countInterviews(entity, surveyId, day, day, true)
								: 0;
						int completed = dayCount - dayCancelled;
						if (completed != 0) {
							result.getByDay().put(day, completed);
							daysSum += completed;
						}
					} catch (Exception e) {
						LOGGER.error("Failed to retrieve completed surveys for SurveyID {} for day {}", surveyId, day,
								e);
					}
					dayCursor.add(Calendar.DAY_OF_MONTH, 1);
				}
				if (daysSum != monthCount - monthCancelled) {
					LOGGER.warn("Dooblo SurveyID {}: la suma diaria ({}) no coincide con el total del mes {} ({})",
							surveyId, daysSum, monthStart, monthCount - monthCancelled);
				}
			}

			cursor.add(Calendar.MONTH, 1);
		}

		return result;
	}

	/**
	 * Cantidad de entrevistas completas entre {@code from} y {@code to} (fechas
	 * inclusive, se usa solo el dia). Con {@code cancelled} se cuentan solo las
	 * canceladas (status 7).
	 */
	private int countInterviews(HttpEntity<String> entity, String surveyId, Date from, Date to, boolean cancelled)
			throws Exception {
		Thread.sleep(2000);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String fromStr = URLEncoder.encode(dateFormat.format(from), StandardCharsets.UTF_8);
		String toStr = URLEncoder.encode(dateFormat.format(to), StandardCharsets.UTF_8);

		String url = String.format(
				"http://api.dooblo.net/newapi/SurveyInterviewIDs?surveyIDs=%s&testMode=False&completed=True&filtered=False%s&dateStart=%s&dateEnd=%s",
				surveyId, cancelled ? "&statuses=7" : "", fromStr, toStr);

		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		LOGGER.info("Successfully retrieved {}interview IDs for SurveyID {} between {} and {}. Response: {}",
				cancelled ? "cancelled " : "", surveyId, fromStr, toStr, response.getBody());

		JsonNode root = new ObjectMapper().readTree(response.getBody());
		return (root != null && root.isArray()) ? root.size() : 0;
	}

	private void showNotification(String message) {
		// La tarea programada corre sin UI: en ese caso solo se loguea.
		if (UI.getCurrent() != null) {
			Notification.show(message, 5000, Position.MIDDLE);
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
