package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.ClienteDTOs;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Modelo.Solicitud;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClienteMapper {

    public static ClienteDTOs toDTO(Cliente cliente) {
        List<Long> solicitudesIds = cliente.getSolicitudesGeneradas() == null ? Collections.emptyList()
                : cliente.getSolicitudesGeneradas().stream().map(Solicitud::getId).collect(Collectors.toList());
        List<Long> serviciosIds = cliente.getServiciosContratados() == null ? Collections.emptyList()
                : cliente.getServiciosContratados().stream().map(Servicio::getId).collect(Collectors.toList());

        return new ClienteDTOs(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getTipoUsuario() != null ? cliente.getTipoUsuario().name() : null,
                cliente.getEstadoAcceso() != null ? cliente.getEstadoAcceso().name() : null,
                cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().name() : null,
                cliente.getNumeroDocumento(),
                cliente.getHistorialConsultas(),
                cliente.getPreferenciasComunicacion(),
                solicitudesIds,
                serviciosIds
        );
    }

    public static Cliente myEntity(ClienteDTOs dto) {
        Cliente cliente = new Cliente();
        cliente.setId(dto.getId());
        cliente.setNombre(dto.getNombre());
        cliente.setApellido(dto.getApellido());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefono(dto.getTelefono());
        // tipoUsuario y estadoAcceso NO se leen del cuerpo de la peticion:
        // los fija el servidor (ClienteServicio). Un cliente es siempre CLIENTE.
        if (dto.getTipoDocumento() != null) {
            cliente.setTipoDocumento(Cliente.TipoDocumento.valueOf(dto.getTipoDocumento()));
        }
        cliente.setNumeroDocumento(dto.getNumeroDocumento());
        cliente.setHistorialConsultas(dto.getHistorialConsultas());
        cliente.setPreferenciasComunicacion(dto.getPreferenciasComunicacion());
        return cliente;
    }
}
