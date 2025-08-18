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
import java.util.Optional;

public interface ExpenseRequestRepository extends JpaRepository<ExpenseRequest, Long>, JpaSpecificationExecutor<ExpenseRequest> {

    List<ExpenseRequest> findAllByExpenseStatus(ExpenseStatus expenseStatus, Pageable pageable);

    @Query("select er from ExpenseRequest er left join fetch er.expenseTransfer et left join fetch et.expenseRequests where er.id = :id")
    Optional<ExpenseRequest> findByIdWithFullExpenseTransfer(@Param("id") Long id);

    @Modifying
    @Query("update ExpenseRequest er set er.expenseStatus = :status, er.aprovalDate = :aprovalDate where er.id in :ids")
    void approveRequests(@Param("ids") List<Long> ids, @Param("status") ExpenseStatus status, @Param("aprovalDate") Date aprovalDate);
}
