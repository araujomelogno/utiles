package uy.com.bay.utiles.views.gantt;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;

public class StudyDetailsDialog extends Dialog {

    public StudyDetailsDialog(Study study) {
        setHeaderTitle("Detalles del Estudio: " + study.getName());
        setWidth("800px");

        VerticalLayout layout = new VerticalLayout();
        add(layout);

        FormLayout formLayout = new FormLayout();
        TextField name = new TextField("Name");
        name.setValue(study.getName());
        name.setReadOnly(true);

        TextField odooId = new TextField("Odoo Id");
        odooId.setValue(study.getOdooId());
        odooId.setReadOnly(true);

        TextArea obs = new TextArea("Observaciones");
        obs.setValue(study.getObs());
        obs.setReadOnly(true);

        TextField casosCompletos = new TextField("Casos completos");
        casosCompletos.setReadOnly(true);

        Checkbox showSurveyor = new Checkbox("Mostrar a encuestador");
        showSurveyor.setValue(study.isShowSurveyor());
        showSurveyor.setReadOnly(true);

        TextField totalTransfered = new TextField("Total de gastos transferidos");
        totalTransfered.setValue(Double.valueOf(study.getTotalTransfered()).toString());
        totalTransfered.setReadOnly(true);

        TextField totalReportedCost = new TextField("Total de gastos rendidos");
        totalReportedCost.setValue(Double.valueOf(study.getTotalReportedCost()).toString());
        totalReportedCost.setReadOnly(true);

        formLayout.add(name, odooId, obs, casosCompletos, showSurveyor, totalTransfered, totalReportedCost);
        layout.add(formLayout);

        Grid<Fieldwork> fieldworkGrid = new Grid<>(Fieldwork.class, false);
        fieldworkGrid.addColumn("status").setHeader("Estado");
        fieldworkGrid.addColumn("type").setHeader("Tipo");
        fieldworkGrid.addColumn("initPlannedDate").setHeader("Fecha de inicio planificada");
        fieldworkGrid.addColumn("endPlannedDate").setHeader("Fecha de fin planificada");
        fieldworkGrid.addColumn("goalQuantity").setHeader("Cantidad objetivo");
        fieldworkGrid.addColumn("completed").setHeader("Completado");
        fieldworkGrid.setItems(study.getFieldworks());
        fieldworkGrid.setAllRowsVisible(true);

        layout.add(fieldworkGrid);
    }
}