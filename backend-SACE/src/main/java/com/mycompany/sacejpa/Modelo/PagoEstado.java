package com.mycompany.sacejpa.Modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados posibles de un pago y reglas de transicion entre ellos.
 *
 * <p>El ciclo de vida de un pago real NO es "se creo y quedo confirmado":
 * una transacion nace en la pasarela como PENDING y solo cuando esa pasarela
 * notifica (webhook) o se consulta (polling) llega a un estado final. Modelar
 * esa diferencia es lo que separa un sistema de pagos de un contador.
 *
 * <pre>
 *   PENDIENTE ──▶ APROBADO    (la pasarela confirmo el cobro)
 *       │       ├─▶ RECHAZADO   (la pasarela rechazo el cobro)
 *       │       ├─▶ ERROR      (fallo de la pasarela, se puede reintentar)
 *       │       └─▶ ANULADO    (cancelado por el usuario o por un asesor)
 *       │
 *       └──▶ (ningun otro estado; los finales son inmutables)
 * </pre>
 */
public enum PagoEstado {

    /** Transaccion creada en la pasarela, aun sin resultado definitivo. */
    PENDIENTE,

    /** La pasarela confirmo el cobro. Unico estado que genera comprobante. */
    APROBADO,

    /** La pasarela rechazo el cobro (fondos insuficientes, tarjeta invalida). */
    RECHAZADO,

    /** Error tecnico de la pasarela. El usuario puede reintentar. */
    ERROR,

    /** Cobro cancelado desde SACE, sin llegar a la pasarela o tras un reembolso. */
    ANULADO;

    private static final Set<PagoEstado> ESTADOS_FINALES =
            EnumSet.of(APROBADO, RECHAZADO, ERROR, ANULADO);

    /**
     * Estado final: no admite mas cambios salvo una anulacion explicita del
     * administrador, que se hace desde un metodo aparte ({@code anularPorAdmin}).
     */
    public boolean esFinal() {
        return ESTADOS_FINALES.contains(this);
    }

    /** Un estado que ya genero comprobante no se puede volver a mover. */
    public boolean esAprobado() {
        return this == APROBADO;
    }

    /**
     * Regla de transicion. Solo se permite salir de PENDIENTE hacia un estado
     * final: un pago aprobado o rechazado jamas vuelve a "pendiente" ni se
     * reescribe su estado, porque eso permitiria reprocesar un cobro ya cerrado.
     *
     * @param destino estado destino propuesto
     * @return true si la transicion es legal
     */
    public boolean puedeTransicionarA(PagoEstado destino) {
        if (destino == null || destino == this) {
            return false;
        }
        return switch (this) {
            case PENDIENTE -> switch (destino) {
                case APROBADO, RECHAZADO, ERROR, ANULADO -> true;
                case PENDIENTE -> false;
            };
            default -> false;
        };
    }

    /**
     * Traduce un codigo de estado al enum interno de SACE.
     *
     * <p>Este unico metodo es la frontera de vocabulario del proyecto, y
     * deliberadamente acepta los tres idiomas que coexisten:
     *
     * <ul>
     *   <li><b>El de la pasarela</b> (Wompi): APPROVED, DECLINED, PENDING,
     *       VOIDED, ERROR.</li>
     *   <li><b>El propio del enum</b>: APROBADO, RECHAZADO, PENDIENTE,
     *       ANULADO. Se aceptan porque son los nombres que aparecen en el
     *       archivo de configuracion, en la documentacion y en las consultas
     *       SQL, asi que escribirlos debe funcionar igual que escribir
     *       APPROVED.</li>
     *   <li><b>El legacy del proyecto</b>: CONFIRMADO y TRANSFERENCIA, para no
     *       romper los registros historicos ya guardados en la base.</li>
     * </ul>
     *
     * <p>Que un metodo acepte tres vocabularios no es descuido: es lo que
     * permite que una pasarela externa, la base de datos y el archivo de
     * configuracion hablen cada uno en su idioma sin que el dominio tenga que
     * traducir en un solo lugar.
     *
     * @param codigo codigo de estado recibido
     * @return estado interno correspondiente, o null si el codigo no existe
     */
    public static PagoEstado desdeCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return switch (codigo.trim().toUpperCase()) {
            case "PENDING", "PENDIENTE" -> PENDIENTE;
            case "APPROVED", "APROBADO", "CONFIRMADO" -> APROBADO;
            case "DECLINED", "REJECTED", "RECHAZADO" -> RECHAZADO;
            case "ERROR" -> ERROR;
            case "VOIDED", "CANCELLED", "CANCELED", "ANULADO" -> ANULADO;
            default -> null;
        };
    }

    /**
     * Igual que {@link #valueOf(String)} pero devolviendo null en vez de lanzar
     * excepcion cuando el nombre no existe, y tolerando mayusculas.
     *
     * <p>Se usa al leer valores venidos de configuracion, donde un nombre mal
     * escrito debe producir un aviso en el log, no una excepcion que impida
     * que la aplicacion arranque.
     *
     * @param nombre nombre del estado
     * @return estado correspondiente, o null si no existe
     */
    public static PagoEstado porNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return desdeCodigo(nombre);
    }

    /** Texto corto y legible para mostrar en la interfaz. */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE -> "Pendiente";
            case APROBADO -> "Aprobado";
            case RECHAZADO -> "Rechazado";
            case ERROR -> "Error";
            case ANULADO -> "Anulado";
        };
    }
}