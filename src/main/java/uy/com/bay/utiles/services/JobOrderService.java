package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.JobOrder;
import uy.com.bay.utiles.data.repository.JobOrderRepository;

@Service
public class JobOrderService {

    private final JobOrderRepository repository;

    public JobOrderService(JobOrderRepository repository) {
        this.repository = repository;
    }

    public Optional<JobOrder> get(Long id) {
        return repository.findById(id);
    }

    public JobOrder save(JobOrder entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<JobOrder> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<JobOrder> list(Pageable pageable, Specification<JobOrder> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<JobOrder> findAll() {
        return repository.findAll();
    }
}
