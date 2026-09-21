package com.mycompany.sacejpa.DTOs;

/**
 * Respuesta unificada de /api/auth/login. Como un login puede corresponder
 * a un Cliente o a un Empleado (dos entidades JPA distintas que heredan de
 * Persona), se devuelve siempre esta misma forma con los campos que el
 * frontend necesita, sin importar cual de las dos tablas respondio.
 */
public class LoginResponseDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String tipoUsuario;
    private String estadoAcceso;
    private String estadoDisponibilidad;
    private String token;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(Long id, String nombre, String apellido, String email, String tipoUsuario) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.tipoUsuario = tipoUsuario;
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

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEstadoAcceso() {
        return estadoAcceso;
    }

    public void setEstadoAcceso(String estadoAcceso) {
        this.estadoAcceso = estadoAcceso;
    }

    public String getEstadoDisponibilidad() {
        return estadoDisponibilidad;
    }

    public void setEstadoDisponibilidad(String estadoDisponibilidad) {
        this.estadoDisponibilidad = estadoDisponibilidad;
    }
}
