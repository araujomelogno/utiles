package uy.com.bay.utiles.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableSupplier;

/**
 * Helper to work around the {@code appreciated/apexcharts-flow} addon rendering
 * blank on the first (direct) load of a view.
 *
 * <p>
 * The addon's {@code <apex-charts-wrapper>} web component creates the ApexCharts
 * instance once, in Lit's {@code firstUpdated()}, with no reactivity afterwards.
 * When a view is opened directly, the element is often inserted before its
 * JavaScript module has registered / before the container has a real size, so
 * {@code firstUpdated()} never produces a chart and the wrapper stays empty. It
 * only renders after navigating away and back, because then the element is
 * created fresh while everything is already loaded and laid out.
 *
 * <p>
 * Both entry points here reproduce that working scenario on purpose: the chart is
 * attached to the DOM only after the browser has laid out its container, so the
 * wrapper is created when the module is loaded and the container has a real width.
 * The chart keeps its percentage width, so responsiveness is preserved.
 */
public final class ApexChartRenderHelper {

	/** Client script that resolves after the browser has painted the view. */
	private static final String WAIT_FOR_LAYOUT = "return new Promise(r => requestAnimationFrame("
			+ "() => requestAnimationFrame(() => setTimeout(r, 50))));";

	private ApexChartRenderHelper() {
	}

	/**
	 * Wraps a chart in a full-width holder that adds it to the DOM only once its
	 * container has been laid out. Drop-in replacement for adding the chart
	 * directly: {@code parent.add(deferred(chart))}.
	 *
	 * @param chart the chart component to render once the layout is ready
	 * @return a holder to add to the layout in place of the chart
	 */
	public static Component deferred(Component chart) {
		Div holder = new Div();
		holder.setWidthFull();
		renderDeferred(holder, () -> chart);
		return holder;
	}

	/**
	 * Builds and adds an ApexCharts component into {@code chartContainer} only after
	 * the browser has laid out that container. The container should be dedicated to
	 * the chart, since it is cleared before the chart is added. Safe to call while
	 * the container is still detached: the client script runs once it is attached.
	 *
	 * @param chartContainer the element that will hold the chart
	 * @param chartFactory   supplies the chart component; invoked once the layout is
	 *                       ready
	 */
	public static void renderDeferred(HasComponents chartContainer, SerializableSupplier<Component> chartFactory) {
		Element container = ((Component) chartContainer).getElement();
		container.executeJs(WAIT_FOR_LAYOUT).then(ignored -> {
			chartContainer.removeAll();
			chartContainer.add(chartFactory.get());
		});
	}
}
