package uy.com.bay.utiles.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import uy.com.bay.utiles.data.AlchemerContact;

public interface AlchemerContactRepository extends JpaRepository<AlchemerContact, Long> {

    List<AlchemerContact> findByInviteCustom1AndSurveyResponseDataSurveyId(String inviteCustom1, int surveyId);
}
