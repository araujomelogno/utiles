package uy.com.bay.utiles.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import uy.com.bay.utiles.services.SupervisionTaskBatchService;

@Component
public class SupervisionScheduledTask {

	private final SupervisionTaskBatchService batchService;

	private static final Logger logger = LoggerFactory.getLogger(SupervisionScheduledTask.class);

	public SupervisionScheduledTask(SupervisionTaskBatchService batchService) {
		this.batchService = batchService;
	}

	@PostConstruct
	public void init() {
		logger.info("[SCHED] SupervisionScheduledTask bean initialized");
	}

	@Scheduled(cron = "0 */9 * * * *")
	public void processPendingSupervisionTasks() {

		try {
			logger.info("Ejecutando SupervisionService Task");
			batchService.processPendingTasksBatch();
		} catch (Exception e) {
			logger.error("Scheduled supervision processing failed", e);
		}
	}
}
