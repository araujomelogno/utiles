package uy.com.bay.utiles.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Indicadores del dashboard "Evolución temporal" de la supervisión, calculados
 * para un encuestador concreto (surveyor) o para todos cuando el filtro es
 * {@code null}. Todas las listas comparten el mismo eje X ({@link #months}); cada
 * lista de valores está alineada por índice con {@code months}. Se calcula en
 * {@code SupervisionSummaryService}.
 */
public class SupervisionTimelineReportDTO {

	/** Etiquetas de mes (eje X), en orden cronológico. */
	private List<String> months = new ArrayList<>();

	/** Promedio del puntaje global (aiScore) por mes. */
	private List<Double> globalScore = new ArrayList<>();

	/** Promedio de cobertura por mes. */
	private List<Double> cobertura = new ArrayList<>();

	/** Promedio de fidelidad por mes. */
	private List<Double> fidelidad = new ArrayList<>();

	/** Promedio de neutralidad por mes. */
	private List<Double> neutralidad = new ArrayList<>();

	/** Promedio de fluidez por mes. */
	private List<Double> fluidez = new ArrayList<>();

	public List<String> getMonths() {
		return months;
	}

	public void setMonths(List<String> months) {
		this.months = months;
	}

	public List<Double> getGlobalScore() {
		return globalScore;
	}

	public void setGlobalScore(List<Double> globalScore) {
		this.globalScore = globalScore;
	}

	public List<Double> getCobertura() {
		return cobertura;
	}

	public void setCobertura(List<Double> cobertura) {
		this.cobertura = cobertura;
	}

	public List<Double> getFidelidad() {
		return fidelidad;
	}

	public void setFidelidad(List<Double> fidelidad) {
		this.fidelidad = fidelidad;
	}

	public List<Double> getNeutralidad() {
		return neutralidad;
	}

	public void setNeutralidad(List<Double> neutralidad) {
		this.neutralidad = neutralidad;
	}

	public List<Double> getFluidez() {
		return fluidez;
	}

	public void setFluidez(List<Double> fluidez) {
		this.fluidez = fluidez;
	}
}
