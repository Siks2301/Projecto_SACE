package com.mycompany.sacejpa.Pagos.Pasarela;

import com.mycompany.sacejpa.Modelo.MetodoPago;
import com.mycompany.sacejpa.Modelo.PagoEstado;

import java.math.BigDecimal;

/**
 * Contrato que debe cumplir cualquier pasarela de pagos que SACE integre.
 *
 * <p>Se aplico el patron <b>Strategy</b>: el modulo de pagos no conoce los
 * detalle de HTTP de Wompi, solo este contrato. Gracias a eso se puede cambiar
 * de pasarela (o apagarla y dejar la simulada) sin tocar ni una linea de
 * PagoServicio ni de los controladores.
 *
 * <p>Regla de seguridad: la implementacion NUNCA recibe datos de tarjeta. El
 * cliente final digita su tarjeta dentro del checkout de la pasarela y solo
 * viaja un token opaco. Asi SACE nunca toca el numero de tarjeta y queda fuera
 * del alcance de PCI-DSS.
 */
public interface PasarelaPagos {

    /** Nombre identificador de la pasarela, tal como se guarda en la tabla pago. */
    String nombre();

    /** true si la pasarela esta configurada y puede recibir transacciones. */
    boolean disponible();

    /**
     * true si esta pasarela no mueve dinero real y solo simula el cobro.
     *
     * <p>La interfaz del cliente cambia segun la respuesta: en modo simulado se
     * rotula «MODO PRUEBAS» para que nadie confunda una transaccion de ensayo
     * con un cobro real.
     *
     * <p>Existe como metodo y no se deduce comparando {@link #nombre()} con una
     * cadena porque esa comparacion se rompe en silencio: bastaria con escribir
     * «Simulada» en vez de «SIMULADA» para que el sistema dejara de avisar de
     * que esta en pruebas.
     *
     * @return true si el cobro es simulado
     */
    default boolean esSimulada() {
        return false;
    }

    /**
     * true si el pago se completa en una pagina de la pasarela, a la que el
     * navegador debe ser redirigido.
     *
     * <p>Cuando es false el flujo se resuelve dentro de la propia pagina de SACE
     * (lo que ocurre en modo simulado), de modo que el frontend no debe intentar
     * abrir ninguna pestana externa.
     *
     * @return true si hay que redirigir al checkout de la pasarela
     */
    default boolean tieneCheckoutExterno() {
        return true;
    }

    /**
     * Crea una transaccion en la pasarela por el monto indicado.
     *
     * @param orden datos de la orden ya calculados por el backend
     * @return transaccion creada, con estado inicial PENDIENTE
     * @throws PasarelaPagosException si la pasarela rechaza la peticion
     */
    TransaccionPasarela crear(OrdenPago orden);

    /**
     * Consulta el estado actual de una transaccion. Es el respaldo del webhook:
     * si un evento se pierde, el frontend sigue preguntando por aqui.
     *
     * @param idTransaccion identificador que devolvio la pasarela al crear
     * @return transaccion con su estado actual
     * @throws PasarelaPagosException si la pasarela no responde o no existe
     */
    TransaccionPasarela consultar(String idTransaccion);

    /**
     * URL de checkout donde se redirige al cliente para pagar. Puede devolver
     * null en modo simulado, donde el flujo ocurre dentro de la misma pagina.
     */
    default String urlCheckout(OrdenPago orden) {
        return null;
    }

    // ------------------------------------------------------------------
    // Estructuras de datos propias del contrato
    // ------------------------------------------------------------------

    /**
     * Orden de pago que SACE envia a la pasarela.
     *
     * <p>El monto ya viene resuelto desde el precio del servicio: es el unico
     * punto del sistema donde se decide cuanto se cobra.
     *
     * @param referencia      referencia unica de la transaccion (idempotencia)
     * @param idSolicitud     solicitud de origen
     * @param correoCliente   correo para el recibo de la pasarela
     * @param nombreCliente   nombre del pagador
     * @param telefonoCliente telefono del pagador, sin prefijo
     * @param documentoCliente numero de documento, o null
     * @param tipoDocumento   CC / CE / PASAPORTE / NIT, o null
     * @param monto           monto en pesos, ya calculado por el backend
     * @param metodo          metodo elegido por el cliente
     * @param ipOrigen        IP del solicitante, para trazabilidad antifraude
     */
    record OrdenPago(
            String referencia,
            Long idSolicitud,
            String correoCliente,
            String nombreCliente,
            String telefonoCliente,
            String documentoCliente,
            String tipoDocumento,
            BigDecimal monto,
            MetodoPago metodo,
            String ipOrigen
    ) {
    }

    /**
     * Transaccion tal como la reporta la pasarela, ya normalizada al
     * vocabulario de SACE para que el resto del sistema no dependa de ella.
     *
     * @param idTransaccion    identificador de la transaccion en la pasarela
     * @param referencia      referencia enviada por SACE
     * @param estado          estado ya traducido a PagoEstado
     * @param metodoTipo      metodo reportado por la pasarela
     * @param codigoAutorizacion codigo de autorizacion bancaria, o null
     * @param montoCentavos   monto en centavos segun la pasarela
     */
    record TransaccionPasarela(
            String idTransaccion,
            String referencia,
            PagoEstado estado,
            String metodoTipo,
            String codigoAutorizacion,
            Long montoCentavos
    ) {
    }
}