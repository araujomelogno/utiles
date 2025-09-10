package uy.com.bay.utiles.views.expenses;

import java.util.Collections;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.User;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.MainLayout;
import uy.com.bay.utiles.views.surveyors.JournalEntryGrid;

@PageTitle("Saldo de Gastos")
@Route(value = "surveyor-journal-entry", layout = MainLayout.class)
@RolesAllowed({ "ADMIN", "ENCUESTADORES" })
public class SurveyorJournalEntryView extends Div {

	private final JournalEntryGrid grid;
	private final Grid<ExpenseRequest> expenseRequestGrid = new Grid<>(ExpenseRequest.class, false);
	private final ExpenseRequestService expenseRequestService;

	public SurveyorJournalEntryView(AuthenticatedUser authenticatedUser, SurveyorService surveyorService,
			JournalEntryService journalEntryService, ExpenseTransferFileService expenseTransferFileService,
			ExpenseReportFileService expenseReportFileService, ExpenseRequestService expenseRequestService) {
		this.expenseRequestService = expenseRequestService;

		addClassName("surveyor-journal-entry-view");
		setSizeFull();

		Button expenseRequestButton = new Button("Ingresar solicitud de gasto");
		expenseRequestButton.addClickListener(e -> UI.getCurrent().navigate(SurveyorExpenseRequestEntry.class));

		Button expenseReportButton = new Button("Ingresar rendición");
		expenseReportButton.addClickListener(e -> UI.getCurrent().navigate(SurveyorExpenseReportEntry.class));

		HorizontalLayout buttonLayout = new HorizontalLayout(expenseRequestButton, expenseReportButton);

		configureExpenseRequestGrid();

		grid = new JournalEntryGrid(expenseTransferFileService, expenseReportFileService);
		grid.addClassNames("journal-entry-grid");
		grid.setSizeFull();

		VerticalLayout mainLayout = new VerticalLayout(buttonLayout,
				new Label("Solicitudes ingresadas pendientes de aprobación"), expenseRequestGrid,
				new Label("Cuenta de gastos transferidos"), grid);
		mainLayout.setSizeFull();

		add(mainLayout);
		updateList(authenticatedUser, surveyorService, journalEntryService);
	}

	private void configureExpenseRequestGrid() {
		expenseRequestGrid.addColumn(ExpenseRequest::getId).setHeader("Id");
		expenseRequestGrid.addColumn(er -> er.getSurveyor().getFirstName()).setHeader("Encuestador");
		expenseRequestGrid.addColumn(ExpenseRequest::getAmount).setHeader("Monto");
		expenseRequestGrid.addColumn(ExpenseRequest::getExpenseStatus).setHeader("Estado");
		expenseRequestGrid.addColumn(ExpenseRequest::getRequestDate).setHeader("Fecha de Creación");
	}

	private void updateList(AuthenticatedUser authenticatedUser, SurveyorService surveyorService,
			JournalEntryService journalEntryService) {
		Optional<User> maybeUser = authenticatedUser.get();
		if (maybeUser.isPresent()) {
			User user = maybeUser.get();
			Optional<Surveyor> surveyor = surveyorService.findByName(user.getUsername());
			if (surveyor.isPresent()) {
				grid.setJournalEntries(journalEntryService.findBySurveyor(surveyor.get()));
				expenseRequestGrid.setItems(
						expenseRequestService.findAllBySurveyorAndStatus(surveyor.get(), ExpenseStatus.INGRESADO));
			} else {
				grid.setJournalEntries(Collections.emptyList());
				expenseRequestGrid.setItems(Collections.emptyList());
			}
		} else {
			grid.setJournalEntries(Collections.emptyList());
			expenseRequestGrid.setItems(Collections.emptyList());
		}
	}
}
