package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.ExpenseRequestType;

public interface ExpenseRequestTypeRepository extends JpaRepository<ExpenseRequestType, Long>, JpaSpecificationExecutor<ExpenseRequestType> {
}
