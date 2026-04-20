package uy.com.bay.utiles.views.gantt;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetExporter;

public class StudyDetailsDialog extends Dialog {
	private final NumberFormat currencyFormat;

	public StudyDetailsDialog(Study study) {
		currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "UY"));
		currencyFormat.setMinimumFractionDigits(0);
		currencyFormat.setMaximumFractionDigits(0);
		setHeaderTitle("Detalles del Estudio: " + study.getName());
		setWidth("1200px");

		VerticalLayout layout = new VerticalLayout();
		add(layout);

		FormLayout formLayout = new FormLayout();
		TextField name = new TextField("Name");
		name.setValue(study.getName());
		name.setReadOnly(true);

		TextField casosCompletos = new TextField("Casos completos");
		Integer completed = 0;
		for (Fieldwork fw : study.getFieldworks()) {
			completed = completed + fw.getCompleted();

		}
		casosCompletos.setValue(completed.toString());
		casosCompletos.setReadOnly(true);

		TextField totalTransfered = new TextField("Total de gastos transferidos");
		totalTransfered.setValue(currencyFormat.format(study.getTotalTransfered()));
		totalTransfered.setReadOnly(true);

		TextField totalReportedCost = new TextField("Total de gastos rendidos");
		totalReportedCost.setValue(currencyFormat.format(study.getTotalReportedCost()));
		totalReportedCost.setReadOnly(true);

		formLayout.add(name, casosCompletos, totalTransfered, totalReportedCost);
		layout.add(formLayout);

		if (study.getBudget() != null && study.getBudget().getEntries() != null
				&& !study.getBudget().getEntries().isEmpty()) {
			Grid<BudgetEntry> budgetGrid = new Grid<>();
			GridListDataView<BudgetEntry> dataView = budgetGrid.setItems(study.getBudget().getEntries());

			budgetGrid.addColumn(entry -> entry.getConcept() != null ? entry.getConcept().getName() : "sin concepto")
					.setHeader("Concepto");
			budgetGrid.addColumn(BudgetEntry::getQuantity).setHeader("Cantidad");
			Grid.Column<BudgetEntry> costouColumn = budgetGrid
					.addColumn((entry -> currencyFormat.format(entry.getAmmount()))).setHeader("Costo U.");
			Grid.Column<BudgetEntry> totalColumn = budgetGrid
					.addColumn((entry -> currencyFormat.format(entry.getTotal()))).setHeader("Total");
			Grid.Column<BudgetEntry> spentColumn = budgetGrid
					.addColumn((entry -> currencyFormat.format(entry.getSpent()))).setHeader("Gastado");

			budgetGrid.addColumn(new ComponentRenderer<>(Button::new, (button, entry) -> {
				button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
				button.setIcon(VaadinIcon.SEARCH.create());
				button.addClickListener(e -> new BudgetEntryDetailsDialog(entry).open());
			})).setHeader("Ver detalle");

			var footerRow = budgetGrid.appendFooterRow();
			double totalSum = dataView.getItems().mapToDouble(BudgetEntry::getTotal).sum();
			double totalSpent = dataView.getItems().mapToDouble(BudgetEntry::getSpent).sum();
			footerRow.getCell(costouColumn).setText("Total:");
			footerRow.getCell(totalColumn).setText(currencyFormat.format(totalSum));
			footerRow.getCell(spentColumn).setText(currencyFormat.format(totalSpent));

			budgetGrid.setAllRowsVisible(true);
			HorizontalLayout budgetHeader = new HorizontalLayout();
			budgetHeader.setAlignItems(Alignment.BASELINE);
			Label budgetLabel = new Label("Presupuesto");
			Anchor downloadLink = new Anchor();
			downloadLink.getStyle().set("display", "none");
			Button exportButton = new Button("Exportar");
			exportButton.addClickListener(event -> {
				try {
					BudgetExporter budgetExporter = new BudgetExporter();
					InputStream excelStream = budgetExporter.export(study);
					SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
					StreamResource resource = new StreamResource(
							"Presupuesto_" + study.getName() + "_" + sdf.format(new Date()) + ".xlsx",
							() -> excelStream);
					downloadLink.setHref(resource);
					downloadLink.getElement().callJsFunction("click");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			budgetHeader.add(budgetLabel, exportButton, downloadLink);
			layout.add(budgetHeader);
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