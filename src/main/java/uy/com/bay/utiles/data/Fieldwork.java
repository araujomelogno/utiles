package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Fieldwork extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "study_id")
	private Study study;

	private LocalDate initPlannedDate;
	private LocalDate endPlannedDate;
	private LocalDate initDate;
	private LocalDate endDate;
	private Integer goalQuantity;
	private Integer completed;
	private String obs;

	@Enumerated(EnumType.STRING)
	private FieldworkStatus status;

	@Enumerated(EnumType.STRING)
	private FieldworkType type;

	@ManyToOne
	@JoinColumn(name = "area_id")
	private Area area;

	private String doobloId;
	private String alchemerId;

	public String getDoobloId() {
		return doobloId;
	}

	public void setDoobloId(String doobloId) {
		this.doobloId = doobloId;
	}

	public String getAlchemerId() {
		return alchemerId;
	}

	public void setAlchemerId(String alchemerId) {
		this.alchemerId = alchemerId;
	}

	public Area getArea() {
		return area;
	}

	public void setArea(Area area) {
		this.area = area;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public LocalDate getInitPlannedDate() {
		return initPlannedDate;
	}

	public void setInitPlannedDate(LocalDate initPlannedDate) {
		this.initPlannedDate = initPlannedDate;
	}

	public LocalDate getEndPlannedDate() {
		return endPlannedDate;
	}

	public void setEndPlannedDate(LocalDate endPlannedDate) {
		this.endPlannedDate = endPlannedDate;
	}

	public LocalDate getInitDate() {
		return initDate;
	}

	public void setInitDate(LocalDate initDate) {
		this.initDate = initDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Integer getGoalQuantity() {
		return goalQuantity;
	}

	public void setGoalQuantity(Integer goalQuantity) {
		this.goalQuantity = goalQuantity;
	}

	public Integer getCompleted() {
		return completed;
	}

	public void setCompleted(Integer completed) {
		this.completed = completed;
	}

	public String getObs() {
		return obs;
	}

	public void setObs(String obse) {
		this.obs = obse;
	}

	public FieldworkStatus getStatus() {
		return status;
	}

	public void setStatus(FieldworkStatus status) {
		this.status = status;
	}

	public FieldworkType getType() {
		return type;
	}

	public void setType(FieldworkType type) {
		this.type = type;
	}
}
