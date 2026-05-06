package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.repo.BudgetEntryRepository;

@Service
public class BudgetEntryService {

    private final BudgetEntryRepository repository;

    public BudgetEntryService(BudgetEntryRepository repository) {
        this.repository = repository;
    }

    public BudgetEntry save(BudgetEntry entity) {
        return repository.save(entity);
    }

    @Transactional
    public void updateDates(Long entryId, LocalDate init, LocalDate end) {
        repository.findById(entryId).ifPresent(entry -> {
            entry.setInit(init);
            entry.setEnd(end);
        });
    }

    @Transactional(readOnly = true)
    public List<BudgetEntry> findForPlanningReport(LocalDate fechaDesde, LocalDate fechaHasta, List<Study> studies) {
        List<BudgetEntry> entries = (studies == null || studies.isEmpty())
                ? repository.findByDateRange(fechaDesde, fechaHasta)
                : repository.findByDateRangeAndStudies(fechaDesde, fechaHasta, studies);
        for (BudgetEntry entry : entries) {
            Hibernate.initialize(entry.getExtras());
            Hibernate.initialize(entry.getExpenseRequests());
            Hibernate.initialize(entry.getOdooCosts());
            Hibernate.initialize(entry.getFieldworks());
            for (Fieldwork fieldwork : entry.getFieldworks()) {
                Hibernate.initialize(fieldwork.getCompletedByMonth());
            }
        }
        return entries;
    }
}