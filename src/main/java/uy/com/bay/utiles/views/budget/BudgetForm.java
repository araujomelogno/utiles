package uy.com.bay.utiles.views.budget;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.entities.BudgetConcept;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetConceptService;
import uy.com.bay.utiles.services.BudgetService;
import uy.com.bay.utiles.services.StudyService;

public class BudgetForm extends VerticalLayout {

	private final TextField name = new TextField("Nombre");
	private final ComboBox<Study> study = new ComboBox<>("Estudio");
	private final Grid<BudgetEntry> entriesGrid = new Grid<>(BudgetEntry.class);
	private final Button addEntryButton = new Button("Agregar concepto");
	private final Button save = new Button("Guardar");
	private final Button delete = new Button("Borrar");
	private final Button close = new Button("Cerrar");
	private final Binder<Budget> binder = new BeanValidationBinder<>(Budget.class);
	private final BudgetConceptService budgetConceptService;
	private final BudgetService budgetService;
	private final Editor<BudgetEntry> editor;
	private Span totalAmountLabel;
	private Span totalSpentLabel;

	public BudgetForm(StudyService studyService, BudgetConceptService budgetConceptService,
			BudgetService budgetService) {
		this.budgetConceptService = budgetConceptService;
		this.budgetService = budgetService;
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
				binder.getBean().setEntries(new HashSet
						<>());
			}
			binder.getBean().getEntries().add(newEntry);
			entriesGrid.setItems(binder.getBean().getEntries());
			editor.editItem(newEntry);
			updateTotal();
		});
	}

	private Component createFormLayout() {
		FormLayout formLayout = new FormLayout();
		formLayout.add(name, study);

		return formLayout;
	}

	private void configureGrid() {
		entriesGrid.setColumns();
		Binder<BudgetEntry> entryBinder = new Binder<>(BudgetEntry.class);
		editor.setBinder(entryBinder);
		editor.setBuffered(true);
		editor.addSaveListener(e -> {
			entriesGrid.setItems(binder.getBean().getEntries());
			updateTotal();
		});

		NumberField ammountField = new NumberField();
		ammountField.setWidthFull();
		entryBinder.forField(ammountField).bind(BudgetEntry::getAmmount, BudgetEntry::setAmmount);
		ComboBox<BudgetConcept> conceptComboBox = new ComboBox<>();
		conceptComboBox.setItems(budgetConceptService.list(Pageable.unpaged()).getContent());
		conceptComboBox.setItemLabelGenerator(BudgetConcept::getName);
		entryBinder.forField(conceptComboBox).bind(BudgetEntry::getConcept, BudgetEntry::setConcept);
		entriesGrid.addColumn(entry -> entry.getConcept() != null ? entry.getConcept().getName() : "")
				.setHeader("Concepto").setEditorComponent(conceptComboBox);
		entriesGrid.addColumn(BudgetEntry::getAmmount).setHeader("Costo unitario").setEditorComponent(ammountField);
		IntegerField quantityField = new IntegerField();
		quantityField.setWidthFull();
		entryBinder.forField(quantityField).bind(BudgetEntry::getQuantity, BudgetEntry::setQuantity);
		Grid.Column<BudgetEntry> quantityColumn = entriesGrid.addColumn(BudgetEntry::getQuantity).setHeader("Cantidad")
				.setEditorComponent(quantityField);
		Grid.Column<BudgetEntry> totalColumn = entriesGrid.addColumn(BudgetEntry::getTotal).setHeader("Total");

		Button saveButton = new Button("Guardar", e -> editor.save());
		HorizontalLayout actions = new HorizontalLayout(saveButton);
		actions.setPadding(false);

		Grid.Column<BudgetEntry> spentColumn = entriesGrid.addColumn(BudgetEntry::getSpent).setHeader("Gastado");
		Grid.Column<BudgetEntry> editorColumn = entriesGrid.addComponentColumn(entry -> {
			Button editButton = new Button("Editar");
			editButton.addClickListener(e -> {
				if (editor.isOpen()) {
					editor.cancel();
				}
				entriesGrid.getEditor().editItem(entry);
			});
			return editButton;
		});
		editorColumn.setEditorComponent(actions);
		entriesGrid.addComponentColumn(entry -> {
			Button removeButton = new Button("Borrar");
			removeButton.addClickListener(e -> {
				if (binder.getBean().getEntries() != null) {
					binder.getBean().getEntries().remove(entry);
					entriesGrid.setItems(binder.getBean().getEntries());
					updateTotal();
				}
			});
			return removeButton;
		}).setWidth("150px").setFlexGrow(0);
		FooterRow footerRow = entriesGrid.appendFooterRow();
		footerRow.getCell(quantityColumn).setText("Total");
		totalAmountLabel = new Span();
		totalSpentLabel = new Span();
		footerRow.getCell(totalColumn).setComponent(totalAmountLabel);
		footerRow.getCell(spentColumn).setComponent(totalSpentLabel);
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
			Budget budget = binder.getBean();
			if (budget.getStudy() != null) {
				Optional<Budget> existingBudgetOpt = budgetService.findByStudy(budget.getStudy());
				if (existingBudgetOpt.isPresent()) {
					Budget existingBudget = existingBudgetOpt.get();
					boolean isNewBudget = budget.getId() == null;
					if (isNewBudget || !existingBudget.getId().equals(budget.getId())) {
						Notification.show("El estudio ingresado tiene otro presupuesto: " + existingBudget.getName(),
								2500, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);

						return;
					}
				}
			} else {
				if (binder.getBean().getCreated() == null)
					binder.getBean().setCreated(LocalDate.now());
				fireEvent(new SaveEvent(this, binder.getBean()));
			}

		}
	}

	public void setBudget(Budget budget) {
		binder.setBean(budget);

		if (budget != null && budget.getEntries() != null) {
			study.setValue(budget.getStudy());
			entriesGrid.setItems(budget.getEntries());
		} else if (budget != null) {
			budget.setEntries(new HashSet<>());
			entriesGrid.setItems(budget.getEntries());
		}

		updateTotal();
	}

	public void clearStudy() {
		study.clear();

	}

	private void updateTotal() {
		double sum = 0.0;
		double spentSum = 0.0;
		if (binder.getBean() != null && binder.getBean().getEntries() != null) {
			sum = binder.getBean().getEntries().stream().mapToDouble(BudgetEntry::getTotal).sum();
			spentSum = binder.getBean().getEntries().stream().mapToDouble(BudgetEntry::getSpent).sum();
		}
		if (totalAmountLabel != null) {
			NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "UY"));
			totalAmountLabel.setText(currencyFormat.format(sum));
			totalSpentLabel.setText(currencyFormat.format(spentSum));
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