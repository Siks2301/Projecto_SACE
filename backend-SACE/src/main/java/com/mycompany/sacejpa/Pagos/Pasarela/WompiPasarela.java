package com.mycompany.sacejpa.Pagos.Pasarela;

import com.mycompany.sacejpa.Modelo.PagoEstado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Integracion con la pasarela de pagos Wompi (Banco de Colombia).
 *
 * <p>Es la clase unica del proyecto que conoce la API de Wompi. Todo lo demas
 * (servicio de pagos, controladores, frontend) trabaja contra la interfaz
 * {@link PasarelaPagos}, asi que cambiar de pasarela es question de cambiar
 * {@code app.pagos.pasarela} en el archivo de configuracion.
 *
 * <h2>Decisiones de seguridad</h2>
 * <ul>
 *   <li>La clave de comercio y la de integridad viven solo en este backend.
 *       El navegador recibe unicamente la clave publica y la referencia.</li>
 *   <li>El numero de tarjeta nunca pasa por SACE: el cliente lo digita en el
 *       checkout de la pasarela. Gracias a eso el proyecto queda fuera del
 *       alcance de PCI-DSS.</li>
 *   <li>Los montos viajan como <b>enteros en centavos</b>. Los flotantes no
 *       representan bien el dinero y {@code 0.1 + 0.2 != 0.3} termina en un
 *       peso perdido en cada venta.</li>
 * </ul>
 *
 * <h2>Ambientes</h2>
 * Sandbox y produccion usan el mismo contrato; lo unico que cambia es el
 * dominio y el prefijo de las llaves ({@code test_} frente a {@code prod_}).
 * Por eso el ambiente se decide por configuracion y no esta escrito en el codigo.
 */
@Component
@ConditionalOnProperty(name = "app.pagos.pasarela", havingValue = "wompi")
public class WompiPasarela implements PasarelaPagos {

    private static final Logger log = LoggerFactory.getLogger(WompiPasarela.class);

    private static final String MONEDA = "COP";

    private final RestTemplate http;
    private final WompiFirmaServicio firma;
    private final String baseUrl;
    private final String clavePublica;
    private final String plantillaCheckout;

