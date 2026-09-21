package com.mycompany.sacejpa.Seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Emite y valida tokens de acceso firmados (HMAC-SHA256).
 *
 * El token tiene forma {@code cuerpo.firma}, donde el cuerpo es un JSON
 * base64url con {sub, email, tipo, exp} y la firma es el HMAC del cuerpo
 * usando el secreto de la aplicacion. Al no haber base de datos de sesiones,
 * cualquier instancia puede validar el token sin estado compartido.
 *
 * IMPORTANTE: el secreto debe sobreescribirse via la variable de entorno
 * SACE_TOKEN_SECRET en produccion. El valor por defecto es solo para
 * desarrollo (ver application.properties ).
 */
@Service
public class TokenServicio {

    // Constante de sesion: expiracion por defecto en horas si falta la propiedad.
    private static final long HORAS_POR_DEFECTO = 24;

    private final String secreto;
    private final long expiracionMillis;

    public TokenServicio(
            @Value("${app.security.token.secreto:}") String secreto,
            @Value("${app.security.token.horas-expiracion:" + HORAS_POR_DEFECTO + "}") long horasExpiracion) {
        this.secreto = (secreto == null || secreto.isBlank()) ? String.valueOf(System.nanoTime()) : secreto;
        this.expiracionMillis = (horasExpiracion > 0 ? horasExpiracion : HORAS_POR_DEFECTO) * 3600_000L;
    }

    /** Usuario autenticado extraido de un token valido. */
    public record Sesion(Long id, String email, String tipoUsuario) {
    }

    /** Genera un token firmado y con vencimiento para un usuario. */
    public String emitir(Long id, String email, String tipoUsuario) {
        if (id == null) {
            throw new IllegalArgumentException("No se puede emitir token sin id de usuario.");
        }
        String cuerpo = base64Url(jsonSerializar(id, email, tipoUsuario));
        return cuerpo + "." + firma(cuerpo);
    }

    /**
     * Valida firma y vencimiento. Devuelve la sesion autenticada o {@code null}
     * si el token es invalido, fue alterado o expiro.
     */
    public Sesion validar(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] partes = token.split("\\.", -1);
        if (partes.length != 2) {
            return null;
        }
        String esperada = firma(partes[0]);
        if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
                partes[1].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Long exp = jsonNumero(json, "exp");
        if (exp == null || exp < System.currentTimeMillis()) {
            return null;
        }
        Long id = jsonNumero(json, "sub");
        if (id == null) {
            return null;
        }
        return new Sesion(id, jsonString(json, "email"), jsonString(json, "tipo"));
    }

    // ------------------------------------------------------------------
    // Firma HMAC-SHA256
    // ------------------------------------------------------------------

    private String firma(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token.", e);
        }
    }

    private String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // JSON minimo (sin dependencias externas): el formato lo controlamos
    // nosotros y solo tiene 4 campos conocidos.
    // ------------------------------------------------------------------

    private String jsonSerializar(Long id, String email, String tipoUsuario) {
        return "{\"sub\":" + id
                + ",\"email\":" + jsonStringRaw(email)
                + ",\"tipo\":" + jsonStringRaw(tipoUsuario)
                + ",\"exp\":" + (System.currentTimeMillis() + expiracionMillis) + "}";
    }

    private static String jsonStringRaw(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                default:
                    if (c < 32) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append("\"").toString();
    }

    private static String jsonString(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx < 0) {
            return null;
        }
        idx = json.indexOf(':', idx);
        if (idx < 0) {
            return null;
        }
        int i = idx + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean escapado = false;
        i++;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (escapado) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u': {
                        if (i + 4 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append('u');
                            }
                        }
                        break;
                    }
                    default: sb.append(c);
                }
                escapado = false;
            } else if (c == '\\') {
                escapado = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    private static Long jsonNumero(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int idx = json.indexOf(patron);
        if (idx < 0) {
            return null;
        }
        idx = json.indexOf(':', idx);
        if (idx < 0) {
            return null;
        }
        int i = idx + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) {
            i++;
        }
        int inicio = i;
        while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') {
            i++;
        }
        try {
            return Long.parseLong(json.substring(inicio, i).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}