package com.mycompany.sacejpa.Exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte los errores mas comunes del proyecto (que hoy llegan al cliente
 * como un 500 generico) en respuestas HTTP claras y con un mensaje
 * entendible. No hay que tocar ningun Servicio ni Controlador existente:
 * esto se activa solo por estar anotado con @RestControllerAdvice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Se dispara cuando Postgres rechaza un DELETE (o un INSERT/UPDATE) por
    // violar una llave foranea o una restriccion unica. Es exactamente lo
    // que pasa hoy al borrar un Cliente/Empleado/Servicio que todavia tiene
    // Solicitudes, o una Solicitud que todavia tiene Mensajes.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> manejarIntegridadReferencial(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return construirRespuesta(HttpStatus.CONFLICT,
                "No se pudo completar la operacion porque este registro todavia "
                        + "tiene otros datos relacionados (por ejemplo, Solicitudes, "
                        + "Mensajes o Servicios asociados). Elimina o reasigna esas "
                        + "relaciones primero.",
                detalle);
    }

    // Se dispara cuando llega un valor de enum invalido, por ejemplo
    // "estado": "activo" en vez de "ACTIVA", o un enum que no existe.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> manejarValorInvalido(IllegalArgumentException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST,
                "Uno de los valores enviados no es valido (por ejemplo, un estado, "
                        + "tipo o categoria que no existe).",
                ex.getMessage());
    }

    // Cubre los RuntimeException que ya lanzan los Servicios de este
    // proyecto, del estilo: throw new RuntimeException("Cliente no
    // encontrado con id: " + id). Si el mensaje habla de "no encontrado" o
    // "no encontrada" respondemos 404; cualquier otro RuntimeException se
    // trata como 400 (regla de negocio incumplida, ej. "Ya existe una
    // cuenta registrada con ese correo.", "Correo o contrasenia
    // incorrectos.").
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> manejarRuntimeException(RuntimeException ex) {
        String mensaje = ex.getMessage() != null ? ex.getMessage() : "Ocurrio un error inesperado.";
        boolean esNoEncontrado = mensaje.toLowerCase().contains("no encontrad");
        HttpStatus estado = esNoEncontrado ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return construirRespuesta(estado, mensaje, null);
    }

    // Errores de regla de negocio (ValidacionException): 400 con el mensaje que
    // lanzo el servicio. Declarado explicitamente para no depender del mapeo
    // por texto del manejador de RuntimeException.
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Object> manejarValidacion(ValidacionException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // Operar sobre recursos de otro usuario o de un rol que no aplica:
    // 403 Forbidden (el usuario esta autenticado pero no autorizado).
    @ExceptionHandler(AutorizacionException.class)
    public ResponseEntity<Object> manejarAutorizacion(AutorizacionException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    private ResponseEntity<Object> construirRespuesta(HttpStatus estado, String mensaje, String detalle) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("mensaje", mensaje);
        if (detalle != null) {
            cuerpo.put("detalle", detalle);
        }
        return new ResponseEntity<>(cuerpo, estado);
    }
}