package uy.com.bay.utiles.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.TemporalType;
import uy.com.bay.utiles.entities.BudgetEntry;

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
 
	private String obs;

	@Enumerated(EnumType.STRING)
	private FieldworkStatus status;

	@Enumerated(EnumType.STRING)
	private FieldworkType type;;

 
	@ManyToOne
	@JoinColumn(name = "budget_entry_id")
	private BudgetEntry budgetEntry;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "fieldwork_dooblo_id", joinColumns = @JoinColumn(name = "fieldwork_id"))
	@OrderColumn(name = "id_order")
	@Column(name = "dooblo_id")
	private List<String> doobloId = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "fieldwork_alchemer_id", joinColumns = @JoinColumn(name = "fieldwork_id"))
	@OrderColumn(name = "id_order")
	@Column(name = "alchemer_id")
	private List<String> alchemerId = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "fieldwork_completed_by_month", joinColumns = @JoinColumn(name = "fieldwork_id"))
	@MapKeyColumn(name = "month")
	@MapKeyTemporal(TemporalType.DATE)
	@Column(name = "completed")
	private Map<Date, Integer> completedByMonth = new HashMap<>();

	public List<String> getDoobloId() {
		return doobloId;
	}

	public void setDoobloId(List<String> doobloId) {
		this.doobloId = doobloId != null ? doobloId : new ArrayList<>();
	}

	public List<String> getAlchemerId() {
		return alchemerId;
	}

	public void setAlchemerId(List<String> alchemerId) {
		this.alchemerId = alchemerId != null ? alchemerId : new ArrayList<>();
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
		if (goalQuantity != null)
			return goalQuantity;
		else
			return 0;
	}

	public void setGoalQuantity(Integer goalQuantity) {
		this.goalQuantity = goalQuantity;
	}

	public Integer getCompleted() {
		Integer sum = 0;
		for (Integer c : completedByMonth.values())
			sum += c;
		return sum;
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

	public BudgetEntry getBudgetEntry() {
		return budgetEntry;
	}

	public void setBudgetEntry(BudgetEntry budgetEntry) {
		this.budgetEntry = budgetEntry;
	}

	public Map<Date, Integer> getCompletedByMonth() {
		return completedByMonth;
	}

	public void setCompletedByMonth(Map<Date, Integer> completedByMonth) {
		this.completedByMonth = completedByMonth;
	}
}
