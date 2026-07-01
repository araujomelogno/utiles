package uy.com.bay.utiles.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
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
 * {@link #renderDeferred} reproduces that working scenario on purpose: it waits
 * for the browser to lay out the container and then builds/adds the chart, so the
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
	 * Builds and adds an ApexCharts component into {@code chartContainer} only after
	 * the browser has laid out that container. Must be called while the container is
	 * attached (typically from {@code onAttach}); the container should be dedicated
	 * to the chart, since it is cleared before the chart is added.
	 *
	 * @param chartContainer the (attached) element that will hold the chart
	 * @param chartFactory   builds a fresh chart component; invoked once the layout
	 *                       is ready
	 */
	public static void renderDeferred(HasComponents chartContainer, SerializableSupplier<Component> chartFactory) {
		Element container = ((Component) chartContainer).getElement();
		container.executeJs(WAIT_FOR_LAYOUT).then(ignored -> {
			chartContainer.removeAll();
			chartContainer.add(chartFactory.get());
		});
	}

	/**
	 * Schedules a window {@code resize} event after the next paints so any already
	 * created ApexCharts recompute their dimensions. Kept for charts that are known
	 * to be created but drawn at the wrong size.
	 *
	 * @param element the element used to run the client-side script (typically the
	 *                view's own element)
	 */
	public static void scheduleResize(Element element) {
		element.executeJs("const fire = () => window.dispatchEvent(new Event('resize'));"
				+ "requestAnimationFrame(() => requestAnimationFrame(fire));"
				+ "setTimeout(fire, 200);");
	}
}
