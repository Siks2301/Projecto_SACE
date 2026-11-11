package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.Modelo.Solicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de la regla de cobro por pasajero.
 *
 * <p>El catalogo de servicios guarda el precio <b>por persona</b>, pero el
 * sistema cobraba una sola vez: una reserva para cuatro personas se pagaba
 * como si fuera de una. Estas pruebas fijan la regla para que no vuelva a
 * romperse en silencio.
 */
class CobroPorPasajerosTest {

    private static final BigDecimal PRECIO_CARTAGENA = new BigDecimal("320000.00");

    @Test
    @DisplayName("CP-P1: el total es el precio unitario multiplicado por los viajeros")
    void totalMultiplicaPorPasajeros() {
        assertEquals(new BigDecimal("640000.00"),
                PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, 2));
        assertEquals(new BigDecimal("1280000.00"),
                PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, 4));
    }

    @Test
    @DisplayName("CP-P2: un solo viajero paga exactamente el precio del catalogo")
    void unViajeroPagaElUnitario() {
        assertEquals(PRECIO_CARTAGENA, PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, 1));
    }

    @Test
    @DisplayName("CP-P3: cantidades invalidas se cobran como una persona")
    void cantidadInvalidaSeCobraComoUno() {
        assertEquals(PRECIO_CARTAGENA, PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, 0));
        assertEquals(PRECIO_CARTAGENA, PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, -3));
    }

    @Test
    @DisplayName("CP-P4: mas de 10 viajeros se acota al tope de la reserva")
    void topeDeDiezViajeros() {
        assertEquals(new BigDecimal("3200000.00"),
                PagoServicio.totalConPasajeros(PRECIO_CARTAGENA, 25));
    }

    @Test
    @DisplayName("CP-P5: el total conserva los centavos sin perder dinero")
    void conservaCentavos() {
        assertEquals(new BigDecimal("599999.97"),
                PagoServicio.totalConPasajeros(new BigDecimal("199999.99"), 3));
    }

    @Test
    @DisplayName("CP-P6: el numero de viajeros enviado por el cliente se respeta y acota")
    void normalizaElNumeroEnviado() {
        assertEquals(3, Solicitud.normalizarPasajeros(3, null, null));
        assertEquals(1, Solicitud.normalizarPasajeros(0, null, null));
        assertEquals(10, Solicitud.normalizarPasajeros(50, null, null));
        assertEquals(1, Solicitud.normalizarPasajeros(null, null, null));
    }

    @Test
    @DisplayName("CP-P7: las reservas historicas rescatan los viajeros del texto libre")
    void rescataPasajerosDelTexto() {
        assertEquals(2, Solicitud.normalizarPasajeros(
                null, "Reserva: Cartagena", "Plan: Todo incluido.\nPasajeros: 2.\nContacto: Ana."));
        assertEquals(4, Solicitud.normalizarPasajeros(
                null, "Reserva para 4 persona(s) - San Andres", "Sin detalle."));
    }

    @Test
    @DisplayName("CP-P8: una solicitud sin dato ni texto se cobra como una persona")
    void sinDatoNiTextoEsUno() {
        Solicitud solicitud = new Solicitud();
        assertEquals(1, solicitud.pasajerosEfectivos());

        solicitud.setPasajeros(6);
        assertEquals(6, solicitud.pasajerosEfectivos());

        // Si la columna quedo nula pero el texto menciona viajeros, manda el texto.
        Solicitud historica = new Solicitud();
        historica.setDescripcion("Pasajeros: 3. Fecha de viaje: 20/12/2026.");
        assertEquals(3, historica.pasajerosEfectivos());
    }
}
