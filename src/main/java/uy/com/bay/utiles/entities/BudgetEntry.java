package uy.com.bay.utiles.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import uy.com.bay.utiles.data.AbstractEntity;

@Entity
public class BudgetEntry extends AbstractEntity {

    private Double ammount;
    private Integer quantity;
    private Double spent;

    @ManyToOne
    @JoinColumn(name = "budget_concept_id")
    private BudgetConcept concept;

    public Double getAmmount() {
        return ammount;
    }

    public void setAmmount(Double ammount) {
        this.ammount = ammount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getSpent() {
        return spent;
    }

    public void setSpent(Double spent) {
        this.spent = spent;
    }

    public BudgetConcept getConcept() {
        return concept;
    }

    public void setConcept(BudgetConcept concept) {
        this.concept = concept;
    }

    public String getConceptName() {
        return concept != null ? concept.getName() : "";
    }
}