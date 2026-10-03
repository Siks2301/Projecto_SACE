package com.mycompany.sacejpa.Pagos.Pasarela;

import com.mycompany.sacejpa.Modelo.PagoEstado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pasarela simulada para trabajar sin conexion.
 *
 * <p>Existe por una razon muy concreta: en una sustentacion no se puede
 * depender de que el aula tenga internet. Si la demo se cae porque el wifi del
 * salon fallo, el jurado se queda sin ver el modulo de pagos.
 *
 * <p>Esta clase reproduce el comportamiento observable de una pasarela real
 * (nace PENDIENTE, se aprueba o se rechaza tras un retardo) pero sin hacer
 * ninguna llamada de red. El resto del sistema no sabe cual de las dos esta
 * activa, porque ambas cumplen el mismo contrato.
 *
 * <p>Honestidad ante el jurado: el modo simulado esta pensado para las
 * rehearsals y para el plan B del dia de la sustentacion. El modo por defecto
 * del proyecto es {@code mock} precisamente porque no requiere ninguna cuenta
 * ni llave; para mostrar la integracion real se cambia a {@code wompi}.
 */
@Component
@ConditionalOnProperty(name = "app.pagos.pasarela", havingValue = "mock", matchIfMissing = true)
public class MockPasarela implements PasarelaPagos {

    private static final Logger log = LoggerFactory.getLogger(MockPasarela.class);

    /** Estado interno de una transaccion simulada. */
    private record TransaccionSimulada(String id, String referencia, PagoEstado estado, String metodo,
                                       String codigoAutorizacion, long centavos,
                                       long creadaEnMilis) {
    }

    private final Map<String, TransaccionSimulada> transacciones = new ConcurrentHashMap<>();
    private final long retardoMilis;
    private final PagoEstado resultadoForzado;

    /**
     * @param retardoMilis  tiempo que la transaccion queda PENDIENTE antes de resolverse
     * @param resultadoForzado estado final que producira; util para ensayar un rechazo
     */
    public MockPasarela(
            @Value("${app.pagos.mock.retardo-ms:3000}") long retardoMilis,
            @Value("${app.pagos.mock.resultado:APROBADO}") String resultadoForzado) {
        this.retardoMilis = Math.max(retardoMilis, 0);
        this.resultadoForzado = resolverResultado(resultadoForzado);
        log.info("Pasarela de pagos en modo SIMULADO. Resultado tras {} ms: {}",
                this.retardoMilis, this.resultadoForzado);
    }

    /**
     * Traduce el valor de configuracion al estado interno.
     * Un valor mal escrito no rompe el arranque: se registra y se aprueba.
     */
    private static PagoEstado resolverResultado(String valor) {
        PagoEstado estado = PagoEstado.desdeCodigo(valor);
        if (estado == null || estado == PagoEstado.PENDIENTE) {
            log.warn("app.pagos.mock.resultado='{}' no es un estado final valido. Se usara APROBADO.", valor);
            return PagoEstado.APROBADO;
        }
        return estado;
    }

    @Override
    public String nombre() {
        return "SIMULADA";
    }

    /** El modo simulado siempre esta disponible: no depende de nada externo. */
    @Override
    public boolean disponible() {
        return true;
    }

    /**
     * Declara que el cobro es de ensayo.
     *
     * <p>Es lo que hace que el modal rotule «MODO PRUEBAS» y que el mensaje de
     * exito advierta de que no se cobro nada.
     */
    @Override
    public boolean esSimulada() {
        return true;
    }

    /**
     * No hay checkout externo: el pago se resuelve dentro de la misma pagina.
     *
     * <p>Por eso {@link #urlCheckout(OrdenPago)} devuelve null.
     */
    @Override
    public boolean tieneCheckoutExterno() {
        return false;
    }

