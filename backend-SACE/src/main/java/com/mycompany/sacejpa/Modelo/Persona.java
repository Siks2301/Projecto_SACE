package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

/**
 * Representa a cualquier persona registrada en el Sistema de Atencion al
 * Cliente Empresarial (SACE). Es la clase base de la que heredan
 * {@link Cliente} y {@link Empleado} (estrategia de herencia JOINED: cada
 * subclase tiene su propia tabla enlazada por llave foranea al id de esta
 * tabla padre).
 */
@Entity
@Table(name = "persona")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_persona_discriminador", discriminatorType = DiscriminatorType.STRING)
public class Persona {

    public enum TipoUsuario {
        CLIENTE, ASESOR_VIAJES, COORDINADOR_DESTINO, ADMINISTRADOR
    }

    public enum EstadoAcceso {
        ACTIVA, INACTIVA, BLOQUEADA
    }

    public enum TipoDocumento {
        CC, CE, PASAPORTE, OTRO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "email")
    private String email;

    @Column(name = "telefono")
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario")
    private TipoUsuario tipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_acceso")
    private EstadoAcceso estadoAcceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    // Usado solo para el ingreso al sistema (login)
    @Column(name = "contrasenia")
    private String contrasenia;

    public Persona() {
    }

    public Persona(String nombre, String apellido, String email, String telefono,
            TipoUsuario tipoUsuario, EstadoAcceso estadoAcceso, String contrasenia) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.tipoUsuario = tipoUsuario;
        this.estadoAcceso = estadoAcceso;
        this.contrasenia = contrasenia;
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

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public EstadoAcceso getEstadoAcceso() {
        return estadoAcceso;
    }

    public void setEstadoAcceso(EstadoAcceso estadoAcceso) {
        this.estadoAcceso = estadoAcceso;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    @Override
    public String toString() {
        return "Persona{" + "id=" + id + ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' + ", email='" + email + '\'' +
                ", tipoUsuario=" + tipoUsuario + ", estadoAcceso=" + estadoAcceso + '}';
    }
}
