package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class ExpenseRequestType extends AbstractEntity {

	private String name;
	private String description;
	private boolean refund;

	public String getName() {
		if (name != null)
			return name;
		else
			return "";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRefund() {
		return refund;
	}

	public void setRefund(boolean refund) {
		this.refund = refund;
	}
	
	
}
