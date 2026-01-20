package uy.com.bay.utiles.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.AudioFile;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;

@Service
public class SupervisionTaskServiceTransactional {

    private static final Logger logger = LoggerFactory.getLogger(SupervisionTaskServiceTransactional.class);
    private final SupervisionTaskRepository supervisionTaskRepository;
    private final OpenAiService openAiService;

    public SupervisionTaskServiceTransactional(SupervisionTaskRepository supervisionTaskRepository, OpenAiService openAiService) {
        this.supervisionTaskRepository = supervisionTaskRepository;
        this.openAiService = openAiService;
    }

    @Transactional
    public void processPendingTasks() {
        List<SupervisionTask> pendingTasks = supervisionTaskRepository.findByStatus(Status.PENDING);
        for (SupervisionTask task : pendingTasks) {
            try {
                AudioFile audioFile = new AudioFile(task.getFileName(), new ByteArrayInputStream(task.getAudioContent()));
                String transcription = openAiService.transcribeAudio(audioFile);
                task.setOutput(transcription);
                task.setStatus(Status.DONE);
                task.setProcessed(new Date());
            } catch (Exception e) {
                logger.error("Error processing supervision task {}: {}", task.getId(), e.getMessage());
                task.setStatus(Status.ERROR);
            } finally {
                supervisionTaskRepository.save(task);
            }
        }
    }
}
