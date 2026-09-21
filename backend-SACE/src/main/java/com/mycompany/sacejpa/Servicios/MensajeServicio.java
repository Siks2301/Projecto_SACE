package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.MensajeDTOs;
import com.mycompany.sacejpa.Exceptions.AutorizacionException;
import com.mycompany.sacejpa.Mapper.MensajeMapper;
import com.mycompany.sacejpa.Modelo.Mensaje;
import com.mycompany.sacejpa.Modelo.Solicitud;
import com.mycompany.sacejpa.Repositorio.Reposi_Mensaje;
import com.mycompany.sacejpa.Repositorio.Reposi_Solicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MensajeServicio {

    @Autowired
    private Reposi_Mensaje repositorioMensaje;

    @Autowired
    private Reposi_Solicitud repositorioSolicitud;

    private static final String ROL_CLIENTE = "CLIENTE";

    private boolean esCliente(String tipoUsuario) {
        return ROL_CLIENTE.equalsIgnoreCase(tipoUsuario);
    }

    private boolean esSolicitudDelCliente(Solicitud solicitud, Long usuarioId) {
        return usuarioId != null
                && solicitud.getCliente() != null
                && usuarioId.equals(solicitud.getCliente().getId());
    }

    // Un cliente solo ve los mensajes de sus propias solicitudes; admin y
    // asesores ven todo.
    public List<MensajeDTOs> listarMensajes(String tipoUsuario, Long usuarioId) {
        return repositorioMensaje.findAll().stream()
                .filter(m -> !esCliente(tipoUsuario)
                        || (m.getSolicitud() != null && esSolicitudDelCliente(m.getSolicitud(), usuarioId)))
                .map(MensajeMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Un cliente solo puede leer el hilo de una solicitud propia. Si la
    // solicitud es ajena se devuelve la lista vacia (sin revelar su existencia).
    public List<MensajeDTOs> listarPorSolicitud(Long solicitudId, String tipoUsuario, Long usuarioId) {
        if (esCliente(tipoUsuario)) {
            Solicitud solicitud = repositorioSolicitud.findById(solicitudId).orElse(null);
            if (solicitud == null || !esSolicitudDelCliente(solicitud, usuarioId)) {
                return java.util.Collections.emptyList();
            }
        }
        return repositorioMensaje.findBySolicitudIdOrderByFechaAsc(solicitudId).stream()
                .map(MensajeMapper::toDTO)
                .collect(Collectors.toList());
    }

    public MensajeDTOs buscarMensaje(Long id, String tipoUsuario, Long usuarioId) {
        Optional<Mensaje> opt = repositorioMensaje.findById(id);
        // Respuesta ambigua (404) para que un cliente no pueda saber siquiera
        // si existe un mensaje que no responde a una solicitud propia.
        if (opt.isEmpty() || (esCliente(tipoUsuario)
                && (opt.get().getSolicitud() == null
                    || !esSolicitudDelCliente(opt.get().getSolicitud(), usuarioId)))) {
            throw new RuntimeException("Mensaje no encontrado con id: " + id);
        }
        return MensajeMapper.toDTO(opt.get());
    }

    public MensajeDTOs insertarMensaje(MensajeDTOs dto, String tipoUsuario, Long usuarioId) {
        Mensaje mensaje = MensajeMapper.myEntity(dto);
        if (dto.getSolicitudId() != null) {
            Solicitud solicitud = repositorioSolicitud.findById(dto.getSolicitudId())
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + dto.getSolicitudId()));
            if (esCliente(tipoUsuario)) {
                // Un cliente solo escribe en el hilo de una solicitud propia y
                // que no esté cancelada (evita inyectar mensajes en hilos de
                // otros clientes — IDOR — o revivir una solicitud cancelada).
                if (!esSolicitudDelCliente(solicitud, usuarioId)) {
                    throw new AutorizacionException("No puedes enviar mensajes a esa solicitud.");
                }
                if (solicitud.getEstado() == Solicitud.Estado.CANCELADA) {
                    throw new RuntimeException("Esta solicitud esta cancelada; no se pueden enviar mas mensajes.");
                }
            }
            mensaje.setSolicitud(solicitud);
        }
        if (mensaje.getFecha() == null) {
            mensaje.setFecha(new java.util.Date());
        }
        Mensaje guardado = repositorioMensaje.save(mensaje);
        return MensajeMapper.toDTO(guardado);
    }

    public MensajeDTOs calificarMensaje(Long id, MensajeDTOs dto, String tipoUsuario, Long usuarioId) {
        Optional<Mensaje> existe = repositorioMensaje.findById(id);
        if (existe.isEmpty()) {
            throw new RuntimeException("Mensaje no encontrado con id: " + id);
        }
        // Un cliente solo califica los mensajes de sus propias solicitudes.
        if (esCliente(tipoUsuario)
                && (existe.get().getSolicitud() == null
                    || !esSolicitudDelCliente(existe.get().getSolicitud(), usuarioId))) {
            throw new AutorizacionException("No puedes calificar ese mensaje.");
        }
        Mensaje mensaje = existe.get();
        mensaje.setCalificacion(dto.getCalificacion());
        mensaje.setComentarioSatisfaccion(dto.getComentarioSatisfaccion());
        Mensaje actualizado = repositorioMensaje.save(mensaje);
        return MensajeMapper.toDTO(actualizado);
    }

    public void eliminarMensaje(Long id) {
        if (!repositorioMensaje.existsById(id)) {
            throw new RuntimeException("Mensaje no encontrado con id: " + id);
        }
        repositorioMensaje.deleteById(id);
    }
}