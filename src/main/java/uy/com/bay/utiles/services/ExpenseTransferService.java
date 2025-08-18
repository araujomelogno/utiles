package uy.com.bay.utiles.services;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.data.repository.ExpenseTransferRepository;

import java.util.Optional;

@Service
public class ExpenseTransferService {

    private final ExpenseTransferRepository repository;

    public ExpenseTransferService(ExpenseTransferRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseTransfer> get(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public ExpenseTransfer findByIdAndInitialize(Long id) {
        ExpenseTransfer et = repository.findById(id).orElse(null);
        if (et != null) {
            Hibernate.initialize(et.getExpenseRequests());
        }
        return et;
    }

    public ExpenseTransfer update(ExpenseTransfer entity) {
        return repository.save(entity);
    }

    public ExpenseTransfer save(ExpenseTransfer entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseTransfer> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
