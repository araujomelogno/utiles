package uy.com.bay.utiles.views.extras;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.data.service.ExtraConceptService;
import uy.com.bay.utiles.entities.Extra;
import uy.com.bay.utiles.services.ExcelExportService;
import uy.com.bay.utiles.services.ExtraService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ExtrasReportDialog extends Dialog {

    private final DatePicker fechaDesde = new DatePicker("Fecha desde");
    private final DatePicker fechaHasta = new DatePicker("Fecha hasta");
    private final MultiSelectComboBox<Surveyor> encuestadores = new MultiSelectComboBox<>("Encuestadores");
    private final MultiSelectComboBox<Study> estudios = new MultiSelectComboBox<>("Estudios");
    private final MultiSelectComboBox<ExtraConcept> conceptos = new MultiSelectComboBox<>("Conceptos");

    public ExtrasReportDialog(SurveyorService surveyorService, StudyService studyService,
                              ExtraConceptService extraConceptService, ExtraService extraService,
                              ExcelExportService excelExportService) {
        setHeaderTitle("Reporte de Extras");

        Locale esLocale = new Locale("es", "UY");
        fechaDesde.setLocale(esLocale);
        fechaHasta.setLocale(esLocale);

        encuestadores.setItems(surveyorService.findAll());
        encuestadores.setItemLabelGenerator(Surveyor::getFirstName);

        estudios.setItems(studyService.findAll());
        estudios.setItemLabelGenerator(Study::getName);

        conceptos.setItems(extraConceptService.findAll());
        conceptos.setItemLabelGenerator(ExtraConcept::getDescription);

        VerticalLayout layout = new VerticalLayout(fechaDesde, fechaHasta, encuestadores, estudios, conceptos);
        add(layout);

        Button descargarButton = new Button("Descargar");
        descargarButton.addClickListener(e -> {
            List<Extra> extras = extraService.findExtrasByFilters(
                    fechaDesde.getValue(),
                    fechaHasta.getValue(),
                    encuestadores.getValue().isEmpty() ? null : List.copyOf(encuestadores.getValue()),
                    estudios.getValue().isEmpty() ? null : List.copyOf(estudios.getValue()),
                    conceptos.getValue().isEmpty() ? null : List.copyOf(conceptos.getValue())
            );

            if (extras.isEmpty()) {
                Notification.show("No hay datos para exportar con los filtros seleccionados.");
                return;
            }

            try {
                ByteArrayInputStream in = excelExportService.exportExtrasToExcel(extras);
                StreamResource resource = new StreamResource("extras.xlsx", () -> in);
                Anchor downloadLink = new Anchor(resource, "Descargar");
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.getStyle().set("display", "none");
                add(downloadLink);
                downloadLink.getElement().callJsFunction("click");
            } catch (IOException ex) {
                Notification.show("Error al generar el archivo de Excel.");
                ex.printStackTrace();
            }
        });

        Button cerrarButton = new Button("Cerrar", e -> close());
        getFooter().add(descargarButton, cerrarButton);
    }
}