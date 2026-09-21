package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.ChatbotDTOs;
import com.mycompany.sacejpa.Mapper.ChatbotMapper;
import com.mycompany.sacejpa.Modelo.Chatbot;
import com.mycompany.sacejpa.Modelo.PreguntaFrecuente;
import com.mycompany.sacejpa.Repositorio.Reposi_Chatbot;
import com.mycompany.sacejpa.Repositorio.Reposi_PreguntaFrecuente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotServicio {

    @Autowired
    private Reposi_Chatbot repositorioChatbot;

    @Autowired
    private Reposi_PreguntaFrecuente repositorioPreguntaFrecuente;

    public List<ChatbotDTOs> listarChatbots() {
        return repositorioChatbot.findAll().stream()
                .map(ChatbotMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ChatbotDTOs buscarChatbot(Long id) {
        Chatbot chatbot = repositorioChatbot.findById(id)
                .orElseThrow(() -> new RuntimeException("Chatbot no encontrado con id: " + id));
        return ChatbotMapper.toDTO(chatbot);
    }

    public ChatbotDTOs insertarChatbot(ChatbotDTOs dto) {
        Chatbot chatbot = ChatbotMapper.myEntity(dto);
        asignarPreguntas(chatbot, dto);
        Chatbot guardado = repositorioChatbot.save(chatbot);
        return ChatbotMapper.toDTO(guardado);
    }

    public ChatbotDTOs servactualiza(Long id, ChatbotDTOs dto) {
        Optional<Chatbot> existe = repositorioChatbot.findById(id);
        if (existe.isPresent()) {
            Chatbot chatbot = existe.get();
            chatbot.setNombre(dto.getNombre());
            chatbot.setVersion(dto.getVersion());
            if (dto.getEstadoOperativo() != null) {
                chatbot.setEstadoOperativo(Chatbot.EstadoOperativo.valueOf(dto.getEstadoOperativo()));
            }
            asignarPreguntas(chatbot, dto);
            Chatbot actualizado = repositorioChatbot.save(chatbot);
            return ChatbotMapper.toDTO(actualizado);
        } else {
            throw new RuntimeException("Chatbot no encontrado con id: " + id);
        }
    }

    public void eliminarChatbot(Long id) {
        if (!repositorioChatbot.existsById(id)) {
            throw new RuntimeException("Chatbot no encontrado con id: " + id);
        }
        repositorioChatbot.deleteById(id);
    }

    private void asignarPreguntas(Chatbot chatbot, ChatbotDTOs dto) {
        if (dto.getPreguntasFrecuentesUsadasIds() != null) {
            List<PreguntaFrecuente> preguntas = dto.getPreguntasFrecuentesUsadasIds().stream()
                    .map(preguntaId -> repositorioPreguntaFrecuente.findById(preguntaId)
                            .orElseThrow(() -> new RuntimeException("Pregunta frecuente no encontrada con id: " + preguntaId)))
                    .collect(Collectors.toList());
            chatbot.setPreguntasFrecuentesUsadas(preguntas);
        }
    }
}
