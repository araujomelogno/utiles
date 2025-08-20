package uy.com.bay.utiles.services;

import java.util.List;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.repository.JournalEntryRepository;

@Service
public class JournalEntryService {

	private final JournalEntryRepository repository;

	public JournalEntryService(JournalEntryRepository repository) {
		this.repository = repository;
	}

	public JournalEntry save(JournalEntry entity) {
		return repository.save(entity);
	}

	public List<JournalEntry> findBySurveyor(Surveyor surveyor) {
		return repository.findAllBySurveyorOrderByDateAsc(surveyor);
	}
}
