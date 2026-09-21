package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.PreguntaFrecuenteDTOs;
import com.mycompany.sacejpa.Servicios.PreguntaFrecuenteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preguntas-frecuentes")
public class PreguntaFrecuenteController {
    @Autowired
    private PreguntaFrecuenteServicio preguntaFrecuenteServicio;

    @GetMapping
    public List<PreguntaFrecuenteDTOs> ListarPreguntas() {
        return preguntaFrecuenteServicio.listarPreguntas();
    }

    @GetMapping("/{id}")
    public PreguntaFrecuenteDTOs BuscarPregunta(@PathVariable Long id) {
        return preguntaFrecuenteServicio.buscarPregunta(id);
    }

    @PostMapping
    public PreguntaFrecuenteDTOs InsertarPregunta(@RequestBody PreguntaFrecuenteDTOs dto) {
        return preguntaFrecuenteServicio.insertarPregunta(dto);
    }

    @PutMapping("/{id}")
    public PreguntaFrecuenteDTOs ActualizaPregunta(@PathVariable Long id, @RequestBody PreguntaFrecuenteDTOs dto) {
        return preguntaFrecuenteServicio.servactualiza(id, dto);
    }

    @DeleteMapping("/{id}")
    public void EliminaPregunta(@PathVariable Long id) {
        preguntaFrecuenteServicio.eliminarPregunta(id);
    }
}
