package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.JournalEntry;
import uy.com.bay.utiles.data.Operation;

@Service
public class ExcelExportService {

	public ByteArrayInputStream exportJournalEntriesToExcel(List<JournalEntry> journalEntries) throws IOException {
		String[] columns = { "Fecha", "Debe", "Haber", "Descripción", "Encuestador" };
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			CreationHelper createHelper = workbook.getCreationHelper();
			Sheet sheet = workbook.createSheet("Journal Entries");

			Row headerRow = sheet.createRow(0);
			for (int col = 0; col < columns.length; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(columns[col]);
			}

			CellStyle dateCellStyle = workbook.createCellStyle();
			dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

			int rowIdx = 1;
			for (JournalEntry entry : journalEntries) {
				Row row = sheet.createRow(rowIdx++);
				row.createCell(0).setCellValue(entry.getDate());
				row.getCell(0).setCellStyle(dateCellStyle);
				if (entry.getOperation() == Operation.DEBITO) {
					row.createCell(1).setCellValue(entry.getAmount());
					row.createCell(2).setCellValue(0);
				} else {
					row.createCell(1).setCellValue(0);
					row.createCell(2).setCellValue(entry.getAmount());
				}
				row.createCell(3).setCellValue(entry.getDetail());
				if (entry.getSurveyor() != null)
					row.createCell(4).setCellValue(entry.getSurveyor().getFirstName());
			}

			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
