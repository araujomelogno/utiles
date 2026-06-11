package uy.com.bay.utiles.views.providers;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Provider;
import uy.com.bay.utiles.data.ProviderType;
import uy.com.bay.utiles.services.ProviderService;

@PageTitle("Proveedores")
@Route("providers/:providerID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class ProvidersView extends Div implements BeforeEnterObserver {

    private final String PROVIDER_ID = "providerID";
    private final String PROVIDER_EDIT_ROUTE_TEMPLATE = "providers/%s/edit";

    private final Grid<Provider> grid = new Grid<>(Provider.class, false);

    private TextField name;
    private IntegerField monthlyCapacity;
    private ComboBox<ProviderType> type;

    private Button addButton;

    private final Button cancel = new Button("Cerrar");
    private final Button save = new Button("Guardar");
    private Button deleteButton;

    private final BeanValidationBinder<Provider> binder;

    private Provider provider;
    private Div editorLayoutDiv;

    private final ProviderService providerService;

    public ProvidersView(ProviderService providerService) {
        this.providerService = providerService;
        this.binder = new BeanValidationBinder<>(Provider.class);
        addClassNames("providers-view");

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

        grid.addColumn("name").setHeader("Nombre").setAutoWidth(true);
        grid.addColumn("monthlyCapacity").setHeader("Capacidad Mensual (hs)").setAutoWidth(true);
        grid.addColumn("type").setHeader("Tipo").setAutoWidth(true);

        grid.setItems(query -> providerService.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PROVIDER_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ProvidersView.class);
            }
        });

        binder.bindInstanceFields(this);
    }

    private void setupButtonListeners() {
        addButton.addClickListener(e -> {
            clearForm();
            this.provider = new Provider();
            binder.readBean(this.provider);
            if (this.editorLayoutDiv != null) {
                this.editorLayoutDiv.setVisible(true);
            }
            if (this.deleteButton != null) {
                this.deleteButton.setEnabled(false);
            }
        });

        deleteButton.addClickListener(e -> {
            if (this.provider != null && this.provider.getId() != null) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Confirmar Borrado");
                dialog.setText(
                        "¿Estás seguro de que quieres borrar este proveedor? Esta acción no se puede deshacer.");
                dialog.setCancelable(true);
                dialog.setConfirmText("Borrar");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    try {
                        providerService.delete(this.provider.getId());
                        clearForm();
                        refreshGrid();
                        Notification.show("Proveedor borrado exitosamente.", 3000, Notification.Position.BOTTOM_START);
                        UI.getCurrent().navigate(ProvidersView.class);
                    } catch (Exception ex) {
                        Notification
                                .show("Error al borrar el proveedor: " + ex.getMessage(), 5000,
                                        Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                dialog.open();
            }
        });

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
            UI.getCurrent().navigate(ProvidersView.class);
        });

        save.addClickListener(e -> {
            try {
                if (this.provider == null) {
                    this.provider = new Provider();
                }
                binder.writeBean(this.provider);
                providerService.save(this.provider);
                clearForm();
                refreshGrid();
                Notification.show("Proveedor guardado.");
                UI.getCurrent().navigate(ProvidersView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error al guardar el proveedor. Alguien más actualizó el registro mientras realizaba cambios.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Fallo al guardar. Verifique que todos los valores son válidos.");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> providerId = event.getRouteParameters().get(PROVIDER_ID).map(Long::parseLong);
        if (providerId.isPresent()) {
            Optional<Provider> providerFromBackend = providerService.get(providerId.get());
            if (providerFromBackend.isPresent()) {
                populateForm(providerFromBackend.get());
            } else {
                Notification.show(
                        String.format("El proveedor solicitado no fue encontrado, ID = %s", providerId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(ProvidersView.class);
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
        name = new TextField("Nombre");
        monthlyCapacity = new IntegerField("Capacidad Mensual");
        type = new ComboBox<>("Tipo");
        type.setItems(ProviderType.values());
        formLayout.add(name, monthlyCapacity, type);

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

        H2 title = new H2("Proveedores");
        HorizontalLayout titleLayout = new HorizontalLayout(title, addButton);
        titleLayout.setWidthFull();
        titleLayout.setAlignItems(Alignment.BASELINE);
        titleLayout.setFlexGrow(1, title);

        wrapper.add(titleLayout);
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

    private void populateForm(Provider value) {
        this.provider = value;
        binder.readBean(this.provider);
        if (this.editorLayoutDiv != null) {
            this.editorLayoutDiv.setVisible(value != null);
        }
        if (this.deleteButton != null) {
            this.deleteButton.setEnabled(value != null && value.getId() != null);
        }
    }
}
