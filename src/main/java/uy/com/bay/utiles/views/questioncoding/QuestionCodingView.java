package uy.com.bay.utiles.views.questioncoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.chat.client.ChatClient;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Question Coding")
@Route(value = "question-coding", layout = MainLayout.class)
public class QuestionCodingView extends VerticalLayout {

	private final VerticalLayout step1;
	private final VerticalLayout step2;
	private final VerticalLayout step3;
	private final VerticalLayout step4;
	private List<ColumnMapping> columnMappings;
	private final ChatClient chatClient;
	private byte[] surveyFileContent;
	private byte[] codeMappingFileContent;

	public QuestionCodingView(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		step1 = createStep1();
		step2 = createStep2();
		step3 = createStep3();
		step4 = createStep4();

		add(step1, step2, step3, step4);
		showStep(1);
	}

	private VerticalLayout createStep1() {
		H2 header = new H2("Paso 1: Cargar archivo de encuesta");
		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		Button nextButton = new Button("Siguiente");

		upload.addSucceededListener(event -> {
			try (InputStream inputStream = buffer.getInputStream()) {
				surveyFileContent = inputStream.readAllBytes();
				Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(surveyFileContent));
				Sheet sheet = workbook.getSheetAt(0);
				Row headerRow = sheet.getRow(0);
				columnMappings = new ArrayList<>();
				for (Cell cell : headerRow) {
					columnMappings.add(new ColumnMapping(cell.getStringCellValue()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		nextButton.addClickListener(event -> showStep(2));

		VerticalLayout layout = new VerticalLayout(header, upload, nextButton);
		layout.setSizeFull();
		layout.setJustifyContentMode(JustifyContentMode.CENTER);
		layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		layout.getStyle().set("text-align", "center");
		return layout;
	}

	private VerticalLayout createStep2() {
		H2 header = new H2("Paso 2: Seleccionar columnas a codificar");
		Grid<ColumnMapping> grid = new Grid<>();
		Button prevButton = new Button("Anterior");
		Button nextButton = new Button("Siguiente");

		grid.addColumn(ColumnMapping::getOriginalName).setHeader("Encabezado");
		grid.addColumn(new ComponentRenderer<>(mapping -> {
			Checkbox checkbox = new Checkbox();
			checkbox.setValue(mapping.isToCode());
			checkbox.addValueChangeListener(event -> mapping.setToCode(event.getValue()));
			return checkbox;
		})).setHeader("Codificar");

		grid.addColumn(new ComponentRenderer<>(mapping -> {
			TextField textField = new TextField();
			textField.setValue(mapping.getNewName() != null ? mapping.getNewName() : "");
			textField.addValueChangeListener(event -> mapping.setNewName(event.getValue()));
			return textField;
		})).setHeader("Nuevo nombre");

		prevButton.addClickListener(event -> showStep(1));
		nextButton.addClickListener(event -> showStep(3));

		VerticalLayout layout = new VerticalLayout(header, grid, prevButton, nextButton);
		layout.setSizeFull();
		layout.setJustifyContentMode(JustifyContentMode.CENTER);
		layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		layout.getStyle().set("text-align", "center");
		return layout;
	}

	private VerticalLayout createStep3() {
		H2 header = new H2("Paso 3: Cargar archivo de mapeo de códigos");
		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		Button prevButton = new Button("Anterior");
		Button nextButton = new Button("Siguiente");

		upload.addSucceededListener(event -> {
			try (InputStream inputStream = buffer.getInputStream()) {
				codeMappingFileContent = inputStream.readAllBytes();
				Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(codeMappingFileContent));
				Sheet sheet = workbook.getSheetAt(0);
				Row headerRow = sheet.getRow(0);
				List<String> headers = new ArrayList<>();
				for (Cell cell : headerRow) {
					headers.add(cell.getStringCellValue());
				}

				List<ColumnMapping> selected = columnMappings.stream().filter(ColumnMapping::isToCode)
						.collect(Collectors.toList());
				boolean allHeadersValid = true;
				for (ColumnMapping mapping : selected) {
					String originalName = mapping.getNewName() != null && !mapping.getNewName().isEmpty()
							? mapping.getNewName()
							: mapping.getOriginalName();
					if (!headers.contains(originalName + "-ETIQUETA") || !headers.contains(originalName + "-CODIGO")) {
						allHeadersValid = false;
						break;
					}
				}
				if (allHeadersValid) {
					Notification.show("Validación exitosa");
				} else {
					Notification
							.show("Error de validación: El archivo de mapeo de códigos no tiene el formato correcto.");
				}

			} catch (Exception e) {
				e.printStackTrace();
				Notification.show("Error al leer el archivo de mapeo de códigos.");
			}
		});

		prevButton.addClickListener(event -> showStep(2));
		nextButton.addClickListener(event -> showStep(4));

		VerticalLayout layout = new VerticalLayout(header, upload, prevButton, nextButton);
		layout.setSizeFull();
		layout.setJustifyContentMode(JustifyContentMode.CENTER);
		layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		layout.getStyle().set("text-align", "center");
		return layout;
	}

	private VerticalLayout createStep4() {
		H2 header = new H2("Paso 4: Procesar codificación");
		Button prevButton = new Button("Anterior");
		Button processButton = new Button("Procesar");
		Button downloadButton = new Button("Descargar");
		downloadButton.setVisible(false);

		Anchor downloadLink = new Anchor();
		downloadLink.getElement().setAttribute("download", true);
		downloadLink.add(downloadButton);

		processButton.addClickListener(event -> {
			ProgressDialog dialog = new ProgressDialog();
			dialog.open();
			Workbook surveyWorkbook = null;
			try {
				surveyWorkbook = new XSSFWorkbook(new ByteArrayInputStream(surveyFileContent));
				Workbook codeMappingWorkbook = new XSSFWorkbook(new ByteArrayInputStream(codeMappingFileContent));

				List<ColumnMapping> selected = columnMappings.stream().filter(ColumnMapping::isToCode)
						.collect(Collectors.toList());
				int total = selected.size();
				int processed = 0;

				for (ColumnMapping mapping : selected) {
					String columnName = mapping.getNewName() != null && !mapping.getNewName().isEmpty()
							? mapping.getNewName()
							: mapping.getOriginalName();

					// Extract data for the prompt
					List<String> surveyResponses = getColumnData(surveyWorkbook, mapping.getOriginalName());
					List<String> codeLabels = getColumnData(codeMappingWorkbook, columnName + "-ETIQUETA");
					List<String> codeValues = getColumnData(codeMappingWorkbook, columnName + "-CODIGO");

					String prompt = buildPrompt(surveyResponses, codeLabels, codeValues);
					String response = chatClient.prompt(prompt).call().content();

					updateSurveyWithCodedResponses(surveyWorkbook, mapping.getOriginalName(), response);

					processed++;
					int percentage = (int) (((double) processed / total) * 100);
					dialog.getProgressBar().setValue(percentage / 100.0);
					dialog.getHeader().setText("Procesando: " + processed + "/" + total);
				}
			} catch (IOException e) {
				e.printStackTrace();
				Notification.show("Error al procesar los archivos.");
			}

			dialog.getHeader().setText("Proceso completado");
			downloadButton.setVisible(true);
			if (surveyWorkbook != null) {
				downloadLink.setHref(createExcelStreamResource(surveyWorkbook));
			}
		});

		prevButton.addClickListener(event -> showStep(3));

		VerticalLayout layout = new VerticalLayout(header, prevButton, processButton, downloadLink);
		layout.setSizeFull();
		layout.setJustifyContentMode(JustifyContentMode.CENTER);
		layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		layout.getStyle().set("text-align", "center");
		return layout;
	}

	private StreamResource createExcelStreamResource(Workbook workbook) {
		return new StreamResource("coded_survey.xlsx", () -> {
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				workbook.write(bos);
				workbook.close();
				return new ByteArrayInputStream(bos.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		});
	}

	private void showStep(int step) {
		step1.setVisible(step == 1);
		step2.setVisible(step == 2);
		step3.setVisible(step == 3);
		step4.setVisible(step == 4);
	}

	private void updateSurveyWithCodedResponses(Workbook workbook, String originalColumnName, String codedResponses) {
		Sheet sheet = workbook.getSheetAt(0);
		Row headerRow = sheet.getRow(0);
		int newColumnIndex = headerRow.getLastCellNum();
		Cell newHeaderCell = headerRow.createCell(newColumnIndex);
		newHeaderCell.setCellValue(originalColumnName + "_CODED");

		String[] responses = codedResponses.split("\n");
		for (int i = 0; i < responses.length; i++) {
			Row row = sheet.getRow(i + 1);
			if (row != null) {
				Cell cell = row.createCell(newColumnIndex);
				cell.setCellValue(responses[i]);
			}
		}
	}

	private List<String> getColumnData(Workbook workbook, String columnName) {
		List<String> data = new ArrayList<>();
		Sheet sheet = workbook.getSheetAt(0);
		int columnIndex = -1;
		Row headerRow = sheet.getRow(0);
		for (Cell cell : headerRow) {
			if (cell.getStringCellValue().equals(columnName)) {
				columnIndex = cell.getColumnIndex();
				break;
			}
		}

		if (columnIndex != -1) {
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cell = row.getCell(columnIndex);
					if (cell != null) {
						data.add(cell.getStringCellValue());
					}
				}
			}
		}
		return data;
	}

	private String buildPrompt(List<String> surveyResponses, List<String> codeLabels, List<String> codeValues) {
		StringBuilder prompt = new StringBuilder();
		prompt.append(System.getProperty("app.openai.prompt"));
		prompt.append("\n\nRespuestas a codificar:\n");
		surveyResponses.forEach(response -> prompt.append("- ").append(response).append("\n"));
		prompt.append("\n\nCódigos:\n");
		for (int i = 0; i < codeLabels.size(); i++) {
			prompt.append("- ").append(codeLabels.get(i)).append(": ").append(codeValues.get(i)).append("\n");
		}
		return prompt.toString();
	}

	public static class ColumnMapping {
		private final String originalName;
		private boolean toCode;
		private String newName;

		public ColumnMapping(String originalName) {
			this.originalName = originalName;
		}

		public String getOriginalName() {
			return originalName;
		}

		public boolean isToCode() {
			return toCode;
		}

		public void setToCode(boolean toCode) {
			this.toCode = toCode;
		}

		public String getNewName() {
			return newName;
		}

		public void setNewName(String newName) {
			this.newName = newName;
		}
	}
}
