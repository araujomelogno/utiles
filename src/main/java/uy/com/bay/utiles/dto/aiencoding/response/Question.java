package uy.com.bay.utiles.dto.aiencoding.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Question {
    @JsonProperty("question_id")
    private String questionId;
    private List<Coding> codings;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public List<Coding> getCodings() {
        return codings;
    }

    public void setCodings(List<Coding> codings) {
        this.codings = codings;
    }
}
