package uy.com.bay.utiles.views.expenses;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridMultiSelectionModel.SelectAllCheckboxVisibility;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.criteria.Predicate;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;

@Route(value = "expenses-approval")
@PageTitle("Aprobar Solicitudes de Gasto")
@RolesAllowed("GASTOS")
public class ExpensesAprovalView extends Div {

	private final ExpenseRequestService expenseRequestService;
	private final ExpenseRequestTypeService expenseRequestTypeService;

	private final Grid<ExpenseRequest> grid = new Grid<>(ExpenseRequest.class, false);
	private final Set<ExpenseRequest> selectedRequests = new HashSet<>();
	private final Filters filters;

	public ExpensesAprovalView(ExpenseRequestService expenseRequestService,
			ExpenseRequestTypeService expenseRequestTypeService) {
		this.expenseRequestService = expenseRequestService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.filters = new Filters();

		addClassName("expenses-aproval-view");
		setSizeFull();

		Button approveButton = new Button("Aprobar solicitudes", event -> approveSelected());
		Button revokeButton = new Button("Rechazar solicitudes", event -> revokeSelected());
		approveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		revokeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		HorizontalLayout toolbar = new HorizontalLayout(approveButton, revokeButton);
		toolbar.setWidthFull();
		toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

		add(toolbar);
		setupGrid();
		add(grid);

	}

	private void setupGrid() {
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		((GridMultiSelectionModel<ExpenseRequest>) grid.getSelectionModel())
				.setSelectAllCheckboxVisibility(SelectAllCheckboxVisibility.VISIBLE);

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

		grid.asMultiSelect().addValueChangeListener(event -> {
			selectedRequests.clear();
			selectedRequests.addAll(event.getValue());
		});

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
		studyFilter.setPlaceholder("Filtrar");
		studyFilter.setClearButtonVisible(true);
		studyFilter.setValueChangeMode(ValueChangeMode.LAZY);
		studyFilter.addValueChangeListener(e -> {
			filters.setStudyName(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(studyColumn).setComponent(studyFilter);

		TextField surveyorFilter = new TextField();
		surveyorFilter.setPlaceholder("Filtrar");
		surveyorFilter.setClearButtonVisible(true);
		surveyorFilter.setValueChangeMode(ValueChangeMode.LAZY);
		surveyorFilter.addValueChangeListener(e -> {
			filters.setSurveyorName(e.getValue());
			grid.getDataProvider().refreshAll();
		});
		headerRow.getCell(surveyorColumn).setComponent(surveyorFilter);

		DatePicker requestDateFilter = new DatePicker();
		requestDateFilter.setPlaceholder("Filtrar");
		requestDateFilter.setClearButtonVisible(true);
		requestDateFilter.addValueChangeListener(e -> {
			filters.setRequestDate(e.getValue());
			grid.getDataProvider().refreshAll();
		});

		headerRow.getCell(requestDateColumn).setComponent(requestDateFilter);

		NumberField amountFilter = new NumberField();
		amountFilter.setPlaceholder("Filtrar");
		amountFilter.setClearButtonVisible(true);
		amountFilter.setValueChangeMode(ValueChangeMode.LAZY);
		amountFilter.addValueChangeListener(e -> {
			filters.setAmount(e.getValue());
			grid.getDataProvider().refreshAll();
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
		});
		headerRow.getCell(conceptColumn).setComponent(conceptFilter);

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

	private void approveSelected() {
		if (selectedRequests.isEmpty()) {
			Notification.show("No hay solicitudes seleccionadas.");
			return;
		}

		List<Long> ids = new ArrayList<>();
		for (ExpenseRequest request : selectedRequests) {
			ids.add(request.getId());
		}
		expenseRequestService.approveRequests(ids);

		Notification.show("Solicitudes aprobadas exitosamente.");
		grid.getDataProvider().refreshAll();
		grid.asMultiSelect().clear();
	}

	private void revokeSelected() {
		if (selectedRequests.isEmpty()) {
			Notification.show("No hay solicitudes seleccionadas.");
			return;
		}

		List<Long> ids = new ArrayList<>();
		for (ExpenseRequest request : selectedRequests) {
			ids.add(request.getId());
		}
		expenseRequestService.revokeRequests(ids);

		Notification.show("Solicitudes rechazads.");
		grid.getDataProvider().refreshAll();
		grid.asMultiSelect().clear();
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
