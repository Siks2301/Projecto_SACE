package com.mycompany.sacejpa.Pagos.Pasarela;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Verificacion criptografica de las comunicaciones con Wompi.
 *
 * <p>Son dos mecanismos distintos y ambos hacen falta:
 *
 * <ol>
 *   <li><b>Firma de integridad</b> ({@link #calcularFirmaIntegridad}): SACE la
 *       calcula al crear la transaccion para que la pasarela sepa que la orden
 *       no fue alterada por el camino. El algoritmo documentado es
 *       {@code SHA256(referencia + montoCentavos + moneda + claveIntegridad)}.</li>
 *   <li><b>Checksum del evento</b> ({@link #verificarEvento}): es Wompi quien
 *       firma sus avisos. Si SACE no lo comprueba, cualquiera que conozca la
 *       URL del webhook podria "aprobar" un pago que nunca ocurrio, porque los
 *       eventos llegan sin autenticacion previa.</li>
 * </ol>
 *
 * <p>El algoritmo del checksum es {@code SHA256(valores + timestamp + secreto)},
 * donde {@code valores} es la concatenacion de los campos indicados en
 * {@code signature.properties}, en ese mismo orden.
 */
@Service
public class WompiFirmaServicio {

    private static final Logger log = LoggerFactory.getLogger(WompiFirmaServicio.class);

    /**
     * Clave de integridad de la pasarela. Solo se usa para FIRMAR (al crear la
     * transaccion); nunca se envia al navegador ni se registra en la base.
     */
    private final String claveIntegridad;

    /** Secreto de eventos de la pasarela. Solo se usa para VERIFICAR. */
    private final String secretoEventos;

    public WompiFirmaServicio(
            @Value("${app.pagos.wompi.clave-integridad:}") String claveIntegridad,
            @Value("${app.pagos.wompi.secreto-eventos:}") String secretoEventos) {
        this.claveIntegridad = claveIntegridad != null ? claveIntegridad.trim() : "";
        this.secretoEventos = secretoEventos != null ? secretoEventos.trim() : "";
    }

    /**
     * Calcula la firma de integridad de una orden.
     *
     * <p>Verificado contra el ejemplo oficial de la documentacion de Wompi:
     * con la referencia {@code sk8-438k4-xmxm392-sn2m249}, monto
     * {@code 0000} COP y la clave {@code prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6}
     * produce {@code 37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5}.
     *
     * @param referencia   referencia unica de la transaccion
     * @param montoCentavos monto entero en centavos
     * @param moneda       codigo de moneda, por ejemplo COP
     * @return hash SHA-256 en hexadecimal minuscula
     * @throws IllegalStateException si no hay clave de integridad configurada
     */
    public String calcularFirmaIntegridad(String referencia, long montoCentavos, String moneda) {
        if (claveIntegridad.isEmpty()) {
            throw new IllegalStateException(
                    "Falta app.pagos.wompi.clave-integridad: no se puede firmar la transaccion.");
        }
        // Concatenacion SIN separadores, tal como lo define la documentacion.
        String semilla = referencia + montoCentavos + moneda + claveIntegridad;
        return sha256Hex(semilla);
    }

    /** Indica si hay clave de integridad configurada. */
    public boolean tieneClaveIntegridad() {
        return !claveIntegridad.isEmpty();
    }

    /** Indica si hay secreto de eventos configurado. */
    public boolean tieneSecretoEventos() {
        return !secretoEventos.isEmpty();
    }

    /**
     * Verifica que un evento sea auténtico.
     *
     * <p>Reconstruye el texto a hashear recorriendo {@code signature.properties}
     * sobre el JSON crudo del evento y compara el resultado con el checksum
     * recibido. La comparacion es de tiempo constante para que un atacante no
     * pueda deducir el valor correcto probando byte a byte.
     *
     * @param evento evento ya deserializado
     * @param jsonCrudo cuerpo del evento tal como llego por el cable, usado para
     *                 leer los valores en las rutas declaradas
     * @return true si el checksum coincide
     */
    public boolean verificarEvento(WompiDTOs.Evento evento, JsonNode jsonCrudo) {
        if (evento == null || evento.signature() == null) {
            log.warn("Evento de Wompi recibido sin bloque 'signature': se rechaza.");
            return false;
        }
        if (secretoEventos.isEmpty()) {
            // Fallar abierto aqui seria un agujero: mejor rechazar el evento y
            // que el cliente lo resuelva por la via de consulta (polling).
            log.error("Evento de Wompi recibido pero falta app.pagos.wompi.secreto-eventos: se rechaza.");
            return false;
        }

        String checksumRecibido = evento.signature().checksum();
        if (checksumRecibido == null || checksumRecibido.isBlank()) {
            log.warn("Evento de Wompi sin checksum: se rechaza.");
            return false;
        }

        List<String> properties = evento.signature().properties();
        if (properties == null || properties.isEmpty()) {
            log.warn("Evento de Wompi sin 'signature.properties': no se puede verificar, se rechaza.");
            return false;
        }

        StringBuilder texto = new StringBuilder();
        for (String ruta : properties) {
            JsonNode valor = resolverRuta(jsonCrudo, ruta);
            if (valor == null || valor.isMissingNode()) {
                // Si falta un campo firmado, el evento esta incompleto o fue
                // manipulado. No se adivina: se rechaza.
                log.warn("El evento declara la propiedad '{}' pero no viene en el cuerpo. Se rechaza.", ruta);
                return false;
            }
            texto.append(valor.isValueNode() ? valor.asText() : valor.toString());
        }
        texto.append(evento.signature().timestamp() != null ? evento.signature().timestamp() : "");
        texto.append(secretoEventos);

        String checksumCalculado = sha256Hex(texto.toString());
        boolean coincide = compararTiempoConstante(checksumCalculado, checksumRecibido.trim());

        if (!coincide) {
            log.warn("Checksum invalido en evento de Wompi: se rechaza el aviso.");
        }
        return coincide;
    }

    /**
     * Resuelve una ruta con notacion de punto sobre el JSON del evento.
     *
     * <p>Ejemplo: {@code transaction.id} o {@code transaction.amount_in_cents}.
     *
     * @param raiz nodo raiz del evento
     * @param ruta ruta separada por puntos
     * @return nodo encontrado, o null si la ruta no existe
     */
    private JsonNode resolverRuta(JsonNode raiz, String ruta) {
        if (raiz == null || ruta == null || ruta.isBlank()) {
            return null;
        }
        JsonNode actual = raiz;
        for (String parte : ruta.split("\\.")) {
            if (actual == null || !actual.isObject()) {
                return null;
            }
            actual = actual.get(parte);
        }
        return actual;
    }

    /**
     * Compara dos cadenas en tiempo constante.
     *
     * <p>Un {@code equals} normal devuelve false en el primer caracter que
     * difiere, lo que filtra informacion sobre el valor secreto. Aqui se
     * comparan todos los caracteres siempre.
     */
    private boolean compararTiempoConstante(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] bytesA = a.getBytes(StandardCharsets.UTF_8);
        byte[] bytesB = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bytesA, bytesB);
    }

    /**
     * Calcula el SHA-256 de un texto y lo devuelve en hexadecimal.
     *
     * @param texto cadena de entrada
     * @return hash de 64 caracteres en minuscula
     */
    public static String sha256Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM: si no existe, la plataforma
            // esta rota y no tiene sentido seguir.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", e);
        }
    }

    }