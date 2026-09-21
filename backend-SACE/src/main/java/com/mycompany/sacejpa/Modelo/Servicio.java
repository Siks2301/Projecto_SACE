package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa un servicio ofrecido por la empresa (paquete turistico, plan,
 * producto, etc.) que puede ser adquirido/poseido por uno o varios
 * {@link Cliente} y que puede ser generado a partir de una {@link Solicitud}.
 */
@Entity
@Table(name = "servicio")
public class Servicio {

    public enum TipoServicio {
        PAQUETE_TODO_INCLUIDO, SOLO_HOTEL, VUELO_HOTEL, TOUR_EXCURSION, PASADIA, ASISTENCIA_MEDICA, OTRO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio")
    private TipoServicio tipoServicio;

    // Dinero en BigDecimal para no acumular errores de redondeo en cobros.
    @Column(name = "precio", precision = 19, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion")
    private String duracion;

    @Column(name = "destino")
    private String destino;

    // Clientes que poseen/han contratado este servicio
    @ManyToMany(mappedBy = "serviciosContratados")
    private List<Cliente> clientes;

    // Solicitudes que dieron origen a este servicio
    @OneToMany(mappedBy = "servicioGenerado")
    private List<Solicitud> solicitudesQueLoGeneraron;

    public Servicio() {
    }

    public Servicio(String nombre, String descripcion, TipoServicio tipoServicio,
            BigDecimal precio, String duracion, String destino) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoServicio = tipoServicio;
        this.precio = precio;
        this.duracion = duracion;
        this.destino = destino;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoServicio getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(TipoServicio tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(List<Cliente> clientes) {
        this.clientes = clientes;
    }

    public List<Solicitud> getSolicitudesQueLoGeneraron() {
        return solicitudesQueLoGeneraron;
    }

    public void setSolicitudesQueLoGeneraron(List<Solicitud> solicitudesQueLoGeneraron) {
        this.solicitudesQueLoGeneraron = solicitudesQueLoGeneraron;
    }

    @Override
    public String toString() {
        return "Servicio{" + "id=" + id + ", nombre='" + nombre + '\'' +
                ", tipoServicio=" + tipoServicio + ", precio=" + precio + '}';
    }
}
