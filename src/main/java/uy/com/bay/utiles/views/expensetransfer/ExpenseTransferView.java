package uy.com.bay.utiles.views.expensetransfer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.views.MainLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.ExpenseTransferService;

@PageTitle("Transferir Solicitudes")
@Route(value = "expense-transfer", layout = MainLayout.class)
@PermitAll
public class ExpenseTransferView extends VerticalLayout {

	private final ExpenseRequestService expenseRequestService;
	private final ExpenseTransferService expenseTransferService;
	private final ExpenseTransferFileService expenseTransferFileService;

	private Grid<ExpenseRequest> grid;
	private Button transferButton;

	private TextField surveyorFilter;
	private TextField studyFilter;
	private TextField conceptFilter;
	private TextField obsFilter;

	public ExpenseTransferView(ExpenseRequestService expenseRequestService,
			ExpenseTransferService expenseTransferService, ExpenseTransferFileService expenseTransferFileService) {
		this.expenseRequestService = expenseRequestService;
		this.expenseTransferService = expenseTransferService;
		this.expenseTransferFileService = expenseTransferFileService;
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

		surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.addValueChangeListener(e -> refreshGrid());
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		studyFilter = new TextField();
		studyFilter.setPlaceholder("Filtrar");
		studyFilter.setClearButtonVisible(true);
		studyFilter.addValueChangeListener(e -> refreshGrid());
		filterRow.getCell(studyColumn).setComponent(studyFilter);

		conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Filtrar");
		conceptFilter.setClearButtonVisible(true);
		conceptFilter.addValueChangeListener(e -> refreshGrid());
		filterRow.getCell(conceptColumn).setComponent(conceptFilter);

		obsFilter = new TextField();
		obsFilter.setPlaceholder("Filtrar");
		obsFilter.setClearButtonVisible(true);
		obsFilter.addValueChangeListener(e -> refreshGrid());
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
		grid.setItems(query -> {
			List<QuerySortOrder> sortOrders = query.getSortOrders();
			Sort sort;
			if (sortOrders.isEmpty()) {
				sort = Sort.by(Sort.Direction.DESC, "requestDate");
			} else {
				QuerySortOrder order = sortOrders.get(0);
				sort = Sort.by(
						order.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC,
						order.getSorted());
			}

			PageRequest pageRequest = PageRequest.of(query.getPage(), query.getPageSize(), sort);

			Specification<ExpenseRequest> spec = (root, q, cb) -> cb.equal(root.get("expenseStatus"),
					ExpenseStatus.APROBADO);

			if (surveyorFilter != null && !surveyorFilter.isEmpty()) {
				spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("surveyor").get("lastName")),
						"%" + surveyorFilter.getValue().toLowerCase() + "%"));
			}
			if (studyFilter != null && !studyFilter.isEmpty()) {
				spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("study").get("name")),
						"%" + studyFilter.getValue().toLowerCase() + "%"));
			}
			if (conceptFilter != null && !conceptFilter.isEmpty()) {
				spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("concept").get("description")),
						"%" + conceptFilter.getValue().toLowerCase() + "%"));
			}
			if (obsFilter != null && !obsFilter.isEmpty()) {
				spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("obs")),
						"%" + obsFilter.getValue().toLowerCase() + "%"));
			}

			return expenseRequestService.list(pageRequest, spec).stream();
		});
	}
}
