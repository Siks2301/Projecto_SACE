package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.ServicioDTOs;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Modelo.Solicitud;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ServicioMapper {

    public static ServicioDTOs toDTO(Servicio servicio) {
        List<Long> clientesIds = servicio.getClientes() == null ? Collections.emptyList()
                : servicio.getClientes().stream().map(Cliente::getId).collect(Collectors.toList());
        List<Long> solicitudesIds = servicio.getSolicitudesQueLoGeneraron() == null ? Collections.emptyList()
                : servicio.getSolicitudesQueLoGeneraron().stream().map(Solicitud::getId).collect(Collectors.toList());

        return new ServicioDTOs(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getTipoServicio() != null ? servicio.getTipoServicio().name() : null,
                servicio.getPrecio(),
                servicio.getDuracion(),
                servicio.getDestino(),
                clientesIds,
                solicitudesIds
        );
    }

    public static Servicio myEntity(ServicioDTOs dto) {
        Servicio servicio = new Servicio();
        servicio.setId(dto.getId());
        servicio.setNombre(dto.getNombre());
        servicio.setDescripcion(dto.getDescripcion());
        if (dto.getTipoServicio() != null) {
            servicio.setTipoServicio(Servicio.TipoServicio.valueOf(dto.getTipoServicio()));
        }
        if (dto.getPrecio() != null) {
            servicio.setPrecio(dto.getPrecio());
        }
        servicio.setDuracion(dto.getDuracion());
        servicio.setDestino(dto.getDestino());
        return servicio;
    }
}
