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
    private Study proyecto;

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
            data.setSurveyResponse(this);
        }
        this.data = data;
    }

    public Study getProyecto() {
        return proyecto;
    }

    public void setProyecto(Study proyecto) {
        this.proyecto = proyecto;
    }
}
