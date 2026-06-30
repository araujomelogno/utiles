package uy.com.bay.utiles.views.supervision;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.TitleSubtitleBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.config.plotoptions.Bar;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Tablero de indicadores metodológicos de la supervisión de campo.
 *
 * <p>
 * Replica el contenido de la solapa "Metodología" del tablero de referencia
 * usando los gráficos de ApexCharts (addon {@code com.github.appreciated}, ya
 * disponible vía {@code pom.xml} y usado también en
 * {@link uy.com.bay.utiles.views.joborders.JobOrderAvailabilityView}).
 *
 * <p>
 * Los datos son de ejemplo: cada gráfico está aislado en su propio método para
 * que sea sencillo reemplazarlos por datos reales de un servicio cuando se
 * definan las fuentes.
 */
@PageTitle("Metodología")
@Route(value = "supervision-methodology", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionMethodologyView extends VerticalLayout {

	public SupervisionMethodologyView() {
		setSizeFull();
		setPadding(true);
		setSpacing(true);
		addClassName("supervision-methodology-view");

		H2 title = new H2("Metodología");
		Paragraph subtitle = new Paragraph(
				"Indicadores metodológicos del trabajo de campo: composición de la muestra, "
						+ "avance del relevamiento y distribución de las encuestas efectivas.");
		subtitle.getStyle().set("margin-top", "0").set("color", "var(--lumo-secondary-text-color)");

		Div board = new Div();
		board.setWidthFull();
		board.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

		board.add(card("Muestra por método de recolección", buildCollectionMethodChart()));
		board.add(card("Encuestas efectivas por mes", buildSurveysPerMonthChart()));
		board.add(card("Composición por sexo y tramo etario", buildAgeBySexChart()));
		board.add(card("Distribución por región", buildRegionChart()));
		board.add(card("Avance acumulado de campo", buildFieldProgressChart()));

		add(title, subtitle, board);
	}

	/** Tarjeta contenedora con título para cada gráfico. */
	private Div card(String cardTitle, ApexCharts chart) {
		Div cardDiv = new Div();
		cardDiv.getStyle().set("flex", "1 1 420px").set("min-width", "320px")
				.set("border", "1px solid var(--lumo-contrast-10pct)")
				.set("border-radius", "var(--lumo-border-radius-l)").set("padding", "var(--lumo-space-m)")
				.set("background-color", "var(--lumo-base-color)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)");

		H4 header = new H4(cardTitle);
		header.getStyle().set("margin-top", "0").set("margin-bottom", "var(--lumo-space-s)");

		chart.setWidth("100%");
		cardDiv.add(header, chart);
		return cardDiv;
	}

	/** Donut: distribución de la muestra según el método de recolección. */
	private ApexCharts buildCollectionMethodChart() {
		return ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.DONUT).withHeight("320").build())
				.withLabels("CATI (telefónica)", "CAPI (presencial)", "CAWI (web)", "Mixta")
				.withSeries(48.0, 27.0, 18.0, 7.0)
				.withColors("#0d47a1", "#1e88e5", "#64b5f6", "#bbdefb")
				.withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
				.build();
	}

	/** Columnas: encuestas efectivas logradas por mes. */
	@SuppressWarnings("unchecked")
	private ApexCharts buildSurveysPerMonthChart() {
		return ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.BAR).withHeight("320").build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(horizontalBar(false)).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withColors("#1e88e5")
				.withXaxis(XAxisBuilder.get().withCategories("Ene", "Feb", "Mar", "Abr", "May", "Jun").build())
				.withSeries(new Series<>("Encuestas", 320.0, 410.0, 380.0, 460.0, 520.0, 490.0))
				.build();
	}

	/** Columnas apiladas: composición por sexo dentro de cada tramo etario. */
	@SuppressWarnings("unchecked")
	private ApexCharts buildAgeBySexChart() {
		return ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.BAR).withHeight("320").withStacked(true).build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(horizontalBar(false)).build())
				.withColors("#1e88e5", "#e91e63")
				.withXaxis(XAxisBuilder.get().withCategories("18-29", "30-44", "45-59", "60+").build())
				.withLegend(LegendBuilder.get().withPosition(Position.TOP).build())
				.withSeries(new Series<>("Hombres", 210.0, 260.0, 180.0, 120.0),
						new Series<>("Mujeres", 230.0, 280.0, 200.0, 150.0))
				.build();
	}

	/** Barras horizontales: distribución de la muestra por región. */
	@SuppressWarnings("unchecked")
	private ApexCharts buildRegionChart() {
		return ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.BAR).withHeight("320").build())
				.withPlotOptions(PlotOptionsBuilder.get().withBar(horizontalBar(true)).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withColors("#00897b")
				.withXaxis(XAxisBuilder.get()
						.withCategories("Montevideo", "Canelones", "Maldonado", "Salto", "Interior")
						.build())
				.withSeries(new Series<>("Encuestas", 640.0, 380.0, 210.0, 160.0, 290.0))
				.build();
	}

	/** Área: avance acumulado de encuestas a lo largo del campo. */
	@SuppressWarnings("unchecked")
	private ApexCharts buildFieldProgressChart() {
		return ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.AREA).withHeight("320").build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
				.withColors("#1e88e5")
				.withTitle(TitleSubtitleBuilder.get().withText("Meta: 2.580 encuestas").build())
				.withXaxis(XAxisBuilder.get()
						.withCategories("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6")
						.build())
				.withSeries(new Series<>("Acumulado", 320.0, 730.0, 1110.0, 1570.0, 2090.0, 2580.0))
				.build();
	}

	/** Construye un {@link Bar} con la orientación indicada para reusar en varios gráficos. */
	private Bar horizontalBar(boolean horizontal) {
		Bar bar = new Bar();
		bar.setHorizontal(horizontal);
		bar.setBorderRadius(4.0);
		return bar;
	}
}
