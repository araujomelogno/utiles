package uy.com.bay.utiles.views.questioncoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.RolesAllowed;
import uy.com.bay.utiles.dto.aiencoding.QuestionAIAnswer;
import uy.com.bay.utiles.dto.aiencoding.QuestionAICode;
import uy.com.bay.utiles.dto.aiencoding.QuestionAIInput;
import uy.com.bay.utiles.dto.aiencoding.QuestionEncodingAIInput;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Codificación")
@Route(value = "question-coding", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class QuestionCodingView extends VerticalLayout {

	private final VerticalLayout step1;
	private VerticalLayout step2;
	private final VerticalLayout step3;
	private final VerticalLayout step4;
	private List<ColumnMapping> columnMappings;
	private final ChatClient chatClient;
	private byte[] surveyFileContent;
	private byte[] codeMappingFileContent;
	@Value("${app.openai.prompt}")
	private String basePrompt;
	Grid<ColumnMapping> grid;

	public QuestionCodingView(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		grid = new Grid();
		step1 = createStep1();
		step2 = createStep2();
		step3 = createStep3();
		step4 = createStep4();

		add(step1, step2, step3, step4);
		showStep(1);
	}

	private VerticalLayout createStep1() {
		H2 header = new H2("Paso 1: Cargar archivo de estudio");
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
				grid.setItems(columnMappings);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		nextButton.addClickListener(event -> this.showStep(2));

		VerticalLayout layout = new VerticalLayout(header, upload, nextButton);
		layout.setSizeFull();
		layout.setJustifyContentMode(JustifyContentMode.CENTER);
		layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		layout.getStyle().set("text-align", "center");
		return layout;
	}

	private VerticalLayout createStep2() {
		H2 header = new H2("Paso 2: Seleccionar columnas a codificar");

		Button prevButton = new Button("Anterior");
		Button nextButton = new Button("Siguiente");

		grid.addColumn(ColumnMapping::getQuestionVariable).setHeader("Encabezado");
		grid.addColumn(new ComponentRenderer<>(mapping -> {
			Checkbox checkbox = new Checkbox();
			checkbox.setValue(mapping.isToCode());
			checkbox.addValueChangeListener(event -> mapping.setToCode(event.getValue()));
			return checkbox;
		})).setHeader("Codificar");

		grid.addColumn(new ComponentRenderer<>(mapping -> {
			TextArea textField = new TextArea();
			textField.setValue(mapping.getQuestion() != null ? mapping.getQuestion() : "");
			textField.addValueChangeListener(event -> mapping.setQuestion(event.getValue()));
			return textField;
		})).setHeader("Pregunta");

		grid.addColumn(new ComponentRenderer<>(mapping -> {
			TextArea textField = new TextArea();
			textField.setValue(mapping.getFineTuning() != null ? mapping.getFineTuning() : "");
			textField.addValueChangeListener(event -> mapping.setFineTuning(event.getValue()));
			return textField;
		})).setHeader("Fine Tuning ");

		prevButton.addClickListener(event -> showStep(1));
		nextButton.addClickListener(event -> showStep(3));

		VerticalLayout layout = new VerticalLayout(header, grid, new HorizontalLayout(prevButton, nextButton));
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
					String originalName = mapping.getQuestionVariable() != null ? mapping.getQuestionVariable() : "";
					if (!headers.contains(originalName + "-CODIGO")) {
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

		VerticalLayout layout = new VerticalLayout(header, upload, new HorizontalLayout(prevButton, nextButton));
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
				QuestionEncodingAIInput aiInput = new QuestionEncodingAIInput();

				for (ColumnMapping mapping : selected) {
					QuestionAIInput question = new QuestionAIInput();
					question.setQuestion(mapping.getQuestion());
					question.setQuestion_fineTunning(mapping.getFineTuning());

					String columnName = mapping.getQuestionVariable();

					// Extract data for the prompt
					List<String> questionResponses = getColumnData(surveyWorkbook, mapping.getQuestionVariable());
					Integer row = 1;
					for (String answer : questionResponses) {
						QuestionAIAnswer aianswer = new QuestionAIAnswer();
						aianswer.setAnswer(answer);
						aianswer.setResponse_id(row.toString());
						question.getResponses().add(aianswer);
						row++;

					}

					List<String> questionCodes = getColumnData(codeMappingWorkbook, columnName + "-CODIGO");
					for (String questionCode : questionCodes) {
						QuestionAICode qCode = new QuestionAICode();
						qCode.setCode(questionCode);
						question.getCodes().add(qCode);
					}
					aiInput.getQuestions().add(question);

				}
				String json = "hola %s";
				basePrompt.formatted("pepe");

				// ACA CON LA AIINPUUT HAGO UN JSON Y LO PONGO EN EL PROMPT
				// LUEGO PROCESO EL JSON DEL OUTPUY UY L O PONGO EN EL EXCEL DE SALIDA

				System.out.println("PROMPT" + basePrompt);
				String response = chatClient.prompt(basePrompt).call().content();
				System.out.println("RESPONSE" + response);
//				updateSurveyWithCodedResponses(surveyWorkbook, mapping.getOriginalName(), response);
//
//				processed++;
//				int percentage = (int) (((double) processed / total) * 100);
//				dialog.getProgressBar().setValue(percentage / 100.0);
//				dialog.getHeaderComponent().setText("Procesando: " + processed + "/" + total);
			} catch (Exception e) {
				e.printStackTrace();
				Notification.show("Error al procesar los archivos. ");
			}

			dialog.getHeaderComponent().setText("Proceso completado");
			downloadButton.setVisible(true);
			if (surveyWorkbook != null) {
				downloadLink.setHref(createExcelStreamResource(surveyWorkbook));
			}
		});

		prevButton.addClickListener(event -> showStep(3));

		VerticalLayout layout = new VerticalLayout(header,
				new HorizontalLayout(prevButton, processButton, downloadLink));
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
		newHeaderCell.setCellValue(originalColumnName + "_CODIFICADA");

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
						if (cell.getCellType().equals(CellType.NUMERIC))
							data.add(Double.valueOf(cell.getNumericCellValue()).toString());
						else
							data.add(cell.getStringCellValue());
					}
				}
			}
		}
		return data;
	}

	private String buildPrompt(String question, List<String> surveyResponses, List<String> codes) {
		StringBuilder prompt = new StringBuilder();
		prompt.append(basePrompt);
		prompt.append("\n\nPregunta:\n");
		prompt.append(question + "\n");
		prompt.append("\n\nRespuesta a codificar:\n");
		surveyResponses.forEach(response -> prompt.append("- ").append(response).append("\n"));
		prompt.append("\n\nCódigos:\n");
		for (int i = 0; i < codes.size(); i++) {
			prompt.append(codes.get(i)).append("\n");
		}
		return prompt.toString();
	}

	public static class ColumnMapping {
		private final String questionVariable;
		private boolean toCode;
		private String fineTuning;
		private String question;

		public ColumnMapping(String originalName) {
			this.questionVariable = originalName;
		}

		public String getQuestionVariable() {
			return questionVariable;
		}

		public boolean isToCode() {
			return toCode;
		}

		public void setToCode(boolean toCode) {
			this.toCode = toCode;
		}

		public String getFineTuning() {
			return fineTuning;
		}

		public void setFineTuning(String fineTuning) {
			this.fineTuning = fineTuning;
		}

		public String getQuestion() {
			return question;
		}

		public void setQuestion(String question) {
			this.question = question;
		}
	}
}
