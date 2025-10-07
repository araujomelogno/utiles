package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.entities.Budget;

import java.util.Optional;

import java.util.List;
import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long>, JpaSpecificationExecutor<Study> {

    Optional<Study> findByBudget(Budget budget);

    List<Study> findAllByShowSurveyor(boolean showSurveyor);

}
