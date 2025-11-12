package uy.com.bay.utiles.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.com.bay.utiles.entities.BudgetEntry;

@Repository
public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

	@Query("SELECT be FROM BudgetEntry be LEFT JOIN FETCH be.extras LEFT JOIN FETCH be.fieldworks LEFT JOIN FETCH be.expenseRequests WHERE be.id = :id")
	Optional<BudgetEntry> findByIdWithExtras(@Param("id") Long id);
}