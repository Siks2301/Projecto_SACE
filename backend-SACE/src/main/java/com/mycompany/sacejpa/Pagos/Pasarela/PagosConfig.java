package com.mycompany.sacejpa.Pagos.Pasarela;

import com.mycompany.sacejpa.Modelo.MetodoPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuracion del modulo de pagos.
 *
 * <p>Resuelve dos cosas: la instancia unica de {@link RestTemplate} que usa
 * Wompi (con timeouts, porque sin ellos una pasarela lenta se traduce en hilos
 * del servidor bloqueados) y el conjunto de metodos de pago habilitados.
 *
 * <h2>Por que una sola pasarela activa</h2>
 * Solo una implementacion de {@link PasarelaPagos} queda registrada en el
 * contenedor, segun {@code app.pagos.pasarela}. Inyectar el contrato en
 * {@code PagoServicio} no obliga al servicio a saber si del otro lado hay una
 * llamada HTTP real o una simulacion en memoria.
 */
@Configuration
@EnableScheduling
public class PagosConfig {

    private static final Logger log = LoggerFactory.getLogger(PagosConfig.class);

    private final Set<MetodoPago> metodosHabilitados;

    public PagosConfig(
            @Value("${app.pagos.metodos-habilitados:PSE,TARJETA}") String metodos,
            @Value("${app.pagos.pasarela:mock}") String pasarela) {
        this.metodosHabilitados = parsearMetodos(metodos);
        log.info("Modulo de pagos: pasarela='{}', metodos habilitados={}",
                pasarela, metodosHabilitados);
    }

    /**
     * Parsea la lista de metodos habilitados.
     *
     * <p>Un valor mal escrito se descarta con aviso en vez de romper el
     * arranque: es preferible arrancar con menos metodos que no arrancar.
     */
    private static Set<MetodoPago> parsearMetodos(String texto) {
        if (texto == null || texto.isBlank()) {
            return EnumSet.of(MetodoPago.PSE, MetodoPago.TARJETA);
        }
        Set<MetodoPago> resultado = Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(MetodoPago::desdeCodigo)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(MetodoPago.class)));

        if (resultado.isEmpty()) {
            log.warn("app.pagos.metodos-habilitados no contiene ningun metodo valido. Se usaran PSE y TARJETA.");
            return EnumSet.of(MetodoPago.PSE, MetodoPago.TARJETA);
        }
        return resultado;
    }

    /** Metodos de pago que el sistema acepta en este momento. */
    public Set<MetodoPago> metodosHabilitados() {
        return metodosHabilitados;
    }

    /** Indica si un metodo concreto puede usarse. */
    public boolean metodoHabilitado(MetodoPago metodo) {
        return metodo != null && metodosHabilitados.contains(metodo);
    }

    /**
     * Cliente HTTP de la pasarela.
     *
     * <p>Los timeouts no son opcionales: sin ellos, si Wompi no responde, el
     * hilo que atiende la peticion queda esperando indefinidamente y con
     * reservas simultaneas el servidor deja de atender solicitudes.
     */
    @Bean
    public RestTemplate pasarelaRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(20))
                .build();
    }
}