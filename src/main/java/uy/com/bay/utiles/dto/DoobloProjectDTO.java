package uy.com.bay.utiles.dto;

/**
 * Proyecto (estudio) de Dooblo tal como lo devuelve el endpoint
 * {@code Account/GetUsageByPeriod}: el nombre del estudio y su identificador en
 * Dooblo.
 */
public class DoobloProjectDTO {

	private String surveyName;
	private String surveyId;

	public DoobloProjectDTO() {
	}

	public DoobloProjectDTO(String surveyName, String surveyId) {
		this.surveyName = surveyName;
		this.surveyId = surveyId;
	}

	public String getSurveyName() {
		return surveyName;
	}

	public void setSurveyName(String surveyName) {
		this.surveyName = surveyName;
	}

	public String getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(String surveyId) {
		this.surveyId = surveyId;
	}
}
