package uy.com.bay.utiles.dto;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Completos obtenidos de Alchemer/Dooblo, agrupados por mes (clave: primer dia
 * del mes) y por dia (clave: el dia a las 00:00).
 */
public class CompletedSurveysCount {

	private final Map<Date, Integer> byMonth = new TreeMap<>();
	private final Map<Date, Integer> byDay = new TreeMap<>();

	public Map<Date, Integer> getByMonth() {
		return byMonth;
	}

	public Map<Date, Integer> getByDay() {
		return byDay;
	}

	/** Suma los completos de {@code other} (mes a mes y dia a dia). */
	public void merge(CompletedSurveysCount other) {
		if (other == null) {
			return;
		}
		mergeInto(byMonth, other.byMonth);
		mergeInto(byDay, other.byDay);
	}

	private static void mergeInto(Map<Date, Integer> target, Map<Date, Integer> source) {
		for (Map.Entry<Date, Integer> entry : source.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}
			target.merge(entry.getKey(), entry.getValue() == null ? 0 : entry.getValue(), Integer::sum);
		}
	}
}
