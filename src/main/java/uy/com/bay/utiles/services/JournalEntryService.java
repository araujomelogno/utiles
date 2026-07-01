package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	/**
	 * Returns, for each surveyor, the sum of the amounts of the
	 * {@link uy.com.bay.utiles.data.ExpenseTransfer} entities referenced by their
	 * journal entries whose transfer date is on or after {@code fromDate}. If
	 * {@code fromDate} is {@code null} no transfers are considered and an empty map
	 * is returned.
	 *
	 * @param fromDate the earliest transfer date to consider (inclusive)
	 * @return a map of surveyor id to the sum of transfer amounts
	 */
	public Map<Long, Double> sumTransferAmountsBySurveyor(LocalDate fromDate) {
		if (fromDate == null) {
			return Collections.emptyMap();
		}
		Map<Long, Double> result = new HashMap<>();
		for (Object[] row : repository.sumTransferAmountsBySurveyor(fromDate)) {
			Long surveyorId = (Long) row[0];
			Double sum = (Double) row[1];
			if (surveyorId != null) {
				result.put(surveyorId, sum != null ? sum : 0.0);
			}
		}
		return result;
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
