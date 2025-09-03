package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class Study extends AbstractEntity {

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

}
