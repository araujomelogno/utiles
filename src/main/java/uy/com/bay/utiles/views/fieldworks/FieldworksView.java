package uy.com.bay.utiles.views.fieldworks;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Area;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.FieldworkStatus;
import uy.com.bay.utiles.data.FieldworkType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.service.FieldworkService;
import uy.com.bay.utiles.services.StudyService;

@Route("fieldworks/:fieldworkID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class FieldworksView extends Div implements BeforeEnterObserver {

	private final String FIELDWORK_ID = "fieldworkID";
	private final String FIELDWORK_EDIT_ROUTE_TEMPLATE = "fieldworks/%s/edit";

	private final Grid<Fieldwork> grid = new Grid<>(Fieldwork.class, false);

	private TextField doobloId;
	private TextField alchemerId;
	private ComboBox<Study> study;
	private ComboBox<uy.com.bay.utiles.entities.BudgetEntry> budgetEntry;
	private DatePicker initPlannedDate;
	private DatePicker endPlannedDate;
	private DatePicker initDate;
	private DatePicker endDate;
	private IntegerField goalQuantity;
	private IntegerField completed;
	private TextField obs;
	private BigDecimalField unitCost;
	private ComboBox<FieldworkStatus> status;
	private ComboBox<FieldworkType> type;
	private ComboBox<Area> area;

	private ComboBox<Study> studyFilter;
	private ComboBox<FieldworkStatus> statusFilter;
	private ComboBox<FieldworkType> typeFilter;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Eliminar");

	private final BeanValidationBinder<Fieldwork> binder;

	private Fieldwork fieldwork;
	private Div editorLayoutDiv;

	private final FieldworkService fieldworkService;
	private final StudyService studyService;
	private final uy.com.bay.utiles.services.AreaService areaService;

	public FieldworksView(FieldworkService fieldworkService, StudyService studyService,
			uy.com.bay.utiles.services.AreaService areaService) {
		this.fieldworkService = fieldworkService;
		this.studyService = studyService;
		this.areaService = areaService;
		addClassNames("fieldworks-view");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid
		grid.addColumn(fieldwork -> fieldwork.getStudy() != null ? fieldwork.getStudy().getName() : "")
				.setHeader("Estudio").setAutoWidth(true);
		grid.addColumn("initPlannedDate").setHeader("Fecha Planificada Inicio").setAutoWidth(true);
		grid.addColumn("endPlannedDate").setHeader("Fecha Planificada Fin").setAutoWidth(true);
		grid.addColumn("initDate").setHeader("Fecha Inicio").setAutoWidth(true);
		grid.addColumn("endDate").setHeader("Fecha Fin").setAutoWidth(true);
		grid.addColumn("goalQuantity").setHeader("Cantidad Objetivo").setAutoWidth(true);
		grid.addColumn("unitCost").setHeader("Costo Unitario").setAutoWidth(true);
		grid.addColumn("completed").setHeader("Completadas").setAutoWidth(true);
		grid.addColumn("obs").setHeader("Observaciones").setAutoWidth(true);
		grid.addColumn("status").setHeader("Estado").setAutoWidth(true);
		grid.addColumn("type").setHeader("Tipo").setAutoWidth(true);
		grid.addColumn("area").setHeader("Area").setAutoWidth(true);

		grid.setItems(query -> {
			Specification<Fieldwork> spec = (root, q, cb) -> {
				return cb.and();
			};
			if (studyFilter.getValue() != null) {
				spec = spec.and((root, q, cb) -> cb.equal(root.get("study"), studyFilter.getValue()));
			}
			if (statusFilter.getValue() != null) {
				spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), statusFilter.getValue()));
			}
			if (typeFilter.getValue() != null) {
				spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), typeFilter.getValue()));
			}
			return fieldworkService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), spec).stream();
		});
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

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
		binder.forField(doobloId).bind(Fieldwork::getDoobloId, Fieldwork::setDoobloId);
		binder.forField(alchemerId).bind(Fieldwork::getAlchemerId, Fieldwork::setAlchemerId);
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
				if (this.fieldwork.getBudgetEntry() == null) {
					Notification.show("Debe seleccionar un Presupuesto.", 3000, Notification.Position.BOTTOM_START);
					return;
				}

				if (this.fieldwork.getType() == null) {
					Notification.show("Debe seleccionar el tipo campo.", 3000, Notification.Position.BOTTOM_START);
					return;
				}
				fieldworkService.save(this.fieldwork);
				if (this.fieldwork.getStudy() != null
						&& !this.fieldwork.getStudy().getFieldworks().contains(this.fieldwork)) {
					this.fieldwork.getStudy().getFieldworks().add(this.fieldwork);
					this.studyService.save(this.fieldwork.getStudy());
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
			if (this.fieldwork != null) {
				fieldworkService.delete(this.fieldwork.getId());
				clearForm();
				refreshGrid();
				Notification.show("Solicitud de campo eliminada.");
				UI.getCurrent().navigate(FieldworksView.class);
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
		doobloId = new TextField("Dooblo Id");
		alchemerId = new TextField("Alchemer Id");
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
		initDate = new DatePicker("Fecha Inicio");
		endDate = new DatePicker("Fecha Fin");
		goalQuantity = new IntegerField("Cantidad Objetivo");
		unitCost = new BigDecimalField("Costo unitario");
		completed = new IntegerField("Completas");
		completed.setReadOnly(true);
		obs = new TextField("Observaciones");
		status = new ComboBox<>("Estado");
		status.setItems(FieldworkStatus.values());
		type = new ComboBox<>("Tipo");
		type.setItems(FieldworkType.values());
		area = new ComboBox<>("Area");
		area.setItems(areaService.listAll());
		area.setItemLabelGenerator(Area::getNombre);
		formLayout.add(study, budgetEntry, doobloId, alchemerId, initPlannedDate, endPlannedDate, goalQuantity,
				unitCost, completed, obs, status, type, area);

		editorDiv.add(formLayout);
		createButtonLayout(this.editorLayoutDiv);

		splitLayout.addToSecondary(this.editorLayoutDiv);
		this.editorLayoutDiv.setVisible(false);
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
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		splitLayout.addToPrimary(wrapper);

		H2 title = new H2("Solicitudes de Campo");
		Button addButton = new Button("Agregar Solicitud");
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.addClickListener(e -> {
			clearForm();
			this.fieldwork = new Fieldwork();
			binder.readBean(this.fieldwork);
			this.editorLayoutDiv.setVisible(true);
		});

		HorizontalLayout topLayout = new HorizontalLayout(title, addButton);
		topLayout.setWidth("100%");
		topLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

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

		HorizontalLayout filterLayout = new HorizontalLayout(studyFilter, statusFilter, typeFilter);
		filterLayout.setWidth("100%");

		wrapper.add(topLayout, filterLayout, grid);
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
		}
		binder.readBean(this.fieldwork);
		this.editorLayoutDiv.setVisible(value != null);
	}
}
