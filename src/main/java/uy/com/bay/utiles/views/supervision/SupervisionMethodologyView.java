package uy.com.bay.utiles.views.supervision;

import java.util.List;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.views.ApexChartRenderHelper;
import uy.com.bay.utiles.views.MainLayout;

/**
 * Read-only methodology view that mirrors the "Metodología" tab of the
 * supervision dashboard mock-up. It explains the supervision scoring rubric:
 * the weight of each dimension (rendered as an ApexCharts donut), how the
 * global score is computed, the quality levels and a card per dimension.
 */
@PageTitle("Metodología")
@Route(value = "supervision-methodology", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionMethodologyView extends VerticalLayout {

	/**
	 * A single rubric dimension: name, weight (%), accent color and description.
	 */
	private record RubricDimension(String name, int weight, String color, String description) {
	}

	/** A quality level shown in the legend: label, color and range text. */
	private record QualityLevel(String label, String color) {
	}

	private static final List<RubricDimension> RUBRIC = List.of(new RubricDimension("Cobertura", 40, "#2E6DB4",
			"Porcentaje de ítems encontrados y leídos completos, incluyendo introducción e instrucciones obligatorias del cuestionario."),
			new RubricDimension("Fidelidad / Exactitud", 35, "#2F8F6B",
					"Mantiene significado, opciones, escala, condiciones y el wording crítico de cada pregunta."),
			new RubricDimension("Neutralidad y protocolo", 15, "#E4A11B",
					"No induce, no presiona ni interpreta por el encuestado. Respeta el protocolo de aplicación."),
			new RubricDimension("Fluidez operacional", 10, "#8A6FB0",
					"Orden lógico, sin confusiones ni repeticiones excesivas durante la entrevista."));

	private static final List<QualityLevel> LEVELS = List.of(new QualityLevel("Excelente ≥ 85", "#2F8F6B"),
			new QualityLevel("Sólido 70–84", "#2E6DB4"), new QualityLevel("Atención 55–69", "#D9982B"),
			new QualityLevel("Crítico < 55", "#C5503F"));

	/** Dedicated holder for the donut; the chart is added once the view is laid out. */
	private final Div weightsChartContainer = new Div();

	public SupervisionMethodologyView() {
		setSizeFull();
		setPadding(true);
		setSpacing(false);
		addClassName("supervision-methodology-view");

		add(buildHeader());
		add(buildTopRow());
		add(buildRubricCards());
	}

	/**
	 * The ApexCharts wrapper renders blank when created before its module is loaded
	 * and the container is laid out (the addon builds the chart once in
	 * {@code firstUpdated()} with no reactivity). Building it after the view is laid
	 * out reproduces the working "navigate away and back" case. The donut keeps
	 * width:100%, so it stays responsive.
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		ApexChartRenderHelper.renderDeferred(weightsChartContainer, this::buildWeightsChart);
	}

	private Div buildHeader() {
		Div header = new Div();
		header.getStyle().set("margin-bottom", "16px");

		Span subtitle = new Span("Rúbrica y cálculo del puntaje de supervisión");
		subtitle.getStyle().set("color", "#8A93A3").set("font-size", "13px");

		header.add(subtitle);
		return header;
	}

	private Div buildTopRow() {
		Div row = new Div();
		row.setWidthFull();
		row.getStyle().set("display", "grid").set("grid-template-columns", "1fr 1.6fr").set("gap", "16px")
				.set("margin-bottom", "16px");
		row.add(buildWeightsCard(), buildCalculationCard());
		return row;
	}

	/** Left card: donut chart with the weight of each rubric dimension. */
	private Div buildWeightsCard() {
		Div card = card();

		card.add(cardTitle("Pesos de la rúbrica"));
		card.add(cardCaption("Ponderación de cada dimensión"));

		// The chart itself is added later, in onAttach, once the container is laid out.
		weightsChartContainer.setWidthFull();
		weightsChartContainer.setMinHeight("240px");
		card.add(weightsChartContainer);

		return card;
	}

	private ApexCharts buildWeightsChart() {
		String[] labels = RUBRIC.stream().map(RubricDimension::name).toArray(String[]::new);
		Double[] series = RUBRIC.stream().map(r -> (double) r.weight()).toArray(Double[]::new);
		String[] colors = RUBRIC.stream().map(RubricDimension::color).toArray(String[]::new);

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.DONUT).build())
				.withLabels(labels).withColors(colors).withSeries(series)
				.withStroke(StrokeBuilder.get().withWidth(2.0).withColors("#ffffff").build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
				.withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build()).build();
		chart.setWidth("100%");
		chart.setHeight("240px");
		return chart;
	}

	/** Right card: explanation of how the global score is computed plus levels. */
	private Div buildCalculationCard() {
		Div card = card();
		card.getStyle().set("padding", "24px 26px");

		card.add(cardTitle("Cómo se calcula el puntaje"));

		Paragraph explanation = new Paragraph();
		explanation.getElement().setProperty("innerHTML",
				"Cada encuesta supervisada se evalúa en cuatro dimensiones, puntuadas de 0 a 100. "
						+ "El <strong style=\"color:#102A4E;\">Puntaje Global</strong> es la suma ponderada de las "
						+ "dimensiones según sus pesos. Permite comparar encuestadores dentro de un proyecto, entre "
						+ "proyectos, de forma acumulada y a lo largo del tiempo.");
		explanation.getStyle().set("font-size", "14px").set("color", "#4A5366").set("line-height", "1.65").set("margin",
				"12px 0 16px");
		card.add(explanation);

		Html formula = new Html("<div style=\"background:#F4F7FB;border:1px solid #E1E6EE;border-radius:9px;"
				+ "padding:14px 18px;font-family:'IBM Plex Mono',monospace;font-size:13px;color:#2A3550;"
				+ "line-height:1.7;\">Global = Cob × 0,40 + Fid × 0,35<br/>"
				+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;+ Neu × 0,15 + Flu × 0,10</div>");
		card.add(formula);

		card.add(buildLevels());
		return card;
	}

	private Div buildLevels() {
		Div levels = new Div();
		levels.getStyle().set("display", "flex").set("gap", "8px").set("flex-wrap", "wrap").set("margin-top", "18px");

		boolean first = true;
		for (QualityLevel level : LEVELS) {
			Div item = new Div();
			item.getStyle().set("display", "flex").set("align-items", "center").set("gap", "7px")
					.set("font-size", "12px").set("color", "#67707F");
			if (!first) {
				item.getStyle().set("margin-left", "14px");
			}
			first = false;

			Span dot = new Span();
			dot.getStyle().set("width", "11px").set("height", "11px").set("border-radius", "3px").set("background",
					level.color());
			item.add(dot, new Span(level.label()));
			levels.add(item);
		}
		return levels;
	}

	/** Bottom row: one card per rubric dimension. */
	private Div buildRubricCards() {
		Div grid = new Div();
		grid.setWidthFull();
		grid.getStyle().set("display", "grid").set("grid-template-columns", "repeat(4, 1fr)").set("gap", "16px");

		for (RubricDimension dimension : RUBRIC) {
			grid.add(buildRubricCard(dimension));
		}
		return grid;
	}

	private Div buildRubricCard(RubricDimension dimension) {
		Div card = card();
		card.getStyle().set("border-top", "4px solid " + dimension.color()).set("display", "flex")
				.set("flex-direction", "column").set("gap", "10px");

		Div headerRow = new Div();
		headerRow.getStyle().set("display", "flex").set("align-items", "baseline").set("justify-content",
				"space-between");

		Span name = new Span(dimension.name());
		name.getStyle().set("font-size", "14px").set("font-weight", "700").set("color", "#102A4E");

		Span weight = new Span(dimension.weight() + "%");
		weight.getStyle().set("font-family", "'IBM Plex Mono',monospace").set("font-size", "22px")
				.set("font-weight", "600").set("color", dimension.color());

		headerRow.add(name, weight);

		Span description = new Span(dimension.description());
		description.getStyle().set("font-size", "12.5px").set("color", "#67707F").set("line-height", "1.55");

		card.add(headerRow, description);
		return card;
	}

	/** A blank white card matching the dashboard styling. */
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
}
