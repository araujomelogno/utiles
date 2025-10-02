package uy.com.bay.utiles.data.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.repository.ExtraConceptRepository;

import java.util.Optional;

@Service
public class ExtraConceptService {

    private final ExtraConceptRepository repository;

    public ExtraConceptService(ExtraConceptRepository repository) {
        this.repository = repository;
    }

    public Optional<ExtraConcept> get(Long id) {
        return repository.findById(id);
    }

    public ExtraConcept save(ExtraConcept entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<ExtraConcept> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<ExtraConcept> list(Pageable pageable, Specification<ExtraConcept> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}