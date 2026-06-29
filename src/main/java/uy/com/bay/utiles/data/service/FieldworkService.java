package uy.com.bay.utiles.data.service;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FieldworkService.class);

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
        if (id == null) {
            return;
        }
        Fieldwork fieldwork = repository.findById(id).orElse(null);
        if (fieldwork == null) {
            logger.warn("delete: no se encontro Fieldwork con id={}", id);
            return;
        }
        try {
            // Las respuestas de encuestas referencian al fieldwork mediante una FK; se
            // desvinculan (sin borrar los datos de encuesta) para no violar la restriccion.
            doobloResponseRepository.clearFieldwork(id);
            alchemerSurveyResponseRepository.clearFieldwork(id);

            // Romper la asociacion por ambos lados. El Study expone la coleccion con
            // cascade = ALL: si el fieldwork sigue referenciado al hacer flush, Hibernate
            // intenta re-guardarlo ("deleted object would be re-saved by cascade"). Se
            // quita de las colecciones inversas y se anulan los lados propietarios (FK).
            Study study = fieldwork.getStudy();
            if (study != null && study.getFieldworks() != null) {
                study.getFieldworks().removeIf(f -> id.equals(f.getId()));
            }
            fieldwork.setStudy(null);

            BudgetEntry budgetEntry = fieldwork.getBudgetEntry();
            if (budgetEntry != null && budgetEntry.getFieldworks() != null) {
                budgetEntry.getFieldworks().removeIf(f -> id.equals(f.getId()));
            }
            fieldwork.setBudgetEntry(null);

            // Persistir la desvinculacion (FK study_id / budget_entry_id en null) antes
            // de eliminar, para que el DELETE no choque con el grafo de cascada.
            repository.saveAndFlush(fieldwork);

            repository.delete(fieldwork);
            repository.flush();
            logger.info("delete: Fieldwork id={} eliminado correctamente", id);
        } catch (RuntimeException e) {
            logger.error("delete: error al eliminar Fieldwork id={}", id, e);
            throw e;
        }
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
