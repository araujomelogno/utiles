package uy.com.bay.utiles.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Extra;

@Repository
public interface ExtraRepository extends JpaRepository<Extra, Integer> {

    List<Extra> findByStudyAndDateBetween(Study study, LocalDate startDate, LocalDate endDate);
}