package uy.com.bay.utiles.dto;

import java.util.ArrayList;
import java.util.List;

import uy.com.bay.utiles.dto.SupervisionStudyReportDTO.SurveyorScore;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.DimensionScores;

/**
 * Indicadores del dashboard "Resultados por encuestador" de la supervisión: el
 * ranking de encuestadores por puntaje global promedio y el perfil por dimensión
 * del mejor encuestador frente al que requiere apoyo. Se calcula en
 * {@code SupervisionSummaryService}.
 */
public class SupervisionSurveyorReportDTO {

	/** Puntaje global promedio por encuestador (ranking). */
	private List<SurveyorScore> ranking = new ArrayList<>();

	/** Encuestador con mayor puntaje global promedio. */
	private SurveyorProfile best;

	/** Encuestador con menor puntaje global promedio. */
	private SurveyorProfile worst;

	/** Perfil de un encuestador: nombre y promedios por dimensión. */
	public record SurveyorProfile(String surveyor, DimensionScores dimensions) {
	}

	public List<SurveyorScore> getRanking() {
		return ranking;
	}

	public void setRanking(List<SurveyorScore> ranking) {
		this.ranking = ranking;
	}

	public SurveyorProfile getBest() {
		return best;
	}

	public void setBest(SurveyorProfile best) {
		this.best = best;
	}

	public SurveyorProfile getWorst() {
		return worst;
	}

	public void setWorst(SurveyorProfile worst) {
		this.worst = worst;
	}
}
