package uy.com.bay.utiles.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.data.AlchemerContact;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;
import uy.com.bay.utiles.data.AlchemerSurveyResponseData;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;
import uy.com.bay.utiles.data.repository.AlchemerContactRepository;
import uy.com.bay.utiles.dto.CompletedSurveysCount;
import uy.com.bay.utiles.dto.SurveyResponseDTO;

@Service
public class AlchemerSurveyResponseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemerSurveyResponseHelper.class);

    private final AlchemerContactRepository contactRepository;
    private final AlchemerAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alchemer.api.token}")
    private String apiToken;

    @Value("${alchemer.api.token.secret}")
    private String apiTokenSecret;

    public AlchemerSurveyResponseHelper(AlchemerContactRepository contactRepository,
                                AlchemerAnswerRepository answerRepository,
                                ObjectMapper objectMapper) {
        this.contactRepository = contactRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
    }

    public CompletedSurveysCount getCompletedSurveys(List<String> surveyIds, Date startDate, Date endDate) {
        CompletedSurveysCount merged = new CompletedSurveysCount();
        if (surveyIds == null) {
            return merged;
        }
        for (String surveyId : surveyIds) {
            if (surveyId == null || surveyId.isBlank()) {
                continue;
            }
            merged.merge(getCompletedSurveys(surveyId, startDate, endDate));
        }
        return merged;
    }

    /**
     * Obtiene los completos mes a mes y, para los meses con completos, dia a dia
     * (solo hasta el dia de hoy).
     */
    public CompletedSurveysCount getCompletedSurveys(String surveyId, Date startDate, Date endDate) {
        CompletedSurveysCount result = new CompletedSurveysCount();
        if (startDate == null || endDate == null || startDate.after(endDate)) {
            return result;
        }

        Date now = new Date();
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(startDate);
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);

        while (!cursor.getTime().after(endDate)) {
            Date monthStart = cursor.getTime();

            Calendar monthEndCal = (Calendar) cursor.clone();
            monthEndCal.set(Calendar.DAY_OF_MONTH, monthEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
            monthEndCal.set(Calendar.HOUR_OF_DAY, 23);
            monthEndCal.set(Calendar.MINUTE, 59);
            monthEndCal.set(Calendar.SECOND, 59);
            Date monthEnd = monthEndCal.getTime();

            int monthCount;
            try {
                monthCount = countCompleted(surveyId, monthStart, monthEnd);
            } catch (Exception e) {
                LOGGER.error("Error fetching completed surveys from Alchemer API for surveyId {} month {}",
                        surveyId, monthStart, e);
                monthCount = 0;
            }
            result.getByMonth().put(monthStart, monthCount);

            if (monthCount > 0) {
                int daysSum = 0;
                Calendar dayCursor = (Calendar) cursor.clone();
                while (!dayCursor.getTime().after(monthEnd) && !dayCursor.getTime().after(now)) {
                    Date dayStart = dayCursor.getTime();
                    Calendar dayEndCal = (Calendar) dayCursor.clone();
                    dayEndCal.set(Calendar.HOUR_OF_DAY, 23);
                    dayEndCal.set(Calendar.MINUTE, 59);
                    dayEndCal.set(Calendar.SECOND, 59);
                    try {
                        int dayCount = countCompleted(surveyId, dayStart, dayEndCal.getTime());
                        if (dayCount > 0) {
                            result.getByDay().put(dayStart, dayCount);
                            daysSum += dayCount;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error fetching completed surveys from Alchemer API for surveyId {} day {}",
                                surveyId, dayStart, e);
                    }
                    dayCursor.add(Calendar.DAY_OF_MONTH, 1);
                }
                if (daysSum != monthCount) {
                    LOGGER.warn("Alchemer surveyId {}: la suma diaria ({}) no coincide con el total del mes {} ({})",
                            surveyId, daysSum, monthStart, monthCount);
                }
            }

            cursor.add(Calendar.MONTH, 1);
        }

        return result;
    }

    private int countCompleted(String surveyId, Date from, Date to) throws JsonProcessingException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startStr = URLEncoder.encode(dateFormat.format(from), StandardCharsets.UTF_8);
        String endStr = URLEncoder.encode(dateFormat.format(to), StandardCharsets.UTF_8);

        String url = String.format(
                "https://api.alchemer.com/v5/survey/%s/surveyresponse?api_token=%s&api_token_secret=%s"
                        + "&filter[field][0]=date_submitted&filter[operator][0]=>=&filter[value][0]=%s"
                        + "&filter[field][1]=date_submitted&filter[operator][1]=<=&filter[value][1]=%s"
                        + "&filter[field][2]=status&filter[operator][2]==&filter[value][2]=Complete"
                        + "&resultsperpage=1",
                surveyId, apiToken, apiTokenSecret, startStr, endStr);

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);
        return root.path("total_count").asInt();
    }

    public String buildSurveyResponseJson(int surveyId, String phoneNumber) throws JsonProcessingException {
        List<SurveyResponseDTO> resultado = new ArrayList<>();

        List<AlchemerContact> contacts = contactRepository
                .findByInviteCustom1AndSurveyResponseDataSurveyId(phoneNumber, surveyId);

        for (AlchemerContact contact : contacts) {
            AlchemerSurveyResponseData responseData = contact.getSurveyResponseData();
            if (responseData == null) {
                continue;
            }

            AlchemerSurveyResponse surveyResponse = responseData.getSurveyResponse();
            if (surveyResponse == null) {
                continue;
            }

            List<AlchemerAnswer> answers = answerRepository
                    .findByResponseIdAndSurveyId(surveyResponse.getId(), surveyId);

            for (AlchemerAnswer answer : answers) {
                resultado.add(new SurveyResponseDTO(answer.getQuestion(), answer.getAnswer()));
            }
        }

        return objectMapper.writeValueAsString(resultado);
    }
}
