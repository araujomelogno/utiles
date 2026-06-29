package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.bay.utiles.data.DoobloResponse;

public interface DoobloResponseRepository extends JpaRepository<DoobloResponse, Long> {
    boolean existsByInterviewId(String interviewId);

    @Modifying
    @Query("UPDATE DoobloResponse d SET d.fieldwork = null WHERE d.fieldwork.id = :fieldworkId")
    void clearFieldwork(@Param("fieldworkId") Long fieldworkId);
}
