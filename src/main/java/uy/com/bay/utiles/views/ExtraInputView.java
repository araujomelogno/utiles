package uy.com.bay.utiles.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.vaadin.addons.componentfactory.monthpicker.MonthPicker;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.service.ExtraConceptService;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.Extra;
import uy.com.bay.utiles.services.BudgetEntryService;
import uy.com.bay.utiles.services.ExtraService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

@PageTitle("Ingresar Extras")
@Route(value = "IngresarExtras", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class ExtraInputView extends VerticalLayout {

	private final ExtraService extraService;
	private final StudyService studyService;
	private final ExtraConceptService extraConceptService;
	private final SurveyorService surveyorService;
	private final BudgetEntryService budgetEntryService;

	private ComboBox<Study> studyComboBox;
	private ComboBox<BudgetEntry> budgetEntryComboBox;
	private MonthPicker monthPicker;
	private Grid<Extra> grid;
	private List<Extra> extras;

	public ExtraInputView(ExtraService extraService, StudyService studyService, ExtraConceptService extraConceptService,
			SurveyorService surveyorService, BudgetEntryService budgetEntryService) {
		this.extraService = extraService;
		this.studyService = studyService;
		this.extraConceptService = extraConceptService;
		this.surveyorService = surveyorService;
		this.budgetEntryService = budgetEntryService;

		createFilters();
		createGrid();

		Button addButton = new Button("Agregar", event -> {
			if (studyComboBox.getValue() != null && monthPicker.getValue() != null) {

				Study selectedStudy = studyComboBox.getValue();
				LocalDate selectedMonth = monthPicker.getValue().atEndOfMonth();

				if (selectedStudy == null || selectedMonth == null) {
					Notification.show("Por favor, seleccione un estudio y un mes.");
					return;
				}

				Extra newExtra = new Extra();
				newExtra.setStudy(selectedStudy);
				newExtra.setDate(selectedMonth.withDayOfMonth(1));
				extras.add(newExtra);
				grid.getDataProvider().refreshAll();
				grid.getEditor().editItem(newExtra);

			}
		});

		HorizontalLayout layout = new HorizontalLayout(studyComboBox, monthPicker, addButton);
		layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, addButton);

		add(layout, grid);
	}

	private void createFilters() {
		studyComboBox = new ComboBox<>("Estudio");
		studyComboBox.setItems(studyService.findAll());
		studyComboBox.setItemLabelGenerator(Study::getName);
		studyComboBox.addValueChangeListener(event -> {
			loadExtras();
			updateBudgetEntryComboBox();
		});

		monthPicker = new MonthPicker();
		monthPicker.setLabel("Fecha:");
		monthPicker.addValueChangeListener(event -> loadExtras());
	}

	private void createGrid() {
		grid = new Grid<>(Extra.class, false);
		extras = new ArrayList<>();
		grid.setItems(extras);

		Binder<Extra> binder = new Binder<>(Extra.class);
		Editor<Extra> editor = grid.getEditor().setBinder(binder);

		// Columnas no editables
		Grid.Column<Extra> dateColumn = grid.addColumn(Extra::getDate).setHeader("Fecha").setKey("date");
		Grid.Column<Extra> amountColumn = grid.addColumn(Extra::getAmount).setHeader("Importe").setKey("amount");

		// Columna Concepto (editable)
		budgetEntryComboBox = new ComboBox<>();
		budgetEntryComboBox.setItemLabelGenerator(
				budgetEntry -> budgetEntry.getConcept() != null ? budgetEntry.getConcept().getName() : "");
		binder.forField(budgetEntryComboBox).bind(Extra::getBudgetEntry, Extra::setBudgetEntry);
		grid.addColumn(extra -> extra.getBudgetEntry() != null && extra.getBudgetEntry().getConcept() != null
				? extra.getBudgetEntry().getConcept().getName()
				: "").setHeader("Concepto-Presupuesto").setEditorComponent(budgetEntryComboBox);


		// Columna "Concepto-Presupuesto" (editable)
		budgetEntryComboBox = new ComboBox<>();
		budgetEntryComboBox.setItemLabelGenerator(
				budgetEntry -> budgetEntry.getConcept() != null ? budgetEntry.getConcept().getName() : "");
		binder.forField(budgetEntryComboBox).bind(Extra::getBudgetEntry, Extra::setBudgetEntry);
		grid.addColumn(extra -> extra.getBudgetEntry() != null && extra.getBudgetEntry().getConcept() != null
				? extra.getBudgetEntry().getConcept().getName()
				: "").setHeader("Concepto-Presupuesto").setEditorComponent(budgetEntryComboBox);

		// Columna Encuestador (editable)
		ComboBox<Surveyor> surveyorComboBox = new ComboBox<>();
		surveyorComboBox.setItems(surveyorService.findAll());
		surveyorComboBox.setItemLabelGenerator(Surveyor::getName);
		binder.forField(surveyorComboBox).bind(Extra::getSurveyor, Extra::setSurveyor);
		grid.addColumn(extra -> extra.getSurveyor() != null ? extra.getSurveyor().getName() : "")
				.setHeader("Encuestador").setEditorComponent(surveyorComboBox).setSortable(true)
				.setComparator(extra -> extra.getSurveyor() != null ? extra.getSurveyor().getName() : "");

		// Columna Cantidad (editable)
		IntegerField quantityField = new IntegerField();
		binder.forField(quantityField).bind(Extra::getQuantity, Extra::setQuantity);
		grid.addColumn(Extra::getQuantity).setHeader("Cantidad").setEditorComponent(quantityField);

		// Columna Precio Unitario (editable)
		NumberField unitPriceField = new NumberField();
		binder.forField(unitPriceField).bind(Extra::getUnitPrice, Extra::setUnitPrice);
		grid.addColumn(Extra::getUnitPrice).setHeader("Precio Unitario").setEditorComponent(unitPriceField);

		// Columna Observaciones (editable)
		TextField obsField = new TextField();
		binder.forField(obsField).bind(Extra::getObs, Extra::setObs);
		grid.addColumn(Extra::getObs).setHeader("Observaciones").setEditorComponent(obsField);

		// Columna de guardado
		Grid.Column<Extra> saveColumn = grid.addComponentColumn(extra -> {
			Button save = new Button(new Icon(VaadinIcon.DISC));
			save.addClickListener(e -> {
				try {
					binder.readBean(extra);
					if (binder.writeBeanIfValid(extra)) {
						if (extra.getBudgetEntry() == null) {
							Notification.show("Debe seleccionar un Concepto-Presupuesto.");
							return;
						}
						BudgetEntry budgetEntry = extra.getBudgetEntry();
						budgetEntry.setSpent(budgetEntry.getSpent() + extra.getAmount());
						budgetEntryService.save(budgetEntry);
						extraService.save(extra);
						grid.getDataProvider().refreshAll();
						updateFooter();
						Notification.show("Extra guardado.");
						editor.cancel();
					}
				} catch (Exception ex) {
					Notification.show("Error al guardar el extra: " + ex.getMessage());
				}
			});
			return save;
		}).setHeader("Guardar");
		// Columna de edición
		Grid.Column<Extra> editorColumn = grid.addComponentColumn(extra -> {
			Button editButton = new Button(new Icon(VaadinIcon.PENCIL));
			editButton.addClickListener(e -> {
				if (editor.isOpen()) {
					editor.cancel();
				}
				if (studyComboBox.getValue() != null && studyComboBox.getValue().getBudget() != null) {
					budgetEntryComboBox.setItems(studyComboBox.getValue().getBudget().getEntries());
				}
				grid.getEditor().editItem(extra);
			});
			return editButton;
		}).setHeader("Editar");

		// Columna de borrado
		grid.addComponentColumn(extra -> {
			Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
			deleteButton.addClickListener(e -> {
				if (extra.getId() != null) {
					extraService.delete(extra);
				}
				extras.remove(extra);
				grid.getDataProvider().refreshAll();
				updateFooter();
				Notification.show("Extra eliminado.");
			});
			return deleteButton;
		}).setHeader("Eliminar");

		updateFooter();
	}

	private void loadExtras() {
		if (studyComboBox.getValue() != null && monthPicker.getValue() != null) {
			Study selectedStudy = studyComboBox.getValue();
			LocalDate selectedMonth = monthPicker.getValue().atEndOfMonth();

			extras.clear();
			extras.addAll(extraService.findByStudyAndMonth(selectedStudy, selectedMonth));
			grid.getDataProvider().refreshAll();
			updateFooter();
		}
	}

	private void updateFooter() {
		if (grid.getFooterRows().isEmpty()) {
			grid.appendFooterRow();
		}
		grid.getFooterRows().get(0).getCell(grid.getColumnByKey("date")).setText("TOTAL:");
		grid.getFooterRows().get(0).getCell(grid.getColumnByKey("amount"))
				.setText("" + extras.stream().mapToDouble(Extra::getAmount).sum());
	}

	private void updateBudgetEntryComboBox() {
		Study selectedStudy = studyComboBox.getValue();
		if (selectedStudy != null && selectedStudy.getBudget() != null) {
			budgetEntryComboBox.setItems(selectedStudy.getBudget().getEntries());
		} else {
			budgetEntryComboBox.clear();
			budgetEntryComboBox.setItems(new ArrayList<>());
		}
	}
}