package uy.com.bay.utiles.services;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetEntry;

import java.time.LocalDate;
import java.util.List;

@Service
public class GanttService {

    private final FieldworkRepository fieldworkRepository;

    public GanttService(FieldworkRepository fieldworkRepository) {
        this.fieldworkRepository = fieldworkRepository;
    }

    @Transactional(readOnly = true)
    public List<Fieldwork> getFieldworks(LocalDate startDate, LocalDate endDate) {
        List<Fieldwork> fieldworks = fieldworkRepository
                .findAllByInitPlannedDateLessThanAndEndPlannedDateGreaterThan(endDate, startDate);
        for (Fieldwork fieldwork : fieldworks) {
            Study study = fieldwork.getStudy();
            if (study == null) {
                continue;
            }
            Budget budget = study.getBudget();
            if (budget == null) {
                continue;
            }
            Hibernate.initialize(budget.getEntries());
            for (BudgetEntry entry : budget.getEntries()) {
                Hibernate.initialize(entry.getExtras());
                Hibernate.initialize(entry.getExpenseRequests());
                Hibernate.initialize(entry.getFieldworks());
                Hibernate.initialize(entry.getOdooCosts());
            }
        }
        return fieldworks;
    }
}
