package uy.com.bay.utiles.views.expenses;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.server.StreamResource;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportFile;
import uy.com.bay.utiles.services.ExpenseReportFileService;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ExpenseReportViewDialog extends Dialog {

    private final ExpenseReport expenseReport;
    private final ExpenseReportFileService expenseReportFileService;
    private Grid<ExpenseReportFile> filesGrid;
    private List<ExpenseReportFile> files;

    public ExpenseReportViewDialog(ExpenseReport expenseReport, ExpenseReportFileService expenseReportFileService) {
        this.expenseReport = expenseReport;
        this.expenseReportFileService = expenseReportFileService;
        this.files = new ArrayList<>(expenseReport.getFiles());

        setHeaderTitle("Detalles de la Rendición");

        FormLayout formLayout = new FormLayout();

        TextField study = new TextField("Estudio");
        study.setValue(expenseReport.getStudy() != null ? expenseReport.getStudy().getName() : "");
        study.setReadOnly(true);

        TextField surveyor = new TextField("Encuestador");
        surveyor.setValue(expenseReport.getSurveyor() != null ? expenseReport.getSurveyor().getFirstName() + " " + expenseReport.getSurveyor().getLastName() : "");
        surveyor.setReadOnly(true);

        DatePicker date = new DatePicker("Fecha");
        if (expenseReport.getDate() != null) {
            date.setValue(new java.sql.Date(expenseReport.getDate().getTime()).toLocalDate());
        }
        date.setReadOnly(true);

        NumberField amount = new NumberField("Monto");
        amount.setValue(expenseReport.getAmount());
        amount.setReadOnly(true);

        TextField concept = new TextField("Concepto");
        concept.setValue(expenseReport.getConcept() != null ? expenseReport.getConcept().getName() : "");
        concept.setReadOnly(true);

        formLayout.add(study, surveyor, date, amount, concept);
        add(formLayout);

        if (files != null && !files.isEmpty()) {
            VerticalLayout filesLayout = new VerticalLayout();
            filesLayout.setSpacing(false);
            filesLayout.setPadding(false);

            com.vaadin.flow.component.html.H4 filesHeader = new com.vaadin.flow.component.html.H4("Archivos adjuntos");
            filesLayout.add(filesHeader);

            filesGrid = new Grid<>(ExpenseReportFile.class, false);
            filesGrid.setItems(files);
            filesGrid.addColumn(ExpenseReportFile::getName).setHeader("Nombre");

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
