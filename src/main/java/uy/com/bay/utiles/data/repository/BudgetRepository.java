package uy.com.bay.utiles.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {

	@Query(value = "select b from Budget b left join fetch b.entries", countQuery = "select count(b) from Budget b")
	Page<Budget> findAllWithEntries(Pageable pageable);

	@Query("select b from Budget b left join fetch b.entries where b.id = :id")
	Optional<Budget> findByIdWithEntries(@Param("id") Long id);

	Optional<Budget> findByStudy(Study study);

	@Query("select b from Budget b left join fetch b.entries")
	List<Budget> findAllWithEntries();

	@Query("select b from Budget b left join fetch b.entries where b.study = :study")
	Optional<Budget> findByStudyWithEntries(@Param("study") Study study);
}