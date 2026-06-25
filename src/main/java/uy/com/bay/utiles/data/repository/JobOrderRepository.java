package uy.com.bay.utiles.data.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.bay.utiles.data.JobOrder;
import uy.com.bay.utiles.data.Provider;

public interface JobOrderRepository extends JpaRepository<JobOrder, Long>, JpaSpecificationExecutor<JobOrder> {

    List<JobOrder> findByProvider(Provider provider);

    List<JobOrder> findByProviderOrderByCreatedDesc(Provider provider);

    /**
     * Returns all job orders whose [init, end] interval overlaps, at least
     * partially, the given [from, to] range.
     */
    @Query("SELECT j FROM JobOrder j WHERE j.init IS NOT NULL AND j.end IS NOT NULL "
            + "AND j.init <= :to AND j.end >= :from")
    List<JobOrder> findOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
