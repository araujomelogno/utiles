package uy.com.bay.utiles.views.expenses;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.services.ExpenseTransferService;
import uy.com.bay.utiles.views.expensetransfer.ExpenseTransferViewDialog;

@PageTitle("Solicitudes de Gastos")
@Route("expenses/:expenseID?/:action?(edit)")
@PermitAll
public class ExpensesView extends Div implements BeforeEnterObserver {

	private final String EXPENSE_ID = "expenseID";
	private final String EXPENSE_EDIT_ROUTE_TEMPLATE = "expenses/%s/edit";

	private final Grid<ExpenseRequest> grid = new Grid<>(ExpenseRequest.class, false);

	private ComboBox<Study> study;
	private ComboBox<Surveyor> surveyor;
	private DatePicker requestDate;
	private DatePicker aprovalDate;
	private DatePicker transferDate;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private com.vaadin.flow.component.textfield.TextArea obs;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Borrar");
	private final Button approve = new Button("Aprobar");
	private final Button viewTransferButton = new Button("Ver Transferencia");

	private final BeanValidationBinder<ExpenseRequest> binder;
	private ExpenseRequest expenseRequest;
	private final ExpenseRequestService expenseRequestService;
	private final StudyService studyService;
	private final SurveyorService surveyorService;
	private final ExpenseRequestTypeService expenseRequestTypeService;
	private final ExpenseTransferService expenseTransferService;
	private final ExpenseTransferFileService expenseTransferFileService;
	private Div editorLayoutDiv;

	private final Filters filters;

