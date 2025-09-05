package uy.com.bay.utiles.views.expenses;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

@Route("expenses-approval/:expenseID?/:action?(edit)")
@PageTitle("Aprobar Solicitudes de Gasto")
@RolesAllowed("GASTOS")
public class ExpensesAprovalView extends Div implements BeforeEnterObserver {

	private final String EXPENSE_ID = "expenseID";
	private final String EXPENSE_EDIT_ROUTE_TEMPLATE = "expenses-approval/%s/edit";

	private final Grid<ExpenseRequest> grid = new Grid<>(ExpenseRequest.class, false);

	private ComboBox<Study> study;
	private ComboBox<Surveyor> surveyor;
	private DatePicker requestDate;
	private DatePicker aprovalDate;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private com.vaadin.flow.component.textfield.TextArea obs;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button approve = new Button("Aprobar");
	private final Button reject = new Button("Rechazar");
	private final Button delete = new Button("Borrar");

	private final BeanValidationBinder<ExpenseRequest> binder;
	private ExpenseRequest expenseRequest;
	private final ExpenseRequestService expenseRequestService;
	private final ExpenseRequestTypeService expenseRequestTypeService;
	private final StudyService studyService;
	private final SurveyorService surveyorService;
	private Div editorLayoutDiv;

	private final Filters filters;

