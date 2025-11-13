package uy.com.bay.utiles.entities;

import jakarta.persistence.*;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.enums.ConceptType;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Concept extends AbstractEntity {

    private String name;
    private String description;
    @ElementCollection(targetClass = ConceptType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "concept_type")
    @Column(name = "type")
    private List<ConceptType> type = new ArrayList<>();

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

    public List<ConceptType> getType() {
        return type;
    }

    public void setType(List<ConceptType> type) {
        this.type = type;
    }
}
