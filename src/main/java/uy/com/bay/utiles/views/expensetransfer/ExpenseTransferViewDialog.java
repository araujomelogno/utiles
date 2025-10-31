package uy.com.bay.utiles.views.expensetransfer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.ExpenseTransfer;
import uy.com.bay.utiles.data.ExpenseTransferFile;
import uy.com.bay.utiles.services.ExpenseTransferFileService;

public class ExpenseTransferViewDialog extends Dialog {

	private Grid<ExpenseTransferFile> filesGrid;
	private List<ExpenseTransferFile> files;

	public ExpenseTransferViewDialog(ExpenseTransfer expenseTransfer) {
		this.files = new ArrayList<>(expenseTransfer.getFiles());

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

		if (expenseTransfer.getObs() != null && !expenseTransfer.getObs().isEmpty()) {
			TextArea obs = new TextArea("Observaciones");
			obs.setValue(expenseTransfer.getObs());
			obs.setReadOnly(true);
			obs.setWidthFull();
			add(obs);
		}

		Set<ExpenseRequest> expenseRequests = expenseTransfer.getExpenseRequests();
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
			grid.addColumn(er -> er.getConcept() != null ? er.getConcept().getName() : "").setHeader("Concepto");
			grid.addColumn(er -> new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getRequestDate()))
					.setHeader("Fecha Solicitud");
			grid.setAllRowsVisible(true);

			grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto");
			expenseRequestsLayout.add(grid);
			add(expenseRequestsLayout);
		}

		if (files != null && !files.isEmpty()) {
			VerticalLayout filesLayout = new VerticalLayout();
			filesLayout.setSpacing(false);
			filesLayout.setPadding(false);

			com.vaadin.flow.component.html.H4 filesHeader = new com.vaadin.flow.component.html.H4("Archivos adjuntos");
			filesLayout.add(filesHeader);

			filesGrid = new Grid<>(ExpenseTransferFile.class, false);
			filesGrid.setItems(files);
			filesGrid.addColumn(ExpenseTransferFile::getName).setHeader("Nombre");

			filesGrid.addComponentColumn(file -> {
				Button downloadButton = new Button("Descargar");
				StreamResource resource = new StreamResource(file.getName(),
						() -> new ByteArrayInputStream(file.getContent()));
				Anchor downloadLink = new Anchor(resource, "");
				downloadLink.getElement().setAttribute("download", true);
				downloadLink.add(downloadButton);
				return downloadLink;
			}).setHeader("Acciones");

			filesGrid.setAllRowsVisible(true);
			filesLayout.add(filesGrid);
			add(filesLayout);
		}

		Button closeButton = new Button("Cerrar", e -> close());
		getFooter().add(closeButton);

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
	}

}
