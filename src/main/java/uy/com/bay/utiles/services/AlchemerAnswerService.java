package uy.com.bay.utiles.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;

@Service
public class AlchemerAnswerService {

    private final AlchemerAnswerRepository repository;

    public AlchemerAnswerService(AlchemerAnswerRepository repository) {
        this.repository = repository;
    }

    public Page<AlchemerAnswer> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<AlchemerAnswer> list(Pageable pageable, Specification<AlchemerAnswer> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }
}
