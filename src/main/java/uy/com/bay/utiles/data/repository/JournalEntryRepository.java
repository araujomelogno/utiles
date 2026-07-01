package uy.com.bay.utiles.data.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;

public interface JournalEntryRepository
		extends JpaRepository<JournalEntry, Long>, JpaSpecificationExecutor<JournalEntry> {

	List<JournalEntry> findAllBySurveyorOrderByDateAsc(Surveyor surveyor);

	List<JournalEntry> findAllByStudyOrderByDateAsc(Study study);

	List<JournalEntry> findAllByExpenseReport(ExpenseReport expenseReport);

	/**
	 * Sums the amount of the {@link uy.com.bay.utiles.data.ExpenseTransfer}
	 * referenced by the journal entries of each surveyor whose transfer date is on
	 * or after the given date, grouped by surveyor id.
	 *
	 * @return a list of {@code [surveyorId, sumOfTransferAmounts]} rows
	 */
	@Query("SELECT je.surveyor.id, SUM(je.transfer.amount) FROM JournalEntry je "
			+ "WHERE je.transfer IS NOT NULL AND je.transfer.transferDate >= :fromDate "
			+ "GROUP BY je.surveyor.id")
	List<Object[]> sumTransferAmountsBySurveyor(@Param("fromDate") LocalDate fromDate);
}
