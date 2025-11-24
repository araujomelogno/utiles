package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.BuiltinFormats;
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
		String[] headers = { "Tipo", "Concepto", "Encuestador", "Obs", "Cantidad", "Costo U.", "Completadas", "Fecha",
				"Total Presupuestado", "Total Gastado" };
		CellStyle headerStyle = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		headerStyle.setFont(font);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		CellStyle dateStyle = workbook.createCellStyle();
		dateStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("d-mmm-yy"));

		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowNum = 1;
		double totalBudgetSum = 0;
		double totalSpentSum = 0;

		for (BudgetEntry entry : study.getBudget().getEntries()) {
			Row entryRow = sheet.createRow(rowNum++);
			entryRow.createCell(0).setCellValue("Presupuestado");
			entryRow.createCell(1).setCellValue(entry.getConcept().getName());
			entryRow.createCell(4).setCellValue(entry.getQuantity());
			entryRow.createCell(5).setCellValue(entry.getAmmount());
			entryRow.createCell(7).setCellValue(entry.getCreated());
			entryRow.getCell(7).setCellStyle(dateStyle);
			entryRow.createCell(8).setCellValue(entry.getTotal());
			totalBudgetSum += entry.getTotal();

			for (Extra extra : entry.getExtras()) {
				Row extraRow = sheet.createRow(rowNum++);
				extraRow.createCell(0).setCellValue("Extras");
				extraRow.createCell(1).setCellValue(extra.getConcept().getDescription());
				if (extra.getSurveyor() != null) {
					extraRow.createCell(2).setCellValue(extra.getSurveyor().getName());
				}
				extraRow.createCell(4).setCellValue(extra.getQuantity());
				extraRow.createCell(5).setCellValue(extra.getUnitPrice());
				double total = extra.getQuantity() * extra.getUnitPrice();
				extraRow.createCell(7).setCellValue(extra.getDate());
				extraRow.getCell(7).setCellStyle(dateStyle);
				extraRow.createCell(9).setCellValue(-total);

				totalSpentSum -= total;
			}

			for (ExpenseRequest expense : entry.getExpenseRequests()) {
				Row expenseRow = sheet.createRow(rowNum++);
				expenseRow.createCell(0).setCellValue("Gastos");
				expenseRow.createCell(1).setCellValue(expense.getConcept().getName());
				if (expense.getSurveyor() != null) {
					expenseRow.createCell(2).setCellValue(expense.getSurveyor().getName());
				}
				expenseRow.createCell(3).setCellValue(expense.getObs());
				expenseRow.createCell(7).setCellValue(expense.getTransferDate());
				expenseRow.getCell(7).setCellStyle(dateStyle);
				expenseRow.createCell(9).setCellValue(-expense.getAmount());
				totalSpentSum -= expense.getAmount();
			}

			for (Fieldwork fieldwork : entry.getFieldworks()) {
				Row fieldworkRow = sheet.createRow(rowNum++);
				fieldworkRow.createCell(0).setCellValue("Campo");
				fieldworkRow.createCell(1).setCellValue(fieldwork.getType().name());
				fieldworkRow.createCell(3).setCellValue(fieldwork.getObs());
				fieldworkRow.createCell(5).setCellValue(
						Objects.nonNull(fieldwork.getUnitCost()) ? fieldwork.getUnitCost().doubleValue() : 0);

				fieldworkRow.createCell(6)
						.setCellValue(Objects.nonNull(fieldwork.getCompleted()) ? fieldwork.getCompleted() : 0);
				double total = (Objects.nonNull(fieldwork.getUnitCost()) ? fieldwork.getUnitCost().doubleValue() : 0)
						* (Objects.nonNull(fieldwork.getCompleted()) ? fieldwork.getCompleted() : 0);
				fieldworkRow.createCell(7).setCellValue(fieldwork.getInitDate());
				fieldworkRow.getCell(7).setCellStyle(dateStyle);
				fieldworkRow.createCell(9).setCellValue(-total);
				totalSpentSum -= total;
			}
		}

		// Footer
		Row footerRow = sheet.createRow(rowNum);
		footerRow.createCell(7).setCellValue("Total:");
		footerRow.createCell(8).setCellValue(totalBudgetSum);
		footerRow.createCell(9).setCellValue(totalSpentSum);
		footerRow.createCell(10).setCellValue(totalSpentSum + totalBudgetSum);
		CellStyle footerStyle = workbook.createCellStyle();
		footerStyle.setFont(font);

		footerRow.getCell(7).setCellStyle(footerStyle);
		footerRow.getCell(8).setCellStyle(footerStyle);
		footerRow.getCell(9).setCellStyle(footerStyle);
		footerRow.getCell(10).setCellStyle(footerStyle);
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return new ByteArrayInputStream(out.toByteArray());
	}
}