package com.mycompany.sacejpa.Config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Convierte a relativas las rutas absolutas de comprobantes que quedaran en
 * {@code pago.url_pdf}.
 *
 * <p>El servicio ya guarda rutas relativas, pero las filas escritas antes de ese
 * cambio guardaban la ruta absoluta del equipo que las creo
 * ({@code C:\Users\cesar\...}). Esos comprobantes no abren en otra maquina ni en
 * un servidor. Este inicializador hace la conversion al arrancar, de modo que no
 * depende de que alguien recuerde ejecutar
 * {@code migracion_rutas_comprobantes.sql}.
 *
 * <p>Es idempotente: la segunda vez que arranca no encuentra nada que convertir.
 * Solo reescribe la columna; los archivos no se mueven. Si un PDF no esta en
 * disco, la aplicacion lo regenera desde los datos del pago cuando el cliente
 * descarga el comprobante.
 */
@Component
public class RutaComprobanteInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RutaComprobanteInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Se respeta la misma propiedad que usa el servicio al escribir. */
    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            // Diagnosticar antes de tocar nada: deja constancia de que se cambio.
            Integer absolutas = contarRutasAbsolutas();
            if (absolutas != null && absolutas > 0) {
                logger.warn("RutaComprobanteInitializer: {} pago(s) con ruta absoluta de comprobante. "
                        + "Se convertiran a relativa ({}/...), porque una ruta absoluta solo "
                        + "funciona en el equipo que la genero.", absolutas, normalizar());
            }

            // La condicion de "ya es relativa" debe usar el mismo prefijo con su
            // barra final. Si se pasa como parametro separado, el LIKE buscaria
            // 'uploads/comprobantes//%', que no coincide nunca, y cada arranque
            // volveria a anteponer el prefijo encima de la ruta ya convertida.
            int corregidas = jdbcTemplate.update(
                    "UPDATE pago SET url_pdf = ? || substring(url_pdf from '[^\\\\/]+$') "
                            + "WHERE url_pdf ~ '[\\\\/]' AND url_pdf NOT LIKE ?",
                    normalizar() + "/", normalizar() + "/%");

            if (corregidas > 0) {
                logger.info("RutaComprobanteInitializer: {} ruta(s) de comprobante convertida(s) "
                        + "a relativa. Los comprobantes ahora se abren desde cualquier maquina.",
                        corregidas);
            }
        } catch (Exception e) {
            // No es motivo para impedir el arranque: la descarga de comprobantes
            // resuelve por nombre cuando la ruta no cuadra.
            logger.warn("RutaComprobanteInitializer: no se pudieron normalizar las rutas de "
                    + "comprobante ({}). Se puede aplicar migracion_rutas_comprobantes.sql "
                    + "manualmente.", e.getMessage());
        }
    }

    private Integer contarRutasAbsolutas() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pago "
                        + "WHERE url_pdf ~ '^[A-Za-z]:[\\\\/]' OR url_pdf ~ '^/'",
                Integer.class);
    }

    /** La ruta se guarda siempre con '/', para que sirva en Windows y en Linux. */
    private String normalizar() {
        return uploadDir.replace('\\', '/').replaceAll("/+$", "");
    }
}
