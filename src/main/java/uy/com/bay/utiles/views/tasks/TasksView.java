package uy.com.bay.utiles.views.tasks;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.service.TaskService;
import uy.com.bay.utiles.views.MainLayout;

import java.util.List;

@PageTitle("Tareas")
@RolesAllowed("ADMIN")
@Route(value = "tasks", layout = MainLayout.class)
public class TasksView extends Div {

    private Grid<Task> grid;
    private GridListDataView<Task> gridListDataView;

    private final TaskService taskService;

    public TasksView(TaskService taskService) {
        this.taskService = taskService;
        setSizeFull();
        addClassNames("tasks-view");

        List<Task> tasks = taskService.findAll();
        createGrid(tasks);
        add(grid);
    }

    private void createGrid(List<Task> tasks) {
        grid = new Grid<>(Task.class, false);

        gridListDataView = grid.setItems(tasks);
        addFiltersToGrid();
    }

    private void addFiltersToGrid() {
        HeaderRow filterRow = grid.appendHeaderRow();

        TextField nameFilter = new TextField();
        nameFilter.setPlaceholder("Filter by name");
        nameFilter.setClearButtonVisible(true);
        nameFilter.setWidth("100%");
        nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        nameFilter.addValueChangeListener(e -> gridListDataView.addFilter(
                task -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(task.getName(), nameFilter.getValue())));
        filterRow.getCell(grid.getColumnByKey("name")).setComponent(nameFilter);

        TextField descriptionFilter = new TextField();
        descriptionFilter.setPlaceholder("Filter by description");
        descriptionFilter.setClearButtonVisible(true);
        descriptionFilter.setWidth("100%");
        descriptionFilter.setValueChangeMode(ValueChangeMode.LAZY);
        descriptionFilter.addValueChangeListener(e -> gridListDataView.addFilter(
                task -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(task.getDescription(), descriptionFilter.getValue())));
        filterRow.getCell(grid.getColumnByKey("description")).setComponent(descriptionFilter);
    }
}
