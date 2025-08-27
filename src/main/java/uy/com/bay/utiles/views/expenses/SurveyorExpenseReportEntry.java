package uy.com.bay.utiles.views.expenses;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.MultiFileReceiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

import elemental.json.Json;
import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportFile;
import uy.com.bay.utiles.data.ExpenseReportStatus;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.security.AuthenticatedUser;
import uy.com.bay.utiles.services.ExpenseReportFileService;
import uy.com.bay.utiles.services.ExpenseReportService;
import uy.com.bay.utiles.services.ExpenseRequestTypeService;
import uy.com.bay.utiles.services.StudyService;
import uy.com.bay.utiles.services.SurveyorService;
import uy.com.bay.utiles.views.MainLayout;

@Route(value = "surveyor-expense-report", layout = MainLayout.class)
@RolesAllowed({ "ADMIN", "ENCUESTADOR" })
public class SurveyorExpenseReportEntry extends Div {

	private ComboBox<Study> study;
	private NumberField amount;
	private ComboBox<ExpenseRequestType> concept;
	private TextArea obs;
	private Upload comprobantes;
	private BeanValidationBinder<ExpenseReport> binder;

	private final StudyService studyService;
	private final ExpenseRequestTypeService expenseRequestTypeService;
	private final ExpenseReportService expenseReportService;
	private final ExpenseReportFileService expenseReportFileService;
	private final AuthenticatedUser authenticatedUser;
	private final SurveyorService surveyorService;

	public SurveyorExpenseReportEntry(StudyService studyService, ExpenseRequestTypeService expenseRequestTypeService,
			ExpenseReportService expenseReportService, ExpenseReportFileService expenseReportFileService,
			AuthenticatedUser authenticatedUser, SurveyorService surveyorService) {
		this.studyService = studyService;
		this.expenseRequestTypeService = expenseRequestTypeService;
		this.expenseReportService = expenseReportService;
		this.expenseReportFileService = expenseReportFileService;
		this.authenticatedUser = authenticatedUser;
		this.surveyorService = surveyorService;

		addClassName("surveyor-expense-report-entry-view");

		FormLayout formLayout = new FormLayout();
		study = new ComboBox<>("Estudio");
		study.setItems(studyService.findAllByShowSurveyor(true));
		study.setItemLabelGenerator(s -> s == null ? "" : s.getName());
		study.setRequired(true);

		amount = new NumberField("Monto");
		amount.setRequiredIndicatorVisible(true);

		concept = new ComboBox<>("Concepto");
		concept.setItems(expenseRequestTypeService.findAll());
		concept.setItemLabelGenerator(c -> c == null ? "" : c.getName());
		concept.setRequired(true);

		obs = new TextArea("Observaciones");
		comprobantes = new Upload();
		comprobantes.setAcceptedFileTypes("image/*", ".pdf");

		Button saveButton = new Button("Guardar");
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		formLayout.add(study, amount, concept, obs, comprobantes, saveButton);
		add(formLayout);

		binder = new BeanValidationBinder<>(ExpenseReport.class);
		binder.bindInstanceFields(this);

		List<ExpenseReportFile> uploadedFiles = new ArrayList<>();
//		MemoryBuffer buffer = new MemoryBuffer();
//		comprobantes.setReceiver(buffer);
//		comprobantes.addSucceededListener(event -> {
//			try (InputStream inputStream = buffer.getInputStream()) {
//				ExpenseReportFile file = new ExpenseReportFile();
//				file.setName(event.getFileName());
//				file.setType(event.getMIMEType());
//				file.setContent(inputStream.readAllBytes());
//				file.setCreated(new Date());
//				uploadedFiles.add(file);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		});

//		comprobantes = new Upload();
//		// files.setMaxFileSize(20480);
//		comprobantes.setAcceptedFileTypes("image/jpeg", "image/png", "application/pdf");
//
		Map<String, ByteArrayOutputStream> fileBuffers = new HashMap<>();

		comprobantes.setReceiver((MultiFileReceiver) (fileName, mimeType) -> {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			fileBuffers.put(fileName, outputStream);
			return outputStream;
		});

		comprobantes.addSucceededListener(event -> {
			String fileName = event.getFileName();
			ByteArrayOutputStream outputStream = fileBuffers.get(fileName);
			byte[] content = outputStream.toByteArray();

			ExpenseReportFile expenseReportFile = new ExpenseReportFile();
			expenseReportFile.setName(fileName);
			expenseReportFile.setCreated(new Date());
			expenseReportFile.setContent(content);
			uploadedFiles.add(expenseReportFile);
			fileBuffers.remove(fileName);
			Notification.show("Archivo " + fileName + " subido.");
			comprobantes.setEnabled(true);
		});
		saveButton.addClickListener(event -> {
			try {
				ExpenseReport expenseReport = new ExpenseReport();
				binder.writeBean(expenseReport);
				authenticatedUser.get().ifPresent(user -> {
					surveyorService.findByName(user.getUsername()).ifPresent(expenseReport::setSurveyor);
				});
				expenseReport.setDate(new Date());
				expenseReport.setExpenseStatus(ExpenseReportStatus.INGRESADO);

				uploadedFiles.forEach(file -> {
					file.setExpenseReport(expenseReport);
					if (expenseReport.getFiles() == null) {
						expenseReport.setFiles(new ArrayList<>());
					}
					expenseReport.getFiles().add(file);

				});
				expenseReport.setFiles(uploadedFiles);
				expenseReportService.save(expenseReport);

				Notification.show("Rendición enviada exitosamente", 3000, Notification.Position.BOTTOM_START);
				clearForm(uploadedFiles);
			} catch (ValidationException e) {
				Notification.show("Por favor, complete todos los campos requeridos.", 3000,
						Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
	}

	private void clearForm(List<ExpenseReportFile> uploadedFiles) {
		binder.readBean(new ExpenseReport());
		obs.clear();
		uploadedFiles.clear();
		// To properly clear the upload component, we need to replace it with a new one.
		// This is a simplified approach.
		comprobantes.getElement().setPropertyJson("files", Json.createArray());
	}
}
