package uy.com.bay.utiles.views.proyectos;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Operation;
import uy.com.bay.utiles.data.Source;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.views.expenses.ExpenseReportViewDialog;
import uy.com.bay.utiles.views.expensetransfer.ExpenseTransferViewDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JournalEntryDialog extends Dialog {

    private final ExpenseReportFileService expenseReportFileService;
    private final ExpenseTransferFileService expenseTransferFileService;

    public JournalEntryDialog(Study study, JournalEntryService journalEntryService,
                              ExpenseReportFileService expenseReportFileService,
                              ExpenseTransferFileService expenseTransferFileService) {
        this.expenseReportFileService = expenseReportFileService;
        this.expenseTransferFileService = expenseTransferFileService;
        setHeaderTitle("Movimientos de Gastos para: " + study.getName());

        Grid<JournalEntry> grid = new Grid<>(JournalEntry.class, false);
        setColumns(grid);

        List<JournalEntry> journalEntries = journalEntryService.findAllByStudy(study);
        grid.setItems(journalEntries);
        calculateSaldo(grid, journalEntries);

        VerticalLayout layout = new VerticalLayout(grid);
        add(layout);

        getFooter().add(new Button("Cerrar", e -> close()));
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setWidth("80%");
        setHeight("80%");
    }

    private void setColumns(Grid<JournalEntry> grid) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        grid.addColumn(entry -> sdf.format(entry.getDate())).setHeader("Date");
        grid.addComponentColumn(entry -> {
            Source source = entry.getSource();
            if (source == null) {
                return new Span(entry.getDetail());
            }
            Button link = new Button(entry.getDetail());
            link.addClickListener(e -> {
                if (source == Source.RENDICION) {
                    ExpenseReportViewDialog dialog = new ExpenseReportViewDialog(entry.getExpenseReport(),
                            expenseReportFileService);
                    dialog.open();
                } else if (source == Source.TRANSFERENCIA) {
                    ExpenseTransferViewDialog dialog = new ExpenseTransferViewDialog(entry.getTransfer(),
                            expenseTransferFileService);
                    dialog.open();
                }
            });
            return link;
        }).setHeader("Detail");
        grid.addColumn(JournalEntry::getAmount).setHeader("Amount");
        grid.addColumn(entry -> entry.getSurveyor() != null ? entry.getSurveyor().getName() : "")
                .setHeader("Surveyor");
        grid.addColumn(JournalEntry::getOperation).setHeader("Operation");
        grid.addColumn(entry -> "").setHeader("Saldo").setKey("saldoColumn");
    }

    private void calculateSaldo(Grid<JournalEntry> grid, Collection<JournalEntry> items) {
        java.util.Map<JournalEntry, Double> saldoMap = new java.util.HashMap<>();
        AtomicReference<Double> runningSaldo = new AtomicReference<>(0.0);

        List<JournalEntry> sortedItems = new ArrayList<>(items);
        // Assuming items are sorted by date, if not, they should be sorted here.

        for (JournalEntry entry : sortedItems) {
            double amount = entry.getAmount();
            if (entry.getOperation() == Operation.DEBITO) {
                runningSaldo.updateAndGet(v -> v + amount);
            } else {
                runningSaldo.updateAndGet(v -> v - amount);
            }
            saldoMap.put(entry, runningSaldo.get());
        }

        grid.getColumnByKey("saldoColumn").setRenderer(new com.vaadin.flow.data.renderer.TextRenderer<>(entry -> {
            return String.format("%.2f", saldoMap.get(entry));
        }));
    }
}
