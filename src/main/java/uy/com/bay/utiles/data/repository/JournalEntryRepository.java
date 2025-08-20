package uy.com.bay.utiles.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Surveyor;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long>, JpaSpecificationExecutor<JournalEntry> {

	List<JournalEntry> findAllBySurveyorOrderByDateAsc(Surveyor surveyor);
}
