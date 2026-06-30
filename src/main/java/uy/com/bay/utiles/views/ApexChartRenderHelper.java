package uy.com.bay.utiles.views;

import com.vaadin.flow.dom.Element;

/**
 * Helper to work around ApexCharts' intermittent blank render inside Vaadin.
 *
 * <p>
 * ApexCharts measures its container size at render time. When a view is opened
 * directly, the chart can be drawn before the layout has settled (drawer
 * animation, container still 0px wide), producing an empty SVG that only shows
 * up after navigating to other views. Asking the browser to fire a {@code resize}
 * event once the view is attached and painted forces ApexCharts to recompute its
 * dimensions and draw, without losing responsiveness (the chart keeps its
 * percentage width).
 */
public final class ApexChartRenderHelper {

	private ApexChartRenderHelper() {
	}

	/**
	 * Schedules a window {@code resize} event after the next paints so any
	 * ApexCharts on the page recompute their dimensions. Call from
	 * {@code onAttach}.
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
