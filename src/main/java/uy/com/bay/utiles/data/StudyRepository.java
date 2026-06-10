package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.bay.utiles.entities.Budget;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long>, JpaSpecificationExecutor<Study> {

    Optional<Study> findByBudget(Budget budget);

    List<Study> findAllByShowSurveyor(boolean showSurveyor);

    Optional<Study> findByOdooId(String odooId);

    Optional<Study> findFirstByNameStartingWith(String prefix);

    Optional<Study> findByName(String name);

    @Query("SELECT DISTINCT s FROM Study s JOIN s.metaCostByDate m WHERE KEY(m) BETWEEN :from AND :to")
    List<Study> findAllWithMetaCostBetween(@Param("from") Date from, @Param("to") Date to);

}
