package com.mycompany.sacejpa.DTOs;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class PagoDTOs {

    public static class CrearPagoDTO {
        private Long solicitudId;
        private BigDecimal monto;
        private String metodoPago;
        private String llaveDestino;
        private String notas;

        public CrearPagoDTO() {}

        public CrearPagoDTO(Long solicitudId, BigDecimal monto, String metodoPago, String llaveDestino) {
            this.solicitudId = solicitudId;
            this.monto = monto;
            this.metodoPago = metodoPago;
            this.llaveDestino = llaveDestino;
        }

        public Long getSolicitudId() { return solicitudId; }
        public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }

        public String getMetodoPago() { return metodoPago; }
        public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

        public String getLlaveDestino() { return llaveDestino; }
        public void setLlaveDestino(String llaveDestino) { this.llaveDestino = llaveDestino; }

        public String getNotas() { return notas; }
        public void setNotas(String notas) { this.notas = notas; }
    }

    public static class PagoRespuestaDTO {
        private Long idPago;
        private Long solicitudId;
        private String solicitudTitulo;
        private Long clienteId;
        private String clienteNombre;
        private String clienteEmail;
        private BigDecimal monto;
        private Date fechaPago;
        private String metodoPago;
        private String llaveDestino;
        private String estado;
        private String urlPdf;
        private Boolean correoNotificado;

        public PagoRespuestaDTO() {}

        public PagoRespuestaDTO(Long idPago, Long solicitudId, String solicitudTitulo, Long clienteId,
                               String clienteNombre, String clienteEmail, BigDecimal monto, Date fechaPago,
                               String metodoPago, String llaveDestino, String estado, String urlPdf, Boolean correoNotificado) {
            this.idPago = idPago;
            this.solicitudId = solicitudId;
            this.solicitudTitulo = solicitudTitulo;
            this.clienteId = clienteId;
            this.clienteNombre = clienteNombre;
            this.clienteEmail = clienteEmail;
            this.monto = monto;
            this.fechaPago = fechaPago;
            this.metodoPago = metodoPago;
            this.llaveDestino = llaveDestino;
            this.estado = estado;
            this.urlPdf = urlPdf;
            this.correoNotificado = correoNotificado;
        }

        public Long getIdPago() { return idPago; }
        public void setIdPago(Long idPago) { this.idPago = idPago; }

        public Long getSolicitudId() { return solicitudId; }
        public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

        public String getSolicitudTitulo() { return solicitudTitulo; }
        public void setSolicitudTitulo(String solicitudTitulo) { this.solicitudTitulo = solicitudTitulo; }

        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

        public String getClienteNombre() { return clienteNombre; }
        public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

        public String getClienteEmail() { return clienteEmail; }
        public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

        public BigDecimal getMonto() { return monto; }
        public void setMonto(BigDecimal monto) { this.monto = monto; }

        public Date getFechaPago() { return fechaPago; }
        public void setFechaPago(Date fechaPago) { this.fechaPago = fechaPago; }

        public String getMetodoPago() { return metodoPago; }
        public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

        public String getLlaveDestino() { return llaveDestino; }
        public void setLlaveDestino(String llaveDestino) { this.llaveDestino = llaveDestino; }

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public String getUrlPdf() { return urlPdf; }
        public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }

        public Boolean getCorreoNotificado() { return correoNotificado; }
        public void setCorreoNotificado(Boolean correoNotificado) { this.correoNotificado = correoNotificado; }
    }
}
