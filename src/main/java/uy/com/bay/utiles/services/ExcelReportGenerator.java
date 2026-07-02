package uy.com.bay.utiles.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.JournalEntryReportDTO;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExcelReportGenerator {

    /**
     * Builds the expenses report from lightweight {@link JournalEntryReportDTO}
     * projections instead of full {@link JournalEntry} entities.
     *
     * <p>
     * Using the projection avoids eagerly loading the attachment collections
     * (which carry {@code @Lob} binary content) of every entry, which previously
     * caused {@link OutOfMemoryError} when a large number of entries was exported.
     */
    public static ByteArrayOutputStream generateReport(List<JournalEntryReportDTO> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Reporte de Gastos");

            String[] headers = { "JournalEntry.detail", "JournalEntry.obs", "JournalEntry.date",
                    "JournalEntry.operation", "JournalEntry.amount", "JournalEntry.source", "Study.name",
                    "Study.odooId", "Study.obs", "Study.clientName", "Study.area", "Study.totalReportedCost",
                    "Study.totalTransfered", "Study.expectedRevenue", "Surveyor.firstName", "Surveyor.lastName",
                    "Surveyor.login", "Surveyor.ci", "Surveyor.surveyToGoId", "Surveyor.balance" };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            int rowNum = 1;
            for (JournalEntryReportDTO dto : rows) {
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;
                cellNum = writeString(row, cellNum, dto.getDetail());
                cellNum = writeString(row, cellNum, dto.getObs());
                cellNum = writeDate(row, cellNum, dto.getDate(), dateCellStyle);
                cellNum = writeString(row, cellNum, dto.getOperation() != null ? dto.getOperation().toString() : null);
                cellNum = writeNumber(row, cellNum, dto.getAmount());
                cellNum = writeString(row, cellNum, dto.getSource() != null ? dto.getSource().toString() : null);
                cellNum = writeString(row, cellNum, dto.getStudyName());
                cellNum = writeString(row, cellNum, dto.getStudyOdooId());
                cellNum = writeString(row, cellNum, dto.getStudyObs());
                cellNum = writeString(row, cellNum, dto.getStudyClientName());
                cellNum = writeString(row, cellNum, dto.getStudyArea());
                cellNum = writeNumber(row, cellNum, dto.getStudyTotalReportedCost());
                cellNum = writeNumber(row, cellNum, dto.getStudyTotalTransfered());
                cellNum = writeNumber(row, cellNum, dto.getStudyExpectedRevenue());
                cellNum = writeString(row, cellNum, dto.getSurveyorFirstName());
                cellNum = writeString(row, cellNum, dto.getSurveyorLastName());
                cellNum = writeString(row, cellNum, dto.getSurveyorLogin());
                cellNum = writeString(row, cellNum, dto.getSurveyorCi());
                cellNum = writeString(row, cellNum, dto.getSurveyorSurveyToGoId());
                cellNum = writeNumber(row, cellNum, dto.getSurveyorBalance());
            }

            workbook.write(out);
            return out;
        }
    }

    private static int writeString(Row row, int cellNum, String value) {
        row.createCell(cellNum++).setCellValue(value != null ? value : "");
        return cellNum;
    }

    private static int writeNumber(Row row, int cellNum, Double value) {
        Cell cell = row.createCell(cellNum++);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        return cellNum;
    }

    private static int writeDate(Row row, int cellNum, Date value, CellStyle dateCellStyle) {
        Cell cell = row.createCell(cellNum++);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(dateCellStyle);
        } else {
            cell.setCellValue("");
        }
        return cellNum;
    }

    public static ByteArrayOutputStream generateExcel(List<JournalEntry> journalEntries) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Reporte de Gastos");

            List<String> headers = new ArrayList<>();
            List<Field> journalEntryFields = getFields(JournalEntry.class);
            for (Field field : journalEntryFields) {
                headers.add("JournalEntry." + field.getName());
            }
            List<Field> studyFields = getFields(Study.class);
            for (Field field : studyFields) {
                headers.add("Study." + field.getName());
            }
            List<Field> surveyorFields = getFields(Surveyor.class);
            for (Field field : surveyorFields) {
                headers.add("Surveyor." + field.getName());
            }

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            int rowNum = 1;
            for (JournalEntry entry : journalEntries) {
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;

                for (Field field : journalEntryFields) {
                    cellNum = writeFieldToCell(entry, field, row, cellNum, dateCellStyle);
                }
                for (Field field : studyFields) {
                    cellNum = writeFieldToCell(entry.getStudy(), field, row, cellNum, dateCellStyle);
                }
                for (Field field : surveyorFields) {
                    cellNum = writeFieldToCell(entry.getSurveyor(), field, row, cellNum, dateCellStyle);
                }
            }

            workbook.write(out);
            return out;
        }
    }

    public static ByteArrayOutputStream generateSurveyorExcel(List<Surveyor> surveyors,
            Map<Long, Double> transferSumsBySurveyorId) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Encuestadores");

            List<Field> surveyorFields = getFields(Surveyor.class);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < surveyorFields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(surveyorFields.get(i).getName());
            }

            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            int rowNum = 1;
            for (Surveyor surveyor : surveyors) {
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;
                for (Field field : surveyorFields) {
                    if ("balance".equals(field.getName())) {
                        double transferSum = 0.0;
                        if (transferSumsBySurveyorId != null && surveyor.getId() != null) {
                            transferSum = transferSumsBySurveyorId.getOrDefault(surveyor.getId(), 0.0);
                        }
                        Cell cell = row.createCell(cellNum++);
                        cell.setCellValue(surveyor.getBalance() - transferSum);
                    } else {
                        cellNum = writeFieldToCell(surveyor, field, row, cellNum, dateCellStyle);
                    }
                }
            }

            workbook.write(out);
            return out;
        }
    }

    private static List<Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private static int writeFieldToCell(Object entity, Field field, Row row, int cellNum, CellStyle dateCellStyle) {
        Cell cell = row.createCell(cellNum++);
        if (entity == null) {
            cell.setCellValue("");
            return cellNum;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value instanceof Date) {
                cell.setCellValue((Date) value);
                cell.setCellStyle(dateCellStyle);
            } else if (value != null) {
                cell.setCellValue(value.toString());
            } else {
                cell.setCellValue("");
            }
        } catch (IllegalAccessException e) {
            cell.setCellValue("Error");
        }
        return cellNum;
    }
}
