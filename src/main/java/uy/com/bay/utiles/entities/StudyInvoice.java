package uy.com.bay.utiles.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.data.Study;

@Entity
@Table(name = "study_invoice", uniqueConstraints = @UniqueConstraint(name = "uk_study_invoice_move_id", columnNames = "move_id"))
public class StudyInvoice extends AbstractEntity {

	@Column(name = "move_id", unique = true)
	private String moveId;

	private LocalDate invoiceDate;

	private Double amountUntaxed;

	private Double tax;

	private Double amountTotal;
	
	private Double totalSigned;

	private String currency;
	

	@ManyToOne
	@JoinColumn(name = "study_id")
	private Study study;

	public String getMoveId() {
		return moveId;
	}

	public void setMoveId(String moveId) {
		this.moveId = moveId;
	}

	public LocalDate getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(LocalDate invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public Double getAmountUntaxed() {
		return amountUntaxed;
	}

	public void setAmountUntaxed(Double amountUntaxed) {
		this.amountUntaxed = amountUntaxed;
	}

	public Double getTax() {
		return tax;
	}

	public void setTax(Double tax) {
		this.tax = tax;
	}

	public Double getAmountTotal() {
		return amountTotal;
	}

	public void setAmountTotal(Double amountTotal) {
		this.amountTotal = amountTotal;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public Double getTotalSigned() {
		return totalSigned;
	}

	public void setTotalSigned(Double totalSigned) {
		this.totalSigned = totalSigned;
	}
}
