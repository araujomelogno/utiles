package uy.com.bay.utiles.data;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class ExpenseTransfer extends AbstractEntity {

    private Date transferDate;
    private Double amount;

    @OneToMany
    private List<ExpenseRequest> expenseRequests;

    @OneToMany(mappedBy = "expenseTransfer", cascade = CascadeType.ALL)
    private List<ExpenseTransferFile> files;

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

    public List<ExpenseRequest> getExpenseRequests() {
        return expenseRequests;
    }

    public void setExpenseRequests(List<ExpenseRequest> expenseRequests) {
        this.expenseRequests = expenseRequests;
    }

    public List<ExpenseTransferFile> getFiles() {
        return files;
    }

    public void setFiles(List<ExpenseTransferFile> files) {
        this.files = files;
    }
}
