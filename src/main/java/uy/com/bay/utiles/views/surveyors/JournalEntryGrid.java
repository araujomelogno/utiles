package uy.com.bay.utiles.views.surveyors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.grid.Grid;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Operation;

public class JournalEntryGrid extends Grid<JournalEntry> {

	public JournalEntryGrid() {
		super(JournalEntry.class, false);
		setColumns();
	}

	private void setColumns() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		addColumn(entry -> sdf.format(entry.getDate())).setHeader("Date");
		addColumn(JournalEntry::getDetail).setHeader("Detail");
		addColumn(JournalEntry::getAmount).setHeader("Amount");
		addColumn(entry -> entry.getStudy() != null ? entry.getStudy().getName() : "").setHeader("Study");
		addColumn(JournalEntry::getOperation).setHeader("Operation");
		addColumn(entry -> "").setHeader("Saldo").setKey("saldoColumn");
	}

	public void setJournalEntries(Collection<JournalEntry> items) {
		super.setItems(items);

		java.util.Map<JournalEntry, Double> saldoMap = new java.util.HashMap<>();
		AtomicReference<Double> runningSaldo = new AtomicReference<>(0.0);

		List<JournalEntry> sortedItems = new ArrayList<>(items);

		for (JournalEntry entry : sortedItems) {
			double amount = entry.getAmount();
			if (entry.getOperation() == Operation.DEBITO) {
				runningSaldo.updateAndGet(v -> v - amount);
			} else {
				runningSaldo.updateAndGet(v -> v + amount);
			}
			saldoMap.put(entry, runningSaldo.get());
		}

		getColumnByKey("saldoColumn").setRenderer(new com.vaadin.flow.data.renderer.TextRenderer<>(entry -> {
			return String.format("%.2f", saldoMap.get(entry));
		}));
	}
}
