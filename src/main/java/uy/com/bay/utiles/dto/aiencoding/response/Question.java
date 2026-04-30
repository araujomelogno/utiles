package uy.com.bay.utiles.dto.aiencoding.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Question {
    @JsonProperty("question_id")
    private String caseId;
    
    private List<Coding> codings;

    public String getCaseId() {
        return caseId;
    }

    public void caseId(String questionId) {
        this.caseId = questionId;
    }

    public List<Coding> getCodings() {
        return codings;
    }

    public void setCodings(List<Coding> codings) {
        this.codings = codings;
    }
}
