package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.AlchemerSurveyResponseData;

public interface AlchemerSurveyResponseDataRepository extends JpaRepository<AlchemerSurveyResponseData, Integer> {
    long countBySurveyId(int surveyId);
}
