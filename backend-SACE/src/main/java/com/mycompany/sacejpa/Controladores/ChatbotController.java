package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.ChatbotDTOs;
import com.mycompany.sacejpa.Servicios.ChatbotServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbots")
public class ChatbotController {
    @Autowired
    private ChatbotServicio chatbotServicio;

    @GetMapping
    public List<ChatbotDTOs> ListarChatbots() {
        return chatbotServicio.listarChatbots();
    }

    @GetMapping("/{id}")
    public ChatbotDTOs BuscarChatbot(@PathVariable Long id) {
        return chatbotServicio.buscarChatbot(id);
    }

    @PostMapping
    public ChatbotDTOs InsertarChatbot(@RequestBody ChatbotDTOs dto) {
        return chatbotServicio.insertarChatbot(dto);
    }

    @PutMapping("/{id}")
    public ChatbotDTOs ActualizaChatbot(@PathVariable Long id, @RequestBody ChatbotDTOs dto) {
        return chatbotServicio.servactualiza(id, dto);
    }

    @DeleteMapping("/{id}")
    public void EliminaChatbot(@PathVariable Long id) {
        chatbotServicio.eliminarChatbot(id);
    }
}
