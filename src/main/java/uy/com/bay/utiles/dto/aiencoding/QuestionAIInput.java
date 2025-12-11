package uy.com.bay.utiles.dto.aiencoding;

import java.util.ArrayList;
import java.util.List;

public class QuestionAIInput {
	private String question_text = "";
	private String question_id =  "";
	private String question_fineTunning = "";
	private List<QuestionAICode> codes = new ArrayList();
	private List<QuestionAIAnswer> responses = new ArrayList();


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

	public String getQuestion_text() {
		return question_text;
	}

	public void setQuestion_text(String question_text) {
		this.question_text = question_text;
	}

	public String getQuestion_id() {
		return question_id;
	}

	public void setQuestion_id(String question_id) {
		this.question_id = question_id;
	}

}
