package com.mycompany.sacejpa.Pagos.Pasarela;

/**
 * Error de negocio o de transporte al hablar con la pasarela de pagos.
 *
 * <p>Se distingue del resto de excepciones porque el modulo de pagos debe
 * diferenciar dos situaciones que se responden de forma distinta:
 * <ul>
 *   <li><b>rechazo definitivo</b>: la pasarela contesto que no se puede cobrar
 *       (datos invalidos, monto invalido). El pago pasa a RECHAZADO y el
 *       usuario debe corregir algo.</li>
 *   <li><b>fallo tecnico</b>: no hubo respuesta, o la respuesta no se pudo
 *       entender. El pago queda PENDIENTE y el usuario puede reintentar, porque
 *       es posible que la transaccion si se haya creado.</li>
 * </ul>
 */
public class PasarelaPagosException extends RuntimeException {

    /** true si el cobro se descarto de forma definitiva y no vale la pena reintentar. */
    private final boolean rechazoDefinitivo;

    public PasarelaPagosException(String mensaje) {
        this(mensaje, null, false);
    }

    public PasarelaPagosException(String mensaje, Throwable causa) {
        this(mensaje, causa, false);
    }

    public PasarelaPagosException(String mensaje, Throwable causa, boolean rechazoDefinitivo) {
        super(mensaje, causa);
        this.rechazoDefinitivo = rechazoDefinitivo;
    }

    /** Error que invalida la orden: no tiene sentido pedirle un reintento al usuario. */
    public static PasarelaPagosException rechazo(String mensaje) {
        return new PasarelaPagosException(mensaje, null, true);
    }

    public boolean esRechazoDefinitivo() {
        return rechazoDefinitivo;
    }
}