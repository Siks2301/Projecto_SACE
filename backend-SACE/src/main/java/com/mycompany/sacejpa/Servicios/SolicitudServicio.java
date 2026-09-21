package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.SolicitudDTOs;
import com.mycompany.sacejpa.Exceptions.AutorizacionException;
import com.mycompany.sacejpa.Mapper.SolicitudMapper;
import com.mycompany.sacejpa.Modelo.Chatbot;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Modelo.Solicitud;
import com.mycompany.sacejpa.Repositorio.Reposi_Chatbot;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Empleado;
import com.mycompany.sacejpa.Repositorio.Reposi_Servicio;
import com.mycompany.sacejpa.Repositorio.Reposi_Solicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SolicitudServicio {

    @Autowired
    private Reposi_Solicitud repositorioSolicitud;

    @Autowired
    private Reposi_Cliente repositorioCliente;

    @Autowired
    private Reposi_Empleado repositorioEmpleado;

    @Autowired
    private Reposi_Chatbot repositorioChatbot;

    @Autowired
    private Reposi_Servicio repositorioServicio;

    private static final String ROL_CLIENTE = "CLIENTE";

    private boolean esCliente(String tipoUsuario) {
        return ROL_CLIENTE.equalsIgnoreCase(tipoUsuario);
    }

    // Un cliente solo ve sus propias solicitudes; admin y asesores ven todo.
    public List<SolicitudDTOs> listarSolicitudes(String tipoUsuario, Long usuarioId) {
        return repositorioSolicitud.findAll().stream()
                .filter(s -> !esCliente(tipoUsuario) || esPropietario(s, usuarioId))
                .map(SolicitudMapper::toDTO)
                .collect(Collectors.toList());
    }

    private boolean esPropietario(Solicitud solicitud, Long usuarioId) {
        return usuarioId != null
                && solicitud.getCliente() != null
                && usuarioId.equals(solicitud.getCliente().getId());
    }

    // Un cliente solo puede leer su propia solicitud. La respuesta es 404
    // ambigua (mismo mensaje que "no existe") para no revelar la existencia
    // de solicitudes ajenas.
    public SolicitudDTOs buscarSolicitud(Long id, String tipoUsuario, Long usuarioId) {
        Solicitud solicitud = repositorioSolicitud.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + id));
        if (esCliente(tipoUsuario) && !esPropietario(solicitud, usuarioId)) {
            throw new RuntimeException("Solicitud no encontrada con id: " + id);
        }
        return SolicitudMapper.toDTO(solicitud);
    }

    public SolicitudDTOs insertarSolicitud(SolicitudDTOs dto, String tipoUsuario, Long usuarioId) {
        Solicitud solicitud = SolicitudMapper.myEntity(dto);
        if (esCliente(tipoUsuario)) {
            // El propietario SIEMPRE es el cliente autenticado; nunca se acepta
            // un clienteId ajeno desde el cuerpo (evita crear solicitudes a
            // nombre de otros). Tampoco se aceptan vinculaciones que el cliente
            // no puede decidir.
            dto.setClienteId(usuarioId);
            dto.setEmpleadoAsignadoId(null);
            dto.setChatbotAsignadoId(null);
        }
        asignarRelaciones(solicitud, dto);
        if (solicitud.getEmpleadoAsignado() == null) {
            try {
                java.util.List<Empleado> dispon = repositorioEmpleado.findAll().stream()
                        .filter(e -> e.getEstadoDisponibilidad() == Empleado.EstadoDisponibilidad.DISPONIBLE)
                        .collect(java.util.stream.Collectors.toList());
                if (!dispon.isEmpty()) {
                    solicitud.setEmpleadoAsignado(dispon.get(0));
                }
            } catch (Exception ex) {}
        }
        Solicitud guardada = repositorioSolicitud.save(solicitud);
        return SolicitudMapper.toDTO(guardada);
    }

    public SolicitudDTOs servactualiza(Long id, SolicitudDTOs dto, String tipoUsuario, Long usuarioId) {
        Optional<Solicitud> existe = repositorioSolicitud.findById(id);
        if (existe.isPresent()) {
            Solicitud solicitud = existe.get();

            // Un cliente solo puede CANCELAR una solicitud de su propiedad.
            if ("CLIENTE".equalsIgnoreCase(tipoUsuario)) {
                if (usuarioId == null
                        || solicitud.getCliente() == null
                        || !usuarioId.equals(solicitud.getCliente().getId())) {
                    throw new AutorizacionException("Solo puedes cancelar tus propias solicitudes.");
                }
                if (dto.getEstado() == null
                        || !"CANCELADA".equalsIgnoreCase(dto.getEstado())) {
                    throw new AutorizacionException("Como cliente solo puedes cancelar tu solicitud.");
                }
                solicitud.setEstado(Solicitud.Estado.CANCELADA);
                Solicitud cancelada = repositorioSolicitud.save(solicitud);
                return SolicitudMapper.toDTO(cancelada);
            }

            // null-guards: un PUT parcial solo toca lo enviado, para no borrar
            // titulo/descripcion/fecha historica al cambiar solo el estado.
            if (dto.getFechaCreacion() != null) {
                solicitud.setFechaCreacion(dto.getFechaCreacion());
            }
            if (dto.getTitulo() != null) {
                solicitud.setTitulo(dto.getTitulo());
            }
            if (dto.getAsunto() != null) {
                solicitud.setAsunto(dto.getAsunto());
            }
            if (dto.getDescripcion() != null) {
                solicitud.setDescripcion(dto.getDescripcion());
            }
            if (dto.getEstado() != null) {
                solicitud.setEstado(Solicitud.Estado.valueOf(dto.getEstado()));
            }
            if (dto.getPrioridad() != null) {
                solicitud.setPrioridad(Solicitud.Prioridad.valueOf(dto.getPrioridad()));
            }
            if (dto.getCategoria() != null) {
                solicitud.setCategoria(dto.getCategoria());
            }
            asignarRelaciones(solicitud, dto);
            Solicitud actualizada = repositorioSolicitud.save(solicitud);
            return SolicitudMapper.toDTO(actualizada);
        } else {
            throw new RuntimeException("Solicitud no encontrada con id: " + id);
        }
    }

    public void eliminarSolicitud(Long id) {
        if (!repositorioSolicitud.existsById(id)) {
            throw new RuntimeException("Solicitud no encontrada con id: " + id);
        }
        repositorioSolicitud.deleteById(id);
    }

    // Resuelve cliente, empleadoAsignado, chatbotAsignado y servicioGenerado
    // a partir de los ids que vienen en el DTO.
    private void asignarRelaciones(Solicitud solicitud, SolicitudDTOs dto) {
        if (dto.getClienteId() != null) {
            Cliente cliente = repositorioCliente.findById(dto.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + dto.getClienteId()));
            solicitud.setCliente(cliente);
        }
        if (dto.getEmpleadoAsignadoId() != null) {
            Empleado empleado = repositorioEmpleado.findById(dto.getEmpleadoAsignadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + dto.getEmpleadoAsignadoId()));
            solicitud.setEmpleadoAsignado(empleado);
        }
        if (dto.getChatbotAsignadoId() != null) {
            Chatbot chatbot = repositorioChatbot.findById(dto.getChatbotAsignadoId())
                    .orElseThrow(() -> new RuntimeException("Chatbot no encontrado con id: " + dto.getChatbotAsignadoId()));
            solicitud.setChatbotAsignado(chatbot);
        }
        if (dto.getServicioGeneradoId() != null) {
            Servicio servicio = repositorioServicio.findById(dto.getServicioGeneradoId())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + dto.getServicioGeneradoId()));
            solicitud.setServicioGenerado(servicio);
        }
    }
}
