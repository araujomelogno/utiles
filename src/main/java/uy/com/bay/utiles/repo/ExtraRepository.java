package uy.com.bay.utiles.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.entities.Extra;

@Repository
public interface ExtraRepository extends JpaRepository<Extra, Integer> {

    List<Extra> findByStudyAndDateBetween(Study study, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Extra e WHERE " +
            "(:fechaDesde IS NULL OR e.date >= :fechaDesde) AND " +
            "(:fechaHasta IS NULL OR e.date <= :fechaHasta) AND " +
            "(:encuestadores IS NULL OR e.surveyor IN :encuestadores) AND " +
            "(:estudios IS NULL OR e.study IN :estudios) AND " +
            "(:conceptos IS NULL OR e.concept IN :conceptos)")
    List<Extra> findExtrasByFilters(
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("encuestadores") List<Surveyor> encuestadores,
            @Param("estudios") List<Study> estudios,
            @Param("conceptos") List<ExtraConcept> conceptos
    );
}