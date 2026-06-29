package uy.com.bay.utiles.data.service;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseRepository;
import uy.com.bay.utiles.data.repository.DoobloResponseRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetEntry;

import java.util.List;
import java.util.Optional;

@Service
public class FieldworkService {

    private final FieldworkRepository repository;
    private final DoobloResponseRepository doobloResponseRepository;
    private final AlchemerSurveyResponseRepository alchemerSurveyResponseRepository;

    public FieldworkService(FieldworkRepository repository, DoobloResponseRepository doobloResponseRepository,
            AlchemerSurveyResponseRepository alchemerSurveyResponseRepository) {
        this.repository = repository;
        this.doobloResponseRepository = doobloResponseRepository;
        this.alchemerSurveyResponseRepository = alchemerSurveyResponseRepository;
    }

    public Optional<Fieldwork> get(Long id) {
        return repository.findById(id);
    }

    public Fieldwork save(Fieldwork entity) {
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id).ifPresent(fieldwork -> {
            // Las respuestas de encuestas referencian al fieldwork mediante una FK; se
            // desvinculan (sin borrar los datos de encuesta) para no violar la restriccion.
            doobloResponseRepository.clearFieldwork(id);
            alchemerSurveyResponseRepository.clearFieldwork(id);

            // El Study expone la coleccion con cascade = ALL. Si el fieldwork sigue en ella
            // al hacer flush, Hibernate intenta re-guardarlo ("deleted object would be
            // re-saved by cascade"), por lo que hay que quitarlo antes de eliminarlo.
            Study study = fieldwork.getStudy();
            if (study != null && study.getFieldworks() != null) {
                study.getFieldworks().remove(fieldwork);
            }

            BudgetEntry budgetEntry = fieldwork.getBudgetEntry();
            if (budgetEntry != null && budgetEntry.getFieldworks() != null) {
                budgetEntry.getFieldworks().remove(fieldwork);
            }

            repository.delete(fieldwork);
        });
    }

    public Page<Fieldwork> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Fieldwork> list(Pageable pageable, Specification<Fieldwork> filter) {
        return repository.findAll(filter, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Fieldwork> listWithBudget(Pageable pageable, Specification<Fieldwork> filter) {
        Page<Fieldwork> page = repository.findAll(filter, pageable);
        for (Fieldwork fw : page.getContent()) {
            initializeBudget(fw);
        }
        return page;
    }

    private void initializeBudget(Fieldwork fw) {
        if (fw.getBudgetEntry() == null || fw.getBudgetEntry().getBudget() == null) {
            return;
        }
        Budget budget = fw.getBudgetEntry().getBudget();
        Hibernate.initialize(budget.getEntries());
        for (BudgetEntry entry : budget.getEntries()) {
            Hibernate.initialize(entry.getExtras());
            Hibernate.initialize(entry.getExpenseRequests());
            Hibernate.initialize(entry.getFieldworks());
            Hibernate.initialize(entry.getOdooCosts());
            for (Fieldwork inner : entry.getFieldworks()) {
                Hibernate.initialize(inner.getCompletedByMonth());
            }
        }
    }

    public int count() {
        return (int) repository.count();
    }

    public List<Fieldwork> findAllByStudy(Study study) {
        return repository.findAllByStudy(study);
    }
}
