package uy.com.bay.utiles.services;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.repo.ExtraConceptRepository;

@Service
public class ExtraConceptService {

    private final ExtraConceptRepository extraConceptRepository;

    @Autowired
    public ExtraConceptService(ExtraConceptRepository extraConceptRepository) {
        this.extraConceptRepository = extraConceptRepository;
    }

    public List<ExtraConcept> findAll() {
        return extraConceptRepository.findAll();
    }
}