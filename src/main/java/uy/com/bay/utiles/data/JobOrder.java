package uy.com.bay.utiles.data;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class JobOrder extends AbstractEntity {

    private LocalDate created;

    private LocalDate init;

    private LocalDate end;

    @ManyToOne
    @JoinColumn(name = "study_id")
    private Study study;

    private Integer hours;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private Provider provider;

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public LocalDate getInit() {
        return init;
    }

    public void setInit(LocalDate init) {
        this.init = init;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
