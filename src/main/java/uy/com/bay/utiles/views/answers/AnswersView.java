package uy.com.bay.utiles.views.answers;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.jpa.domain.Specification;
import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.services.AlchemerAnswerService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Respuestas Alchemer")
@Route(value = "answers", layout = MainLayout.class)
@PermitAll
public class AnswersView extends Div {

    private Grid<AlchemerAnswer> grid;
    private final AlchemerAnswerFilter filter;
    private final AlchemerAnswerService alchemerAnswerService;

    public AnswersView(AlchemerAnswerService alchemerAnswerService) {
        this.alchemerAnswerService = alchemerAnswerService;
        this.filter = new AlchemerAnswerFilter();
        addClassName("answers-view");
        setSizeFull();
        createGrid();
        add(grid);
    }

    private void createGrid() {
        grid = new Grid<>(AlchemerAnswer.class, false);
        grid.setSizeFull();

        Grid.Column<AlchemerAnswer> questionColumn = grid.addColumn(AlchemerAnswer::getQuestion).setHeader("Question").setSortable(true);
        Grid.Column<AlchemerAnswer> answerColumn = grid.addColumn(AlchemerAnswer::getAnswer).setHeader("Answer").setSortable(true);
        Grid.Column<AlchemerAnswer> typeColumn = grid.addColumn(AlchemerAnswer::getType).setHeader("Type").setSortable(true);
        grid.addColumn(AlchemerAnswer::getSurveyId).setHeader("Survey ID").setSortable(true);
        grid.addColumn(AlchemerAnswer::getResponseId).setHeader("Response ID").setSortable(true);

        GridLazyDataView<AlchemerAnswer> dataView = grid.setItems(q -> alchemerAnswerService.list(
                org.springframework.data.domain.PageRequest.of(q.getPage(), q.getPageSize()),
                createSpecification(filter)
        ).stream());

        filter.setDataView(dataView);
        addGridFilters(grid.appendHeaderRow(), questionColumn, answerColumn, typeColumn);
    }

    private void addGridFilters(HeaderRow headerRow, Grid.Column<AlchemerAnswer> questionColumn,
                              Grid.Column<AlchemerAnswer> answerColumn, Grid.Column<AlchemerAnswer> typeColumn) {
        TextField questionFilter = createTextFieldFilter(filter::setQuestion);
        TextField answerFilter = createTextFieldFilter(filter::setAnswer);
        TextField typeFilter = createTextFieldFilter(filter::setType);

        headerRow.getCell(questionColumn).setComponent(questionFilter);
        headerRow.getCell(answerColumn).setComponent(answerFilter);
        headerRow.getCell(typeColumn).setComponent(typeFilter);
    }

    private TextField createTextFieldFilter(java.util.function.Consumer<String> consumer) {
        TextField filterField = new TextField();
        filterField.setValueChangeMode(ValueChangeMode.EAGER);
        filterField.setClearButtonVisible(true);
        filterField.addValueChangeListener(e -> consumer.accept(e.getValue()));
        return filterField;
    }

    private Specification<AlchemerAnswer> createSpecification(AlchemerAnswerFilter filter) {
        return (root, query, cb) -> {
            Specification<AlchemerAnswer> spec = Specification.where(null);
            if (!filter.getQuestion().isEmpty()) {
                spec = spec.and((r, q, c) -> c.like(c.lower(r.get("question")), "%" + filter.getQuestion().toLowerCase() + "%"));
            }
            if (!filter.getAnswer().isEmpty()) {
                spec = spec.and((r, q, c) -> c.like(c.lower(r.get("answer")), "%" + filter.getAnswer().toLowerCase() + "%"));
            }
            if (!filter.getType().isEmpty()) {
                spec = spec.and((r, q, c) -> c.like(c.lower(r.get("type")), "%" + filter.getType().toLowerCase() + "%"));
            }
            return spec.toPredicate(root, query, cb);
        };
    }
}
