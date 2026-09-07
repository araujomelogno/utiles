package uy.com.bay.utiles.views.proyectos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.dto.DoobloProjectDTO;
import uy.com.bay.utiles.tasks.DoobloSurveyRetriever;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Lista los proyectos (estudios) de Dooblo con actividad en un período,
 * consultando el endpoint {@code Account/GetUsageByPeriod} de la API de Dooblo.
 */
@PageTitle("Id proyectos Dooblo")
@Route(value = "dooblo-projects", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DoobloProjectsView extends VerticalLayout {

	private final DoobloSurveyRetriever doobloSurveyRetriever;

	private final DatePicker fromDatePicker = new DatePicker("Desde");
	private final DatePicker toDatePicker = new DatePicker("Hasta");
	private final Grid<DoobloProjectDTO> grid = new Grid<>(DoobloProjectDTO.class, false);

	public DoobloProjectsView(DoobloSurveyRetriever doobloSurveyRetriever) {
		this.doobloSurveyRetriever = doobloSurveyRetriever;

		setSizeFull();
		setPadding(true);
		setSpacing(false);

		add(buildDateFilterRow());

		grid.addColumn(DoobloProjectDTO::getSurveyName).setHeader("Estudio").setAutoWidth(true).setSortable(true);
		grid.addColumn(DoobloProjectDTO::getSurveyId).setHeader("Id Doolo").setAutoWidth(true).setSortable(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		grid.setSizeFull();
		add(grid);

		refresh();
	}

	/**
	 * Fila horizontal con los DatePicker "Desde" y "Hasta" que acotan el período
	 * consultado a Dooblo. "Desde" arranca seis meses atrás y "Hasta" en el día
	 * actual; al cambiar cualquiera se vuelve a consultar la API.
	 */
	private Component buildDateFilterRow() {
		LocalDate today = LocalDate.now();
		toDatePicker.setValue(today);
		fromDatePicker.setValue(today.minusMonths(6));

		fromDatePicker.addValueChangeListener(event -> refresh());
		toDatePicker.addValueChangeListener(event -> refresh());

		HorizontalLayout row = new HorizontalLayout(fromDatePicker, toDatePicker);
		row.setAlignItems(Alignment.BASELINE);
		row.getStyle().set("margin-bottom", "16px");
		return row;
	}

	/**
	 * Consulta los proyectos de Dooblo para el rango elegido y los vuelca en la
	 * grilla.
	 */
	private void refresh() {
		LocalDate fromDate = fromDatePicker.getValue();
		LocalDate toDate = toDatePicker.getValue();
		if (fromDate == null || toDate == null) {
			grid.setItems(Collections.emptyList());
			return;
		}
		if (fromDate.isAfter(toDate)) {
			grid.setItems(Collections.emptyList());
			Notification.show("La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.", 5000, Position.MIDDLE);
			return;
		}

		Date from = Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date to = Date.from(toDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

		try {
			List<DoobloProjectDTO> projects = doobloSurveyRetriever.getUsageByPeriod(from, to);
			grid.setItems(projects);
		} catch (Exception e) {
			grid.setItems(Collections.emptyList());
			Notification notification = Notification.show("No se pudieron obtener los proyectos de Dooblo.", 5000,
					Position.MIDDLE);
			notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}
}
