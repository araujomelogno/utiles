package uy.com.bay.utiles.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import uy.com.bay.utiles.data.Status;

public class SupervisionTaskDTO {

	private Long id;
	private String fileName;
	private Status status;
	private Double aiScore;
	private Double totalAudioDuration;
	private Double speakingDuration;
	private Map<String, Double> durationBySpeakers = new HashMap<>();
	private Date created;
	private String output;
	private String evaluationOutput;

	private int scoreCobertura;
	private int scoreFidelidad;
	private int scoreNeutralidad;
	private int scoreFluidez;

	private Integer itemsEsperados;
	private Integer itemsFaltantes;
	private Integer itemsEncontrados;
	private String problemasMayores;
	private String problemasMenores;
	
	
	private String alchemerStudyName;

	public SupervisionTaskDTO() {
	}

	public SupervisionTaskDTO(Long id, String fileName, Status status, Double aiScore, Double totalAudioDuration,
			Double speakingDuration, Date created, String output, String evaluationOutput) {
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

	public Double getAiScore() {
		return aiScore;
	}

	public void setAiScore(Double aiScore) {
		this.aiScore = aiScore;
	}

	public Double getTotalAudioDuration() {
		return totalAudioDuration;
	}

	public void setTotalAudioDuration(Double totalAudioDuration) {
		this.totalAudioDuration = totalAudioDuration;
	}

	public Double getSpeakingDuration() {
		return speakingDuration;
	}

	public void setSpeakingDuration(Double speakingDuration) {
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

	public int getScoreCobertura() {
		return scoreCobertura;
	}

	public void setScoreCobertura(int scoreCobertura) {
		this.scoreCobertura = scoreCobertura;
	}

	public int getScoreFidelidad() {
		return scoreFidelidad;
	}

	public void setScoreFidelidad(int scoreFidelidad) {
		this.scoreFidelidad = scoreFidelidad;
	}

	public int getScoreNeutralidad() {
		return scoreNeutralidad;
	}

	public void setScoreNeutralidad(int scoreNeutralidad) {
		this.scoreNeutralidad = scoreNeutralidad;
	}

	public int getScoreFluidez() {
		return scoreFluidez;
	}

	public void setScoreFluidez(int scoreFluidez) {
		this.scoreFluidez = scoreFluidez;
	}

	public Integer getItemsEsperados() {
		return itemsEsperados;
	}

	public void setItemsEsperados(Integer itemsEsperados) {
		this.itemsEsperados = itemsEsperados;
	}

	public Integer getItemsFaltantes() {
		return itemsFaltantes;
	}

	public void setItemsFaltantes(Integer itemsFaltantes) {
		this.itemsFaltantes = itemsFaltantes;
	}

	public Integer getItemsEncontrados() {
		return itemsEncontrados;
	}

	public void setItemsEncontrados(Integer itemsEncontrados) {
		this.itemsEncontrados = itemsEncontrados;
	}

	public String getProblemasMayores() {
		return problemasMayores;
	}

	public void setProblemasMayores(String problemasMayores) {
		this.problemasMayores = problemasMayores;
	}

	public String getProblemasMenores() {
		return problemasMenores;
	}

	public void setProblemasMenores(String problemasMenores) {
		this.problemasMenores = problemasMenores;
	}

	public String getAlchemerStudyName() {
		return alchemerStudyName;
	}

	public void setAlchemerStudyName(String alchemerStudyName) {
		this.alchemerStudyName = alchemerStudyName;
	}
}
