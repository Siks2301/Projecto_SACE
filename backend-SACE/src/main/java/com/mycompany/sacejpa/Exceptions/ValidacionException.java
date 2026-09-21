package com.mycompany.sacejpa.Exceptions;

/**
 * Error de validacion de reglas de negocio (p. ej. contrasena debil).
 *
 * Se distingue de un conflicto (email duplicado = 409) para que el
 * {@link GlobalExceptionHandler} pueda responder 400 con un mensaje claro.
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}