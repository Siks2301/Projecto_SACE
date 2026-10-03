package com.mycompany.sacejpa.Modelo;

/**
 * Metodos de pago soportados por la pasarela.
 *
 * <p>El metodo es lo UNICO que elige el cliente. El monto nunca lo elige el
 * cliente: lo calcula el backend desde el precio del servicio reservado.
 * Separar estas dos decisiones es la regla de negocio central del modulo.
 */
public enum MetodoPago {

    /** Pago en linea contra el sistema de transferencias electrónicas (PSE). */
    PSE("PSE", "PSE (transferencia bancaria)"),

    /** Pago con tarjeta de credito o debito, tokenizada por la pasarela. */
    TARJETA("CARD", "Tarjeta de credito o debito"),

    /** Pago desde la billetera movil Nequi. */
    NEQUI("NEQUI", "Nequi (billetera movil)");

    /** Codigo que entiende la API de la pasarela. */
    private final String codigoPasarela;

    /** Descripcion para la interfaz de usuario. */
    private final String etiqueta;

    MetodoPago(String codigoPasarela, String etiqueta) {
        this.codigoPasarela = codigoPasarela;
        this.etiqueta = etiqueta;
    }

    public String codigoPasarela() {
        return codigoPasarela;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /**
     * Traduce un codigo de metodo al enum interno.
     *
     * <p>Al igual que {@link PagoEstado#desdeCodigo(String)}, este metodo es la
     * frontera de vocabulario del modulo y acepta tres idiomas:
     *
     * <ul>
     *   <li>el de la pasarela: CARD, NEQUI;</li>
     *   <li>el propio del enum: PSE, TARJETA, NEQUI;</li>
     *   <li>los del modelo viejo: TRANSFERENCIA y BRE-B, ambos significan
     *       "transferencia bancaria", es decir PSE.</li>
     * </ul>
     *
     * <p>Aceptar los valores legacy no es compatibilidad nostalgia: la base de
     * datos los tiene guardados y una lectura que no los contemplate fallaria o
     * devolveria null en produccion el primer dia.
     *
     * @param codigo texto recibido
     * @return metodo correspondiente, o null si no es valido
     */
    public static MetodoPago desdeCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String normalizado = codigo.trim().toUpperCase();
        for (MetodoPago metodo : values()) {
            if (metodo.name().equals(normalizado) || metodo.codigoPasarela.equals(normalizado)) {
                return metodo;
            }
        }
        // Valores del modelo anterior. Los dos designan una transferencia
        // bancaria, que es exactamente lo que hoy se llama PSE.
        if ("TRANSFERENCIA".equals(normalizado) || "BRE-B".equals(normalizado)
                || "BRE_B".equals(normalizado) || "BREB".equals(normalizado)) {
            return PSE;
        }
        return null;
    }

    /**
     * Igual que {@link #valueOf(String)} pero devolviendo null en vez de lanzar
     * excepcion si el nombre no existe.
     *
     * <p>Se usa al leer valores de configuracion: un nombre mal escrito debe
     * producir un aviso en el log, no impedir que la aplicacion arranque.
     *
     * @param nombre nombre del metodo
     * @return metodo correspondiente, o null si no existe
     */
    public static MetodoPago porNombre(String nombre) {
        return desdeCodigo(nombre);
    }
}