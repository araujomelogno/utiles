package uy.com.bay.utiles.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import uy.com.bay.utiles.entities.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {

	@Query(value = "select b from Budget b left join fetch b.entries", countQuery = "select count(b) from Budget b")
	Page<Budget> findAllWithEntries(Pageable pageable);

}