package uy.com.bay.utiles.services;

import org.springframework.stereotype.Service;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.repo.BudgetEntryRepository;

@Service
public class BudgetEntryService {

    private final BudgetEntryRepository repository;

    public BudgetEntryService(BudgetEntryRepository repository) {
        this.repository = repository;
    }

    public BudgetEntry save(BudgetEntry entity) {
        return repository.save(entity);
    }
}