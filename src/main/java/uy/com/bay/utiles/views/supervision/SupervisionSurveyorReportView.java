package uy.com.bay.utiles.views.supervision;

import java.util.List;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
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
import uy.com.bay.utiles.dto.SupervisionStudyReportDTO.SurveyorScore;
import uy.com.bay.utiles.dto.SupervisionSurveyorReportDTO;
import uy.com.bay.utiles.dto.SupervisionSurveyorReportDTO.SurveyorProfile;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Dashboard "Resultados por encuestador" de la supervisión. Muestra dos gráficos
 * ApexCharts: el ranking de encuestadores por puntaje global promedio (barras) y
 * un radar de múltiples series que compara al mejor encuestador con el que
 * requiere apoyo, según el promedio de cada dimensión de la rúbrica.
 */
@PageTitle("Resultados por encuestador")
@Route(value = "supervision-surveyor-report", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionSurveyorReportView extends VerticalLayout {

	private static final String ACCENT = "#2E6DB4";
	private static final String BEST_COLOR = "#2F8F6B";
	private static final String WORST_COLOR = "#C5503F";

	public SupervisionSurveyorReportView(SupervisionSummaryService summaryService) {
		setSizeFull();
		setPadding(true);
		setSpacing(false);
		addClassName("supervision-surveyor-report-view");

		SupervisionSurveyorReportDTO report = summaryService.computeSurveyorReport();

		add(buildHeader());
		add(buildChartsRow(report));
	}

	private Div buildHeader() {
		Div header = new Div();
		header.getStyle().set("margin-bottom", "16px");

		Span subtitle = new Span("Desempeño de los encuestadores");
		subtitle.getStyle().set("color", "#8A93A3").set("font-size", "13px");

		header.add(subtitle);
		return header;
	}

	private Div buildChartsRow(SupervisionSurveyorReportDTO report) {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "1.3fr 1fr").set("gap", "16px")
				.set("align-items", "start");

		row.add(buildRankingCard(report.getRanking()));
		row.add(buildBestVsWorstCard(report.getBest(), report.getWorst()));
		return row;
	}

	/** RANKING DE ENCUESTADORES: barras con el promedio de aiScore por encuestador. */
	private Div buildRankingCard(List<SurveyorScore> ranking) {
		Div card = card();
		card.add(cardTitle("Ranking de encuestadores"));
		card.add(cardCaption("Puntaje global promedio por encuestador"));

		if (ranking.isEmpty()) {
			card.add(noData());
			return card;
		}

		ChartPoint[] points = ranking.stream().map(s -> new ChartPoint(s.surveyor(), s.score()))
				.toArray(ChartPoint[]::new);

		Bar bar = new Bar();
		bar.setHorizontal(true);

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.BAR).build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(bar).build()).withColors(ACCENT)
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withSeries(new Series<>("Puntaje Global", points)).build();
		int height = Math.max(320, ranking.size() * 42 + 80);
		chart.setWidth("100%");
		chart.setHeight(height + "px");
		card.add(chart);
		return card;
	}

	/** MEJOR VS. REQUIERE APOYO: radar de dos series (mejor y peor encuestador). */
	private Div buildBestVsWorstCard(SurveyorProfile best, SurveyorProfile worst) {
		Div card = card();
		card.add(cardTitle("Mejor vs. requiere apoyo"));
		card.add(cardCaption("Perfil por dimensión del mejor encuestador y del que requiere apoyo"));

		if (best == null || worst == null) {
			card.add(noData());
			return card;
		}

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.RADAR).build())
				.withColors(BEST_COLOR, WORST_COLOR).withLabels("Cobertura", "Fidelidad", "Neutralidad", "Fluidez")
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
				.withSeries(new Series<>("Mejor — " + best.surveyor(), dimensionValues(best)),
						new Series<>("Requiere apoyo — " + worst.surveyor(), dimensionValues(worst)))
				.build();
		chart.setWidth("100%");
		chart.setHeight("360px");
		card.add(chart);
		return card;
	}

	private Double[] dimensionValues(SurveyorProfile profile) {
		return new Double[] { profile.dimensions().cobertura(), profile.dimensions().fidelidad(),
				profile.dimensions().neutralidad(), profile.dimensions().fluidez() };
	}

	// --------------------------------------------------------------- Helpers

	/**
	 * Punto de datos {@code {x, y}} consumido por ApexCharts. El eje X toma el
	 * encuestador y el eje Y el puntaje promedio. Jackson serializa ambos campos en
	 * el objeto de datos de la serie.
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
