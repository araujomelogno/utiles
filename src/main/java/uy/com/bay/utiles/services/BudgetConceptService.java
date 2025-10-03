package uy.com.bay.utiles.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.repo.BudgetConceptRepository;

@Service
public class BudgetConceptService {

    private final BudgetConceptRepository repository;

    public BudgetConceptService(BudgetConceptRepository repository) {
        this.repository = repository;
    }

    public Optional<BudgetConcept> get(Long id) {
        return repository.findById(id);
    }

    public BudgetConcept save(BudgetConcept entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<BudgetConcept> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<BudgetConcept> list(Pageable pageable, Specification<BudgetConcept> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}