package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.ExpenseTransferFile;

public interface ExpenseTransferFileRepository extends JpaRepository<ExpenseTransferFile, Long> {
}
