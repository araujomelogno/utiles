package uy.com.bay.utiles.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.enums.MatchType;

@Entity
public class BudgetConcept extends AbstractEntity {

    private String name;
    private String description;
    private String odooProductId;

    @Enumerated(EnumType.STRING)
    private MatchType matchType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOdooProductId() {
        return odooProductId;
    }

    public void setOdooProductId(String odooProductId) {
        this.odooProductId = odooProductId;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }
}