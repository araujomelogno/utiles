package uy.com.bay.utiles.views.joborders;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.StrokeBuilder;
import com.github.appreciated.apexcharts.config.builder.TitleSubtitleBuilder;
import com.github.appreciated.apexcharts.config.builder.TooltipBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.plotoptions.Heatmap;
import com.github.appreciated.apexcharts.config.xaxis.XAxisType;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.JobOrder;
import uy.com.bay.utiles.data.Provider;
import uy.com.bay.utiles.services.JobOrderService;

@PageTitle("Disponibilidad de proveedores")
@Route("joborder-availability")
@RolesAllowed("ADMIN")
public class JobOrderAvailabilityView extends VerticalLayout {

	private static final String[] MONTH_NAMES = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct",
			"Nov", "Dic" };

	/**
	 * Custom tooltip rendered when hovering a cell. It reads the extra
	 * {@code studies} field that we serialize into each data point and lists the
	 * study names grouped in that cell. The wrapper {@code eval}s this string into
	 * a real JS function.
	 */
	private static final String TOOLTIP_FUNCTION = """
			function({ seriesIndex, dataPointIndex, w }) {
			    var dp = w.config.series[seriesIndex].data[dataPointIndex] || {};
			    var provider = w.config.series[seriesIndex].name || '';
			    var month = dp.x || '';
			    var count = (dp.y != null) ? dp.y : 0;
			    var studies = dp.studies || [];
			    function esc(s) {
			        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
			    }
			    var body;
			    if (studies.length) {
			        body = '<ul style="margin:4px 0 0 16px; padding:0;">';
			        for (var i = 0; i < studies.length; i++) {
			            body += '<li>' + esc(studies[i]) + '</li>';
			        }
			        body += '</ul>';
			    } else {
			        body = '<div style="margin-top:4px;"><i>Sin estudios</i></div>';
			    }
			    return '<div style="padding:8px; max-width:340px; white-space:normal;">'
			         + '<b>' + esc(provider) + '</b> \\u2014 ' + esc(month)
			         + '<br/>Cantidad: ' + count
			         + body
			         + '</div>';
			}
			""";

	private final JobOrderService jobOrderService;

	private final DatePicker fromPicker = new DatePicker("Desde");
	private final DatePicker toPicker = new DatePicker("Hasta");
	private final Div chartContainer = new Div();

	public JobOrderAvailabilityView(JobOrderService jobOrderService) {
		this.jobOrderService = jobOrderService;

		setSizeFull();
		addClassName("joborder-availability-view");

		add(new H2("Disponibilidad de proveedores"));

		fromPicker.setValue(LocalDate.now());
		toPicker.setValue(LocalDate.now().plusMonths(6));

		Button verButton = new Button("Ver", e -> rebuildChart());
		verButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		HorizontalLayout controls = new HorizontalLayout(fromPicker, toPicker, verButton);
		controls.setAlignItems(Alignment.END);
		add(controls);

		chartContainer.setWidthFull();
		add(chartContainer);

		rebuildChart();
	}

	private void rebuildChart() {
		LocalDate from = fromPicker.getValue();
		LocalDate to = toPicker.getValue();

		chartContainer.removeAll();

		if (from == null || to == null) {
			Notification.show("Seleccione las fechas \"Desde\" y \"Hasta\".");
			return;
		}
		if (from.isAfter(to)) {
			Notification.show("La fecha \"Desde\" no puede ser posterior a \"Hasta\".");
			return;
		}

		List<Series> series = buildSeries(from, to);
		if (series.isEmpty()) {
			chartContainer.add(new Span("No hay órdenes de trabajo en el rango seleccionado."));
			return;
		}

		ApexCharts chart = buildChart(from, to, series);
		chartContainer.add(chart);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Series> buildSeries(LocalDate from, LocalDate to) {
		List<JobOrder> jobOrders = jobOrderService.findOverlapping(from, to);

		// X axis: every month between "from" and "to" (inclusive).
		List<YearMonth> months = new ArrayList<>();
		YearMonth end = YearMonth.from(to);
		for (YearMonth m = YearMonth.from(from); !m.isAfter(end); m = m.plusMonths(1)) {
			months.add(m);
		}

		// Y axis: providers present in the fetched job orders (step 2 grouping).
		Map<Long, Provider> providerMap = new LinkedHashMap<>();
		for (JobOrder jo : jobOrders) {
			if (jo.getProvider() != null && jo.getProvider().getId() != null) {
				providerMap.putIfAbsent(jo.getProvider().getId(), jo.getProvider());
			}
		}
		List<Provider> providers = new ArrayList<>(providerMap.values());
		providers.sort(Comparator.comparing(p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER));

		List<Series> series = new ArrayList<>();
		for (Provider provider : providers) {
			HeatmapDataPoint[] data = new HeatmapDataPoint[months.size()];
			for (int i = 0; i < months.size(); i++) {
				YearMonth month = months.get(i);
				List<JobOrder> matching = matching(jobOrders, provider, month);
				List<String> studies = matching.stream()
						.map(jo -> jo.getStudy() != null ? jo.getStudy().getName() : null)
						.filter(name -> name != null && !name.isBlank()).distinct().sorted()
						.collect(Collectors.toList());
				data[i] = new HeatmapDataPoint(monthLabel(month), matching.size(), studies);
			}
			series.add(new Series<>(provider.getName(), data));
		}
		return series;
	}

	/**
	 * Job orders of {@code provider} whose [init, end] interval (by month) contains
	 * {@code month}.
	 */
	private List<JobOrder> matching(List<JobOrder> jobOrders, Provider provider, YearMonth month) {
		List<JobOrder> result = new ArrayList<>();
		for (JobOrder jo : jobOrders) {
			if (jo.getProvider() == null || !provider.getId().equals(jo.getProvider().getId())) {
				continue;
			}
			if (jo.getInit() == null || jo.getEnd() == null) {
				continue;
			}
			YearMonth init = YearMonth.from(jo.getInit());
			YearMonth finish = YearMonth.from(jo.getEnd());
			if (!month.isBefore(init) && !month.isAfter(finish)) {
				result.add(jo);
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private ApexCharts buildChart(LocalDate from, LocalDate to, List<Series> series) {
		Heatmap heatmap = new Heatmap();
		heatmap.setRadius(12.0);
		heatmap.setEnableShades(true);
		heatmap.setShadeIntensity(0.5);

		ApexCharts chart = ApexChartsBuilder.get()
				.withChart(ChartBuilder.get().withType(Type.HEATMAP).withHeight("500").build())
				.withPlotOptions(PlotOptionsBuilder.get().withHeatmap(heatmap).build())
				.withDataLabels(DataLabelsBuilder.get().withEnabled(false).build())
				.withStroke(StrokeBuilder.get().withWidth(1.0).withColors("#ffffff").build())
				.withColors("#008FFB")
				.withTitle(TitleSubtitleBuilder.get().withText("Órdenes de trabajo por proveedor y mes").build())
				.withXaxis(XAxisBuilder.get().withType(XAxisType.CATEGORIES).build())
				.withTooltip(TooltipBuilder.get().withCustom(TOOLTIP_FUNCTION).build())
				.withSeries(series.toArray(new Series[0])).build();

		chart.setWidthFull();
		chart.setHeight("500px");
		return chart;
	}

	private String monthLabel(YearMonth month) {
		return MONTH_NAMES[month.getMonthValue() - 1] + " " + month.getYear();
	}

	/**
	 * Data point for a heatmap cell. Besides the {@code x}/{@code y} required by
	 * ApexCharts, it carries the list of study names so the custom tooltip can
	 * display them. Jackson serializes all three fields into the data object.
	 */
	public static class HeatmapDataPoint {
		private final String x;
		private final Integer y;
		private final List<String> studies;

		public HeatmapDataPoint(String x, Integer y, List<String> studies) {
			this.x = x;
			this.y = y;
			this.studies = studies;
		}

		public String getX() {
			return x;
		}

		public Integer getY() {
			return y;
		}

		public List<String> getStudies() {
			return studies;
		}
	}
}
