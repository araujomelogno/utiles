package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Date;

@Entity
public class CodingInstruction extends AbstractEntity {

    private Date created;
    private String variableName;
    private String question;
    private String fineTuning;

    @ManyToOne
    @JoinColumn(name = "encoding_task_id")
    private EncodingTask encodingTask;

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getFineTuning() {
        return fineTuning;
    }

    public void setFineTuning(String fineTuning) {
        this.fineTuning = fineTuning;
    }

    public EncodingTask getEncodingTask() {
        return encodingTask;
    }

    public void setEncodingTask(EncodingTask encodingTask) {
        this.encodingTask = encodingTask;
    }
}
