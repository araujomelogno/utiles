package uy.com.bay.utiles.data.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.WhatsappFlowTask;

public interface WhatsappFlowTaskRepository extends JpaRepository<WhatsappFlowTask, Long> {
    List<WhatsappFlowTask> findByCreatedBetweenOrderByCreatedDesc(Date start, Date end);
}
