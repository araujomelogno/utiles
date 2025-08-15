package uy.com.bay.utiles.data;

import java.util.Date;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class ExpenseReport extends AbstractEntity {

    @ManyToOne
    private Study study;

    @ManyToOne
    private Surveyor surveyor;

    private Date date;
    private Double amount;

    @ManyToOne
    private ExpenseRequestType concept;

    @Enumerated(EnumType.STRING)
    private ExpenseStatus expenseStatus;

    @ElementCollection
    private List<String> files;

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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}
