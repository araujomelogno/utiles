package uy.com.bay.utiles.data;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import uy.com.bay.utiles.entities.BudgetEntry;
@Entity
public class ExpenseRequest extends AbstractEntity {

    @ManyToOne
    private BudgetEntry budgetEntry;

    @ManyToOne
    private Study study;

    @ManyToOne
    private Surveyor surveyor;

    private Date requestDate;
    private Date aprovalDate;
    private Date transferDate;
    private Double amount;

    @ManyToOne
    private ExpenseRequestType concept;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus expenseStatus;

    private String obs;

    @ManyToOne
    private ExpenseTransfer expenseTransfer;

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public Surveyor getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(Surveyor surveyor) {
        this.surveyor = surveyor;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getAprovalDate() {
        return aprovalDate;
    }

    public void setAprovalDate(Date aprovalDate) {
        this.aprovalDate = aprovalDate;
    }

    public Date getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(Date transferDate) {
        this.transferDate = transferDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public ExpenseRequestType getConcept() {
        return concept;
    }

    public void setConcept(ExpenseRequestType concept) {
        this.concept = concept;
    }

    public ExpenseStatus getExpenseStatus() {
        return expenseStatus;
    }

    public void setExpenseStatus(ExpenseStatus expenseStatus) {
        this.expenseStatus = expenseStatus;
    }

    public ExpenseTransfer getExpenseTransfer() {
        return expenseTransfer;
    }

    public void setExpenseTransfer(ExpenseTransfer expenseTransfer) {
        this.expenseTransfer = expenseTransfer;
    }

    public BudgetEntry getBudgetEntry() {
        return budgetEntry;
    }

    public void setBudgetEntry(BudgetEntry budgetEntry) {
        this.budgetEntry = budgetEntry;
    }
}
