package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.ExpenseRequest;

public interface ExpenseRequestRepository extends JpaRepository<ExpenseRequest, Long>, JpaSpecificationExecutor<ExpenseRequest> {
}