    /**
     * @param builder          constructor de HTTP con timeouts ya configurados
     * @param firma            servicio de firmas SHA-256
     * @param baseUrl          raiz de la API, por ejemplo {@code https://sandbox.wompi.co/v1}
     * @param clavePublica     clave de comercio del ambiente configurado
     * @param plantillaCheckout base del checkout alojado, para el flujo por navegador
     */
    public WompiPasarela(
            RestTemplateBuilder builder,
            WompiFirmaServicio firma,
            @Value("${app.pagos.wompi.base-url}") String baseUrl,
            @Value("${app.pagos.wompi.clave-publica:}") String clavePublica,
            @Value("${app.pagos.wompi.url-checkout:https://checkout.wompi.co/l/}") String plantillaCheckout) {
        this.firma = firma;
        this.baseUrl = sinBarraFinal(baseUrl);
        this.clavePublica = clavePublica != null ? clavePublica.trim() : "";
        this.plantillaCheckout = plantillaCheckout;
        this.http = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String nombre() {
        return "WOMPI";
    }

    @Override
    public boolean disponible() {
        return !clavePublica.isEmpty() && firma.tieneClaveIntegridad();
    }

    /**
     * Convierte el monto de pesos a centavos enteros.
     *
     * <p>Se redondea de forma explicita y no por truncar: truncar 1999.99 a
     * 1999 centavos es un peso que la pasarela cobraria de menos.
     *
     * @param monto monto en pesos
     * @return monto exacto en centavos
     */
    public static long aCentavos(BigDecimal monto) {
        if (monto == null) {
            throw new PasarelaPagosException("No hay monto para convertir a centavos.");
        }
        return monto.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /**
     * Crea la transaccion en la pasarela.
     *
     * <p>Se usa la API JSON nativa y no el widget porque asi el backend controla
     * la orden completa y recibe el resultado por webhook, en vez de depender de
     * un script de terceros cargado en el navegador del cliente.
     */
    @Override
    public TransaccionPasarela crear(OrdenPago orden) {
        if (!disponible()) {
            throw new PasarelaPagosException(
                    "Wompi no esta configurado. Revise app.pagos.wompi.clave-publica "
                            + "y app.pagos.wompi.clave-integridad.");
        }

        long centavos = aCentavos(orden.monto());
        String referencia = orden.referencia();

        WompiDTOs.TransaccionPeticion peticion = new WompiDTOs.TransaccionPeticion(
                // El token de aceptacion viaja vacio a proposito: solo es
                // obligatorio si el comercio exige_accept_terms=true.
                "",
                centavos,
                MONEDA,
                orden.correoCliente(),
                orden.metodo().codigoPasarela(),
                referencia,
                new WompiDTOs.DatosCliente(
                        orden.documentoCliente(),
                        orden.nombreCliente(),
                        orden.telefonoCliente()),
                firma.calcularFirmaIntegridad(referencia, centavos, MONEDA),
                orden.ipOrigen(),
                null);

        log.info("Creando transaccion en Wompi. referencia={} centavos={} metodo={}",
                referencia, centavos, orden.metodo());

        WompiDTOs.TransaccionRespuesta respuesta = enviar(
                HttpMethod.POST, baseUrl + "/transactions", peticion, referencia);

        PagoEstado estado = PagoEstado.desdeCodigo(respuesta.status());
        if (estado == null) {
            throw PasarelaPagosException.rechazo(
                    "La pasarela devolvio un estado desconocido: " + respuesta.status());
        }

        return normalizar(respuesta, estado);
    }

    /**
     * Consulta el estado de una transaccion.
     *
     * <p>Es el respaldo del webhook: si un evento se pierde o el tunel de pruebas
     * se cae, el frontend sigue preguntando por aqui y SACE reconcilia solo.
     */
    @Override
    public TransaccionPasarela consultar(String idTransaccion) {
        if (idTransaccion == null || idTransaccion.isBlank()) {
            throw new PasarelaPagosException("No hay transaccion que consultar.");
        }
        if (!disponible()) {
            throw new PasarelaPagosException("Wompi no esta configurado.");
        }

        WompiDTOs.TransaccionRespuesta respuesta = enviar(
                HttpMethod.GET, baseUrl + "/transactions/" + idTransaccion, null, idTransaccion);

        PagoEstado estado = PagoEstado.desdeCodigo(respuesta.status());
        if (estado == null) {
            // Un estado ilegible no se asume: se propaga el error y el pago
            // sigue pendiente hasta que la pasarela responda algo coherente.
            throw new PasarelaPagosException(
                    "La pasarela devolvio un estado desconocido: " + respuesta.status());
        }

        return normalizar(respuesta, estado);
    }

    /**
     * URL del checkout alojado, construida a partir de la referencia.
     *
     * @param orden orden de pago ya construida
     * @return URL publica del checkout, o null si no hay plantilla configurada
     */
    @Override
    public String urlCheckout(OrdenPago orden) {
        if (orden == null || orden.referencia() == null || orden.referencia().isBlank()) {
            return null;
        }
        if (plantillaCheckout == null || plantillaCheckout.isBlank()) {
            return null;
        }
        return plantillaCheckout + orden.referencia();
    }

    /** Traduce la respuesta cruda de la pasarela al vocabulario de SACE. */
    private TransaccionPasarela normalizar(WompiDTOs.TransaccionRespuesta r, PagoEstado estado) {
        return new TransaccionPasarela(
                r.id(),
                r.reference(),
                estado,
                r.paymentMethodType(),
                r.authorizationCode(),
                r.amountInCents());
    }

    /**
     * Ejecuta la llamada HTTP con la cabecera de autenticacion.
     *
     * <p>Los errores se clasifican en dos familias porque el servicio de pagos
     * reacciona distinto a cada una:
     * <ul>
     *   <li><b>4xx</b>: la orden esta mal (documento invalido, monto fuera de
     *       rango). Reintentar no arregla nada; el usuario debe corregir algo.</li>
     *   <li><b>5xx o sin respuesta</b>: fallo tecnico. La transaccion pudo
     *       haberse creado, asi que el pago queda pendiente y se reconcilia
     *       despues en vez de marcarlo como error.</li>
     * </ul>
     */
    private WompiDTOs.TransaccionRespuesta enviar(
            HttpMethod metodo, String url, Object cuerpo, String contexto) {
        var cabeceras = new org.springframework.http.HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.setAccept(List.of(MediaType.APPLICATION_JSON));
        // La autenticacion viaja en cabecera, nunca en la URL: si fuera parte
        // de la ruta, el secreto quedaria escrito en los logs del servidor y
        // de cualquier proxy intermedio.
        cabeceras.set("Authorization", "Bearer " + clavePublica);

        try {
            var respuesta = metodo == HttpMethod.GET
                    ? http.exchange(url, HttpMethod.GET, new org.springframework.http.HttpEntity<>(cabeceras),
                            WompiDTOs.TransaccionRespuesta.class).getBody()
                    : http.exchange(url, HttpMethod.POST,
                            new org.springframework.http.HttpEntity<>(cuerpo, cabeceras),
                            WompiDTOs.TransaccionRespuesta.class).getBody();

            if (respuesta == null) {
                throw new PasarelaPagosException("La pasarela respondio vacio a la operacion.");
            }
            return respuesta;

        } catch (HttpClientErrorException e) {
            log.warn("Wompi rechazo la operacion {} ({}): {}", contexto, e.getStatusCode(), resumir(e.getResponseBodyAsString()));
            throw PasarelaPagosException.rechazo(
                    "La pasarela rechazo la operacion (" + e.getStatusCode().value() + "). "
                            + resumir(e.getResponseBodyAsString()));

        } catch (HttpServerErrorException e) {
            log.error("Wompi fallo internamente en {} ({}). El pago quedara pendiente y se reconciliara.",
                    contexto, e.getStatusCode());
            throw new PasarelaPagosException(
                    "La pasarela presento un error temporal. Tu reserva sigue pendiente; no se realizo ningun cobro.", e);

        } catch (RestClientException e) {
            log.error("No se pudo comunicar con Wompi en {}: {}", contexto, e.getMessage());
            throw new PasarelaPagosException(
                    "No se pudo comunicar con la pasarela de pagos. Intenta de nuevo en unos segundos.", e);
        }
    }

    /** Recorta el cuerpo de un error para no volcar datos sensibles al usuario. */
    private String resumir(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return "sin detalle del proveedor.";
        }
        String limpio = cuerpo.replaceAll("\\s+", " ").trim();
        return limpio.length() > 200 ? limpio.substring(0, 200) + "..." : limpio;
    }

    /** Evita dobles barras al concatenar la base con la ruta del endpoint. */
    private static String sinBarraFinal(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Falta app.pagos.wompi.base-url en la configuracion.");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}