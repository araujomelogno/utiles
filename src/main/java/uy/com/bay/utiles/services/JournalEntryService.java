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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.JournalEntryReportDTO;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.repository.JournalEntryRepository;

@Service
public class JournalEntryService {

	private final JournalEntryRepository repository;

	@PersistenceContext
	private EntityManager entityManager;

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

	/**
	 * Loads the data required to build the expenses Excel report as a lightweight
	 * scalar projection.
	 *
	 * <p>
	 * The report only needs scalar fields of {@link JournalEntry} plus a few from
	 * the associated {@link Study} and {@link Surveyor}. Fetching full entities via
	 * {@code repository.findAll(filter)} eagerly loaded the attachment collections
	 * (which hold {@code @Lob} binary content), exhausting the heap when many
	 * entries matched the filter. This projection selects only the needed columns
	 * (with {@code LEFT} joins so entries without a study or surveyor are kept) and
	 * therefore never materializes the binary attachments.
	 *
	 * @param filter the same dynamic filter used by the UI (may be {@code null})
	 * @return the report rows ordered by date
	 */
	public List<JournalEntryReportDTO> listReport(Specification<JournalEntry> filter) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
		Root<JournalEntry> root = query.from(JournalEntry.class);
		Join<JournalEntry, Study> study = root.join("study", JoinType.LEFT);
		Join<JournalEntry, Surveyor> surveyor = root.join("surveyor", JoinType.LEFT);

		query.multiselect(root.get("detail"), root.get("obs"), root.get("date"), root.get("operation"),
				root.get("amount"), root.get("source"), study.get("name"), study.get("odooId"), study.get("obs"),
				study.get("clientName"), study.get("area"), study.get("totalReportedCost"),
				study.get("totalTransfered"), study.get("expectedRevenue"), surveyor.get("firstName"),
				surveyor.get("lastName"), surveyor.get("login"), surveyor.get("ci"), surveyor.get("surveyToGoId"),
				surveyor.get("balance"));

		if (filter != null) {
			Predicate predicate = filter.toPredicate(root, query, cb);
			if (predicate != null) {
				query.where(predicate);
			}
		}
		query.orderBy(cb.asc(root.get("date")));

		List<JournalEntryReportDTO> result = new java.util.ArrayList<>();
		for (Object[] row : entityManager.createQuery(query).getResultList()) {
			result.add(new JournalEntryReportDTO((String) row[0], (String) row[1], (Date) row[2],
					(uy.com.bay.utiles.data.Operation) row[3], (Double) row[4], (uy.com.bay.utiles.data.Source) row[5],
					(String) row[6], (String) row[7], (String) row[8], (String) row[9], (String) row[10],
					(Double) row[11], (Double) row[12], (Double) row[13], (String) row[14], (String) row[15],
					(String) row[16], (String) row[17], (String) row[18], (Double) row[19]));
		}
		return result;
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
