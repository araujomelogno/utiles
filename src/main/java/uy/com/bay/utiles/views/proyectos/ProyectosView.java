package uy.com.bay.utiles.views.proyectos;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog; // Added import
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseDataRepository;
import uy.com.bay.utiles.data.service.FieldworkService;
import uy.com.bay.utiles.services.ExcelExportService;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseTransferFileService;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.StudyService;

@PageTitle("Proyectos")
@Route("/:proyectoID?/:action?(edit)")
@Menu(order = 0, icon = LineAwesomeIconUrl.BRIEFCASE_SOLID)
@RouteAlias("studies")
@RolesAllowed("ADMIN")
public class ProyectosView extends Div implements BeforeEnterObserver {

	private final String PROYECTO_ID = "proyectoID";
	private final String PROYECTO_EDIT_ROUTE_TEMPLATE = "/%s/edit";

	private final Grid<Study> grid = new Grid<>(Study.class, false);

	private TextField name;
	private TextField odooId;
	private TextArea obs;
	private Checkbox showSurveyor;
	private TextField totalTransfered;
	private TextField totalReportedCost;

	private Button addButton;
	private TextField nameFilter;
	private TextField odooIdFilter;
	private TextField obsFilter;

	private final Button cancel = new Button("Cancel");
	private final Button save = new Button("Save");
	private Button deleteButton; // Added deleteButton declaration
	private Button viewMovementsButton;
	private Button viewFieldworksButton;

	private final BeanValidationBinder<Study> binder;

	private Study proyecto;
	private Div editorLayoutDiv; // Added field declaration

	private final StudyService proyectoService;
	private final JournalEntryService journalEntryService;
	private final AlchemerSurveyResponseDataRepository alchemerSurveyResponseDataRepository;
	private final ExpenseReportFileService expenseReportFileService;
	private final ExpenseTransferFileService expenseTransferFileService;
	private final FieldworkService fieldworkService;
	private final uy.com.bay.utiles.services.ExcelExportService excelExportService;

	public ProyectosView(StudyService proyectoService, JournalEntryService journalEntryService,
			AlchemerSurveyResponseDataRepository alchemerSurveyResponseDataRepository,
			ExpenseReportFileService expenseReportFileService, ExpenseTransferFileService expenseTransferFileService,
			FieldworkService fieldworkService, uy.com.bay.utiles.services.ExcelExportService excelExportService) {
		this.proyectoService = proyectoService;
		this.journalEntryService = journalEntryService;
		this.fieldworkService = fieldworkService;
		this.alchemerSurveyResponseDataRepository = alchemerSurveyResponseDataRepository;
		this.expenseReportFileService = expenseReportFileService;
		this.expenseTransferFileService = expenseTransferFileService;
		this.excelExportService = excelExportService;
		this.binder = new BeanValidationBinder<>(Study.class); // Moved initialization here
		addClassNames("proyectos-view");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		addButton = new Button("Agregar Proyecto");
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY); // Optional: Add theme for consistency

