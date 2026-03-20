package uy.com.bay.utiles.data;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

	private String questionnaireFileName;

	@Lob
	private String fullPrompt;

	@Lob
	private String output;

	private double totalAudioDuration;
	private double speakingDuration;

	@ElementCollection
	@CollectionTable(name = "supervision_task_speakers", joinColumns = @JoinColumn(name = "task_id"))
	@MapKeyColumn(name = "speaker")
	@Column(name = "duration")
	private Map<String, Double> durationBySpeakers = new HashMap<>();

	private double aiScore;

	@ManyToOne
	@JoinColumn(name = "fieldwork_id")
	private Fieldwork fieldwork;

	@ManyToOne
	@JoinColumn(name = "study_id")
	private Study study;

	@Lob
	private String evaluationOutput;

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

	public String getQuestionnaireFileName() {
		return questionnaireFileName;
	}

	public void setQuestionnaireFileName(String questionnaireFileName) {
		this.questionnaireFileName = questionnaireFileName;
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

	public double getTotalAudioDuration() {
		return totalAudioDuration;
	}

	public void setTotalAudioDuration(double totalAudioDuration) {
		this.totalAudioDuration = totalAudioDuration;
	}

	public Double getSpeakingDuration() {
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

	public double getAiScore() {
		return aiScore;
	}

	public void setAiScore(double aiScore) {
		this.aiScore = aiScore;
	}

	public Fieldwork getFieldwork() {
		return fieldwork;
	}

	public void setFieldwork(Fieldwork fieldwork) {
		this.fieldwork = fieldwork;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public String getEvaluationOutput() {
		return evaluationOutput;
	}

	public void setEvaluationOutput(String evaluationOutput) {
		this.evaluationOutput = evaluationOutput;
	}

	public String getFullPrompt() {
		return fullPrompt;
	}

	public void setFullPrompt(String fullPrompt) {
		this.fullPrompt = fullPrompt;
	}
}