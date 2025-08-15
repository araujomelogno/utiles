package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.ExpenseReportFile;

public interface ExpenseReportFileRepository extends JpaRepository<ExpenseReportFile, Long>, JpaSpecificationExecutor<ExpenseReportFile> {
}
