package uy.com.bay.utiles.data.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.EncodingTask;
import uy.com.bay.utiles.data.Status;

@Repository
public interface EncodingTaskRepository extends JpaRepository<EncodingTask, Long> {

	@Query("SELECT t FROM EncodingTask t WHERE t.created BETWEEN :from AND :to "
			+ "AND (:surveyFileName IS NULL OR :surveyFileName = '' OR lower(t.surveyFileName) LIKE lower(concat('%', :surveyFileName, '%'))) "
			+ "AND (:status IS NULL OR t.status = :status) " + "ORDER BY t.created DESC")
	List<EncodingTask> findByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to,
			@Param("surveyFileName") String fileName, @Param("status") Status status);
}
