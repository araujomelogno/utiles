package uy.com.bay.utiles.services;

import java.util.ArrayList;
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

    public Integer getCompletedSurveys(String surveyId) {
        try {
            String url = String.format(
                    "https://api.alchemer.com/v5/survey/%s/surveyresponse?api_token=%s&api_token_secret=%s"
                            + "&filter[field][0]=status&filter[operator][0]==&filter[value][0]=Complete",
                    surveyId, apiToken, apiTokenSecret);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return root.path("total_count").asInt();
        } catch (Exception e) {
            LOGGER.error("Error fetching completed surveys from Alchemer API for surveyId {}", surveyId, e);
            return 0;
        }
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
