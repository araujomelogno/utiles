package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class Surveyor extends AbstractEntity {

	private String firstName;
	private String lastName;
	private String ci;
	private String surveyToGoId;
	
	public String getSurveyToGoId() {
		return surveyToGoId;
	}

	public void setSurveyToGoId(String surveyToGoId) {
		this.surveyToGoId = surveyToGoId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCi() {
		return ci;
	}

	public void setCi(String ci) {
		this.ci = ci;
	}

}
