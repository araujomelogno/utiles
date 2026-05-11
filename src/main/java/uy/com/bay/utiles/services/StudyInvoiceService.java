package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.entities.StudyInvoice;
import uy.com.bay.utiles.repo.StudyInvoiceRepository;

@Service
public class StudyInvoiceService {

	private final StudyInvoiceRepository repository;

	public StudyInvoiceService(StudyInvoiceRepository repository) {
		this.repository = repository;
	}

	public Optional<StudyInvoice> get(Long id) {
		return repository.findById(id);
	}

	public Optional<StudyInvoice> findByMoveId(String moveId) {
		return repository.findByMoveId(moveId);
	}

	public StudyInvoice save(StudyInvoice entity) {
		if (entity.getId() == null && entity.getMoveId() != null) {
			Optional<StudyInvoice> existing = repository.findByMoveId(entity.getMoveId());
			if (existing.isPresent()) {
				return existing.get();
			}
		}
		return repository.save(entity);
	}

	public void delete(Long id) {
		repository.deleteById(id);
	}

	public List<StudyInvoice> findAll() {
		return repository.findAll();
	}
}
