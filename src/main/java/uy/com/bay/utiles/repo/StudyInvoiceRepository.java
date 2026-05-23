package uy.com.bay.utiles.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.StudyInvoice;

@Repository
public interface StudyInvoiceRepository extends JpaRepository<StudyInvoice, Long> {

	Optional<StudyInvoice> findByMoveId(String moveId);

	List<StudyInvoice> findByStudy(Study study);

	@Query("SELECT COALESCE(SUM(si.totalSigned), 0) FROM StudyInvoice si WHERE si.study = :study")
	Double sumAmountTotalByStudy(@Param("study") Study study);
}
