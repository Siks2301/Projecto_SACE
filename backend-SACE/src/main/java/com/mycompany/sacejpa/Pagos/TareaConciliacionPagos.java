package com.mycompany.sacejpa.Pagos;

import com.mycompany.sacejpa.Servicios.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea periodica de conciliacion de pagos.
 *
 * <h2>Por que existe esta tarea</h2>
 * El flujo normal es: SACE crea la transaccion y la pasarela avisa por webhook
 * cuando la aprueba. Ese camino es el rapido, pero tiene un punto debil
 * inevitable: <b>la red falla</b>. Si el evento no llega porque se cayo el
 * servidor, porque el tunel de pruebas estaba caido o porque la peticion se
 * perdio, el pago se queda en PENDIENTE para siempre aunque el cliente si haya
 * pagado. Para el usuario eso es una reserva cobrada que nunca aparece
 * confirmada; para la agencia, un ingreso que no suma en los reportes.
 *
 * <p>Un sistema de pagos profesional resuelve esto con reconciliacion
 * periodica: cada cierto tiempo se pregunta a la pasarela por los pagos que
 * siguen abiertos y se cierran los que ya estan resueltos alla. Es la misma idea
 * de cuadrar un libro contable contra el banco.
 *
 * <p>La operacion es idempotente: si el pago ya se cerro, no hace nada. Por eso
 * se puede ejecutar cada pocos minutos sin riesgo de efectos acumulativos.
 */
@Component
public class TareaConciliacionPagos {

    private static final Logger log = LoggerFactory.getLogger(TareaConciliacionPagos.class);

    @Autowired
    private PagoServicio pagoServicio;

    /** Minutos entre ejecuciones. Solo informativo: el intervalo real se define en el @Scheduled. */
    @Value("${app.pagos.conciliacion.minutos:5}")
    private long intervaloMinutos;

    /**
     * Indica si se registra el resultado en cada ejecucion.
     *
     * <p>Por defecto false: un mensaje cada cinco minutos aunque no pase nada
     * moja el log y esconde justamente los mensajes que si importan.
     */
    @Value("${app.pagos.conciliacion.verbose:false}")
    private boolean verboso;

    @Scheduled(
            initialDelayString = "${app.pagos.conciliacion.espera-inicial-ms:60000}",
            fixedDelayString = "${app.pagos.conciliacion.intervalo-ms:300000}")
    public void conciliar() {
        try {
            int resueltos = pagoServicio.reconciliarPendientes();

            if (resueltos > 0 || verboso) {
                log.info("Tarea de conciliacion: {} pago(s) pendiente(s) reconciliado(s).",
                        resueltos);
            }
        } catch (Exception e) {
            // La tarea nunca debe propagar el fallo: si la pasarela esta caida se
            // reintenta en la proxima ejecucion, en lugar de tumbar la aplicacion.
            log.error("La tarea de conciliacion de pagos fallo: " + e.getMessage());
        }
    }

    /** Intervalo configurado, en minutos. */
    public long intervaloMinutos() {
        return intervaloMinutos;
    }
}