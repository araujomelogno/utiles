package uy.com.bay.utiles.views.encoding;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.EncodingTask;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.service.EncodingTaskService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Tareas de Codificación")
@Route(value = "encoding-tasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class EncodingTaskView extends VerticalLayout {

    private final EncodingTaskService encodingTaskService;

    private final DatePicker fromDateField = new DatePicker("Desde:");
    private final DatePicker toDateField = new DatePicker("Hasta:");
    private final Grid<EncodingTask> grid = new Grid<>(EncodingTask.class, false);
    private final TextField fileNameFilter = new TextField();
    private final ComboBox<Status> statusFilter = new ComboBox<>();

    public EncodingTaskView(EncodingTaskService encodingTaskService) {
        this.encodingTaskService = encodingTaskService;
        setSizeFull();
        configureGrid();
        add(getToolbar(), grid);
        toDateField.setValue(LocalDateTime.now().plusDays(1).toLocalDate());
        fromDateField.setValue(LocalDateTime.now().minusDays(7).toLocalDate());
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();

        Grid.Column<EncodingTask> fileNameColumn = grid.addColumn(EncodingTask::getSurveyFileName)
                .setHeader("Archivo Base");

        grid.addColumn(task -> {
            if (task.getCreated() != null) {
                return new java.text.SimpleDateFormat("dd/MM/yyyy").format(task.getCreated());
            }
            return "";
        }).setHeader("Creada");

        Grid.Column<EncodingTask> statusColumn = grid.addColumn(EncodingTask::getStatus)
                .setHeader("Estado");

        grid.addColumn(task -> {
            if (task.getProcessed() != null) {
                return new java.text.SimpleDateFormat("dd/MM/yyyy").format(task.getProcessed());
            }
            return "";
        }).setHeader("Procesada");

        grid.addComponentColumn(task -> {
            if (task.getEncodedBaseFile() != null && task.getEncodedBaseFile().length > 0) {
                String fileName = task.getSurveyFileName() != null ? task.getSurveyFileName()+"_codificado" : "encoded_file";
                StreamResource resource = new StreamResource(fileName,
                        () -> new ByteArrayInputStream(task.getEncodedBaseFile()));
                Anchor downloadLink = new Anchor(resource, "Descargar");
                downloadLink.getElement().setAttribute("download", true);
                return downloadLink;
            } else {
                Button button = new Button("Sin archivo");
                button.setEnabled(false);
                return button;
            }
        }).setHeader("Archivo Codificado");

        HeaderRow headerRow = grid.appendHeaderRow();

        fileNameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        fileNameFilter.addValueChangeListener(e -> refreshGrid());
        headerRow.getCell(fileNameColumn).setComponent(fileNameFilter);

        statusFilter.setItems(Status.values());
        statusFilter.addValueChangeListener(e -> refreshGrid());
        headerRow.getCell(statusColumn).setComponent(statusFilter);

        grid.getColumns().forEach(col -> col.setSortable(true));
        grid.getColumns().forEach(c -> c.setResizable(true));
    }

    private HorizontalLayout getToolbar() {
        Button searchButton = new Button("Buscar");
        searchButton.addClickListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(fromDateField, toDateField, searchButton);
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private void refreshGrid() {
        Date from = Date.from(fromDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(toDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (from != null && to != null) {
            grid.setItems(encodingTaskService.findByCreatedBetween(from, to,
                    fileNameFilter.getValue(), statusFilter.getValue()));
        }
    }
}
