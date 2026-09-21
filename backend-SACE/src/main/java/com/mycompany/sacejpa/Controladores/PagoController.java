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

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoServicio pagoServicio;

    private TokenServicio.Sesion obtenerSesion(HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion) request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (sesion == null) {
            throw new ValidacionException("Acceso denegado: Debes iniciar sesion para realizar transacciones.");
        }
        return sesion;
    }

    /**
     * POST /api/pagos: Procesa el pago, valida que el monto sea > 0 y asocia obligatoriamente la llave Bre-B @VXM301.
     */
    @PostMapping
    public ResponseEntity<PagoDTOs.PagoRespuestaDTO> procesarPago(
            @RequestBody PagoDTOs.CrearPagoDTO dto,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        PagoDTOs.PagoRespuestaDTO resultado = pagoServicio.procesarPago(dto, sesion);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/pagos/comprobante/{idPago}: Devuelve el archivo PDF del comprobante.
     * Seguridad: Valida mediante Token Bearer que el usuario sea el Cliente dueño del pago, un Empleado o Administrador.
     */
    @GetMapping("/comprobante/{idPago}")
    public ResponseEntity<InputStreamResource> obtenerComprobantePdf(
            @PathVariable Long idPago,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        File pdfFile = pagoServicio.obtenerArchivoPdfComprobante(idPago, sesion);

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdfFile.getName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.length())
                    .body(resource);
        } catch (Exception e) {
            throw new ValidacionException("Error al leer el archivo del comprobante PDF.");
        }
    }

    /**
     * GET /api/pagos/solicitud/{idSolicitud}: Devuelve los pagos asociados a una solicitud.
     */
    @GetMapping("/solicitud/{idSolicitud}")
    public ResponseEntity<List<PagoDTOs.PagoRespuestaDTO>> obtenerPagosPorSolicitud(
            @PathVariable Long idSolicitud,
            HttpServletRequest request) {
        TokenServicio.Sesion sesion = obtenerSesion(request);
        List<PagoDTOs.PagoRespuestaDTO> lista = pagoServicio.obtenerPagosPorSolicitud(idSolicitud, sesion);
        return ResponseEntity.ok(lista);
    }
}
