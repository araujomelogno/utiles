package uy.com.bay.utiles.dto;

import uy.com.bay.utiles.data.Status;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SupervisionTaskDTO {

    private Long id;
    private String fileName;
    private Status status;
    private double aiScore;
    private double totalAudioDuration;
    private double speakingDuration;
    private Map<String, Double> durationBySpeakers = new HashMap<>();
    private Date created;
    private String output;
    private String evaluationOutput;

    public SupervisionTaskDTO() {
    }

    public SupervisionTaskDTO(Long id, String fileName, Status status, double aiScore,
                              double totalAudioDuration, double speakingDuration,
                              Date created, String output, String evaluationOutput) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        this.aiScore = aiScore;
        this.totalAudioDuration = totalAudioDuration;
        this.speakingDuration = speakingDuration;
        this.created = created;
        this.output = output;
        this.evaluationOutput = evaluationOutput;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getAiScore() {
        return aiScore;
    }

    public void setAiScore(double aiScore) {
        this.aiScore = aiScore;
    }

    public double getTotalAudioDuration() {
        return totalAudioDuration;
    }

    public void setTotalAudioDuration(double totalAudioDuration) {
        this.totalAudioDuration = totalAudioDuration;
    }

    public double getSpeakingDuration() {
        return speakingDuration;
    }

    public void setSpeakingDuration(double speakingDuration) {
        this.speakingDuration = speakingDuration;
    }

    public Map<String, Double> getDurationBySpeakers() {
        return durationBySpeakers;
    }

    public void setDurationBySpeakers(Map<String, Double> durationBySpeakers) {
        this.durationBySpeakers = durationBySpeakers;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getEvaluationOutput() {
        return evaluationOutput;
    }

    public void setEvaluationOutput(String evaluationOutput) {
        this.evaluationOutput = evaluationOutput;
    }
}
