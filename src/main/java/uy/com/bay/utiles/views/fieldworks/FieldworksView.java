package uy.com.bay.utiles.views.fieldworks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.FieldworkStatus;
import uy.com.bay.utiles.data.FieldworkType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.service.FieldworkService;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetEntryService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.tasks.FieldworkUpdateTask;

@PageTitle("Solicitudes de campo")
@Route("fieldworks/:fieldworkID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class FieldworksView extends Div implements BeforeEnterObserver {

	private final String FIELDWORK_ID = "fieldworkID";
	private final String FIELDWORK_EDIT_ROUTE_TEMPLATE = "fieldworks/%s/edit";

	private final Grid<Fieldwork> grid = new Grid<>(Fieldwork.class, false);

	private MultiSelectComboBox<String> doobloId;
	private MultiSelectComboBox<String> alchemerId;
	private ComboBox<Study> study;
	private ComboBox<uy.com.bay.utiles.entities.BudgetEntry> budgetEntry;
	private DatePicker initPlannedDate;
	private DatePicker endPlannedDate;

	private IntegerField goalQuantity;
	private IntegerField completed;
	private TextArea obs;
	private ComboBox<FieldworkStatus> status;
	private ComboBox<FieldworkType> type;

	private ComboBox<Study> studyFilter;
	private ComboBox<FieldworkStatus> statusFilter;
	private ComboBox<FieldworkType> typeFilter;
	private DatePicker fromDateFilter;
	private DatePicker toDateFilter;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Eliminar");

	private final BeanValidationBinder<Fieldwork> binder;

	private Fieldwork fieldwork;
	private Div editorLayoutDiv;
	private final NumberFormat currencyFormat;
	private final FieldworkService fieldworkService;
	private final StudyService studyService;
	private final BudgetEntryService budgetEntryService;
	private final FieldworkUpdateTask fieldworkUpdateTask;

	public FieldworksView(FieldworkService fieldworkService, StudyService studyService,
			BudgetEntryService budgetEntryService, FieldworkUpdateTask fieldworkUpdateTask) {
		this.fieldworkService = fieldworkService;
		this.studyService = studyService;
		this.budgetEntryService = budgetEntryService;
		this.fieldworkUpdateTask = fieldworkUpdateTask;
		currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "UY"));
		currencyFormat.setMinimumFractionDigits(0);
		currencyFormat.setMaximumFractionDigits(0);

		addClassNames("fieldworks-view");
		setHeight("100%");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(70);

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid
		grid.addColumn(fieldwork -> fieldwork.getStudy() != null
				? fieldwork.getStudy().getName().substring(0, Math.min(40, fieldwork.getStudy().getName().length()))
				: "").setHeader("Estudio").setAutoWidth(true);
		grid.addColumn("obs").setHeader("Observaciones").setAutoWidth(true);
		grid.addColumn("initPlannedDate").setHeader("Fecha Planificada Inicio").setAutoWidth(true);
		grid.addColumn("endPlannedDate").setHeader("Fecha Planificada Fin").setAutoWidth(true);
		grid.addColumn("goalQuantity").setHeader("Cantidad Objetivo").setAutoWidth(true);
		grid.addColumn("completed").setHeader("Completadas").setAutoWidth(true);
		grid.addColumn("status").setHeader("Estado").setAutoWidth(true);
		grid.addColumn("type").setHeader("Tipo").setAutoWidth(true);
		grid.addColumn(fw -> formatCurrency(getBudgetedCost(fw))).setHeader("Costo presupuestado").setAutoWidth(true);
		grid.addColumn(fw -> formatCurrency(getActualCost(fw))).setHeader("Costo actual").setAutoWidth(true);

		grid.setItems(query -> fieldworkService
				.listWithBudget(VaadinSpringDataHelpers.toSpringPageRequest(query), buildSpecification()).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		grid.setSizeFull();

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(FIELDWORK_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(FieldworksView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Fieldwork.class);
		binder.forField(doobloId)
				.<List<String>>withConverter(set -> set == null ? new ArrayList<>() : new ArrayList<>(set),
						list -> list == null ? new LinkedHashSet<>() : new LinkedHashSet<>(list))
				.bind(Fieldwork::getDoobloId, Fieldwork::setDoobloId);
		binder.forField(alchemerId)
				.<List<String>>withConverter(set -> set == null ? new ArrayList<>() : new ArrayList<>(set),
						list -> list == null ? new LinkedHashSet<>() : new LinkedHashSet<>(list))
				.bind(Fieldwork::getAlchemerId, Fieldwork::setAlchemerId);
		binder.forField(budgetEntry).bind(Fieldwork::getBudgetEntry, Fieldwork::setBudgetEntry);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.fieldwork == null) {
					this.fieldwork = new Fieldwork();
				}

				binder.writeBean(this.fieldwork);

				if (this.fieldwork.getInitPlannedDate() == null) {
					Notification.show("Debe seleccionar fecha planificada de inicio .", 3000,
							Notification.Position.MIDDLE);
					return;
				}

				if (this.fieldwork.getEndPlannedDate() == null) {
					Notification.show("Debe seleccionar fecha planificada de fin .", 3000,
							Notification.Position.MIDDLE);
					return;
				}

				if (this.fieldwork.getBudgetEntry() == null) {
					Notification.show("Debe seleccionar un Presupuesto.", 3000, Notification.Position.MIDDLE);
					return;
				}

				if (this.fieldwork.getType() == null) {
					Notification.show("Debe seleccionar el tipo campo.", 3000, Notification.Position.MIDDLE);
					return;
				}
				fieldworkService.save(this.fieldwork);
				if (this.fieldwork.getStudy() != null
						&& !this.fieldwork.getStudy().getFieldworks().contains(this.fieldwork)) {
					this.fieldwork.getStudy().getFieldworks().add(this.fieldwork);
					this.studyService.save(this.fieldwork.getStudy());
				}

				if (this.fieldwork.getStudy() != null && this.fieldwork.getStudy().getBudget() != null
						&& this.fieldwork.getStudy().getBudget().getEntries() != null) {
					for (BudgetEntry entry : this.fieldwork.getStudy().getBudget().getEntries()) {
						this.budgetEntryService.updateDates(entry.getId(), this.fieldwork.getInitPlannedDate(),
								this.fieldwork.getEndPlannedDate());
					}
				}

				clearForm();
				refreshGrid();
				Notification.show("Solicitud de campo guardada.");
				UI.getCurrent().navigate(FieldworksView.class);
			} catch (ValidationException validationException) {
				Notification.show("No se pudo guardar la solicitud. Verifique los campos.");
			}
		});

		delete.addClickListener(e -> {
			if (this.fieldwork == null || this.fieldwork.getId() == null) {
				Notification.show("No hay ninguna solicitud de campo seleccionada para eliminar.", 4000,
						Notification.Position.MIDDLE);
				return;
			}
			Long idToDelete = this.fieldwork.getId();
			try {
				fieldworkService.delete(idToDelete);
				if (fieldworkService.get(idToDelete).isPresent()) {
					Notification.show(
							"La solicitud de campo no pudo eliminarse (sigue existiendo en la base de datos).", 6000,
							Notification.Position.MIDDLE);
					return;
				}
				clearForm();
				refreshGrid();
				Notification.show("Solicitud de campo eliminada.");
				UI.getCurrent().navigate(FieldworksView.class);
			} catch (Exception ex) {
				Notification.show("No se pudo eliminar la solicitud de campo: " + ex.getMessage(), 6000,
						Notification.Position.MIDDLE);
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> fieldworkId = event.getRouteParameters().get(FIELDWORK_ID).map(Long::parseLong);
		if (fieldworkId.isPresent()) {
			Optional<Fieldwork> fieldworkFromBackend = fieldworkService.get(fieldworkId.get());
			if (fieldworkFromBackend.isPresent()) {
				populateForm(fieldworkFromBackend.get());
			} else {
				Notification.show(
						String.format("La solicitud de campo con id = %s no fue encontrada", fieldworkId.get()), 3000,
						Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(FieldworksView.class);
			}
		} else {
			Optional.ofNullable(event.getLocation().getQueryParameters().getParameters().get("studyId"))
					.flatMap(list -> list.stream().findFirst()).ifPresent(studyId -> {
						try {
							Optional<Study> study = studyService.get(Long.parseLong(studyId));
							if (study.isPresent()) {
								clearForm();
								this.fieldwork = new Fieldwork();
								this.fieldwork.setStudy(study.get());
								binder.readBean(this.fieldwork);
								this.editorLayoutDiv.setVisible(true);
							} else {
								Notification.show("El estudio no fue encontrado.", 3000,
										Notification.Position.BOTTOM_START);
							}
						} catch (NumberFormatException e) {
							Notification.show("Id de estudio invalido.", 3000, Notification.Position.BOTTOM_START);
						}
					});
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		this.editorLayoutDiv = new Div();

		this.editorLayoutDiv.setClassName("editor-layout");
		this.editorLayoutDiv.setWidth("30%");
		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		this.editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		doobloId = createCustomValueMultiSelect("Dooblo Id");
		alchemerId = createCustomValueMultiSelect("Alchemer Id");
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.listAll());
		study.setItemLabelGenerator(Study::getName);
		study.addValueChangeListener(event -> {
			if (event.getValue() != null && event.getValue().getBudget() != null) {
				budgetEntry.setItems(event.getValue().getBudget().getEntries());
			} else {
				budgetEntry.clear();
			}
		});
		budgetEntry = new ComboBox<>("Presupuesto");
		budgetEntry.setItemLabelGenerator(be -> be.getConcept() != null ? be.getConcept().getName() : "N/A");
		initPlannedDate = new DatePicker("Fecha Planificada Inicio");
		endPlannedDate = new DatePicker("Fecha Planificada Fin");

		goalQuantity = new IntegerField("Cantidad Objetivo");
		completed = new IntegerField("Completas");
		completed.setReadOnly(true);
		obs = new TextArea("Observaciones");
		status = new ComboBox<>("Estado");
		status.setItems(FieldworkStatus.values());
		type = new ComboBox<>("Tipo");
		type.setItems(FieldworkType.values());

		formLayout.add(study, budgetEntry, doobloId, alchemerId, initPlannedDate, endPlannedDate, goalQuantity,
				completed, status, type, obs);

		editorDiv.add(formLayout);
		createButtonLayout(this.editorLayoutDiv);

		splitLayout.addToSecondary(this.editorLayoutDiv);
		this.editorLayoutDiv.setVisible(false);
	}

	private MultiSelectComboBox<String> createCustomValueMultiSelect(String label) {
		MultiSelectComboBox<String> combo = new MultiSelectComboBox<>(label);
		combo.setAllowCustomValue(true);
		combo.setItems(new ArrayList<>());
		combo.addCustomValueSetListener(event -> {
			String customValue = event.getDetail();
			if (customValue == null || customValue.isBlank()) {
				return;
			}
			Set<String> selected = new LinkedHashSet<>(combo.getValue());
			selected.add(customValue);
			// The selected items must be present in the data provider, otherwise the
			// newly typed value would be discarded when the selection is applied.
			combo.setItems(new ArrayList<>(selected));
			combo.setValue(selected);
		});
		return combo;
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		buttonLayout.add(save, delete, cancel);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setClassName("grid-wrapper");
		wrapper.setSizeFull();
		wrapper.setPadding(false);
		wrapper.setSpacing(false);

		Button addButton = new Button("Agregar Solicitud");
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.addClickListener(e -> {
			clearForm();
			this.fieldwork = new Fieldwork();
			binder.readBean(this.fieldwork);
			this.editorLayoutDiv.setVisible(true);
		});

		Button updateButton = new Button("Actualizar Campos");
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.addClickListener(e -> {
			try {
				fieldworkUpdateTask.updateFieldworks();
				refreshGrid();
				Notification.show("Actualización de campos finalizada.", 3000, Notification.Position.BOTTOM_START);
			} catch (Exception ex) {
				Notification.show("Error al actualizar campos: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
			}
		});

		Button exportButton = new Button("Exportar");
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		exportButton.addClickListener(e -> exportToExcel());

		studyFilter = new ComboBox<>("Estudio");
		studyFilter.setItems(studyService.listAll());
		studyFilter.setItemLabelGenerator(Study::getName);
		studyFilter.setClearButtonVisible(true);
		studyFilter.addValueChangeListener(e -> refreshGrid());

		statusFilter = new ComboBox<>("Estado");
		statusFilter.setItems(FieldworkStatus.values());
		statusFilter.setClearButtonVisible(true);
		statusFilter.addValueChangeListener(e -> refreshGrid());

		typeFilter = new ComboBox<>("Tipo");
		typeFilter.setItems(FieldworkType.values());
		typeFilter.setClearButtonVisible(true);
		typeFilter.addValueChangeListener(e -> refreshGrid());

		int currentYear = LocalDate.now().getYear();
		fromDateFilter = new DatePicker("Desde");
		fromDateFilter.setValue(LocalDate.of(currentYear, 1, 1));
		fromDateFilter.setClearButtonVisible(true);
		fromDateFilter.addValueChangeListener(e -> refreshGrid());

		toDateFilter = new DatePicker("Hasta");
		toDateFilter.setValue(LocalDate.of(currentYear, 12, 31));
		toDateFilter.setClearButtonVisible(true);
		toDateFilter.addValueChangeListener(e -> refreshGrid());

		HorizontalLayout filterLayout = new HorizontalLayout(studyFilter, statusFilter, typeFilter, fromDateFilter,
				toDateFilter, addButton, updateButton, exportButton);
		filterLayout.setWidth("100%");
		filterLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);

		wrapper.add(filterLayout, grid);
		wrapper.setFlexGrow(1, grid);
		splitLayout.addToPrimary(wrapper);
	}

	private Specification<Fieldwork> buildSpecification() {
		Specification<Fieldwork> spec = (root, q, cb) -> cb.and();
		if (studyFilter != null && studyFilter.getValue() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("study"), studyFilter.getValue()));
		}
		if (statusFilter != null && statusFilter.getValue() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), statusFilter.getValue()));
		}
		if (typeFilter != null && typeFilter.getValue() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), typeFilter.getValue()));
		}
		if (fromDateFilter != null && fromDateFilter.getValue() != null) {
			LocalDate fromDate = fromDateFilter.getValue();
			spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("initPlannedDate"), fromDate));
		}
		if (toDateFilter != null && toDateFilter.getValue() != null) {
			LocalDate toDate = toDateFilter.getValue();
			spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("endPlannedDate"), toDate));
		}
		return spec;
	}

	private Double getBudgetedCost(Fieldwork fw) {
		if (fw == null || fw.getBudgetEntry() == null || fw.getBudgetEntry().getBudget() == null) {
			return null;
		}
		return fw.getBudgetEntry().getBudget().getTotal();
	}

	private Double getActualCost(Fieldwork fw) {
		if (fw == null || fw.getBudgetEntry() == null || fw.getBudgetEntry().getBudget() == null) {
			return null;
		}
		return fw.getBudgetEntry().getBudget().getSpent();
	}

	private String formatCurrency(Double value) {
		if (value == null) {
			return "";
		}
		return currencyFormat.format(value);
	}

	private void exportToExcel() {
		try {
			StreamResource sr = new StreamResource("solicitudes-de-campo.xlsx", () -> {
				try {
					List<Fieldwork> data = fieldworkService.listWithBudget(Pageable.unpaged(), buildSpecification())
							.getContent();
					return buildExcel(data);
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

	private ByteArrayInputStream buildExcel(List<Fieldwork> data) throws IOException {
		String[] columns = { "Estudio", "Observaciones", "Fecha Planificada Inicio", "Fecha Planificada Fin",
				"Cantidad Objetivo", "Completadas", "Estado", "Tipo", "Costo presupuestado", "Costo actual" };
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Solicitudes de campo");

			Font boldFont = workbook.createFont();
			boldFont.setBold(true);
			CellStyle boldStyle = workbook.createCellStyle();
			boldStyle.setFont(boldFont);

			int rowIdx = 0;

			Row title = sheet.createRow(rowIdx++);
			Cell titleCell = title.createCell(0);
			titleCell.setCellValue("Filtros aplicados");
			titleCell.setCellStyle(boldStyle);

			rowIdx = writeFilterRow(sheet, rowIdx, boldStyle, "Estudio",
					studyFilter.getValue() != null ? studyFilter.getValue().getName() : "Todos");
			rowIdx = writeFilterRow(sheet, rowIdx, boldStyle, "Estado",
					statusFilter.getValue() != null ? statusFilter.getValue().toString() : "Todos");
			rowIdx = writeFilterRow(sheet, rowIdx, boldStyle, "Tipo",
					typeFilter.getValue() != null ? typeFilter.getValue().toString() : "Todos");
			rowIdx = writeFilterRow(sheet, rowIdx, boldStyle, "Desde",
					fromDateFilter.getValue() != null ? fromDateFilter.getValue().format(dateFormatter) : "");
			rowIdx = writeFilterRow(sheet, rowIdx, boldStyle, "Hasta",
					toDateFilter.getValue() != null ? toDateFilter.getValue().format(dateFormatter) : "");

			rowIdx++;

			Row headerRow = sheet.createRow(rowIdx++);
			for (int col = 0; col < columns.length; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(columns[col]);
				cell.setCellStyle(boldStyle);
			}

			for (Fieldwork fw : data) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(fw.getStudy() != null ? fw.getStudy().getName() : "");
				row.createCell(1).setCellValue(fw.getObs() != null ? fw.getObs() : "");
				row.createCell(2).setCellValue(
						fw.getInitPlannedDate() != null ? fw.getInitPlannedDate().format(dateFormatter) : "");
				row.createCell(3).setCellValue(
						fw.getEndPlannedDate() != null ? fw.getEndPlannedDate().format(dateFormatter) : "");
				row.createCell(4).setCellValue(fw.getGoalQuantity() != null ? fw.getGoalQuantity() : 0);
				row.createCell(5).setCellValue(fw.getCompleted() != null ? fw.getCompleted() : 0);
				row.createCell(6).setCellValue(fw.getStatus() != null ? fw.getStatus().toString() : "");
				row.createCell(7).setCellValue(fw.getType() != null ? fw.getType().toString() : "");
				Double budgeted = getBudgetedCost(fw);
				if (budgeted != null) {
					row.createCell(8).setCellValue(budgeted);
				} else {
					row.createCell(8).setCellValue("");
				}
				Double actual = getActualCost(fw);
				if (actual != null) {
					row.createCell(9).setCellValue(actual);
				} else {
					row.createCell(9).setCellValue("");
				}
			}

			for (int col = 0; col < columns.length; col++) {
				sheet.autoSizeColumn(col);
			}

			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}

	private int writeFilterRow(Sheet sheet, int rowIdx, CellStyle labelStyle, String label, String value) {
		Row row = sheet.createRow(rowIdx);
		Cell labelCell = row.createCell(0);
		labelCell.setCellValue(label + ":");
		labelCell.setCellStyle(labelStyle);
		row.createCell(1).setCellValue(value);
		return rowIdx + 1;
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(Fieldwork value) {
		this.fieldwork = value;
		if (value != null) {
			this.study.setValue(value.getStudy());
			if (value.getStudy() != null && value.getStudy().getBudget() != null) {
				this.budgetEntry.setItems(value.getStudy().getBudget().getEntries());
			}
			// Seed the data providers with the existing values so they render as
			// selected chips and remain selectable.
			this.doobloId.setItems(value.getDoobloId() != null ? new ArrayList<>(value.getDoobloId()) : new ArrayList<>());
			this.alchemerId.setItems(
					value.getAlchemerId() != null ? new ArrayList<>(value.getAlchemerId()) : new ArrayList<>());
		} else {
			this.doobloId.setItems(new ArrayList<>());
			this.alchemerId.setItems(new ArrayList<>());
		}
		binder.readBean(this.fieldwork);
		this.editorLayoutDiv.setVisible(value != null);
	}
}
