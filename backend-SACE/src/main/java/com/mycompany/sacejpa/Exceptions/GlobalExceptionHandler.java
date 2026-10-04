package com.mycompany.sacejpa.Exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    // Se dispara cuando Postgres rechaza una operacion por violar una
    // restriccion. Ojo: DataIntegrityViolationException no significa solo
    // "hay datos relacionados", tambien cubre otros fallos de integridad, y
    // responder siempre 409 hacia que el usuario persiguiera un problema que
    // no tiene. Se clasifica por el SQLState de PostgreSQL para que el mensaje
    // diga la verdad:
    //
    //   22001  el texto no cabe en la columna   -> 400, no es un conflicto
    //   23503  llave foranea                    -> 409, si hay datos relacionados
    //   23505  valor duplicado                  -> 409, ya existe
    //
    // El caso 22001 se veia a diario: al escribir mas de 255 caracteres en la
    // descripcion de una solicitud, el navegador recibia "este registro todavia
    // tiene otros datos relacionados, elimina o reasigna esas relaciones", y
    // el usuario se iba a borrar datos de verdad para un problema de longitud.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> manejarIntegridadReferencial(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String sqlState = sqlStateDe(ex);

        if ("22001".equals(sqlState)) {
            return construirRespuesta(HttpStatus.BAD_REQUEST,
                    "Alguno de los textos enviados es mas largo de lo que admite "
                            + "el campo. Revisa el detalle para saber cual es.",
                    detalle);
        }
        if ("23505".equals(sqlState)) {
            return construirRespuesta(HttpStatus.CONFLICT,
                    "Ya existe un registro con esos mismos datos unicos.",
                    detalle);
        }
        return construirRespuesta(HttpStatus.CONFLICT,
                "No se pudo completar la operacion porque este registro todavia "
                        + "tiene otros datos relacionados (por ejemplo, Solicitudes, "
                        + "Mensajes o Servicios asociados). Elimina o reasigna esas "
                        + "relaciones primero.",
                detalle);
    }

    /**
     * Busca el SQLState de PostgreSQL recorriendo toda la cadena de causas.
     *
     * <p>Spring envuelve el fallo de PostgreSQL en varias capas
     * (DataIntegrityViolationException -&gt; PSQLException -&gt; ServerErrorMessage),
     * asi que el codigo no esta ni en la primera ni en la ultima: hay que
     * recorrer la cadena y quedarse con el primer SQLException que aparezca.
     *
     * @param ex excepcion a desenvolver
     * @return codigo SQLState de PostgreSQL, o null si no se pudo determinar
     */
    private String sqlStateDe(Throwable ex) {
        Throwable actual = ex;
        int guardia = 0;
        while (actual != null && guardia++ < 20) {
            if (actual instanceof java.sql.SQLException) {
                String state = ((java.sql.SQLException) actual).getSQLState();
                if (state != null) {
                    return state;
                }
            }
            Throwable siguiente = actual.getCause();
            if (siguiente == actual) {
                break;
            }
            actual = siguiente;
        }
        return null;
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

    // El cuerpo JSON no se pudo convertir al objeto esperado. Es el caso que
    // produce el DTO de pagos cuando alguien intenta enviar un "monto": el
    // deserializador lo detecta y lanza, y Jackson envuelve ese fallo aqui.
    //
    // Sin este handler Spring responderia con su propio error generico de
    // parseo, que no le dice nada al usuario. Con el, se traduce la causa real
    // para que el mensaje que llega al navegador sea entendible.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        String causa = causaRaiz(ex).getMessage();
        String mensaje = causa != null && causa.contains("El monto no lo puede elegir el cliente")
                ? causa
                : "El cuerpo de la peticion no tiene el formato esperado. Revisa los campos enviados.";

        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje, causa);
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

    /**
     * Desciende hasta la excepcion original.
     *
     * <p>Spring envuelve el fallo real del deserializador en varias capas
     * (HttpMessageNotReadableException -> JsonMappingException -> la excepcion
     * que lanzo el codigo). Sin desempaquetar, el mensaje util queda enterrado y
     * solo se ve la envoltura.
     *
     * @param ex excepcion a desenvolver
     * @return la excepcion mas profunda de la cadena
     */
    private Throwable causaRaiz(Throwable ex) {
        Throwable actual = ex;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        return actual;
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