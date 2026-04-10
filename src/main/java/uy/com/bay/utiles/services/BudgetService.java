package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.BudgetRepository;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.repo.BudgetEntryRepository;

@Service
public class BudgetService {

	private final BudgetRepository repository;
	private final BudgetEntryRepository budgetEntryRepository;

	public BudgetService(BudgetRepository repository, BudgetEntryRepository budgetEntryRepository) {
		this.repository = repository;
		this.budgetEntryRepository = budgetEntryRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Budget> get(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(this::initializeEntryCollections);
		return budget;
	}

	private void initializeEntryCollections(Budget budget) {
		if (budget.getEntries() != null) {
			for (BudgetEntry entry : budget.getEntries()) {
				Hibernate.initialize(entry.getExtras());
				Hibernate.initialize(entry.getExpenseRequests());
				Hibernate.initialize(entry.getFieldworks());
			}
		}
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
		budgetEntryRepository.detachExtrasByBudgetId(id);
		budgetEntryRepository.detachExpenseRequestsByBudgetId(id);
		budgetEntryRepository.detachFieldworksByBudgetId(id);
		budgetEntryRepository.deleteAllByBudgetId(id);
		repository.deleteByIdBulk(id);
	}

	@Transactional(readOnly = true)
	public Page<Budget> list(Pageable pageable) {
		return repository.findAllWithEntries(pageable);
	}

	public int count() {
		return (int) repository.count();
	}

	@Transactional(readOnly = true)
	public List<Budget> findAll() {
		return repository.findAllWithEntries();
	}

	@Transactional(readOnly = true)
	public Optional<Budget> findByStudy(Study study) {
		return repository.findByStudyWithEntries(study);
	}

	@Transactional(readOnly = true)
	public Optional<Budget> findByIdWithEntries(Long id) {
		Optional<Budget> budget = repository.findByIdWithEntries(id);
		budget.ifPresent(this::initializeEntryCollections);
		return budget;
	}

	@Transactional(readOnly = true)
	public Optional<BudgetEntry> getEntryByIdWithExtras(Long id) {
		return budgetEntryRepository.findByIdWithExtras(id);
	}

}