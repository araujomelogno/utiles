package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.data.repository.BudgetRepository;

@Service
public class BudgetService {

	private final BudgetRepository repository;

	public BudgetService(BudgetRepository repository) {
		this.repository = repository;
	}

	public Optional<Budget> get(Long id) {
		return repository.findById(id);
	}

	public Budget save(Budget entity) {
		return repository.save(entity);
	}

	public void delete(Long id) {
		repository.deleteById(id);
	}

	public Page<Budget> list(Pageable pageable) {
		return repository.findAllWithEntries(pageable);
	}

	public int count() {
		return (int) repository.count();
	}

	public List<Budget> findAll() {
		return repository.findAll();
	}

}