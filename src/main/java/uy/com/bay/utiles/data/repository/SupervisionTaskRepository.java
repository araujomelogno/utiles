package uy.com.bay.utiles.data.repository;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Tuple;

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

    @Query("SELECT st.id as id, st.fileName as fileName, st.status as status, st.aiScore as aiScore, " +
            "st.totalAudioDuration as totalAudioDuration, st.speakingDuration as speakingDuration, " +
            "st.created as created, st.output as output, st.evaluationOutput as evaluationOutput, " +
            "KEY(ds) as speaker, VALUE(ds) as duration " +
            "FROM SupervisionTask st LEFT JOIN st.durationBySpeakers ds " +
            "WHERE st.created BETWEEN :from AND :to " +
            "AND (:fileName IS NULL OR :fileName = '' OR lower(st.fileName) LIKE lower(concat('%', :fileName, '%'))) " +
            "AND (:status IS NULL OR st.status = :status) " +
            "ORDER BY st.created DESC")
    List<Tuple> findTuplesByCreatedBetweenOrderByCreatedDesc(@Param("from") Date from, @Param("to") Date to,
                                                             @Param("fileName") String fileName, @Param("status") Status status);
}
