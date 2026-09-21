package com.mycompany.sacejpa.DTOs;

import java.util.List;

public class PreguntaFrecuenteDTOs {
    private Long id;
    private String pregunta;
    private String respuesta;
    private String palabrasClave;
    private String categoria;
    private List<Long> chatbotsIds;

    public PreguntaFrecuenteDTOs() {
    }

    public PreguntaFrecuenteDTOs(Long id, String pregunta, String respuesta, String palabrasClave,
            String categoria, List<Long> chatbotsIds) {
        this.id = id;
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.palabrasClave = palabrasClave;
        this.categoria = categoria;
        this.chatbotsIds = chatbotsIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getPalabrasClave() {
        return palabrasClave;
    }

    public void setPalabrasClave(String palabrasClave) {
        this.palabrasClave = palabrasClave;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public List<Long> getChatbotsIds() {
        return chatbotsIds;
    }

    public void setChatbotsIds(List<Long> chatbotsIds) {
        this.chatbotsIds = chatbotsIds;
    }

    @Override
    public String toString() {
        return "PreguntaFrecuenteDTOs{" + "id=" + id + ", pregunta='" + pregunta + '\'' + "}";
    }
}
