package uy.com.bay.utiles.views.joborders;

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
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.JobOrder;
import uy.com.bay.utiles.data.Provider;
import uy.com.bay.utiles.data.ProviderType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.JobOrderService;
import uy.com.bay.utiles.services.ProviderService;
import uy.com.bay.utiles.services.StudyService;

@PageTitle("Órdenes de Trabajo")
@Route("joborders/:jobOrderID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class JobOrdersView extends Div implements BeforeEnterObserver {

	private final String JOBORDER_ID = "jobOrderID";
	private final String JOBORDER_EDIT_ROUTE_TEMPLATE = "joborders/%s/edit";

	private final Grid<JobOrder> grid = new Grid<>(JobOrder.class, false);

	private DatePicker init;
	private DatePicker end;
	private ComboBox<Study> study;
	private IntegerField hours;
	private ComboBox<Provider> provider;

	private ComboBox<Study> studyFilter;
	private ComboBox<Provider> providerFilter;

	private Button addButton;

	private final Button cancel = new Button("Cerrar");
	private final Button save = new Button("Guardar");
	private Button deleteButton;

	private final BeanValidationBinder<JobOrder> binder;

	private JobOrder jobOrder;
	private Div editorLayoutDiv;

	private final JobOrderService jobOrderService;
	private final StudyService studyService;
	private final ProviderService providerService;

	public JobOrdersView(JobOrderService jobOrderService, StudyService studyService, ProviderService providerService) {
		this.jobOrderService = jobOrderService;
		this.studyService = studyService;
		this.providerService = providerService;
		this.binder = new BeanValidationBinder<>(JobOrder.class);
		addClassNames("joborders-view");

		SplitLayout splitLayout = new SplitLayout();

		addButton = new Button("Crear");
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		deleteButton = new Button("Borrar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		deleteButton.setEnabled(false);

		setupButtonListeners();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		grid.addColumn("init").setHeader("Inicio").setAutoWidth(true);
		grid.addColumn("end").setHeader("Fin").setAutoWidth(true);
		grid.addColumn(jo -> jo.getStudy() != null ? jo.getStudy().getName() : "").setHeader("Estudio")
				.setAutoWidth(true);
		grid.addColumn("hours").setHeader("Horas").setAutoWidth(true);
		grid.addColumn(jo -> jo.getProvider() != null ? jo.getProvider().getName() : "").setHeader("Proveedor")
				.setAutoWidth(true);

		grid.setItems(query -> jobOrderService
				.list(VaadinSpringDataHelpers.toSpringPageRequest(query), buildSpecification()).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(JOBORDER_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(JobOrdersView.class);
			}
		});

		binder.bindInstanceFields(this);
	}

	private void setupButtonListeners() {
		addButton.addClickListener(e -> {
			clearForm();
			this.jobOrder = new JobOrder();
			binder.readBean(this.jobOrder);
			if (this.editorLayoutDiv != null) {
				this.editorLayoutDiv.setVisible(true);
			}
			if (this.deleteButton != null) {
				this.deleteButton.setEnabled(false);
			}
		});

		deleteButton.addClickListener(e -> {
			if (this.jobOrder != null && this.jobOrder.getId() != null) {
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setHeader("Confirmar Borrado");
				dialog.setText(
						"¿Estás seguro de que quieres borrar esta orden de trabajo? Esta acción no se puede deshacer.");
				dialog.setCancelable(true);
				dialog.setConfirmText("Borrar");
				dialog.setConfirmButtonTheme("error primary");

				dialog.addConfirmListener(event -> {
					try {
						jobOrderService.delete(this.jobOrder.getId());
						clearForm();
						refreshGrid();
						Notification.show("Orden de trabajo borrada exitosamente.", 3000,
								Notification.Position.BOTTOM_START);
						UI.getCurrent().navigate(JobOrdersView.class);
					} catch (Exception ex) {
						Notification.show("Error al borrar la orden de trabajo: " + ex.getMessage(), 5000,
								Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
					}
				});
				dialog.open();
			}
		});

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
			UI.getCurrent().navigate(JobOrdersView.class);
		});

		save.addClickListener(e -> {
			try {
				if (this.jobOrder == null) {
					this.jobOrder = new JobOrder();
				}
				binder.writeBean(this.jobOrder);
				jobOrderService.save(this.jobOrder);
				clearForm();
				refreshGrid();
				Notification.show("Orden de trabajo guardada.");
				UI.getCurrent().navigate(JobOrdersView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error al guardar la orden de trabajo. Alguien más actualizó el registro mientras realizaba cambios.");
				n.setPosition(Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Fallo al guardar. Verifique que todos los valores son válidos.");
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> jobOrderId = event.getRouteParameters().get(JOBORDER_ID).map(Long::parseLong);
		if (jobOrderId.isPresent()) {
			Optional<JobOrder> jobOrderFromBackend = jobOrderService.get(jobOrderId.get());
			if (jobOrderFromBackend.isPresent()) {
				populateForm(jobOrderFromBackend.get());
			} else {
				Notification.show(
						String.format("La orden de trabajo solicitada no fue encontrada, ID = %s", jobOrderId.get()),
						3000, Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(JobOrdersView.class);
			}
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		this.editorLayoutDiv = new Div();
		this.editorLayoutDiv.setClassName("editor-layout");

		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		init = new DatePicker("inicio");
		end = new DatePicker("fin");
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.findAll());
		study.setItemLabelGenerator(Study::getName);
		hours = new IntegerField("Horas");
		provider = new ComboBox<>("Proveedor");
		provider.setItems(providerService.findAll());
		provider.setItemLabelGenerator(Provider::getName);
		provider.addValueChangeListener(e -> applyProviderHoursRule(e.getValue()));

		formLayout.add(init, end, study, provider, hours);

		editorDiv.add(formLayout);
		createButtonLayout(this.editorLayoutDiv);

		splitLayout.addToSecondary(this.editorLayoutDiv);
		this.editorLayoutDiv.setVisible(false);
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttonLayout.add(save, deleteButton, cancel);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		wrapper.setWidthFull();

		H2 title = new H2("Órdenes de Trabajo");
		HorizontalLayout titleLayout = new HorizontalLayout(title, addButton);
		titleLayout.setWidthFull();
		titleLayout.setAlignItems(Alignment.BASELINE);
		titleLayout.setFlexGrow(1, title);

		studyFilter = new ComboBox<>("Estudio");
		studyFilter.setItems(studyService.findAll());
		studyFilter.setItemLabelGenerator(Study::getName);
		studyFilter.setClearButtonVisible(true);
		studyFilter.addValueChangeListener(e -> refreshGrid());

		providerFilter = new ComboBox<>("Proveedor");
		providerFilter.setItems(providerService.findAll());
		providerFilter.setItemLabelGenerator(Provider::getName);
		providerFilter.setClearButtonVisible(true);
		providerFilter.addValueChangeListener(e -> refreshGrid());

		HorizontalLayout filterLayout = new HorizontalLayout(studyFilter, providerFilter);
		filterLayout.setWidthFull();
		filterLayout.setAlignItems(Alignment.END);

		wrapper.add(titleLayout);
		wrapper.add(filterLayout);
		wrapper.add(grid);
		splitLayout.addToPrimary(wrapper);
	}

	private Specification<JobOrder> buildSpecification() {
		Specification<JobOrder> spec = (root, q, cb) -> cb.and();
		if (studyFilter != null && studyFilter.getValue() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("study"), studyFilter.getValue()));
		}
		if (providerFilter != null && providerFilter.getValue() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("provider"), providerFilter.getValue()));
		}
		return spec;
	}

	private void applyProviderHoursRule(Provider selectedProvider) {
		if (selectedProvider != null && selectedProvider.getType() == ProviderType.PROYECTO) {
			hours.setValue(1);
			hours.setReadOnly(true);
		} else {
			hours.setReadOnly(false);
		}
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(JobOrder value) {
		this.jobOrder = value;
		binder.readBean(this.jobOrder);
		if (this.editorLayoutDiv != null) {
			this.editorLayoutDiv.setVisible(value != null);
		}
		if (this.deleteButton != null) {
			this.deleteButton.setEnabled(value != null && value.getId() != null);
		}
	}
}
