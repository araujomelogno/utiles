package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import java.util.Date;

@Entity
public class SupervisionTask extends AbstractEntity {

    private Date created;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String fileName;

    @Lob
    private byte[] audioContent;

    @Lob
    private byte[] questionnaire;

    @Lob
    private String input;

    @Lob
    private String output;

    private Date processed;

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getAudioContent() {
        return audioContent;
    }

    public void setAudioContent(byte[] audioContent) {
        this.audioContent = audioContent;
    }

    public byte[] getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(byte[] questionnaire) {
        this.questionnaire = questionnaire;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Date getProcessed() {
        return processed;
    }

    public void setProcessed(Date processed) {
        this.processed = processed;
    }
}
