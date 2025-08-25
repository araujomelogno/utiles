package uy.com.bay.utiles.views.expenses;

import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportStatus;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Aprobar Rendiciones")
@Route(value = "expense-reports-approval", layout = MainLayout.class)
@RolesAllowed("GASTOS")
public class ExpenseReportApprovalView extends Div {

	private final ExpenseReportService expenseReportService;
	private final Grid<ExpenseReport> grid = new Grid<>(ExpenseReport.class, false);
	private ListDataProvider<ExpenseReport> dataProvider;

	public ExpenseReportApprovalView(ExpenseReportService expenseReportService) {
		this.expenseReportService = expenseReportService;
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
		Grid.Column<ExpenseReport> surveyorColumn = grid
				.addColumn(report -> report.getSurveyor().getFirstName() + " " + report.getSurveyor().getLastName())
				.setHeader("Surveyor").setSortable(true);
		Grid.Column<ExpenseReport> studyColumn = grid.addColumn(report -> report.getStudy().getName())
				.setHeader("Study").setSortable(true);
		Grid.Column<ExpenseReport> amountColumn = grid.addColumn(ExpenseReport::getAmount).setHeader("Amount")
				.setSortable(true);
		Grid.Column<ExpenseReport> conceptColumn = grid
				.addColumn(report -> report.getConcept() != null ? report.getConcept().getName() : "")
				.setHeader("Concept").setSortable(true);
		Grid.Column<ExpenseReport> statusColumn = grid.addColumn(ExpenseReport::getExpenseStatus).setHeader("Status")
				.setSortable(true);

		// Fetch and set data
		List<ExpenseReport> reports = expenseReportService.findAllByExpenseStatus(ExpenseReportStatus.INGRESADO);
		reports.sort(Comparator.comparing(ExpenseReport::getDate, Comparator.nullsFirst(Comparator.naturalOrder()))
				.reversed());
		dataProvider = new ListDataProvider<>(reports);
		grid.setDataProvider(dataProvider);

		// Create filters
		HeaderRow filterRow = grid.appendHeaderRow();

		// Date filter
		TextField dateFilter = new TextField();
		dateFilter.setPlaceholder("Filter by date...");
		dateFilter.addValueChangeListener(
				event -> dataProvider.addFilter(report -> report.getDate().toString().contains(event.getValue())));
		filterRow.getCell(dateColumn).setComponent(dateFilter);

		// Surveyor filter
		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filter by surveyor...");
		surveyorFilter.addValueChangeListener(event -> dataProvider
				.addFilter(report -> (report.getSurveyor().getFirstName() + " " + report.getSurveyor().getLastName())
						.toLowerCase().contains(event.getValue().toLowerCase())));
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		// Study filter
		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filter by study...");
		studyFilter.addValueChangeListener(event -> dataProvider.addFilter(
				report -> report.getStudy().getName().toLowerCase().contains(event.getValue().toLowerCase())));
		filterRow.getCell(studyColumn).setComponent(studyFilter);

		// Amount filter
		TextField amountFilter = new TextField();
		amountFilter.setPlaceholder("Filter by amount...");
		amountFilter.addValueChangeListener(
				event -> dataProvider.addFilter(report -> report.getAmount().toString().contains(event.getValue())));
		filterRow.getCell(amountColumn).setComponent(amountFilter);

		// Concept filter
		TextField conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Filter by concept...");
		conceptFilter.addValueChangeListener(event -> dataProvider.addFilter(report -> {
			if (report.getConcept() == null) {
				return event.getValue().isEmpty();
			}
			return report.getConcept().getName().toLowerCase().contains(event.getValue().toLowerCase());
		}));
		filterRow.getCell(conceptColumn).setComponent(conceptFilter);

		// Status filter
		TextField statusFilter = new TextField();
		statusFilter.setPlaceholder("Filter by status...");
		statusFilter.addValueChangeListener(event -> dataProvider.addFilter(
				report -> report.getExpenseStatus().toString().toLowerCase().contains(event.getValue().toLowerCase())));
		filterRow.getCell(statusColumn).setComponent(statusFilter);

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
}