		deleteButton = new Button("Borrar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		deleteButton.setEnabled(false);

		viewMovementsButton = new Button("Ver movimientos de gastos");
		viewMovementsButton.setEnabled(false);

		viewFieldworksButton = new Button("Ver solicitudes de campo");
		viewFieldworksButton.setEnabled(false);

		nameFilter = new TextField();
		nameFilter.setPlaceholder("Nombre...");
		nameFilter.setClearButtonVisible(true);
		nameFilter.setWidth("100%");
		nameFilter.addValueChangeListener(e -> refreshGrid());

		odooIdFilter = new TextField();
		odooIdFilter.setPlaceholder("Odoo ID...");
		odooIdFilter.setClearButtonVisible(true);
		odooIdFilter.setWidth("100%");
		odooIdFilter.addValueChangeListener(e -> refreshGrid());

		obsFilter = new TextField();
		obsFilter.setPlaceholder("Obs...");
		obsFilter.setClearButtonVisible(true);
		obsFilter.setWidth("100%");
		obsFilter.addValueChangeListener(e -> refreshGrid());

		setupButtonListeners(); // Call to new method

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid
		grid.addColumn("name").setHeader("Nombre").setAutoWidth(true);
		grid.addColumn("odooId").setHeader("Odoo ID").setAutoWidth(true);
		grid.addColumn("obs").setHeader("Observaciones").setAutoWidth(true);

		grid.setItems(query -> {
			String nameVal = nameFilter.getValue() != null ? nameFilter.getValue().trim().toLowerCase() : "";
			String odooVal = odooIdFilter.getValue() != null ? odooIdFilter.getValue().trim().toLowerCase() : "";
			String obsVal = obsFilter.getValue() != null ? obsFilter.getValue().trim().toLowerCase() : "";

			java.util.stream.Stream<Study> stream = proyectoService
					.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream();

			if (!nameVal.isEmpty()) {
				stream = stream.filter(p -> p.getName() != null && p.getName().toLowerCase().contains(nameVal));
			}
			if (!odooVal.isEmpty()) {
				stream = stream.filter(p -> p.getOdooId() != null && p.getOdooId().toLowerCase().contains(odooVal));
			}
			if (!obsVal.isEmpty()) {
				stream = stream.filter(p -> p.getObs() != null && p.getObs().toLowerCase().contains(obsVal));
			}
			return stream;
		});
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(PROYECTO_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(ProyectosView.class);
			}
		});

		// Configure Form
		// binder = new BeanValidationBinder<>(Proyecto.class); // Removed from here

		// Bind fields. This is where you'd define e.g. validation rules

		binder.bindInstanceFields(this);
	}

	private void setupButtonListeners() {
		addButton.addClickListener(e -> {
			clearForm();
			this.proyecto = new Study();
			binder.readBean(this.proyecto);
			if (this.editorLayoutDiv != null) {
				this.editorLayoutDiv.setVisible(true);
			}
			if (this.deleteButton != null) {
				this.deleteButton.setEnabled(false);
			}
		});

		deleteButton.addClickListener(e -> {
			if (this.proyecto != null && this.proyecto.getId() != null) {
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setHeader("Confirmar Borrado");
				dialog.setText("¿Estás seguro de que quieres borrar este proyecto? Esta acción no se puede deshacer.");
				dialog.setCancelable(true);
				dialog.setConfirmText("Borrar");
				dialog.setConfirmButtonTheme("error primary");

				dialog.addConfirmListener(event -> {
					try {
						proyectoService.delete(this.proyecto.getId());
						clearForm();
						refreshGrid();
						Notification.show("Proyecto borrado exitosamente.", 3000, Notification.Position.BOTTOM_START);
					} catch (Exception ex) {
						Notification.show("Error al borrar el proyecto: " + ex.getMessage(), 5000,
								Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
					}
				});
				dialog.open();
			}
		});

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.proyecto == null) {
					this.proyecto = new Study();
				}
				binder.writeBean(this.proyecto);
				proyectoService.save(this.proyecto);
				clearForm();
				refreshGrid();
				Notification.show("Data updated");
				UI.getCurrent().navigate(ProyectosView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error updating the data. Somebody else has updated the record while you were making changes.");
				n.setPosition(Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("Failed to update the data. Check again that all values are valid");
			}
		});

		viewMovementsButton.addClickListener(e -> {
			if (this.proyecto != null) {
				JournalEntryDialog dialog = new JournalEntryDialog(this.proyecto, journalEntryService,
						excelExportService);

				dialog.open();
			}
		});

		viewFieldworksButton.addClickListener(e -> {
			if (this.proyecto != null) {
				FieldworkDialog dialog = new FieldworkDialog(this.proyecto, fieldworkService);
				dialog.open();
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> proyectoId = event.getRouteParameters().get(PROYECTO_ID).map(Long::parseLong);
		if (proyectoId.isPresent()) {
			Optional<Study> proyectoFromBackend = proyectoService.get(proyectoId.get());
			if (proyectoFromBackend.isPresent()) {
				populateForm(proyectoFromBackend.get());
			} else {
				Notification.show(String.format("The requested proyecto was not found, ID = %s", proyectoId.get()),
						3000, Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid();
				event.forwardTo(ProyectosView.class);
			}
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		this.editorLayoutDiv = new Div(); // Changed to use the class field
		this.editorLayoutDiv.setClassName("editor-layout");

		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		name = new TextField("Name");
		odooId = new TextField("Odoo Id");
		obs = new TextArea("Observaciones");
		showSurveyor = new Checkbox("Mostrar a encuestador");
		totalTransfered = new TextField("Total de gastos transferidos");
		totalTransfered.setReadOnly(true);
		totalReportedCost = new TextField("Total de gastos rendidos");
		totalReportedCost.setReadOnly(true);
		formLayout.add(name, odooId, obs, showSurveyor, totalTransfered, totalReportedCost);

		editorDiv.add(formLayout);
		editorDiv.add(viewMovementsButton);
		editorDiv.add(viewFieldworksButton);
		createButtonLayout(this.editorLayoutDiv);

		splitLayout.addToSecondary(this.editorLayoutDiv);
		this.editorLayoutDiv.setVisible(false); // Set initial visibility
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
		wrapper.setWidthFull(); // Ensure wrapper takes full width

		// Title Layout
		H2 title = new H2("Proyectos");
		HorizontalLayout titleLayout = new HorizontalLayout(title, addButton);
		titleLayout.setWidthFull();
		titleLayout.setAlignItems(Alignment.BASELINE); // Align items nicely
		titleLayout.setFlexGrow(1, title); // Title takes available space

		// Filter Layout
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setWidthFull();
		filterLayout.add(nameFilter, odooIdFilter, obsFilter);

		wrapper.add(titleLayout);
		wrapper.add(filterLayout);
		wrapper.add(grid);
		splitLayout.addToPrimary(wrapper);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(Study value) {
		this.proyecto = value;
		binder.readBean(this.proyecto);
		if (value != null) {
			totalTransfered.setValue(Double.valueOf(value.getTotalTransfered()).toString());
			totalReportedCost.setValue(Double.valueOf(value.getTotalReportedCost()).toString());
		} else {
			totalTransfered.setValue("");
			totalReportedCost.setValue("");
		}
		if (this.editorLayoutDiv != null) {
			this.editorLayoutDiv.setVisible(value != null);
		}
		if (this.deleteButton != null) {
			this.deleteButton.setEnabled(value != null && value.getId() != null);
		}
		if (this.viewMovementsButton != null) {
			this.viewMovementsButton.setEnabled(value != null && value.getId() != null);
		}
		if (this.viewFieldworksButton != null) {
			this.viewFieldworksButton.setEnabled(value != null && value.getId() != null);
		}
	}
}