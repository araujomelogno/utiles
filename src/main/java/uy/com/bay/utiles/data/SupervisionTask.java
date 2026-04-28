package uy.com.bay.utiles.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;

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
	@Column(columnDefinition = "LONGTEXT")

	private String fullPrompt;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String output;

	private double totalAudioDuration;
	private double speakingDuration;

	@ElementCollection
	@CollectionTable(name = "supervision_task_speakers", joinColumns = @JoinColumn(name = "task_id"))
	@MapKeyColumn(name = "speaker")
	@Column(name = "duration")
	private Map<String, Double> durationBySpeakers = new HashMap<>();

	@ElementCollection
	@CollectionTable(name = "supervision_task_coincidence_by_item", joinColumns = @JoinColumn(name = "task_id"))
	@MapKeyColumn(name = "item_id")
	@Column(name = "coincidence")
	private Map<String, String> coincidenceByItem = new HashMap<>();

	@ElementCollection
	@CollectionTable(name = "supervision_task_score_by_item", joinColumns = @JoinColumn(name = "task_id"))
	@MapKeyColumn(name = "item_id")
	@Column(name = "score")
	private Map<String, Integer> scoreByItem = new HashMap<>();

	private double aiScore;

	private int scoreCobertura;
	private int scoreFidelidad;
	private int scoreNeutralidad;
	private int scoreFluidez;

	private Integer itemsEsperados;
	private Integer itemsFaltantes;
	private Integer itemsEncontrados;
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String problemasMayores;
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String problemasMenores;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String evaluationOutput;

	private Date processed;

	private String alchemerStudyName;

	private Integer alchemerSuerveyId;

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

	public String getAlchemerStudyName() {
		return alchemerStudyName;
	}

	public void setAlchemerStudyName(String alchemerStudyName) {
		this.alchemerStudyName = alchemerStudyName;
	}

	public Integer getAlchemerSuerveyId() {
		return alchemerSuerveyId;
	}

	public void setAlchemerSuerveyId(Integer alchemerSuerveyId) {
		this.alchemerSuerveyId = alchemerSuerveyId;
	}

	public double getScoreCobertura() {
		return scoreCobertura;
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

	public void setScoreCobertura(int scoreCobertura) {
		this.scoreCobertura = scoreCobertura;
	}

	public Integer getItemsFaltantes() {
		return itemsFaltantes;
	}

	public void setItemsFaltantes(Integer itemsFaltantes) {
		this.itemsFaltantes = itemsFaltantes;
	}

	public Map<String, String> getCoincidenceByItem() {
		return coincidenceByItem;
	}

	public void setCoincidenceByItem(Map<String, String> coincidenceByItem) {
		this.coincidenceByItem = coincidenceByItem;
	}

	public Map<String, Integer> getScoreByItem() {
		return scoreByItem;
	}

	public void setScoreByItem(Map<String, Integer> scoreByItem) {
		this.scoreByItem = scoreByItem;
	}
}