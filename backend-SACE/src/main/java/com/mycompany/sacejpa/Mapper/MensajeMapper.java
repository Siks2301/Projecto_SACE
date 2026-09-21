package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.MensajeDTOs;
import com.mycompany.sacejpa.Modelo.Mensaje;

public class MensajeMapper {

    public static MensajeDTOs toDTO(Mensaje mensaje) {
        return new MensajeDTOs(
                mensaje.getId(),
                mensaje.getRemitente(),
                mensaje.getFecha(),
                mensaje.getContenido(),
                mensaje.getTipo() != null ? mensaje.getTipo().name() : null,
                mensaje.getNombreArchivo(),
                mensaje.getRutaArchivo(),
                mensaje.getCalificacion(),
                mensaje.getComentarioSatisfaccion(),
                mensaje.getSolicitud() != null ? mensaje.getSolicitud().getId() : null
        );
    }

    // Nota: solicitud se asigna en la capa de Servicio.
    public static Mensaje myEntity(MensajeDTOs dto) {
        Mensaje mensaje = new Mensaje();
        mensaje.setId(dto.getId());
        mensaje.setRemitente(dto.getRemitente());
        mensaje.setFecha(dto.getFecha());
        mensaje.setContenido(dto.getContenido());
        if (dto.getTipo() != null) {
            mensaje.setTipo(Mensaje.TipoMensaje.valueOf(dto.getTipo()));
        }
        mensaje.setNombreArchivo(dto.getNombreArchivo());
        mensaje.setRutaArchivo(dto.getRutaArchivo());
        mensaje.setCalificacion(dto.getCalificacion());
        mensaje.setComentarioSatisfaccion(dto.getComentarioSatisfaccion());
        return mensaje;
    }
}
