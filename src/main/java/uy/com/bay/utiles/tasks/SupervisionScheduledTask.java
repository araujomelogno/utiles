package uy.com.bay.utiles.tasks;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.com.bay.utiles.services.SupervisionTaskServiceTransactional;

@Component
public class SupervisionScheduledTask {

    private final SupervisionTaskServiceTransactional supervisionTaskService;

    public SupervisionScheduledTask(SupervisionTaskServiceTransactional supervisionTaskService) {
        this.supervisionTaskService = supervisionTaskService;
    }

    @Scheduled(fixedRate = 600000) // 10 minutes
    public void processPendingSupervisionTasks() {
        supervisionTaskService.processPendingTasks();
    }
}
