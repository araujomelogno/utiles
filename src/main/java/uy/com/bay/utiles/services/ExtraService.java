package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Extra;
import uy.com.bay.utiles.repo.ExtraRepository;

@Service
public class ExtraService {

    private final ExtraRepository extraRepository;

    @Autowired
    public ExtraService(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    public List<Extra> findByStudyAndMonth(Study study, LocalDate month) {
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());
        return extraRepository.findByStudyAndDateBetween(study, startDate, endDate);
    }

    public Extra save(Extra extra) {
        return extraRepository.save(extra);
    }

    public void delete(Extra extra) {
        extraRepository.delete(extra);
    }
}