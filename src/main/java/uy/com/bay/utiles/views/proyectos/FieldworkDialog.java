package uy.com.bay.utiles.views.proyectos;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.service.FieldworkService;

public class FieldworkDialog extends Dialog {

	public FieldworkDialog(Study study, FieldworkService fieldworkService) {
		setHeaderTitle("Solicitudes de Campo para: " + study.getName());
		setWidth("80%");

		Grid<Fieldwork> grid = new Grid<>(Fieldwork.class, false);
		grid.addColumn("type").setHeader("Tipo").setAutoWidth(true);
		grid.addColumn("area").setHeader("Area").setAutoWidth(true);
		grid.addColumn("status").setHeader("Estado").setAutoWidth(true);
		grid.addColumn("initPlannedDate").setHeader("Fecha inicio planificada").setAutoWidth(true);
		grid.addColumn("endPlannedDate").setHeader("Fecha fin planificada").setAutoWidth(true);
		grid.addColumn("goalQuantity").setHeader("Encuestas objetivo").setAutoWidth(true);

		grid.setItems(fieldworkService.findAllByStudy(study));
		grid.addItemClickListener(event -> {
			UI.getCurrent().navigate("fieldworks/" + event.getItem().getId() + "/edit");
			close();
		});

		VerticalLayout layout = new VerticalLayout(grid);
		add(layout);

		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
	}
}
