package uy.com.bay.utiles.data.service;

import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.SupervisionTaskDTO;

import jakarta.persistence.Tuple;
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

    public List<SupervisionTaskDTO> findDTOsByCreatedBetweenAndFileNameAndStatus(Date from, Date to, String fileName, Status status) {
        List<Tuple> tuples = repository.findTuplesByCreatedBetweenOrderByCreatedDesc(from, to, fileName, status);
        Map<Long, SupervisionTaskDTO> dtoMap = new LinkedHashMap<>();

        for (Tuple tuple : tuples) {
            Long id = tuple.get("id", Long.class);
            SupervisionTaskDTO dto = dtoMap.get(id);

            if (dto == null) {
                dto = new SupervisionTaskDTO();
                dto.setId(id);
                dto.setFileName(tuple.get("fileName", String.class));
                dto.setStatus(tuple.get("status", Status.class));
                dto.setAiScore(tuple.get("aiScore", Double.class));
                dto.setTotalAudioDuration(tuple.get("totalAudioDuration", Double.class));
                dto.setSpeakingDuration(tuple.get("speakingDuration", Double.class));
                dto.setCreated(tuple.get("created", Date.class));
                dto.setOutput(tuple.get("output", String.class));
                dto.setEvaluationOutput(tuple.get("evaluationOutput", String.class));

                dtoMap.put(id, dto);
            }

            String speaker = tuple.get("speaker", String.class);
            Double duration = tuple.get("duration", Double.class);

            if (speaker != null && duration != null) {
                dto.getDurationBySpeakers().put(speaker, duration);
            }
        }
        return new ArrayList<>(dtoMap.values());
    }

    public void saveAll(List<SupervisionTask> tasks) {
        repository.saveAll(tasks);
    }
}
