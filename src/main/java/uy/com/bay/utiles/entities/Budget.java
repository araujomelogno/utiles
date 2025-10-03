package uy.com.bay.utiles.entities;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.data.Study;

@Entity
public class Budget extends AbstractEntity {

    private Date created;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<BudgetEntry> entries = new ArrayList<>();

    @OneToOne
    private Study study;

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
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