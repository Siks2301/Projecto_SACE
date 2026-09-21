package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.List;

/**
 * Empleado de la empresa encargado de atender las {@link Solicitud} de los
 * clientes. Hereda los datos comunes de {@link Persona}.
 *
 * Relacion: un Empleado "atiende" varias Solicitudes.
 */
@Entity
@Table(name = "empleado")
@PrimaryKeyJoinColumn(name = "id")
public class Empleado extends Persona {

    public enum EstadoDisponibilidad {
        DISPONIBLE, OCUPADO, EN_PAUSA, FUERA_DE_TURNO
    }

    @Column(name = "departamento")
    private String departamento;

    @Column(name = "cargo_especifico")
    private String cargoEspecifico;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_disponibilidad")
    private EstadoDisponibilidad estadoDisponibilidad;

    // Solicitudes que este empleado tiene asignadas para atender
    @OneToMany(mappedBy = "empleadoAsignado")
    private List<Solicitud> solicitudesAtendidas;

    public Empleado() {
        super();
    }

    public Empleado(String nombre, String apellido, String email, String telefono,
            TipoUsuario tipoUsuario, EstadoAcceso estadoAcceso, String contrasenia,
            String departamento, EstadoDisponibilidad estadoDisponibilidad) {
        super(nombre, apellido, email, telefono, tipoUsuario, estadoAcceso, contrasenia);
        this.departamento = departamento;
        this.estadoDisponibilidad = estadoDisponibilidad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getCargoEspecifico() {
        return cargoEspecifico;
    }

    public void setCargoEspecifico(String cargoEspecifico) {
        this.cargoEspecifico = cargoEspecifico;
    }

    public EstadoDisponibilidad getEstadoDisponibilidad() {
        return estadoDisponibilidad;
    }

    public void setEstadoDisponibilidad(EstadoDisponibilidad estadoDisponibilidad) {
        this.estadoDisponibilidad = estadoDisponibilidad;
    }

    public List<Solicitud> getSolicitudesAtendidas() {
        return solicitudesAtendidas;
    }

    public void setSolicitudesAtendidas(List<Solicitud> solicitudesAtendidas) {
        this.solicitudesAtendidas = solicitudesAtendidas;
    }

    @Override
    public String toString() {
        return "Empleado{" + "id=" + getId() + ", nombre='" + getNombre() + '\'' +
                ", departamento='" + departamento + '\'' + ", cargoEspecifico='" + cargoEspecifico + '\'' + '}';
    }
}
