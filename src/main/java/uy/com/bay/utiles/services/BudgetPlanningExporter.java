package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.BudgetEntry;

@Service
public class BudgetPlanningExporter {

	private static final Locale ES_LOCALE = new Locale("es", "UY");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public InputStream export(List<BudgetEntry> entries, LocalDate fechaDesde, LocalDate fechaHasta,
			List<Study> selectedStudies) throws IOException {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Planificación presupuestal");

		Font boldFont = workbook.createFont();
		boldFont.setBold(true);

		CellStyle labelStyle = workbook.createCellStyle();
		labelStyle.setFont(boldFont);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(boldFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		int rowIndex = 0;

		Row reporteRow = sheet.createRow(rowIndex++);
		Cell reporteLabel = reporteRow.createCell(0);
		reporteLabel.setCellValue("Reporte:");
		reporteLabel.setCellStyle(labelStyle);
		reporteRow.createCell(1).setCellValue("Planificación presupuestal");

		Row creadoRow = sheet.createRow(rowIndex++);
		Cell creadoLabel = creadoRow.createCell(0);
		creadoLabel.setCellValue("Creado:");
		creadoLabel.setCellStyle(labelStyle);
		creadoRow.createCell(1).setCellValue(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

		Row estudioRow = sheet.createRow(rowIndex++);
		Cell estudioLabel = estudioRow.createCell(0);
		estudioLabel.setCellValue("Estudio:");
		estudioLabel.setCellStyle(labelStyle);
		String estudiosTexto = (selectedStudies == null || selectedStudies.isEmpty()) ? "Todos"
				: String.join(", ", selectedStudies.stream().map(Study::getName).toList());
		estudioRow.createCell(1).setCellValue(estudiosTexto);

		Row fechaInicioRow = sheet.createRow(rowIndex++);
		Cell fechaInicioLabel = fechaInicioRow.createCell(0);
		fechaInicioLabel.setCellValue("Fecha Inicio :");
		fechaInicioLabel.setCellStyle(labelStyle);
		fechaInicioRow.createCell(1).setCellValue(fechaDesde != null ? fechaDesde.format(DATE_FORMAT) : "");

		Row fechaFinRow = sheet.createRow(rowIndex++);
		Cell fechaFinLabel = fechaFinRow.createCell(0);
		fechaFinLabel.setCellValue("Fecha Fin:");
		fechaFinLabel.setCellStyle(labelStyle);
		fechaFinRow.createCell(1).setCellValue(fechaHasta != null ? fechaHasta.format(DATE_FORMAT) : "");

		rowIndex++;

		List<YearMonth> months = monthsBetween(fechaDesde, fechaHasta);

		List<String> headers = new ArrayList<>();
		headers.add("Estudio");
		headers.add("Concepto presupuesto");
		headers.add("Tipo");
		headers.add("Total");
		for (YearMonth ym : months) {
			String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, ES_LOCALE);
			monthName = monthName.substring(0, 1).toUpperCase(ES_LOCALE) + monthName.substring(1);
			headers.add(monthName + "-" + ym.getYear());
		}

		Row headerRow = sheet.createRow(rowIndex++);
		for (int i = 0; i < headers.size(); i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers.get(i));
			cell.setCellStyle(headerStyle);
		}

		List<BudgetEntry> sorted = new ArrayList<>(entries);
		sorted.sort(Comparator
				.comparing((BudgetEntry be) -> studyName(be), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
				.thenComparing(BudgetPlanningExporter::conceptName,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
				.thenComparing(BudgetPlanningExporter::tipo,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

		for (BudgetEntry entry : sorted) {
			Row row = sheet.createRow(rowIndex++);
			row.createCell(0).setCellValue(studyName(entry));
			row.createCell(1).setCellValue(conceptName(entry));
			row.createCell(2).setCellValue(tipo(entry));
			row.createCell(3).setCellValue(entry.getTotal() != null ? entry.getTotal() : 0d);

			double[] distribution = distribute(entry, months);
			for (int i = 0; i < months.size(); i++) {
				row.createCell(4 + i).setCellValue(distribution[i]);
			}
		}

		for (int i = 0; i < headers.size(); i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private static String studyName(BudgetEntry entry) {
		if (entry.getBudget() != null && entry.getBudget().getStudy() != null) {
			return entry.getBudget().getStudy().getName();
		}
		return "";
	}

	private static String conceptName(BudgetEntry entry) {
		if (entry.getConcept() != null && entry.getConcept().getName() != null) {
			return entry.getConcept().getName();
		}
		return "";
	}

	private static String tipo(BudgetEntry entry) {
		if (entry.getConcept() != null && entry.getConcept().isSalaryCost()) {
			return "Costo Salarial";
		}
		return "Otros costos";
	}

	private static List<YearMonth> monthsBetween(LocalDate fechaDesde, LocalDate fechaHasta) {
		List<YearMonth> months = new ArrayList<>();
		if (fechaDesde == null || fechaHasta == null || fechaHasta.isBefore(fechaDesde)) {
			return months;
		}
		YearMonth current = YearMonth.from(fechaDesde);
		YearMonth last = YearMonth.from(fechaHasta);
		while (!current.isAfter(last)) {
			months.add(current);
			current = current.plusMonths(1);
		}
		return months;
	}

	private double[] distribute(BudgetEntry entry, List<YearMonth> months) {
		double[] result = new double[months.size()];
		if (months.isEmpty()) {
			return result;
		}
		Double total = entry.getTotal();
		if (total == null || total == 0d) {
			return result;
		}
		LocalDate init = entry.getInit();
		LocalDate end = entry.getEnd();
		if (init == null || end == null || end.isBefore(init)) {
			return result;
		}

		YearMonth initYm = YearMonth.from(init);
		YearMonth endYm = YearMonth.from(end);

		int spannedCount = 0;
		YearMonth cursor = initYm;
		while (!cursor.isAfter(endYm)) {
			spannedCount++;
			cursor = cursor.plusMonths(1);
		}
		if (spannedCount == 0) {
			return result;
		}

		double perMonth = total / spannedCount;
		for (int i = 0; i < months.size(); i++) {
			YearMonth ym = months.get(i);
			if (!ym.isBefore(initYm) && !ym.isAfter(endYm)) {
				result[i] = perMonth;
			}
		}
		return result;
	}
}
