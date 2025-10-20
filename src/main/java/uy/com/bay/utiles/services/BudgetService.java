package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.data.repository.BudgetRepository;

@Service
public class BudgetService {

	private final BudgetRepository repository;

	public BudgetService(BudgetRepository repository) {
		this.repository = repository;
	}

	public Optional<Budget> get(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

	public Budget save(Budget entity) {
		return repository.save(entity);
	}

	public void delete(Long id) {
		repository.deleteById(id);
	}

	public Page<Budget> list(Pageable pageable) {
		Page<Budget> budgets = repository.findAllWithEntries(pageable);
		budgets.forEach(b -> b.getEntries().size());
		return budgets;
	}

	public int count() {
		return (int) repository.count();
	}

	public List<Budget> findAll() {
		List<Budget> budgets = repository.findAllWithEntries();
		budgets.forEach(b -> b.getEntries().size());
		return budgets;
	}

	public Optional<Budget> findByStudy(Study study) {
		Optional<Budget> budget = repository.findByStudyWithEntries(study);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

	public Optional<Budget> findByIdWithEntries(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

}