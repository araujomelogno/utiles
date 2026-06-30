package uy.com.bay.utiles.views.supervision;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.service.SupervisionTaskService;
import uy.com.bay.utiles.dto.AlchemerStudy;
import uy.com.bay.utiles.services.AlchemerSurveyService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.views.MainLayout;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Supervisión de Encuestas")
@Route(value = "supervision", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class SupervisionView extends VerticalLayout {

	private final SupervisionTaskService supervisionTaskService;
	private final StudyService studyService;
	private final AlchemerSurveyService alchemerSurveyService;

	public SupervisionView(SupervisionTaskService supervisionTaskService, StudyService studyService,
			AlchemerSurveyService alchemerSurveyService) {
		this.supervisionTaskService = supervisionTaskService;
		this.studyService = studyService;
		this.alchemerSurveyService = alchemerSurveyService;

		H2 estudioTitle = new H2("Estudio");
		ComboBox<AlchemerStudy> studyComboBox = new ComboBox<>("Estudio");
		studyComboBox.setItems(alchemerSurveyService.fetchRecentSurveys());
		studyComboBox.setItemLabelGenerator(AlchemerStudy::title);

		H2 title = new H2("Audios");

		MultiFileMemoryBuffer multiFileBuffer = new MultiFileMemoryBuffer();
		Upload multiFileUpload = new Upload(multiFileBuffer);
		multiFileUpload.setAcceptedFileTypes("audio/*");

		H2 questionnaireTitle = new H2("Cuestionario");
		MemoryBuffer questionnaireBuffer = new MemoryBuffer();
		Upload questionnaireUpload = new Upload(questionnaireBuffer);
		questionnaireUpload
				.setAcceptedFileTypes("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		Button processButton = new Button("Procesar", e -> {

			if (studyComboBox.getValue() == null) {
				Notification.show("Por favor, seleccione un estudio.");
				return;
			}
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
						parseFileName(task, fileName);
						task.setAudioContent(audioContent);
						task.setQuestionnaire(questionnaireContent);
						task.setQuestionnaireFileName(questionnaireBuffer.getFileName());
						AlchemerStudy selectedStudy = studyComboBox.getValue();
						task.setAlchemerStudyName(selectedStudy.title());
						task.setAlchemerSuerveyId(selectedStudy.id());

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

		add(estudioTitle, studyComboBox, title, multiFileUpload, questionnaireTitle, questionnaireUpload,
				processButton);
		setSpacing(true);
		setAlignItems(Alignment.CENTER);
	}

	/**
	 * Parsea el nombre del archivo de audio y completa los atributos de la tarea.
	 * El nombre tiene el formato:
	 * {@code audioDate-surveyor-mobilePhone-phoneDisposition-surveyDisposition-...},
	 * por ejemplo
	 * {@code 202606081416-kgonzalez-098424586-Contesta_Completa-A_Benchmark_202606_S00541-_.mp3}.
	 * El primer campo es la fecha del audio en formato {@code yyyyMMddHHmm}.
	 */
	private void parseFileName(SupervisionTask task, String fileName) {
		if (fileName == null) {
			return;
		}
		String[] parts = fileName.split("-");
		if (parts.length > 0 && !parts[0].isBlank()) {
			try {
				task.setAudioDate(new SimpleDateFormat("yyyyMMddHHmm").parse(parts[0].trim()));
			} catch (ParseException ex) {
				ex.printStackTrace();
			}
		}
		if (parts.length > 1) {
			task.setSurveyor(parts[1]);
		}
		if (parts.length > 2) {
			task.setMobilePhone(parts[2]);
		}
		if (parts.length > 3) {
			String aux = parts[3];
			String[] part2 = aux.split("_");
			if (part2.length == 2) {
				task.setPhoneDisposition(part2[0]);
				task.setSurveyDisposition(part2[1]);
			}

		}

		if (parts.length > 4) {
			task.setStudyName(parts[4]);
		}
	}
}
