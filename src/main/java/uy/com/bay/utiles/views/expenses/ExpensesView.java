package uy.com.bay.utiles.views.expenses;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uy.com.bay.utiles.data.*;
import uy.com.bay.utiles.services.ExpenseRequestService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;

import java.util.Optional;

@PageTitle("Solicitudes de Gastos")
@Route("expenses/:expenseID?/:action?(edit)")
@PermitAll
public class ExpensesView extends Div implements BeforeEnterObserver {

    private final String EXPENSE_ID = "expenseID";
    private final String EXPENSE_EDIT_ROUTE_TEMPLATE = "expenses/%s/edit";

    private final Grid<ExpenseRequest> grid = new Grid<>(ExpenseRequest.class, false);

    private ComboBox<Study> study;
    private ComboBox<Surveyor> surveyor;
    private DatePicker requestDate;
    private DatePicker aprovalDate;
    private DatePicker transferDate;
    private NumberField amount;
    private ComboBox<ExpenseRequestType> concept; 

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");
    private final Button delete = new Button("Delete");

    private final BeanValidationBinder<ExpenseRequest> binder;

    private ExpenseRequest expenseRequest;

    private final ExpenseRequestService expenseRequestService;
    private final StudyService studyService;
    private final SurveyorService surveyorService;
    private final ExpenseRequestTypeService expenseRequestTypeService;

    private Div editorLayoutDiv;

    public ExpensesView(ExpenseRequestService expenseRequestService, StudyService studyService,
                        SurveyorService surveyorService, ExpenseRequestTypeService expenseRequestTypeService) {
        this.expenseRequestService = expenseRequestService;
        this.studyService = studyService;
        this.surveyorService = surveyorService;
        this.expenseRequestTypeService = expenseRequestTypeService;
        addClassNames("expenses-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn(er -> er.getStudy().getName()).setHeader("Estudio").setAutoWidth(true);
        grid.addColumn(er -> er.getSurveyor().getFirstName() + " " + er.getSurveyor().getLastName()).setHeader("Encuestador").setAutoWidth(true);
        grid.addColumn(ExpenseRequest::getRequestDate).setHeader("Solicitado:").setAutoWidth(true);
        grid.addColumn(ExpenseRequest::getAprovalDate).setHeader("Aprobado:").setAutoWidth(true);
        grid.addColumn(ExpenseRequest::getTransferDate).setHeader("Transferido").setAutoWidth(true);
        grid.addColumn(ExpenseRequest::getAmount).setHeader("Monto").setAutoWidth(true);
        grid.addColumn(er -> er.getConcept().getConcept()).setHeader("Concepto").setAutoWidth(true);
        grid.addColumn(ExpenseRequest::getExpenseStatus).setHeader("Estado").setAutoWidth(true);

        grid.setItems(query -> expenseRequestService.list(
                com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(EXPENSE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ExpensesView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(ExpenseRequest.class);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.expenseRequest == null) {
                    this.expenseRequest = new ExpenseRequest();
                }
                binder.writeBean(this.expenseRequest);
                expenseRequestService.update(this.expenseRequest);
                clearForm();
                refreshGrid();
                Notification.show("ExpenseRequest details stored.");
                UI.getCurrent().navigate(ExpensesView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> expenseId = event.getRouteParameters().get(EXPENSE_ID).map(Long::parseLong);
        if (expenseId.isPresent()) {
            Optional<ExpenseRequest> expenseRequestFromBackend = expenseRequestService.get(expenseId.get());
            if (expenseRequestFromBackend.isPresent()) {
                populateForm(expenseRequestFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested expense request was not found, ID = %d", expenseId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(ExpensesView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        study = new ComboBox<>("Estudio");
        study.setItems(studyService.listAll());
        study.setItemLabelGenerator(Study::getName);
        surveyor = new ComboBox<>("Encuestador");
        surveyor.setItems(surveyorService.listAll());
        surveyor.setItemLabelGenerator(s -> s.getFirstName() + " " + s.getLastName());
        requestDate = new DatePicker("Fecha solicitud");
        requestDate.setReadOnly(true);
        aprovalDate = new DatePicker("Fecha aprobación");
        aprovalDate.setReadOnly(true);
        transferDate = new DatePicker("Fecha transferencia");
        transferDate.setReadOnly(true);
        amount = new NumberField("Monto");
        concept = new ComboBox<>("Concepto");
        concept.setItems(expenseRequestTypeService.findAll());
        concept.setItemLabelGenerator(ExpenseRequestType::getConcept);
        formLayout.add(study, surveyor, requestDate, aprovalDate, transferDate, amount, concept);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttonLayout.add(save, cancel, delete);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        H2 title = new H2("Solicitudes de Gastos");
        HorizontalLayout titleLayout = new HorizontalLayout(title);
        titleLayout.setWidthFull();
        titleLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        titleLayout.setFlexGrow(1, title);
        wrapper.add(titleLayout, grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(ExpenseRequest value) {
        this.expenseRequest = value;
        binder.readBean(this.expenseRequest);
    }
}
