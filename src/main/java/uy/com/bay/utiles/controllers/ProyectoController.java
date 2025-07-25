package uy.com.bay.utiles.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.bay.utiles.data.Proyecto;
import uy.com.bay.utiles.services.ProyectoService;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    // Inyección por constructor (preferida)
    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @GetMapping
    public List<Proyecto> getAllProyectos() {
        return proyectoService.findAll();
    }
}
