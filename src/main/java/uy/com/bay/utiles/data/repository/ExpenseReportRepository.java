package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportStatus;

import java.util.List;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long>, JpaSpecificationExecutor<ExpenseReport> {

    List<ExpenseReport> findAllByStatus(ExpenseReportStatus status);
}
