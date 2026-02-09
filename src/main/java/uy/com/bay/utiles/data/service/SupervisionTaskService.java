package uy.com.bay.utiles.data.service;

import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupervisionTaskService {

    private final SupervisionTaskRepository repository;

    public SupervisionTaskService(SupervisionTaskRepository repository) {
        this.repository = repository;
    }

    public List<SupervisionTask> findByCreatedBetweenAndFileNameAndStatus(Date from, Date to, String fileName,
			Status status) {
		return repository.findByCreatedBetweenOrderByCreatedDesc(from, to, fileName, status);
	}

    public List<SupervisionTaskDTO> findDTOByCreatedBetweenAndFileNameAndStatus(Date from, Date to, String fileName,
			Status status) {
        List<Tuple> tuples = repository.findTuplesByCreatedBetweenOrderByCreatedDesc(from, to, fileName, status);
        Map<Long, SupervisionTaskDTO> dtoMap = new LinkedHashMap<>();

        for (Tuple t : tuples) {
            Long id = t.get("id", Long.class);
            SupervisionTaskDTO dto = dtoMap.computeIfAbsent(id, k -> {
                String fName = t.get("fileName", String.class);
                Status st = t.get("status", Status.class);
                Double aiScoreVal = t.get("aiScore", Double.class);
                double aiScore = aiScoreVal != null ? aiScoreVal : 0.0;

                Double totalDurationVal = t.get("totalAudioDuration", Double.class);
                double totalDuration = totalDurationVal != null ? totalDurationVal : 0.0;

                Double speakingDurationVal = t.get("speakingDuration", Double.class);
                double speakingDuration = speakingDurationVal != null ? speakingDurationVal : 0.0;

                Date created = t.get("created", Date.class);
                String output = t.get("output", String.class);
                String evaluationOutput = t.get("evaluationOutput", String.class);

                return new SupervisionTaskDTO(id, fName, st, aiScore, totalDuration, speakingDuration, created, output, evaluationOutput);
            });

            String speaker = t.get("speaker", String.class);
            Double duration = t.get("duration", Double.class);
            if (speaker != null) {
                dto.getDurationBySpeakers().put(speaker, duration);
            }
        }
        return new ArrayList<>(dtoMap.values());
    }

    public void saveAll(List<SupervisionTask> tasks) {
        repository.saveAll(tasks);
    }
}
