package uy.com.bay.utiles.views.proyectos;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Source;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.ExcelExportService;
import uy.com.bay.utiles.services.JournalEntryService;

public class JournalEntryDialog extends Dialog {

	private final ExcelExportService excelExportService;
	private final Study proyecto;
	private final List<JournalEntry> journalEntries;
	private final Map<JournalEntry, Double> balanceMap = new HashMap<>();

	public JournalEntryDialog(Study proyecto, JournalEntryService journalEntryService,
			ExcelExportService excelExportService) {
		this.excelExportService = excelExportService;
		this.proyecto = proyecto;
		this.journalEntries = journalEntryService.findAllByStudy(proyecto);
		this.journalEntries.sort(Comparator.comparing(JournalEntry::getDate));
		calculateBalances();

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		setWidth("80%");
		setHeight("90%");

		H2 title = new H2("Movimientos de " + proyecto.getName());
		title.getStyle().set("margin-top", "0");

		Grid<JournalEntry> grid = new Grid<>(JournalEntry.class, false);
		ListDataProvider<JournalEntry> dataProvider = new ListDataProvider<>(journalEntries);
		grid.setDataProvider(dataProvider);

		// Fecha
		Grid.Column<JournalEntry> dateColumn = grid
				.addColumn(entry -> new SimpleDateFormat("dd/MM/yyyy").format(entry.getDate())).setHeader("Fecha")
				.setSortable(true);

		// Monto
		Grid.Column<JournalEntry> amountColumn = grid
				.addColumn(new NumberRenderer<>(JournalEntry::getAmount, NumberFormat.getIntegerInstance(Locale.US)))
				.setHeader("Monto").setSortable(true);

		// Saldo
		Grid.Column<JournalEntry> balanceColumn = grid.addColumn(new NumberRenderer<>(this::getBalance, NumberFormat.getIntegerInstance(Locale.US)))
				.setHeader("Saldo").setSortable(true);

		// Movimiento
		Grid.Column<JournalEntry> sourceColumn = grid.addColumn(JournalEntry::getSource).setHeader("Movimiento")
				.setSortable(true);

		// Encuestador
		Grid.Column<JournalEntry> surveyorColumn = grid.addColumn(entry -> {
			if (entry.getSurveyor() != null) {
				return entry.getSurveyor().getFirstName() + " " + entry.getSurveyor().getLastName();
			}
			return "";
		}).setHeader("Encuestador").setSortable(true);

		// Add filters
		HeaderRow filterRow = grid.appendHeaderRow();

		// Date filter
		TextField dateFilter = new TextField();
		dateFilter.setPlaceholder("Filter by date");
		dateFilter.setValueChangeMode(ValueChangeMode.EAGER);
		dateFilter.addValueChangeListener(event -> dataProvider.addFilter(
				entry -> new SimpleDateFormat("dd/MM/yyyy").format(entry.getDate()).contains(event.getValue())));
		filterRow.getCell(dateColumn).setComponent(dateFilter);

		// Amount filter
		TextField amountFilter = new TextField();
		amountFilter.setPlaceholder("Filter by amount");
		amountFilter.setValueChangeMode(ValueChangeMode.EAGER);
		amountFilter.addValueChangeListener(event -> dataProvider.addFilter(
				entry -> String.valueOf(entry.getAmount()).contains(event.getValue())));
		filterRow.getCell(amountColumn).setComponent(amountFilter);

		// Balance filter
		TextField balanceFilter = new TextField();
		balanceFilter.setPlaceholder("Filter by balance");
		balanceFilter.setValueChangeMode(ValueChangeMode.EAGER);
		balanceFilter.addValueChangeListener(event -> dataProvider.addFilter(
				entry -> String.valueOf(getBalance(entry)).contains(event.getValue())));
		filterRow.getCell(balanceColumn).setComponent(balanceFilter);

		// Source filter
		TextField sourceFilter = new TextField();
		sourceFilter.setPlaceholder("Filter by source");
		sourceFilter.setValueChangeMode(ValueChangeMode.EAGER);
		sourceFilter.addValueChangeListener(event -> dataProvider.addFilter(
				entry -> entry.getSource().toString().toLowerCase().contains(event.getValue().toLowerCase())));
		filterRow.getCell(sourceColumn).setComponent(sourceFilter);

		// Surveyor filter
		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filter by surveyor");
		surveyorFilter.setValueChangeMode(ValueChangeMode.EAGER);
		surveyorFilter.addValueChangeListener(event -> dataProvider.addFilter(entry -> {
			if (entry.getSurveyor() != null) {
				return (entry.getSurveyor().getFirstName() + " " + entry.getSurveyor().getLastName()).toLowerCase()
						.contains(event.getValue().toLowerCase());
			}
			return false;
		}));
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);

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

	private void calculateBalances() {
		double currentBalance = 0;
		for (JournalEntry entry : journalEntries) {
			if (entry.getSource() == Source.TRANSFERENCIA) {
				currentBalance += entry.getAmount();
			} else if (entry.getSource() == Source.RENDICION || entry.getSource() == Source.LIQUIDACION) {
				currentBalance -= entry.getAmount();
			}
			balanceMap.put(entry, currentBalance);
		}
	}

	private Double getBalance(JournalEntry entry) {
		return balanceMap.getOrDefault(entry, 0.0);
	}

	private StreamResource getStreamResource() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String filename = "journal_entries_" + proyecto.getName().replaceAll("\\s+", "_") + "_" + sdf.format(new Date())
				+ ".xlsx";
		return new StreamResource(filename, (outputStream, vaadinSession) -> {
			try {
				java.io.ByteArrayInputStream inputStream = excelExportService.exportJournalEntriesToExcel(journalEntries);
				inputStream.transferTo(outputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
