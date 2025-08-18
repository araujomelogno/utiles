package uy.com.bay.utiles.views.expensetransfer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.NumberField;

import java.util.List;

public class ExpenseTransferViewDialog extends Dialog {

	public ExpenseTransferViewDialog(ExpenseTransfer expenseTransfer) {
		
		setHeaderTitle("Detalles de la Transferencia");

		FormLayout formLayout = new FormLayout();
		DatePicker transferDate = new DatePicker("Fecha de Transferencia");
		transferDate.setValue(new java.sql.Date(expenseTransfer.getTransferDate().getTime()).toLocalDate());
		transferDate.setReadOnly(true);

		NumberField amount = new NumberField("Monto");
		amount.setValue(expenseTransfer.getAmount());
		amount.setReadOnly(true);

		TextField surveyor = new TextField("Encuestador");
		surveyor.setValue(expenseTransfer.getSurveyor().getName());
		surveyor.setReadOnly(true);

		formLayout.add(transferDate, amount, surveyor);
		add(formLayout);

		List<ExpenseRequest> expenseRequests = expenseTransfer.getExpenseRequests();
		if (expenseRequests != null && !expenseRequests.isEmpty()) {
			VerticalLayout expenseRequestsLayout = new VerticalLayout();
			expenseRequestsLayout.setSpacing(false);
			expenseRequestsLayout.setPadding(false);

			com.vaadin.flow.component.html.H4 expenseRequestsHeader = new com.vaadin.flow.component.html.H4(
					"Solicitudes de Gasto");
			expenseRequestsLayout.add(expenseRequestsHeader);

			Grid<ExpenseRequest> grid = new Grid<>(ExpenseRequest.class, false);
			grid.setItems(expenseRequests);
			grid.addColumn(er -> er.getStudy().getName()).setHeader("Estudio");
			grid.addColumn(er -> er.getConcept().getName()).setHeader("Concepto");
			grid.addColumn(er -> new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getRequestDate()))
					.setHeader("Fecha Solicitud");
			grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto");
			expenseRequestsLayout.add(grid);
			add(expenseRequestsLayout);
		}

		List<ExpenseTransferFile> files = expenseTransfer.getFiles();
		if (files != null && !files.isEmpty()) {
			VerticalLayout filesLayout = new VerticalLayout();
			filesLayout.setSpacing(false);
			filesLayout.setPadding(false);

			com.vaadin.flow.component.html.H4 filesHeader = new com.vaadin.flow.component.html.H4("Archivos adjuntos");
			filesLayout.add(filesHeader);

			for (ExpenseTransferFile file : files) {
				String downloadUrl = "/api/files/" + file.getId();
				Anchor downloadLink = new Anchor(downloadUrl, file.getName());
				downloadLink.setTarget("_blank");
				filesLayout.add(downloadLink);
			}
			add(filesLayout);
		}

		Button closeButton = new Button("Cerrar", e -> close());
		getFooter().add(closeButton);

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
	}
}
