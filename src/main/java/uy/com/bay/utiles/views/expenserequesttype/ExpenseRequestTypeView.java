package uy.com.bay.utiles.views.expenserequesttype;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
//GASTOS
@PageTitle("Conceptos")
@Route("conceptos/:expenseRequestTypeID?/:action?(edit)")
@RolesAllowed("GASTOS")
public class ExpenseRequestTypeView extends Div implements BeforeEnterObserver {

    private final String EXPENSEREQUESTTYPE_ID = "expenseRequestTypeID";
    private final String EXPENSEREQUESTTYPE_EDIT_ROUTE_TEMPLATE = "conceptos/%s/edit";

    private final Grid<ExpenseRequestType> grid = new Grid<>(ExpenseRequestType.class, false);

    private TextField name;
    private TextArea description;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");
    private final Button add = new Button("Agregar Concepto");

    private final BeanValidationBinder<ExpenseRequestType> binder;

    private ExpenseRequestType expenseRequestType;

    private final ExpenseRequestTypeService expenseRequestTypeService;

    private Div editorLayoutDiv;

    public ExpenseRequestTypeView(ExpenseRequestTypeService expenseRequestTypeService) {
        this.expenseRequestTypeService = expenseRequestTypeService;
        addClassNames("expense-request-type-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("name").setAutoWidth(true).setHeader("Concepto");
        grid.addColumn("description").setAutoWidth(true).setHeader("Descripción");
        grid.setItems(query -> expenseRequestTypeService.list(
                com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(EXPENSEREQUESTTYPE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ExpenseRequestTypeView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(ExpenseRequestType.class);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.expenseRequestType == null) {
                    this.expenseRequestType = new ExpenseRequestType();
                }
                binder.writeBean(this.expenseRequestType);
                expenseRequestTypeService.save(this.expenseRequestType);
                clearForm();
                refreshGrid();
                Notification.show("Concepto guardado.");
                UI.getCurrent().navigate(ExpenseRequestTypeView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error al guardar el concepto. Alguien más ha actualizado el registro mientras usted realizaba cambios.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("No se pudo guardar el concepto. Verifique que todos los valores sean válidos.");
            }
        });

        delete.addClickListener(e -> {
            if (this.expenseRequestType != null && this.expenseRequestType.getId() != null) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Confirmar borrado");
                dialog.setText("¿Está seguro de que desea borrar este concepto? Esta acción no se puede deshacer.");
                dialog.setCancelable(true);
                dialog.setConfirmText("Borrar");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    try {
                        expenseRequestTypeService.delete(this.expenseRequestType.getId());
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
        });

        add.addClickListener(e -> {
            clearForm();
            this.expenseRequestType = new ExpenseRequestType();
            binder.readBean(this.expenseRequestType);
            editorLayoutDiv.setVisible(true);
            delete.setEnabled(false);
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> expenseRequestTypeId = event.getRouteParameters().get(EXPENSEREQUESTTYPE_ID).map(Long::parseLong);
        if (expenseRequestTypeId.isPresent()) {
            Optional<ExpenseRequestType> expenseRequestTypeFromBackend = expenseRequestTypeService.get(expenseRequestTypeId.get());
            if (expenseRequestTypeFromBackend.isPresent()) {
                populateForm(expenseRequestTypeFromBackend.get());
            } else {
                Notification.show(
                        String.format("El concepto solicitado no fue encontrado, ID = %s", expenseRequestTypeId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(ExpenseRequestTypeView.class);
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
        name = new TextField("Concepto");
        description = new TextArea("Descripción");
        formLayout.add(name, description);

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

        H2 title = new H2("Conceptos");
        HorizontalLayout titleLayout = new HorizontalLayout(title, add);
        titleLayout.setWidthFull();
        titleLayout.setAlignItems(Alignment.BASELINE);
        titleLayout.setFlexGrow(1, title);

        splitLayout.addToPrimary(wrapper);
        wrapper.add(titleLayout, grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(ExpenseRequestType value) {
        this.expenseRequestType = value;
        binder.readBean(this.expenseRequestType);
        editorLayoutDiv.setVisible(value != null);
        delete.setEnabled(value != null && value.getId() != null);
    }
}
