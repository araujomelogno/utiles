package uy.com.bay.utiles.views.gantt;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.function.SerializableToDoubleFunction;

import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.dto.BudgetEntryDetailItem;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.Extra;

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
                items.add(new BudgetEntryDetailItem(
                        "Extras",
                        extra.getConcept() != null ? extra.getConcept().getDescription() : "",
                        extra.getQuantity(),
                        extra.getUnitPrice()
                ));
            }
        }

        // Process Fieldworks
        if (budgetEntry.getFieldworks() != null) {
            for (Fieldwork fieldwork : budgetEntry.getFieldworks()) {
                items.add(new BudgetEntryDetailItem(
                        "Campo",
                        fieldwork.getType() != null ? fieldwork.getType().toString() : "",
                        fieldwork.getCompleted(),
                        fieldwork.getUnitCost()
                ));
            }
        }

        // Process ExpenseRequests
        if (budgetEntry.getExpenseRequests() != null) {
            for (ExpenseRequest expenseRequest : budgetEntry.getExpenseRequests()) {
                items.add(new BudgetEntryDetailItem(
                        "Gastos",
                        expenseRequest.getConcept() != null ? expenseRequest.getConcept().getName() : "",
                        1,
                        expenseRequest.getAmount()
                ));
            }
        }

        GridListDataView<BudgetEntryDetailItem> dataView = grid.setItems(items);

        grid.addColumn(BudgetEntryDetailItem::getTipo).setHeader("Tipo");
        grid.addColumn(BudgetEntryDetailItem::getDetalle).setHeader("Detalle");
        grid.addColumn(new NumberRenderer<>(BudgetEntryDetailItem::getCantidad, NumberFormat.getIntegerInstance(Locale.US))).setHeader("Cantidad");
        grid.addColumn(new NumberRenderer<>(BudgetEntryDetailItem::getCostoUnitario, NumberFormat.getCurrencyInstance(Locale.US))).setHeader("Costo U.");

        Grid.Column<BudgetEntryDetailItem> totalColumn = grid.addColumn(new NumberRenderer<>(BudgetEntryDetailItem::getTotal, NumberFormat.getCurrencyInstance(Locale.US)))
                .setHeader("Total");

        // Footer for Total sum
        var footerRow = grid.appendFooterRow();
        SerializableToDoubleFunction<BudgetEntryDetailItem> totalSum = item -> item.getTotal().doubleValue();
        BigDecimal total = dataView.getItems().map(BudgetEntryDetailItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        footerRow.getCell(totalColumn).setText(String.format("$%.2f", total));

        grid.setAllRowsVisible(true);
        layout.add(grid);

        Button closeButton = new Button("Cerrar", e -> close());
        getFooter().add(closeButton);
    }
}