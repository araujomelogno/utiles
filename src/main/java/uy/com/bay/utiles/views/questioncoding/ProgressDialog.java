package uy.com.bay.utiles.views.questioncoding;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class ProgressDialog extends Dialog {

    private final H3 header;
    private final ProgressBar progressBar;

    public ProgressDialog() {
        header = new H3("Procesando...");
        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        VerticalLayout layout = new VerticalLayout(header, progressBar);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(VerticalLayout.Alignment.CENTER);

        add(layout);
    }

    public H3 getHeader() {
        return header;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
