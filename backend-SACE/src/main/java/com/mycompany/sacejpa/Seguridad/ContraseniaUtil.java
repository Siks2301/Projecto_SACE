package com.mycompany.sacejpa.Seguridad;

import com.mycompany.sacejpa.Exceptions.ValidacionException;


public final class ContraseniaUtil {

    private static final int LONGITUD_MINIMA = 8;

    private ContraseniaUtil() {
    }

    /** True si cumple todas las reglas de fortaleza. */
    public static boolean esValida(String contrasenia) {
        return validarReglas(contrasenia) == null;
    }

    /**
     * Valida la fortaleza y lanza {@link ValidacionException} con el primer
     * requisito incumplido (mensaje entendible, mismo estilo del frontend).
     */
    public static void validar(String contrasenia) {
        String error = validarReglas(contrasenia);
        if (error != null) {
            throw new ValidacionException(error);
        }
    }

    /** Devuelve el primer error o null si la contrasena es valida. */
    private static String validarReglas(String contrasenia) {
        if (contrasenia == null || contrasenia.length() < LONGITUD_MINIMA) {
            return "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres.";
        }
        if (!contrasenia.chars().anyMatch(Character::isUpperCase)) {
            return "La contraseña debe incluir al menos una letra mayúscula (A-Z).";
        }
        if (!contrasenia.chars().anyMatch(Character::isLowerCase)) {
            return "La contraseña debe incluir al menos una letra minúscula (a-z).";
        }
        if (!contrasenia.chars().anyMatch(Character::isDigit)) {
            return "La contraseña debe incluir al menos un número (0-9).";
        }
        // Mismo set de caracteres especiales que el frontend (js/app.js,
        // validarFortalezaContrasenia), para que la regla no diverja.
        String especiales = "!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?";
        if (!contrasenia.matches(".*[" + especiales + "].*")) {
            return "La contraseña debe incluir al menos un carácter especial (ej: @, #, $, %, !).";
        }
        return null;
    }
}