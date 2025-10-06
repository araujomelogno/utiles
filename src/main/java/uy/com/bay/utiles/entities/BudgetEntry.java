package uy.com.bay.utiles.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import uy.com.bay.utiles.data.AbstractEntity;

@Entity
public class BudgetEntry extends AbstractEntity {

	private Double ammount;
	private Integer quantity;
	private Double spent;

	@ManyToOne
	@JoinColumn(name = "budget_concept_id")
	private BudgetConcept concept;

	@ManyToOne
	@JoinColumn(name = "budget_id")
	private Budget budget;

	@Transient
	private Double total;

	public Double getTotal() {
		if (ammount != null && quantity != null) {
			return ammount * quantity;
		}
		return 0.0;
	}

	public Double getAmmount() {
		return ammount;
	}

	public void setAmmount(Double ammount) {
		this.ammount = ammount;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Double getSpent() {
		if (spent != null)
			return spent;
		return 0d;
	}

	public void setSpent(Double spent) {
		this.spent = spent;
	}

	public BudgetConcept getConcept() {
		return concept;
	}

	public void setConcept(BudgetConcept concept) {
		this.concept = concept;
	}

	public Budget getBudget() {
		return budget;
	}

	public void setBudget(Budget budget) {
		this.budget = budget;
	}
}