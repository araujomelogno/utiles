package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.ExpenseTransfer;

public interface ExpenseTransferRepository extends JpaRepository<ExpenseTransfer, Long> {
}
