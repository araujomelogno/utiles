package uy.com.bay.utiles.views.proyectos;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uy.com.bay.utiles.views.MainLayout;

/**
 * Lista los proyectos (estudios) de Dooblo con actividad en un período,
 * consultando el endpoint {@code Account/GetUsageByPeriod} de la API de Dooblo.
 */
@PageTitle("Id proyectos Dooblo")
@Route(value = "dooblo-projects", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class DoobloProjectsView extends VerticalLayout {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final String username;
	private final String password;
	private final RestTemplate restTemplate = new RestTemplate();

	private final DatePicker fromDatePicker = new DatePicker("Desde");
	private final DatePicker toDatePicker = new DatePicker("Hasta");
	private final Grid<JsonNode> grid = new Grid<>();

	public DoobloProjectsView(@Value("${surveyToGo.username}") String username,
			@Value("${surveyToGo.password}") String password) {
		this.username = username;
		this.password = password;

		setSizeFull();
		setPadding(true);
		setSpacing(false);

		add(buildDateFilterRow());

		grid.addColumn(project -> project.path("SurveyName").asText("")).setHeader("Estudio").setAutoWidth(true)
				.setSortable(true);
		grid.addColumn(project -> project.path("SurveyID").asText("")).setHeader("Id Doolo").setAutoWidth(true)
				.setSortable(true);
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
	 * Consulta {@code Account/GetUsageByPeriod} para el rango elegido, usando la
	 * misma autenticación básica y el mismo formato de fecha ({@code yyyy-MM-dd})
	 * que el resto de las llamadas a Dooblo, y vuelca los proyectos en la grilla.
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

		String startDate = URLEncoder.encode(fromDate.format(DATE_FORMAT), StandardCharsets.UTF_8);
		String endDate = URLEncoder.encode(toDate.format(DATE_FORMAT), StandardCharsets.UTF_8);
		String url = String.format("http://api.dooblo.net/newapi/Account/GetUsageByPeriod?StartDate=%s&EndDate=%s",
				startDate, endDate);

		HttpHeaders headers = new HttpHeaders();
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
		headers.set("Authorization", "Basic " + new String(encodedAuth));

		try {
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
					String.class);

			Map<String, JsonNode> byId = new LinkedHashMap<>();
			collectProjects(new ObjectMapper().readTree(response.getBody()), byId);
			grid.setItems(new ArrayList<>(byId.values()));
		} catch (Exception e) {
			grid.setItems(Collections.emptyList());
			Notification notification = Notification.show("No se pudieron obtener los proyectos de Dooblo.", 5000,
					Position.MIDDLE);
			notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
		}
	}

	/**
	 * Recorre la respuesta de {@code GetUsageByPeriod} y acumula en {@code byId}
	 * todo objeto que traiga un SurveyID, sin importar a qué profundidad venga
	 * anidado dentro del JSON, descartando los repetidos.
	 */
	private void collectProjects(JsonNode node, Map<String, JsonNode> byId) {
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			JsonNode idNode = node.get("SurveyID");
			if (idNode != null && !idNode.isNull() && !idNode.asText().isBlank()) {
				byId.putIfAbsent(idNode.asText(), node);
				return;
			}
		}
		if (node.isArray() || node.isObject()) {
			node.forEach(child -> collectProjects(child, byId));
		}
	}
}
