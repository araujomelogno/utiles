package uy.com.bay.utiles.services;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportStatus;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.repository.JournalEntryRepository;
import uy.com.bay.utiles.data.Operation;
import uy.com.bay.utiles.data.Source;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.StudyRepository;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.SurveyorRepository;
import uy.com.bay.utiles.data.repository.ExpenseReportRepository;

@Service
public class ExpenseReportService {

	private final ExpenseReportRepository repository;
	private final JournalEntryRepository journalEntryRepository;
	private final SurveyorRepository surveyorRepository;
	private final StudyRepository studyRepository;

	public ExpenseReportService(ExpenseReportRepository repository, JournalEntryRepository journalEntryRepository,
			SurveyorRepository surveyorRepository, StudyRepository studyRepository) {
		this.repository = repository;
		this.journalEntryRepository = journalEntryRepository;
		this.surveyorRepository = surveyorRepository;
		this.studyRepository = studyRepository;
	}

	public Optional<ExpenseReport> get(Long id) {
		return repository.findById(id);
	}

	public ExpenseReport update(ExpenseReport entity) {
		return repository.save(entity);
	}

	public ExpenseReport save(ExpenseReport entity) {
		return repository.save(entity);
	}

	public void delete(Long id) {
		repository.deleteById(id);
	}

	public Page<ExpenseReport> list(Pageable pageable) {
		return repository.findAll(pageable);
	}

	public Page<ExpenseReport> list(Pageable pageable, Specification<ExpenseReport> filter) {
		return repository.findAll(filter, pageable);
	}

	public List<ExpenseReport> findAllByExpenseStatus(ExpenseReportStatus status) {
		return repository.findAllByExpenseStatus(status);
	}

	public void approveReports(Collection<ExpenseReport> reports) {
		reports.forEach(report -> {
			this.approveReport(report);
		});
	}

	public void approveReport(ExpenseReport report) {
		report.setExpenseStatus(ExpenseReportStatus.APROBADO);
		report.setApprovalDate(new Date());
		repository.save(report);
		JournalEntry journalEntry = new JournalEntry();
		journalEntry.setDetail("rendicion aprobada por el concepto " + report.getConcept().getName());
		journalEntry.setDate(new Date());
		journalEntry.setOperation(Operation.CREDITO);
		journalEntry.setAmount(report.getAmount());
		journalEntry.setSurveyor(report.getSurveyor());
		journalEntry.setStudy(report.getStudy());
		journalEntry.setSource(Source.RENDICION);

		journalEntry.setExpenseReport(report);
		journalEntryRepository.save(journalEntry);
		Surveyor surveyor = report.getSurveyor();
		surveyor.setBalance(surveyor.getBalance() - report.getAmount());
		surveyorRepository.save(surveyor);
		Study study = report.getStudy();
		study.setTotalReportedCost(study.getTotalReportedCost() + report.getAmount());
		studyRepository.save(study);
	}

	public void revokeReports(Collection<ExpenseReport> reports) {
		reports.forEach(report -> {
			report.setExpenseStatus(ExpenseReportStatus.RECHAZADO);
			report.setApprovalDate(new Date());
			repository.save(report);
		});
	}

	public long count(Specification<ExpenseReport> filter) {
		return repository.count(filter);
	}

	public int count() {
		return (int) repository.count();
	}
}
