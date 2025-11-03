package uy.com.bay.utiles.views.gantt;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;

import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;

public class StudyDetailsDialog extends Dialog {

	public StudyDetailsDialog(Study study) {
		setHeaderTitle("Detalles del Estudio: " + study.getName());
		setWidth("1200px");

		VerticalLayout layout = new VerticalLayout();
		add(layout);

		FormLayout formLayout = new FormLayout();
		TextField name = new TextField("Name");
		name.setValue(study.getName());
		name.setReadOnly(true);

		TextField casosCompletos = new TextField("Casos completos");
		casosCompletos.setReadOnly(true);

		TextField totalTransfered = new TextField("Total de gastos transferidos");
		totalTransfered.setValue(Double.valueOf(study.getTotalTransfered()).toString());
		totalTransfered.setReadOnly(true);

		TextField totalReportedCost = new TextField("Total de gastos rendidos");
		totalReportedCost.setValue(Double.valueOf(study.getTotalReportedCost()).toString());
		totalReportedCost.setReadOnly(true);

		formLayout.add(name, casosCompletos, totalTransfered, totalReportedCost);
		layout.add(formLayout);

		if (study.getBudget() != null && study.getBudget().getEntries() != null
				&& !study.getBudget().getEntries().isEmpty()) {
			Grid<BudgetEntry> budgetGrid = new Grid<>();
			GridListDataView<BudgetEntry> dataView = budgetGrid.setItems(study.getBudget().getEntries());

			budgetGrid.addColumn(entry -> entry.getConcept().getName()).setHeader("Concepto");
			budgetGrid.addColumn(BudgetEntry::getQuantity).setHeader("Cantidad");
			Grid.Column<BudgetEntry> costouColumn = budgetGrid
					.addColumn(
							new NumberRenderer<>(BudgetEntry::getAmmount, NumberFormat.getCurrencyInstance(Locale.US)))
					.setHeader("Costo U.");
			Grid.Column<BudgetEntry> totalColumn = budgetGrid
					.addColumn(new NumberRenderer<>(BudgetEntry::getTotal, NumberFormat.getCurrencyInstance(Locale.US)))
					.setHeader("Total");
			Grid.Column<BudgetEntry> spentColumn = budgetGrid
					.addColumn(new NumberRenderer<>(BudgetEntry::getSpent, NumberFormat.getCurrencyInstance(Locale.US)))
					.setHeader("Gastado");

			budgetGrid.addColumn(new ComponentRenderer<>(Button::new, (button, entry) -> {
				button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
				button.setIcon(VaadinIcon.SEARCH.create());
				button.addClickListener(e -> new BudgetEntryDetailsDialog(entry).open());
			})).setHeader("Ver detalle");

			var footerRow = budgetGrid.appendFooterRow();
			double totalSum = dataView.getItems().mapToDouble(BudgetEntry::getTotal).sum();
			double totalSpent = dataView.getItems().mapToDouble(BudgetEntry::getSpent).sum();
			footerRow.getCell(costouColumn).setText("Total:");
			footerRow.getCell(totalColumn).setText(String.format("$%,.2f", totalSum));
			footerRow.getCell(spentColumn).setText(String.format("$%,.2f", totalSpent));

			budgetGrid.setAllRowsVisible(true);
			layout.add(new Label("Presupuesto"));
			layout.add(budgetGrid);
		}

		Grid<Fieldwork> fieldworkGrid = new Grid<>(Fieldwork.class, false);

		fieldworkGrid.addColumn("status").setHeader("Estado");
		fieldworkGrid.addColumn("type").setHeader("Tipo");
		fieldworkGrid.addColumn("initPlannedDate").setHeader("Inicio planificado");
		fieldworkGrid.addColumn("endPlannedDate").setHeader("Fin planificado");
		fieldworkGrid.addColumn("goalQuantity").setHeader("Cantidad objetivo");
		fieldworkGrid.addColumn("completed").setHeader("Completas");
		if (study.getFieldworks() != null) {
			fieldworkGrid.setItems(study.getFieldworks());
		} else {
			fieldworkGrid.setItems(Collections.emptyList());
		}
		fieldworkGrid.setAllRowsVisible(true);

		layout.add(new Label("Campos"));
		layout.add(fieldworkGrid);

		Button editButton = new Button("Editar");
		editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		editButton.addClickListener(e -> {
			UI.getCurrent().navigate(study.getId() + "/edit");
			close();
		});

		Button closeButton = new Button("Cerrar", e -> close());
		getFooter().add(editButton, closeButton);
	}
}