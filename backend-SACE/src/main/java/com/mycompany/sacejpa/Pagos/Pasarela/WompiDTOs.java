package com.mycompany.sacejpa.Pagos.Pasarela;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Estructuras JSON de la API de Wompi (Colombia).
 *
 * <p>Son clases "espejo" de los payloads que la pasarela envia y recibe. Se
 * mantienen aisladas del resto del dominio a proposito: si manana se cambia de
 * pasarela, estos tipos desaparecen sin arrastrar nada mas.
 *
 * <p>Todas ignoran campos desconocidos porque Wompi anade propiedades nuevas
 * con frecuencia y un campo extra nunca debe romper el cobro de una reserva.
 */
public final class WompiDTOs {

    private WompiDTOs() {
    }

    // ------------------------------------------------------------------
    // Peticion: POST /v1/transactions
    // ------------------------------------------------------------------

    /**
     * Cuerpo de la peticion de creacion de transaccion.
     *
     * <p>Nunca incluye numero de tarjeta, CVV ni fecha de vencimiento: el
     * cliente digita eso dentro del checkout de Wompi. Aqui solo viaja el
     * identificador de la sesion de aceptacion de terminos.
     *
     * @param acceptanceToken token de aceptacion de terminos
     * @param amountInCents   monto en centavos enteros (nunca decimal ni float)
     * @param currency        moneda, siempre COP para este proyecto
     * @param customerEmail   correo del pagador
     * @param paymentMethod   metodo elegido: PSE, CARD o NEQUI
     * @param reference       referencia unica generada por SACE
     * @param customerData    documento, nombre y telefono del pagador
     * @param signature       firma de integridad SHA-256
     * @param ipAddress       IP del solicitante, para el antifraude de la pasarela
     * @param redirectUrl     a donde vuelve el cliente tras pagar
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransaccionPeticion(
            @JsonProperty("acceptance_token") String acceptanceToken,
            @JsonProperty("amount_in_cents") Long amountInCents,
            String currency,
            @JsonProperty("customer_email") String customerEmail,
            @JsonProperty("payment_method") String paymentMethod,
            String reference,
            @JsonProperty("customer_data") DatosCliente customerData,
            String signature,
            @JsonProperty("ip_address") String ipAddress,
            @JsonProperty("redirect_url") String redirectUrl
    ) {
    }

    /** Datos basicos del pagador que Wompi exige para PSE. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DatosCliente(
            String document,
            @JsonProperty("full_name") String fullName,
            String phone
    ) {
    }

    // ------------------------------------------------------------------
    // Respuesta: POST /v1/transactions  y  GET /v1/transactions/{id}
    // ------------------------------------------------------------------

    /**
     * Transaccion tal como la reporta Wompi.
     *
     * @param id              identificador de la transaccion en Wompi
     * @param status          APPROVED / DECLINED / PENDING / VOIDED / ERROR
     * @param statusMessage   descripcion legible del estado
     * @param reference       referencia enviada por SACE
     * @param amountInCents   monto cobrado, en centavos
     * @param currency        moneda
     * @param paymentMethod   metodo solicitado
     * @param paymentMethodType metodo realmente usado
     * @param authorizationCode codigo de autorizacion bancaria
     * @param createdAt       fecha de creacion (ISO-8601)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransaccionRespuesta(
            String id,
            String status,
            @JsonProperty("status_message") String statusMessage,
            String reference,
            @JsonProperty("amount_in_cents") Long amountInCents,
            String currency,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("payment_method_type") String paymentMethodType,
            @JsonProperty("authorization_code") String authorizationCode,
            @JsonProperty("created_at") String createdAt
    ) {
    }

    // ------------------------------------------------------------------
    // Eventos: POST /api/pagos/webhook/wompi
    // ------------------------------------------------------------------

    /**
     * Evento de notificacion que Wompi envia cuando una transaccion cambia de
     * estado (por ejemplo {@code transaction.updated} con APPROVED).
     *
     * @param data      cuerpo con la transaccion afectada
     * @param signature firma del evento: permite saber que el aviso es autentico
     * @param event     nombre del evento
     * @param sentAt    fecha de envio (ISO-8601)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Evento(
            EventoData data,
            FirmaEvento signature,
            String event,
            @JsonProperty("sent_at") String sentAt
    ) {
    }

    /** Envoltura {@code data} del evento. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventoData(
            TransaccionRespuesta transaction
    ) {
    }

    /**
     * Firma del evento.
     *
     * <p>La lista {@code properties} es la clave de seguridad: son las rutas
     * exactas de los campos que el evento debe incluir. El backend NO las
     * inventa, las copia tal cual llegan y con ellas arma el texto a hashear.
     * Si se hardcodeara esa lista, un atacante podria alterar el monto o el
     * estado del evento sin que el checksum cambiara.
     *
     * @param properties rutas de los campos firmados, en orden
     * @param timestamp  instante de emision, en segundos epoch
     * @param checksum   SHA-256 calculado por Wompi
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FirmaEvento(
            List<String> properties,
            Long timestamp,
            String checksum
    ) {
    }
}