package uy.com.bay.utiles.data;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AlchemerAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long alchemerId;
	private String type;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String question;
	private Integer sectionId;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String answer;
	private boolean shown;
	private Integer surveyId;
	private Integer responseId;
	private String studyName;

	private LocalDate created = LocalDate.now();

	public LocalDate getCreated() {
		return created;
	}

	public void setCreated(LocalDate created) {
		this.created = created;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAlchemerId() {
		return alchemerId;
	}

	public void setAlchemerId(Long alchemerId) {
		this.alchemerId = alchemerId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public Integer getSectionId() {
		return sectionId;
	}

	public void setSectionId(Integer sectionId) {
		this.sectionId = sectionId;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public boolean isShown() {
		return shown;
	}

	public void setShown(boolean shown) {
		this.shown = shown;
	}

	public String getSurveyId() {
		return surveyId.toString();
	}

	public void setSurveyId(Integer surveyId) {
		this.surveyId = surveyId;
	}

	public Integer getResponseId() {
		return responseId;
	}

	public String getResponseIdString() {
		return responseId.toString();
	}

	public void setResponseId(Integer responseId) {
		this.responseId = responseId;
	}

	public String getStudyName() {
		return studyName;
	}

	public void setStudyName(String studyName) {
		this.studyName = studyName;
	}
}
