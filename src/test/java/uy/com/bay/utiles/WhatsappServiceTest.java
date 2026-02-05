package uy.com.bay.utiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uy.com.bay.utiles.data.TaskType;
import uy.com.bay.utiles.data.WhatsappFlowTask;
import uy.com.bay.utiles.data.repository.WhatsappFlowTaskRepository;
import uy.com.bay.utiles.dto.whatsapp.WhatsappButton;
import uy.com.bay.utiles.dto.whatsapp.WhatsappComponent;
import uy.com.bay.utiles.dto.whatsapp.WhatsappTemplate;
import uy.com.bay.utiles.services.WhatsappService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WhatsappServiceTest {

    @Test
    public void testFlowTemplate() throws Exception {
        // Setup Template
        WhatsappTemplate template = new WhatsappTemplate();
        template.setName("test_flow");
        template.setLanguage("es");
        template.setComponents(new ArrayList<>());
        template.setId("123");

        WhatsappComponent header = new WhatsappComponent();
        header.setType("HEADER");
        header.setText("Hello {{1}}");
        template.getComponents().add(header);

        WhatsappComponent body = new WhatsappComponent();
        body.setType("BODY");
        body.setText("Body {{1}}");
        template.getComponents().add(body);

        WhatsappComponent buttons = new WhatsappComponent();
        buttons.setType("BUTTONS");
        WhatsappButton flowBtn = new WhatsappButton();
        flowBtn.setType("FLOW");
        buttons.setButtons(Collections.singletonList(flowBtn));
        template.getComponents().add(buttons);

        // Setup Excel
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0); // Header
        row.createCell(0).setCellValue("Phone");
        row.createCell(1).setCellValue("HeaderParam");
        row.createCell(2).setCellValue("BodyParam");

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("59899123456");
        row1.createCell(1).setCellValue("HVal");
        row1.createCell(2).setCellValue("BVal");

        workbook.write(bos);
        ByteArrayInputStream excelIs = new ByteArrayInputStream(bos.toByteArray());

        // Setup Service
        WhatsappFlowTaskRepository repo = mock(WhatsappFlowTaskRepository.class);
        WhatsappService service = new WhatsappService(repo, new ObjectMapper());

        service.processExcel(excelIs, true, template, new Date());

        ArgumentCaptor<WhatsappFlowTask> captor = ArgumentCaptor.forClass(WhatsappFlowTask.class);
        verify(repo).save(captor.capture());

        WhatsappFlowTask task = captor.getValue();

        assertEquals(TaskType.FLOW, task.getType());
        assertTrue(task.isHasHeaderParameter());
        assertTrue(task.isHasBodyParameters());
        assertEquals("HVal", task.getHeaderParameter());
        assertEquals(1, task.getParameters().size());
        assertEquals("BVal", task.getParameters().get(0));

        String input = task.getInput();
        assertTrue(input.contains("\"type\":\"header\""));
        assertTrue(input.contains("\"sub_type\":\"flow\""));
    }

    @Test
    public void testUrlTemplate() throws Exception {
        // Setup Template
        WhatsappTemplate template = new WhatsappTemplate();
        template.setName("test_url");
        template.setLanguage("es");
        template.setComponents(new ArrayList<>());

        WhatsappComponent buttons = new WhatsappComponent();
        buttons.setType("BUTTONS");
        WhatsappButton urlBtn = new WhatsappButton();
        urlBtn.setType("URL");
        urlBtn.setUrl("https://example.com/{{1}}");
        buttons.setButtons(Collections.singletonList(urlBtn));
        template.getComponents().add(buttons);

        // Setup Excel
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0); // Header
        row.createCell(0).setCellValue("Phone");
        row.createCell(1).setCellValue("UrlParam");

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("59899123456");
        row1.createCell(1).setCellValue("UVal");

        workbook.write(bos);
        ByteArrayInputStream excelIs = new ByteArrayInputStream(bos.toByteArray());

        // Setup Service
        WhatsappFlowTaskRepository repo = mock(WhatsappFlowTaskRepository.class);
        WhatsappService service = new WhatsappService(repo, new ObjectMapper());

        service.processExcel(excelIs, true, template, new Date());

        ArgumentCaptor<WhatsappFlowTask> captor = ArgumentCaptor.forClass(WhatsappFlowTask.class);
        verify(repo).save(captor.capture());

        WhatsappFlowTask task = captor.getValue();

        assertEquals(TaskType.URL, task.getType());
        assertEquals(Boolean.TRUE, task.getHasUrlParameter());
        assertEquals("UVal", task.getUrlParameter());

        String input = task.getInput();
        assertTrue(input.contains("\"sub_type\":\"url\""));
        assertTrue(input.contains("UVal"));
    }
}
