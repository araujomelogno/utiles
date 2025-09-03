package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Date;

@Entity
public class DoobloResponse extends AbstractEntity {

    private String interviewId;

    @ManyToOne
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    @ManyToOne
    @JoinColumn(name = "fieldwork_id")
    private Fieldwork fieldwork;

    private Date date;

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public Surveyor getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(Surveyor surveyor) {
        this.surveyor = surveyor;
    }

    public Fieldwork getFieldwork() {
        return fieldwork;
    }

    public void setFieldwork(Fieldwork fieldwork) {
        this.fieldwork = fieldwork;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
