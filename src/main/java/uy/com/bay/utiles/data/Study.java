package uy.com.bay.utiles.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.List;
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
}
