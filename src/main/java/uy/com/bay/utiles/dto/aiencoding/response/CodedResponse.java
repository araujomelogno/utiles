package uy.com.bay.utiles.dto.aiencoding.response;

import java.util.List;

public class CodedResponse {
    private List<Question> questions;

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
