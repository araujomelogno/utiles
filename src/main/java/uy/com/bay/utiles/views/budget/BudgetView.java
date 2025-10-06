package uy.com.bay.utiles.views.budget;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Pageable;
import uy.com.bay.utiles.entities.Budget;
import uy.com.bay.utiles.services.BudgetConceptService;
import uy.com.bay.utiles.services.BudgetService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Presupuestos")
@Route(value = "budgets", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class BudgetView extends VerticalLayout {

	private final Grid<Budget> grid = new Grid<>(Budget.class);
	private final BudgetForm form;
	private final BudgetService budgetService;

	public BudgetView(BudgetService budgetService, StudyService studyService,
			BudgetConceptService budgetConceptService) {
		this.budgetService = budgetService;
		addClassName("budget-view");
		setSizeFull();
		configureGrid();

		form = new BudgetForm(studyService, budgetConceptService);
		form.addSaveListener(this::saveBudget);
		form.addDeleteListener(this::deleteBudget);
		form.addCloseListener(e -> closeEditor());

		Component content = getContent();
		add(getToolbar(), content);
		updateList();
		closeEditor();
	}

	private Component getContent() {
		HorizontalLayout content = new HorizontalLayout(grid, form);
		content.setFlexGrow(2, grid);
		content.setFlexGrow(1, form);
		content.addClassNames("content");
		content.setSizeFull();
		return content;
	}

	private void configureGrid() {
		grid.addClassNames("budget-grid");
		grid.setSizeFull();
		grid.setColumns("created");
		grid.addColumn(budget -> budget.getStudy() == null ? "" : budget.getStudy().getName()).setHeader("Estudio");
		grid.getColumns().forEach(col -> col.setAutoWidth(true));
		grid.asSingleSelect().addValueChangeListener(event -> editBudget(event.getValue()));
	}

	private Component getToolbar() {
		Button addBudgetButton = new Button("Crear");
		addBudgetButton.addClickListener(click -> addBudget());

		HorizontalLayout toolbar = new HorizontalLayout(addBudgetButton);
		toolbar.addClassName("toolbar");
		return toolbar;
	}

	public void editBudget(Budget budget) {
		if (budget == null) {
			closeEditor();
		} else {
			form.setBudget(budget);
			form.setVisible(true);
			addClassName("editing");
		}
	}

	private void closeEditor() {
		form.setBudget(null);
		form.setVisible(false);
		removeClassName("editing");
	}

	private void addBudget() {
		grid.asSingleSelect().clear();
		Budget newBudget = new Budget();
		newBudget.setEntries(new java.util.ArrayList<>());
		newBudget.setCreated(java.time.LocalDate.now());
		editBudget(newBudget);
	}

	private void saveBudget(BudgetForm.SaveEvent event) {
		Budget budget = event.getBudget();
		if (budget.getEntries() != null) {
			budget.getEntries().forEach(entry -> entry.setBudget(budget));
		}
		budgetService.save(budget);
		updateList();
		closeEditor();
	}

	private void deleteBudget(BudgetForm.DeleteEvent event) {
		budgetService.delete(event.getBudget().getId());
		updateList();
		closeEditor();
	}

	private void updateList() {
		grid.setItems(budgetService.list(Pageable.unpaged()).getContent());
	}
}