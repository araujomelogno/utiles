package uy.com.bay.utiles.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alchemer_survey_response")
public class AlchemerSurveyResponse extends AbstractEntity {

    @JsonProperty("webhook_name")
    private String webhookName;

    @OneToOne(cascade = CascadeType.ALL)
    private AlchemerSurveyResponseData data;

    @ManyToOne
    private Proyecto proyecto;

    public String getWebhookName() {
        return webhookName;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public AlchemerSurveyResponseData getData() {
        return data;
    }

    public void setData(AlchemerSurveyResponseData data) {
        if (data != null) {
            data.setContact(data.getContact());
        }
        this.data = data;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }
}
