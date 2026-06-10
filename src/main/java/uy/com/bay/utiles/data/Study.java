package uy.com.bay.utiles.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uy.com.bay.utiles.entities.Budget;

@Entity
public class Study extends AbstractEntity {

	@OneToOne(mappedBy = "study")
	private Budget budget;

	@OneToMany(mappedBy = "study", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<Fieldwork> fieldworks;
	private String name;
	private String odooId;
	private String obs;
	private double totalReportedCost;
	private double totalTransfered;
	private boolean showSurveyor;
	private String clientName;
	private double expectedRevenue;
	private String area;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "study_meta_cost_by_date", joinColumns = @JoinColumn(name = "study_id"))
	@MapKeyColumn(name = "cost_date")
	@MapKeyTemporal(TemporalType.DATE)
	@Column(name = "cost")
	private Map<Date, Double> metaCostByDate = new HashMap<>();

	public boolean isShowSurveyor() {
		return showSurveyor;
	}

	public void setShowSurveyor(boolean showSurveyor) {
		this.showSurveyor = showSurveyor;
	}

	public double getTotalReportedCost() {
		return totalReportedCost;
	}

	public void setTotalReportedCost(double debt) {
		this.totalReportedCost = debt;
	}

	public double getTotalTransfered() {
		return totalTransfered;
	}

	public void setTotalTransfered(double totalCost) {
		this.totalTransfered = totalCost;
	}

	public String getName() {
		if (name != null)
			return name;
		else
			return "";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOdooId() {
		return odooId;
	}

	public void setOdooId(String odooId) {
		this.odooId = odooId;
	}

	public String getObs() {
		return obs;
	}

	public void setObs(String obs) {
		this.obs = obs;
	}

	public List<Fieldwork> getFieldworks() {
		return fieldworks;
	}

	public void setFieldworks(List<Fieldwork> fieldworks) {
		this.fieldworks = fieldworks;
	}

	public Budget getBudget() {
		return budget;
	}

	public void setBudget(Budget budget) {
		this.budget = budget;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public double getExpectedRevenue() {
		return expectedRevenue;
	}

	public void setExpectedRevenue(double expectedRevenue) {
		this.expectedRevenue = expectedRevenue;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public Map<Date, Double> getMetaCostByDate() {
		return metaCostByDate;
	}

	public void setMetaCostByDate(Map<Date, Double> metaCostByDate) {
		this.metaCostByDate = metaCostByDate;
	}
}
