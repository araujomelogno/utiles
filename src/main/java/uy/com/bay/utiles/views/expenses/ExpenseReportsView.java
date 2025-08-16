package uy.com.bay.utiles.views.expenses;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.MultiFileReceiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportFile;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

@PageTitle("Expense Reports")
@Route("expense-reports/:expenseReportID?/:action?(edit)")
@PermitAll
public class ExpenseReportsView extends Div implements BeforeEnterObserver {

	private final String EXPENSE_REPORT_ID = "expenseReportID";
	private final String EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE = "expense-reports/%s/edit";

	private final Grid<ExpenseReport> grid = new Grid<>(ExpenseReport.class, false);

	private TextField studyFilter;
	private TextField surveyorFilter;
	private DatePicker dateFromFilter;
	private DatePicker dateToFilter;
	private NumberField amountFromFilter;
	private NumberField amountToFilter;
	private TextField conceptFilter;
	private ComboBox<ExpenseStatus> statusFilter;

	private ComboBox<Study> study;
	private ComboBox<Surveyor> surveyor;
	private DatePicker date;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private ComboBox<ExpenseStatus> expenseStatus;
	private Upload files;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Borrar");
	private final Button comprobantes = new Button("Ver comprobantes");

	private final BeanValidationBinder<ExpenseReport> binder;
	private ExpenseReport expenseReport;
	private final ExpenseReportService expenseReportService;
	private final StudyService studyService;
	private final SurveyorService surveyorService;
	private final ExpenseRequestTypeService expenseRequestTypeService;
	private final ExpenseReportFileService expenseReportFileService;
	private Div editorLayoutDiv;

	public ExpenseReportsView(ExpenseReportService expenseReportService, StudyService studyService,
			SurveyorService surveyorService, ExpenseRequestTypeService expenseRequestTypeService,
			ExpenseReportFileService expenseReportFileService) {
		this.expenseReportService = expenseReportService;
		this.studyService = studyService;
		this.surveyorService = surveyorService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.expenseReportFileService = expenseReportFileService;
		addClassNames("expense-reports-view");

		comprobantes.addClickListener(e -> openComprobantesDialog());

		SplitLayout splitLayout = new SplitLayout();
		splitLayout.setSplitterPosition(80);

		createEditorLayout(splitLayout);
		createGridLayout(splitLayout);

		add(splitLayout);

		grid.addColumn(er -> er.getStudy() != null ? er.getStudy().getName() : "").setHeader("Estudio")
				.setSortProperty("study.name").setKey("study");
		grid.addColumn(
				er -> er.getSurveyor() != null ? er.getSurveyor().getFirstName() + " " + er.getSurveyor().getLastName()
						: "")
				.setHeader("Encuestador").setSortProperty("surveyor.firstName").setKey("surveyor");
		grid.addColumn(
				er -> er.getDate() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getDate()) : "")
				.setHeader("Fecha").setSortProperty("date").setKey("date");
		grid.addColumn(ExpenseReport::getAmount).setHeader("Monto").setSortProperty("amount").setKey("amount");
		grid.addColumn(er -> er.getConcept() != null ? er.getConcept().getConcept() : "").setHeader("Concepto")
				.setSortProperty("concept.concept").setKey("concept");
		grid.addColumn(ExpenseReport::getExpenseStatus).setHeader("Estado").setSortProperty("expenseStatus")
				.setKey("expenseStatus");

