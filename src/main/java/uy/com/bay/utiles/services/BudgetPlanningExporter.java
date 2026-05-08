package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

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
import uy.com.bay.utiles.entities.OdooCost;

@Service
public class BudgetPlanningExporter {

	private static final Locale ES_LOCALE = new Locale("es", "UY");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public InputStream export(List<BudgetEntry> entries, LocalDate fechaDesde, LocalDate fechaHasta,
			List<Study> selectedStudies, boolean totalizarConceptos) throws IOException {
		Workbook workbook = new XSSFWorkbook();

		Font boldFont = workbook.createFont();
		boldFont.setBold(true);

		CellStyle labelStyle = workbook.createCellStyle();
		labelStyle.setFont(boldFont);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(boldFont);
		headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		List<YearMonth> months = monthsBetween(fechaDesde, fechaHasta);

		writeSheet(workbook, "Planificación presupuestal", entries, fechaDesde, fechaHasta, selectedStudies,
				totalizarConceptos, months, labelStyle, headerStyle, this::distribute);
		writeSheet(workbook, "Ejecución presupuestal", entries, fechaDesde, fechaHasta, selectedStudies,
				totalizarConceptos, months, labelStyle, headerStyle, this::distributeExecution);
		writeSheet(workbook, "Presupuestado - Ejecutado ACUMULADO", entries, fechaDesde, fechaHasta, selectedStudies,
				totalizarConceptos, months, labelStyle, headerStyle, this::distributeCumulativeDifference);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private void writeSheet(Workbook workbook, String sheetName, List<BudgetEntry> entries, LocalDate fechaDesde,
			LocalDate fechaHasta, List<Study> selectedStudies, boolean totalizarConceptos, List<YearMonth> months,
			CellStyle labelStyle, CellStyle headerStyle,
			BiFunction<BudgetEntry, List<YearMonth>, double[]> distributor) {
		Sheet sheet = workbook.createSheet(sheetName);

		int rowIndex = 0;

		Row reporteRow = sheet.createRow(rowIndex++);
		Cell reporteLabel = reporteRow.createCell(0);
		reporteLabel.setCellValue("Reporte:");
		reporteLabel.setCellStyle(labelStyle);
		reporteRow.createCell(1).setCellValue(sheetName);

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

		List<String> headers = new ArrayList<>();
		headers.add("Estudio");
		if (!totalizarConceptos) {
			headers.add("Concepto presupuesto");
		}
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

		int totalColumnIndex;
		int firstMonthColumnIndex;
		double totalSum = 0d;
		double[] monthlySums = new double[months.size()];

		if (totalizarConceptos) {
			totalColumnIndex = 2;
			firstMonthColumnIndex = 3;
			List<AggregatedRow> aggregated = aggregateByStudyAndTipo(entries, months, distributor);
			for (AggregatedRow agg : aggregated) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0).setCellValue(agg.estudio);
				row.createCell(1).setCellValue(agg.tipo);
				double rowTotal = 0d;
				for (int i = 0; i < months.size(); i++) {
					row.createCell(firstMonthColumnIndex + i).setCellValue(agg.monthly[i]);
					rowTotal += agg.monthly[i];
					monthlySums[i] += agg.monthly[i];
				}
				row.createCell(totalColumnIndex).setCellValue(rowTotal);
				totalSum += rowTotal;
			}
		} else {
			totalColumnIndex = 3;
			firstMonthColumnIndex = 4;
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

				double[] distribution = distributor.apply(entry, months);
				double rowTotal = 0d;
				for (int i = 0; i < months.size(); i++) {
					row.createCell(firstMonthColumnIndex + i).setCellValue(distribution[i]);
					rowTotal += distribution[i];
					monthlySums[i] += distribution[i];
				}
				row.createCell(totalColumnIndex).setCellValue(rowTotal);
				totalSum += rowTotal;
			}
		}

		Row totalsRow = sheet.createRow(rowIndex++);
		Cell totalsLabel = totalsRow.createCell(0);
		totalsLabel.setCellValue("Total");
		totalsLabel.setCellStyle(headerStyle);
		for (int i = 1; i < totalColumnIndex; i++) {
			Cell emptyCell = totalsRow.createCell(i);
			emptyCell.setCellStyle(headerStyle);
		}
		Cell totalSumCell = totalsRow.createCell(totalColumnIndex);
		totalSumCell.setCellValue(totalSum);
		totalSumCell.setCellStyle(headerStyle);
		for (int i = 0; i < months.size(); i++) {
			Cell monthSumCell = totalsRow.createCell(firstMonthColumnIndex + i);
			monthSumCell.setCellValue(monthlySums[i]);
			monthSumCell.setCellStyle(headerStyle);
		}

		for (int i = 0; i < headers.size(); i++) {
			sheet.autoSizeColumn(i);
		}
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

	private double[] distributeExecution(BudgetEntry entry, List<YearMonth> months) {
		double[] result = new double[months.size()];
		if (months.isEmpty()) {
			return result;
		}

		if (entry.getExtras() != null) {
			for (Extra extra : entry.getExtras()) {
				int idx = monthIndex(extra.getDate(), months);
				if (idx < 0) {
					continue;
				}
				Double amount = extra.getAmount();
				if (amount != null) {
					result[idx] += amount;
				}
			}
		}

		if (entry.getExpenseRequests() != null) {
			for (ExpenseRequest expenseRequest : entry.getExpenseRequests()) {
				int idx = monthIndex(expenseRequest.getTransferDate(), months);
				if (idx < 0) {
					continue;
				}
				Double amount = expenseRequest.getAmount();
				if (amount != null) {
					result[idx] += amount;
				}
			}
		}

		if (entry.getOdooCosts() != null) {
			for (OdooCost odooCost : entry.getOdooCosts()) {
				int idx = monthIndex(odooCost.getDate(), months);
				if (idx < 0) {
					continue;
				}
				BigDecimal balance = odooCost.getBalance();
				if (balance != null) {
					result[idx] += balance.doubleValue();
				}
			}
		}

		if (entry.getFieldworks() != null) {
			Double entryAmount = entry.getAmmount();
			if (entryAmount != null && entryAmount != 0d) {
				for (Fieldwork fieldwork : entry.getFieldworks()) {
					Map<Date, Integer> completedByMonth = fieldwork.getCompletedByMonth();
					if (completedByMonth == null) {
						continue;
					}
					for (Map.Entry<Date, Integer> e : completedByMonth.entrySet()) {
						Date key = e.getKey();
						Integer value = e.getValue();
						if (key == null || value == null) {
							continue;
						}
						int idx = monthIndex(toLocalDate(key), months);
						if (idx < 0) {
							continue;
						}
						result[idx] += entryAmount * value;
					}
				}
			}
		}

		return result;
	}

	private double[] distributeCumulativeDifference(BudgetEntry entry, List<YearMonth> months) {
		double[] result = new double[months.size()];
		if (months.isEmpty()) {
			return result;
		}
		double[] planned = distribute(entry, months);
		double[] executed = distributeExecution(entry, months);
		double accumulated = 0d;
		for (int i = 0; i < months.size(); i++) {
			accumulated += executed[i] - planned[i];
			result[i] = accumulated;
		}
		return result;
	}

	private static int monthIndex(LocalDate date, List<YearMonth> months) {
		if (date == null) {
			return -1;
		}
		YearMonth target = YearMonth.from(date);
		for (int i = 0; i < months.size(); i++) {
			if (months.get(i).equals(target)) {
				return i;
			}
		}
		return -1;
	}

	private static LocalDate toLocalDate(Date date) {
		if (date instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private List<AggregatedRow> aggregateByStudyAndTipo(List<BudgetEntry> entries, List<YearMonth> months,
			BiFunction<BudgetEntry, List<YearMonth>, double[]> distributor) {
		Map<String, AggregatedRow> map = new LinkedHashMap<>();
		for (BudgetEntry entry : entries) {
			String estudio = studyName(entry);
			String tipo = tipo(entry);
			String key = estudio + "||" + tipo;
			AggregatedRow agg = map.computeIfAbsent(key, k -> new AggregatedRow(estudio, tipo, months.size()));
			agg.total += entry.getTotal() != null ? entry.getTotal() : 0d;
			double[] distribution = distributor.apply(entry, months);
			for (int i = 0; i < months.size(); i++) {
				agg.monthly[i] += distribution[i];
			}
		}
		List<AggregatedRow> list = new ArrayList<>(map.values());
		list.sort(Comparator
				.comparing((AggregatedRow a) -> a.estudio, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
				.thenComparing(a -> a.tipo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return list;
	}

	private static class AggregatedRow {
		final String estudio;
		final String tipo;
		double total;
		final double[] monthly;

		AggregatedRow(String estudio, String tipo, int monthCount) {
			this.estudio = estudio;
			this.tipo = tipo;
			this.monthly = new double[monthCount];
		}
	}
}
