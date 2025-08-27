package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class Study extends AbstractEntity {

	private String name;
	private String alchemerId;
	private String doobloId;
	private String odooId;
	private String obs;
	private double debt;
	private double totalCost;
	private boolean showSurveyor;

	public boolean isShowSurveyor() {
		return showSurveyor;
	}

	public void setShowSurveyor(boolean showSurveyor) {
		this.showSurveyor = showSurveyor;
	}

	public double getDebt() {
		return debt;
	}

	public void setDebt(double debt) {
		this.debt = debt;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
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

	public String getAlchemerId() {
		return alchemerId;
	}

	public void setAlchemerId(String alchemerId) {
		this.alchemerId = alchemerId;
	}

	public String getDoobloId() {
		return doobloId;
	}

	public void setDoobloId(String doobloId) {
		this.doobloId = doobloId;
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

}
