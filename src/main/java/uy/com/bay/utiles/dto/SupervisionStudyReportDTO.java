package uy.com.bay.utiles.dto;

import java.util.ArrayList;
import java.util.List;

import uy.com.bay.utiles.dto.SupervisionSummaryDTO.DimensionScores;

/**
 * Indicadores del dashboard "Reporte por proyecto" de la supervisión, calculados
 * para un proyecto concreto (alchemerStudyName) o para todos los proyectos cuando
 * el filtro es {@code null}. Se calcula en {@code SupervisionSummaryService}.
 */
public class SupervisionStudyReportDTO {

	/** Suma de encuestas completas (getCompleted de los fieldworks asociados). */
	private long completedSurveys;

	/** Cantidad de tareas de supervisión del proyecto. */
	private long supervisedSurveys;

	/** Cociente entre encuestas supervisadas y encuestas completas. */
	private double supervisionLevel;

	/** Promedio del puntaje global (aiScore). */
	private double globalScoreAverage;

	/** Promedio de encuestas supervisadas por encuestador. */
	private double averagePerSurveyor;

	/** Promedio de cada dimensión de calidad. */
	private DimensionScores dimensionScores = new DimensionScores(0, 0, 0, 0);

	/** Promedio del puntaje global por encuestador. */
	private List<SurveyorScore> scoreBySurveyor = new ArrayList<>();

	/** Puntaje global promedio de un encuestador. */
	public record SurveyorScore(String surveyor, double score) {
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

	public double getAveragePerSurveyor() {
		return averagePerSurveyor;
	}

	public void setAveragePerSurveyor(double averagePerSurveyor) {
		this.averagePerSurveyor = averagePerSurveyor;
	}

	public DimensionScores getDimensionScores() {
		return dimensionScores;
	}

	public void setDimensionScores(DimensionScores dimensionScores) {
		this.dimensionScores = dimensionScores;
	}

	public List<SurveyorScore> getScoreBySurveyor() {
		return scoreBySurveyor;
	}

	public void setScoreBySurveyor(List<SurveyorScore> scoreBySurveyor) {
		this.scoreBySurveyor = scoreBySurveyor;
	}
}
