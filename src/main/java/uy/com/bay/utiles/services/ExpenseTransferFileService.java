package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import uy.com.bay.utiles.data.repository.ExpenseTransferFileRepository;

import java.util.Optional;

@Service
public class ExpenseTransferFileService {

    private final ExpenseTransferFileRepository repository;

    public ExpenseTransferFileService(ExpenseTransferFileRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseTransferFile> get(Long id) {
        return repository.findById(id);
    }

    public ExpenseTransferFile update(ExpenseTransferFile entity) {
        return repository.save(entity);
    }

    public ExpenseTransferFile save(ExpenseTransferFile entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseTransferFile> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
