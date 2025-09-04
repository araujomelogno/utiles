package uy.com.bay.utiles.views.surveyors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Operation;
import uy.com.bay.utiles.data.Source;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.utils.FormattingUtils;
import uy.com.bay.utiles.views.expenses.ExpenseReportViewDialog;
import uy.com.bay.utiles.views.expensetransfer.ExpenseTransferViewDialog;

public class JournalEntryGrid extends Grid<JournalEntry> {

	private final ExpenseTransferFileService expenseTransferFileService;
	private final ExpenseReportFileService expenseReportFileService;

	public JournalEntryGrid(ExpenseTransferFileService expenseTransferFileService,
			ExpenseReportFileService expenseReportFileService) {
		super(JournalEntry.class, false);
		this.expenseTransferFileService = expenseTransferFileService;
		this.expenseReportFileService = expenseReportFileService;
		setColumns();
		this.getColumns().forEach(col -> {
			col.setAutoWidth(true); // Ajusta según contenido
			col.setFlexGrow(0); // No se expande más allá de lo necesario
		});
	}

	private void setColumns() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		addColumn(entry -> sdf.format(entry.getDate())).setHeader("Fecha");
		addComponentColumn(this::createDetailLink).setHeader("Detalle");
		addColumn(entry -> {
			double amount = entry.getAmount();
			if (entry.getOperation() == Operation.CREDITO) {
				amount *= -1;
			}
			return FormattingUtils.formatAmount(amount);
		}).setHeader("Monto");
		addColumn(entry -> entry.getStudy() != null ? entry.getStudy().getName() : "").setHeader("Estudio");
		addColumn(JournalEntry::getOperation).setHeader("Movimiento");
		addColumn(entry -> "").setHeader("Saldo").setKey("saldoColumn");
	}

	private Button createDetailLink(JournalEntry entry) {
		Button link = new Button(entry.getDetail());
		link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		if (entry.getSource() != null) {
			if (entry.getSource() == Source.RENDICION && entry.getExpenseReport() != null) {
				link.addClickListener(e -> {
					ExpenseReportViewDialog dialog = new ExpenseReportViewDialog(entry.getExpenseReport(),
							expenseReportFileService);
					dialog.open();
				});
			} else if (entry.getSource() == Source.TRANSFERENCIA && entry.getTransfer() != null) {
				link.addClickListener(e -> {
					ExpenseTransferViewDialog dialog = new ExpenseTransferViewDialog(entry.getTransfer(),
							expenseTransferFileService);
					dialog.open();
				});
			}
		}
		return link;
	}

	public void setJournalEntries(Collection<JournalEntry> items) {
		super.setItems(items);

		java.util.Map<JournalEntry, Double> saldoMap = new java.util.HashMap<>();
		AtomicReference<Double> runningSaldo = new AtomicReference<>(0.0);

		List<JournalEntry> sortedItems = new ArrayList<>(items);

		for (JournalEntry entry : sortedItems) {
			double amount = entry.getAmount();
			if (entry.getOperation() == Operation.CREDITO) {
				runningSaldo.updateAndGet(v -> v - amount);
			} else {
				runningSaldo.updateAndGet(v -> v + amount);
			}
			saldoMap.put(entry, runningSaldo.get());
		}

		getColumnByKey("saldoColumn").setRenderer(new com.vaadin.flow.data.renderer.TextRenderer<>(
				entry -> FormattingUtils.formatAmount(saldoMap.get(entry))));
	}
}