	public ExpensesAprovalView(ExpenseRequestService expenseRequestService,
			ExpenseRequestTypeService expenseRequestTypeService, StudyService studyService,
			SurveyorService surveyorService) {
		this.expenseRequestService = expenseRequestService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.studyService = studyService;
		this.surveyorService = surveyorService;
		this.filters = new Filters();

		addClassName("expenses-aproval-view");
		setHeight("100%");

		SplitLayout splitLayout = new SplitLayout();
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(80);

		createEditorLayout(splitLayout);
		createGridLayout(splitLayout);

		add(splitLayout);

		grid.setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
			Specification<ExpenseRequest> spec = createSpecification(filters);
			return expenseRequestService
					.list(com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query), spec)
					.stream();
		}, query -> {
			Specification<ExpenseRequest> spec = createSpecification(filters);
			return expenseRequestService.count(spec);
		}));

		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(EXPENSE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				editorLayoutDiv.setVisible(false);
				clearForm();
				UI.getCurrent().navigate(ExpensesAprovalView.class);
			}
		});

		binder = new BeanValidationBinder<>(ExpenseRequest.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
			editorLayoutDiv.setVisible(false);
		});

		save.addClickListener(e -> {
			try {
				if (this.expenseRequest == null) {
					this.expenseRequest = new ExpenseRequest();
				}
				binder.writeBean(this.expenseRequest);
				expenseRequestService.update(this.expenseRequest);
				clearForm();
				refreshGrid();
				Notification.show("Detalles de la solicitud guardados.");
				UI.getCurrent().navigate(ExpensesAprovalView.class);
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
				if (this.expenseRequest == null) {
					Notification.show("No expense request selected.");
					return;
				}
				binder.writeBean(this.expenseRequest);
				this.expenseRequest.setExpenseStatus(ExpenseStatus.RECHAZADO);
				expenseRequestService.update(this.expenseRequest);
				clearForm();
				refreshGrid();
				Notification.show("Solicitud de gasto rechazada.");
				UI.getCurrent().navigate(ExpensesAprovalView.class);
			} catch (ObjectOptimisticLockingFailureException | ValidationException exception) {
				Notification.show("Error al rechazar la solicitud.").addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});

		approve.addClickListener(e -> {
			try {
				if (this.expenseRequest == null) {
					Notification.show("No expense request selected.");
					return;
				}
				binder.writeBean(this.expenseRequest);
				this.expenseRequest.setExpenseStatus(ExpenseStatus.APROBADO);
				this.expenseRequest.setAprovalDate(new Date());
				expenseRequestService.update(this.expenseRequest);
				clearForm();
				refreshGrid();
				Notification.show("Solicitud de gasto aprobada.");
				UI.getCurrent().navigate(ExpensesAprovalView.class);
			} catch (ObjectOptimisticLockingFailureException | ValidationException exception) {
				Notification.show("Error al aprobar la solicitud.").addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});

		delete.addClickListener(e -> {
			if (this.expenseRequest != null && this.expenseRequest.getId() != null) {
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setHeader("Confirmar borrado");
				dialog.setText("¿Está seguro de que desea borrar esta solicitud? Esta acción no se puede deshacer.");
				dialog.setCancelable(true);
				dialog.setConfirmText("Borrar");
				dialog.setConfirmButtonTheme("error primary");
				dialog.addConfirmListener(event -> {
					try {
						expenseRequestService.delete(this.expenseRequest.getId());
						clearForm();
						refreshGrid();
						Notification.show("Solicitud de gasto borrada exitosamente.", 3000,
								Notification.Position.BOTTOM_START);
						UI.getCurrent().navigate(ExpensesAprovalView.class);
					} catch (Exception ex) {
						Notification.show("Error al borrar el concepto: " + ex.getMessage(), 5000,
								Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
					}
				});
				dialog.open();
			}
		});
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		wrapper.setWidthFull();
		splitLayout.addToPrimary(wrapper);
		wrapper.add(grid);

		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
		grid.setSelectionMode(Grid.SelectionMode.SINGLE);

		Grid.Column<ExpenseRequest> studyColumn = grid
				.addColumn(er -> er.getStudy() != null ? er.getStudy().getName() : "").setHeader("Estudio")
				.setAutoWidth(true).setSortable(true).setSortProperty("study.name");
		Grid.Column<ExpenseRequest> surveyorColumn = grid
				.addColumn(er -> er.getSurveyor() != null
						? er.getSurveyor().getFirstName() + " " + er.getSurveyor().getLastName()
						: "")
				.setHeader("Encuestador").setAutoWidth(true).setSortable(true).setSortProperty("surveyor.firstName");
		Grid.Column<ExpenseRequest> requestDateColumn = grid
				.addColumn(er -> er.getRequestDate() != null
						? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getRequestDate())
						: "")
				.setHeader("Solicitado").setAutoWidth(true).setSortable(true).setSortProperty("requestDate");
		Grid.Column<ExpenseRequest> amountColumn = grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto")
				.setAutoWidth(true).setSortable(true).setSortProperty("amount");
		Grid.Column<ExpenseRequest> conceptColumn = grid
				.addColumn(er -> er.getConcept() != null ? er.getConcept().getName() : "").setHeader("Concepto")
				.setAutoWidth(true).setSortable(true).setSortProperty("concept.name");

		FooterRow footerRow = grid.appendFooterRow();
		updateFooter(footerRow, studyColumn, amountColumn);

		HeaderRow headerRow = grid.appendHeaderRow();

		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filtrar");
		studyFilter.setClearButtonVisible(true);
		studyFilter.setValueChangeMode(ValueChangeMode.LAZY);
		studyFilter.addValueChangeListener(e -> {
			filters.setStudyName(e.getValue());
			grid.getDataProvider().refreshAll();
			updateFooter(footerRow, studyColumn, amountColumn);
		});
		headerRow.getCell(studyColumn).setComponent(studyFilter);

		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.setValueChangeMode(ValueChangeMode.LAZY);
		surveyorFilter.addValueChangeListener(e -> {
			filters.setSurveyorName(e.getValue());
			grid.getDataProvider().refreshAll();
			updateFooter(footerRow, studyColumn, amountColumn);
		});
		headerRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		DatePicker requestDateFilter = new DatePicker();
		requestDateFilter.setPlaceholder("Filtrar");
		requestDateFilter.setClearButtonVisible(true);
		requestDateFilter.addValueChangeListener(e -> {
			filters.setRequestDate(e.getValue());
			grid.getDataProvider().refreshAll();
			updateFooter(footerRow, studyColumn, amountColumn);
		});
		headerRow.getCell(requestDateColumn).setComponent(requestDateFilter);

		NumberField amountFilter = new NumberField();
		amountFilter.setPlaceholder("Filtrar");
		amountFilter.setClearButtonVisible(true);
		amountFilter.setValueChangeMode(ValueChangeMode.LAZY);
		amountFilter.addValueChangeListener(e -> {
			filters.setAmount(e.getValue());
			grid.getDataProvider().refreshAll();
			updateFooter(footerRow, studyColumn, amountColumn);
		});
		headerRow.getCell(amountColumn).setComponent(amountFilter);

		ComboBox<ExpenseRequestType> conceptFilter = new ComboBox<>();
		conceptFilter.setItems(expenseRequestTypeService.findAll());
		conceptFilter.setItemLabelGenerator(ExpenseRequestType::getName);
		conceptFilter.setPlaceholder("Filtrar");
		conceptFilter.setClearButtonVisible(true);
		conceptFilter.addValueChangeListener(e -> {
			filters.setConcept(e.getValue());
			grid.getDataProvider().refreshAll();
			updateFooter(footerRow, studyColumn, amountColumn);
		});
		headerRow.getCell(conceptColumn).setComponent(conceptFilter);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> expenseId = event.getRouteParameters().get(EXPENSE_ID).map(Long::parseLong);
		if (expenseId.isPresent()) {
			Optional<ExpenseRequest> expenseRequestFromBackend = expenseRequestService.get(expenseId.get());
			if (expenseRequestFromBackend.isPresent()) {
				populateForm(expenseRequestFromBackend.get());
			} else {
				Notification.show(
						String.format("The requested expense request was not found, ID = %d", expenseId.get()), 3000,
						Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(ExpensesAprovalView.class);
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
		study.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		surveyor = new ComboBox<>("Encuestador");
		surveyor.setItems(surveyorService.listAll());
		surveyor.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		requestDate = new DatePicker("Fecha solicitud");
		requestDate.setReadOnly(true);
		aprovalDate = new DatePicker("Fecha aprobación");
		aprovalDate.setReadOnly(true);
		amount = new NumberField("Monto");
		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(ert -> ert == null ? "" : ert.getName());
		obs = new com.vaadin.flow.component.textfield.TextArea("Observaciones");
		formLayout.add(study, surveyor, requestDate, aprovalDate, amount, concept, obs);
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
		approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
		buttonLayout.add(save, cancel, approve, reject, delete);
		editorLayoutDiv.add(buttonLayout);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(ExpenseRequest value) {
		this.expenseRequest = value;
		binder.readBean(this.expenseRequest);
		if (value != null) {
			boolean isIngresado = value.getId() != null && value.getExpenseStatus() == ExpenseStatus.INGRESADO;
			approve.setEnabled(isIngresado);
			reject.setEnabled(isIngresado);
			save.setEnabled(true);
			delete.setEnabled(true);
		} else {
			approve.setEnabled(false);
			reject.setEnabled(false);
			save.setEnabled(false);
			delete.setEnabled(false);
		}
	}

	private void updateFooter(FooterRow footerRow, Grid.Column<ExpenseRequest> studyColumn,
			Grid.Column<ExpenseRequest> amountColumn) {
		Specification<ExpenseRequest> spec = createSpecification(filters);
		Double total = expenseRequestService.sumAmount(spec);
		footerRow.getCell(studyColumn).setText("TOTAL");
		footerRow.getCell(amountColumn).setText(String.format("$%.2f", total));
	}

	private Specification<ExpenseRequest> createSpecification(Filters filters) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("expenseStatus"), ExpenseStatus.INGRESADO));

			if (filters.getStudyName() != null && !filters.getStudyName().isEmpty()) {
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("study").get("name")),
						"%" + filters.getStudyName().toLowerCase() + "%"));
			}
			if (filters.getSurveyorName() != null && !filters.getSurveyorName().isEmpty()) {
				Predicate surveyorFirstName = criteriaBuilder.like(
						criteriaBuilder.lower(root.get("surveyor").get("firstName")),
						"%" + filters.getSurveyorName().toLowerCase() + "%");
				Predicate surveyorLastName = criteriaBuilder.like(
						criteriaBuilder.lower(root.get("surveyor").get("lastName")),
						"%" + filters.getSurveyorName().toLowerCase() + "%");
				predicates.add(criteriaBuilder.or(surveyorFirstName, surveyorLastName));
			}
			if (filters.getRequestDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("requestDate"), filters.getRequestDate()));
			}
			if (filters.getAmount() != null) {
				predicates.add(criteriaBuilder.equal(root.get("amount"), filters.getAmount()));
			}
			if (filters.getConcept() != null) {
				predicates.add(criteriaBuilder.equal(root.get("concept"), filters.getConcept()));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static class Filters {
		private String studyName;
		private String surveyorName;
		private LocalDate requestDate;
		private Double amount;
		private ExpenseRequestType concept;

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

		public LocalDate getRequestDate() {
			return requestDate;
		}

		public void setRequestDate(LocalDate requestDate) {
			this.requestDate = requestDate;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public ExpenseRequestType getConcept() {
			return concept;
		}

		public void setConcept(ExpenseRequestType concept) {
			this.concept = concept;
		}
	}
}
