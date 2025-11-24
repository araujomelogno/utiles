package uy.com.bay.utiles.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uy.com.bay.utiles.dto.CompletedSurveyDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {

    public static ByteArrayInputStream exportToExcel(List<CompletedSurveyDTO> data) throws IOException {
        String[] columns = {"Encuestador", "Estudio", "Fecha", "Cantidad"};
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Encuestas Completas");

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
            }

            int rowIdx = 1;
            for (CompletedSurveyDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getSurveyor());
                row.createCell(1).setCellValue(dto.getStudyName());
                row.createCell(2).setCellValue(dto.getCreated().format(dateFormatter));
                row.createCell(3).setCellValue(dto.getCount());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
