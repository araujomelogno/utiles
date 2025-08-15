package uy.com.bay.utiles.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.repository.ExpenseRequestTypeRepository;

@Service
public class ExpenseRequestTypeService {

    private final ExpenseRequestTypeRepository repository;

    public ExpenseRequestTypeService(ExpenseRequestTypeRepository repository) {
        this.repository = repository;
    }

    public Optional<ExpenseRequestType> get(Long id) {
        return repository.findById(id);
    }

    public ExpenseRequestType save(ExpenseRequestType entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExpenseRequestType> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ExpenseRequestType> list(Pageable pageable, Specification<ExpenseRequestType> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
