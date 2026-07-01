package uy.com.bay.utiles.views.supervision;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.plotoptions.Bar;
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
import uy.com.bay.utiles.dto.SupervisionStudyReportDTO;
import uy.com.bay.utiles.dto.SupervisionStudyReportDTO.SurveyorScore;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.DimensionScores;
import uy.com.bay.utiles.views.ApexChartRenderHelper;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Dashboard "Reporte por proyecto" de la supervisión. Un combobox permite elegir
 * un proyecto (alchemerStudyName) o "Todos los estudios"; al cambiar la selección
 * se recalculan los indicadores (encuestas completas, supervisadas, nivel de
 * supervisión, puntaje global promedio y promedio por encuestador) y los gráficos
 * ApexCharts: dimensiones del proyecto (radar) y puntaje global por encuestador
 * (barras).
 */
@PageTitle("Reporte por proyecto")
@Route(value = "supervision-study-report", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionStudyReportView extends VerticalLayout {

	private static final String ACCENT = "#2E6DB4";
	private static final String ALL_STUDIES = "Todos los estudios";

	private final SupervisionSummaryService summaryService;
	private final Div content = new Div();

	public SupervisionStudyReportView(SupervisionSummaryService summaryService) {
		this.summaryService = summaryService;

		setSizeFull();
		setPadding(true);
		setSpacing(false);
		addClassName("supervision-study-report-view");

		add(buildHeader());
		add(buildStudySelector());

		content.setWidthFull();
		add(content);

		refresh(null);
	}

	private Div buildHeader() {
		Div header = new Div();
		header.getStyle().set("margin-bottom", "16px");

		Span subtitle = new Span("Indicadores de supervisión por proyecto");
		subtitle.getStyle().set("color", "#8A93A3").set("font-size", "13px");

		header.add(subtitle);
		return header;
	}

	private Component buildStudySelector() {
		List<String> items = new ArrayList<>();
		items.add(ALL_STUDIES);
		items.addAll(summaryService.findStudyNames());

		ComboBox<String> studyComboBox = new ComboBox<>("Estudio");
		studyComboBox.setItems(items);
		studyComboBox.setValue(ALL_STUDIES);
		studyComboBox.setWidth("320px");
		studyComboBox.getStyle().set("margin-bottom", "16px");
		studyComboBox.addValueChangeListener(event -> {
			String value = event.getValue();
			refresh(value == null || ALL_STUDIES.equals(value) ? null : value);
		});
		return studyComboBox;
	}

	/** Recalcula y vuelve a dibujar los indicadores para el proyecto seleccionado. */
	private void refresh(String studyName) {
		SupervisionStudyReportDTO report = summaryService.computeStudyReport(studyName);

		content.removeAll();
		content.add(buildKpiRow(report));
		content.add(buildChartsRow(report));
	}

	// ---------------------------------------------------------------- KPI row

	private Div buildKpiRow(SupervisionStudyReportDTO report) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "repeat(5, 1fr)").set("gap", "16px")
				.set("margin-bottom", "16px");

		row.add(kpiCard("Encuestas completas", formatInteger(report.getCompletedSurveys()), "#2F8F6B"));
		row.add(kpiCard("Encuestas supervisadas", formatInteger(report.getSupervisedSurveys()), "#8A6FB0"));
		row.add(kpiCard("Nivel de supervisión", formatPercentage(report.getSupervisionLevel()), "#E4A11B"));
		row.add(kpiCard("Puntaje global prom.", formatDecimal(report.getGlobalScoreAverage()), "#C5503F"));
		row.add(kpiCard("Prom. x encuestador", formatDecimal(report.getAveragePerSurveyor()), ACCENT));
		return row;
	}

	private Div kpiCard(String title, String value, String color) {
		Div card = card();
		card.getStyle().set("border-top", "4px solid " + color).set("display", "flex").set("flex-direction", "column")
				.set("gap", "6px");

		Span label = new Span(title);
		label.getStyle().set("font-size", "12px").set("color", "#8A93A3").set("text-transform", "uppercase")
				.set("letter-spacing", "0.04em").set("font-weight", "600");

		Span number = new Span(value);
		number.getStyle().set("font-family", "'IBM Plex Mono',monospace").set("font-size", "30px")
				.set("font-weight", "600").set("color", "#102A4E");

		card.add(label, number);
		return card;
	}

	// -------------------------------------------------------------- Chart row

	private Div buildChartsRow(SupervisionStudyReportDTO report) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr").set("gap", "16px");

		row.add(buildDimensionCard(report.getDimensionScores()));
		row.add(buildScoreBySurveyorCard(report.getScoreBySurveyor()));
		return row;
	}

	/** DIMENSIONES DEL PROYECTO: gráfico de radar con los promedios. */
	private Div buildDimensionCard(DimensionScores dimensions) {
		Div card = card();
		card.add(cardTitle("Dimensiones del proyecto"));
		card.add(cardCaption("Promedio por dimensión de la rúbrica"));

		Double[] values = { dimensions.cobertura(), dimensions.fidelidad(), dimensions.neutralidad(),
				dimensions.fluidez() };

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.RADAR).build())
				.withColors(ACCENT).withLabels("Cobertura", "Fidelidad", "Neutralidad", "Fluidez")
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Promedio", values)).build();
		chart.setWidth("100%");
		chart.setHeight("320px");
		card.add(ApexChartRenderHelper.deferred(chart));
		return card;
	}

	/** PUNTAJE GLOBAL POR ENCUESTADOR: gráfico de barras (promedio por encuestador). */
	private Div buildScoreBySurveyorCard(List<SurveyorScore> scoreBySurveyor) {
		Div card = card();
		card.add(cardTitle("Puntaje global por encuestador"));
		card.add(cardCaption("Promedio del puntaje global por encuestador"));

		if (scoreBySurveyor.isEmpty()) {
			card.add(noData());
			return card;
		}

		ChartPoint[] points = scoreBySurveyor.stream().map(s -> new ChartPoint(s.surveyor(), s.score()))
				.toArray(ChartPoint[]::new);

		Bar bar = new Bar();
		bar.setHorizontal(true);

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.BAR).build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(bar).build()).withColors(ACCENT)
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Puntaje Global", points)).build();
		int height = Math.max(320, scoreBySurveyor.size() * 42 + 80);
		chart.setWidth("100%");
		chart.setHeight(height + "px");
		card.add(ApexChartRenderHelper.deferred(chart));
		return card;
	}

	// --------------------------------------------------------------- Helpers

	/**
	 * Punto de datos {@code {x, y}} consumido por ApexCharts. El eje X toma la
	 * etiqueta (encuestador) y el eje Y el valor numérico. Jackson serializa ambos
	 * campos en el objeto de datos de la serie.
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

	private String formatInteger(long value) {
		return String.format(Locale.US, "%,d", value);
	}

	private String formatDecimal(double value) {
		return String.format(Locale.US, "%.1f", value);
	}

	private String formatPercentage(double ratio) {
		return String.format(Locale.US, "%.1f%%", ratio * 100d);
	}
}
