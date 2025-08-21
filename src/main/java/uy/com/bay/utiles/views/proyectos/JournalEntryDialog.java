package uy.com.bay.utiles.views.proyectos;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Operation;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.JournalEntryService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JournalEntryDialog extends Dialog {

    public JournalEntryDialog(Study study, JournalEntryService journalEntryService) {
        setHeaderTitle("Movimientos de Gastos para: " + study.getName());

        Grid<JournalEntry> grid = new Grid<>(JournalEntry.class, false);
        setColumns(grid);

        List<JournalEntry> journalEntries = journalEntryService.findAllByStudy(study);
        grid.setItems(journalEntries);
        calculateSaldo(grid, journalEntries);

        VerticalLayout layout = new VerticalLayout(grid);
        add(layout);

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setWidth("80%");
        setHeight("80%");
    }

    private void setColumns(Grid<JournalEntry> grid) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        grid.addColumn(entry -> sdf.format(entry.getDate())).setHeader("Date");
        grid.addColumn(JournalEntry::getDetail).setHeader("Detail");
        grid.addColumn(JournalEntry::getAmount).setHeader("Amount");
        grid.addColumn(entry -> entry.getSurveyor() != null ? entry.getSurveyor().getName() : "").setHeader("Surveyor");
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