		grid.setDataProvider(new CallbackDataProvider<>(
				query -> expenseReportService
						.list(VaadinSpringDataHelpers.toSpringPageRequest(query), createFilterSpecification()).stream(),
				query -> (int) expenseReportService.count(createFilterSpecification())));

		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				editorLayoutDiv.setVisible(false);
				clearForm();
				UI.getCurrent().navigate(ExpenseReportsView.class);
			}
		});

		binder = new BeanValidationBinder<>(ExpenseReport.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
			editorLayoutDiv.setVisible(false);
		});

		delete.addClickListener(e -> {
			if (this.expenseReport != null && this.expenseReport.getId() != null) {
				expenseReportService.delete(this.expenseReport.getId());
				clearForm();
				refreshGrid();
				Notification.show("Rendición borrada.");
			}
		});

		save.addClickListener(e -> {
			try {
				if (this.expenseReport == null) {
					this.expenseReport = new ExpenseReport();
				}
				binder.writeBean(this.expenseReport);
				expenseReportService.save(this.expenseReport);
				clearForm();
				refreshGrid();
				Notification.show("Rendición guardada.");
				UI.getCurrent().navigate(ExpenseReportsView.class);
			} catch (ValidationException validationException) {
				Notification.show("Error al guardar la rendición.");
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> expenseReportId = event.getRouteParameters().get(EXPENSE_REPORT_ID).map(Long::parseLong);
		if (expenseReportId.isPresent()) {
			Optional<ExpenseReport> expenseReportFromBackend = expenseReportService.get(expenseReportId.get());
			if (expenseReportFromBackend.isPresent()) {
				populateForm(expenseReportFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(
						String.format("The requested expense report was not found, ID = %d", expenseReportId.get()),
						3000, Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(ExpenseReportsView.class);
			}
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		editorLayoutDiv = new Div();
		editorLayoutDiv.setClassName("editor-layout");
		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);
		FormLayout formLayout = new FormLayout();
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.listAll());
		study.setItemLabelGenerator(Study::getName);
		surveyor = new ComboBox<>("Encuestador");
		surveyor.setItems(surveyorService.listAll());
		surveyor.setItemLabelGenerator(s -> s.getFirstName() + " " + s.getLastName());
		date = new DatePicker("Fecha");
		date.setReadOnly(true);
		amount = new NumberField("Monto");
		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(ExpenseRequestType::getConcept);
		expenseStatus = new ComboBox<>("Estado");
		expenseStatus.setItems(ExpenseStatus.values());

		files = new Upload();
		// files.setMaxFileSize(20480);
		files.setAcceptedFileTypes("image/jpeg", "image/png", "application/pdf");

		Map<String, ByteArrayOutputStream> fileBuffers = new HashMap<>();

		files.setReceiver((MultiFileReceiver) (fileName, mimeType) -> {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			fileBuffers.put(fileName, outputStream);
			return outputStream;
		});

		files.addSucceededListener(event -> {
			String fileName = event.getFileName();
			ByteArrayOutputStream outputStream = fileBuffers.get(fileName);
			byte[] content = outputStream.toByteArray();

			ExpenseReportFile expenseReportFile = new ExpenseReportFile();
			expenseReportFile.setName(fileName);
			expenseReportFile.setCreated(new Date());
			expenseReportFile.setContent(content);
			expenseReportFile.setExpenseReport(this.expenseReport);

			if (this.expenseReport.getFiles() == null) {
				this.expenseReport.setFiles(new ArrayList<>());
			}
			this.expenseReport.getFiles().add(expenseReportFile);

			fileBuffers.remove(fileName);
			Notification.show("Archivo " + fileName + " subido.");
			comprobantes.setEnabled(true);
		});

		formLayout.add(study, surveyor, date, amount, concept, expenseStatus, new Label("Subir comprobantes"), files,
				comprobantes);
		editorDiv.add(formLayout);
		createButtonLayout(editorLayoutDiv);
		splitLayout.addToSecondary(editorLayoutDiv);
		editorLayoutDiv.setVisible(false);
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		buttonLayout.add(save, cancel, delete);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setWidthFull();

		studyFilter = new TextField();
		studyFilter.setPlaceholder("Estudio...");
		studyFilter.setClearButtonVisible(true);
		studyFilter.addValueChangeListener(e -> refreshGrid());

		surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Encuestador...");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.addValueChangeListener(e -> refreshGrid());

		dateFromFilter = new DatePicker();
		dateFromFilter.setPlaceholder("Desde...");
		dateFromFilter.setClearButtonVisible(true);
		dateFromFilter.addValueChangeListener(e -> refreshGrid());

		dateToFilter = new DatePicker();
		dateToFilter.setPlaceholder("Hasta...");
		dateToFilter.setClearButtonVisible(true);
		dateToFilter.addValueChangeListener(e -> refreshGrid());

		amountFromFilter = new NumberField();
		amountFromFilter.setPlaceholder("Monto desde...");
		amountFromFilter.setClearButtonVisible(true);
		amountFromFilter.addValueChangeListener(e -> refreshGrid());

		amountToFilter = new NumberField();
		amountToFilter.setPlaceholder("Monto hasta...");
		amountToFilter.setClearButtonVisible(true);
		amountToFilter.addValueChangeListener(e -> refreshGrid());

		conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Concepto...");
		conceptFilter.setClearButtonVisible(true);
		conceptFilter.addValueChangeListener(e -> refreshGrid());

		statusFilter = new ComboBox<>();
		statusFilter.setPlaceholder("Estado...");
		statusFilter.setItems(ExpenseStatus.values());
		statusFilter.setClearButtonVisible(true);
		statusFilter.addValueChangeListener(e -> refreshGrid());

		filterLayout.add(studyFilter, surveyorFilter, dateFromFilter, dateToFilter, amountFromFilter, amountToFilter,
				conceptFilter, statusFilter);

		Button createButton = new Button("Crear Rendición", e -> {
			grid.asSingleSelect().clear();
			populateForm(new ExpenseReport());
			editorLayoutDiv.setVisible(true);
		});
		createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout topLayout = new HorizontalLayout(createButton);
		topLayout.setWidth("100%");
		topLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
		splitLayout.addToPrimary(wrapper);
		wrapper.add(topLayout, filterLayout, grid);
	}

	private Specification<ExpenseReport> createFilterSpecification() {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (studyFilter.getValue() != null && !studyFilter.getValue().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("study").get("name")),
						"%" + studyFilter.getValue().toLowerCase() + "%"));
			}
			if (surveyorFilter.getValue() != null && !surveyorFilter.getValue().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("surveyor").get("firstName")),
						"%" + surveyorFilter.getValue().toLowerCase() + "%"));
			}
			if (dateFromFilter.getValue() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), dateFromFilter.getValue()));
			}
			if (dateToFilter.getValue() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), dateToFilter.getValue()));
			}
			if (amountFromFilter.getValue() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), amountFromFilter.getValue()));
			}
			if (amountToFilter.getValue() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), amountToFilter.getValue()));
			}
			if (conceptFilter.getValue() != null && !conceptFilter.getValue().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("concept").get("concept")),
						"%" + conceptFilter.getValue().toLowerCase() + "%"));
			}
			if (statusFilter.getValue() != null) {
				predicates.add(criteriaBuilder.equal(root.get("expenseStatus"), statusFilter.getValue()));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
		files.clearFileList();
	}

	private void populateForm(ExpenseReport value) {
		this.expenseReport = value;
		binder.readBean(this.expenseReport);
		comprobantes.setEnabled(value != null && value.getFiles() != null && !value.getFiles().isEmpty());
	}

	private void openComprobantesDialog() {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Comprobantes");
		dialog.setWidth("80%");
		Grid<ExpenseReportFile> filesGrid = new Grid<>(ExpenseReportFile.class, false);
		filesGrid.addColumn(ExpenseReportFile::getName).setHeader("Nombre");
		filesGrid.addColumn(file -> new java.text.SimpleDateFormat("dd/MM/yyyy").format(file.getCreated()))
				.setHeader("Fecha de Creación");

		filesGrid.addComponentColumn(file -> {
			Button downloadButton = new Button("Descargar");
			StreamResource resource = new StreamResource(file.getName(),
					() -> new ByteArrayInputStream(file.getContent()));
			Anchor downloadLink = new Anchor(resource, "");
			downloadLink.getElement().setAttribute("download", true);
			downloadLink.add(downloadButton);
			return downloadLink;
		}).setHeader("Acciones");

		filesGrid.addComponentColumn(file -> {
			Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
			deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
			deleteButton.addClickListener(e -> {
				ConfirmDialog cdialog = new ConfirmDialog();
				cdialog.setHeader("Confirmar borrado");
				cdialog.setText("¿Está seguro de que desea borrar el archivo?");
				cdialog.setCancelable(true);
				cdialog.setConfirmText("Borrar");
				cdialog.setConfirmButtonTheme("error primary");
				cdialog.addConfirmListener(event -> {
					expenseReportFileService.delete(file.getId());
					this.expenseReport.getFiles().remove(file);
					filesGrid.setItems(this.expenseReport.getFiles());
					Notification.show("Archivo borrado.");
				});
				cdialog.open();
			});
			return deleteButton;
		}).setHeader("");

		if (this.expenseReport != null) {
			filesGrid.setItems(this.expenseReport.getFiles());
		}

		dialog.add(filesGrid);

		Button closeButton = new Button("Cerrar", e -> dialog.close());
		dialog.getFooter().add(closeButton);

		dialog.open();
	}
}
