package uy.com.bay.utiles.tasks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.data.service.FieldworkService;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.OdooCost;
import uy.com.bay.utiles.services.AlchemerSurveyResponseHelper;
import uy.com.bay.utiles.services.OdooCostService;
import uy.com.bay.utiles.services.OdooService;

@Component
public class FieldworkUpdateTask {

	private static final Logger logger = LoggerFactory.getLogger(FieldworkUpdateTask.class);

	private final FieldworkRepository fieldworkRepository;
	private final FieldworkService fieldworkService;
	private final AlchemerSurveyResponseHelper alchemerSurveyResponseHelper;
	private final DoobloSurveyRetriever doobloSurveyRetriever;
	private final OdooService odooService;
	private final OdooCostService odooCostService;

	public FieldworkUpdateTask(FieldworkRepository fieldworkRepository, FieldworkService fieldworkService,
			AlchemerSurveyResponseHelper alchemerSurveyResponseHelper, DoobloSurveyRetriever doobloSurveyRetriever,
			OdooService odooService, OdooCostService odooCostService) {
		this.fieldworkRepository = fieldworkRepository;
		this.fieldworkService = fieldworkService;
		this.alchemerSurveyResponseHelper = alchemerSurveyResponseHelper;
		this.doobloSurveyRetriever = doobloSurveyRetriever;
		this.odooService = odooService;
		this.odooCostService = odooCostService;
	}

	@PostConstruct
	public void init() {
		logger.info("[SCHED] FieldworkUpdateTask bean initialized");
	}

	@Scheduled(cron = "0 0 8 * * *")
	@Transactional
	public void updateFieldworks() {
		logger.info("Starting FieldworkUpdateTask...");

		LocalDate today = LocalDate.now();
		LocalDate upperInit = today.plusDays(10);
		LocalDate lowerEnd = today.minusDays(10);

		List<Fieldwork> fieldworks = fieldworkRepository
				.findAllByInitPlannedDateLessThanAndEndPlannedDateGreaterThan(upperInit, lowerEnd);

		logger.info("FieldworkUpdateTask: found {} fieldwork(s) to process", fieldworks.size());

		Set<Long> processedBudgetEntries = new HashSet<>();
		Set<Long> processedBudgets = new HashSet<>();

		for (Fieldwork fieldwork : fieldworks) {
			try {
				BudgetEntry budgetEntry = fieldwork.getBudgetEntry();
				if (budgetEntry != null && budgetEntry.getId() != null
						&& processedBudgetEntries.add(budgetEntry.getId())) {
					refreshSpentFromAlchemerAndDooblo(budgetEntry);
				}

				Study study = fieldwork.getStudy();
				if (study == null || study.getBudget() == null) {
					continue;
				}
				Budget budget = study.getBudget();
				if (budget.getId() != null && !processedBudgets.add(budget.getId())) {
					continue;
				}
				refreshCostsFromOdoo(study);
			} catch (Exception e) {
				logger.error("FieldworkUpdateTask: error processing fieldwork id={}", fieldwork.getId(), e);
			}
		}

		logger.info("FieldworkUpdateTask finished.");
	}

	private void refreshSpentFromAlchemerAndDooblo(BudgetEntry budgetEntry) {
		if (budgetEntry.getFieldworks() == null) {
			return;
		}
		for (Fieldwork fieldwork : budgetEntry.getFieldworks()) {
			if (fieldwork.getInitPlannedDate() == null || fieldwork.getEndPlannedDate() == null) {
				continue;
			}
			Date initDate = Date.from(fieldwork.getInitPlannedDate().minusMonths(1)
					.atStartOfDay(ZoneId.systemDefault()).toInstant());
			Date endDate = Date.from(fieldwork.getEndPlannedDate().plusMonths(3)
					.atStartOfDay(ZoneId.systemDefault()).toInstant());
			if (fieldwork.getAlchemerId() != null && !fieldwork.getAlchemerId().isEmpty()) {
				Map<Date, Integer> completedSurveys = alchemerSurveyResponseHelper
						.getCompletedSurveys(fieldwork.getAlchemerId(), initDate, endDate);
				fieldwork.setCompletedByMonth(completedSurveys);
				fieldworkService.save(fieldwork);
			} else if (fieldwork.getDoobloId() != null && !fieldwork.getDoobloId().isEmpty()) {
				Map<Date, Integer> completedSurveys = doobloSurveyRetriever
						.getCompletedSurveys(fieldwork.getDoobloId(), initDate, endDate);
				fieldwork.setCompletedByMonth(completedSurveys);
				fieldworkService.save(fieldwork);
			}
		}
	}

	private void refreshCostsFromOdoo(Study study) {
		if (study.getOdooId() == null || study.getOdooId().isEmpty()) {
			return;
		}
		Budget budget = study.getBudget();
		if (budget == null || budget.getEntries() == null) {
			return;
		}
		for (BudgetEntry budgetEntry : budget.getEntries()) {
			BudgetConcept concept = budgetEntry.getConcept();
			if (concept == null || concept.getOdooProductId() == null || concept.getOdooProductId().isEmpty()) {
				continue;
			}
			odooCostService.deleteByBudgetEntry(budgetEntry);
			List<Map<String, Object>> moveLines = odooService.getOdooAccountMoveLines(study.getOdooId(),
					concept.getOdooProductId(), budgetEntry.getInit(), budgetEntry.getEnd());
			for (Map<String, Object> line : moveLines) {
				String moveId = odooIdToString(line.get("move_id"));
				if (moveId == null || moveId.isEmpty()) {
					continue;
				}
				if (odooCostService.findByMoveId(moveId).isPresent()) {
					continue;
				}
				OdooCost cost = new OdooCost();
				cost.setDate(odooValueToLocalDate(line.get("date")));
				cost.setMoveId(moveId);
				cost.setName(odooValueToString(line.get("name")));
				cost.setProductId(odooIdToString(line.get("product_id")));
				cost.setAccountId(odooIdToString(line.get("account_id")));
				cost.setDebit(odooValueToBigDecimal(line.get("debit")));
				cost.setCredit(odooValueToBigDecimal(line.get("credit")));
				cost.setBalance(odooValueToBigDecimal(line.get("balance")));
				cost.setBudgetEntry(budgetEntry);
				odooCostService.save(cost);
			}
		}
	}

	private String odooIdToString(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return null;
		}
		if (value instanceof Object[]) {
			Object[] arr = (Object[]) value;
			return arr.length > 0 && arr[0] != null ? String.valueOf(arr[0]) : null;
		}
		if (value instanceof List) {
			List<?> list = (List<?>) value;
			return !list.isEmpty() && list.get(0) != null ? String.valueOf(list.get(0)) : null;
		}
		return String.valueOf(value);
	}

	private String odooValueToString(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return null;
		}
		return String.valueOf(value);
	}

	private BigDecimal odooValueToBigDecimal(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return null;
		}
		if (value instanceof Number) {
			return BigDecimal.valueOf(((Number) value).doubleValue());
		}
		return new BigDecimal(String.valueOf(value));
	}

	private LocalDate odooValueToLocalDate(Object value) {
		if (value == null || Boolean.FALSE.equals(value)) {
			return null;
		}
		return LocalDate.parse(String.valueOf(value));
	}
}
