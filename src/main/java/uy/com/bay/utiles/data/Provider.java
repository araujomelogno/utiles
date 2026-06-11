package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Provider extends AbstractEntity {

    private String name;

    private Integer monthlyCapacity;

    @Enumerated(EnumType.STRING)
    private ProviderType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMonthlyCapacity() {
        return monthlyCapacity;
    }

    public void setMonthlyCapacity(Integer monthlyCapacity) {
        this.monthlyCapacity = monthlyCapacity;
    }

    public ProviderType getType() {
        return type;
    }

    public void setType(ProviderType type) {
        this.type = type;
    }
}
