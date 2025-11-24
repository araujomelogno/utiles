package uy.com.bay.utiles.views.extraconcept;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.service.ExtraConceptService;

@PageTitle("Conceptos de extra")
@Route("extraconcepts/:extraConceptID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class ExtraConceptView extends Div implements BeforeEnterObserver {

	private final String EXTRACONCEPT_ID = "extraConceptID";
	private final String EXTRACONCEPT_EDIT_ROUTE_TEMPLATE = "extraconcepts/%s/edit";

	private final Grid<ExtraConcept> grid = new Grid<>(ExtraConcept.class, false);
	private TextField description;

	private final Button cancel = new Button("Cerrar");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Borrar");

	private final BeanValidationBinder<ExtraConcept> binder;
	private ExtraConcept extraConcept;
	private final ExtraConceptService extraConceptService;

	private Div editorLayoutDiv;

	public ExtraConceptView(ExtraConceptService extraConceptService) {
		this.extraConceptService = extraConceptService;
		addClassNames("extraconcept-view");

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);

		add(splitLayout);

		// Configure Grid

		grid.addColumn("description").setHeader("Descripción").setAutoWidth(true);
		grid.setItems(query -> extraConceptService.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				UI.getCurrent().navigate(String.format(EXTRACONCEPT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm();
				UI.getCurrent().navigate(ExtraConceptView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(ExtraConcept.class);
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.extraConcept == null) {
					this.extraConcept = new ExtraConcept();
				}

				binder.writeBean(this.extraConcept);
				extraConceptService.save(this.extraConcept);
				clearForm();
				refreshGrid();
				Notification.show("Concepto de extra guardado.");
				UI.getCurrent().navigate(ExtraConceptView.class);
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
			if (this.extraConcept != null && this.extraConcept.getId() != null) {
				extraConceptService.delete(this.extraConcept.getId());
				clearForm();
				refreshGrid();
				Notification.show("Concepto de extra borrado.");
				UI.getCurrent().navigate(ExtraConceptView.class);
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> extraConceptId = event.getRouteParameters().get(EXTRACONCEPT_ID).map(Long::parseLong);
		if (extraConceptId.isPresent()) {
			Optional<ExtraConcept> extraConceptFromBackend = extraConceptService.get(extraConceptId.get());
			if (extraConceptFromBackend.isPresent()) {
				populateForm(extraConceptFromBackend.get());
			} else {
				Notification.show(
						String.format("El concepto de extra solicitado no se encontró, ID = %s", extraConceptId.get()),
						3000, Notification.Position.BOTTOM_START);
				refreshGrid();
				event.forwardTo(ExtraConceptView.class);
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

		description = new TextField("Descripción");
		formLayout.add(description);

		editorDiv.add(formLayout);
		createButtonLayout(editorLayoutDiv);

		splitLayout.addToSecondary(editorLayoutDiv);
		editorLayoutDiv.setVisible(false);
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		buttonLayout.add(save, delete, cancel);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		wrapper.setWidthFull();
		splitLayout.addToPrimary(wrapper);

		Button createButton = new Button("Crear");
		createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		createButton.addClickListener(e -> {
			clearForm();
			populateForm(new ExtraConcept());
		});

		Button helloButton = new Button("hola");
		helloButton.addClickListener(e -> {
			com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
			dialog.add(new com.vaadin.flow.component.Text("Hola mundo"));
			dialog.open();
		});

		HorizontalLayout topLayout = new HorizontalLayout(createButton, helloButton);
		topLayout.setWidthFull();
		topLayout.setAlignItems(Alignment.BASELINE);

		wrapper.add(topLayout, grid);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void clearForm() {
		populateForm(null);
	}

	private void populateForm(ExtraConcept value) {
		this.extraConcept = value;

		binder.readBean(this.extraConcept);
		editorLayoutDiv.setVisible(value != null);
	}
}