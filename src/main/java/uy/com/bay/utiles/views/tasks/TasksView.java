package uy.com.bay.utiles.views.tasks;

import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.service.TaskService;
import uy.com.bay.utiles.views.MainLayout;

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
        grid.addColumn(Task::getJobType).setHeader("Tipo de tarea").setSortable(true);
        grid.addColumn(Task::getStatus).setHeader("Estado").setSortable(true);
        grid.addColumn(Task::getSurveyId).setHeader("Id encuesta").setSortable(true);

        gridListDataView = grid.setItems(tasks);
        addFiltersToGrid();
    }

    private void addFiltersToGrid() {
        HeaderRow filterRow = grid.appendHeaderRow();

        TextField jobTypeFilter = new TextField();
        jobTypeFilter.setPlaceholder("Filter");
        jobTypeFilter.setClearButtonVisible(true);
        jobTypeFilter.setWidth("100%");
        jobTypeFilter.setValueChangeMode(ValueChangeMode.LAZY);
        jobTypeFilter.addValueChangeListener(
                event -> gridListDataView.addFilter(task -> caseInsensitiveContains(task.getJobType().name(), jobTypeFilter.getValue())));
        filterRow.getCell(grid.getColumnByKey("jobType")).setComponent(jobTypeFilter);

        TextField statusFilter = new TextField();
        statusFilter.setPlaceholder("Filter");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("100%");
        statusFilter.setValueChangeMode(ValueChangeMode.LAZY);
        statusFilter.addValueChangeListener(
                event -> gridListDataView.addFilter(task -> caseInsensitiveContains(task.getStatus().name(), statusFilter.getValue())));
        filterRow.getCell(grid.getColumnByKey("status")).setComponent(statusFilter);

        TextField surveyIdFilter = new TextField();
        surveyIdFilter.setPlaceholder("Filter");
        surveyIdFilter.setClearButtonVisible(true);
        surveyIdFilter.setWidth("100%");
        surveyIdFilter.setValueChangeMode(ValueChangeMode.LAZY);
        surveyIdFilter.addValueChangeListener(event -> gridListDataView
                .addFilter(task -> caseInsensitiveContains(task.getSurveyId().toString(), surveyIdFilter.getValue())));
        filterRow.getCell(grid.getColumnByKey("surveyId")).setComponent(surveyIdFilter);
    }

    private boolean caseInsensitiveContains(String where, String what) {
        return where.toLowerCase().contains(what.toLowerCase());
    }

    
}
