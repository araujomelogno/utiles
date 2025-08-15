package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.StudyRepository;

@Service
public class StudyService {

    private final StudyRepository repository;

    public StudyService(StudyRepository repository) {
        this.repository = repository;
    }

    public Optional<Study> get(Long id) {
        return repository.findById(id);
    }

    public Study save(Study entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Study> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Study> list(Pageable pageable, Specification<Study> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Study> listAll() {
        return repository.findAll();
    }

    public List<Study> findAll() {
        return repository.findAll();
    }
}
