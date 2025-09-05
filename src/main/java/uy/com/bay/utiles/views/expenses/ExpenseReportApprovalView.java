package uy.com.bay.utiles.views.expenses;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Predicate;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportStatus;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.utils.FormattingUtils;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Aprobar Rendiciones")
@Route(value = "expense-reports-approval", layout = MainLayout.class)
@RolesAllowed("GASTOS")
public class ExpenseReportApprovalView extends Div {

	private final ExpenseReportService expenseReportService;
	private final Grid<ExpenseReport> grid = new Grid<>(ExpenseReport.class, false);
	private final Filters filters;

	public ExpenseReportApprovalView(ExpenseReportService expenseReportService) {
		this.expenseReportService = expenseReportService;
		this.filters = new Filters();
		addClassName("expensereport-approval-view");
		setSizeFull();
		createGrid();
		add(createToolbar(), grid);
	}

	private HorizontalLayout createToolbar() {
		Button approveButton = new Button("Aprobar rendiciones", event -> approveSelectedReports());
		Button revokeButton = new Button("Rechazar rendiciones", event -> revokeSelectedReports());
		approveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		revokeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		HorizontalLayout toolbar = new HorizontalLayout(approveButton, revokeButton);
		toolbar.setWidthFull();
		toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
		return toolbar;
	}

	private void createGrid() {
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
		grid.setSelectionMode(Grid.SelectionMode.MULTI);

		Grid.Column<ExpenseReport> dateColumn = grid.addColumn(ExpenseReport::getDate).setHeader("Date")
				.setSortable(true);
		Grid.Column<ExpenseReport> surveyorColumn = grid.addColumn(report -> {
			if (report.getSurveyor() == null) {
				return "";
			}
			return report.getSurveyor().getFirstName() + " " + report.getSurveyor().getLastName();
		}).setHeader("Surveyor").setSortable(true);
		Grid.Column<ExpenseReport> studyColumn = grid.addColumn(report -> report.getStudy().getName())
				.setHeader("Study").setSortable(true);
		Grid.Column<ExpenseReport> amountColumn = grid
				.addColumn(report -> FormattingUtils.formatAmount(report.getAmount())).setHeader("Amount")
				.setSortable(true);
		Grid.Column<ExpenseReport> conceptColumn = grid
				.addColumn(report -> report.getConcept() != null ? report.getConcept().getName() : "")
				.setHeader("Concept").setSortable(true);
		Grid.Column<ExpenseReport> statusColumn = grid.addColumn(ExpenseReport::getExpenseStatus).setHeader("Status")
				.setSortable(true);

		grid.setDataProvider(new CallbackDataProvider<>(
				query -> expenseReportService.list(
						com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query),
						createSpecification(filters)).stream(),
				query -> (int) expenseReportService.count(createSpecification(filters))));

		// Create filters
		HeaderRow filterRow = grid.appendHeaderRow();

		// Date filter
		DatePicker dateFilter = new DatePicker();
		dateFilter.setPlaceholder("Filtrar por fecha...");
		dateFilter.addValueChangeListener(event -> {
			filters.setDate(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(dateColumn).setComponent(dateFilter);

		// Surveyor filter
		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar por encuestador...");
		surveyorFilter.addValueChangeListener(event -> {
			filters.setSurveyorName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		// Study filter
		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filtrar por estudio...");
		studyFilter.addValueChangeListener(event -> {
			filters.setStudyName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(studyColumn).setComponent(studyFilter);

		// Amount filter
		NumberField amountFilter = new NumberField();
		amountFilter.setPlaceholder("Filtrar por monto...");
		amountFilter.addValueChangeListener(event -> {
			filters.setAmount(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(amountColumn).setComponent(amountFilter);

		// Concept filter
		TextField conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Filtrar por concepto...");
		conceptFilter.addValueChangeListener(event -> {
			filters.setConceptName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(conceptColumn).setComponent(conceptFilter);

		// Status filter
		TextField statusFilter = new TextField();
		statusFilter.setPlaceholder("Filtrar por estado...");
		statusFilter.addValueChangeListener(event -> {
			filters.setStatus(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(statusColumn).setComponent(statusFilter);

		FooterRow footerRow = grid.appendFooterRow();
		updateFooter(footerRow, studyColumn, amountColumn);
	}

	private void approveSelectedReports() {
		expenseReportService.approveReports(grid.getSelectedItems());
		grid.getSelectionModel().deselectAll();
		UI.getCurrent().getPage().reload();
	}

	private void revokeSelectedReports() {
		expenseReportService.revokeReports(grid.getSelectedItems());
		grid.getSelectionModel().deselectAll();
		UI.getCurrent().getPage().reload();
	}

	private void updateFooter(FooterRow footerRow, Grid.Column<ExpenseReport> studyColumn,
			Grid.Column<ExpenseReport> amountColumn) {
		Specification<ExpenseReport> spec = createSpecification(filters);
		Double total = expenseReportService.sumAmount(spec);
		footerRow.getCell(studyColumn).setText("TOTAL");
		footerRow.getCell(amountColumn).setText(FormattingUtils.formatAmount(total));
	}

	private Specification<ExpenseReport> createSpecification(Filters filters) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("expenseStatus"), ExpenseReportStatus.INGRESADO));

			if (filters.getDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("date"), filters.getDate()));
			}
			if (filters.getSurveyorName() != null && !filters.getSurveyorName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("surveyor").get("lastName")),
						"%" + filters.getSurveyorName().toLowerCase() + "%"));
			}
			if (filters.getStudyName() != null && !filters.getStudyName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("study").get("name")),
						"%" + filters.getStudyName().toLowerCase() + "%"));
			}
			if (filters.getAmount() != null) {
				predicates.add(criteriaBuilder.equal(root.get("amount"), filters.getAmount()));
			}
			if (filters.getConceptName() != null && !filters.getConceptName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("concept").get("name")),
						"%" + filters.getConceptName().toLowerCase() + "%"));
			}
			if (filters.getStatus() != null && !filters.getStatus().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("expenseStatus").as(String.class)),
						"%" + filters.getStatus().toLowerCase() + "%"));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static class Filters {
		private String studyName;
		private String surveyorName;
		private LocalDate date;
		private Double amount;
		private String conceptName;
		private String status;

		public String getStudyName() {
			return studyName;
		}

		public void setStudyName(String studyName) {
			this.studyName = studyName;
		}

		public String getSurveyorName() {
			return surveyorName;
		}

		public void setSurveyorName(String surveyorName) {
			this.surveyorName = surveyorName;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public String getConceptName() {
			return conceptName;
		}

		public void setConceptName(String conceptName) {
			this.conceptName = conceptName;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}
}
