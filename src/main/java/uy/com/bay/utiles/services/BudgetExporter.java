package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.ExpenseRequest;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;
import uy.com.bay.utiles.entities.Extra;

@Service
public class BudgetExporter {

	public InputStream export(Study study) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Presupuesto");

		// Header
		Row headerRow = sheet.createRow(0);
		String[] headers = { "Tipo", "Concepto", "Encuestador", "Obs", "Cantidad", "Costo U.", "Completadas", "TOTAL" };
		CellStyle headerStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		headerStyle.setFont(font);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowNum = 1;
		double totalSum = 0;

		for (BudgetEntry entry : study.getBudget().getEntries()) {
			Row entryRow = sheet.createRow(rowNum++);
			entryRow.createCell(0).setCellValue("Presupuestado");
			entryRow.createCell(1).setCellValue(entry.getConcept().getName());
			entryRow.createCell(4).setCellValue(entry.getQuantity());
			entryRow.createCell(5).setCellValue(entry.getAmmount());
			entryRow.createCell(7).setCellValue(entry.getTotal());
			totalSum += entry.getTotal();

			for (Extra extra : entry.getExtras()) {
				Row extraRow = sheet.createRow(rowNum++);
				extraRow.createCell(0).setCellValue("Gastado");
				extraRow.createCell(1).setCellValue(extra.getConcept().getDescription());
				if (extra.getSurveyor() != null) {
					extraRow.createCell(2).setCellValue(extra.getSurveyor().getName());
				}
				extraRow.createCell(4).setCellValue(extra.getQuantity());
				extraRow.createCell(5).setCellValue(extra.getUnitPrice());
				double total = extra.getQuantity() * extra.getUnitPrice();
				extraRow.createCell(7).setCellValue(-total);
				totalSum -= total;
			}

			for (ExpenseRequest expense : entry.getExpenseRequests()) {
				Row expenseRow = sheet.createRow(rowNum++);
				expenseRow.createCell(0).setCellValue("Gastado");
				expenseRow.createCell(1).setCellValue(expense.getConcept().getName());
				if (expense.getSurveyor() != null) {
					expenseRow.createCell(2).setCellValue(expense.getSurveyor().getName());
				}
				expenseRow.createCell(3).setCellValue(expense.getObs());
				expenseRow.createCell(7).setCellValue(-expense.getAmount());
				totalSum -= expense.getAmount();
			}

			for (Fieldwork fieldwork : entry.getFieldworks()) {
				Row fieldworkRow = sheet.createRow(rowNum++);
				fieldworkRow.createCell(0).setCellValue("Gastado");
				fieldworkRow.createCell(1).setCellValue(fieldwork.getType().name());
				fieldworkRow.createCell(3).setCellValue(fieldwork.getObs());
				fieldworkRow.createCell(5).setCellValue(
						Objects.nonNull(fieldwork.getUnitCost()) ? fieldwork.getUnitCost().doubleValue() : 0);

				fieldworkRow.createCell(6)
						.setCellValue(Objects.nonNull(fieldwork.getCompleted()) ? fieldwork.getCompleted() : 0);
				double total = (Objects.nonNull(fieldwork.getUnitCost()) ? fieldwork.getUnitCost().doubleValue() : 0)
						* (Objects.nonNull(fieldwork.getCompleted()) ? fieldwork.getCompleted() : 0);
				fieldworkRow.createCell(7).setCellValue(-total);
				totalSum -= total;
			}
		}

		// Footer
		Row footerRow = sheet.createRow(rowNum);
		footerRow.createCell(6).setCellValue("Total:");
		footerRow.createCell(7).setCellValue(totalSum);
		CellStyle footerStyle = workbook.createCellStyle();
		footerStyle.setFont(font);
		footerRow.getCell(6).setCellStyle(footerStyle);
		footerRow.getCell(7).setCellStyle(footerStyle);
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return new ByteArrayInputStream(out.toByteArray());
	}
}