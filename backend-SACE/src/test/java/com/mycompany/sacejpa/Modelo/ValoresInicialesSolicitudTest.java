package com.mycompany.sacejpa.Modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pruebas de los valores con los que nace una solicitud.
 *
 * <p>El API acepta un {@code POST /api/solicitudes} sin fecha, estado ni
 * prioridad (asi lo hace una integracion o una prueba manual). Antes esos
 * campos quedaban {@code NULL} en la base: la solicitud no salia en la
 * tendencia por fecha y no se podia filtrar por estado. Estas pruebas fijan el
 * comportamiento esperado.
 */
class ValoresInicialesSolicitudTest {

    @Test
    @DisplayName("CP-V1: una solicitud sin datos nace PENDIENTE, prioridad MEDIA, con fecha y un viajero")
    void completaTodosLosValoresVacios() {
        Solicitud solicitud = new Solicitud();

        Solicitud.aplicarValoresIniciales(solicitud);

        assertNotNull(solicitud.getFechaCreacion(), "la fecha de creacion no puede quedar nula");
        assertEquals(Solicitud.Estado.PENDIENTE, solicitud.getEstado());
        assertEquals(Solicitud.Prioridad.MEDIA, solicitud.getPrioridad());
        assertEquals(1, solicitud.getPasajeros());
    }

    @Test
    @DisplayName("CP-V2: los valores que ya trae la solicitud se respetan")
    void respetaLosValoresEnviados() {
        Date fecha = new Date(1_700_000_000_000L);
        Solicitud solicitud = new Solicitud();
        solicitud.setFechaCreacion(fecha);
        solicitud.setEstado(Solicitud.Estado.EN_PROCESO);
        solicitud.setPrioridad(Solicitud.Prioridad.ALTA);
        solicitud.setPasajeros(4);

        Solicitud.aplicarValoresIniciales(solicitud);

        assertSame(fecha, solicitud.getFechaCreacion());
        assertEquals(Solicitud.Estado.EN_PROCESO, solicitud.getEstado());
        assertEquals(Solicitud.Prioridad.ALTA, solicitud.getPrioridad());
        assertEquals(4, solicitud.getPasajeros());
    }

    @Test
    @DisplayName("CP-V3: si falta el numero de viajeros se rescata del texto de la reserva")
    void rescataPasajerosDelTexto() {
        Solicitud solicitud = new Solicitud();
        solicitud.setDescripcion("Plan: Todo incluido.\nPasajeros: 3.\nContacto: Ana.");

        Solicitud.aplicarValoresIniciales(solicitud);

        assertEquals(3, solicitud.getPasajeros());
    }
}
