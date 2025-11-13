package uy.com.bay.utiles.views.concepts;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uy.com.bay.utiles.entities.Concept;
import uy.com.bay.utiles.services.ConceptService;
import uy.com.bay.utiles.views.MainLayout;

import java.util.Optional;

@PageTitle("Conceptos")
@Route(value = "concepts/:conceptID?/:action?(edit)", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class ConceptsView extends Div implements BeforeEnterObserver {

    private final String CONCEPT_ID = "conceptID";
    private final String CONCEPT_EDIT_ROUTE_TEMPLATE = "concepts/%s/edit";

    private final Grid<Concept> grid = new Grid<>(Concept.class, false);
    private final BeanValidationBinder<Concept> binder;
    private final ConceptService conceptService;

    private ConceptForm form;
    private Concept concept;

    public ConceptsView(ConceptService conceptService) {
        this.conceptService = conceptService;
        this.binder = new BeanValidationBinder<>(Concept.class);

        addClassNames("concepts-view");
        setSizeFull();

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        grid.addColumn("name").setAutoWidth(true).setHeader("Nombre");
        grid.addColumn("description").setAutoWidth(true).setHeader("Descripción");
        grid.setItems(query -> conceptService.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(CONCEPT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ConceptsView.class);
            }
        });

        binder.bindInstanceFields(this);
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        form = new ConceptForm();
        binder.bindInstanceFields(form);

        Div editorLayoutDiv = new Div(form);
        editorLayoutDiv.setClassName("editor-layout");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        Button save = new Button("Guardar", event -> validateAndSave());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button delete = new Button("Borrar", event -> deleteConcept());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button cancel = new Button("Cerrar", e -> clearForm());
        buttonLayout.add(save, delete, cancel);

        editorLayoutDiv.add(buttonLayout);
        splitLayout.addToSecondary(editorLayoutDiv);
        form.setVisible(false);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setId("grid-wrapper");
        wrapper.setWidthFull();
        splitLayout.addToPrimary(wrapper);

        Button addButton = new Button("Crear");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(click -> {
            populateForm(new Concept());
            form.setVisible(true);
        });

        wrapper.add(addButton, grid);
    }

    private void validateAndSave() {
        try {
            binder.writeBean(concept);
            conceptService.save(concept);
            clearForm();
            refreshGrid();
            Notification.show("Concepto guardado correctamente.", 3000, Notification.Position.BOTTOM_START);
            UI.getCurrent().navigate(ConceptsView.class);
        } catch (ObjectOptimisticLockingFailureException exception) {
            Notification.show("Error al guardar el concepto. Otro usuario ha modificado el registro.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (ValidationException validationException) {
            Notification.show("No se pudo guardar el concepto. Verifique que todos los valores sean válidos.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteConcept() {
        if (concept != null && concept.getId() != null) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Confirmar borrado");
            dialog.setText("¿Está seguro de que desea borrar este concepto? Esta acción no se puede deshacer.");
            dialog.setCancelable(true);
            dialog.setConfirmText("Borrar");
            dialog.setConfirmButtonTheme("error primary");
            dialog.addConfirmListener(event -> {
                try {
                    conceptService.delete(concept.getId());
                    clearForm();
                    refreshGrid();
                    Notification.show("Concepto borrado exitosamente.", 3000, Notification.Position.BOTTOM_START);
                } catch (Exception ex) {
                    Notification.show("Error al borrar el concepto: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            dialog.open();
        }
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Concept value) {
        this.concept = value;
        binder.readBean(this.concept);
        if (value == null) {
            form.setVisible(false);
        } else {
            form.setVisible(true);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> conceptId = event.getRouteParameters().get(CONCEPT_ID).map(Long::parseLong);
        if (conceptId.isPresent()) {
            Optional<Concept> conceptFromBackend = conceptService.get(conceptId.get());
            if (conceptFromBackend.isPresent()) {
                populateForm(conceptFromBackend.get());
            } else {
                Notification.show(String.format("El concepto solicitado no se encontró, ID = %s", conceptId.get()), 3000, Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(ConceptsView.class);
            }
        } else {
            clearForm();
        }
    }
}
