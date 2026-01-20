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

	List<SupervisionTask> findByStatus(Status status);

	@Query("SELECT st FROM SupervisionTask st WHERE st.created BETWEEN :from AND :to " + "ORDER BY st.created DESC")
	List<SupervisionTask> findByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to);
}
