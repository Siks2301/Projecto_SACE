package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.EmpleadoDTOs;
import com.mycompany.sacejpa.Servicios.EmpleadoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {
    @Autowired
    private EmpleadoServicio empleadoServicio;

    @GetMapping
    public List<EmpleadoDTOs> ListarEmpleados() {
        return empleadoServicio.listarEmpleados();
    }

    @GetMapping("/{id}")
    public EmpleadoDTOs BuscarEmpleado(@PathVariable Long id) {
        return empleadoServicio.buscarEmpleado(id);
    }

    @PostMapping
    public EmpleadoDTOs InsertarEmpleado(@RequestBody EmpleadoDTOs dto) {
        return empleadoServicio.insertarEmpleado(dto);
    }

    @PutMapping("/{id}")
    public EmpleadoDTOs ActualizaEmpleado(@PathVariable Long id, @RequestBody EmpleadoDTOs dto) {
        return empleadoServicio.servactualiza(id, dto);
    }

    @DeleteMapping("/{id}")
    public void EliminaEmpleado(@PathVariable Long id) {
        empleadoServicio.eliminarEmpleado(id);
    }
}
