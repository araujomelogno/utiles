package uy.com.bay.utiles.data;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class AlchemerAnswer extends AbstractEntity {

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
	private String studyTeam;
	private String campaignName;
	private String surveyor;

	private LocalDate created = LocalDate.now();

	public LocalDate getCreated() {
		return created;
	}

	public void setCreated(LocalDate created) {
		this.created = created;
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

	public String getSurveyor() {
		return surveyor;
	}

	public void setSurveyor(String surveyor) {
		this.surveyor = surveyor;
	}

	public String getStudyTeam() {
		return studyTeam;
	}

	public void setStudyTeam(String studyTeam) {
		this.studyTeam = studyTeam;
	}

	public String getCampaignName() {
		return campaignName;
	}

	public void setCampaignName(String campaignName) {
		this.campaignName = campaignName;
	}

}
