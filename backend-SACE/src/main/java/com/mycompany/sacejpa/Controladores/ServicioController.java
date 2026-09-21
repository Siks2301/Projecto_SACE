package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.ServicioDTOs;
import com.mycompany.sacejpa.Servicios.ServicioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    @Autowired
    private ServicioServicio servicioServicio;

    @GetMapping
    public List<ServicioDTOs> ListarServicios() {
        return servicioServicio.listarServicios();
    }

    @GetMapping("/{id}")
    public ServicioDTOs BuscarServicio(@PathVariable Long id) {
        return servicioServicio.buscarServicio(id);
    }

    @PostMapping
    public ServicioDTOs InsertarServicio(@RequestBody ServicioDTOs dto) {
        return servicioServicio.insertarServicio(dto);
    }

    @PutMapping("/{id}")
    public ServicioDTOs ActualizaServicio(@PathVariable Long id, @RequestBody ServicioDTOs dto) {
        return servicioServicio.servactualiza(id, dto);
    }

    @DeleteMapping("/{id}")
    public void EliminaServicio(@PathVariable Long id) {
        servicioServicio.eliminarServicio(id);
    }
}
