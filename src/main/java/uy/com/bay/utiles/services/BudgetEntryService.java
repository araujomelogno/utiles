package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
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

    public List<BudgetEntry> findForPlanningReport(LocalDate fechaDesde, LocalDate fechaHasta, List<Study> studies) {
        if (studies == null || studies.isEmpty()) {
            return repository.findByDateRange(fechaDesde, fechaHasta);
        }
        return repository.findByDateRangeAndStudies(fechaDesde, fechaHasta, studies);
    }
}