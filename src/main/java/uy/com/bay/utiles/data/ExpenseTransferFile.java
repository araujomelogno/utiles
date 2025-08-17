package uy.com.bay.utiles.data;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
public class ExpenseTransferFile extends AbstractEntity {

    private String name;
    private Date created;

    @Lob
    private byte[] content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_transfer_id")
    private ExpenseTransfer expenseTransfer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public ExpenseTransfer getExpenseTransfer() {
        return expenseTransfer;
    }

    public void setExpenseTransfer(ExpenseTransfer expenseTransfer) {
        this.expenseTransfer = expenseTransfer;
    }
}
