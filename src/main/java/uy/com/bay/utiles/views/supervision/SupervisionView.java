package uy.com.bay.utiles.views.supervision;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.service.SupervisionTaskService;
import uy.com.bay.utiles.views.MainLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Supervisión de Encuestas")
@Route(value = "supervision", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionView extends VerticalLayout {

	private final SupervisionTaskService supervisionTaskService;

	public SupervisionView(SupervisionTaskService supervisionTaskService) {
		this.supervisionTaskService = supervisionTaskService;

		H2 title = new H2("Audios");

		MultiFileMemoryBuffer multiFileBuffer = new MultiFileMemoryBuffer();
		Upload multiFileUpload = new Upload(multiFileBuffer);
		multiFileUpload.setAcceptedFileTypes("audio/*");

		H2 questionnaireTitle = new H2("Cuestionario");
		MemoryBuffer questionnaireBuffer = new MemoryBuffer();
		Upload questionnaireUpload = new Upload(questionnaireBuffer);

		Button processButton = new Button("Procesar", e -> {
			if (questionnaireBuffer.getInputStream() == null) {
				Notification.show("Por favor, suba un archivo de cuestionario.");
				return;
			}
			if (multiFileBuffer.getFiles().isEmpty()) {
				Notification.show("No hay archivos de audio para procesar.");
				return;
			}
			try {
				byte[] questionnaireContent = questionnaireBuffer.getInputStream().readAllBytes();
				List<SupervisionTask> tasks = new ArrayList<>();
				multiFileBuffer.getFiles().forEach(fileName -> {
					try {
						InputStream inputStream = multiFileBuffer.getInputStream(fileName);
						byte[] audioContent = inputStream.readAllBytes();
						SupervisionTask task = new SupervisionTask();
						task.setCreated(new java.util.Date());
						task.setStatus(uy.com.bay.utiles.data.Status.PENDING);
						task.setFileName(fileName);
						task.setAudioContent(audioContent);
						task.setQuestionnaire(questionnaireContent);
						tasks.add(task);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				});
				supervisionTaskService.saveAll(tasks);
				
				Notification.show("Tareas de supervisión creadas exitosamente.", 5000, Notification.Position.MIDDLE);
				multiFileBuffer.getFiles().clear();
				multiFileUpload.clearFileList();

				questionnaireUpload.clearFileList();

			} catch (IOException ex) {
				ex.printStackTrace();
				Notification.show("Error al leer el cuestionario.");
			}
		});

		add(title, multiFileUpload, questionnaireTitle, questionnaireUpload, processButton);
		setSpacing(true);
		setAlignItems(Alignment.CENTER);
	}
}
