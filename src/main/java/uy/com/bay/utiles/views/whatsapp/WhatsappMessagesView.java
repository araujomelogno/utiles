package uy.com.bay.utiles.views.whatsapp;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.WhatsappFlowTask;
import uy.com.bay.utiles.services.WhatsappService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Envíos Whatsapp")
@Route(value = "whatsapp-messages", layout = MainLayout.class)
@PermitAll
public class WhatsappMessagesView extends VerticalLayout {

	private final WhatsappService whatsappService;
	private final Grid<WhatsappFlowTask> grid = new Grid<>(WhatsappFlowTask.class, false);
	private GridListDataView<WhatsappFlowTask> dataView;

	private DateTimePicker fromDate;
	private DateTimePicker toDate;

	// Filter fields
	private TextField createdFilter;
	private TextField statusFilter;
	private TextField scheduleFilter;
	private TextField processedDateFilter;
	private TextField toFilter;
	private TextField templateNameFilter;
	private TextField wamidFilter;
	private TextField responseStatusFilter;

	public WhatsappMessagesView(WhatsappService whatsappService) {
		this.whatsappService = whatsappService;
		setSizeFull();

		createFilters();
		configureGrid();

		Anchor link = new Anchor(
				"https://docs.google.com/spreadsheets/d/1csAXJJjMeSUlUD1SrFtLZ4wEV2ToGpN97u51sU9ZFAw/edit?usp=sharing",
				"Ver respuestas");

		//
		add(link, createTopBar(), grid);
	}

	private HorizontalLayout createTopBar() {
		fromDate = new DateTimePicker("Programados desde:");
		toDate = new DateTimePicker("Programados hasta:");
		fromDate.setValue(LocalDateTime.now().minusDays(15));

		toDate.setValue(LocalDateTime.now().plusDays(15));
		refreshGrid();
		fromDate.addValueChangeListener(e -> refreshGrid());
		toDate.addValueChangeListener(e -> refreshGrid());

		HorizontalLayout layout = new HorizontalLayout(fromDate, toDate);
		layout.setAlignItems(Alignment.BASELINE);
		return layout;
	}

	private void createFilters() {
		createdFilter = new TextField();
		statusFilter = new TextField();
		scheduleFilter = new TextField();
		processedDateFilter = new TextField();
		toFilter = new TextField();
		templateNameFilter = new TextField();
		wamidFilter = new TextField();
		responseStatusFilter = new TextField();
	}

	private void configureGrid() {
		grid.setSizeFull();

		// Add columns
		Grid.Column<WhatsappFlowTask> createdColumn = grid.addColumn(task -> formatDate(task.getCreated()))
				.setHeader("Created").setSortable(true).setComparator(WhatsappFlowTask::getCreated).setResizable(true)
				.setKey("created");

		Grid.Column<WhatsappFlowTask> statusColumn = grid.addColumn(WhatsappFlowTask::getStatus).setHeader("Status")
				.setSortable(true).setResizable(true).setKey("status");

		Grid.Column<WhatsappFlowTask> scheduleColumn = grid.addColumn(task -> formatDate(task.getSchedule()))
				.setHeader("Schedule").setSortable(true).setComparator(WhatsappFlowTask::getSchedule).setResizable(true)
				.setKey("schedule");

		Grid.Column<WhatsappFlowTask> processedDateColumn = grid.addColumn(task -> formatDate(task.getProcessedDate()))
				.setHeader("Processed Date").setSortable(true).setComparator(WhatsappFlowTask::getProcessedDate)
				.setResizable(true).setKey("processedDate");

		Grid.Column<WhatsappFlowTask> toColumn = grid.addColumn(WhatsappFlowTask::getTo).setHeader("To")
				.setSortable(true).setResizable(true).setKey("to");

		Grid.Column<WhatsappFlowTask> templateNameColumn = grid.addColumn(WhatsappFlowTask::getTemplateName)
				.setHeader("Template Name").setSortable(true).setResizable(true).setKey("templateName");

		Grid.Column<WhatsappFlowTask> wamidColumn = grid.addColumn(WhatsappFlowTask::getWamid).setHeader("Wamid")
				.setSortable(true).setResizable(true).setKey("wamid");

		Grid.Column<WhatsappFlowTask> responseStatusColumn = grid.addColumn(WhatsappFlowTask::getResponseStatus)
				.setHeader("Responsestatus").setSortable(true).setResizable(true).setKey("responseStatus");

		// Prepare DataView
		dataView = grid.setItems(new ArrayList<>());
		dataView.addFilter(this::filter);

		// Header Row for Filters
		HeaderRow headerRow = grid.appendHeaderRow();

		setupFilterHeader(headerRow, createdColumn, createdFilter);
		setupFilterHeader(headerRow, statusColumn, statusFilter);
		setupFilterHeader(headerRow, scheduleColumn, scheduleFilter);
		setupFilterHeader(headerRow, processedDateColumn, processedDateFilter);
		setupFilterHeader(headerRow, toColumn, toFilter);
		setupFilterHeader(headerRow, templateNameColumn, templateNameFilter);
		setupFilterHeader(headerRow, wamidColumn, wamidFilter);
		setupFilterHeader(headerRow, responseStatusColumn, responseStatusFilter);
	}

	private void setupFilterHeader(HeaderRow headerRow, Grid.Column<WhatsappFlowTask> column, TextField filterField) {
		filterField.setPlaceholder("Filter");
		filterField.setClearButtonVisible(true);
		filterField.setValueChangeMode(ValueChangeMode.LAZY);
		filterField.addValueChangeListener(e -> dataView.refreshAll());
		filterField.setWidthFull();
		headerRow.getCell(column).setComponent(filterField);
	}

	private void refreshGrid() {
		if (fromDate.getValue() != null && toDate.getValue() != null) {
			List<WhatsappFlowTask> tasks = whatsappService.getTasksByDateRange(fromDate.getValue(), toDate.getValue());
			dataView = grid.setItems(tasks);
			dataView.addFilter(this::filter);
		} else {
			dataView = grid.setItems(new ArrayList<>());
			dataView.addFilter(this::filter);
		}
	}

	private boolean filter(WhatsappFlowTask task) {
		boolean matchesCreated = matches(formatDate(task.getCreated()), createdFilter.getValue());
		boolean matchesStatus = matches(task.getStatus() != null ? task.getStatus().toString() : "",
				statusFilter.getValue());
		boolean matchesSchedule = matches(formatDate(task.getSchedule()), scheduleFilter.getValue());
		boolean matchesProcessedDate = matches(formatDate(task.getProcessedDate()), processedDateFilter.getValue());
		boolean matchesTo = matches(task.getTo(), toFilter.getValue());
		boolean matchesTemplateName = matches(task.getTemplateName(), templateNameFilter.getValue());
		boolean matchesWamid = matches(task.getWamid(), wamidFilter.getValue());
		boolean matchesResponseStatus = matches(task.getResponseStatus(), responseStatusFilter.getValue());

		return matchesCreated && matchesStatus && matchesSchedule && matchesProcessedDate && matchesTo
				&& matchesTemplateName && matchesWamid && matchesResponseStatus;
	}

	private boolean matches(String value, String searchTerm) {
		if (searchTerm == null || searchTerm.isEmpty()) {
			return true;
		}
		if (value == null) {
			return false;
		}
		return value.toLowerCase().contains(searchTerm.toLowerCase());
	}

	private String formatDate(Date date) {
		if (date == null)
			return "";
		return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
	}
}
