package uy.com.bay.utiles.views.budget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import org.springframework.data.domain.Pageable;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetConceptService;
import uy.com.bay.utiles.services.StudyService;

import java.util.ArrayList;

public class BudgetForm extends VerticalLayout {

    private final DatePicker created = new DatePicker("Fecha de Creación");
    private final ComboBox<Study> study = new ComboBox<>("Estudio");
    private final Grid<BudgetEntry> entriesGrid = new Grid<>(BudgetEntry.class);
    private final Button addEntryButton = new Button("Agregar concepto");

    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");
    private final Button close = new Button("Cerrar");

    private final Binder<Budget> binder = new BeanValidationBinder<>(Budget.class);
    private final BudgetConceptService budgetConceptService;
    private final Editor<BudgetEntry> editor;

    public BudgetForm(StudyService studyService, BudgetConceptService budgetConceptService) {
        this.budgetConceptService = budgetConceptService;

        addClassName("budget-form");
        binder.bindInstanceFields(this);

        study.setItems(studyService.listAll());
        study.setItemLabelGenerator(Study::getName);

        editor = entriesGrid.getEditor();
        configureGrid();

        add(createFormLayout(), entriesGrid, addEntryButton, createButtonsLayout());

        addEntryButton.addClickListener(click -> {
            BudgetEntry newEntry = new BudgetEntry();
            if (binder.getBean().getEntries() == null) {
                binder.getBean().setEntries(new ArrayList<>());
            }
            binder.getBean().getEntries().add(newEntry);
            entriesGrid.setItems(binder.getBean().getEntries());
            editor.editItem(newEntry);
        });
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(created, study);
        return formLayout;
    }

    private void configureGrid() {
        entriesGrid.setColumns(); // Clear existing columns

        Binder<BudgetEntry> entryBinder = new Binder<>(BudgetEntry.class);
        editor.setBinder(entryBinder);
        editor.setBuffered(true);

        // Amount Column
        NumberField ammountField = new NumberField();
        ammountField.setWidthFull();
        entryBinder.forField(ammountField).bind(BudgetEntry::getAmmount, BudgetEntry::setAmmount);
        entriesGrid.addColumn(BudgetEntry::getAmmount)
                .setHeader("Monto").setEditorComponent(ammountField);

        // Quantity Column
        IntegerField quantityField = new IntegerField();
        quantityField.setWidthFull();
        entryBinder.forField(quantityField).bind(BudgetEntry::getQuantity, BudgetEntry::setQuantity);
        entriesGrid.addColumn(BudgetEntry::getQuantity)
                .setHeader("Cantidad").setEditorComponent(quantityField);

        // Concept Column
        ComboBox<BudgetConcept> conceptComboBox = new ComboBox<>();
        conceptComboBox.setItems(budgetConceptService.list(Pageable.unpaged()).getContent());
        conceptComboBox.setItemLabelGenerator(BudgetConcept::getName);
        entryBinder.forField(conceptComboBox).bind(BudgetEntry::getConcept, BudgetEntry::setConcept);
        entriesGrid.addColumn(entry -> entry.getConcept() != null ? entry.getConcept().getName() : "")
                .setHeader("Concepto").setEditorComponent(conceptComboBox);

        // Edit Column
        Button saveButton = new Button("Guardar", e -> editor.save());
        Button cancelButton = new Button("Cancelar", e -> editor.cancel());
        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        actions.setPadding(false);

        Grid.Column<BudgetEntry> editorColumn = entriesGrid.addComponentColumn(entry -> {
            Button editButton = new Button("Editar");
            editButton.addClickListener(e -> {
                if (editor.isOpen()) {
                    editor.cancel();
                }
                entriesGrid.getEditor().editItem(entry);
            });
            return editButton;
        }).setWidth("150px").setFlexGrow(0);

        editorColumn.setEditorComponent(actions);

        // Remove Column
        entriesGrid.addComponentColumn(entry -> {
            Button removeButton = new Button("Borrar");
            removeButton.addClickListener(e -> {
                if (binder.getBean().getEntries() != null) {
                    binder.getBean().getEntries().remove(entry);
                    entriesGrid.setItems(binder.getBean().getEntries());
                }
            });
            return removeButton;
        }).setWidth("150px").setFlexGrow(0);
    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }

    public void setBudget(Budget budget) {
        binder.setBean(budget);
        if (budget != null && budget.getEntries() != null) {
            entriesGrid.setItems(budget.getEntries());
        } else if (budget != null) {
            budget.setEntries(new ArrayList<>());
            entriesGrid.setItems(budget.getEntries());
        }
    }

    // Events
    public static abstract class BudgetFormEvent extends com.vaadin.flow.component.ComponentEvent<BudgetForm> {
        private final Budget budget;

        protected BudgetFormEvent(BudgetForm source, Budget budget) {
            super(source, false);
            this.budget = budget;
        }

        public Budget getBudget() {
            return budget;
        }
    }

    public static class SaveEvent extends BudgetFormEvent {
        SaveEvent(BudgetForm source, Budget budget) {
            super(source, budget);
        }
    }

    public static class DeleteEvent extends BudgetFormEvent {
        DeleteEvent(BudgetForm source, Budget budget) {
            super(source, budget);
        }
    }

    public static class CloseEvent extends BudgetFormEvent {
        CloseEvent(BudgetForm source) {
            super(source, null);
        }
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }
}