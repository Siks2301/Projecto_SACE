package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Entidad Pago: una orden de cobro de SACE y su transaccion en la pasarela.
 *
 * <p>Cambio de modelo importante respecto a la version anterior:
 *
 * <ul>
 *   <li><b>El monto ya no viene del cliente.</b> Lo calcula el backend desde el
 *       precio del servicio de la solicitud. Antes el navegador enviaba el valor
 *       y el servidor lo aceptaba tal cual, asi que un cliente podia pagar
 *       $1 por un tour de dos millones.</li>
 *   <li><b>El pago nace PENDIENTE</b>, no confirmado. Solo se confirma cuando la
 *       pasarela lo avisa (webhook) o cuando el backend lo consulta. Antes el
 *       sistema declaraba el pago exitoso en el mismo instante en que se
 *       recibia la peticion, sin que hubiera ocurrido transaccion alguna.</li>
 *   <li><b>Se guardan los datos de trazabilidad</b> de la pasarela
 *       (referencia, identificador de transaccion, codigo de autorizacion) para
 *       poder conciliar meses despues.</li>
 *   <li><b>Sin llave de destino hardcodeada.</b> Ya no existe un "@VXM301"
 *       fijo: la cuenta que recibe el dinero es la de la pasarela y no un dato
 *       de negocio que viva en el codigo.</li>
 * </ul>
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    /**
     * Monto cobrado, en pesos. Lo fija el backend a partir del precio del
     * servicio; nunca se lee del cuerpo de la peticion.
     */
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "COP";

    /**
     * Estado del pago. Persistido como texto para poder leer los registros
     * historicos y para que un estado nuevo no requiera cambiar la base.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private PagoEstado estado = PagoEstado.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private MetodoPago metodoPago = MetodoPago.PSE;

    /**
     * Referencia unica de la transaccion.
     *
     * <p>Cumple dos funciones: identifica el pago ante la pasarela y sirve de
     * <b>clave de idempotencia</b>. Gracias a ella, si el navegador reintenta la
     * peticion o el usuario hace doble clic, el sistema reconoce que ya existe
     * una orden para esa solicitud y no cobra dos veces.
     */
    @Column(name = "referencia", nullable = false, length = 64, unique = true)
    private String referencia;

    /** Nombre de la pasarela que proceso el cobro: WOMPI o SIMULADA. */
    @Column(name = "pasarela", nullable = false, length = 30)
    private String pasarela;

    /** Identificador de la transaccion dentro de la pasarela. */
    @Column(name = "id_transaccion_pasarela", length = 64, unique = true)
    private String idTransaccionPasarela;

    /** Codigo de autorizacion bancaria devuelto por la pasarela. */
    @Column(name = "codigo_autorizacion", length = 30)
    private String codigoAutorizacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion = new Date();

    /** Instante en que la pasarela confirmo el cobro. Null mientras siga pendiente. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_aprobacion")
    private Date fechaAprobacion;

    @Column(name = "url_pdf", length = 255)
    private String urlPdf;

    @Column(name = "correo_notificado", nullable = false)
    private Boolean correoNotificado = false;

    @Column(name = "notas", length = 500)
    private String notas;

    /**
     * Constructor vacio exigido por JPA.
     *
     * <p>Un pago nuevo nace SIEMPRE pendiente y sin transaccion asociada: todavia
     * no se sabe si la pasarela lo va a aprobar. Dejar el estado en confirmado
     * por defecto seria exactamente el error que se corrige aqui.
     */
    public Pago() {
        this.estado = PagoEstado.PENDIENTE;
        this.metodoPago = MetodoPago.PSE;
        this.moneda = "COP";
        this.fechaCreacion = new Date();
        this.correoNotificado = false;
    }

    public Pago(Solicitud solicitud, Cliente cliente, BigDecimal monto, MetodoPago metodoPago) {
        this();
        this.solicitud = solicitud;
        this.cliente = cliente;
        this.monto = monto;
        this.metodoPago = metodoPago;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public PagoEstado getEstado() {
        return estado;
    }

    public void setEstado(PagoEstado estado) {
        this.estado = estado;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getPasarela() {
        return pasarela;
    }

    public void setPasarela(String pasarela) {
        this.pasarela = pasarela;
    }

    public String getIdTransaccionPasarela() {
        return idTransaccionPasarela;
    }

    public void setIdTransaccionPasarela(String idTransaccionPasarela) {
        this.idTransaccionPasarela = idTransaccionPasarela;
    }

    public String getCodigoAutorizacion() {
        return codigoAutorizacion;
    }

    public void setCodigoAutorizacion(String codigoAutorizacion) {
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Date getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(Date fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    /**
     * Fecha efectiva del pago: la de aprobacion si ya ocurrio, y si no la de
     * creacion. Se usa para ordenar y para los reportes, que antes leian un
     * campo unico que hoy quedo partido en dos.
     */
    public Date getFechaPago() {
        return fechaAprobacion != null ? fechaAprobacion : fechaCreacion;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public void setUrlPdf(String urlPdf) {
        this.urlPdf = urlPdf;
    }

    public Boolean getCorreoNotificado() {
        return correoNotificado;
    }

    public void setCorreoNotificado(Boolean correoNotificado) {
        this.correoNotificado = correoNotificado;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    /** Indica si el pago ya genero comprobante. */
    public boolean estaAprobado() {
        return estado != null && estado.esAprobado();
    }

    @Override
    public String toString() {
        return "Pago{" +
                "id=" + id +
                ", solicitudId=" + (solicitud != null ? solicitud.getId() : null) +
                ", clienteId=" + (cliente != null ? cliente.getId() : null) +
                ", monto=" + monto + " " + moneda +
                ", metodoPago=" + metodoPago +
                ", estado=" + estado +
                ", referencia='" + referencia + '\'' +
                ", pasarela='" + pasarela + '\'' +
                '}';
    }
}