package uy.com.bay.utiles.data.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Tuple;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;

@Repository
public interface SupervisionTaskRepository extends JpaRepository<SupervisionTask, Long> {

	List<SupervisionTask> findByStatus(Status status);

	@Query("select t.id from SupervisionTask t where t.status = :status")
	List<Long> findIdsByStatus(@Param("status") Status status);

	@Query("SELECT DISTINCT st FROM SupervisionTask st LEFT JOIN FETCH st.durationBySpeakers WHERE st.created BETWEEN :from AND :to "
			+ "AND (:fileName IS NULL OR :fileName = '' OR lower(st.fileName) LIKE lower(concat('%', :fileName, '%'))) "
			+ "AND (:status IS NULL OR st.status = :status) " + "ORDER BY st.created DESC")
	List<SupervisionTask> findByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to,
			@Param("fileName") String fileName, @Param("status") Status status);

	@Query("SELECT st.id as id, st.fileName as fileName, st.status as status, st.aiScore as aiScore, "
			+ "st.totalAudioDuration as totalAudioDuration, st.speakingDuration as speakingDuration, "
			+ "st.created as created, st.output as output, st.evaluationOutput as evaluationOutput, "
			+ "KEY(ds) as speaker, VALUE(ds) as duration, st.scoreCobertura as scoreCobertura, "
			+ "st.scoreFidelidad as scoreFidelidad,  st.scoreNeutralidad as scoreNeutralidad,"
			+ "st.scoreFluidez as scoreFluidez, st.itemsEsperados as itemsEsperados, "
			+ "st.itemsFaltantes as itemsFaltantes, st.itemsEncontrados as itemsEncontrados, "
			+ "st.problemasMayores as problemasMayores, st.problemasMenores as problemasMenores, "
			+ "st.alchemerStudyName as alchemerStudyName "
			+ "FROM SupervisionTask st LEFT JOIN st.durationBySpeakers ds " + "WHERE st.created BETWEEN :from AND :to "
			+ "AND (:fileName IS NULL OR :fileName = '' OR lower(st.fileName) LIKE lower(concat('%', :fileName, '%'))) "
			+ "AND (:alchemerStudyName IS NULL OR :alchemerStudyName = '' OR lower(st.alchemerStudyName) LIKE lower(concat('%', :alchemerStudyName, '%'))) "
			+ "AND (:status IS NULL OR st.status = :status) " + "ORDER BY st.created DESC")
	List<Tuple> findTuplesByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to,
			@Param("fileName") String fileName, @Param("alchemerStudyName") String alchemerStudyName,
			@Param("status") Status status);

	@Query("SELECT st.id as id, KEY(c) as itemId, VALUE(c) as coincidence "
			+ "FROM SupervisionTask st JOIN st.coincidenceByItem c WHERE st.id IN :ids")
	List<Tuple> findCoincidenceByItemForIds(@Param("ids") Collection<Long> ids);

	@Query("SELECT st.id as id, KEY(s) as itemId, VALUE(s) as score "
			+ "FROM SupervisionTask st JOIN st.scoreByItem s WHERE st.id IN :ids")
	List<Tuple> findScoreByItemForIds(@Param("ids") Collection<Long> ids);

	@Query("SELECT st.audioContent FROM SupervisionTask st WHERE st.id = :id")
	byte[] findAudioContentById(@Param("id") Long id);

	/** Cantidad de proyectos distintos (valores distintos de alchemerStudyName). */
	@Query("SELECT COUNT(DISTINCT st.alchemerStudyName) FROM SupervisionTask st WHERE st.alchemerStudyName IS NOT NULL")
	long countDistinctAlchemerStudyName();

	/** Identificadores de encuesta (alchemerSuerveyId) distintos presentes en las tareas. */
	@Query("SELECT DISTINCT st.alchemerSuerveyId FROM SupervisionTask st WHERE st.alchemerSuerveyId IS NOT NULL")
	List<Integer> findDistinctAlchemerSuerveyIds();

	/** Promedio del puntaje global (aiScore) sobre todas las tareas. */
	@Query("SELECT AVG(st.aiScore) FROM SupervisionTask st")
	Double averageAiScore();

	/** Proyección de fecha de audio y puntaje, para agrupar la evolución por mes. */
	@Query("SELECT st.audioDate as audioDate, st.aiScore as aiScore FROM SupervisionTask st WHERE st.audioDate IS NOT NULL")
	List<Tuple> findAudioDateAndAiScore();

	/** Promedio de cada dimensión de calidad (para el gráfico de radar). */
	@Query("SELECT AVG(st.scoreCobertura) as cobertura, AVG(st.scoreFidelidad) as fidelidad, "
			+ "AVG(st.scoreNeutralidad) as neutralidad, AVG(st.scoreFluidez) as fluidez FROM SupervisionTask st")
	Tuple findAverageDimensionScores();

	/** Promedio del puntaje global agrupado por proyecto (alchemerStudyName). */
	@Query("SELECT st.alchemerStudyName as studyName, AVG(st.aiScore) as avgScore FROM SupervisionTask st "
			+ "WHERE st.alchemerStudyName IS NOT NULL GROUP BY st.alchemerStudyName ORDER BY st.alchemerStudyName")
	List<Tuple> findAverageAiScoreByStudy();

	// ---- Reporte por proyecto (filtrado por alchemerStudyName; null = todos) ----

	/** Nombres de proyecto (alchemerStudyName) distintos, para el combobox. */
	@Query("SELECT DISTINCT st.alchemerStudyName FROM SupervisionTask st WHERE st.alchemerStudyName IS NOT NULL "
			+ "ORDER BY st.alchemerStudyName")
	List<String> findDistinctAlchemerStudyNames();

	/** Identificadores de encuesta distintos del proyecto seleccionado (null = todos). */
	@Query("SELECT DISTINCT st.alchemerSuerveyId FROM SupervisionTask st WHERE st.alchemerSuerveyId IS NOT NULL "
			+ "AND (:studyName IS NULL OR st.alchemerStudyName = :studyName)")
	List<Integer> findDistinctAlchemerSuerveyIdsByStudy(@Param("studyName") String studyName);

	/** Cantidad de tareas de supervisión del proyecto seleccionado (null = todos). */
	@Query("SELECT COUNT(st) FROM SupervisionTask st WHERE (:studyName IS NULL OR st.alchemerStudyName = :studyName)")
	long countByStudy(@Param("studyName") String studyName);

	/** Promedio del puntaje global del proyecto seleccionado (null = todos). */
	@Query("SELECT AVG(st.aiScore) FROM SupervisionTask st WHERE (:studyName IS NULL OR st.alchemerStudyName = :studyName)")
	Double averageAiScoreByStudy(@Param("studyName") String studyName);

	/** Cantidad de encuestadores distintos del proyecto seleccionado (null = todos). */
	@Query("SELECT COUNT(DISTINCT st.surveyor) FROM SupervisionTask st WHERE st.surveyor IS NOT NULL "
			+ "AND (:studyName IS NULL OR st.alchemerStudyName = :studyName)")
	long countDistinctSurveyorByStudy(@Param("studyName") String studyName);

	/** Promedio de cada dimensión de calidad del proyecto seleccionado (null = todos). */
	@Query("SELECT AVG(st.scoreCobertura) as cobertura, AVG(st.scoreFidelidad) as fidelidad, "
			+ "AVG(st.scoreNeutralidad) as neutralidad, AVG(st.scoreFluidez) as fluidez FROM SupervisionTask st "
			+ "WHERE (:studyName IS NULL OR st.alchemerStudyName = :studyName)")
	Tuple findAverageDimensionScoresByStudy(@Param("studyName") String studyName);

	/** Promedio del puntaje global agrupado por encuestador del proyecto seleccionado (null = todos). */
	@Query("SELECT st.surveyor as surveyor, AVG(st.aiScore) as avgScore FROM SupervisionTask st "
			+ "WHERE st.surveyor IS NOT NULL AND (:studyName IS NULL OR st.alchemerStudyName = :studyName) "
			+ "GROUP BY st.surveyor ORDER BY st.surveyor")
	List<Tuple> findAverageAiScoreBySurveyor(@Param("studyName") String studyName);
}
