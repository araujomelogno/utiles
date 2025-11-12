package uy.com.bay.utiles.entities;

import jakarta.persistence.Entity;
import uy.com.bay.utiles.data.AbstractEntity;

@Entity
public class Concept extends AbstractEntity {

    private String name;
    private String description;

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
}
