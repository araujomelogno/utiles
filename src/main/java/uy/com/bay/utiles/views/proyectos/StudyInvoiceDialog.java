package uy.com.bay.utiles.views.proyectos;

import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.StudyInvoice;
import uy.com.bay.utiles.services.StudyInvoiceService;

public class StudyInvoiceDialog extends Dialog {

	public StudyInvoiceDialog(Study study, StudyInvoiceService studyInvoiceService) {
		setHeaderTitle("Facturas de: " + study.getName());
		setWidth("80%");

		Grid<StudyInvoice> grid = new Grid<>(StudyInvoice.class, false);
		grid.addColumn("moveId").setHeader("Move ID").setAutoWidth(true);
		grid.addColumn("invoiceDate").setHeader("Fecha").setAutoWidth(true);
		grid.addColumn(new NumberRenderer<>(StudyInvoice::getAmountUntaxed, NumberFormat.getNumberInstance(Locale.US)))
				.setHeader("Subtotal").setAutoWidth(true);
		grid.addColumn(new NumberRenderer<>(StudyInvoice::getTax, NumberFormat.getNumberInstance(Locale.US)))
				.setHeader("Impuestos").setAutoWidth(true);
		grid.addColumn(new NumberRenderer<>(StudyInvoice::getAmountTotal, NumberFormat.getNumberInstance(Locale.US)))
				.setHeader("Total").setAutoWidth(true);
		grid.addColumn("currency").setHeader("Moneda").setAutoWidth(true);

		grid.setItems(studyInvoiceService.findByStudy(study));

		Button closeButton = new Button("Cerrar", e -> close());

		HorizontalLayout buttonLayout = new HorizontalLayout(closeButton);
		VerticalLayout layout = new VerticalLayout(grid, buttonLayout);
		add(layout);

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
	}
}
