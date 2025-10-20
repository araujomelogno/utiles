package uy.com.bay.utiles.views.budgetconcept;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.enums.MatchType;
import uy.com.bay.utiles.services.BudgetConceptService;

import java.util.Optional;

@PageTitle("Conceptos de Presupuesto")
@Route("budgetconcepts/:budgetconceptID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class BudgetConceptView extends Div implements BeforeEnterObserver {

	private final String BUDGET_CONCEPT_ID = "budgetconceptID";
	private final String BUDGET_CONCEPT_EDIT_ROUTE_TEMPLATE = "budgetconcepts/%s/edit";

	private final Grid<BudgetConcept> grid = new Grid<>(BudgetConcept.class, false);

	private TextField name;
	private TextArea description;
	private ComboBox<MatchType> matchType;

	private final Button cancel = new Button("Cerrar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Borrar");

	private final BeanValidationBinder<BudgetConcept> binder;
	private BudgetConcept budgetConcept;
	private final BudgetConceptService budgetConceptService;

	private Div editorLayoutDiv;

	public BudgetConceptView(BudgetConceptService budgetConceptService) {
		this.budgetConceptService = budgetConceptService;
		addClassNames("budget-concept-view");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid
		grid.addColumn("name").setAutoWidth(true);
		grid.addColumn("description").setAutoWidth(true);
		grid.addColumn("matchType").setAutoWidth(true);

		grid.setItems(query -> budgetConceptService
				.list(com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(BUDGET_CONCEPT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(BudgetConceptView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(BudgetConcept.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.budgetConcept == null) {
					this.budgetConcept = new BudgetConcept();
				}
				binder.writeBean(this.budgetConcept);
				budgetConceptService.save(this.budgetConcept);
				clearForm();
				refreshGrid();
				Notification.show("Concepto de presupuesto guardado.");
				UI.getCurrent().navigate(BudgetConceptView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification
						.show("Error al guardar. Alguien más ha actualizado el registro mientras hacía cambios.");
				n.setPosition(Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification.show("No se pudo guardar. Verifique que todos los valores son válidos.");
			}
		});

		delete.addClickListener(e -> {
			if (this.budgetConcept != null && this.budgetConcept.getId() != null) {
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setHeader("Confirmar borrado");
				dialog.setText("¿Está seguro de que quiere borrar este concepto de presupuesto?");
				dialog.setCancelable(true);
				dialog.setConfirmText("Borrar");
				dialog.setConfirmButtonTheme("error primary");

				dialog.addConfirmListener(event -> {
					try {
						budgetConceptService.delete(this.budgetConcept.getId());
						clearForm();
						refreshGrid();
						Notification.show("Concepto de presupuesto borrado.", 3000, Notification.Position.BOTTOM_START);
					} catch (Exception ex) {
						Notification.show("Error al borrar: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
								.addThemeVariants(NotificationVariant.LUMO_ERROR);
					}
				});
				dialog.open();
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> budgetConceptId = event.getRouteParameters().get(BUDGET_CONCEPT_ID).map(Long::parseLong);

		if (budgetConceptId.isPresent()) {
			Optional<BudgetConcept> budgetConceptFromBackend = budgetConceptService.get(budgetConceptId.get());
			if (budgetConceptFromBackend.isPresent()) {
				populateForm(budgetConceptFromBackend.get());
			} else {
				Notification.show(String.format("El concepto con id = %s no fue encontrado", budgetConceptId.get()),
						3000, Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(BudgetConceptView.class);
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
		name = new TextField("Nombre");
		description = new TextArea("Descripción");
		matchType = new ComboBox<>("Tipo de Coincidencia");
		matchType.setItems(MatchType.values());
		formLayout.add(name, description, matchType);

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
		buttonLayout.add(save, delete, cancel);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		wrapper.setWidthFull();
		splitLayout.addToPrimary(wrapper);

		Button addButton = new Button("Crear");
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.addClickListener(e -> {
			clearForm();
			populateForm(new BudgetConcept());
			editorLayoutDiv.setVisible(true);
		});

		HorizontalLayout titleLayout = new HorizontalLayout(addButton);
		titleLayout.setWidthFull();
		titleLayout.setAlignItems(Alignment.BASELINE);

		wrapper.add(titleLayout, grid);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(BudgetConcept value) {
		this.budgetConcept = value;
		binder.readBean(this.budgetConcept);
		editorLayoutDiv.setVisible(value != null);
		delete.setEnabled(value != null && value.getId() != null);
	}
}