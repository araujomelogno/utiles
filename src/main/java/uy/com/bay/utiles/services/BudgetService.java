package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.BudgetRepository;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetEntry;

@Service
public class BudgetService {

	private final BudgetRepository repository;

	public BudgetService(BudgetRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Optional<Budget> get(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

	@Transactional
	public Budget save(Budget entity) {
		if (entity.getEntries() != null) {
			for (BudgetEntry entry : entity.getEntries()) {
				entry.setBudget(entity);
			}
		}
		return repository.save(entity);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public Page<Budget> list(Pageable pageable) {
		Page<Budget> budgets = repository.findAllWithEntries(pageable);
		budgets.forEach(b -> b.getEntries().size());
		return budgets;
	}

	public int count() {
		return (int) repository.count();
	}

	@Transactional(readOnly = true)
	public List<Budget> findAll() {
		List<Budget> budgets = repository.findAllWithEntries();
		budgets.forEach(b -> b.getEntries().size());
		return budgets;
	}

	@Transactional(readOnly = true)
	public Optional<Budget> findByStudy(Study study) {
		Optional<Budget> budget = repository.findByStudyWithEntries(study);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

	@Transactional(readOnly = true)
	public Optional<Budget> findByIdWithEntries(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(b -> b.getEntries().size());
		return budget;
	}

}