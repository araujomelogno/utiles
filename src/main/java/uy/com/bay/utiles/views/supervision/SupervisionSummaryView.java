package uy.com.bay.utiles.views.supervision;

import java.util.List;
import java.util.Locale;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.config.plotoptions.Bar;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.service.SupervisionSummaryService;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.DimensionScores;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.MonthScore;
import uy.com.bay.utiles.dto.SupervisionSummaryDTO.StudyScore;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Dashboard "Resumen ejecutivo" de la supervisión. Muestra los indicadores
 * generales (proyectos, encuestas completas, encuestas supervisadas, nivel de
 * supervisión y puntaje global promedio) y cuatro gráficos ApexCharts: evolución
 * del puntaje global (línea), nivel de supervisión (dona), perfil de calidad por
 * dimensión (radar) y puntaje global por proyecto (barras).
 */
@PageTitle("Resumen ejecutivo")
@Route(value = "supervision-summary", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionSummaryView extends VerticalLayout {

	private static final String ACCENT = "#2E6DB4";

	public SupervisionSummaryView(SupervisionSummaryService summaryService) {
		setSizeFull();
		setPadding(true);
		setSpacing(false);
		addClassName("supervision-summary-view");

		SupervisionSummaryDTO summary = summaryService.computeSummary();

		add(buildHeader());
		add(buildKpiRow(summary));
		add(buildChartsTopRow(summary));
		add(buildChartsBottomRow(summary));
	}

	private Div buildHeader() {
		Div header = new Div();
		header.getStyle().set("margin-bottom", "16px");

		Span subtitle = new Span("Indicadores generales de la supervisión");
		subtitle.getStyle().set("color", "#8A93A3").set("font-size", "13px");

		header.add(subtitle);
		return header;
	}

	// ---------------------------------------------------------------- KPI row

	private Div buildKpiRow(SupervisionSummaryDTO summary) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "repeat(5, 1fr)").set("gap", "16px")
				.set("margin-bottom", "16px");

		row.add(kpiCard("Proyectos", formatInteger(summary.getProjectCount()), ACCENT));
		row.add(kpiCard("Encuestas completas", formatInteger(summary.getCompletedSurveys()), "#2F8F6B"));
		row.add(kpiCard("Encuestas supervisadas", formatInteger(summary.getSupervisedSurveys()), "#8A6FB0"));
		row.add(kpiCard("Nivel de supervisión", formatPercentage(summary.getSupervisionLevel()), "#E4A11B"));
		row.add(kpiCard("Puntaje global prom.", formatDecimal(summary.getGlobalScoreAverage()), "#C5503F"));
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

	// ------------------------------------------------------------- Chart rows

	private Div buildChartsTopRow(SupervisionSummaryDTO summary) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "2fr 1fr").set("gap", "16px")
				.set("margin-bottom", "16px");

		row.add(buildEvolutionCard(summary.getScoreEvolution()));
		row.add(buildSupervisionLevelCard(summary.getSupervisionLevel()));
		return row;
	}

	private Div buildChartsBottomRow(SupervisionSummaryDTO summary) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1fr").set("gap", "16px");

		row.add(buildDimensionCard(summary.getDimensionScores()));
		row.add(buildScoreByStudyCard(summary.getScoreByStudy()));
		return row;
	}

	/** EVOLUCIÓN DEL PUNTAJE GLOBAL: gráfico de línea por mes. */
	private Div buildEvolutionCard(List<MonthScore> evolution) {
		Div card = card();
		card.add(cardTitle("Evolución del puntaje global"));
		card.add(cardCaption("Promedio del puntaje global por mes"));

		if (evolution.isEmpty()) {
			card.add(noData());
			return card;
		}

		ChartPoint[] points = evolution.stream().map(m -> new ChartPoint(m.label(), m.score()))
				.toArray(ChartPoint[]::new);

		ApexCharts chart = ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.LINE).build())
				.withColors(ACCENT)
				.withStroke(StrokeBuilder.get().withWidth(3.0).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Puntaje Global", points)).build();
		chart.setWidth("100%");
		chart.setHeight("300px");
		card.add(chart);
		return card;
	}

	/** NIVEL DE SUPERVISIÓN: gráfico de dona con el nivel ya calculado. */
	private Div buildSupervisionLevelCard(double level) {
		Div card = card();
		card.add(cardTitle("Nivel de supervisión"));
		card.add(cardCaption("Supervisadas sobre completas"));

		double levelPct = round1(Math.max(0d, Math.min(1d, level)) * 100d);
		double rest = round1(100d - levelPct);

		Div chartContainer = new Div();
		chartContainer.getStyle().set("display", "flex").set("align-items", "center").set("justify-content", "center");

		ApexCharts chart = ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.DONUT).build())
				.withLabels("Nivel de supervisión", "Restante").withColors(ACCENT, "#E1E6EE")
				.withSeries(new Double[] { levelPct, rest })
				.withStroke(StrokeBuilder.get().withWidth(2.0).withColors("#ffffff").build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build()).build();
		chart.setWidth("100%");
		chart.setHeight("300px");
		chartContainer.add(chart);
		card.add(chartContainer);
		return card;
	}

	/** PERFIL DE CALIDAD POR DIMENSIÓN: gráfico de radar con los promedios. */
	private Div buildDimensionCard(DimensionScores dimensions) {
		Div card = card();
		card.add(cardTitle("Perfil de calidad por dimensión"));
		card.add(cardCaption("Promedio por dimensión de la rúbrica"));

		Double[] values = { dimensions.cobertura(), dimensions.fidelidad(), dimensions.neutralidad(),
				dimensions.fluidez() };

		ApexCharts chart = ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.RADAR).build())
				.withColors(ACCENT).withLabels("Cobertura", "Fidelidad", "Neutralidad", "Fluidez")
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Promedio", values)).build();
		chart.setWidth("100%");
		chart.setHeight("320px");
		card.add(chart);
		return card;
	}

	/** PUNTAJE GLOBAL POR PROYECTO: gráfico de barras (promedio por proyecto). */
	private Div buildScoreByStudyCard(List<StudyScore> scoreByStudy) {
		Div card = card();
		card.add(cardTitle("Puntaje global por proyecto"));
		card.add(cardCaption("Promedio del puntaje global por proyecto"));

		if (scoreByStudy.isEmpty()) {
			card.add(noData());
			return card;
		}

		ChartPoint[] points = scoreByStudy.stream().map(s -> new ChartPoint(s.study(), s.score()))
				.toArray(ChartPoint[]::new);

		Bar bar = new Bar();
		bar.setHorizontal(true);

		ApexCharts chart = ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.BAR).build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(bar).build())
				.withColors(ACCENT)
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Puntaje Global", points)).build();
		int height = Math.max(320, scoreByStudy.size() * 42 + 80);
		chart.setWidth("100%");
		chart.setHeight(height + "px");
		card.add(chart);
		return card;
	}

	// --------------------------------------------------------------- Helpers

	/**
	 * Punto de datos {@code {x, y}} consumido por ApexCharts. El eje X toma la
	 * etiqueta (mes o proyecto) y el eje Y el valor numérico. Jackson serializa
	 * ambos campos en el objeto de datos de la serie.
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

	private static double round1(double value) {
		return Math.round(value * 10d) / 10d;
	}
}
