package uy.com.bay.utiles.views.extraconcept;

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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.service.ExtraConceptService;
import uy.com.bay.utiles.views.MainLayout;

import java.util.Optional;

@PageTitle("Conceptos de extra")
@Route(value = "extra-concept/:extraConceptID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class ExtraConceptView extends Div implements BeforeEnterObserver {

    private final String EXTRACONCEPT_ID = "extraConceptID";
    private final String EXTRACONCEPT_EDIT_ROUTE_TEMPLATE = "extra-concept/%s/edit";

    private final Grid<ExtraConcept> grid = new Grid<>(ExtraConcept.class, false);

    private TextField name;
    private TextField description;

    private final Button cancel = new Button("Cerrar");
    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");
    private final Button create = new Button("Crear");

    private final BeanValidationBinder<ExtraConcept> binder;

    private ExtraConcept extraConcept;
    private Div editorLayoutDiv;

    private final ExtraConceptService extraConceptService;

    public ExtraConceptView(ExtraConceptService extraConceptService) {
        this.extraConceptService = extraConceptService;
        addClassNames("extra-concept-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("name").setHeader("Nombre").setAutoWidth(true);
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

        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClickListener(e -> {
            clearForm();
            this.extraConcept = new ExtraConcept();
            binder.readBean(this.extraConcept);
            this.editorLayoutDiv.setVisible(true);
        });

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
                Notification n = Notification.show(
                        "Error al guardar el concepto de extra. Alguien más ha actualizado el registro mientras usted realizaba cambios.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("No se pudo guardar el concepto de extra. Verifique que todos los valores son válidos.");
            }
        });

        delete.addClickListener(e -> {
            if (this.extraConcept != null) {
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
        this.editorLayoutDiv = new Div();
        this.editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        this.editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        name = new TextField("Nombre");
        description = new TextField("Descripción");
        formLayout.add(name, description);

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
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttonLayout.add(save, delete, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        wrapper.setWidthFull();
        splitLayout.addToPrimary(wrapper);

        H2 title = new H2("Conceptos de extra");
        HorizontalLayout titleLayout = new HorizontalLayout(title, create);
        titleLayout.setWidthFull();
        titleLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        titleLayout.setFlexGrow(1, title);

        wrapper.add(titleLayout);
        wrapper.add(grid);
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
        if (this.editorLayoutDiv != null) {
            this.editorLayoutDiv.setVisible(value != null);
        }
    }
}