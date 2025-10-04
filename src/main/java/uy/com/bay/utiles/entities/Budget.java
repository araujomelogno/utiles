package uy.com.bay.utiles.entities;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import uy.com.bay.utiles.entities.BudgetEntry;

@Entity
public class Budget extends AbstractEntity {

    private LocalDate created;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetEntry> entries;

    @OneToOne
    private Study study;

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public List<BudgetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<BudgetEntry> entries) {
        this.entries = entries;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }
}