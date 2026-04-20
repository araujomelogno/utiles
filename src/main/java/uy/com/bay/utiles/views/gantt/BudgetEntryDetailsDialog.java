package uy.com.bay.utiles.views.gantt;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;

import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.dto.BudgetEntryDetailItem;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.Extra;
import uy.com.bay.utiles.entities.OdooCost;

public class BudgetEntryDetailsDialog extends Dialog {

	public BudgetEntryDetailsDialog(BudgetEntry budgetEntry) {
		setHeaderTitle("Detalle de la Entrada del Presupuesto: " + budgetEntry.getConcept().getName());
		setWidth("1000px");

		VerticalLayout layout = new VerticalLayout();
		add(layout);

		Grid<BudgetEntryDetailItem> grid = new Grid<>();

		List<BudgetEntryDetailItem> items = new ArrayList<>();

		// Process Extras
		if (budgetEntry.getExtras() != null) {
			for (Extra extra : budgetEntry.getExtras()) {
				items.add(new BudgetEntryDetailItem("Extras",
						budgetEntry.getConcept() != null ? budgetEntry.getConcept().getName().toString() : "",
						extra.getQuantity(), extra.getUnitPrice(), extra.getSurveyor().getName(),
						extra.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
			}
		}

		// Process Fieldworks
		if (budgetEntry.getFieldworks() != null) {
			for (Fieldwork fieldwork : budgetEntry.getFieldworks()) {
				items.add(new BudgetEntryDetailItem("Campo",
						fieldwork.getType() != null ? fieldwork.getType().toString() : "", fieldwork.getCompleted(),
						budgetEntry.getAmmount(), "N/A",
						fieldwork.getEndPlannedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
			}
		}

		// Process OdooCosts
		if (budgetEntry.getOdooCosts() != null) {
			for (OdooCost cost : budgetEntry.getOdooCosts()) {
				items.add(new BudgetEntryDetailItem("G.Odoo", cost.getName() != null ? cost.getName() : "", 1,
						cost.getBalance(), "N/A",
						(cost.getDate() != null ? cost.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
								: "")));
			}
		}

		// Process ExpenseRequests
		if (budgetEntry.getExpenseRequests() != null) {
			for (ExpenseRequest expenseRequest : budgetEntry.getExpenseRequests()) {
				if (expenseRequest.getExpenseStatus().equals(ExpenseStatus.TRANSFERIDO)
						|| expenseRequest.getExpenseStatus().equals(ExpenseStatus.RENDIDO))
					items.add(new BudgetEntryDetailItem("Gastos",
							expenseRequest.getConcept() != null ? expenseRequest.getConcept().getName() : "", 1,
							expenseRequest.getAmount(), expenseRequest.getSurveyor().getName(),
							expenseRequest.getTransferDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
			}
		}

		GridListDataView<BudgetEntryDetailItem> dataView = grid.setItems(items);

		grid.addColumn(BudgetEntryDetailItem::getTipo).setHeader("Tipo").setResizable(true);
		grid.addColumn(BudgetEntryDetailItem::getDate).setHeader("Fecha").setResizable(true);
		grid.addColumn(BudgetEntryDetailItem::getSurveyor).setHeader("Encuestador").setResizable(true);
		grid.addColumn(BudgetEntryDetailItem::getDetalle).setHeader("Detalle").setResizable(true);
		grid.addColumn(
				new NumberRenderer<>(BudgetEntryDetailItem::getCantidad, NumberFormat.getIntegerInstance(Locale.US)))
				.setHeader("Cantidad").setResizable(true);
		Grid.Column<BudgetEntryDetailItem> ucolumn = grid
				.addColumn(new NumberRenderer<>(BudgetEntryDetailItem::getCostoUnitario,
						NumberFormat.getCurrencyInstance(Locale.US)))
				.setHeader("Costo U.").setResizable(true);

		Grid.Column<BudgetEntryDetailItem> totalColumn = grid.addColumn(
				new NumberRenderer<>(BudgetEntryDetailItem::getTotal, NumberFormat.getCurrencyInstance(Locale.US)))
				.setHeader("Total").setResizable(true);

		// Footer for Total sum
		var footerRow = grid.appendFooterRow();
		BigDecimal total = dataView.getItems().map(BudgetEntryDetailItem::getTotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		footerRow.getCell(totalColumn).setText(String.format("$%,.2f", total));
		footerRow.getCell(ucolumn).setText("Total");

		grid.setAllRowsVisible(true);
		layout.add(grid);

		Button closeButton = new Button("Cerrar", e -> close());
		getFooter().add(closeButton);
	}
}