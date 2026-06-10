package uy.com.bay.utiles.views.meta;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.StudyService;

@PageTitle("Costos Meta")
@Route("meta-costs")
@RolesAllowed("ADMIN")
public class MetaCostsView extends Div {

	private final StudyService studyService;
	private final DatePicker fromDate;
	private final DatePicker toDate;
	private final Grid<StudyMetaCostRow> grid = new Grid<>(StudyMetaCostRow.class, false);
	private final NumberFormat currencyFormat;
	private ListDataProvider<StudyMetaCostRow> dataProvider;
	private final TextField studyNameFilter = new TextField();
	private final NumberField totalCostFilter = new NumberField();

	public MetaCostsView(StudyService studyService) {
		this.studyService = studyService;
		currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "UY"));
		currencyFormat.setMinimumFractionDigits(0);
		currencyFormat.setMaximumFractionDigits(0);

		addClassNames("meta-costs-view");
		setSizeFull();

		int currentYear = LocalDate.now().getYear();
		fromDate = new DatePicker("Desde");
		fromDate.setValue(LocalDate.of(currentYear, 1, 1));
		toDate = new DatePicker("Hasta");
		toDate.setValue(LocalDate.of(currentYear, 12, 31));

		fromDate.addValueChangeListener(e -> refreshGrid());
		toDate.addValueChangeListener(e -> refreshGrid());

		HorizontalLayout filterLayout = new HorizontalLayout(fromDate, toDate);
		filterLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);

		Grid.Column<StudyMetaCostRow> studyColumn = grid.addColumn(StudyMetaCostRow::getStudyName)
				.setHeader("Estudio").setAutoWidth(true).setSortable(true).setKey("studyName");
		Grid.Column<StudyMetaCostRow> costColumn = grid.addColumn(row -> formatCurrency(row.getTotalCost()))
				.setHeader("Costo").setAutoWidth(true).setSortable(true).setKey("totalCost")
				.setComparator(Comparator.comparingDouble(StudyMetaCostRow::getTotalCost));
		grid.addComponentColumn(row -> {
			Button detailButton = new Button(new Icon(VaadinIcon.SEARCH));
			detailButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			detailButton.addClickListener(e -> openDetailDialog(row));
			return detailButton;
		}).setHeader("Ver detalle").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		grid.setSizeFull();

		dataProvider = new ListDataProvider<>(new ArrayList<>());
		grid.setItems(dataProvider);

		HeaderRow filterRow = grid.appendHeaderRow();
		studyNameFilter.setPlaceholder("Filtrar...");
		studyNameFilter.setClearButtonVisible(true);
		studyNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		studyNameFilter.setWidthFull();
		studyNameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
		filterRow.getCell(studyColumn).setComponent(studyNameFilter);

		totalCostFilter.setPlaceholder("Mínimo");
		totalCostFilter.setClearButtonVisible(true);
		totalCostFilter.setValueChangeMode(ValueChangeMode.EAGER);
		totalCostFilter.setWidthFull();
		totalCostFilter.addValueChangeListener(e -> dataProvider.refreshAll());
		filterRow.getCell(costColumn).setComponent(totalCostFilter);

		dataProvider.addFilter(this::matchesFilters);

		VerticalLayout wrapper = new VerticalLayout(filterLayout, grid);
		wrapper.setSizeFull();
		wrapper.setFlexGrow(1, grid);
		add(wrapper);

		refreshGrid();
	}

	private boolean matchesFilters(StudyMetaCostRow row) {
		String nameFilterValue = studyNameFilter.getValue();
		if (nameFilterValue != null && !nameFilterValue.isBlank()) {
			String name = row.getStudyName() == null ? "" : row.getStudyName();
			if (!name.toLowerCase(Locale.ROOT).contains(nameFilterValue.toLowerCase(Locale.ROOT))) {
				return false;
			}
		}
		Double minCost = totalCostFilter.getValue();
		if (minCost != null && row.getTotalCost() < minCost) {
			return false;
		}
		return true;
	}

	private void refreshGrid() {
		dataProvider.getItems().clear();
		dataProvider.getItems().addAll(loadRows());
		dataProvider.refreshAll();
	}

	private List<StudyMetaCostRow> loadRows() {
		LocalDate from = fromDate.getValue();
		LocalDate to = toDate.getValue();
		if (from == null || to == null) {
			return List.of();
		}
		Date fromDateValue = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date toDateValue = Date.from(to.atStartOfDay(ZoneId.systemDefault()).toInstant());

		List<Study> studies = studyService.findAllWithMetaCostBetween(fromDateValue, toDateValue);
		List<StudyMetaCostRow> rows = new ArrayList<>();
		for (Study study : studies) {
			Map<Date, Double> costs = study.getMetaCostByDate();
			if (costs == null || costs.isEmpty()) {
				continue;
			}
			double total = 0d;
			List<Map.Entry<Date, Double>> filteredEntries = new ArrayList<>();
			for (Map.Entry<Date, Double> entry : costs.entrySet()) {
				Date key = entry.getKey();
				Double value = entry.getValue();
				if (key == null || value == null) {
					continue;
				}
				if (!key.before(fromDateValue) && !key.after(toDateValue)) {
					total += value;
					filteredEntries.add(entry);
				}
			}
			if (filteredEntries.isEmpty()) {
				continue;
			}
			rows.add(new StudyMetaCostRow(study, total, filteredEntries));
		}
		rows.sort(Comparator.comparing(StudyMetaCostRow::getStudyName, String.CASE_INSENSITIVE_ORDER));
		return rows;
	}

	private void openDetailDialog(StudyMetaCostRow row) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Detalle de costos - " + row.getStudyName());
		dialog.setWidth("520px");

		Grid<Map.Entry<Date, Double>> detailGrid = new Grid<>();
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		detailGrid.addColumn(entry -> entry.getKey() == null ? ""
				: java.time.Instant.ofEpochMilli(entry.getKey().getTime()).atZone(ZoneId.systemDefault()).toLocalDate()
						.format(dateFormatter))
				.setHeader("Fecha").setAutoWidth(true);
		detailGrid.addColumn(entry -> formatCurrency(entry.getValue())).setHeader("Costo").setAutoWidth(true);

		List<Map.Entry<Date, Double>> entries = new ArrayList<>(row.getEntries());
		entries.sort(Comparator.comparing(Map.Entry::getKey));
		detailGrid.setItems(entries);
		detailGrid.setAllRowsVisible(true);

		VerticalLayout content = new VerticalLayout(new H3(row.getStudyName()), detailGrid);
		content.setPadding(false);
		dialog.add(content);

		Button close = new Button("Cerrar", e -> dialog.close());
		dialog.getFooter().add(close);
		dialog.open();
	}

	private String formatCurrency(Double value) {
		if (value == null) {
			return "";
		}
		return currencyFormat.format(value);
	}

	private static class StudyMetaCostRow {
		private final Study study;
		private final double totalCost;
		private final List<Map.Entry<Date, Double>> entries;

		StudyMetaCostRow(Study study, double totalCost, List<Map.Entry<Date, Double>> entries) {
			this.study = study;
			this.totalCost = totalCost;
			this.entries = entries;
		}

		public String getStudyName() {
			return study.getName();
		}

		public double getTotalCost() {
			return totalCost;
		}

		public List<Map.Entry<Date, Double>> getEntries() {
			return entries;
		}
	}
}
