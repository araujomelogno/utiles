package uy.com.bay.utiles.views.proyectos;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.ExcelExportService;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;

public class JournalEntryDialog extends Dialog {

	private final JournalEntryService journalEntryService;
	private final ExpenseReportFileService expenseReportFileService;
	private final ExpenseTransferFileService expenseTransferFileService;
	private final ExcelExportService excelExportService;
	private final Study proyecto;

	public JournalEntryDialog(Study proyecto, JournalEntryService journalEntryService,
			ExpenseReportFileService expenseReportFileService, ExpenseTransferFileService expenseTransferFileService,
			ExcelExportService excelExportService) {
		this.journalEntryService = journalEntryService;
		this.expenseReportFileService = expenseReportFileService;
		this.expenseTransferFileService = expenseTransferFileService;
		this.excelExportService = excelExportService;
		this.proyecto = proyecto;

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		setWidth("80%");
		setHeight("90%");

		H2 title = new H2("Movimientos de " + proyecto.getName());
		title.getStyle().set("margin-top", "0");

		Grid<JournalEntry> grid = new Grid<>(JournalEntry.class, false);
		grid.addColumn("date").setHeader("Fecha");
		grid.addColumn("debit").setHeader("Debe");
		grid.addColumn("credit").setHeader("Haber");
		grid.addColumn("description").setHeader("Descripción");
		grid.setItems(journalEntryService.getJournalEntries(proyecto));

		Button closeButton = new Button("Cerrar", e -> close());

		Anchor exportButton = new Anchor(getStreamResource(), "Exportar");
		exportButton.setClassName("vaadin-button");

		getFooter().add(exportButton, closeButton);

		VerticalLayout layout = new VerticalLayout(title, grid);
		layout.setSizeFull();
		layout.setPadding(false);
		layout.setSpacing(false);

		add(layout);
	}

	private StreamResource getStreamResource() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String filename = "journal_entries_" + proyecto.getName().replaceAll("\\s+", "_") + "_" + sdf.format(new Date())
				+ ".xlsx";
		return new StreamResource(filename, () -> {
			try {
				return excelExportService.exportJournalEntriesToExcel(journalEntryService.getJournalEntries(proyecto));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
