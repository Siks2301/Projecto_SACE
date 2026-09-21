package com.mycompany.sacejpa.DTOs;

import java.util.Date;

public class MensajeDTOs {
    private Long id;
    private String remitente;
    private Date fecha;
    private String contenido;
    private String tipo;
    private String nombreArchivo;
    private String rutaArchivo;
    private Integer calificacion;
    private String comentarioSatisfaccion;
    private Long solicitudId;

    public MensajeDTOs() {
    }

    public MensajeDTOs(Long id, String remitente, Date fecha, String contenido, String tipo,
            String nombreArchivo, String rutaArchivo, Integer calificacion, String comentarioSatisfaccion,
            Long solicitudId) {
        this.id = id;
        this.remitente = remitente;
        this.fecha = fecha;
        this.contenido = contenido;
        this.tipo = tipo;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.calificacion = calificacion;
        this.comentarioSatisfaccion = comentarioSatisfaccion;
        this.solicitudId = solicitudId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentarioSatisfaccion() {
        return comentarioSatisfaccion;
    }

    public void setComentarioSatisfaccion(String comentarioSatisfaccion) {
        this.comentarioSatisfaccion = comentarioSatisfaccion;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    @Override
    public String toString() {
        return "MensajeDTOs{" + "id=" + id + ", remitente='" + remitente + '\'' + ", tipo='" + tipo + '\'' + "}";
    }
}
