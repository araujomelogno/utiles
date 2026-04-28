package uy.com.bay.utiles.views.supervision;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
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
	private final TextField alchemerStudyNameFilter = new TextField();
	private final ComboBox<Status> statusFilter = new ComboBox<>();

	private List<SupervisionTaskDTO> currentItems = List.of();

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
		Grid.Column<SupervisionTaskDTO> alchemerStudyNameColumn = grid
				.addColumn(SupervisionTaskDTO::getAlchemerStudyName).setHeader("Estudio");
		Grid.Column<SupervisionTaskDTO> fileNameColumn = grid.addColumn(SupervisionTaskDTO::getFileName)
				.setHeader("Archivo");
		Grid.Column<SupervisionTaskDTO> statusColumn = grid.addColumn(SupervisionTaskDTO::getStatus)
				.setHeader("Estado");
		grid.addColumn(SupervisionTaskDTO::getAiScore).setHeader("Scoring global");
		grid.addColumn(SupervisionTaskDTO::getScoreCobertura).setHeader("Cobertura");
		grid.addColumn(SupervisionTaskDTO::getScoreFidelidad).setHeader("Fidelidad");
		grid.addColumn(SupervisionTaskDTO::getScoreNeutralidad).setHeader("Neutralidad");
		grid.addColumn(SupervisionTaskDTO::getScoreFluidez).setHeader("Fluidez");
		grid.addColumn(SupervisionTaskDTO::getItemsEsperados).setHeader("Items Esperados");
		grid.addColumn(SupervisionTaskDTO::getItemsFaltantes).setHeader("Items Faltantes");
		grid.addColumn(SupervisionTaskDTO::getItemsEncontrados).setHeader("Items Encontrados");
		grid.addColumn(SupervisionTaskDTO::getProblemasMayores).setHeader("Problemas Mayores");
		grid.addColumn(SupervisionTaskDTO::getProblemasMenores).setHeader("Problemas Menores");
		grid.addColumn(SupervisionTaskDTO::getTotalAudioDuration).setHeader("Duración del audio");
		grid.addColumn(task -> {
			return Double.valueOf(task.getSpeakingDuration() * 100).intValue() + "%";
		}).setHeader("% hablando/ total");
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
				Anchor downloadLink = new Anchor(
						new StreamResource("Supervision_Report_" + task.getFileName() + ".docx",
								() -> wordExportService.generateSupervisionTaskReport(task)),
						"Descargar Word");
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

		alchemerStudyNameFilter.setValueChangeMode(ValueChangeMode.LAZY);
		alchemerStudyNameFilter.addValueChangeListener(e -> refreshGrid());
		headerRow.getCell(alchemerStudyNameColumn).setComponent(alchemerStudyNameFilter);

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
		Button exportButton = new Button("Exportar");
		exportButton.addClickListener(e -> exportToExcel());
		HorizontalLayout toolbar = new HorizontalLayout(fromDateField, toDateField, searchButton, exportButton);
		toolbar.setAlignItems(Alignment.BASELINE);
		return toolbar;
	}

	private void refreshGrid() {
		Date from = Date.from(fromDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

		Date to = Date.from(toDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
		if (from != null && to != null) {
			currentItems = supervisionTaskService.findDTOsByCreatedBetweenAndFileNameAndStatus(from, to,
					fileNameFilter.getValue(), alchemerStudyNameFilter.getValue(), statusFilter.getValue());
			grid.setItems(currentItems);
		}
	}

	private void exportToExcel() {
		try {
			StreamResource sr = new StreamResource("supervision-tasks.xlsx", () -> {
				try {
					return buildExcel(currentItems);
				} catch (IOException ex) {
					Notification.show("Error al generar el archivo Excel.", 3000, Notification.Position.TOP_CENTER);
					return null;
				}
			});
			Anchor anchor = new Anchor(sr, "");
			anchor.getElement().setAttribute("download", true);
			anchor.getStyle().set("display", "none");
			add(anchor);
			anchor.getElement().callJsFunction("click");
		} catch (Exception ex) {
			Notification.show("Error al exportar a Excel.", 3000, Notification.Position.TOP_CENTER);
		}
	}

	private ByteArrayInputStream buildExcel(List<SupervisionTaskDTO> data) throws IOException {
		String[] columns = { "Estudio", "Archivo", "Estado", "Scoring global", "Cobertura", "Fidelidad", "Neutralidad",
				"Fluidez", "Items Esperados", "Items Faltantes", "Items Encontrados", "Problemas Mayores",
				"Problemas Menores", "Duración del audio", "% hablando", "Duración/ participante", "Creada", "Output" };
		java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("dd/MM/yyyy");

		java.util.LinkedHashSet<String> scoreKeys = new java.util.LinkedHashSet<>();
		java.util.LinkedHashSet<String> coincidenceKeys = new java.util.LinkedHashSet<>();
		for (SupervisionTaskDTO dto : data) {
			if (dto.getScoreByItem() != null) {
				scoreKeys.addAll(dto.getScoreByItem().keySet());
			}
			if (dto.getCoincidenceByItem() != null) {
				coincidenceKeys.addAll(dto.getCoincidenceByItem().keySet());
			}
		}

		Map<String, Integer> scoreColumnIndex = new java.util.HashMap<>();
		Map<String, Integer> coincidenceColumnIndex = new java.util.HashMap<>();

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Supervision Tasks");

			Row headerRow = sheet.createRow(0);
			for (int col = 0; col < columns.length; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(columns[col]);
			}

			int nextCol = columns.length;
			for (String key : scoreKeys) {
				scoreColumnIndex.put(key, nextCol);
				headerRow.createCell(nextCol).setCellValue("puntaje" + key);
				nextCol++;
				if (coincidenceKeys.contains(key)) {
					coincidenceColumnIndex.put(key, nextCol);
					headerRow.createCell(nextCol).setCellValue("coincidencia" + key);
					nextCol++;
				}
			}
			for (String key : coincidenceKeys) {
				if (!coincidenceColumnIndex.containsKey(key)) {
					coincidenceColumnIndex.put(key, nextCol);
					headerRow.createCell(nextCol).setCellValue("coincidencia" + key);
					nextCol++;
				}
			}

			int rowIdx = 1;
			for (SupervisionTaskDTO dto : data) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(dto.getAlchemerStudyName() != null ? dto.getAlchemerStudyName() : "");
				row.createCell(1).setCellValue(dto.getFileName() != null ? dto.getFileName() : "");
				row.createCell(2).setCellValue(dto.getStatus() != null ? dto.getStatus().name() : "");
				if (dto.getAiScore() != null) {
					row.createCell(3).setCellValue(dto.getAiScore());
				} else {
					row.createCell(3).setCellValue("");
				}
				row.createCell(4).setCellValue(dto.getScoreCobertura());
				row.createCell(5).setCellValue(dto.getScoreFidelidad());
				row.createCell(6).setCellValue(dto.getScoreNeutralidad());
				row.createCell(7).setCellValue(dto.getScoreFluidez());
				if (dto.getItemsEsperados() != null) {
					row.createCell(8).setCellValue(dto.getItemsEsperados());
				} else {
					row.createCell(8).setCellValue("");
				}
				if (dto.getItemsFaltantes() != null) {
					row.createCell(9).setCellValue(dto.getItemsFaltantes());
				} else {
					row.createCell(9).setCellValue("");
				}
				if (dto.getItemsEncontrados() != null) {
					row.createCell(10).setCellValue(dto.getItemsEncontrados());
				} else {
					row.createCell(10).setCellValue("");
				}
				row.createCell(11).setCellValue(dto.getProblemasMayores() != null ? dto.getProblemasMayores() : "");
				row.createCell(12).setCellValue(dto.getProblemasMenores() != null ? dto.getProblemasMenores() : "");
				if (dto.getTotalAudioDuration() != null) {
					row.createCell(13).setCellValue(dto.getTotalAudioDuration());
				} else {
					row.createCell(13).setCellValue("");
				}
				row.createCell(14).setCellValue(Double.valueOf(dto.getSpeakingDuration() * 100).intValue() + "%");

				Map<String, Double> speakers = dto.getDurationBySpeakers();
				String durationBySpeakers = "";
				if (speakers != null && dto.getSpeakingDuration() != null && dto.getSpeakingDuration() != 0) {
					durationBySpeakers = speakers.entrySet().stream()
							.map(entry -> entry.getKey() + ":"
									+ Double.valueOf(100 * entry.getValue() / dto.getSpeakingDuration()).intValue()
									+ "%")
							.collect(Collectors.joining(", "));
				}
				row.createCell(15).setCellValue(durationBySpeakers);
				row.createCell(16).setCellValue(dto.getCreated() != null ? dateFormatter.format(dto.getCreated()) : "");
				row.createCell(17).setCellValue(dto.getOutput() != null && !dto.getOutput().isEmpty() ? "Sí" : "No");

				if (dto.getScoreByItem() != null) {
					for (Map.Entry<String, Integer> entry : dto.getScoreByItem().entrySet()) {
						Integer colIdx = scoreColumnIndex.get(entry.getKey());
						if (colIdx != null && entry.getValue() != null) {
							row.createCell(colIdx).setCellValue(entry.getValue());
						}
					}
				}
				if (dto.getCoincidenceByItem() != null) {
					for (Map.Entry<String, String> entry : dto.getCoincidenceByItem().entrySet()) {
						Integer colIdx = coincidenceColumnIndex.get(entry.getKey());
						if (colIdx != null && entry.getValue() != null) {
							row.createCell(colIdx).setCellValue(entry.getValue());
						}
					}
				}
			}

			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
