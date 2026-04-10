package uy.com.bay.utiles.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.com.bay.utiles.entities.BudgetEntry;

@Repository
public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {

	@Query("SELECT be FROM BudgetEntry be LEFT JOIN FETCH be.extras LEFT JOIN FETCH be.fieldworks LEFT JOIN FETCH be.expenseRequests WHERE be.id = :id")
	Optional<BudgetEntry> findByIdWithExtras(@Param("id") Long id);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE Extra e SET e.budgetEntry = null WHERE e.budgetEntry.id IN (SELECT be.id FROM BudgetEntry be WHERE be.budget.id = :budgetId)")
	void detachExtrasByBudgetId(@Param("budgetId") Long budgetId);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE ExpenseRequest er SET er.budgetEntry = null WHERE er.budgetEntry.id IN (SELECT be.id FROM BudgetEntry be WHERE be.budget.id = :budgetId)")
	void detachExpenseRequestsByBudgetId(@Param("budgetId") Long budgetId);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE Fieldwork fw SET fw.budgetEntry = null WHERE fw.budgetEntry.id IN (SELECT be.id FROM BudgetEntry be WHERE be.budget.id = :budgetId)")
	void detachFieldworksByBudgetId(@Param("budgetId") Long budgetId);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM BudgetEntry be WHERE be.budget.id = :budgetId")
	void deleteAllByBudgetId(@Param("budgetId") Long budgetId);
}