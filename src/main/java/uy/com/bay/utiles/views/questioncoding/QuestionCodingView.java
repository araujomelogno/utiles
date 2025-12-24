package uy.com.bay.utiles.views.questioncoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uy.com.bay.utiles.dto.aiencoding.response.CodedResponse;
import uy.com.bay.utiles.dto.aiencoding.response.Coding;
import uy.com.bay.utiles.dto.aiencoding.response.Question;
import uy.com.bay.utiles.tasks.AlchemerAnswerRetriever;
import uy.com.bay.utiles.views.MainLayout;

@PageTitle("Codificación")
@Route(value = "question-coding", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class QuestionCodingView extends VerticalLayout {
	private static final Logger logger = LoggerFactory.getLogger(QuestionCodingView.class);

	private final VerticalLayout step1;
	private VerticalLayout step2;
	private final VerticalLayout step3;
	private final VerticalLayout step4;
	private List<ColumnMapping> columnMappings;
	private final ChatClient chatClient;
	private byte[] surveyFileContent;
	private byte[] codeMappingFileContent;
	private String fileName;
	private String basePrompt;
	Grid<ColumnMapping> grid;

	public QuestionCodingView(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		try (InputStream inputStream = getClass().getResourceAsStream("/prompts/questionEncoding.txt")) {
			byte[] byteArray = FileCopyUtils.copyToByteArray(inputStream);
			this.basePrompt = new String(byteArray, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				fileName = event.getFileName();
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
					question.setQuestion_text(mapping.getQuestion());
					question.setQuestion_id(mapping.getQuestionVariable());
					question.setQuestion_fineTunning(mapping.getFineTuning());

					String columnName = mapping.getQuestionVariable();

					List<String> questionCodes = getColumnData(codeMappingWorkbook, columnName + "-CODIGO");
					for (String questionCode : questionCodes) {
						QuestionAICode qCode = new QuestionAICode();
						qCode.setCode(questionCode);
						question.getCodes().add(qCode);
					}
					aiInput.getQuestions().add(question);

					// Extract data for the prompt
					List<String> questionResponses = getColumnData(surveyWorkbook, mapping.getQuestionVariable());
					Integer size = questionResponses.size();
					int batchSize = 50;
					int iterCount = size / batchSize + 1;
					for (int i = 0; i < iterCount; i++) {
						Integer row = 1 + i * batchSize;
						for (int j = i * batchSize; j < batchSize * (i + 1) && j < questionResponses.size(); j++) {
							String answer = questionResponses.get(j);
							QuestionAIAnswer aianswer = new QuestionAIAnswer();
							aianswer.setAnswer(answer);
							aianswer.setResponse_id(row.toString());
							question.getResponses().add(aianswer);
							row++;

						}

						/////
						ObjectMapper objectMapper = new ObjectMapper();
						String json = objectMapper.writeValueAsString(aiInput);
						String formattedPrompt = basePrompt.formatted(json);
						System.out.println("PROMPT" + formattedPrompt);
						String response = chatClient.prompt().user(formattedPrompt).call().content()
								.replace("```json", "").replace("```", "");
						logger.info("RESPONSE:\n{}", response);

						System.out.println("RESPONSE" + response);
						this.updateSurveyWithCodedResponses(surveyWorkbook, response);
						// limpio las respuestas
						question.getResponses().clear();

						///

					}
					aiInput.getQuestions().clear();
				}

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
		return new StreamResource(fileName + "_codificada.xlsx", () -> {
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

	private void updateSurveyWithCodedResponses(Workbook workbook, String codedResponses) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			CodedResponse codedResponse = objectMapper.readValue(codedResponses.replace('`', ' '), CodedResponse.class);
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);

			for (Question question : codedResponse.getQuestions()) {
				// Create new headers for code and comment
				int codeColumnIndex = this.getOrCreateColumnIndex(headerRow, question.getQuestionId() + "CODIGO");

				int commentColumnIndex = this.getOrCreateColumnIndex(headerRow,
						question.getQuestionId() + "COMENTARIO");

				for (Coding coding : question.getCodings()) {
					try {
						int responseId = Integer.parseInt(coding.getResponseId());
						Row dataRow = sheet.getRow(responseId);
						if (dataRow == null) {
							dataRow = sheet.createRow(responseId);
						}

						Cell codeCell = dataRow.createCell(codeColumnIndex);
						codeCell.setCellValue(coding.getAssignedCode());

						Cell commentCell = dataRow.createCell(commentColumnIndex);
						commentCell.setCellValue(coding.getComment());
					} catch (NumberFormatException e) {
						System.err.println("Invalid response_id format: " + coding.getResponseId());
					}
				}
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			Notification.show("Error al parsear la respuesta JSON.");
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

	private static int getOrCreateColumnIndex(Row headerRow, String headerName) {

		// 1. Buscar si ya existe
		for (Cell cell : headerRow) {
			if (cell.getCellType() == CellType.STRING
					&& headerName.equalsIgnoreCase(cell.getStringCellValue().trim())) {
				return cell.getColumnIndex();
			}
		}

		// 2. No existe → crear al final
		int newColumnIndex = headerRow.getLastCellNum();
		if (newColumnIndex < 0) {
			newColumnIndex = 0; // fila vacía
		}

		Cell newCell = headerRow.createCell(newColumnIndex);
		newCell.setCellValue(headerName);

		return newColumnIndex;
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
		private String questionVariable = "";
		private boolean toCode;
		private String fineTuning = "";
		private String question = "";

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
