package com.mycompany.sacejpa.DTOs;

import java.util.List;

public class ChatbotDTOs {
    private Long id;
    private String nombre;
    private String version;
    private String estadoOperativo;
    private List<Long> solicitudesAtendidasIds;
    private List<Long> preguntasFrecuentesUsadasIds;

    public ChatbotDTOs() {
    }

    public ChatbotDTOs(Long id, String nombre, String version, String estadoOperativo,
            List<Long> solicitudesAtendidasIds, List<Long> preguntasFrecuentesUsadasIds) {
        this.id = id;
        this.nombre = nombre;
        this.version = version;
        this.estadoOperativo = estadoOperativo;
        this.solicitudesAtendidasIds = solicitudesAtendidasIds;
        this.preguntasFrecuentesUsadasIds = preguntasFrecuentesUsadasIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEstadoOperativo() {
        return estadoOperativo;
    }

    public void setEstadoOperativo(String estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    public List<Long> getSolicitudesAtendidasIds() {
        return solicitudesAtendidasIds;
    }

    public void setSolicitudesAtendidasIds(List<Long> solicitudesAtendidasIds) {
        this.solicitudesAtendidasIds = solicitudesAtendidasIds;
    }

    public List<Long> getPreguntasFrecuentesUsadasIds() {
        return preguntasFrecuentesUsadasIds;
    }

    public void setPreguntasFrecuentesUsadasIds(List<Long> preguntasFrecuentesUsadasIds) {
        this.preguntasFrecuentesUsadasIds = preguntasFrecuentesUsadasIds;
    }

    @Override
    public String toString() {
        return "ChatbotDTOs{" + "id=" + id + ", nombre='" + nombre + '\'' + ", estadoOperativo='" + estadoOperativo + '\'' + "}";
    }
}
