package uy.com.bay.utiles.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import uy.com.bay.utiles.data.AlchemerSurveyResponse;

public interface AlchemerSurveyResponseRepository extends JpaRepository<AlchemerSurveyResponse, Long> {


    List<AlchemerSurveyResponse> findByDataResponseIdAndDataSurveyId(Long responseId, Integer surveyId);


}
