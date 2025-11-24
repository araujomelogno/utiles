package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SurveyorRepository extends JpaRepository<Surveyor, Long>, JpaSpecificationExecutor<Surveyor> {
    Optional<Surveyor> findBySurveyToGoId(String surveyToGoId);
    Optional<Surveyor> findByFirstName(String firstName);
    @Query("SELECT s FROM Surveyor s WHERE s.firstName = :name")
    Optional<Surveyor> findByName(@Param("name") String name);
    Optional<Surveyor> findByLogin(String login);
}
