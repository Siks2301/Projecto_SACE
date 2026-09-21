package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.PreguntaFrecuenteDTOs;
import com.mycompany.sacejpa.Modelo.Chatbot;
import com.mycompany.sacejpa.Modelo.PreguntaFrecuente;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PreguntaFrecuenteMapper {

    public static PreguntaFrecuenteDTOs toDTO(PreguntaFrecuente pregunta) {
        List<Long> chatbotsIds = pregunta.getChatbots() == null ? Collections.emptyList()
                : pregunta.getChatbots().stream().map(Chatbot::getId).collect(Collectors.toList());

        return new PreguntaFrecuenteDTOs(
                pregunta.getId(),
                pregunta.getPregunta(),
                pregunta.getRespuesta(),
                pregunta.getPalabrasClave(),
                pregunta.getCategoria(),
                chatbotsIds
        );
    }

    public static PreguntaFrecuente myEntity(PreguntaFrecuenteDTOs dto) {
        PreguntaFrecuente pregunta = new PreguntaFrecuente();
        pregunta.setId(dto.getId());
        pregunta.setPregunta(dto.getPregunta());
        pregunta.setRespuesta(dto.getRespuesta());
        pregunta.setPalabrasClave(dto.getPalabrasClave());
        pregunta.setCategoria(dto.getCategoria());
        return pregunta;
    }
}
