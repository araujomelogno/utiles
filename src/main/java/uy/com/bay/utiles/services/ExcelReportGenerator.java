package uy.com.bay.utiles.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExcelReportGenerator {

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
