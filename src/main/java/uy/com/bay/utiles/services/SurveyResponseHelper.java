package uy.com.bay.utiles.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.data.AlchemerContact;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;
import uy.com.bay.utiles.data.AlchemerSurveyResponseData;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;
import uy.com.bay.utiles.data.repository.AlchemerContactRepository;
import uy.com.bay.utiles.dto.SurveyResponseDTO;

@Service
public class SurveyResponseHelper {

    private final AlchemerContactRepository contactRepository;
    private final AlchemerAnswerRepository answerRepository;
    private final ObjectMapper objectMapper;

    public SurveyResponseHelper(AlchemerContactRepository contactRepository,
                                AlchemerAnswerRepository answerRepository,
                                ObjectMapper objectMapper) {
        this.contactRepository = contactRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
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
