package uy.com.bay.utiles.entities;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Fieldwork;

@Entity
public class BudgetEntry extends AbstractEntity {

	private Double ammount;
	private Integer quantity;
	private LocalDate created;
	private LocalDate init;
	private LocalDate end;

	public BudgetEntry() {
		this.created = LocalDate.now();

	}

	@ManyToOne
	@JoinColumn(name = "budget_concept_id")
	private BudgetConcept concept;

	@ManyToOne
	@JoinColumn(name = "budget_id")
	private Budget budget;

	@OneToMany(mappedBy = "budgetEntry")
	private Set<Extra> extras = new HashSet<>();

	@OneToMany(mappedBy = "budgetEntry")
	private Set<ExpenseRequest> expenseRequests = new HashSet<>();

	@OneToMany(mappedBy = "budgetEntry")
	private Set<Fieldwork> fieldworks = new HashSet<>();

	@OneToMany(mappedBy = "budgetEntry")
	private Set<OdooCost> odooCosts = new HashSet<>();

	@Transient
	private Double total;

	public Double getTotal() {
		if (ammount != null && quantity != null) {
			return ammount * quantity;
		}
		return 0.0;
	}

	public Double getAmmount() {
		if (ammount != null)
			return ammount;
		return 0d;
	}

	public void setAmmount(Double ammount) {
		this.ammount = ammount;
	}

	public Integer getQuantity() {
		if (quantity != null)
			return quantity;
		return 0;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Double getSpent() {
		double totalSpent = 0.0;
		if (extras != null) {
			for (Extra extra : extras) {
				if (extra.getQuantity() != null && extra.getUnitPrice() != null) {
					totalSpent += extra.getQuantity() * extra.getUnitPrice();
				}
			}
		}
		if (expenseRequests != null) {
			for (ExpenseRequest expenseRequest : expenseRequests) {
				if ((ExpenseStatus.TRANSFERIDO.equals(expenseRequest.getExpenseStatus())
						|| ExpenseStatus.RENDIDO.equals(expenseRequest.getExpenseStatus()))
						&& expenseRequest.getExpenseTransfer() != null
						&& expenseRequest.getExpenseTransfer().getAmount() != null) {
					totalSpent += expenseRequest.getExpenseTransfer().getAmount();
				}
			}
		}
		if (fieldworks != null) {
			for (Fieldwork fieldwork : fieldworks) {
				if (ammount != null && fieldwork.getCompleted() != null) {
					totalSpent += ammount * fieldwork.getCompleted();
				}
			}
		}

		if (odooCosts != null) {
			for (OdooCost cost : odooCosts) {
				totalSpent += cost.getBalance().doubleValue();
			}
		}
		return totalSpent;
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

	public Set<Extra> getExtras() {
		return extras;
	}

	public void setExtras(Set<Extra> extras) {
		this.extras = extras;
	}

	public Set<Fieldwork> getFieldworks() {
		return fieldworks;
	}

	public void setFieldworks(Set<Fieldwork> fieldworks) {
		this.fieldworks = fieldworks;
	}

	public Set<ExpenseRequest> getExpenseRequests() {
		return expenseRequests;
	}

	public void setExpenseRequests(Set<ExpenseRequest> expenseRequests) {
		this.expenseRequests = expenseRequests;
	}

	public Set<OdooCost> getOdooCosts() {
		return odooCosts;
	}

	public void setOdooCosts(Set<OdooCost> odooCosts) {
		this.odooCosts = odooCosts;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public LocalDate getCreated() {
		if (created == null)
			return LocalDate.now();
		return created;
	}

	public void setCreated(LocalDate created) {
		this.created = created;
	}

	public LocalDate getInit() {
		return init;
	}

	public void setInit(LocalDate init) {
		this.init = init;
	}

	public LocalDate getEnd() {
		return end;
	}

	public void setEnd(LocalDate end) {
		this.end = end;
	}

}