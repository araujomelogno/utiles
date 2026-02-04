package uy.com.bay.utiles.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import uy.com.bay.utiles.services.SupervisionTaskServiceTransactional;

@Component
public class SupervisionScheduledTask {

	private final SupervisionTaskServiceTransactional supervisionTaskService;
	private static final Logger logger = LoggerFactory.getLogger(SupervisionScheduledTask.class);

	public SupervisionScheduledTask(SupervisionTaskServiceTransactional supervisionTaskService) {
		this.supervisionTaskService = supervisionTaskService;
	}

	
	@PostConstruct
	public void init() {
	  logger.info("[SCHED] SupervisionScheduledTask bean initialized");
	}
	
	@Scheduled(cron = "0 */9 * * * *")
	public void processPendingSupervisionTasks() {
		logger.info("Ejecutando SupervisionService Task");
		supervisionTaskService.processPendingTasks();
	}
}
