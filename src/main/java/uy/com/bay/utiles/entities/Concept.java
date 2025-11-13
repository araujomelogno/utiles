package uy.com.bay.utiles.entities;

import jakarta.persistence.*;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.enums.ConceptType;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Concept extends AbstractEntity {

    private String name;
    private String description;
    @ElementCollection(targetClass = ConceptType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "concept_type")
    @Column(name = "type")
    private Set<ConceptType> type = new HashSet<>();

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

    public Set<ConceptType> getType() {
        return type;
    }

    public void setType(Set<ConceptType> type) {
        this.type = type;
    }
}
