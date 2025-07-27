package uy.com.bay.utiles.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;
import uy.com.bay.utiles.data.JobType;
import uy.com.bay.utiles.data.Proyecto;
import uy.com.bay.utiles.data.ProyectoRepository;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseDataRepository;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseRepository;
import uy.com.bay.utiles.data.repository.TaskRepository;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class AlchemerController {

    @Autowired
    private AlchemerSurveyResponseRepository alchemerSurveyResponseRepository;

    @Autowired
    private AlchemerSurveyResponseDataRepository alchemerSurveyResponseDataRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private TaskRepository taskRepository;

    @PostMapping("/survey-response")
    public ResponseEntity<Void> receiveAlchemerResponse(@RequestBody AlchemerSurveyResponse response) {
        Optional<Proyecto> optionalProyecto = proyectoRepository.findByAlchemerId(String.valueOf(response.getData().getSurveyId()));
        optionalProyecto.ifPresent(response::setProyecto);

        response.getData().setSurveyResponse(response);
        alchemerSurveyResponseRepository.save(response);

        Task task = new Task();
        task.setJobType(JobType.ALCHEMERANSWERRETRIEVAL);
        task.setStatus(Status.PENDING);
        task.setCreated(new Date());
        task.setSurveyId(response.getData().getSurveyId());
        taskRepository.save(task);

        return ResponseEntity.ok().build();
    }
}
