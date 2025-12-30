package uy.com.bay.utiles.tasks;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.com.bay.utiles.services.SupervisionTaskService;

@Component
public class SupervisionScheduledTask {

    private final SupervisionTaskService supervisionTaskService;

    public SupervisionScheduledTask(SupervisionTaskService supervisionTaskService) {
        this.supervisionTaskService = supervisionTaskService;
    }

    @Scheduled(fixedRate = 600000) // 10 minutes
    public void processPendingSupervisionTasks() {
        supervisionTaskService.processPendingTasks();
    }
}
