package uy.com.bay.utiles.data.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;

import java.util.Date;
import java.util.List;

public interface ExpenseRequestRepository extends JpaRepository<ExpenseRequest, Long>, JpaSpecificationExecutor<ExpenseRequest> {

    List<ExpenseRequest> findAllByExpenseStatus(ExpenseStatus expenseStatus, Pageable pageable);

    @Modifying
    @Query("update ExpenseRequest er set er.expenseStatus = :status, er.aprovalDate = :aprovalDate where er.id in :ids")
    void approveRequests(@Param("ids") List<Long> ids, @Param("status") ExpenseStatus status, @Param("aprovalDate") Date aprovalDate);
    
    @Modifying
    @Query("update ExpenseRequest er set er.expenseStatus = :status, er.aprovalDate = :aprovalDate where er.id in :ids")
    void revokeRequests(@Param("ids") List<Long> ids, @Param("status") ExpenseStatus status, @Param("aprovalDate") Date aprovalDate);
}
