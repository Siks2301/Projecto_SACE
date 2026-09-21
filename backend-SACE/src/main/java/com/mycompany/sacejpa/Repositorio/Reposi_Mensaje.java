package com.mycompany.sacejpa.Repositorio;

import com.mycompany.sacejpa.Modelo.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Reposi_Mensaje extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findBySolicitudIdOrderByFechaAsc(Long solicitudId);
}