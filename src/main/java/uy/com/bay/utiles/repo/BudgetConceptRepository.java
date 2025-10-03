package uy.com.bay.utiles.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.entities.BudgetConcept;

public interface BudgetConceptRepository extends JpaRepository<BudgetConcept, Long>, JpaSpecificationExecutor<BudgetConcept> {
}