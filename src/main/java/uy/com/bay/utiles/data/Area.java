package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;

@Entity
public class Area extends AbstractEntity {

    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
