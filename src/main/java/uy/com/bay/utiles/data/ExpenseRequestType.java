package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class ExpenseRequestType extends AbstractEntity {

    private String concept;
    private String description;

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
