package uy.com.bay.utiles.data.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Tuple;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.SupervisionStudyReportDTO;
import uy.com.bay.utiles.dto.SupervisionStudyReportDTO.SurveyorScore;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.DimensionScores;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.MonthScore;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.StudyScore;

/**
 * Calcula los indicadores del dashboard "Resumen ejecutivo" combinando las
 * tareas de supervisión ({@link SupervisionTaskRepository}) y los trabajos de
 * campo ({@link FieldworkRepository}).
 */
@Service
public class SupervisionSummaryService {

	private static final String[] MONTH_NAMES = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct",
			"Nov", "Dic" };

	private final SupervisionTaskRepository supervisionTaskRepository;
	private final FieldworkRepository fieldworkRepository;

	public SupervisionSummaryService(SupervisionTaskRepository supervisionTaskRepository,
			FieldworkRepository fieldworkRepository) {
		this.supervisionTaskRepository = supervisionTaskRepository;
		this.fieldworkRepository = fieldworkRepository;
	}

	@Transactional(readOnly = true)
	public SupervisionSummaryDTO computeSummary() {
		SupervisionSummaryDTO dto = new SupervisionSummaryDTO();

		// Proyectos: cantidad de valores distintos de alchemerStudyName.
		dto.setProjectCount(supervisionTaskRepository.countDistinctAlchemerStudyName());

		// Encuestas supervisadas: cantidad de tareas de supervisión.
		long supervised = supervisionTaskRepository.count();
		dto.setSupervisedSurveys(supervised);

		// Encuestas completas: a partir de los alchemerSuerveyId distintos de las
		// tareas se recuperan los fieldworks que los referencian y se suma getCompleted.
		long completed = computeCompletedSurveys(supervisionTaskRepository.findDistinctAlchemerSuerveyIds());
		dto.setCompletedSurveys(completed);

		// Nivel de supervisión: cociente entre supervisadas y completas.
		dto.setSupervisionLevel(completed > 0 ? (double) supervised / completed : 0d);

		// Puntaje global promedio: promedio de aiScore.
		Double avg = supervisionTaskRepository.averageAiScore();
		dto.setGlobalScoreAverage(avg != null ? avg : 0d);

		// Evolución del puntaje global por mes del audioDate.
		dto.setScoreEvolution(computeScoreEvolution());

		// Perfil de calidad por dimensión (promedios).
		dto.setDimensionScores(computeDimensionScores());

		// Puntaje global por proyecto.
		dto.setScoreByStudy(computeScoreByStudy());

		return dto;
	}

	/** Nombres de proyecto distintos para alimentar el combobox del reporte. */
	@Transactional(readOnly = true)
	public List<String> findStudyNames() {
		return supervisionTaskRepository.findDistinctAlchemerStudyNames();
	}

	/**
	 * Indicadores del reporte por proyecto. Cuando {@code studyName} es {@code null}
	 * se consideran todas las tareas de supervisión ("Todos los estudios").
	 */
	@Transactional(readOnly = true)
	public SupervisionStudyReportDTO computeStudyReport(String studyName) {
		SupervisionStudyReportDTO dto = new SupervisionStudyReportDTO();

		// Encuestas supervisadas: cantidad de tareas del proyecto.
		long supervised = supervisionTaskRepository.countByStudy(studyName);
		dto.setSupervisedSurveys(supervised);

		// Encuestas completas: suma de getCompleted de los fieldworks asociados a los
		// alchemerSuerveyId distintos del proyecto.
		long completed = computeCompletedSurveys(
				supervisionTaskRepository.findDistinctAlchemerSuerveyIdsByStudy(studyName));
		dto.setCompletedSurveys(completed);

		// Nivel de supervisión: cociente entre supervisadas y completas.
		dto.setSupervisionLevel(completed > 0 ? (double) supervised / completed : 0d);

		// Puntaje global promedio del proyecto.
		Double avg = supervisionTaskRepository.averageAiScoreByStudy(studyName);
		dto.setGlobalScoreAverage(avg != null ? avg : 0d);

		// Promedio por encuestador: supervisadas / encuestadores distintos.
		long surveyors = supervisionTaskRepository.countDistinctSurveyorByStudy(studyName);
		dto.setAveragePerSurveyor(surveyors > 0 ? (double) supervised / surveyors : 0d);

		// Dimensiones del proyecto (promedios).
		dto.setDimensionScores(toDimensionScores(supervisionTaskRepository.findAverageDimensionScoresByStudy(studyName)));

		// Puntaje global por encuestador.
		dto.setScoreBySurveyor(computeScoreBySurveyor(studyName));

		return dto;
	}

	private List<SurveyorScore> computeScoreBySurveyor(String studyName) {
		List<SurveyorScore> scores = new ArrayList<>();
		for (Tuple tuple : supervisionTaskRepository.findAverageAiScoreBySurveyor(studyName)) {
			String surveyor = tuple.get("surveyor", String.class);
			Double average = tuple.get("avgScore", Double.class);
			scores.add(new SurveyorScore(surveyor, round1(average != null ? average : 0d)));
		}
		return scores;
	}

	private long computeCompletedSurveys(List<Integer> surveyIds) {
		if (surveyIds.isEmpty()) {
			return 0;
		}
		List<String> surveyIdStrings = surveyIds.stream().filter(id -> id != null).map(String::valueOf)
				.collect(Collectors.toList());
		if (surveyIdStrings.isEmpty()) {
			return 0;
		}
		List<Fieldwork> fieldworks = fieldworkRepository.findDistinctByAlchemerIdIn(surveyIdStrings);
		long completed = 0;
		for (Fieldwork fieldwork : fieldworks) {
			Integer value = fieldwork.getCompleted();
			if (value != null) {
				completed += value;
			}
		}
		return completed;
	}

	private List<MonthScore> computeScoreEvolution() {
		// Agrupado cronológicamente por año-mes para conservar el orden del eje X.
		Map<YearMonth, double[]> byMonth = new TreeMap<>();
		for (Tuple tuple : supervisionTaskRepository.findAudioDateAndAiScore()) {
			Date audioDate = tuple.get("audioDate", Date.class);
			Double score = tuple.get("aiScore", Double.class);
			if (audioDate == null) {
				continue;
			}
			YearMonth month = YearMonth
					.from(audioDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			double[] acc = byMonth.computeIfAbsent(month, k -> new double[2]);
			acc[0] += score != null ? score : 0d;
			acc[1] += 1;
		}

		List<MonthScore> evolution = new ArrayList<>();
		for (Map.Entry<YearMonth, double[]> entry : byMonth.entrySet()) {
			double[] acc = entry.getValue();
			double average = acc[1] > 0 ? acc[0] / acc[1] : 0d;
			evolution.add(new MonthScore(monthLabel(entry.getKey()), round1(average)));
		}
		return evolution;
	}

	private DimensionScores computeDimensionScores() {
		return toDimensionScores(supervisionTaskRepository.findAverageDimensionScores());
	}

	private DimensionScores toDimensionScores(Tuple tuple) {
		if (tuple == null) {
			return new DimensionScores(0, 0, 0, 0);
		}
		return new DimensionScores(round1(value(tuple, "cobertura")), round1(value(tuple, "fidelidad")),
				round1(value(tuple, "neutralidad")), round1(value(tuple, "fluidez")));
	}

	private List<StudyScore> computeScoreByStudy() {
		List<StudyScore> scores = new ArrayList<>();
		for (Tuple tuple : supervisionTaskRepository.findAverageAiScoreByStudy()) {
			String study = tuple.get("studyName", String.class);
			Double average = tuple.get("avgScore", Double.class);
			scores.add(new StudyScore(study, round1(average != null ? average : 0d)));
		}
		return scores;
	}

	private double value(Tuple tuple, String alias) {
		Double v = tuple.get(alias, Double.class);
		return v != null ? v : 0d;
	}

	private String monthLabel(YearMonth month) {
		return MONTH_NAMES[month.getMonthValue() - 1] + " " + month.getYear();
	}

	private static double round1(double value) {
		return Math.round(value * 10d) / 10d;
	}
}
