package com.mycompany.sacejpa.DTOs;

import java.util.List;

public class ClienteDTOs {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String tipoUsuario;
    private String estadoAcceso;
    private String tipoDocumento;
    private String numeroDocumento;
    private String historialConsultas;
    private String preferenciasComunicacion;
    private List<Long> solicitudesGeneradasIds;
    private List<Long> serviciosContratadosIds;

    public ClienteDTOs() {
    }

    public ClienteDTOs(Long id, String nombre, String apellido, String email, String telefono,
            String tipoUsuario, String estadoAcceso, String tipoDocumento, String numeroDocumento,
            String historialConsultas, String preferenciasComunicacion,
            List<Long> solicitudesGeneradasIds, List<Long> serviciosContratadosIds) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.tipoUsuario = tipoUsuario;
        this.estadoAcceso = estadoAcceso;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.historialConsultas = historialConsultas;
        this.preferenciasComunicacion = preferenciasComunicacion;
        this.solicitudesGeneradasIds = solicitudesGeneradasIds;
        this.serviciosContratadosIds = serviciosContratadosIds;
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

    public List<Long> getSolicitudesGeneradasIds() {
        return solicitudesGeneradasIds;
    }

    public void setSolicitudesGeneradasIds(List<Long> solicitudesGeneradasIds) {
        this.solicitudesGeneradasIds = solicitudesGeneradasIds;
    }

    public List<Long> getServiciosContratadosIds() {
        return serviciosContratadosIds;
    }

    public void setServiciosContratadosIds(List<Long> serviciosContratadosIds) {
        this.serviciosContratadosIds = serviciosContratadosIds;
    }

    @Override
    public String toString() {
        return "ClienteDTOs{" + "id=" + id + ", nombre='" + nombre + '\'' + ", apellido='" + apellido + '\'' + "}";
    }
}
