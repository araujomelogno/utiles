package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;

import java.util.Optional;

public interface AlchemerSurveyResponseRepository extends JpaRepository<AlchemerSurveyResponse, Long> {

    Optional<AlchemerSurveyResponse> findByDataResponseIdAndDataSurveyId(Long responseId, Integer surveyId);

}
