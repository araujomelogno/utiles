package uy.com.bay.utiles.views.gantt;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import uy.com.bay.utiles.data.Fieldwork;

public class FieldworkDetailsDialog extends Dialog {

	public FieldworkDetailsDialog(Fieldwork fieldwork) {
		setHeaderTitle("Detalles del Trabajo de Campo");

		FormLayout formLayout = new FormLayout();

		TextField study = new TextField("Estudio");
		study.setValue(fieldwork.getStudy() != null ? fieldwork.getStudy().getName() : "");
		study.setReadOnly(true);

		TextField area = new TextField("Area");
		area.setValue(fieldwork.getArea() != null ? fieldwork.getArea().getNombre() : "");
		area.setReadOnly(true);

		TextField status = new TextField("Estado");
		status.setValue(fieldwork.getStatus() != null ? fieldwork.getStatus().toString() : "");
		status.setReadOnly(true);

		TextField type = new TextField("Tipo");
		type.setValue(fieldwork.getType() != null ? fieldwork.getType().toString() : "");
		type.setReadOnly(true);

		TextField doobloId = new TextField("Dooblo Id");
		doobloId.setValue(fieldwork.getDoobloId() != null ? fieldwork.getDoobloId() : "");
		doobloId.setReadOnly(true);

		TextField alchemerId = new TextField("Alchemer Id");
		alchemerId.setValue(fieldwork.getAlchemerId() != null ? fieldwork.getAlchemerId() : "");
		alchemerId.setReadOnly(true);

		TextField initPlannedDate = new TextField("Fecha Planificada Inicio");
		initPlannedDate
				.setValue(fieldwork.getInitPlannedDate() != null ? fieldwork.getInitPlannedDate().toString() : "");
		initPlannedDate.setReadOnly(true);

		TextField endPlannedDate = new TextField("Fecha Planificada Fin");
		endPlannedDate.setValue(fieldwork.getEndPlannedDate() != null ? fieldwork.getEndPlannedDate().toString() : "");
		endPlannedDate.setReadOnly(true);

		TextField initDate = new TextField("Fecha Inicio");
		initDate.setValue(fieldwork.getInitDate() != null ? fieldwork.getInitDate().toString() : "");
		initDate.setReadOnly(true);

		TextField endDate = new TextField("Fecha Fin");
		endDate.setValue(fieldwork.getEndDate() != null ? fieldwork.getEndDate().toString() : "");
		endDate.setReadOnly(true);

		TextField goalQuantity = new TextField("Cantidad Objetivo");
		goalQuantity.setValue(fieldwork.getGoalQuantity() != null ? fieldwork.getGoalQuantity().toString() : "");
		goalQuantity.setReadOnly(true);

		TextField completed = new TextField("Completas");
		completed.setValue(fieldwork.getCompleted() != null ? fieldwork.getCompleted().toString() : "");
		completed.setReadOnly(true);

 
		TextArea obs = new TextArea("Observaciones");
		obs.setValue(fieldwork.getObs() != null ? fieldwork.getObs() : "");
		obs.setReadOnly(true);

		formLayout.add(study, type, status, area, doobloId, alchemerId, initPlannedDate, endPlannedDate, initDate,
				endDate, goalQuantity, completed, obs);
		add(formLayout);

		Button editButton = new Button("Editar");
		editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		editButton.addClickListener(e -> {
			UI.getCurrent().navigate("fieldworks/" + fieldwork.getId() + "/edit");
			close();
		});

		Button closeButton = new Button("Cerrar", e -> close());
		getFooter().add(editButton, closeButton);
	}
}