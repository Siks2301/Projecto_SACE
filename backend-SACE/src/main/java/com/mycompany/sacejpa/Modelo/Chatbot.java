package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.List;

/**
 * Chatbot que puede atender {@link Solicitud} de manera automatica y que
 * usa las {@link PreguntaFrecuente} registradas para responder.
 */
@Entity
@Table(name = "chatbot")
public class Chatbot {

    public enum EstadoOperativo {
        ACTIVO, INACTIVO, MANTENIMIENTO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "version")
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo")
    private EstadoOperativo estadoOperativo;

    // Solicitudes que este chatbot ha atendido
    @OneToMany(mappedBy = "chatbotAsignado")
    private List<Solicitud> solicitudesAtendidas;

    // Preguntas frecuentes que el chatbot usa para responder
    @ManyToMany
    @JoinTable(
            name = "chatbot_pregunta_frecuente",
            joinColumns = @JoinColumn(name = "chatbot_id"),
            inverseJoinColumns = @JoinColumn(name = "pregunta_frecuente_id"))
    private List<PreguntaFrecuente> preguntasFrecuentesUsadas;

    public Chatbot() {
    }

    public Chatbot(String nombre, String version, EstadoOperativo estadoOperativo) {
        this.nombre = nombre;
        this.version = version;
        this.estadoOperativo = estadoOperativo;
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

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public void setEstadoOperativo(EstadoOperativo estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    public List<Solicitud> getSolicitudesAtendidas() {
        return solicitudesAtendidas;
    }

    public void setSolicitudesAtendidas(List<Solicitud> solicitudesAtendidas) {
        this.solicitudesAtendidas = solicitudesAtendidas;
    }

    public List<PreguntaFrecuente> getPreguntasFrecuentesUsadas() {
        return preguntasFrecuentesUsadas;
    }

    public void setPreguntasFrecuentesUsadas(List<PreguntaFrecuente> preguntasFrecuentesUsadas) {
        this.preguntasFrecuentesUsadas = preguntasFrecuentesUsadas;
    }

    @Override
    public String toString() {
        return "Chatbot{" + "id=" + id + ", nombre='" + nombre + '\'' +
                ", version='" + version + '\'' + ", estadoOperativo=" + estadoOperativo + '}';
    }
}
