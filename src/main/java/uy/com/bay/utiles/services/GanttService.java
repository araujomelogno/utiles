package uy.com.bay.utiles.services;

import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.repository.FieldworkRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class GanttService {

    private final FieldworkRepository fieldworkRepository;

    public GanttService(FieldworkRepository fieldworkRepository) {
        this.fieldworkRepository = fieldworkRepository;
    }

    public List<Fieldwork> getFieldworks(LocalDate startDate, LocalDate endDate) {
        return fieldworkRepository.findAllByInitPlannedDateLessThanAndEndPlannedDateGreaterThan(endDate, startDate);
    }
}
