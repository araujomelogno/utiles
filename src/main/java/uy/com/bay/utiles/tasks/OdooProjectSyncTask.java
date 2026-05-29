package uy.com.bay.utiles.tasks;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.entities.StudyInvoice;
import uy.com.bay.utiles.services.OdooService;
import uy.com.bay.utiles.services.StudyInvoiceService;
import uy.com.bay.utiles.services.StudyService;

@Component
public class OdooProjectSyncTask {

	private final OdooService odooService;
	private final StudyService proyectoService;
	private final StudyInvoiceService studyInvoiceService;

	@Value("${odoo.invoices.sync.days:7}")
	private int invoicesSyncDays;

	public OdooProjectSyncTask(OdooService odooService, StudyService proyectoService,
			StudyInvoiceService studyInvoiceService) {
		this.odooService = odooService;
		this.proyectoService = proyectoService;
		this.studyInvoiceService = studyInvoiceService;
	}

	@Scheduled(cron = "0 0 7 * * *")
	public void updateInvoices() {
		System.out.println("Starting Odoo Invoices Update Task...");

		LocalDate end = LocalDate.now();
		LocalDate init = end.minusDays(invoicesSyncDays);

		List<Map<String, Object>> lines = odooService.getOdooInvoiceAccountMoveLines(init, end);

		int newInvoicesCount = 0;
		for (Map<String, Object> line : lines) {
			String moveId = extractRelationName(line.get("move_id"));
			if (moveId == null || moveId.trim().isEmpty()) {
				continue;
			}

			Optional<StudyInvoice> existing = studyInvoiceService.findByMoveId(moveId);
			if (existing.isPresent()) {
				continue;
			}

			String analyticAccountId = extractRelationId(line.get("analytic_account_id"));
			if (analyticAccountId == null) {
				continue;
			}

			Optional<Study> studyOpt = proyectoService.findByOdooId(analyticAccountId);
			if (studyOpt.isEmpty()) {
				continue;
			}

			StudyInvoice invoice = new StudyInvoice();
			invoice.setMoveId(moveId);
			invoice.setStudy(studyOpt.get());

			Object dateObj = line.get("date");
			if (dateObj != null && !(dateObj instanceof Boolean)) {
				try {
					invoice.setInvoiceDate(LocalDate.parse(String.valueOf(dateObj)));
				} catch (Exception ignored) {
				}
			}

			Double priceSubtotal = toDouble(line.get("price_subtotal"));
			Double priceTotal = toDouble(line.get("price_total"));
			Double totalSigned = toDouble(line.get("balance")) * -1d;
			invoice.setAmountUntaxed(priceSubtotal);
			invoice.setAmountTotal(priceTotal);
			invoice.setTotalSigned(totalSigned);
			if (priceSubtotal != null && priceTotal != null) {
				invoice.setTax(priceTotal - priceSubtotal);
			}
			invoice.setCurrency(extractRelationName(line.get("currency_id")));

			studyInvoiceService.save(invoice);
			newInvoicesCount++;
		}

		System.out.println("Odoo Invoices Update Task finished. Added " + newInvoicesCount + " new invoice(s).");
	}

	private static String extractRelationId(Object value) {
		if (value instanceof Object[] arr && arr.length >= 1 && arr[0] != null) {
			return String.valueOf(arr[0]);
		}
		if (value instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
			return String.valueOf(list.get(0));
		}
		return null;
	}

	private static String extractRelationName(Object value) {
		if (value instanceof Object[] arr && arr.length >= 2 && arr[1] != null) {
			return String.valueOf(arr[1]);
		}
		if (value instanceof List<?> list && list.size() >= 2 && list.get(1) != null) {
			return String.valueOf(list.get(1));
		}
		return null;
	}

	private static Double toDouble(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return 0d;
	}

