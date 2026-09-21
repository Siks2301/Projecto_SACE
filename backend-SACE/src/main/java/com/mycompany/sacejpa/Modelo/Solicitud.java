package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

/**
 * Solicitud (PQR / requerimiento) que un {@link Cliente} genera dentro del
 * SACE. Puede ser atendida por un {@link Empleado} o por el {@link Chatbot},
 * puede generar un {@link Servicio}, y acumula {@link Mensaje} en su hilo
 * de conversacion.
 */
@Entity
@Table(name = "solicitud")
public class Solicitud {

    public enum Estado {
        PENDIENTE, EN_PROCESO, RESUELTA, CANCELADA
    }

    public enum Prioridad {
        BAJA, MEDIA, ALTA, URGENTE
    }

    public enum Categoria {
        COTIZACION, RESERVA, CAMBIO_FECHA, CANCELACION_REEMBOLSO, PQR_SERVICIO, CONSULTA_GENERAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion")
    private Date fechaCreacion;

    @Column(name = "titulo")
    private String titulo;

    @Column(name = "asunto")
    private String asunto;

    @Column(name = "descripcion")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad")
    private Prioridad prioridad;

    @Column(name = "categoria")
    private String categoria;

    // Cliente que genera la solicitud
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Empleado que atiende la solicitud (puede ser null si aun no se asigna)
    @ManyToOne
    @JoinColumn(name = "empleado_asignado_id")
    private Empleado empleadoAsignado;

    // Chatbot que atiende la solicitud (puede ser null si la atiende un empleado)
    @ManyToOne
    @JoinColumn(name = "chatbot_asignado_id")
    private Chatbot chatbotAsignado;

    // Servicio que se genera como resultado de esta solicitud (puede ser null)
    @ManyToOne
    @JoinColumn(name = "servicio_generado_id")
    private Servicio servicioGenerado;

    // Mensajes que se registran dentro de esta solicitud
    @OneToMany(mappedBy = "solicitud")
    @OrderBy("fecha ASC")
    private List<Mensaje> mensajes;

    public Solicitud() {
    }

    public Solicitud(Date fechaCreacion, String titulo, String descripcion, Estado estado,
            Prioridad prioridad, String categoria, Cliente cliente) {
        this(fechaCreacion, titulo, null, descripcion, estado, prioridad, categoria, cliente);
    }

    public Solicitud(Date fechaCreacion, String titulo, String asunto, String descripcion, Estado estado,
            Prioridad prioridad, String categoria, Cliente cliente) {
        this.fechaCreacion = fechaCreacion;
        this.titulo = titulo;
        this.asunto = asunto;
        this.descripcion = descripcion;
        this.estado = estado;
        this.prioridad = prioridad;
        this.categoria = categoria;
        this.cliente = cliente;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Empleado getEmpleadoAsignado() {
        return empleadoAsignado;
    }

    public void setEmpleadoAsignado(Empleado empleadoAsignado) {
        this.empleadoAsignado = empleadoAsignado;
    }

    public Chatbot getChatbotAsignado() {
        return chatbotAsignado;
    }

    public void setChatbotAsignado(Chatbot chatbotAsignado) {
        this.chatbotAsignado = chatbotAsignado;
    }

    public Servicio getServicioGenerado() {
        return servicioGenerado;
    }

    public void setServicioGenerado(Servicio servicioGenerado) {
        this.servicioGenerado = servicioGenerado;
    }

    public List<Mensaje> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = mensajes;
    }

    @Override
    public String toString() {
        return "Solicitud{" + "id=" + id + ", titulo='" + titulo + '\'' +
                ", estado=" + estado + ", prioridad=" + prioridad +
                ", cliente=" + (cliente != null ? cliente.getNombre() : "N/A") + '}';
    }
}
