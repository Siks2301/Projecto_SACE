package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.MensajeDTOs;
import com.mycompany.sacejpa.Seguridad.AuthInterceptor;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import com.mycompany.sacejpa.Servicios.MensajeServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeController {
    @Autowired
    private MensajeServicio mensajeServicio;

    @GetMapping
    public List<MensajeDTOs> ListarMensajes(HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return mensajeServicio.listarMensajes(
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @GetMapping("/solicitud/{solicitudId}")
    public List<MensajeDTOs> ListarPorSolicitud(@PathVariable Long solicitudId, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return mensajeServicio.listarPorSolicitud(solicitudId,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @GetMapping("/{id}")
    public MensajeDTOs BuscarMensaje(@PathVariable Long id, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return mensajeServicio.buscarMensaje(id,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @PostMapping
    public MensajeDTOs InsertarMensaje(@RequestBody MensajeDTOs dto, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return mensajeServicio.insertarMensaje(dto,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @PutMapping("/{id}/calificacion")
    public MensajeDTOs CalificarMensaje(@PathVariable Long id, @RequestBody MensajeDTOs dto,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion)
                request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        return mensajeServicio.calificarMensaje(id, dto,
                sesion != null ? sesion.tipoUsuario() : null,
                sesion != null ? sesion.id() : null);
    }

    @DeleteMapping("/{id}")
    public void EliminaMensaje(@PathVariable Long id) {
        mensajeServicio.eliminarMensaje(id);
    }
}