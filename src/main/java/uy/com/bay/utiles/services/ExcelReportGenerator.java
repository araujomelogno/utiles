package uy.com.bay.utiles.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uy.com.bay.utiles.data.JournalEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelReportGenerator {

    public static ByteArrayOutputStream generateExcel(List<JournalEntry> journalEntries) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Reporte de Gastos");

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Fecha", "Encuestador", "Estudio", "Monto"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Date cell style
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            // Data rows
            int rowNum = 1;
            for (JournalEntry entry : journalEntries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getDate());
                row.getCell(0).setCellStyle(dateCellStyle);
                row.createCell(1).setCellValue(entry.getSurveyor() != null ? entry.getSurveyor().getFirstName() : "");
                row.createCell(2).setCellValue(entry.getStudy() != null ? entry.getStudy().getName() : "");
                row.createCell(3).setCellValue(entry.getAmount());
            }

            workbook.write(out);
            return out;
        }
    }
}
