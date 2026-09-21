package com.mycompany.sacejpa.Repositorio;

import com.mycompany.sacejpa.Modelo.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Reposi_Empleado extends JpaRepository<Empleado, Long> {

    // Busqueda insensible a mayusculas/minusculas, igual que en Reposi_Cliente,
    // usada por AuthServicio para el login de empleados.
    Optional<Empleado> findByEmailIgnoreCase(String email);

}
