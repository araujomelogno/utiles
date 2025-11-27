package uy.com.bay.utiles.views.questioncoding;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class ProgressDialog extends Dialog {

    private final ProgressBar progressBar;
    private final H3 header;

    public ProgressDialog() {
        this.progressBar = new ProgressBar();
        this.header = new H3("Procesando...");
        VerticalLayout layout = new VerticalLayout(header, progressBar);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setAlignItems(VerticalLayout.Alignment.CENTER);
        add(layout);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public H3 getHeaderComponent() {
        return header;
    }

    public void setHeaderText(String text) {
        this.header.setText(text);
    }
}
