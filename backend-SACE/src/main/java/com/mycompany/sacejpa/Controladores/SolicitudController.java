package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.SolicitudDTOs;
import com.mycompany.sacejpa.Seguridad.AuthInterceptor;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import com.mycompany.sacejpa.Servicios.SolicitudServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {
    @Autowired
    private SolicitudServicio solicitudServicio;

    @GetMapping
    public List<SolicitudDTOs> ListarSolicitudes(HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return solicitudServicio.listarSolicitudes(
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @GetMapping("/{id}")
    public SolicitudDTOs BuscarSolicitud(@PathVariable Long id, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return solicitudServicio.buscarSolicitud(id,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @PostMapping
    public SolicitudDTOs InsertarSolicitud(@RequestBody SolicitudDTOs dto, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return solicitudServicio.insertarSolicitud(dto,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @PutMapping("/{id}")
    public SolicitudDTOs ActualizaSolicitud(@PathVariable Long id, @RequestBody SolicitudDTOs dto,
            HttpServletRequest request) {
        // La sesion autenticada la deja el AuthInterceptor en el request.
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        String tipoUsuario = sesion != null ? sesion.tipoUsuario() : null;
        Long usuarioId = sesion != null ? sesion.id() : null;
        return solicitudServicio.servactualiza(id, dto, tipoUsuario, usuarioId);
    }

    @DeleteMapping("/{id}")
    public void EliminaSolicitud(@PathVariable Long id) {
        solicitudServicio.eliminarSolicitud(id);
    }
}