	public ExpensesView(ExpenseRequestService expenseRequestService, StudyService studyService,
			SurveyorService surveyorService, ExpenseRequestTypeService expenseRequestTypeService,
			ExpenseTransferService expenseTransferService, ExpenseTransferFileService expenseTransferFileService) {
		this.expenseRequestService = expenseRequestService;
		this.studyService = studyService;
		this.surveyorService = surveyorService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.expenseTransferService = expenseTransferService;
		this.expenseTransferFileService = expenseTransferFileService;
		addClassNames("expenses-view");

		filters = new Filters();

		SplitLayout splitLayout = new SplitLayout();
		splitLayout.setSplitterPosition(80);

		createEditorLayout(splitLayout);
		createGridLayout(splitLayout);

		add(splitLayout);

		Grid.Column<ExpenseRequest> studyColumn = grid
				.addColumn(er -> er.getStudy() != null ? er.getStudy().getName() : "").setHeader("Estudio")
				.setAutoWidth(true).setSortable(true).setSortProperty("study.name");
		Grid.Column<ExpenseRequest> surveyorColumn = grid
				.addColumn(er -> er.getSurveyor() != null
						? er.getSurveyor().getFirstName() + " " + er.getSurveyor().getLastName()
						: "")
				.setHeader("Encuestador").setAutoWidth(true).setSortable(true).setSortProperty("surveyor.firstName");
		Grid.Column<ExpenseRequest> requestDateColumn = grid
				.addColumn(new com.vaadin.flow.data.renderer.TextRenderer<>(er -> er.getRequestDate() != null
						? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getRequestDate())
						: ""))
				.setHeader("Solicitado:").setAutoWidth(true).setSortable(true).setSortProperty("requestDate");
		Grid.Column<ExpenseRequest> aprovalDateColumn = grid
				.addColumn(new com.vaadin.flow.data.renderer.TextRenderer<>(er -> er.getAprovalDate() != null
						? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getAprovalDate())
						: ""))
				.setHeader("Aprobado:").setAutoWidth(true).setSortable(true).setSortProperty("aprovalDate");
		Grid.Column<ExpenseRequest> transferDateColumn = grid
				.addColumn(new com.vaadin.flow.data.renderer.TextRenderer<>(er -> er.getTransferDate() != null
						? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getTransferDate())
						: ""))
				.setHeader("Transferido").setAutoWidth(true).setSortable(true).setSortProperty("transferDate");
		Grid.Column<ExpenseRequest> amountColumn = grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto")
				.setAutoWidth(true).setSortable(true).setSortProperty("amount");
		Grid.Column<ExpenseRequest> conceptColumn = grid
				.addColumn(er -> er.getConcept() != null ? er.getConcept().getName() : "").setHeader("Concepto")
				.setAutoWidth(true).setSortable(true).setSortProperty("concept.name");
		Grid.Column<ExpenseRequest> statusColumn = grid.addColumn(ExpenseRequest::getExpenseStatus).setHeader("Estado")
				.setAutoWidth(true).setSortable(true).setSortProperty("expenseStatus");

		grid.sort(List.of(new GridSortOrder<>(requestDateColumn, SortDirection.DESCENDING)));

		grid.setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
			Specification<ExpenseRequest> spec = createSpecification(filters);
			return expenseRequestService
					.list(com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query), spec)
					.stream();
		}, query -> {
			Specification<ExpenseRequest> spec = createSpecification(filters);
			return expenseRequestService.count(spec);
		}));

		HeaderRow headerRow = grid.appendHeaderRow();

		TextField studyFilter = new TextField();
		studyFilter.setPlaceholder("Filter");
		studyFilter.setClearButtonVisible(true);
		studyFilter.setValueChangeMode(ValueChangeMode.LAZY);
		studyFilter.addValueChangeListener(e -> {
			filters.setStudyName(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(studyColumn).setComponent(studyFilter);

		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filter");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.setValueChangeMode(ValueChangeMode.LAZY);
		surveyorFilter.addValueChangeListener(e -> {
			filters.setSurveyorName(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		DatePicker requestDateFilter = new DatePicker();
		requestDateFilter.setPlaceholder("Filter");
		requestDateFilter.addValueChangeListener(e -> {
			filters.setRequestDate(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(requestDateColumn).setComponent(requestDateFilter);

		DatePicker aprovalDateFilter = new DatePicker();
		aprovalDateFilter.setPlaceholder("Filter");
		aprovalDateFilter.setClearButtonVisible(true);
		aprovalDateFilter.addValueChangeListener(e -> {
			filters.setAprovalDate(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(aprovalDateColumn).setComponent(aprovalDateFilter);

		DatePicker transferDateFilter = new DatePicker();
		transferDateFilter.setPlaceholder("Filter");
		transferDateFilter.setClearButtonVisible(true);
		transferDateFilter.addValueChangeListener(e -> {
			filters.setTransferDate(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(transferDateColumn).setComponent(transferDateFilter);

		NumberField amountFilter = new NumberField();
		amountFilter.setPlaceholder("Filter");
		amountFilter.setClearButtonVisible(true);
		amountFilter.setValueChangeMode(ValueChangeMode.LAZY);
		amountFilter.addValueChangeListener(e -> {
			filters.setAmount(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(amountColumn).setComponent(amountFilter);

		ComboBox<ExpenseRequestType> conceptFilter = new ComboBox<>();
		conceptFilter.setItems(expenseRequestTypeService.findAll());
		conceptFilter.setItemLabelGenerator(ert -> ert.getName() != null ? ert.getName() : "");
		conceptFilter.setPlaceholder("Filter");
		conceptFilter.setClearButtonVisible(true);
		conceptFilter.addValueChangeListener(e -> {
			filters.setConcept(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(conceptColumn).setComponent(conceptFilter);

		ComboBox<ExpenseStatus> statusFilter = new ComboBox<>();
		statusFilter.setItems(ExpenseStatus.values());
		statusFilter.setPlaceholder("Filter");
		statusFilter.setClearButtonVisible(true);
		statusFilter.addValueChangeListener(e -> {
			filters.setExpenseStatus(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(statusColumn).setComponent(statusFilter);

		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(EXPENSE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
				approve.setEnabled(event.getValue().getExpenseStatus() == ExpenseStatus.INGRESADO);
				viewTransferButton.setEnabled(event.getValue().getExpenseStatus() == ExpenseStatus.TRANSFERIDO
						|| event.getValue().getExpenseStatus() == ExpenseStatus.RENDIDO);
			} else {
				editorLayoutDiv.setVisible(false);
				clearForm();
				UI.getCurrent().navigate(ExpensesView.class);
				approve.setEnabled(false);
				viewTransferButton.setEnabled(false);
			}
		});

		binder = new BeanValidationBinder<>(ExpenseRequest.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
			editorLayoutDiv.setVisible(false);
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
					} catch (Exception ex) {
						Notification.show("Error al borrar el concepto: " + ex.getMessage(), 5000,
								Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
					}
				});
				dialog.open();
			}
		});
		save.addClickListener(e -> {
			try {
				if (this.expenseRequest == null) {
					this.expenseRequest = new ExpenseRequest();
				}

				binder.writeBean(this.expenseRequest);

				if (this.expenseRequest.getRequestDate() == null) {
					this.expenseRequest.setRequestDate(new Date());
				}
				if (this.expenseRequest.getExpenseStatus() == null) {

					this.expenseRequest.setExpenseStatus(ExpenseStatus.INGRESADO);
				}
				expenseRequestService.update(this.expenseRequest);
				clearForm();
				refreshGrid();
				Notification.show("ExpenseRequest details stored.");
				editorLayoutDiv.setVisible(false);
				UI.getCurrent().navigate(ExpensesView.class);
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
				Notification.show("ExpenseRequest approved.");
				editorLayoutDiv.setVisible(false);
				UI.getCurrent().navigate(ExpensesView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error updating the data. Somebody else has updated the record while you were making changes.");
				n.setPosition(Notification.Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Failed to update the data. Check again that all values are valid");
			}
		});

		viewTransferButton.addClickListener(e -> {
			if (this.expenseRequest != null && this.expenseRequest.getExpenseTransfer() != null) {
				uy.com.bay.utiles.data.ExpenseTransfer initializedTransfer = expenseTransferService
						.findByIdAndInitialize(this.expenseRequest.getExpenseTransfer().getId());
				ExpenseTransferViewDialog dialog = new ExpenseTransferViewDialog(initializedTransfer,
						expenseTransferFileService);
				dialog.open();
			} else {
				Notification.show("No hay una transferencia asociada a esta solicitud.", 3000,
						Notification.Position.BOTTOM_START);
			}
		});

	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> expenseId = event.getRouteParameters().get(EXPENSE_ID).map(Long::parseLong);
		if (expenseId.isPresent()) {
			Optional<ExpenseRequest> expenseRequestFromBackend = expenseRequestService.get(expenseId.get());
			if (expenseRequestFromBackend.isPresent()) {
				populateForm(expenseRequestFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(
						String.format("The requested expense request was not found, ID = %d", expenseId.get()), 3000,
						Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(ExpensesView.class);
			}
		}
	}

	private Specification<ExpenseRequest> createSpecification(Filters filters) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
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
			if (filters.getAprovalDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("aprovalDate"), filters.getAprovalDate()));
			}
			if (filters.getTransferDate() != null) {
				predicates.add(criteriaBuilder.equal(root.get("transferDate"), filters.getTransferDate()));
			}
			if (filters.getAmount() != null) {
				predicates.add(criteriaBuilder.equal(root.get("amount"), filters.getAmount()));
			}
			if (filters.getConcept() != null) {
				predicates.add(criteriaBuilder.equal(root.get("concept"), filters.getConcept()));
			}
			if (filters.getExpenseStatus() != null) {
				predicates.add(criteriaBuilder.equal(root.get("expenseStatus"), filters.getExpenseStatus()));
			}
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
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
		study.setItemLabelGenerator(s -> s.getName() != null ? s.getName() : "");
		surveyor = new ComboBox<>("Encuestador");
		surveyor.setItems(surveyorService.listAll());
		surveyor.setItemLabelGenerator(Surveyor::getName);
		requestDate = new DatePicker("Fecha solicitud");
		requestDate.setReadOnly(true);
		aprovalDate = new DatePicker("Fecha aprobación");
		aprovalDate.setReadOnly(true);
		transferDate = new DatePicker("Fecha transferencia");
		transferDate.setReadOnly(true);
		amount = new NumberField("Monto");
		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(ert -> ert.getName() != null ? ert.getName() : "");
		obs = new com.vaadin.flow.component.textfield.TextArea("Observaciones");
		formLayout.add(study, surveyor, requestDate, aprovalDate, transferDate, amount, concept, obs);
		editorDiv.add(formLayout);
		approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		viewTransferButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttons = new HorizontalLayout(approve, viewTransferButton);
		editorDiv.add(buttons);
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
		Button createButton = new Button("Crear solicitud", e -> {
			grid.asSingleSelect().clear();
			populateForm(new ExpenseRequest());
			editorLayoutDiv.setVisible(true);
		});
		createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout topLayout = new HorizontalLayout(createButton);
		topLayout.setWidth("100%");
		topLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		splitLayout.addToPrimary(wrapper);
		wrapper.add(topLayout, grid);
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
			approve.setEnabled(
					value.getId() != null && value.getId() != 0 && value.getExpenseStatus() == ExpenseStatus.INGRESADO);
			viewTransferButton.setEnabled(value.getId() != null && value.getId() != 0
					&& (value.getExpenseStatus() == ExpenseStatus.TRANSFERIDO
							|| value.getExpenseStatus() == ExpenseStatus.RENDIDO));
		} else {
			approve.setEnabled(false);
			viewTransferButton.setEnabled(false);
		}
	}

	private static class Filters {
		private String studyName;
		private String surveyorName;
		private LocalDate requestDate;
		private LocalDate aprovalDate;
		private LocalDate transferDate;
		private Double amount;
		private ExpenseRequestType concept;
		private ExpenseStatus expenseStatus;

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

		public LocalDate getAprovalDate() {
			return aprovalDate;
		}

		public void setAprovalDate(LocalDate aprovalDate) {
			this.aprovalDate = aprovalDate;
		}

		public LocalDate getTransferDate() {
			return transferDate;
		}

		public void setTransferDate(LocalDate transferDate) {
			this.transferDate = transferDate;
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

		public ExpenseStatus getExpenseStatus() {
			return expenseStatus;
		}

		public void setExpenseStatus(ExpenseStatus expenseStatus) {
			this.expenseStatus = expenseStatus;
		}
	}
}
