package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.JobOrder;

public interface JobOrderRepository extends JpaRepository<JobOrder, Long>, JpaSpecificationExecutor<JobOrder> {
}
