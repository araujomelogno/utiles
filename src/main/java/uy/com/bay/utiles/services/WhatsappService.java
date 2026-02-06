package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.TaskType;
import uy.com.bay.utiles.data.WhatsappFlowTask;
import uy.com.bay.utiles.data.repository.WhatsappFlowTaskRepository;
import uy.com.bay.utiles.dto.whatsapp.WhatsappButton;
import uy.com.bay.utiles.dto.whatsapp.WhatsappComponent;
import uy.com.bay.utiles.dto.whatsapp.WhatsappTemplate;
import uy.com.bay.utiles.dto.whatsapp.WhatsappTemplatesResponse;

@Service
public class WhatsappService {

	@Value("${whatsapp.business.account.id}")
	private String accountId;

	@Value("${whatsapp.api.token}")
	private String apiToken;

	private final RestClient restClient;
	private final WhatsappFlowTaskRepository taskRepository;
	private final ObjectMapper objectMapper;

	@Autowired
	public WhatsappService(WhatsappFlowTaskRepository taskRepository, ObjectMapper objectMapper) {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
		converter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, new MediaType("text", "javascript")));
		this.restClient = RestClient.builder().messageConverters(converters -> converters.add(converter)).build();
		this.taskRepository = taskRepository;
		this.objectMapper = objectMapper;
	}

	public List<WhatsappTemplate> getTemplates() {
		if (accountId == null || accountId.isEmpty() || apiToken == null || apiToken.isEmpty()) {
			return Collections.emptyList();
		}

		String url = "https://graph.facebook.com/v19.0/" + accountId + "/message_templates";

		try {
			WhatsappTemplatesResponse response = restClient.get().uri(url).header("Authorization", "Bearer " + apiToken)
					.retrieve().body(WhatsappTemplatesResponse.class);

			if (response != null && response.getData() != null) {
				return response.getData().stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus()))
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// In a real app, log this using a logger
		}

		return Collections.emptyList();
	}

	@Transactional
	public InputStream processExcel(InputStream fileContent, boolean skipHeader, WhatsappTemplate selectedTemplate,
			Date schedule) throws IOException {
		Workbook workbook = new XSSFWorkbook(fileContent);
		Sheet sheet = workbook.getSheetAt(0);

		// Find the maximum column count to ensure we write the status in a consistent
		// new column if desired,
		// or just append to each row. The prompt implies appending to the row.
		// Let's just append to the end of each row.

		for (Row row : sheet) {
			if (row.getRowNum() == 0 && skipHeader) {
				continue;
			}

			Cell firstCell = row.getCell(0);
			String phoneNumber = getCellValueAsString(firstCell);

			boolean isValid = false;
			if (phoneNumber != null && phoneNumber.startsWith("598") && phoneNumber.length() == 11) {
				isValid = true;
			}

			// Determine where to write the status.
			// We want to avoid overwriting data if the row is sparse.
			// But usually we append.
			int lastCellNum = row.getLastCellNum();
			int resultCellIndex = lastCellNum < 0 ? 0 : lastCellNum;

			if (isValid) {
				List<String> parameters = new ArrayList<>();
				// Iterate from 2nd column (index 1) to end
				for (int i = 1; i < resultCellIndex; i++) {
					Cell cell = row.getCell(i);
					String paramValue = getCellValueAsString(cell);
					if (paramValue != null && !paramValue.trim().isEmpty()) {
						parameters.add(paramValue);
					}
				}

				WhatsappFlowTask task = new WhatsappFlowTask();
				task.setCreated(new Date());
				task.setStatus(Status.PENDING);
				task.setSchedule(schedule);
				task.setTo(phoneNumber);
				task.setParameters(parameters);
				task.setTemplateName(selectedTemplate.getName());
				task.setFirstScreenName(selectedTemplate.getNavigateScreen());
				task.setLanguage(selectedTemplate.getLanguage());

				boolean hasHeaderParameter = false;
				boolean hasBodyParameters = false;
				boolean hasUrlParameter = false;
				TaskType type = TaskType.PLAINTEXT;

				if (selectedTemplate.getComponents() != null) {
					for (WhatsappComponent component : selectedTemplate.getComponents()) {
						if ("HEADER".equalsIgnoreCase(component.getType()) && component.getText() != null
								&& component.getText().contains("{{")) {
							hasHeaderParameter = true;
						}
						if ("BODY".equalsIgnoreCase(component.getType()) && component.getText() != null
								&& component.getText().contains("{{")) {
							hasBodyParameters = true;
						}
						if ("BUTTONS".equalsIgnoreCase(component.getType())) {
							if (component.getButtons() != null) {
								for (WhatsappButton button : component.getButtons()) {
									if (button.getUrl() != null && button.getUrl().contains("{{")) {
										hasUrlParameter = true;
										break;
									}
								}

								if (!component.getButtons().isEmpty()) {
									WhatsappButton firstButton = component.getButtons().get(0);
									if ("FLOW".equalsIgnoreCase(firstButton.getType())) {
										type = TaskType.FLOW;
									} else if ("URL".equalsIgnoreCase(firstButton.getType())) {
										type = TaskType.URL;
									}
								}
							}
						}
					}
				}

				task.setHasHeaderParameter(hasHeaderParameter);
				task.setHasBodyParameters(hasBodyParameters);
				task.setHasUrlParameter(hasUrlParameter);
				task.setType(type);

				if (hasHeaderParameter && !parameters.isEmpty()) {
					String headerParam = parameters.remove(0);
					task.setHeaderParameter(headerParam);
				}

				if (hasUrlParameter && !parameters.isEmpty()) {
					String urlParam = parameters.remove(parameters.size() - 1);
					task.setUrlParameter(urlParam);
				}

				task.setParameters(parameters);

				// Construct JSON input
				String inputJson = constructInputJson(task);
				task.setInput(inputJson);

				taskRepository.save(task);

				Cell resultCell = row.createCell(resultCellIndex, CellType.STRING);
				resultCell.setCellValue("procesado");

			} else {
				Cell resultCell = row.createCell(resultCellIndex, CellType.STRING);
				resultCell.setCellValue("No Procesado - Con error");
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();

		return new ByteArrayInputStream(out.toByteArray());
	}

	private String getCellValueAsString(Cell cell) {
		if (cell == null)
			return null;
		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue();
		case NUMERIC:
			long longVal = (long) cell.getNumericCellValue();
			return String.valueOf(longVal);
		default:
			return null;
		}
	}

	private String constructInputJson(WhatsappFlowTask task) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("messaging_product", "whatsapp");
		root.put("to", task.getTo());
		root.put("type", "template");

		ObjectNode template = root.putObject("template");
		template.put("name", task.getTemplateName());

		ObjectNode language = template.putObject("language");
		language.put("code", task.getLanguage());

		ArrayNode components = template.putArray("components");

		// Header
		if (task.isHasHeaderParameter()) {
			ObjectNode headerComponent = components.addObject();
			headerComponent.put("type", "header");
			ArrayNode headerParams = headerComponent.putArray("parameters");
			ObjectNode paramObj = headerParams.addObject();
			paramObj.put("type", "text");
			paramObj.put("text", task.getHeaderParameter() != null ? task.getHeaderParameter() : "");
		}

		// Body
		if (task.isHasBodyParameters()) {
			ObjectNode bodyComponent = components.addObject();
			bodyComponent.put("type", "body");
			ArrayNode bodyParams = bodyComponent.putArray("parameters");
			if (task.getParameters() != null) {
				for (String param : task.getParameters()) {
					ObjectNode paramObj = bodyParams.addObject();
					paramObj.put("type", "text");
					paramObj.put("text", param);
				}
			}
		}

		if (task.getType() == TaskType.FLOW) {
			ObjectNode buttonComponent = components.addObject();
			buttonComponent.put("type", "button");
			buttonComponent.put("sub_type", "flow");
			buttonComponent.put("index", "0");

			ArrayNode buttonParams = buttonComponent.putArray("parameters");
			ObjectNode actionParam = buttonParams.addObject();
			actionParam.put("type", "action");
			ObjectNode action = actionParam.putObject("action");

			// Random number for rid
			long rid = System.currentTimeMillis();
			String flowToken = "tpl=" + task.getTemplateName() + "|rid=" + rid;
			action.put("flow_token", flowToken);

			ObjectNode flowActionData = action.putObject("flow_action_data");
			flowActionData.put("screen", task.getFirstScreenName());
		} else if (task.getType() == TaskType.URL) {
			ObjectNode buttonComponent = components.addObject();
			buttonComponent.put("type", "button");
			buttonComponent.put("sub_type", "url");
			buttonComponent.put("index", "0");

			ArrayNode buttonParams = buttonComponent.putArray("parameters");
			if (Boolean.TRUE.equals(task.getHasUrlParameter())) {
				ObjectNode paramObj = buttonParams.addObject();
				paramObj.put("type", "text");
				paramObj.put("text", task.getUrlParameter() != null ? task.getUrlParameter() : "");
			}
		}

		try {
			return objectMapper.writeValueAsString(root);
		} catch (Exception e) {
			e.printStackTrace();
			return "{}";
		}
	}

	public List<WhatsappFlowTask> getTasksByDateRange(LocalDateTime from, LocalDateTime to) {
		if (from == null || to == null) {
			return Collections.emptyList();
		}
		Date fromDate = Date.from(from.atZone(ZoneId.systemDefault()).toInstant());
		Date toDate = Date.from(to.atZone(ZoneId.systemDefault()).toInstant());
		return taskRepository.findByScheduleBetweenOrderByCreatedDesc(fromDate, toDate);
	}
}
