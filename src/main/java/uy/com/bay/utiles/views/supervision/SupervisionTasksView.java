package uy.com.bay.utiles.views.supervision;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.service.SupervisionTaskService;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;
import uy.com.bay.utiles.services.WordExportService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Supervision Tasks")
@Route(value = "supervision-tasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionTasksView extends VerticalLayout {

	private final SupervisionTaskService supervisionTaskService;
	private final WordExportService wordExportService;

	private final DatePicker fromDateField = new DatePicker("Desde:");
	private final DatePicker toDateField = new DatePicker("Hasta:");
	private final Grid<SupervisionTaskDTO> grid = new Grid<>(SupervisionTaskDTO.class, false);
	private final TextField fileNameFilter = new TextField();
	private final ComboBox<Status> statusFilter = new ComboBox<>();

	public SupervisionTasksView(SupervisionTaskService supervisionTaskService, WordExportService wordExportService) {
		this.supervisionTaskService = supervisionTaskService;
		this.wordExportService = wordExportService;
		setSizeFull();
		configureGrid();
		add(getToolbar(), grid);
		toDateField.setValue(LocalDateTime.now().plusDays(1).toLocalDate());
		fromDateField.setValue(LocalDateTime.now().minusDays(7).toLocalDate());
		refreshGrid();
	}

	private void configureGrid() {
		grid.setSizeFull();
		Grid.Column<SupervisionTaskDTO> fileNameColumn = grid.addColumn(SupervisionTaskDTO::getFileName).setHeader("Archivo");
		Grid.Column<SupervisionTaskDTO> statusColumn = grid.addColumn(SupervisionTaskDTO::getStatus).setHeader("Estado");
		grid.addColumn(SupervisionTaskDTO::getAiScore).setHeader("Scoring");
		grid.addColumn(SupervisionTaskDTO::getTotalAudioDuration).setHeader("Duración del audio");
		grid.addColumn(SupervisionTaskDTO::getSpeakingDuration).setHeader("Duración hablando");
		grid.addColumn(task -> {
			Map<String, Double> speakers = task.getDurationBySpeakers();
			if (speakers != null) {
				return speakers.entrySet().stream()
						.map(entry -> entry.getKey() + ":"
								+ Double.valueOf(100 * entry.getValue() / task.getSpeakingDuration()).intValue() + "%")
						.collect(Collectors.joining(", "));
			}
			return "";
		}).setHeader("Duración/ participante");

		grid.addColumn(task -> {
			if (task.getCreated() != null) {
				return new java.text.SimpleDateFormat("dd/MM/yyyy").format(task.getCreated());
			}
			return "";
		}).setHeader("Creada").setSortable(true);

		grid.addComponentColumn(task -> {
			if (task.getOutput() != null && !task.getOutput().isEmpty()) {
				Anchor downloadLink = new Anchor(new StreamResource("Supervision_Report_" + task.getId() + ".docx",
						() -> wordExportService.generateSupervisionTaskReport(task)), "Descargar Word");
				downloadLink.getElement().setAttribute("download", true);
				return downloadLink;
			} else {
				Button button = new Button("No Output");
				button.setEnabled(false);
				return button;
			}
		}).setHeader("Output").setSortable(true)
				.setComparator(task -> task.getOutput() != null && !task.getOutput().isEmpty());

		HeaderRow headerRow = grid.appendHeaderRow();

		fileNameFilter.setValueChangeMode(ValueChangeMode.LAZY);
		fileNameFilter.addValueChangeListener(e -> refreshGrid());
		headerRow.getCell(fileNameColumn).setComponent(fileNameFilter);

		statusFilter.setItems(Status.values());
		statusFilter.addValueChangeListener(e -> refreshGrid());
		headerRow.getCell(statusColumn).setComponent(statusFilter);

		grid.getColumns().forEach(col -> col.setSortable(true));
		grid.getColumns().forEach(c -> c.setResizable(true));

	}

	private HorizontalLayout getToolbar() {
		Button searchButton = new Button("Buscar");
		searchButton.addClickListener(e -> refreshGrid());
		HorizontalLayout toolbar = new HorizontalLayout(fromDateField, toDateField, searchButton);
		toolbar.setAlignItems(Alignment.BASELINE);
		return toolbar;
	}

	private void refreshGrid() {
		Date from = Date.from(fromDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

		Date to = Date.from(toDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
		if (from != null && to != null) {
			grid.setItems(supervisionTaskService.findDTOByCreatedBetweenAndFileNameAndStatus(from, to,
					fileNameFilter.getValue(), statusFilter.getValue()));
		}
	}
}
