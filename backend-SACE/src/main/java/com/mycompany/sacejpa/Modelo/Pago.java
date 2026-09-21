package com.mycompany.sacejpa.Modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entidad Pago / Comprobante.
 * Registra las transacciones financieras asociadas a una Solicitud y Cliente.
 * Incluye la llave de destino obligatoria "Bre-B @VXM301" y la trazabilidad del comprobante PDF.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private Solicitud solicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_pago", nullable = false)
    private Date fechaPago;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(name = "llave_destino", nullable = false)
    private String llaveDestino = "Bre-B @VXM301";

    @Column(name = "estado", nullable = false)
    private String estado = "CONFIRMADO";

    @Column(name = "url_pdf")
    private String urlPdf;

    @Column(name = "correo_notificado", nullable = false)
    private Boolean correoNotificado = false;

    public Pago() {
        this.fechaPago = new Date();
        this.llaveDestino = "Bre-B @VXM301";
        this.estado = "CONFIRMADO";
        this.correoNotificado = false;
    }

    public Pago(Solicitud solicitud, Cliente cliente, BigDecimal monto, String metodoPago, String llaveDestino) {
        this();
        this.solicitud = solicitud;
        this.cliente = cliente;
        this.monto = monto;
        this.metodoPago = metodoPago;
        if (llaveDestino != null && !llaveDestino.trim().isEmpty()) {
            this.llaveDestino = llaveDestino.trim();
        }
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

    public Date getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(Date fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getLlaveDestino() {
        return llaveDestino;
    }

    public void setLlaveDestino(String llaveDestino) {
        this.llaveDestino = llaveDestino;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    @Override
    public String toString() {
        return "Pago{" +
                "id=" + id +
                ", solicitudId=" + (solicitud != null ? solicitud.getId() : "null") +
                ", clienteId=" + (cliente != null ? cliente.getId() : "null") +
                ", monto=" + monto +
                ", llaveDestino='" + llaveDestino + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
