package com.mycompany.sacejpa.DTOs;

import java.util.Date;
import java.util.List;

public class SolicitudDTOs {
    private Long id;
    private Date fechaCreacion;
    private String titulo;
    private String asunto;
    private String descripcion;
    private String estado;
    private String prioridad;
    private String categoria;
    private Long clienteId;
    private String clienteNombre;
    private String clienteCorreo;
    private String clienteTelefono;
    private Long empleadoAsignadoId;
    private Long chatbotAsignadoId;
    private Long servicioGeneradoId;
    private List<Long> mensajesIds;

    public SolicitudDTOs() {
    }

    public SolicitudDTOs(Long id, Date fechaCreacion, String titulo, String asunto, String descripcion,
            String estado, String prioridad, String categoria, Long clienteId, String clienteNombre,
            String clienteCorreo, String clienteTelefono, Long empleadoAsignadoId,
            Long chatbotAsignadoId, Long servicioGeneradoId, List<Long> mensajesIds) {
        this.id = id;
        this.fechaCreacion = fechaCreacion;
        this.titulo = titulo;
        this.asunto = asunto;
        this.descripcion = descripcion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.categoria = categoria;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteCorreo = clienteCorreo;
        this.clienteTelefono = clienteTelefono;
        this.empleadoAsignadoId = empleadoAsignadoId;
        this.chatbotAsignadoId = chatbotAsignadoId;
        this.servicioGeneradoId = servicioGeneradoId;
        this.mensajesIds = mensajesIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteCorreo() {
        return clienteCorreo;
    }

    public void setClienteCorreo(String clienteCorreo) {
        this.clienteCorreo = clienteCorreo;
    }

    public String getClienteTelefono() {
        return clienteTelefono;
    }

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
    }

    public Long getEmpleadoAsignadoId() {
        return empleadoAsignadoId;
    }

    public void setEmpleadoAsignadoId(Long empleadoAsignadoId) {
        this.empleadoAsignadoId = empleadoAsignadoId;
    }

    public Long getChatbotAsignadoId() {
        return chatbotAsignadoId;
    }

    public void setChatbotAsignadoId(Long chatbotAsignadoId) {
        this.chatbotAsignadoId = chatbotAsignadoId;
    }

    public Long getServicioGeneradoId() {
        return servicioGeneradoId;
    }

    public void setServicioGeneradoId(Long servicioGeneradoId) {
        this.servicioGeneradoId = servicioGeneradoId;
    }

    public List<Long> getMensajesIds() {
        return mensajesIds;
    }

    public void setMensajesIds(List<Long> mensajesIds) {
        this.mensajesIds = mensajesIds;
    }

    @Override
    public String toString() {
        return "SolicitudDTOs{" + "id=" + id + ", titulo='" + titulo + '\'' + ", estado='" + estado + '\'' + "}";
    }
}
