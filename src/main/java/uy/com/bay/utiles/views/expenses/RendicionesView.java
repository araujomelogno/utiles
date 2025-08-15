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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.upload.MultiFileReceiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.ExpenseStatus;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@PageTitle("Rendiciones")
@Route("rendiciones/:expenseReportID?/:action?(edit)")
@PermitAll
public class RendicionesView extends Div implements BeforeEnterObserver {

    private final String EXPENSE_REPORT_ID = "expenseReportID";
    private final String EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE = "rendiciones/%s/edit";

    private final Grid<ExpenseReport> grid = new Grid<>(ExpenseReport.class, false);

    private ComboBox<Study> study;
    private ComboBox<Surveyor> surveyor;
    private DatePicker date;
    private NumberField amount;
    private ComboBox<ExpenseRequestType> concept;
    private ComboBox<ExpenseStatus> expenseStatus;
    private Upload files;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");

    private final BeanValidationBinder<ExpenseReport> binder;
    private ExpenseReport expenseReport;
    private final ExpenseReportService expenseReportService;
    private final StudyService studyService;
    private final SurveyorService surveyorService;
    private final ExpenseRequestTypeService expenseRequestTypeService;
    private Div editorLayoutDiv;

    public RendicionesView(ExpenseReportService expenseReportService, StudyService studyService,
                           SurveyorService surveyorService, ExpenseRequestTypeService expenseRequestTypeService) {
        this.expenseReportService = expenseReportService;
        this.studyService = studyService;
        this.surveyorService = surveyorService;
        this.expenseRequestTypeService = expenseRequestTypeService;
        addClassNames("rendiciones-view");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSplitterPosition(80);

        createEditorLayout(splitLayout);
        createGridLayout(splitLayout);

        add(splitLayout);

        grid.addColumn(er -> er.getStudy() != null ? er.getStudy().getName() : "").setHeader("Estudio");
        grid.addColumn(er -> er.getSurveyor() != null ? er.getSurveyor().getFirstName() + " " + er.getSurveyor().getLastName() : "").setHeader("Encuestador");
        grid.addColumn(er -> er.getDate() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(er.getDate()) : "").setHeader("Fecha");
        grid.addColumn(ExpenseReport::getAmount).setHeader("Monto");
        grid.addColumn(er -> er.getConcept() != null ? er.getConcept().getConcept() : "").setHeader("Concepto");
        grid.addColumn(ExpenseReport::getExpenseStatus).setHeader("Estado");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editorLayoutDiv.setVisible(true);
                UI.getCurrent().navigate(String.format(EXPENSE_REPORT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                editorLayoutDiv.setVisible(false);
                clearForm();
                UI.getCurrent().navigate(RendicionesView.class);
            }
        });

        binder = new BeanValidationBinder<>(ExpenseReport.class);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
            editorLayoutDiv.setVisible(false);
        });

        delete.addClickListener(e -> {
            if (this.expenseReport != null && this.expenseReport.getId() != null) {
                expenseReportService.delete(this.expenseReport.getId());
                clearForm();
                refreshGrid();
                Notification.show("Rendición borrada.");
            }
        });

        save.addClickListener(e -> {
            try {
                if (this.expenseReport == null) {
                    this.expenseReport = new ExpenseReport();
                }
                binder.writeBean(this.expenseReport);
                expenseReportService.save(this.expenseReport);
                clearForm();
                refreshGrid();
                Notification.show("Rendición guardada.");
                UI.getCurrent().navigate(RendicionesView.class);
            } catch (ValidationException validationException) {
                Notification.show("Error al guardar la rendición.");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> expenseReportId = event.getRouteParameters().get(EXPENSE_REPORT_ID).map(Long::parseLong);
        if (expenseReportId.isPresent()) {
            Optional<ExpenseReport> expenseReportFromBackend = expenseReportService.get(expenseReportId.get());
            if (expenseReportFromBackend.isPresent()) {
                populateForm(expenseReportFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(String.format("The requested expense report was not found, ID = %d", expenseReportId.get()), 3000, Notification.Position.BOTTOM_START);
                refreshGrid();
                event.forwardTo(RendicionesView.class);
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
        date = new DatePicker("Fecha");
        date.setReadOnly(true);
        amount = new NumberField("Monto");
        concept = new ComboBox<>("Concepto");
        concept.setItems(expenseRequestTypeService.findAll());
        concept.setItemLabelGenerator(ExpenseRequestType::getConcept);
        expenseStatus = new ComboBox<>("Estado");
        expenseStatus.setItems(ExpenseStatus.values());

        files = new Upload();
        files.setAcceptedFileTypes("image/jpeg", "image/png", "application/pdf");
        MultiFileReceiver receiver = (fileName, mimeType) -> {
            try {
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                File file = new File(uploadDir.toFile(), fileName);
                FileOutputStream fos = new FileOutputStream(file);
                if (expenseReport.getFiles() == null) {
                    expenseReport.setFiles(new ArrayList<>());
                }
                expenseReport.getFiles().add(file.getAbsolutePath());
                return fos;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };
        files.setReceiver(receiver);

        formLayout.add(study, surveyor, date, amount, concept, expenseStatus, files);
        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
        editorLayoutDiv.setVisible(false);
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
        Button createButton = new Button("Crear Rendición", e -> {
            grid.asSingleSelect().clear();
            populateForm(new ExpenseReport());
            editorLayoutDiv.setVisible(true);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout topLayout = new HorizontalLayout(createButton);
        topLayout.setWidth("100%");
        topLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        splitLayout.addToPrimary(wrapper);
        wrapper.add(topLayout, grid);
    }

    private void refreshGrid() {
        grid.setItems(expenseReportService.list(org.springframework.data.domain.Pageable.unpaged()).getContent());
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(ExpenseReport value) {
        this.expenseReport = value;
        binder.readBean(this.expenseReport);
    }
}
