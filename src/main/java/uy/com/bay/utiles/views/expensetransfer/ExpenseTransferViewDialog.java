package uy.com.bay.utiles.views.expensetransfer;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

        formLayout.add(transferDate, amount);
        add(formLayout);

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

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }
}
