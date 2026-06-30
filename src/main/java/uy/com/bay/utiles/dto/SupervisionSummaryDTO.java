package uy.com.bay.utiles.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Indicadores agregados que alimentan el dashboard "Resumen ejecutivo" de la
 * supervisión. Todos los valores se calculan en {@code SupervisionSummaryService}
 * a partir de las entidades {@code SupervisionTask} y {@code Fieldwork}.
 */
public class SupervisionSummaryDTO {

	/** Cantidad de proyectos distintos (alchemerStudyName distintos). */
	private long projectCount;

	/** Suma de encuestas completas (getCompleted de los fieldworks asociados). */
	private long completedSurveys;

	/** Cantidad de tareas de supervisión. */
	private long supervisedSurveys;

	/** Cociente entre encuestas supervisadas y encuestas completas. */
	private double supervisionLevel;

	/** Promedio del puntaje global (aiScore). */
	private double globalScoreAverage;

	/** Promedio del puntaje global por mes (eje X = mes del audioDate). */
	private List<MonthScore> scoreEvolution = new ArrayList<>();

	/** Promedio de cada dimensión de calidad. */
	private DimensionScores dimensionScores = new DimensionScores(0, 0, 0, 0);

	/** Promedio del puntaje global por proyecto. */
	private List<StudyScore> scoreByStudy = new ArrayList<>();

	/** Puntaje global promedio de un mes (etiqueta legible del mes). */
	public record MonthScore(String label, double score) {
	}

	/** Puntaje global promedio de un proyecto. */
	public record StudyScore(String study, double score) {
	}

	/** Promedios de las cuatro dimensiones de calidad evaluadas. */
	public record DimensionScores(double cobertura, double fidelidad, double neutralidad, double fluidez) {
	}

	public long getProjectCount() {
		return projectCount;
	}

	public void setProjectCount(long projectCount) {
		this.projectCount = projectCount;
	}

	public long getCompletedSurveys() {
		return completedSurveys;
	}

	public void setCompletedSurveys(long completedSurveys) {
		this.completedSurveys = completedSurveys;
	}

	public long getSupervisedSurveys() {
		return supervisedSurveys;
	}

	public void setSupervisedSurveys(long supervisedSurveys) {
		this.supervisedSurveys = supervisedSurveys;
	}

	public double getSupervisionLevel() {
		return supervisionLevel;
	}

	public void setSupervisionLevel(double supervisionLevel) {
		this.supervisionLevel = supervisionLevel;
	}

	public double getGlobalScoreAverage() {
		return globalScoreAverage;
	}

	public void setGlobalScoreAverage(double globalScoreAverage) {
		this.globalScoreAverage = globalScoreAverage;
	}

	public List<MonthScore> getScoreEvolution() {
		return scoreEvolution;
	}

	public void setScoreEvolution(List<MonthScore> scoreEvolution) {
		this.scoreEvolution = scoreEvolution;
	}

	public DimensionScores getDimensionScores() {
		return dimensionScores;
	}

	public void setDimensionScores(DimensionScores dimensionScores) {
		this.dimensionScores = dimensionScores;
	}

	public List<StudyScore> getScoreByStudy() {
		return scoreByStudy;
	}

	public void setScoreByStudy(List<StudyScore> scoreByStudy) {
		this.scoreByStudy = scoreByStudy;
	}
}
