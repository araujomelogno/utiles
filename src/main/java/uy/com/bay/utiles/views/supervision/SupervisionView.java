package uy.com.bay.utiles.views.supervision;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import uy.com.bay.utiles.dto.AudioFile;
import uy.com.bay.utiles.services.OpenAiService;
import uy.com.bay.utiles.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PageTitle("Supervisión de Encuestas")
@Route(value = "supervision", layout = MainLayout.class)
public class SupervisionView extends VerticalLayout {

    private final OpenAiService openAiService;
    private final VerticalLayout resultsLayout = new VerticalLayout();

    public SupervisionView(OpenAiService openAiService) {
        this.openAiService = openAiService;

        H2 title = new H2("Wizard de Supervisión de Encuestas");

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload multiFileUpload = new Upload(buffer);
        multiFileUpload.setAcceptedFileTypes("audio/*");

        Button processButton = new Button("Procesar", e -> {
            List<AudioFile> uploadedFiles = new ArrayList<>();
            buffer.getFiles().forEach(fileName -> {
                InputStream inputStream = buffer.getInputStream(fileName);
                uploadedFiles.add(new AudioFile(fileName, inputStream));
            });
            processFiles(uploadedFiles);
        });

        add(title, multiFileUpload, processButton, resultsLayout);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
    }

    private void processFiles(List<AudioFile> uploadedFiles) {
        if (uploadedFiles.isEmpty()) {
            return;
        }

        Dialog progressDialog = new Dialog();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressDialog.add(new Span("Procesando archivos..."));
        progressDialog.add(progressBar);
        progressDialog.setCloseOnEsc(false);
        progressDialog.open();

        new Thread(() -> {
            Map<String, String> results = openAiService.transcribeAudio(uploadedFiles);
            getUI().ifPresent(ui -> ui.access(() -> {
                progressDialog.close();
                displayResults(results);
            }));
        }).start();
    }

    private void displayResults(Map<String, String> results) {
        resultsLayout.removeAll();
        AtomicInteger counter = new AtomicInteger(1);
        results.forEach((fileName, json) -> {
            if (json != null && !json.startsWith("Error")) {
                StreamResource resource = new StreamResource("transcription-" + counter.getAndIncrement() + ".json",
                        () -> new ByteArrayInputStream(json.getBytes()));
                Anchor downloadLink = new Anchor(resource, "Descargar transcripción de " + fileName);
                resultsLayout.add(downloadLink);
            } else {
                resultsLayout.add(new Span("Error procesando " + fileName + ": " + json));
            }
        });
    }
}
