package uy.com.bay.utiles.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.JobOrder;
import uy.com.bay.utiles.data.Provider;

public interface JobOrderRepository extends JpaRepository<JobOrder, Long>, JpaSpecificationExecutor<JobOrder> {

    List<JobOrder> findByProvider(Provider provider);

    List<JobOrder> findByProviderOrderByCreatedDesc(Provider provider);
}
