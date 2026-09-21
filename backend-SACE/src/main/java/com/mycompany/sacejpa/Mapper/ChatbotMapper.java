package com.mycompany.sacejpa.Mapper;

import com.mycompany.sacejpa.DTOs.ChatbotDTOs;
import com.mycompany.sacejpa.Modelo.Chatbot;
import com.mycompany.sacejpa.Modelo.PreguntaFrecuente;
import com.mycompany.sacejpa.Modelo.Solicitud;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChatbotMapper {

    public static ChatbotDTOs toDTO(Chatbot chatbot) {
        List<Long> solicitudesIds = chatbot.getSolicitudesAtendidas() == null ? Collections.emptyList()
                : chatbot.getSolicitudesAtendidas().stream().map(Solicitud::getId).collect(Collectors.toList());
        List<Long> preguntasIds = chatbot.getPreguntasFrecuentesUsadas() == null ? Collections.emptyList()
                : chatbot.getPreguntasFrecuentesUsadas().stream().map(PreguntaFrecuente::getId).collect(Collectors.toList());

        return new ChatbotDTOs(
                chatbot.getId(),
                chatbot.getNombre(),
                chatbot.getVersion(),
                chatbot.getEstadoOperativo() != null ? chatbot.getEstadoOperativo().name() : null,
                solicitudesIds,
                preguntasIds
        );
    }

    // Nota: preguntasFrecuentesUsadas se asigna en la capa de Servicio.
    public static Chatbot myEntity(ChatbotDTOs dto) {
        Chatbot chatbot = new Chatbot();
        chatbot.setId(dto.getId());
        chatbot.setNombre(dto.getNombre());
        chatbot.setVersion(dto.getVersion());
        if (dto.getEstadoOperativo() != null) {
            chatbot.setEstadoOperativo(Chatbot.EstadoOperativo.valueOf(dto.getEstadoOperativo()));
        }
        return chatbot;
    }
}
