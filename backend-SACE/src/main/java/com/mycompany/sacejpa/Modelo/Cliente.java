package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.List;

/**
 * Cliente de la empresa. Hereda los datos comunes de {@link Persona} y
 * agrega la informacion propia de la relacion comercial: su historial de
 * consultas y sus preferencias de contacto.
 *
 * Relaciones (segun el diagrama SACE):
 * - Un Cliente "genera" varias {@link Solicitud}.
 * - Un Cliente "posee" (tiene contratados) varios {@link Servicio}.
 */
@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "id")
public class Cliente extends Persona {

    @Column(name = "historial_consultas")
    private String historialConsultas;

    @Column(name = "preferencias_comunicacion")
    private String preferenciasComunicacion;

    // Solicitudes generadas por este cliente
    @OneToMany(mappedBy = "cliente")
    private List<Solicitud> solicitudesGeneradas;

    // Servicios que el cliente posee/ha contratado
    @ManyToMany
    @JoinTable(
            name = "cliente_servicio",
            joinColumns = @JoinColumn(name = "cliente_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id"))
    private List<Servicio> serviciosContratados;

    public Cliente() {
        super();
    }

    public Cliente(String nombre, String apellido, String email, String telefono,
            TipoUsuario tipoUsuario, EstadoAcceso estadoAcceso, String contrasenia,
            String historialConsultas, String preferenciasComunicacion) {
        super(nombre, apellido, email, telefono, tipoUsuario, estadoAcceso, contrasenia);
        this.historialConsultas = historialConsultas;
        this.preferenciasComunicacion = preferenciasComunicacion;
    }

    public String getHistorialConsultas() {
        return historialConsultas;
    }

    public void setHistorialConsultas(String historialConsultas) {
        this.historialConsultas = historialConsultas;
    }

    public String getPreferenciasComunicacion() {
        return preferenciasComunicacion;
    }

    public void setPreferenciasComunicacion(String preferenciasComunicacion) {
        this.preferenciasComunicacion = preferenciasComunicacion;
    }

    public List<Solicitud> getSolicitudesGeneradas() {
        return solicitudesGeneradas;
    }

    public void setSolicitudesGeneradas(List<Solicitud> solicitudesGeneradas) {
        this.solicitudesGeneradas = solicitudesGeneradas;
    }

    public List<Servicio> getServiciosContratados() {
        return serviciosContratados;
    }

    public void setServiciosContratados(List<Servicio> serviciosContratados) {
        this.serviciosContratados = serviciosContratados;
    }

    @Override
    public String toString() {
        return "Cliente{" + "id=" + getId() + ", nombre='" + getNombre() + '\'' + '}';
    }
}