	// @Scheduled(cron = "0 0 * * * ?") // Runs every hour at the beginning of the
	// hour
	// For testing, you might use a more frequent cron like "*/30 * * * * ?" (every
	// 30 seconds)
//	@Scheduled(cron = "* */2 * * * *")
	@Scheduled(cron = "0 0  5 * * *")
	public void syncOdooProjects() {
		System.out.println("Starting Odoo Project Sync Task...");

//		List<Map<String, Object>> costs = odooService.getOdooAccountMoveLines("33647", "118");
//		for (Map<String, Object> odooProjectMap : costs) {
//			Object odooIdObj = odooProjectMap.get("name");
//			String odooId = null;
//			if (odooIdObj != null) {
//				odooId = String.valueOf(odooIdObj); // Convert to String, ensure it's not null
//			}
//
//			Object nameIdObj = odooProjectMap.get("balance");
//			String odooName = null;
//			if (nameIdObj != null) {
//				odooName = String.valueOf(nameIdObj); // Convert to String, ensure it's not null
//			}
//
//			System.out.println(odooId + "-" + odooName);
//		}

		List<Map<String, Object>> odooProjects = odooService.getOdooAnalyticAccounts();
//				odooService.getOdooProjects();
//		odooProjects.addAll(odooService.getOdooLeads());

//		for (Map<String, Object> odooProjectMap : odooProjects) {
//			Object odooIdObj = odooProjectMap.get("id");
//			String odooId = null;
//			if (odooIdObj != null) {
//				odooId = String.valueOf(odooIdObj); // Convert to String, ensure it's not null
//			}
//
//			Object nameIdObj = odooProjectMap.get("name");
//			String odooName = null;
//			if (nameIdObj != null) {
//				odooName = String.valueOf(nameIdObj); // Convert to String, ensure it's not null
//			}
//
//			System.out.println(odooId + "-" + odooName);
//		}
//		if (odooProjects.isEmpty()) {
//			System.out.println("No projects fetched from Odoo. Sync task finished.");
//			return;
//		}

		List<Study> existingProyectos = proyectoService.findAll();
		Map<String, Study> existingByName = new HashMap<>();
		for (Study s : existingProyectos) {
			String name = s.getName();
			if (name != null && !name.isEmpty()) {
				existingByName.putIfAbsent(name.toLowerCase(Locale.ROOT), s);
			}
		}

		int newProjectsCount = 0;
		for (Map<String, Object> odooProjectMap : odooProjects) {
			// Assuming Odoo project map contains "id" as the Odoo ID and "name" as the
			// project name.
			// These keys might need adjustment based on the actual data from OdooService.
			Object odooIdObj = odooProjectMap.get("id");
			String odooId = null;
			if (odooIdObj != null) {
				odooId = String.valueOf(odooIdObj); // Convert to String, ensure it's not null
			}

			if (odooId == null || odooId.trim().isEmpty()) {
				System.out.println("Skipping Odoo project with null or empty ID.");
				continue;
			}
			Object nameObj = odooProjectMap.get("name");
			String odooName = nameObj == null ? null : String.valueOf(nameObj);
			String key = odooName == null ? null : odooName.toLowerCase(Locale.ROOT);
			Study existing = key == null ? null : existingByName.get(key);

			Object clientNameObj = odooProjectMap.get("client_name");
			String clientName = clientNameObj == null ? null : String.valueOf(clientNameObj);

			Object expectedRevenueObj = odooProjectMap.get("expected_revenue");
			double expectedRevenue = expectedRevenueObj instanceof Number ? ((Number) expectedRevenueObj).doubleValue()
					: 0d;

			Object areaObj = odooProjectMap.get("crm_team");
			String area = areaObj == null ? null : String.valueOf(areaObj);
			if (area != null)
				area = area.replace("Opinión Publica", "Opinion Publica");
			if (existing == null) {
				Study newProyecto = new Study();
				newProyecto.setOdooId(odooId);

				if (odooName != null) {
					newProyecto.setName(odooName);
				} else {
					newProyecto.setName("Default Name - ID: " + odooId); // Or handle as an error
				}

				newProyecto.setClientName(clientName);
				newProyecto.setExpectedRevenue(expectedRevenue);
				newProyecto.setArea(area);

				Study saved = proyectoService.save(newProyecto);
				if (saved.getName() != null && !saved.getName().isEmpty()) {
					existingByName.put(saved.getName().toLowerCase(Locale.ROOT), saved);
				}
				newProjectsCount++;
				System.out.println("Saved new project: " + saved.getName() + " (Odoo ID: " + odooId + ")");
			} else {
				boolean changed = false;
				if (!Objects.equals(existing.getOdooId(), odooId)) {
					existing.setOdooId(odooId);
					changed = true;
				}
				if (!Objects.equals(existing.getClientName(), clientName)) {
					existing.setClientName(clientName);
					changed = true;
				}
				if (Double.compare(existing.getExpectedRevenue(), expectedRevenue) != 0) {
					existing.setExpectedRevenue(expectedRevenue);
					changed = true;
				}
				if (!Objects.equals(existing.getArea(), area)) {
					existing.setArea(area);
					changed = true;
				}
				if (changed) {
					Study saved = proyectoService.save(existing);
					existingByName.put(key, saved);
				}
			}
		}

		if (newProjectsCount > 0) {
			System.out.println("Odoo Project Sync Task finished. Added " + newProjectsCount + " new project(s).");
		} else {
			System.out.println("Odoo Project Sync Task finished. No new projects to add.");
		}
	}
}
