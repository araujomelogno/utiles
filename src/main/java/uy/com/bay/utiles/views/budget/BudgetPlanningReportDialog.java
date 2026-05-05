package uy.com.bay.utiles.views.budget;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.services.BudgetEntryService;
import uy.com.bay.utiles.services.BudgetPlanningExporter;
import uy.com.bay.utiles.services.StudyService;

public class BudgetPlanningReportDialog extends Dialog {

	private final DatePicker fechaDesde = new DatePicker("Fecha desde");
	private final DatePicker fechaHasta = new DatePicker("Fecha hasta");
	private final MultiSelectComboBox<Study> estudios = new MultiSelectComboBox<>("Estudios");
	private final Checkbox totalizarConceptos = new Checkbox("Totalizar conceptos");

	public BudgetPlanningReportDialog(StudyService studyService, BudgetEntryService budgetEntryService,
			BudgetPlanningExporter exporter) {
		setHeaderTitle("Reporte planificación presupuestal");

		Locale esLocale = new Locale("es", "UY");
		DatePicker.DatePickerI18n dateI18n = new DatePicker.DatePickerI18n();
		dateI18n.setDateFormat("dd/MM/yyyy");

		fechaDesde.setLocale(esLocale);
		fechaDesde.setI18n(dateI18n);
		fechaDesde.setRequiredIndicatorVisible(true);

		fechaHasta.setLocale(esLocale);
		fechaHasta.setI18n(dateI18n);
		fechaHasta.setRequiredIndicatorVisible(true);

		estudios.setItems(studyService.findAll());
		estudios.setItemLabelGenerator(s -> s == null ? "" : s.getName());

		FormLayout layout = new FormLayout();
		layout.add(fechaDesde, fechaHasta, estudios, totalizarConceptos);
		add(layout);

		Button descargar = new Button("Descargar");
		descargar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		descargar.addClickListener(e -> generarReporte(budgetEntryService, exporter));

		Button cerrar = new Button("Cerrar", e -> close());
		cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		getFooter().add(cerrar, descargar);
	}

	private void generarReporte(BudgetEntryService budgetEntryService, BudgetPlanningExporter exporter) {
		if (fechaDesde.getValue() == null) {
			Notification.show("Debe ingresar la Fecha desde.");
			return;
		}
		if (fechaHasta.getValue() == null) {
			Notification.show("Debe ingresar la Fecha hasta.");
			return;
		}

		List<Study> selectedStudies = estudios.getValue().isEmpty() ? null : List.copyOf(estudios.getValue());
		List<BudgetEntry> entries = budgetEntryService.findForPlanningReport(fechaDesde.getValue(),
				fechaHasta.getValue(), selectedStudies);

		if (entries.isEmpty()) {
			Notification.show("No hay datos para generar el reporte con los filtros seleccionados.");
			return;
		}

		try {
			InputStream in = exporter.export(entries, fechaDesde.getValue(), fechaHasta.getValue(), selectedStudies,
					totalizarConceptos.getValue());
			StreamResource resource = new StreamResource("planificacion-presupuestal.xlsx", () -> in);
			Anchor downloadLink = new Anchor(resource, "");
			downloadLink.getElement().setAttribute("download", true);
			downloadLink.getStyle().set("display", "none");
			add(downloadLink);
			downloadLink.getElement().callJsFunction("click");
		} catch (IOException ex) {
			Notification.show("Error al generar el archivo de Excel.");
			ex.printStackTrace();
		}
	}
}
