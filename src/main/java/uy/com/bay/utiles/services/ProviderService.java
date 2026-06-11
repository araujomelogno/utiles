package uy.com.bay.utiles.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Provider;
import uy.com.bay.utiles.data.repository.ProviderRepository;

@Service
public class ProviderService {

    private final ProviderRepository repository;

    public ProviderService(ProviderRepository repository) {
        this.repository = repository;
    }

    public Optional<Provider> get(Long id) {
        return repository.findById(id);
    }

    public Provider save(Provider entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Provider> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Provider> list(Pageable pageable, Specification<Provider> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Provider> findAll() {
        return repository.findAll();
    }
}
