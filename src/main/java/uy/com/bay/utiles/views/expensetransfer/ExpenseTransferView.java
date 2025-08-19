package uy.com.bay.utiles.views.expensetransfer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.Pageable;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseTransferService;
import uy.com.bay.utiles.views.MainLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
@PageTitle("Transferir Solicitudes")
@Route(value = "expense-transfer", layout = MainLayout.class)
@PermitAll
public class ExpenseTransferView extends VerticalLayout {
	private final ExpenseRequestService expenseRequestService;
	private final ExpenseTransferService expenseTransferService;
	private Grid<ExpenseRequest> grid;
	private Button transferButton;
	private ListDataProvider<ExpenseRequest> dataProvider;
	public ExpenseTransferView(ExpenseRequestService expenseRequestService,
			ExpenseTransferService expenseTransferService) {
		this.expenseRequestService = expenseRequestService;
		this.expenseTransferService = expenseTransferService;
		addClassName("expensetransfer-view");
		setSizeFull();
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setWidthFull();
		createTransferButton();
		buttonLayout.add(transferButton);
		createGrid();
		add(buttonLayout, grid);
		refreshGrid();
	}
	private void createGrid() {
		grid = new Grid<>(ExpenseRequest.class, false);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		Grid.Column<ExpenseRequest> surveyorColumn = grid
				.addColumn(er -> er.getSurveyor() != null ? er.getSurveyor().getName() : "").setHeader("Encuestador")
				.setSortable(true).setKey("surveyor.lastName");
		Grid.Column<ExpenseRequest> studyColumn = grid
				.addColumn(er -> er.getStudy() != null ? er.getStudy().getName() : "").setHeader("Proyecto")
				.setSortable(true).setKey("study.name");
		grid.addColumn(ExpenseRequest::getRequestDate).setHeader("Fecha Solicitud").setSortable(true)
				.setKey("requestDate");
		grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto").setSortable(true).setKey("amount");
		Grid.Column<ExpenseRequest> conceptColumn = grid
				.addColumn(er -> er.getConcept() != null ? er.getConcept().getDescription() : "").setHeader("Concepto")
				.setSortable(true).setKey("concept.description");
		Grid.Column<ExpenseRequest> obsColumn = grid.addColumn(ExpenseRequest::getObs).setHeader("Observaciones");
		HeaderRow filterRow = grid.appendHeaderRow();
		// Surveyor filter
		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.addValueChangeListener(event -> {
			String filterValue = event.getValue().toLowerCase();
			dataProvider.addFilter(report -> {
				if (report.getSurveyor() == null) {
					return filterValue.isEmpty();
				}
				String surveyorName = (report.getSurveyor().getFirstName() + " "
						+ report.getSurveyor().getLastName()).toLowerCase();
				return surveyorName.contains(filterValue);
			});
		});
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);
		// Study filter
		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filtrar");
		studyFilter.setClearButtonVisible(true);
		studyFilter.addValueChangeListener(event -> {
			String filterValue = event.getValue().toLowerCase();
			dataProvider.addFilter(report -> {
				if (report.getStudy() == null) {
					return filterValue.isEmpty();
				}
				return report.getStudy().getName().toLowerCase().contains(filterValue);
			});
		});
		filterRow.getCell(studyColumn).setComponent(studyFilter);
		// Concept filter
		TextField conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Filtrar");
		conceptFilter.setClearButtonVisible(true);
		conceptFilter.addValueChangeListener(event -> {
			String filterValue = event.getValue().toLowerCase();
			dataProvider.addFilter(report -> {
				if (report.getConcept() == null || report.getConcept().getDescription() == null) {
					return filterValue.isEmpty();
				}
				return report.getConcept().getDescription().toLowerCase().contains(filterValue);
			});
		});
		filterRow.getCell(conceptColumn).setComponent(conceptFilter);
		// Obs filter
		TextField obsFilter = new TextField();
		obsFilter.setPlaceholder("Filtrar");
		obsFilter.setClearButtonVisible(true);
		obsFilter.addValueChangeListener(event -> {
			String filterValue = event.getValue().toLowerCase();
			dataProvider.addFilter(request -> {
				if (request.getObs() == null) {
					return filterValue.isEmpty();
				}
				return request.getObs().toLowerCase().contains(filterValue);
			});
		});
		filterRow.getCell(obsColumn).setComponent(obsFilter);
		grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER);
		grid.asMultiSelect().addValueChangeListener(event -> {
			transferButton.setEnabled(!event.getValue().isEmpty());
		});
	}
	private void createTransferButton() {
		transferButton = new Button("Transferir solicitudes");
		transferButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		transferButton.setEnabled(false);
		transferButton.addClickListener(e -> {
			if (!grid.getSelectedItems().isEmpty()) {
				ExpenseTransferDialog dialog = new ExpenseTransferDialog(grid.getSelectedItems());
				dialog.addListener(ExpenseTransferDialog.SaveEvent.class, this::saveTransfer);
				dialog.open();
			}
		});
	}
	private void saveTransfer(ExpenseTransferDialog.SaveEvent event) {
		ExpenseTransfer expenseTransfer = event.getExpenseTransfer();
		List<ExpenseRequest> requestsToUpdate = new ArrayList<>(expenseTransfer.getExpenseRequests());
		Date now = new Date();
		expenseTransfer.setTransferDate(now);
		expenseTransfer.setExpenseRequests(new ArrayList<>());
		expenseTransfer = expenseTransferService.save(expenseTransfer);
		for (ExpenseRequest request : requestsToUpdate) {
			request.setExpenseStatus(ExpenseStatus.TRANSFERIDO);
			request.setTransferDate(now);
			request.setExpenseTransfer(expenseTransfer);
			expenseRequestService.update(request);
		}
		refreshGrid();
		Notification.show("Transferencia creada exitosamente.", 3000, Notification.Position.BOTTOM_START);
	}
	private void refreshGrid() {
		List<ExpenseRequest> requests = expenseRequestService
				.findAllByExpenseStatus(ExpenseStatus.APROBADO, Pageable.unpaged()).getContent().stream()
				.sorted(Comparator.comparing(ExpenseRequest::getRequestDate).reversed()).collect(Collectors.toList());
		dataProvider = new ListDataProvider<>(requests);
		grid.setDataProvider(dataProvider);
	}
}
