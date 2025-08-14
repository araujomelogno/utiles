package uy.com.bay.utiles.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.services.StudyService;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyService studyService;

    // Inyección por constructor (preferida)
    public StudyController(StudyService proyectoService) {
        this.studyService = proyectoService;
    }

    @GetMapping
    public List<Study> getAllProyectos() {
        return studyService.findAll();
    }
}
