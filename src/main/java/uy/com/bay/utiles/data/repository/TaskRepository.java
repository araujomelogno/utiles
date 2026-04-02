package uy.com.bay.utiles.data.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uy.com.bay.utiles.data.JobType;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByJobTypeAndStatus(JobType jobType, Status status);

    @Query("SELECT t FROM Task t WHERE t.jobType = :jobType AND (t.status = 'PENDING' OR (t.status = 'RUNNING' AND t.processDate < :cutoffDate)) LIMIT 1")
    Optional<Task> findPendingOrStuckRunning(@Param("jobType") JobType jobType, @Param("cutoffDate") Date cutoffDate);
}
