package uy.com.bay.utiles.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.entities.StudyInvoice;

@Repository
public interface StudyInvoiceRepository extends JpaRepository<StudyInvoice, Long> {

	Optional<StudyInvoice> findByMoveId(String moveId);
}
