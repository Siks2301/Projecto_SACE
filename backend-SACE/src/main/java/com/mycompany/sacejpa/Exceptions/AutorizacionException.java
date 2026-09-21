package com.mycompany.sacejpa.Exceptions;

/**
 * El usuario esta autenticado pero intenta operar sobre recursos que no son
 * suyos (o que su rol no le permite tocar), por ejemplo leer los pagos de la
 * solicitud de otro cliente. Se distingue de una validacion de negocio (400)
 * para que el API responda 403 Forbidden.
 */
public class AutorizacionException extends RuntimeException {

    public AutorizacionException(String mensaje) {
        super(mensaje);
    }
}
