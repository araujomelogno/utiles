package uy.com.bay.utiles.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.User;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.surveyors.JournalEntryGrid;

import java.util.Collections;
import java.util.Optional;

@PageTitle("Saldo de Gastos")
@Route(value = "surveyor-journal-entry", layout = MainLayout.class)
@PermitAll
public class SurveyorJournalEntryView extends Div {

    private final JournalEntryGrid grid;

    public SurveyorJournalEntryView(AuthenticatedUser authenticatedUser, SurveyorService surveyorService,
                                    JournalEntryService journalEntryService,
                                    ExpenseTransferFileService expenseTransferFileService,
                                    ExpenseReportFileService expenseReportFileService) {

        addClassName("surveyor-journal-entry-view");
        setSizeFull();
        grid = new JournalEntryGrid(expenseTransferFileService, expenseReportFileService);
        grid.addClassNames("journal-entry-grid");
        grid.setSizeFull();
        add(grid);
        updateList(authenticatedUser, surveyorService, journalEntryService);
    }

    private void updateList(AuthenticatedUser authenticatedUser, SurveyorService surveyorService, JournalEntryService journalEntryService) {
        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Optional<Surveyor> surveyor = surveyorService.findByName(user.getName());
            if (surveyor.isPresent()) {
                grid.setJournalEntries(journalEntryService.findBySurveyor(surveyor.get()));
            } else {
                grid.setJournalEntries(Collections.emptyList());
            }
        } else {
            grid.setJournalEntries(Collections.emptyList());
        }
    }
}
