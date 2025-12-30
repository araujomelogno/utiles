package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;

import java.util.Date;
import java.util.List;

@Repository
public interface SupervisionTaskRepository extends JpaRepository<SupervisionTask, Long> {

    @Query("SELECT t FROM SupervisionTask t WHERE t.created BETWEEN :from AND :to " +
            "AND (:fileName IS NULL OR lower(t.fileName) LIKE lower(concat('%', :fileName, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "ORDER BY t.created DESC")
    List<SupervisionTask> findByCreatedBetweenOrderByCreatedDesc(
            @Param("from") Date from,
            @Param("to") Date to,
            @Param("fileName") String fileName,
            @Param("status") Status status
    );
}
