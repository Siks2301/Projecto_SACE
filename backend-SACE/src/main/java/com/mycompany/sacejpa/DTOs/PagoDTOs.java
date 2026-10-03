package com.mycompany.sacejpa.DTOs;

import com.mycompany.sacejpa.Modelo.MetodoPago;
import com.mycompany.sacejpa.Modelo.PagoEstado;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Objetos de transferencia del modulo de pagos.
 *
 * <p>Dos cambios de fondo respecto a la version anterior:
 *
 * <ul>
 *   <li><b>{@code CrearPagoDTO} ya no tiene campo {@code monto}.</b> Ese es el
 *       arreglo del problema de seguridad mas grave del proyecto: el precio lo
 *       decide el backend leyendo el catalogo de servicios. Si el DTO lo
 *       aceptara, un cliente podria enviar cualquier cifra y el servidor la
 *       guardaria.</li>
 *   <li><b>La respuesta describe el flujo completo</b>: estado, referencia y URL
 *       de checkout son lo que el navegador necesita para seguir el pago hasta
 *       el final.</li>
 * </ul>
 */
public class PagoDTOs {

    private PagoDTOs() {
    }

    /**
     * Datos que envia el cliente para iniciar un pago.
     *
     * <p>Solo tres campos, y ninguno es el precio:
     * <ul>
     *   <li>{@code solicitudId}: sobre que reserva se cobra.</li>
     *   <li>{@code metodoPago}: como quiere pagar (lo unico que el cliente decide).</li>
     *   <li>{@code notas}: comentario opcional.</li>
     * </ul>
     *
     * El backend responde con un error explicito si llega un {@code monto}, de
     * forma que quede registro de que alguien intento alterar el precio.
     */
    public static class CrearPagoDTO {

        private Long solicitudId;
        private String metodoPago;
        private String notas;

        public CrearPagoDTO() {
        }

        public CrearPagoDTO(Long solicitudId, String metodoPago) {
            this.solicitudId = solicitudId;
            this.metodoPago = metodoPago;
        }

        public Long getSolicitudId() {
            return solicitudId;
        }

        public void setSolicitudId(Long solicitudId) {
            this.solicitudId = solicitudId;
        }

        public String getMetodoPago() {
            return metodoPago;
        }

        public void setMetodoPago(String metodoPago) {
            this.metodoPago = metodoPago;
        }

        public String getNotas() {
            return notas;
        }

        public void setNotas(String notas) {
            this.notas = notas;
        }

        /**
         * Lee un posible campo "monto" aunque no exista en la clase.
         *
         * <p>Sirve para DETECTAR el intento de manipulo en vez de ignorarlo en
         * silencio. Spring Jackson ignora por defecto los campos desconocidos, asi
         * que sin esto el ataque pasaria inadvertido y el log no dejaria rastro.
         *
         * @param json cuerpo crudo de la peticion, ya deserializado en mapa
         * @return true si el cliente intento enviar un monto
         */
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void registrarCampoDesconocido(String clave, Object valor) {
            if ("monto".equalsIgnoreCase(clave) || "amount".equalsIgnoreCase(clave)
                    || "precio".equalsIgnoreCase(clave)) {
                throw new IllegalArgumentException(
                        "El monto no lo puede elegir el cliente: se calcula con el precio del servicio reservado.");
            }
        }
    }

    /**
     * Estado de un pago tal como lo ve el navegador.
     *
     * @param idPago              identificador interno
     * @param referencia          clave de idempotencia, la que conoce la pasarela
     * @param solicitudId         solicitud pagada
     * @param solicitudTitulo     nombre del tour
     * @param clienteId           cliente que paga
     * @param clienteNombre       nombre completo
     * @param clienteEmail        correo
     * @param monto               monto cobrado, calculado por el backend
     * @param moneda              codigo de moneda
     * @param metodoPago          metodo elegido
     * @param metodoPagoEtiqueta  nombre legible del metodo
     * @param estado              estado actual
     * @param estadoEtiqueta      nombre legible del estado
     * @param pasarela            pasarela procesadora
     * @param codigoAutorizacion  codigo bancario de autorizacion
     * @param fechaCreacion       momento en que se creo la orden
     * @param fechaAprobacion     momento en que la pasarela aprobo, si ya ocurrio
     * @param urlCheckout         pagina de pago de la pasarela, o null
     * @param urlPdf              comprobante generado, disponible solo si esta aprobado
     * @param correoNotificado    si el comprobante ya se envio por correo
     * @param comprobanteDisponible si ya se puede descargar el PDF
     */
    public record PagoRespuestaDTO(
            Long idPago,
            String referencia,
            Long solicitudId,
            String solicitudTitulo,
            Long clienteId,
            String clienteNombre,
            String clienteEmail,
            BigDecimal monto,
            String moneda,
            MetodoPago metodoPago,
            String metodoPagoEtiqueta,
            PagoEstado estado,
            String estadoEtiqueta,
            String pasarela,
            String codigoAutorizacion,
            Date fechaCreacion,
            Date fechaAprobacion,
            String urlCheckout,
            String urlPdf,
            Boolean correoNotificado,
            boolean comprobanteDisponible
    ) {
    }

    /**
     * Cotizacion que el backend devuelve ANTES de cobrar.
     *
     * <p>Es la fuente de verdad del precio para la interfaz: el campo monto del
     * formulario se rellena con esto y queda bloqueado. Asi el cliente ve el
     * valor correcto y a la vez el backend conserva la autoridad sobre el.
     *
     * @param solicitudId  solicitud a cotizar
     * @param titulo       nombre del tour
     * @param monto        precio del servicio, en pesos
     * @param moneda       codigo de moneda
     * @param metodos      metodos de pago que el sistema acepta
     * @param yaPagado     true si la solicitud ya tiene un cobro aprobado
     */
    public record CotizacionDTO(
            Long solicitudId,
            String titulo,
            BigDecimal monto,
            String moneda,
            java.util.List<String> metodos,
            boolean yaPagado
    ) {
    }

    /**
     * Configuracion de pagos que el frontend necesita antes de mostrar el modal.
     *
     * <p>Se expone la pasarela activa y si el flujo termina en el checkout
     * externo o dentro de la misma pagina. El frontend necesita saberlo para no
     * intentar abrir una pestana que no lleva a ningun sitio.
     *
     * @param pasarela          nombre de la pasarela activa
     * @param modoSimulado      true si es la pasarela de pruebas local
     * @param checkoutExterno   true si el pago ocurre en una pagina de la pasarela
     * @param metodos           metodos habilitados
     * @param mensajes          textos a mostrar por metodo
     */
    public record ConfiguracionPagosDTO(
            String pasarela,
            boolean modoSimulado,
            boolean checkoutExterno,
            java.util.List<MetodoPagoInfoDTO> metodos
    ) {
    }

    /** Descripcion de un metodo de pago para pintar el selector. */
    public record MetodoPagoInfoDTO(String codigo, String nombre) {
    }
}