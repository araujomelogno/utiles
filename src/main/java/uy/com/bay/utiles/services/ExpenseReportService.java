package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.repository.ExpenseReportRepository;

import java.util.Optional;

@Service
public class ExpenseReportService {

    private final ExpenseReportRepository repository;

    public ExpenseReportService(ExpenseReportRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseReport> get(Long id) {
        return repository.findById(id);
    }

    public ExpenseReport update(ExpenseReport entity) {
        return repository.save(entity);
    }

    public ExpenseReport save(ExpenseReport entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseReport> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ExpenseReport> list(Pageable pageable, Specification<ExpenseReport> filter) {
        return repository.findAll(filter, pageable);
    }

    public long count(Specification<ExpenseReport> filter) {
        return repository.count(filter);
    }

    public int count() {
        return (int) repository.count();
    }
}
