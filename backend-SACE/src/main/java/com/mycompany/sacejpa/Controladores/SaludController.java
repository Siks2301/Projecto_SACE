package com.mycompany.sacejpa.Controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de salud del servicio.
 *
 * <p>Sirve para tres cosas concretas:
 * <ol>
 *   <li>Las plataformas en la nube (Render, Railway, Fly.io, Koyeb) lo usan como
 *       <em>health check</em>: si el contenedor no responde 200, la plataforma lo
 *       reinicia o deja de enviarle trafico.</li>
 *   <li>La verificacion post-despliegue documentada en el Plan de Despliegue
 *       (verificacion de que la API quedo arriba y hablo con la base de datos).</li>
 *   <li>Un diagnostico rapido desde el navegador: abrir la URL no exige token.</li>
 * </ol>
 *
 * <p>Devuelve siempre HTTP 200 para que un problema transitorio de la base de
 * datos no provoque que la plataforma mate un contenedor que se puede recuperar.
 * El estado real de la base viaja en el campo {@code baseDatos}.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    @Autowired
    private DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> salud() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", "OK");
        respuesta.put("aplicacion", "SACE - Sistema de Atencion al Cliente (AleLeo Tours)");
        respuesta.put("version", "1.0.0");
        respuesta.put("baseDatos", estadoBaseDeDatos());
        respuesta.put("momento", Instant.now().toString());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Comprueba la conexion con PostgreSQL sin propagar la excepcion: un fallo
     * aqui es informacion util para el diagnostico, no un fallo del endpoint.
     */
    private String estadoBaseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(3) ? "conectada" : "sin respuesta";
        } catch (Exception e) {
            return "no disponible: " + e.getClass().getSimpleName();
        }
    }
}
