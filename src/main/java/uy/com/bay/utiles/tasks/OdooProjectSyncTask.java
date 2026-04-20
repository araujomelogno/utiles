package uy.com.bay.utiles.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.OdooService;
import uy.com.bay.utiles.services.StudyService;

@Component
public class OdooProjectSyncTask {

	private final OdooService odooService;
	private final StudyService proyectoService;

	public OdooProjectSyncTask(OdooService odooService, StudyService proyectoService) {
		this.odooService = odooService;
		this.proyectoService = proyectoService;
	}

	// @Scheduled(cron = "0 0 * * * ?") // Runs every hour at the beginning of the
	// hour
	// For testing, you might use a more frequent cron like "*/30 * * * * ?" (every
	// 30 seconds)
	@Scheduled(cron = "0 */2 * * * *")
	public void syncOdooProjects() {
		System.out.println("Starting Odoo Project Sync Task...");

		List<Map<String, Object>> costs = odooService.getOdooAccountMoveLines("33647", "118");
		for (Map<String, Object> odooProjectMap : costs) {
			Object odooIdObj = odooProjectMap.get("name");
			String odooId = null;
			if (odooIdObj != null) {
				odooId = String.valueOf(odooIdObj); // Convert to String, ensure it's not null
			}

			Object nameIdObj = odooProjectMap.get("balance");
			String odooName = null;
			if (nameIdObj != null) {
				odooName = String.valueOf(nameIdObj); // Convert to String, ensure it's not null
			}

			System.out.println(odooId + "-" + odooName);
		}

		List<Map<String, Object>> odooProjects = odooService.getOdooAnalyticAccounts();
//				odooService.getOdooProjects();
//		odooProjects.addAll(odooService.getOdooLeads());

		for (Map<String, Object> odooProjectMap : odooProjects) {
			Object odooIdObj = odooProjectMap.get("id");
			String odooId = null;
			if (odooIdObj != null) {
				odooId = String.valueOf(odooIdObj); // Convert to String, ensure it's not null
			}

			Object nameIdObj = odooProjectMap.get("name");
			String odooName = null;
			if (nameIdObj != null) {
				odooName = String.valueOf(nameIdObj); // Convert to String, ensure it's not null
			}

			System.out.println(odooId + "-" + odooName);
		}
		if (odooProjects.isEmpty()) {
			System.out.println("No projects fetched from Odoo. Sync task finished.");
			return;
		}

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
			if (existing == null) {
				Study newProyecto = new Study();
				newProyecto.setOdooId(odooId);

				if (odooName != null) {
					newProyecto.setName(odooName);
				} else {
					newProyecto.setName("Default Name - ID: " + odooId); // Or handle as an error
				}

				// Map other fields as necessary from odooProjectMap to newProyecto
				// For example:
				// String description = (String) odooProjectMap.get("description");
				// newProyecto.setObs(description);

				Study saved = proyectoService.save(newProyecto);
				if (saved.getName() != null && !saved.getName().isEmpty()) {
					existingByName.put(saved.getName().toLowerCase(Locale.ROOT), saved);
				}
				newProjectsCount++;
				System.out.println("Saved new project: " + saved.getName() + " (Odoo ID: " + odooId + ")");
			} else if (!Objects.equals(existing.getOdooId(), odooId)) {
				existing.setOdooId(odooId);
				Study saved = proyectoService.save(existing);
				existingByName.put(key, saved);
			}
		}

		if (newProjectsCount > 0) {
			System.out.println("Odoo Project Sync Task finished. Added " + newProjectsCount + " new project(s).");
		} else {
			System.out.println("Odoo Project Sync Task finished. No new projects to add.");
		}
	}
}
