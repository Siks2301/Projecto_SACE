package com.mycompany.sacejpa.DTOs;

import java.util.List;

public class EmpleadoDTOs {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String tipoUsuario;
    private String estadoAcceso;
    private String tipoDocumento;
    private String numeroDocumento;
    private String departamento;
    private String cargoEspecifico;
    private String estadoDisponibilidad;
    private List<Long> solicitudesAtendidasIds;

    // Solo se usa al crear/actualizar (login inicial del empleado). El mapper
    // de salida (toDTO) nunca la llena, para no exponer nunca el hash en las
    // respuestas de GET/listado.
    private String contrasenia;

    public EmpleadoDTOs() {
    }

    public EmpleadoDTOs(Long id, String nombre, String apellido, String email, String telefono,
            String tipoUsuario, String estadoAcceso, String tipoDocumento, String numeroDocumento,
            String departamento, String cargoEspecifico, String estadoDisponibilidad,
            List<Long> solicitudesAtendidasIds) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.tipoUsuario = tipoUsuario;
        this.estadoAcceso = estadoAcceso;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.departamento = departamento;
        this.cargoEspecifico = cargoEspecifico;
        this.estadoDisponibilidad = estadoDisponibilidad;
        this.solicitudesAtendidasIds = solicitudesAtendidasIds;
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getEstadoAcceso() {
        return estadoAcceso;
    }

    public void setEstadoAcceso(String estadoAcceso) {
        this.estadoAcceso = estadoAcceso;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
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

    public String getEstadoDisponibilidad() {
        return estadoDisponibilidad;
    }

    public void setEstadoDisponibilidad(String estadoDisponibilidad) {
        this.estadoDisponibilidad = estadoDisponibilidad;
    }

    public List<Long> getSolicitudesAtendidasIds() {
        return solicitudesAtendidasIds;
    }

    public void setSolicitudesAtendidasIds(List<Long> solicitudesAtendidasIds) {
        this.solicitudesAtendidasIds = solicitudesAtendidasIds;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    @Override
    public String toString() {
        return "EmpleadoDTOs{" + "id=" + id + ", nombre='" + nombre + '\'' + ", departamento='" + departamento + '\'' + "}";
    }
}
