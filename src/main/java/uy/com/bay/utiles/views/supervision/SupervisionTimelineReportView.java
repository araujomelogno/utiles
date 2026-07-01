package uy.com.bay.utiles.views.supervision;

import java.util.ArrayList;
import java.util.List;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.service.SupervisionSummaryService;
import uy.com.bay.utiles.dto.SupervisionTimelineReportDTO;
import uy.com.bay.utiles.views.ApexChartRenderHelper;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Dashboard "Evolución temporal" de la supervisión. Un combobox permite elegir un
 * encuestador (surveyor) o "Todos los encuestadores"; al cambiar la selección se
 * recalculan dos gráficos de líneas: el puntaje global en el tiempo y la evolución
 * por dimensión de la rúbrica, ambos agrupados por mes del audioDate.
 */
@PageTitle("Evolución temporal")
@Route(value = "supervision-timeline-report", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionTimelineReportView extends VerticalLayout {

	private static final String ACCENT = "#2E6DB4";
	private static final String ALL_SURVEYORS = "Todos los encuestadores";
	private static final String[] DIMENSION_COLORS = { "#2E6DB4", "#2F8F6B", "#E4A11B", "#8A6FB0" };

	private final SupervisionSummaryService summaryService;
	private final Div content = new Div();

	public SupervisionTimelineReportView(SupervisionSummaryService summaryService) {
		this.summaryService = summaryService;

		setSizeFull();
		setPadding(true);
		setSpacing(false);
		addClassName("supervision-timeline-report-view");

		add(buildHeader());
		add(buildSurveyorSelector());

		content.setWidthFull();
		add(content);

		refresh(null);
	}

	private Div buildHeader() {
		Div header = new Div();
		header.getStyle().set("margin-bottom", "16px");

		Span subtitle = new Span("Evolución de la supervisión a lo largo del tiempo");
		subtitle.getStyle().set("color", "#8A93A3").set("font-size", "13px");

		header.add(subtitle);
		return header;
	}

	private Component buildSurveyorSelector() {
		List<String> items = new ArrayList<>();
		items.add(ALL_SURVEYORS);
		items.addAll(summaryService.findSurveyorNames());

		ComboBox<String> surveyorComboBox = new ComboBox<>("Encuestador");
		surveyorComboBox.setItems(items);
		surveyorComboBox.setValue(ALL_SURVEYORS);
		surveyorComboBox.setWidth("320px");
		surveyorComboBox.getStyle().set("margin-bottom", "16px");
		surveyorComboBox.addValueChangeListener(event -> {
			String value = event.getValue();
			refresh(value == null || ALL_SURVEYORS.equals(value) ? null : value);
		});
		return surveyorComboBox;
	}

	/** Recalcula y vuelve a dibujar los gráficos para el encuestador seleccionado. */
	private void refresh(String surveyor) {
		SupervisionTimelineReportDTO report = summaryService.computeTimelineReport(surveyor);

		content.removeAll();
		content.add(buildGlobalScoreCard(report));
		content.add(buildDimensionsCard(report));
	}

	/** PUNTAJE GLOBAL EN EL TIEMPO: línea con el promedio de aiScore por mes. */
	private Div buildGlobalScoreCard(SupervisionTimelineReportDTO report) {
		Div card = card();
		card.getStyle().set("margin-bottom", "16px");
		card.add(cardTitle("Puntaje global en el tiempo"));
		card.add(cardCaption("Promedio del puntaje global por mes"));

		if (report.getMonths().isEmpty()) {
			card.add(noData());
			return card;
		}

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.LINE).build())
				.withColors(ACCENT).withStroke(StrokeBuilder.get().withWidth(3.0).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
				.withSeries(new Series<>("Puntaje Global", toPoints(report.getMonths(), report.getGlobalScore())))
				.build();
		chart.setWidth("100%");
		chart.setHeight("320px");
		card.add(ApexChartRenderHelper.deferred(chart));
		return card;
	}

	/** EVOLUCIÓN POR DIMENSIÓN DE LA RÚBRICA: una línea por dimensión. */
	private Div buildDimensionsCard(SupervisionTimelineReportDTO report) {
		Div card = card();
		card.add(cardTitle("Evolución por dimensión de la rúbrica"));
		card.add(cardCaption("Promedio de cada dimensión por mes"));

		if (report.getMonths().isEmpty()) {
			card.add(noData());
			return card;
		}

		List<String> months = report.getMonths();

		@SuppressWarnings("unchecked")
		Series<ChartPoint>[] series = new Series[] {
				new Series<>("Cobertura", toPoints(months, report.getCobertura())),
				new Series<>("Fidelidad", toPoints(months, report.getFidelidad())),
				new Series<>("Neutralidad", toPoints(months, report.getNeutralidad())),
				new Series<>("Fluidez", toPoints(months, report.getFluidez())) };

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.LINE).build())
				.withColors(DIMENSION_COLORS).withStroke(StrokeBuilder.get().withWidth(2.0).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
				.withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build()).withSeries(series).build();
		chart.setWidth("100%");
		chart.setHeight("340px");
		card.add(ApexChartRenderHelper.deferred(chart));
		return card;
	}

	private ChartPoint[] toPoints(List<String> months, List<Double> values) {
		ChartPoint[] points = new ChartPoint[months.size()];
		for (int i = 0; i < months.size(); i++) {
			points[i] = new ChartPoint(months.get(i), values.get(i));
		}
		return points;
	}

	// --------------------------------------------------------------- Helpers

	/**
	 * Punto de datos {@code {x, y}} consumido por ApexCharts. El eje X toma el mes y
	 * el eje Y el valor promedio. Jackson serializa ambos campos en el objeto de
	 * datos de la serie.
	 */
	public static class ChartPoint {
		private final String x;
		private final Double y;

		public ChartPoint(String x, Double y) {
			this.x = x;
			this.y = y;
		}

		public String getX() {
			return x;
		}

		public Double getY() {
			return y;
		}
	}

	/** Una tarjeta blanca con el estilo del dashboard. */
	private Div card() {
		Div card = new Div();
		card.getStyle().set("background", "#fff").set("border", "1px solid #E1E6EE").set("border-radius", "11px")
				.set("padding", "20px 22px").set("box-shadow", "0 1px 2px rgba(16,42,78,0.04)");
		return card;
	}

	private Span cardTitle(String text) {
		Span span = new Span(text);
		span.getStyle().set("display", "block").set("font-size", "13px").set("font-weight", "700")
				.set("color", "#102A4E").set("text-transform", "uppercase").set("letter-spacing", "0.04em");
		return span;
	}

	private Span cardCaption(String text) {
		Span span = new Span(text);
		span.getStyle().set("display", "block").set("font-size", "12px").set("color", "#8A93A3").set("margin",
				"2px 0 10px");
		return span;
	}

	private Component noData() {
		Span span = new Span("Sin datos disponibles");
		span.getStyle().set("color", "#8A93A3").set("font-size", "13px").set("font-style", "italic");
		return span;
	}
}
