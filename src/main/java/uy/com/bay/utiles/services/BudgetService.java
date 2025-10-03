package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.repo.BudgetRepository;

import java.util.Optional;

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
        return repository.findAll(pageable);
    }

    public Page<Budget> list(Pageable pageable, Specification<Budget> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}