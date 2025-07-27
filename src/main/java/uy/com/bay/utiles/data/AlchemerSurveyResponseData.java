package uy.com.bay.utiles.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "alchemer_survey_response_data")
public class AlchemerSurveyResponseData extends AbstractEntity {

    @JsonProperty("is_test")
    private boolean isTest;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("response_id")
    private int responseId;

    @JsonProperty("account_id")
    private int accountId;

    @JsonProperty("survey_id")
    private int surveyId;

    @JsonProperty("response_status")
    private String responseStatus;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_id", referencedColumnName = "id")
    private AlchemerContact contact;

    @OneToOne(mappedBy = "data")
    private AlchemerSurveyResponse surveyResponse;

    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean test) {
        isTest = test;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(int responseId) {
        this.responseId = responseId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public AlchemerContact getContact() {
        return contact;
    }

    public void setContact(AlchemerContact contact) {
        if (contact != null) {
            contact.setSurveyResponseData(this);
        }
        this.contact = contact;
    }

    public AlchemerSurveyResponse getSurveyResponse() {
        return surveyResponse;
    }

    public void setSurveyResponse(AlchemerSurveyResponse surveyResponse) {
        this.surveyResponse = surveyResponse;
    }
}
