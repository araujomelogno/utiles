package uy.com.bay.utiles.data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class ExpenseTransfer extends AbstractEntity {

    private LocalDate transferDate;
    private Double amount;

    @ManyToOne
    private Surveyor surveyor;

    @OneToMany(mappedBy = "expenseTransfer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<ExpenseRequest> expenseRequests = new HashSet<>();

    @OneToMany(mappedBy = "expenseTransfer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ExpenseTransferFile> files;

    private String obs;

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Set<ExpenseRequest> getExpenseRequests() {
        return expenseRequests;
    }

    public void setExpenseRequests(Set<ExpenseRequest> expenseRequests) {
        this.expenseRequests = expenseRequests;
    }

    public List<ExpenseTransferFile> getFiles() {
        return files;
    }

    public void setFiles(List<ExpenseTransferFile> files) {
        this.files = files;
    }

    public Surveyor getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(Surveyor surveyor) {
        this.surveyor = surveyor;
    }
}
