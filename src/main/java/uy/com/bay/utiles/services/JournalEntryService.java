package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.repository.JournalEntryRepository;

@Service
public class JournalEntryService {

	private final JournalEntryRepository repository;

	public JournalEntryService(JournalEntryRepository repository) {
		this.repository = repository;
	}

	public JournalEntry save(JournalEntry entity) {
		return repository.save(entity);
	}

	public List<JournalEntry> findBySurveyor(Surveyor surveyor) {
		return repository.findAllBySurveyorOrderByDateAsc(surveyor);
	}

	public List<JournalEntry> findAllByStudy(Study study) {
		return repository.findAllByStudyOrderByDateAsc(study);
	}

	public List<JournalEntry> list(Specification<JournalEntry> filter) {
		return repository.findAll(filter);
	}

	public Specification<JournalEntry> createFilterSpecification(LocalDate fechaDesde, LocalDate fechaHasta,
			Set<Surveyor> surveyors, Set<Study> studies) {
		return (root, query, criteriaBuilder) -> {
			java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
			if (fechaDesde != null) {
				Date desde = Date.from(fechaDesde.atStartOfDay(ZoneId.systemDefault()).toInstant());
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), desde));
			}
			if (fechaHasta != null) {
				Date hasta = Date.from(fechaHasta.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), hasta));
			}
			if (surveyors != null && !surveyors.isEmpty()) {
				predicates.add(root.get("surveyor").in(surveyors));
			}
			if (studies != null && !studies.isEmpty()) {
				predicates.add(root.get("study").in(studies));
			}
			return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		};
	}
}
