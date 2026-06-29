package uy.com.bay.utiles.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.com.bay.utiles.data.AlchemerSurveyResponse;

public interface AlchemerSurveyResponseRepository extends JpaRepository<AlchemerSurveyResponse, Long> {


    List<AlchemerSurveyResponse> findByDataResponseIdAndDataSurveyId(Long responseId, Integer surveyId);

    @Modifying
    @Query("UPDATE AlchemerSurveyResponse a SET a.fieldwork = null WHERE a.fieldwork.id = :fieldworkId")
    void clearFieldwork(@Param("fieldworkId") Long fieldworkId);

}
