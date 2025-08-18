package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.repository.ExpenseRequestRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpenseRequestService {

    private final ExpenseRequestRepository repository;

    public ExpenseRequestService(ExpenseRequestRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseRequest> get(Long id) {
        return repository.findById(id);
    }

    public ExpenseRequest update(ExpenseRequest entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseRequest> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ExpenseRequest> list(Pageable pageable, Specification<ExpenseRequest> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public int count(Specification<ExpenseRequest> filter) {
        return (int) repository.count(filter);
    }

    public List<ExpenseRequest> findAllByExpenseStatus(ExpenseStatus expenseStatus, Pageable pageable) {
        return repository.findAllByExpenseStatus(expenseStatus, pageable);
    }

    @Transactional
    public void approveRequests(List<Long> ids) {
        repository.approveRequests(ids, ExpenseStatus.APROBADO, new Date());
    }
}
