package uy.com.bay.utiles.views.budget;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.services.BudgetService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.BudgetConceptService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Optional;
import java.util.Date;
import java.util.ArrayList;

@PageTitle("Presupuestos")
@Route("budgets/:budgetID?/:action?(edit)")
@RolesAllowed("ADMIN")
public class BudgetView extends Div implements BeforeEnterObserver {

    private final String BUDGET_ID = "budgetID";
    private final String BUDGET_EDIT_ROUTE_TEMPLATE = "budgets/%s/edit";

    private final Grid<Budget> grid = new Grid<>(Budget.class, false);
    private Grid<BudgetEntry> entriesGrid;

    private TextField created;
    private ComboBox<Study> study;

    private final Button cancel = new Button("Cerrar");
    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");
    private Button addButton;
    private Button addEntryButton;

    private final BeanValidationBinder<Budget> binder;

    private Budget budget;

    private final BudgetService budgetService;
    private final StudyService studyService;
    private final BudgetConceptService budgetConceptService;

    private Div editorLayoutDiv;

    public BudgetView(BudgetService budgetService, StudyService studyService, BudgetConceptService budgetConceptService) {
        this.budgetService = budgetService;
        this.studyService = studyService;
        this.budgetConceptService = budgetConceptService;
        addClassNames("budget-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn(budget -> budget.getStudy() != null ? budget.getStudy().getName() : "N/A").setHeader("Estudio").setAutoWidth(true);
        grid.addColumn("created").setHeader("Fecha de Creación").setAutoWidth(true);

        grid.setItems(query -> budgetService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(BUDGET_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(BudgetView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Budget.class);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.budget == null) {
                    this.budget = new Budget();
                }
                binder.writeBean(this.budget);
                budgetService.save(this.budget);
                clearForm();
                refreshGrid();
                Notification.show("Presupuesto guardado.");
                UI.getCurrent().navigate(BudgetView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error: El registro ha sido modificado por otro usuario.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Error de validación. Revise los campos.");
            }
        });

        delete.addClickListener(e -> {
            if (this.budget != null) {
                budgetService.delete(this.budget.getId());
                clearForm();
                refreshGrid();
                Notification.show("Presupuesto borrado.");
                UI.getCurrent().navigate(BudgetView.class);
            }
        });

        addButton.addClickListener(e -> {
            clearForm();
            this.budget = new Budget();
            this.budget.setCreated(new Date());
            this.budget.setEntries(new ArrayList<>());
            binder.readBean(this.budget);
            entriesGrid.setItems(this.budget.getEntries());
            editorLayoutDiv.setVisible(true);
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> budgetId = event.getRouteParameters().get(BUDGET_ID).map(Long::parseLong);
        if (budgetId.isPresent()) {
            Optional<Budget> budgetFromBackend = budgetService.get(budgetId.get());
            if (budgetFromBackend.isPresent()) {
                populateForm(budgetFromBackend.get());
            } else {
                Notification.show(
                        String.format("El presupuesto solicitado no fue encontrado, ID = %s", budgetId.get()),
                        3000, Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(BudgetView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");
        editorLayoutDiv.setVisible(false);

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        created = new DatePicker("Fecha de Creación");
        created.setReadOnly(true);
        study = new ComboBox<>("Estudio");
        study.setItems(studyService.findAll());
        study.setItemLabelGenerator(Study::getName);
        formLayout.add(created, study);

        editorDiv.add(formLayout);

        // Nested Grid for Budget Entries
        entriesGrid = new Grid<>(BudgetEntry.class, false);
        entriesGrid.setHeight("200px");
        Binder<BudgetEntry> entryBinder = new Binder<>(BudgetEntry.class);

        Grid.Column<BudgetEntry> conceptColumn = entriesGrid.addColumn(BudgetEntry::getConceptName).setHeader("Concepto");
        Grid.Column<BudgetEntry> quantityColumn = entriesGrid.addColumn(BudgetEntry::getQuantity).setHeader("Cantidad");
        Grid.Column<BudgetEntry> amountColumn = entriesGrid.addColumn(BudgetEntry::getAmmount).setHeader("Monto");

        ComboBox<BudgetConcept> conceptEditor = new ComboBox<>();
        conceptEditor.setItems(budgetConceptService.findAll());
        conceptEditor.setItemLabelGenerator(BudgetConcept::getName);
        entryBinder.forField(conceptEditor).bind(BudgetEntry::getConcept, BudgetEntry::setConcept);
        conceptColumn.setEditorComponent(conceptEditor);

        NumberField quantityEditor = new NumberField();
        entryBinder.forField(quantityEditor).bind(entry -> (double) entry.getQuantity(), (entry, value) -> entry.setQuantity(value.intValue()));
        quantityColumn.setEditorComponent(quantityEditor);

        NumberField amountEditor = new NumberField();
        entryBinder.forField(amountEditor).bind(BudgetEntry::getAmmount, BudgetEntry::setAmmount);
        amountColumn.setEditorComponent(amountEditor);

        entriesGrid.getEditor().setBinder(entryBinder);
        entriesGrid.getEditor().setBuffered(true);

        entriesGrid.addItemDoubleClickListener(e -> {
            entriesGrid.getEditor().editItem(e.getItem());
            conceptEditor.focus();
        });

        addEntryButton = new Button("Agregar Renglón", e -> {
            if (this.budget != null) {
                this.budget.getEntries().add(new BudgetEntry());
                entriesGrid.setItems(this.budget.getEntries());
            }
        });

        editorDiv.add(entriesGrid, addEntryButton);
        createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
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

        HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setWidthFull();
        topLayout.setAlignItems(Alignment.BASELINE);
        H2 title = new H2("Presupuestos");
        addButton = new Button("Crear");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        topLayout.add(title);
        topLayout.setFlexGrow(1, title);
        topLayout.add(addButton);

        wrapper.add(topLayout, grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Budget value) {
        this.budget = value;
        binder.readBean(this.budget);
        if (value != null) {
            entriesGrid.setItems(value.getEntries());
            created.setValue(value.getCreated().toString());
            editorLayoutDiv.setVisible(true);
            delete.setEnabled(true);
        } else {
            entriesGrid.setItems(new ArrayList<>());
            created.clear();
            editorLayoutDiv.setVisible(false);
            delete.setEnabled(false);
        }
    }
}