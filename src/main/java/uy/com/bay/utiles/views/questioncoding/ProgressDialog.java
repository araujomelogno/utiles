package uy.com.bay.utiles.views.questioncoding;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class ProgressDialog extends Dialog {

	private final H2 header;
	private final ProgressBar progressBar;

	public ProgressDialog() {
		header = new H2("Procesando...");
		progressBar = new ProgressBar();
		progressBar.setIndeterminate(true);

		VerticalLayout layout = new VerticalLayout(header, progressBar);
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.setAlignItems(VerticalLayout.Alignment.CENTER);

		add(layout);
	}

	public ProgressBar getProgressBar() {
		return progressBar;
	}
}
