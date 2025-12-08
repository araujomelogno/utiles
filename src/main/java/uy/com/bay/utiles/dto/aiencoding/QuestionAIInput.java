package uy.com.bay.utiles.dto.aiencoding;

import java.util.ArrayList;
import java.util.List;

public class QuestionAIInput {
	private String question;
	private String question_fineTunning;
	private List<QuestionAICode> codes = new ArrayList();
	private List<QuestionAIAnswer> responses = new ArrayList();

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getQuestion_fineTunning() {
		return question_fineTunning;
	}

	public void setQuestion_fineTunning(String question_fineTunning) {
		this.question_fineTunning = question_fineTunning;
	}

	public List<QuestionAICode> getCodes() {
		return codes;
	}

	public void setCodes(List<QuestionAICode> codes) {
		this.codes = codes;
	}

	public List<QuestionAIAnswer> getResponses() {
		return responses;
	}

	public void setResponses(List<QuestionAIAnswer> responses) {
		this.responses = responses;
	}

}
