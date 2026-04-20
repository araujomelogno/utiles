package uy.com.bay.utiles.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import uy.com.bay.utiles.data.AbstractEntity;

@Entity
@Table(name = "odoo_cost")
public class OdooCost extends AbstractEntity {

	private LocalDate date;

	private String moveId;

	private String name;

	private String productId;

	private String accountId;

	private BigDecimal debit;

	private BigDecimal credit;

	private BigDecimal balance;

	@ManyToOne
	@JoinColumn(name = "budget_entry_id")
	private BudgetEntry budgetEntry;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getMoveId() {
		return moveId;
	}

	public void setMoveId(String moveId) {
		this.moveId = moveId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public BigDecimal getDebit() {
		return debit;
	}

	public void setDebit(BigDecimal debit) {
		this.debit = debit;
	}

	public BigDecimal getCredit() {
		return credit;
	}

	public void setCredit(BigDecimal credit) {
		this.credit = credit;
	}

	public BigDecimal getBalance() {
		if (balance == null)
			return BigDecimal.ZERO;
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BudgetEntry getBudgetEntry() {
		return budgetEntry;
	}

	public void setBudgetEntry(BudgetEntry budgetEntry) {
		this.budgetEntry = budgetEntry;
	}
}