    @Override
    public TransaccionPasarela crear(OrdenPago orden) {
        if (orden == null || orden.referencia() == null || orden.referencia().isBlank()) {
            throw new PasarelaPagosException("La orden simulada no trae referencia.");
        }
        if (orden.monto() == null || orden.monto().signum() <= 0) {
            throw PasarelaPagosException.rechazo("El monto del pago debe ser mayor a cero.");
        }

        String id = "mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        TransaccionSimulada simulada = new TransaccionSimulada(
                id,
                orden.referencia(),
                PagoEstado.PENDIENTE,
                orden.metodo() != null ? orden.metodo().codigoPasarela() : "CARD",
                null,
                WompiPasarela.aCentavos(orden.monto()),
                System.currentTimeMillis());

        transacciones.put(id, simulada);
        log.info("Transaccion simulada creada. referencia={} id={}", orden.referencia(), id);

        return toContrato(simulada);
    }

    /**
     * Resuelve la transaccion cuando ya paso el retardo configurado.
     *
     * <p>Ese retardo es lo que hace visible el estado "Procesando pago..." en la
     * interfaz. Si fuera instantaneo, el usuario nunca veria la pantalla
     * intermedia y la demo perderia su parte mas interesante.
     */
    @Override
    public TransaccionPasarela consultar(String idTransaccion) {
        TransaccionSimulada simulada = transacciones.get(idTransaccion);
        if (simulada == null) {
            throw new PasarelaPagosException("La transaccion simulada no existe o ya expiro: " + idTransaccion);
        }

        if (simulada.estado() != PagoEstado.PENDIENTE) {
            return toContrato(simulada);
        }

        long transcurrido = System.currentTimeMillis() - simulada.creadaEnMilis();
        if (transcurrido < retardoMilis) {
            return toContrato(simulada);
        }

        TransaccionSimulada resuelta = new TransaccionSimulada(
                simulada.id(),
                simulada.referencia(),
                resultadoForzado,
                simulada.metodo(),
                resultadoForzado == PagoEstado.APROBADO ? "SIM-" + (int) (Math.abs(simulada.referencia().hashCode()) % 100000) : null,
                simulada.centavos(),
                simulada.creadaEnMilis());

        transacciones.put(idTransaccion, resuelta);
        log.info("Transaccion simulada resuelta. referencia={} estado={}",
                resuelta.referencia(), resuelta.estado());
        return toContrato(resuelta);
    }

    /**
     * En modo simulado no hay checkout externo: el flujo ocurre en la misma
     * pagina, asi que se devuelve null para que el frontend no intente abrir
     * una pestana que no lleva a ningun lado.
     */
    @Override
    public String urlCheckout(OrdenPago orden) {
        return null;
    }

    /** Proyecta el registro simulado al contrato que entiende el servicio. */
    private TransaccionPasarela toContrato(TransaccionSimulada s) {
        return new TransaccionPasarela(
                s.id(),
                s.referencia(),
                s.estado(),
                s.metodo(),
                s.codigoAutorizacion(),
                s.centavos());
    }

    /**
     * Fuerza el resultado de una transaccion concreta.
     *
     * <p>Lo usa el endpoint de demostracion para ensayar tanto el camino feliz
     * como el rechazo sin tener que reiniciar el servidor.
     *
     * @param idTransaccion identificador de la transaccion simulada
     * @param estado       estado final a aplicar
     * @return true si la transaccion existia
     */
    public boolean forzarResultado(String idTransaccion, PagoEstado estado) {
        TransaccionSimulada s = transacciones.get(idTransaccion);
        if (s == null || estado == null || estado == PagoEstado.PENDIENTE) {
            return false;
        }
        transacciones.put(idTransaccion, new TransaccionSimulada(
                s.id(), s.referencia(), estado, s.metodo(),
                estado == PagoEstado.APROBADO ? "SIM-FORZADO" : null,
                s.centavos(), s.creadaEnMilis()));
        return true;
    }

    /** Duracion configurada del retardo, para pruebas. */
    public Duration retardo() {
        return Duration.ofMillis(retardoMilis);
    }
}