package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SurveyorRepository extends JpaRepository<Test, Long>, JpaSpecificationExecutor<Test> {

}
