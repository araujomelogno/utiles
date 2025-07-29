package uy.com.bay.utiles.views.encuestadores;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.Encuestador;
import uy.com.bay.utiles.services.EncuestadorService;

@PageTitle("Encuestadores")
@Route("surveyors/:encuestadorID?/:action?(edit)")
@Menu(order = 1, icon = LineAwesomeIconUrl.USER_ALT_SOLID)
@PermitAll
public class EncuestadoresView extends Div implements BeforeEnterObserver {

    private final String ENCUESTADOR_ID = "encuestadorID";
    private final String ENCUESTADOR_EDIT_ROUTE_TEMPLATE = "surveyors/%s/edit";

    private final Grid<Encuestador> grid = new Grid<>(Encuestador.class, false);

    private TextField firstName;
    private TextField lastName;
    private TextField ci;

    private Button addButton;
    private TextField firstNameFilter;
    private TextField lastNameFilter;
    private TextField ciFilter;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");
    private Button deleteButton; // Added deleteButton declaration

    private final BeanValidationBinder<Encuestador> binder;

    private Encuestador encuestador;
    private Div editorLayoutDiv; // Added field declaration

    private final EncuestadorService encuestadorService;

    public EncuestadoresView(EncuestadorService encuestadorService) {
        this.encuestadorService = encuestadorService;
        this.binder = new BeanValidationBinder<>(Encuestador.class); // Moved initialization here
        addClassNames("encuestadores-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        addButton = new Button("Agregar Encuestador");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
         
        deleteButton = new Button("Borrar");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setEnabled(false);

         

        firstNameFilter = new TextField();
        firstNameFilter.setPlaceholder("Nombre...");
        firstNameFilter.setClearButtonVisible(true);
        firstNameFilter.setWidth("100%");
        firstNameFilter.addValueChangeListener(e -> refreshGrid()); // Asume que refreshGrid() llama a dataProvider.refreshAll()

        lastNameFilter = new TextField();
        lastNameFilter.setPlaceholder("Apellido...");
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setWidth("100%");
        lastNameFilter.addValueChangeListener(e -> refreshGrid());

        ciFilter = new TextField();
        ciFilter.setPlaceholder("CI...");
        ciFilter.setClearButtonVisible(true);
        ciFilter.setWidth("100%");
        ciFilter.addValueChangeListener(e -> refreshGrid());
 
        setupButtonListeners(); // Call to new method
 
        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("firstName").setHeader("Nombre").setAutoWidth(true);
        grid.addColumn("lastName").setHeader("Apellido").setAutoWidth(true);
        grid.addColumn("ci").setHeader("CI").setAutoWidth(true);

        grid.setItems(query -> {
            String fnameFilter = firstNameFilter.getValue() != null ? firstNameFilter.getValue().trim().toLowerCase() : "";
            String lnameFilter = lastNameFilter.getValue() != null ? lastNameFilter.getValue().trim().toLowerCase() : "";
            String ciValFilter = ciFilter.getValue() != null ? ciFilter.getValue().trim().toLowerCase() : "";

            // Obtener el stream del servicio
            java.util.stream.Stream<Encuestador> stream = encuestadorService.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream();

            // Aplicar filtros si hay texto en los campos de filtro
            if (!fnameFilter.isEmpty()) {
                stream = stream.filter(enc -> enc.getFirstName() != null && enc.getFirstName().toLowerCase().contains(fnameFilter));
            }
            if (!lnameFilter.isEmpty()) {
                stream = stream.filter(enc -> enc.getLastName() != null && enc.getLastName().toLowerCase().contains(lnameFilter));
            }
            if (!ciValFilter.isEmpty()) {
                stream = stream.filter(enc -> enc.getCi() != null && enc.getCi().toLowerCase().contains(ciValFilter));
            }
            return stream;
        });
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(ENCUESTADOR_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(EncuestadoresView.class);
            }
        });

        // Configure Form
        // binder = new BeanValidationBinder<>(Encuestador.class); // Removed from here

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);
    }

    private void setupButtonListeners() {
        addButton.addClickListener(e -> {
            clearForm();
            this.encuestador = new Encuestador();
            binder.readBean(this.encuestador);
            if (this.editorLayoutDiv != null) {
                 this.editorLayoutDiv.setVisible(true);
            }
            if (this.deleteButton != null) {
                this.deleteButton.setEnabled(false);
            }
        });

        deleteButton.addClickListener(e -> {
            if (this.encuestador != null && this.encuestador.getId() != null) {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Confirmar Borrado");
                dialog.setText("¿Estás seguro de que quieres borrar este encuestador? Esta acción no se puede deshacer.");
                dialog.setCancelable(true);
                dialog.setConfirmText("Borrar");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    try {
                        encuestadorService.delete(this.encuestador.getId());
                        clearForm();
                        refreshGrid();
                        Notification.show("Encuestador borrado exitosamente.", 3000, Notification.Position.BOTTOM_START);
                    } catch (Exception ex) {
                        Notification.show("Error al borrar el encuestador: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
                if (this.encuestador == null) {
                    this.encuestador = new Encuestador();
                }
                binder.writeBean(this.encuestador);
                encuestadorService.save(this.encuestador);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(EncuestadoresView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> encuestadorId = event.getRouteParameters().get(ENCUESTADOR_ID).map(Long::parseLong);
        if (encuestadorId.isPresent()) {
            Optional<Encuestador> encuestadorFromBackend = encuestadorService.get(encuestadorId.get());
            if (encuestadorFromBackend.isPresent()) {
                populateForm(encuestadorFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested encuestador was not found, ID = %s", encuestadorId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(EncuestadoresView.class);
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
        firstName = new TextField("First Name");
        lastName = new TextField("Last Name");
        ci = new TextField("Ci");
        formLayout.add(firstName, lastName, ci);

        editorDiv.add(formLayout);
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
        H2 title = new H2("Encuestadores");
        HorizontalLayout titleLayout = new HorizontalLayout(title, addButton);
        titleLayout.setWidthFull();
        titleLayout.setAlignItems(Alignment.BASELINE); // Align items nicely
        titleLayout.setFlexGrow(1, title); // Title takes available space

        // Filter Layout
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.add(firstNameFilter, lastNameFilter, ciFilter);

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

    private void populateForm(Encuestador value) {
        this.encuestador = value;
        binder.readBean(this.encuestador);
        if (this.editorLayoutDiv != null) {
            this.editorLayoutDiv.setVisible(value != null);
        }
        if (this.deleteButton != null) {
            this.deleteButton.setEnabled(value != null && value.getId() != null);
        }
    }
}
