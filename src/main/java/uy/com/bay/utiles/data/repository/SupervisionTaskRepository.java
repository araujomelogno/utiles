package uy.com.bay.utiles.data.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;

@Repository
public interface SupervisionTaskRepository extends JpaRepository<SupervisionTask, Long> {

    List<SupervisionTask> findByStatus(Status status);

    

    @Query("select t.id from SupervisionTask t where t.status = :status")
    List<Long> findIdsByStatus(@Param("status") Status status);
    
    @Query("SELECT DISTINCT st FROM SupervisionTask st LEFT JOIN FETCH st.durationBySpeakers WHERE st.created BETWEEN :from AND :to " +
            "AND (:fileName IS NULL OR :fileName = '' OR lower(st.fileName) LIKE lower(concat('%', :fileName, '%'))) " +
            "AND (:status IS NULL OR st.status = :status) " +
            "ORDER BY st.created DESC")
    List<SupervisionTask> findByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to,
                                                                 @Param("fileName") String fileName, @Param("status") Status status);
}
