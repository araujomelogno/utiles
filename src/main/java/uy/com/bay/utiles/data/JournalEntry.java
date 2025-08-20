package uy.com.bay.utiles.data;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class JournalEntry extends AbstractEntity {

	private String detail;
	private String obs;
	private Date date;
	@Enumerated(EnumType.STRING)
	private Operation operation;
	private double amount;
	@ManyToOne
    @JoinColumn(name = "surveyor_id")
	private Surveyor surveyor;
	@ManyToOne
    @JoinColumn(name = "study_id")
	private Study study;

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getObs() {
		return obs;
	}

	public void setObs(String obs) {
		this.obs = obs;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public Surveyor getSurveyor() {
		return surveyor;
	}

	public void setSurveyor(Surveyor surveyor) {
		this.surveyor = surveyor;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

}
