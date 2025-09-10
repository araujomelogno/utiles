package uy.com.bay.utiles.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Area;
import uy.com.bay.utiles.data.repository.AreaRepository;

@Service
public class AreaService {

    private final AreaRepository repository;

    public AreaService(AreaRepository repository) {
        this.repository = repository;
    }

    public Optional<Area> get(Long id) {
        return repository.findById(id);
    }

    public Area save(Area entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Area> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Area> list(Pageable pageable, Specification<Area> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public java.util.List<Area> listAll() {
        return repository.findAll();
    }
}
