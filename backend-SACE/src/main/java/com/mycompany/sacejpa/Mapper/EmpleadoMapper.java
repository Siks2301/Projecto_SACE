package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.EmpleadoDTOs;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Modelo.Solicitud;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmpleadoMapper {

    public static EmpleadoDTOs toDTO(Empleado empleado) {
        List<Long> solicitudesIds = empleado.getSolicitudesAtendidas() == null ? Collections.emptyList()
                : empleado.getSolicitudesAtendidas().stream().map(Solicitud::getId).collect(Collectors.toList());

        return new EmpleadoDTOs(
                empleado.getId(),
                empleado.getNombre(),
                empleado.getApellido(),
                empleado.getEmail(),
                empleado.getTelefono(),
                empleado.getTipoUsuario() != null ? empleado.getTipoUsuario().name() : null,
                empleado.getEstadoAcceso() != null ? empleado.getEstadoAcceso().name() : null,
                empleado.getTipoDocumento() != null ? empleado.getTipoDocumento().name() : null,
                empleado.getNumeroDocumento(),
                empleado.getDepartamento(),
                empleado.getCargoEspecifico(),
                empleado.getEstadoDisponibilidad() != null ? empleado.getEstadoDisponibilidad().name() : null,
                solicitudesIds
        );
    }

    public static Empleado myEntity(EmpleadoDTOs dto) {
        Empleado empleado = new Empleado();
        empleado.setId(dto.getId());
        empleado.setNombre(dto.getNombre());
        empleado.setApellido(dto.getApellido());
        empleado.setEmail(dto.getEmail());
        empleado.setTelefono(dto.getTelefono());
        // tipoUsuario y estadoAcceso NO se leen del cuerpo de la peticion:
        // son campos de privilegio que fija el servidor (ver EmpleadoServicio).
        if (dto.getTipoDocumento() != null) {
            empleado.setTipoDocumento(Empleado.TipoDocumento.valueOf(dto.getTipoDocumento()));
        }
        empleado.setNumeroDocumento(dto.getNumeroDocumento());
        empleado.setDepartamento(dto.getDepartamento());
        empleado.setCargoEspecifico(dto.getCargoEspecifico());
        if (dto.getEstadoDisponibilidad() != null) {
            empleado.setEstadoDisponibilidad(Empleado.EstadoDisponibilidad.valueOf(dto.getEstadoDisponibilidad()));
        }
        return empleado;
    }
}
