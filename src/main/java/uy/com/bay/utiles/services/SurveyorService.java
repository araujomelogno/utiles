package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Test;
import uy.com.bay.utiles.data.SurveyorRepository;

@Service
public class SurveyorService {

    private final SurveyorRepository repository;

    public SurveyorService(SurveyorRepository repository) {
        this.repository = repository;
    }

    public Optional<Test> get(Long id) {
        return repository.findById(id);
    }

    public Test save(Test entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Test> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Test> list(Pageable pageable, Specification<Test> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Test> findAll() {
        return repository.findAll();
    }
}
