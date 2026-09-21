package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.Date;

/**
 * Mensaje que queda registrado dentro del hilo de una {@link Solicitud}
 * (comentario del cliente, respuesta del empleado o del chatbot, o un
 * archivo adjunto).
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {

    public enum TipoMensaje {
        TEXTO, ARCHIVO, SISTEMA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "remitente")
    private String remitente;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha")
    private Date fecha;

    @Column(name = "contenido")
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoMensaje tipo;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    // Calificacion (1-10) que el cliente le da a este mensaje si es una
    // respuesta recibida, y comentario opcional de esa calificacion.
    // Quedan en null/0 mientras el mensaje no ha sido calificado.
    @Column(name = "calificacion")
    private Integer calificacion;

    @Column(name = "comentario_satisfaccion")
    private String comentarioSatisfaccion;

    // Solicitud a la que pertenece (registra) este mensaje
    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    public Mensaje() {
    }

    public Mensaje(String remitente, Date fecha, String contenido, TipoMensaje tipo,
            String nombreArchivo, String rutaArchivo, Solicitud solicitud) {
        this.remitente = remitente;
        this.fecha = fecha;
        this.contenido = contenido;
        this.tipo = tipo;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.solicitud = solicitud;
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

    public TipoMensaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensaje tipo) {
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

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
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

    @Override
    public String toString() {
        return "Mensaje{" + "id=" + id + ", remitente='" + remitente + '\'' +
                ", tipo=" + tipo + '}';
    }
}
