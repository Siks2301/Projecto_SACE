package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.SolicitudDTOs;
import com.mycompany.sacejpa.Modelo.Mensaje;
import com.mycompany.sacejpa.Modelo.Solicitud;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SolicitudMapper {

    private static String nombreCompleto(com.mycompany.sacejpa.Modelo.Cliente cliente) {
        String nombre = cliente.getNombre() == null ? "" : cliente.getNombre().trim();
        String apellido = cliente.getApellido() == null ? "" : cliente.getApellido().trim();
        return (nombre + " " + apellido).trim();
    }

    public static SolicitudDTOs toDTO(Solicitud solicitud) {
        List<Long> mensajesIds = solicitud.getMensajes() == null ? Collections.emptyList()
                : solicitud.getMensajes().stream().map(Mensaje::getId).collect(Collectors.toList());

        return new SolicitudDTOs(
                solicitud.getId(),
                solicitud.getFechaCreacion(),
                solicitud.getTitulo(),
                solicitud.getAsunto(),
                solicitud.getDescripcion(),
                solicitud.getEstado() != null ? solicitud.getEstado().name() : null,
                solicitud.getPrioridad() != null ? solicitud.getPrioridad().name() : null,
                solicitud.getCategoria(),
                solicitud.getCliente() != null ? solicitud.getCliente().getId() : null,
                solicitud.getCliente() != null ? nombreCompleto(solicitud.getCliente()) : null,
                solicitud.getCliente() != null ? solicitud.getCliente().getEmail() : null,
                solicitud.getCliente() != null ? solicitud.getCliente().getTelefono() : null,
                solicitud.getEmpleadoAsignado() != null ? solicitud.getEmpleadoAsignado().getId() : null,
                solicitud.getChatbotAsignado() != null ? solicitud.getChatbotAsignado().getId() : null,
                solicitud.getServicioGenerado() != null ? solicitud.getServicioGenerado().getId() : null,
                mensajesIds
        );
    }

    // Nota: cliente, empleadoAsignado, chatbotAsignado y servicioGenerado se
    // asignan en la capa de Servicio (que sí tiene acceso a los repositorios
    // para buscar esas entidades por id), no aquí en el Mapper.
    public static Solicitud myEntity(SolicitudDTOs dto) {
        Solicitud solicitud = new Solicitud();
        solicitud.setId(dto.getId());
        solicitud.setFechaCreacion(dto.getFechaCreacion());
        solicitud.setTitulo(dto.getTitulo());
        solicitud.setAsunto(dto.getAsunto());
        solicitud.setDescripcion(dto.getDescripcion());
        if (dto.getEstado() != null) {
            solicitud.setEstado(Solicitud.Estado.valueOf(dto.getEstado()));
        }
        if (dto.getPrioridad() != null) {
            solicitud.setPrioridad(Solicitud.Prioridad.valueOf(dto.getPrioridad()));
        }
        solicitud.setCategoria(dto.getCategoria());
        return solicitud;
    }
}
