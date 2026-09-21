package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.ClienteDTOs;
import com.mycompany.sacejpa.Servicios.ClienteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    @Autowired
    private ClienteServicio clienteServicio;

    @GetMapping
    public List<ClienteDTOs> ListarClientes() {
        return clienteServicio.listarClientes();
    }

    @GetMapping("/{id}")
    public ClienteDTOs BuscarCliente(@PathVariable Long id) {
        return clienteServicio.buscarCliente(id);
    }

    @PostMapping
    public ClienteDTOs InsertarCliente(@RequestBody ClienteDTOs dto) {
        return clienteServicio.insertarCliente(dto);
    }

    @PutMapping("/{id}")
    public ClienteDTOs ActualizaCliente(@PathVariable Long id, @RequestBody ClienteDTOs dto) {
        return clienteServicio.servactualiza(id, dto);
    }

    @DeleteMapping("/{id}")
    public void EliminaCliente(@PathVariable Long id) {
        clienteServicio.eliminarCliente(id);
    }
}
