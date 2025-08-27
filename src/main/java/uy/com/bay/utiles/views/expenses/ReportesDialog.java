package uy.com.bay.utiles.views.expenses;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.ExcelReportGenerator;
import uy.com.bay.utiles.services.JournalEntryService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ReportesDialog extends Dialog {

    private final DatePicker fechaDesde = new DatePicker("Fecha desde");
    private final DatePicker fechaHasta = new DatePicker("Fecha hasta");
    private final MultiSelectComboBox<Surveyor> encuestadores = new MultiSelectComboBox<>("Encuestadores");
    private final MultiSelectComboBox<Study> estudios = new MultiSelectComboBox<>("Estudios");

    private final Button descargar = new Button("Descargar");
    private final Button cerrar = new Button("Cerrar");

    private final SurveyorService surveyorService;
    private final StudyService studyService;
	private final JournalEntryService journalEntryService;

    public ReportesDialog(SurveyorService surveyorService, StudyService studyService,
			JournalEntryService journalEntryService) {
        this.surveyorService = surveyorService;
        this.studyService = studyService;
		this.journalEntryService = journalEntryService;

        setHeaderTitle("Generar Reporte de Gastos");

        // Configurar el formato de fecha
        fechaDesde.setLocale(new java.util.Locale("es", "ES"));
        fechaHasta.setLocale(new java.util.Locale("es", "ES"));

        // Poblar ComboBoxes
        encuestadores.setItems(surveyorService.listAll());
        encuestadores.setItemLabelGenerator(s -> s == null ? "" : s.getName());
        estudios.setItems(studyService.listAll());
        estudios.setItemLabelGenerator(s -> s == null ? "" : s.getName());

        FormLayout formLayout = new FormLayout();
        formLayout.add(fechaDesde, fechaHasta, encuestadores, estudios);
        add(formLayout);

        getFooter().add(cerrar, descargar);
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        descargar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cerrar.addClickListener(e -> close());
        descargar.addClickListener(e -> downloadReport());
    }

    private void downloadReport() {
        Specification<JournalEntry> spec = journalEntryService.createFilterSpecification(fechaDesde.getValue(),
                fechaHasta.getValue(), encuestadores.getValue(), estudios.getValue());
        List<JournalEntry> journalEntries = journalEntryService.list(spec);

        if (journalEntries.isEmpty()) {
            Notification.show("No hay datos para generar el reporte con los filtros seleccionados.");
            return;
        }

        try {
            ByteArrayOutputStream excelStream = ExcelReportGenerator.generateExcel(journalEntries);
            StreamResource resource = new StreamResource("reporte.xlsx",
                    () -> new ByteArrayInputStream(excelStream.toByteArray()));

            final Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.getElement().getStyle().set("display", "none");
            add(downloadLink);
            downloadLink.getElement().callJsFunction("click");
            Notification.show("Reporte generado con éxito.");
        } catch (IOException e) {
            Notification.show("Error al generar el reporte: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
