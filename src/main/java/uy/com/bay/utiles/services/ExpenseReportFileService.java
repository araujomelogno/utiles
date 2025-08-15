package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExpenseReportFile;
import uy.com.bay.utiles.data.repository.ExpenseReportFileRepository;

import java.util.Optional;

@Service
public class ExpenseReportFileService {

    private final ExpenseReportFileRepository repository;

    public ExpenseReportFileService(ExpenseReportFileRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseReportFile> get(Long id) {
        return repository.findById(id);
    }

    public ExpenseReportFile update(ExpenseReportFile entity) {
        return repository.save(entity);
    }

    public ExpenseReportFile save(ExpenseReportFile entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseReportFile> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ExpenseReportFile> list(Pageable pageable, Specification<ExpenseReportFile> filter) {
        return repository.findAll(filter, pageable);
    }
}
