package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.PagoDTOs;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Seguridad.AuthInterceptor;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import com.mycompany.sacejpa.Servicios.PagoServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Endpoints de pagos para el usuario autenticado.
 *
 * <p>El flujo completo tiene tres pasos, y el orden importa:
 *
 * <ol>
 *   <li>{@code GET /api/pagos/cotizacion/{idSolicitud}} el backend informa el
 *       precio. El formulario lo muestra bloqueado.</li>
 *   <li>{@code POST /api/pagos} el cliente elige el metodo y arranca el cobro. El
 *       backend vuelve a calcular el precio y lo ignora por completo.</li>
 *   <li>{@code GET /api/pagos/estado/{referencia}} el navegador sondea mientras
 *       espera la confirmacion de la pasarela.</li>
 * </ol>
 *
 * <p>Nota de seguridad: este controlador NO acepta un monto. No es que lo
 * descarte al llegar: el DTO no tiene donde almacenarlo, asi que ni siquiera
 * existe la posibilidad de que alguien lo imponga.
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoServicio pagoServicio;

    /**
     * Recupera la sesion que dejo el interceptor.
     *
     * <p>Si no hay sesion se lanza excepcion en vez de seguir como anonimo: una
     * operacion de pago sin saber quien la pide nunca debe ejecutarse.
     */
    private TokenServicio.Sesion obtenerSesion(HttpServletRequest request) {
        TokenServicio.Sesion sesion =
                (TokenServicio.Sesion) request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (sesion == null) {
            throw new ValidacionException("Acceso denegado: debes iniciar sesion para realizar transacciones.");
        }
        return sesion;
    }

    /**
     * Configuracion de pagos: pasarela activa y metodos habilitados.
     *
     * <p>El frontend la consulta antes de pintar el modal, para no ofrecer un
     * metodo que el servidor va a rechazar.
     */
    @GetMapping("/configuracion")
    public ResponseEntity<PagoDTOs.ConfiguracionPagosDTO> configuracion() {
        return ResponseEntity.ok(pagoServicio.configuracion());
    }

    /**
     * Precio que SACE cobra por una solicitud.
     *
     * <p>Este endpoint es la fuente de verdad del monto. El campo del formulario
     * es solo una espejo: aunque el usuario lo altere, el servidor cobra lo que
     * dice el catalogo.
     */
    @GetMapping("/cotizacion/{idSolicitud}")
    public ResponseEntity<PagoDTOs.CotizacionDTO> cotizacion(
            @PathVariable Long idSolicitud,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        return ResponseEntity.ok(pagoServicio.cotizar(idSolicitud, sesion));
    }

    /**
     * Inicia un pago.
     *
     * <p>Devuelve 201 con el pago recien creado. La respuesta NO implica cobro
     * exitoso: incluye el estado, que solo pasara a APROBADO cuando la pasarela
     * lo confirme.
     */
    @PostMapping
    public ResponseEntity<PagoDTOs.PagoRespuestaDTO> iniciarPago(
            @RequestBody PagoDTOs.CrearPagoDTO dto,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        String ip = ipDelCliente(request);
        PagoDTOs.PagoRespuestaDTO resultado = pagoServicio.iniciarPago(dto, sesion, ip);
        return ResponseEntity.status(201).body(resultado);
    }

    /**
     * Consulta el estado de un pago.
     *
     * <p>Reconcilia contra la pasarela antes de responder, de modo que si el
     * webhook se perdio el usuario igual ve el pago como aprobado.
     */
    @GetMapping("/estado/{referencia}")
    public ResponseEntity<PagoDTOs.PagoRespuestaDTO> consultarEstado(
            @PathVariable String referencia,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        return ResponseEntity.ok(pagoServicio.consultarEstado(referencia, sesion));
    }

    /** Descarga el comprobante PDF de un pago ya aprobado. */
    @GetMapping("/comprobante/{idPago}")
    public ResponseEntity<InputStreamResource> obtenerComprobantePdf(
            @PathVariable Long idPago,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        File pdfFile = pagoServicio.obtenerArchivoPdfComprobante(idPago, sesion);

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + pdfFile.getName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.length())
                    .body(resource);
        } catch (Exception e) {
            // El archivo se borro entre que se verifico y que se abrio. Se
            // informa como error de validacion, no como fallo del servidor.
            throw new ValidacionException("No se pudo leer el comprobante. Intenta descargarlo de nuevo.");
        }
    }

    /** Pagos asociados a una solicitud. */
    @GetMapping("/solicitud/{idSolicitud}")
    public ResponseEntity<List<PagoDTOs.PagoRespuestaDTO>> obtenerPagosPorSolicitud(
            @PathVariable Long idSolicitud,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        return ResponseEntity.ok(pagoServicio.obtenerPagosPorSolicitud(idSolicitud, sesion));
    }

    /**
     * Extrae la IP del solicitante para la trazabilidad antifraude.
     *
     * <p>Solo se toma en cuenta la cabecera que agrega un proxy si el cliente no
     * se esta conectando directamente. Detras de un proxy, sin esta distincion,
     * todas las peticiones parecieran venir de la misma direccion.
     */
    private String ipDelCliente(HttpServletRequest request) {
        String directo = request.getRemoteAddr();
        if (directo == null || "127.0.0.1".equals(directo) || "::1".equals(directo)) {
            String reenviada = request.getHeader("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                return reenviada.split(",")[0].trim();
            }
        }
        return directo;
    }
}