package uy.com.bay.utiles.services;

import java.util.List;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;

@Service
public class SupervisionTaskBatchService {

    private final SupervisionTaskRepository supervisionTaskRepository;
    private final SupervisionTaskProcessorService processorService;

    public SupervisionTaskBatchService(SupervisionTaskRepository supervisionTaskRepository,
                                       SupervisionTaskProcessorService processorService) {
        this.supervisionTaskRepository = supervisionTaskRepository;
        this.processorService = processorService;
    }

    public void processPendingTasksBatch() {
        // Ideal: traer IDs, no entidades. Si no tenés este método, agregalo al repo (más abajo te lo dejo).
        List<Long> pendingIds = supervisionTaskRepository.findIdsByStatus(Status.PENDING);

        for (Long id : pendingIds) {
            processorService.processSingleTask(id); // COMMIT por task
        }
    }
}
