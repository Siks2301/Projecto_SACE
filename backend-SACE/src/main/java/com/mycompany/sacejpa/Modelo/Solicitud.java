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

    @Column(name = "asunto", columnDefinition = "TEXT")
    private String asunto;

    // El requerimiento del cliente es texto libre y puede pasar de 255
    // caracteres: ver migracion_textos_largos.sql. Sin columnDefinition, el
    // ddl-auto de Hibernate volveria a declararla varchar(255) y el error
    // reaparece con un mensaje que no explica la causa.
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad")
    private Prioridad prioridad;

    @Column(name = "categoria")
    private String categoria;

    // Numero de viajeros de la reserva. El precio del catalogo es POR PERSONA,
    // asi que el cobro se multiplica por esta cifra. Antes solo existia dentro
    // del texto libre del asunto/descripcion ("Reserva para 2 persona(s)"), que
    // no se podia validar ni usar para calcular: el pago cobraba una sola vez.
    // Se deja nullable a proposito: `ddl-auto=update` agrega la columna a una
    // tabla que ya tiene filas y un NOT NULL ahi tumba el arranque. El valor
    // nunca queda vacio en la practica porque @PrePersist lo completa y
    // migracion_pasajeros.sql remata la columna con DEFAULT 1 + CHECK 1..10.
    @Column(name = "pasajeros")
    private Integer pasajeros;

    /** Tope de viajeros por reserva, igual al que aplica el formulario web. */
    public static final int MAX_PASAJEROS = 10;

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

    /**
     * Asegura que toda solicitud nazca con un numero de viajeros valido.
     *
     * <p>Cubre los caminos que crean la entidad sin pasar por el DTO (datos
     * semilla, pruebas, importaciones): si el numero falta se rescata del texto
     * y, en ultimo caso, se asume una persona. Sin esto, la columna quedaria
     * nula y el cobro multiplicaria por nada.
     */
    @PrePersist
    private void completarPasajeros() {
        if (pasajeros == null || pasajeros < 1) {
            pasajeros = normalizarPasajeros(null, asunto, descripcion);
        }
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

    public Integer getPasajeros() {
        return pasajeros;
    }

    public void setPasajeros(Integer pasajeros) {
        this.pasajeros = pasajeros;
    }

    /**
     * Numero de viajeros que se debe cobrar.
     *
     * <p>Nunca devuelve menos de 1: una solicitud sin el dato (creada antes de
     * que existiera la columna) se cobra como una persona, igual que antes.
     *
     * @return viajeros efectivos, entre 1 y {@link #MAX_PASAJEROS}
     */
    public int pasajerosEfectivos() {
        Integer valor = pasajeros != null ? pasajeros : pasajerosDesdeTexto(asunto, descripcion);
        if (valor == null || valor < 1) {
            return 1;
        }
        return Math.min(valor, MAX_PASAJEROS);
    }

    /**
     * Normaliza el dato que llega del cliente al crear o editar una solicitud.
     *
     * <p>Si el DTO no trae el numero se intenta leer del texto libre, para no
     * perder las reservas que ya venian con "Pasajeros: 2" escrito a mano.
     *
     * @param valor       viajeros enviados en el cuerpo (puede ser null)
     * @param asunto      asunto de la solicitud
     * @param descripcion detalle de la solicitud
     * @return viajeros validados, entre 1 y {@link #MAX_PASAJEROS}
     */
    public static int normalizarPasajeros(Integer valor, String asunto, String descripcion) {
        Integer efectivo = valor != null ? valor : pasajerosDesdeTexto(asunto, descripcion);
        if (efectivo == null || efectivo < 1) {
            return 1;
        }
        return Math.min(efectivo, MAX_PASAJEROS);
    }

    /**
     * Busca el numero de viajeros dentro del texto de la solicitud.
     *
     * <p>Reconoce las formas que ya usa el sistema: "Pasajeros: 2" en la
     * descripcion y "Reserva para 2 persona(s)" en el asunto. Es solo un
     * respaldo para datos historicos; lo normal es que el numero venga aparte.
     *
     * @return viajeros encontrados, o {@code null} si el texto no los menciona
     */
    private static Integer pasajerosDesdeTexto(String asunto, String descripcion) {
        String texto = ((descripcion == null ? "" : descripcion) + " " + (asunto == null ? "" : asunto)).trim();
        if (texto.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)(?:pasajeros?|personas?|viajeros?)\\s*[:=]?\\s*(\\d{1,2})")
                .matcher(texto);
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        m = java.util.regex.Pattern.compile("(?i)para\\s+(\\d{1,2})\\s+(?:persona|viajero)")
                .matcher(texto);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
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
