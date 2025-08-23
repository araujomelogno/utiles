package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;

@Service
public class SurveyorService {

    private final SurveyorRepository repository;

    public SurveyorService(SurveyorRepository repository) {
        this.repository = repository;
    }

    public Optional<Surveyor> get(Long id) {
        return repository.findById(id);
    }

    public Surveyor save(Surveyor entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Surveyor> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Surveyor> list(Pageable pageable, Specification<Surveyor> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Surveyor> listAll() {
        return repository.findAll();
    }

    public List<Surveyor> findAll() {
        return repository.findAll();
    }
    public Optional<Surveyor> findBySurveyToGoId(String surveyToGoId) {
        return repository.findBySurveyToGoId(surveyToGoId);
    }

    public Optional<Surveyor> findByName(String name) {
        return repository.findByName(name);
    }
}
