package uy.com.bay.utiles.data.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.repository.FieldworkRepository;

import java.util.Optional;

@Service
public class FieldworkService {

    private final FieldworkRepository repository;

    public FieldworkService(FieldworkRepository repository) {
        this.repository = repository;
    }

    public Optional<Fieldwork> get(Long id) {
        return repository.findById(id);
    }

    public Fieldwork save(Fieldwork entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Fieldwork> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Fieldwork> list(Pageable pageable, Specification<Fieldwork> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
