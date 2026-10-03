package com.mycompany.sacejpa.Controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.sacejpa.Modelo.PagoEstado;
import com.mycompany.sacejpa.Pagos.Pasarela.PasarelaPagos;
import com.mycompany.sacejpa.Pagos.Pasarela.WompiDTOs;
import com.mycompany.sacejpa.Pagos.Pasarela.WompiFirmaServicio;
import com.mycompany.sacejpa.Servicios.PagoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint que recibe los avisos de la pasarela de pagos.
 *
 * <h2>Por que este controlador es publico</h2>
 * Wompi no tiene sesion de usuario ni puede enviar el token de SACE: solo puede
 * hacer un POST a una URL. Por eso la ruta queda fuera del interceptor de
 * autenticacion (ver {@code WebConfig}) y la seguridad se traslada COMPLETAMENTE
 * al cuerpo del mensaje.
 *
 * <h2>Como se compensa que sea publico</h2>
 * Un endpoint sin verificar permitiria a cualquiera "aprobar" un pago falso, lo
 * que equivaldria a regalar viajes. Por eso la validacion no es opcional:
 *
 * <ol>
 *   <li>Se recalcula el checksum SHA-256 del evento usando el secreto que solo
 *       conocen Wompi y el backend. Si no coincide, se rechaza. Un atacante
 *       puede enviar lo que quiera, pero no puede producir un checksum valido.</li>
 *   <li>Se busca el pago por su referencia, que es la que SACE genero. Un evento
 *       sobre una referencia desconocida se ignora.</li>
 *   <li>La maquina de estados impide retroceder: un pago ya resuelto no cambia.</li>
 * </ol>
 *
 * <p>Ese patron (un webhook publico_autenticado por firma) es exactamente lo que
 * usan los sistemas de pago reales, y es un buen punto para explicar en la
 * sustentacion como se resuelve la paradoja de "publico pero seguro".
 */
@RestController
@RequestMapping("/api/pagos/webhook")
public class WebhookPagoController {

    private static final Logger log = LoggerFactory.getLogger(WebhookPagoController.class);

    @Autowired
    private PagoServicio pagoServicio;

    @Autowired
    private WompiFirmaServicio firma;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Recibe un evento de la pasarela.
     *
     * @param cuerpo cuerpo del evento tal como llego por el cable
     * @return 200 para que la pasarela deje de reenviar
     */
    @PostMapping("/wompi")
    public ResponseEntity<Map<String, String>> recibirEventoWompi(@RequestBody String cuerpo) {
        WompiDTOs.Evento evento;
        JsonNode jsonCrudo;

        try {
            jsonCrudo = mapper.readTree(cuerpo);
            evento = mapper.treeToValue(jsonCrudo, WompiDTOs.Evento.class);
        } catch (Exception e) {
            // Un cuerpo que no se puede leer no se procesa, pero se responde 400
            // para que la pasarela no insista: reenviar un mensaje ilegible una
            // y otra vez no va a arreglarlo.
            log.warn("Evento de Wompi ilegible: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "estado", "rechazado",
                    "motivo", "El cuerpo del evento no es valido."));
        }

        if (!firma.verificarEvento(evento, jsonCrudo)) {
            // Se responde 401 para que quede claro que el problema es de
            // autenticidad del mensaje, no de formato.
            log.warn("Evento de Wompi con firma invalida. Se rechaza sin aplicar ningun cambio.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "estado", "rechazado",
                    "motivo", "La firma del evento no es valida."));
        }

        // A partir de aqui el evento esta autenticado: si no, el checksum habria
        // fallado y ya se habria retornado.
        if (evento.data() == null || evento.data().transaction() == null) {
            return ResponseEntity.ok(Map.of(
                    "estado", "ignorado",
                    "motivo", "El evento no trae informacion de transaccion."));
        }

        WompiDTOs.TransaccionRespuesta t = evento.data().transaction();
        PagoEstado estado = PagoEstado.desdeCodigo(t.status());

        if (estado == null) {
            log.warn("Evento de Wompi con estado desconocido '{}'. Se responde 200 para no generar reintentos.", t.status());
            return ResponseEntity.ok(Map.of(
                    "estado", "ignorado",
                    "motivo", "Estado de transaccion no reconocido."));
        }

        PasarelaPagos.TransaccionPasarela transaccion = new PasarelaPagos.TransaccionPasarela(
                t.id(),
                t.reference(),
                estado,
                t.paymentMethodType(),
                t.authorizationCode(),
                t.amountInCents());

        var pago = pagoServicio.aplicarEventoDePasarela(transaccion);

        if (pago == null) {
            log.info("Evento legitimo pero sin pago asociado. referencia={}", t.reference());
            return ResponseEntity.ok(Map.of(
                    "estado", "ignorado",
                    "motivo", "La transaccion no corresponde a un pago de SACE."));
        }

        log.info("Evento aplicado. referencia={} estado={}", pago.getReferencia(), pago.getEstado());
        return ResponseEntity.ok(Map.of(
                "estado", "procesado",
                "referencia", String.valueOf(pago.getReferencia()),
                "resultado", pago.getEstado().etiqueta()));
    }

    /**
     * Aviso de que la pasarela quiere verificar que el webhook esta vivo.
     *
     * <p>Devuelve un 200 sin pedir autenticacion. No filtra informacion: solo
     * confirma que el endpoint existe.
     */
    @PostMapping("/verificacion")
    public ResponseEntity<Map<String, String>> verificarEndpoint() {
        return ResponseEntity.ok(Map.of(
                "estado", "ok",
                "mensaje", "Endpoint de notificacion activo para SACE / AleLeo Tours."));
    }
}