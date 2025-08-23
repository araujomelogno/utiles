package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SurveyorRepository extends JpaRepository<Surveyor, Long>, JpaSpecificationExecutor<Surveyor> {
    Optional<Surveyor> findBySurveyToGoId(String surveyToGoId);
    Optional<Surveyor> findByFirstName(String firstName);
    Optional<Surveyor> findByName(String name);
}
