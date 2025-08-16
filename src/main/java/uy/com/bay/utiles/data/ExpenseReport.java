package uy.com.bay.utiles.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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

    @OneToMany(mappedBy = "expenseReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ExpenseReportFile> files = new ArrayList<>();

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

    public List<ExpenseReportFile> getFiles() {
        return files;
    }

    public void setFiles(List<ExpenseReportFile> files) {
        this.files = files;
    }
}
