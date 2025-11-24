package uy.com.bay.utiles.data.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.dto.CompletedSurveyDTO;

public interface AlchemerAnswerRepository
		extends JpaRepository<AlchemerAnswer, Long>, JpaSpecificationExecutor<AlchemerAnswer> {

	List<AlchemerAnswer> findByResponseIdAndSurveyId(Long responseId, Integer surveyId);

	Optional<AlchemerAnswer> findByAlchemerId(Long alchemerId);

	@Query("SELECT new uy.com.bay.utiles.dto.CompletedSurveyDTO(a.surveyor, a.studyName, a.created, COUNT(a)) " +
			"FROM AlchemerAnswer a " +
			"WHERE a.surveyor IN :surveyors AND a.created >= :startDate AND a.created <= :endDate " +
			"GROUP BY a.surveyor, a.studyName, a.created " +
			"ORDER BY a.created DESC, a.surveyor")
	List<CompletedSurveyDTO> findCompletedSurveys(@Param("surveyors") List<String> surveyors,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
