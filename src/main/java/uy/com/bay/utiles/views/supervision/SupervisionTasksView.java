package uy.com.bay.utiles.views.supervision;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
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
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.views.MainLayout;
import uy.com.bay.utiles.data.service.SupervisionTaskService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@PageTitle("Supervision Tasks")
@Route(value = "supervision-tasks", layout = MainLayout.class)
public class SupervisionTasksView extends VerticalLayout {

    private final SupervisionTaskService supervisionTaskService;

    private final DateTimePicker fromDateField = new DateTimePicker("fromDate:");
    private final DateTimePicker toDateField = new DateTimePicker("toDate:");
    private final Grid<SupervisionTask> grid = new Grid<>(SupervisionTask.class);
    private final TextField fileNameFilter = new TextField();
    private final ComboBox<Status> statusFilter = new ComboBox<>();

    public SupervisionTasksView(SupervisionTaskService supervisionTaskService) {
        this.supervisionTaskService = supervisionTaskService;
        setSizeFull();
        configureGrid();
        add(getToolbar(), grid);
        toDateField.setValue(LocalDateTime.now());
        fromDateField.setValue(LocalDateTime.now().minusDays(7));
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        Grid.Column<SupervisionTask> fileNameColumn = grid.addColumn(SupervisionTask::getFileName).setHeader("File Name");
        Grid.Column<SupervisionTask> statusColumn = grid.addColumn(SupervisionTask::getStatus).setHeader("Status");

        grid.addColumn(task -> {
            if (task.getCreated() != null) {
                return new java.text.SimpleDateFormat("dd/MM/yyyy").format(task.getCreated());
            }
            return "";
        }).setHeader("Created").setSortable(true);

        grid.addComponentColumn(task -> {
            if (task.getOutput() != null && !task.getOutput().isEmpty()) {
                Anchor downloadLink = new Anchor(new StreamResource("output.txt",
                        () -> new ByteArrayInputStream(task.getOutput().getBytes(StandardCharsets.UTF_8))), "Download");
                downloadLink.getElement().setAttribute("download", true);
                return downloadLink;
            } else {
                return new Button("No Output");
            }
        }).setHeader("Output").setSortable(true).setComparator(task -> task.getOutput() != null && !task.getOutput().isEmpty());

        HeaderRow headerRow = grid.appendHeaderRow();

        fileNameFilter.setPlaceholder("Filter by file name");
        fileNameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        fileNameFilter.addValueChangeListener(e -> refreshGrid());
        headerRow.getCell(fileNameColumn).setComponent(fileNameFilter);

        statusFilter.setPlaceholder("Filter by status");
        statusFilter.setItems(Status.values());
        statusFilter.addValueChangeListener(e -> refreshGrid());
        headerRow.getCell(statusColumn).setComponent(statusFilter);

        grid.getColumns().forEach(col -> col.setSortable(true));
    }

    private HorizontalLayout getToolbar() {
        Button searchButton = new Button("Search");
        searchButton.addClickListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(fromDateField, toDateField, searchButton);
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private void refreshGrid() {
        LocalDateTime from = fromDateField.getValue();
        LocalDateTime to = toDateField.getValue();
        if (from != null && to != null) {
            grid.setItems(supervisionTaskService.findByCreatedBetween(
                    Date.from(from.atZone(ZoneId.systemDefault()).toInstant()),
                    Date.from(to.atZone(ZoneId.systemDefault()).toInstant()),
                    fileNameFilter.getValue(),
                    statusFilter.getValue()
            ));
        }
    }
}
