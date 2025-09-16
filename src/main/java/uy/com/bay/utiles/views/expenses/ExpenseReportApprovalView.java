package uy.com.bay.utiles.views.expenses;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Predicate;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportStatus;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Aprobar Rendiciones")
@Route(value = "expense-reports-approval/:expenseReportID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("GASTOS")
public class ExpenseReportApprovalView extends Div implements BeforeEnterObserver {

	private final String EXPENSE_REPORT_ID = "expenseReportID";
	private final String EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE = "expense-reports-approval/%s/edit";

	private final ExpenseReportService expenseReportService;
	private final Grid<ExpenseReport> grid = new Grid<>(ExpenseReport.class, false);
	private final Filters filters;
	private Div editorLayoutDiv;

	private ComboBox<Study> study;
	private ComboBox<Surveyor> surveyor;
	private DatePicker date;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private TextArea obs;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button approve = new Button("Aprobar");
	private final Button reject = new Button("Rechazar");
	private final Button approveSelected = new Button("Aprobar rendiciones");
	private final Button rejectSelected = new Button("Rechazar rendiciones");

	private final BeanValidationBinder<ExpenseReport> binder;
	private ExpenseReport expenseReport;

	public ExpenseReportApprovalView(ExpenseReportService expenseReportService, StudyService studyService,
			SurveyorService surveyorService, ExpenseRequestTypeService expenseRequestTypeService) {
		this.expenseReportService = expenseReportService;
		this.filters = new Filters();
		addClassName("expensereport-approval-view");
		setSizeFull();

		SplitLayout splitLayout = new SplitLayout();
//		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(80);

		createEditorLayout(splitLayout, studyService, surveyorService, expenseRequestTypeService);
		createGridLayout(splitLayout);
		add(splitLayout);

		binder = new BeanValidationBinder<>(ExpenseReport.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
			editorLayoutDiv.setVisible(false);
		});

		save.addClickListener(e -> {
			try {
				if (this.expenseReport == null) {
					this.expenseReport = new ExpenseReport();
				}
				binder.writeBean(this.expenseReport);
				expenseReportService.update(this.expenseReport);
				clearForm();
				refreshGrid();
				Notification.show("ExpenseReport details stored.");
				editorLayoutDiv.setVisible(false);
				UI.getCurrent().navigate(ExpenseReportApprovalView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error updating the data. Somebody else has updated the record while you were making changes.");
				n.setPosition(Notification.Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Failed to update the data. Check again that all values are valid");
			}
		});

		approve.addClickListener(e -> {
			try {
				if (this.expenseReport == null) {
					Notification.show("No expense report selected.");
					return;
				}
				binder.writeBean(this.expenseReport);
				this.expenseReport.setExpenseStatus(ExpenseReportStatus.APROBADO);
				this.expenseReport.setApprovalDate(new java.util.Date());
				expenseReportService.update(this.expenseReport);
				clearForm();
				refreshGrid();
				Notification.show("ExpenseReport approved.");
				editorLayoutDiv.setVisible(false);
				UI.getCurrent().navigate(ExpenseReportApprovalView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error updating the data. Somebody else has updated the record while you were making changes.");
				n.setPosition(Notification.Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Failed to update the data. Check again that all values are valid");
			}
		});

		reject.addClickListener(e -> {
			try {
				if (this.expenseReport == null) {
					Notification.show("No expense report selected.");
					return;
				}
				binder.writeBean(this.expenseReport);
				this.expenseReport.setExpenseStatus(ExpenseReportStatus.RECHAZADO);
				expenseReportService.update(this.expenseReport);
				clearForm();
				refreshGrid();
				Notification.show("ExpenseReport rejected.");
				editorLayoutDiv.setVisible(false);
				UI.getCurrent().navigate(ExpenseReportApprovalView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error updating the data. Somebody else has updated the record while you were making changes.");
				n.setPosition(Notification.Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Failed to update the data. Check again that all values are valid");
			}
		});

		approveSelected.addClickListener(e -> {
			java.util.Set<ExpenseReport> selectedItems = grid.getSelectedItems();
			if (selectedItems.isEmpty()) {
				Notification.show("No hay rendiciones seleccionadas para aprobar.");
				return;
			}
			selectedItems.forEach(report -> {
				report.setExpenseStatus(ExpenseReportStatus.APROBADO);
				report.setApprovalDate(new java.util.Date());
				expenseReportService.update(report);
			});
			refreshGrid();
			Notification.show(selectedItems.size() + " rendiciones aprobadas.");
		});

		rejectSelected.addClickListener(e -> {
			java.util.Set<ExpenseReport> selectedItems = grid.getSelectedItems();
			if (selectedItems.isEmpty()) {
				Notification.show("No hay rendiciones seleccionadas para rechazar.");
				return;
			}
			selectedItems.forEach(report -> {
				report.setExpenseStatus(ExpenseReportStatus.RECHAZADO);
				report.setApprovalDate(new java.util.Date());
				expenseReportService.update(report);
			});
			refreshGrid();
			Notification.show(selectedItems.size() + " rendiciones rechazadas.");
		});
	}

	private void createGridLayout(SplitLayout splitLayout) {
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setClassName("grid-wrapper");
		wrapper.setSizeFull();
		wrapper.setPadding(false);
		wrapper.setSpacing(false);

		HorizontalLayout topButtons = new HorizontalLayout(approveSelected, rejectSelected);

		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.setHeightFull();

		grid.addItemClickListener(event -> {
			if (event.getItem() != null) {
				editorLayoutDiv.setVisible(true);
				populateForm(event.getItem());
				UI.getCurrent().navigate(String.format(EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE, event.getItem().getId()));
			} else {
				editorLayoutDiv.setVisible(false);
				clearForm();
				UI.getCurrent().navigate(ExpenseReportApprovalView.class);
			}
		});

		Grid.Column<ExpenseReport> dateColumn = grid.addColumn(ExpenseReport::getDate).setHeader("Fecha")
				.setSortable(true);
		Grid.Column<ExpenseReport> surveyorColumn = grid.addColumn(report -> {
			if (report.getSurveyor() == null) {
				return "";
			}
			return report.getSurveyor().getFirstName() + " " + report.getSurveyor().getLastName();
		}).setHeader("Encuestador").setSortable(true);
		Grid.Column<ExpenseReport> studyColumn = grid
				.addColumn(report -> report.getStudy() != null ? report.getStudy().getName() : "").setHeader("Estudio")
				.setSortable(true);
		Grid.Column<ExpenseReport> amountColumn = grid.addColumn(report -> (report.getAmount())).setHeader("Monto")
				.setSortable(true);
		Grid.Column<ExpenseReport> conceptColumn = grid
				.addColumn(report -> report.getConcept() != null ? report.getConcept().getName() : "")
				.setHeader("Concepto").setSortable(true);
		Grid.Column<ExpenseReport> statusColumn = grid.addColumn(ExpenseReport::getExpenseStatus).setHeader("Estado")
				.setSortable(true);

		grid.setDataProvider(new CallbackDataProvider<>(
				query -> expenseReportService
						.list(com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query),
								createSpecification(filters))
						.stream(),
				query -> (int) expenseReportService.count(createSpecification(filters))));

		// Create filters
		HeaderRow filterRow = grid.appendHeaderRow();

		// Date filter
		DatePicker dateFilter = new DatePicker();
		dateFilter.setPlaceholder("Filtrar por fecha...");
		dateFilter.addValueChangeListener(event -> {
			filters.setDate(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(dateColumn).setComponent(dateFilter);

		// Surveyor filter
		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar por encuestador...");
		surveyorFilter.addValueChangeListener(event -> {
			filters.setSurveyorName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		// Study filter
		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filtrar por estudio...");
		studyFilter.addValueChangeListener(event -> {
			filters.setStudyName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(studyColumn).setComponent(studyFilter);

		// Amount filter
		NumberField amountFilter = new NumberField();
		amountFilter.setPlaceholder("Filtrar por monto...");
		amountFilter.addValueChangeListener(event -> {
			filters.setAmount(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(amountColumn).setComponent(amountFilter);

		// Concept filter
		TextField conceptFilter = new TextField();
		conceptFilter.setPlaceholder("Filtrar por concepto...");
		conceptFilter.addValueChangeListener(event -> {
			filters.setConceptName(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(conceptColumn).setComponent(conceptFilter);

		// Status filter
		TextField statusFilter = new TextField();
		statusFilter.setPlaceholder("Filtrar por estado...");
		statusFilter.addValueChangeListener(event -> {
			filters.setStatus(event.getValue());
			grid.getDataProvider().refreshAll();
		});
		filterRow.getCell(statusColumn).setComponent(statusFilter);

		FooterRow footerRow = grid.appendFooterRow();
		updateFooter(footerRow, studyColumn, amountColumn);

		wrapper.add(topButtons, grid);
		wrapper.setFlexGrow(1, grid);
		splitLayout.addToPrimary(wrapper);
	}

	private void createEditorLayout(SplitLayout splitLayout, StudyService studyService, SurveyorService surveyorService,
			ExpenseRequestTypeService expenseRequestTypeService) {
		editorLayoutDiv = new Div();
		editorLayoutDiv.setClassName("editor-layout");
		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);

		com.vaadin.flow.component.formlayout.FormLayout formLayout = new com.vaadin.flow.component.formlayout.FormLayout();
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.listAll());
		study.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		surveyor = new ComboBox<>("Encuestador");
		surveyor.setItems(surveyorService.listAll());
		surveyor.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		date = new DatePicker("Fecha");
		amount = new NumberField("Monto");
		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(ert -> ert == null ? "" : ert.getName());
		obs = new TextArea("Observaciones");
		formLayout.add(study, surveyor, date, amount, concept, obs);
		editorDiv.add(formLayout);

		createButtonLayout(editorDiv);
		splitLayout.addToSecondary(editorLayoutDiv);
		editorLayoutDiv.setVisible(false);
	}

	private void createButtonLayout(Div editorDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		buttonLayout.add(approve, reject, save, cancel);
		editorDiv.add(buttonLayout);
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
				event.forwardTo(ExpenseReportApprovalView.class);
			}
		}
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(ExpenseReport value) {
		this.expenseReport = value;
		binder.readBean(this.expenseReport);
		if (value != null) {
			approve.setEnabled(value.getId() != null && value.getId() != 0
					&& value.getExpenseStatus() == ExpenseReportStatus.INGRESADO);
			reject.setEnabled(value.getId() != null && value.getId() != 0
					&& value.getExpenseStatus() == ExpenseReportStatus.INGRESADO);
		} else {
			approve.setEnabled(false);
			reject.setEnabled(false);
		}
	}

	private void updateFooter(FooterRow footerRow, Grid.Column<ExpenseReport> studyColumn,
			Grid.Column<ExpenseReport> amountColumn) {
		Specification<ExpenseReport> spec = createSpecification(filters);
		Double total = expenseReportService.sumAmount(spec);
		footerRow.getCell(studyColumn).setText("TOTAL");
		footerRow.getCell(amountColumn).setText((total.toString()));
	}

	private Specification<ExpenseReport> createSpecification(Filters filters) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("expenseStatus"), ExpenseReportStatus.INGRESADO));

			if (filters.getDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("date"), filters.getDate()));
			}
			if (filters.getSurveyorName() != null && !filters.getSurveyorName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("surveyor").get("lastName")),
						"%" + filters.getSurveyorName().toLowerCase() + "%"));
			}
			if (filters.getStudyName() != null && !filters.getStudyName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("study").get("name")),
						"%" + filters.getStudyName().toLowerCase() + "%"));
			}
			if (filters.getAmount() != null) {
				predicates.add(criteriaBuilder.equal(root.get("amount"), filters.getAmount()));
			}
			if (filters.getConceptName() != null && !filters.getConceptName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("concept").get("name")),
						"%" + filters.getConceptName().toLowerCase() + "%"));
			}
			if (filters.getStatus() != null && !filters.getStatus().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("expenseStatus").as(String.class)),
						"%" + filters.getStatus().toLowerCase() + "%"));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static class Filters {
		private String studyName;
		private String surveyorName;
		private LocalDate date;
		private Double amount;
		private String conceptName;
		private String status;

		public String getStudyName() {
			return studyName;
		}

		public void setStudyName(String studyName) {
			this.studyName = studyName;
		}

		public String getSurveyorName() {
			return surveyorName;
		}

		public void setSurveyorName(String surveyorName) {
			this.surveyorName = surveyorName;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public String getConceptName() {
			return conceptName;
		}

		public void setConceptName(String conceptName) {
			this.conceptName = conceptName;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}
}