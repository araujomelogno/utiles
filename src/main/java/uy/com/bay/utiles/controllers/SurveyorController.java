package uy.com.bay.utiles.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.services.SurveyorService;

@RestController
@RequestMapping("/api/surveyors")
public class SurveyorController {

    private final SurveyorService surveyorService;

    public SurveyorController(SurveyorService encuestadorService) {
        this.surveyorService = encuestadorService;
    }

    @GetMapping
    public List<Surveyor> getAllEncuestadores() {
        return surveyorService.findAll();
    }
}
