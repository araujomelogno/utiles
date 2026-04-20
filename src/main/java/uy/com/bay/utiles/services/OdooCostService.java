package uy.com.bay.utiles.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.entities.OdooCost;
import uy.com.bay.utiles.repo.OdooCostRepository;

@Service
public class OdooCostService {

    private final OdooCostRepository repository;

    public OdooCostService(OdooCostRepository repository) {
        this.repository = repository;
    }

    public OdooCost save(OdooCost entity) {
        return repository.save(entity);
    }

    public Optional<OdooCost> findByMoveId(String moveId) {
        return repository.findByMoveId(moveId);
    }
}
