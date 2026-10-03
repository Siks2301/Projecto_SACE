package com.mycompany.sacejpa.Repositorio;

import com.mycompany.sacejpa.Modelo.Pago;
import com.mycompany.sacejpa.Modelo.PagoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface Reposi_Pago extends JpaRepository<Pago, Long> {

    List<Pago> findByClienteId(Long clienteId);

    List<Pago> findBySolicitudId(Long solicitudId);

    /**
     * Busca un pago por su clave de idempotencia.
     *
     * <p>Es lo que permite que un doble clic en "Pagar" o un reintento del
     * navegador no genere una segunda orden de cobro.
     */
    Optional<Pago> findByReferencia(String referencia);

    /** Busca el pago asociado al identificador que dio la pasarela. */
    Optional<Pago> findByIdTransaccionPasarela(String idTransaccionPasarela);

    /**
     * Busca el pago ya aprobado de una solicitud, si existe.
     *
     * <p>La condicion de estado va en la consulta y no en un filtro de Java:
     * asi la base es la que garantiza que solo haya un cobro aprobado por
     * solicitud, sin depender de una ventana de tiempo entre leer y escribir.
     */
    Optional<Pago> findFirstBySolicitudIdAndEstado(Long solicitudId, PagoEstado estado);

    /**
     * Busca un pago pendiente de una solicitud para poder reutilizarlo en vez de
     * crear otro si el cliente habia cerrado el navegador sin terminar.
     */
    Optional<Pago> findFirstBySolicitudIdAndEstadoOrderByIdDesc(Long solicitudId, PagoEstado estado);

    @Query("SELECT p FROM Pago p WHERE p.fechaAprobacion >= :desde AND p.fechaAprobacion <= :hasta")
    List<Pago> findEntreFechas(@Param("desde") Date desde, @Param("hasta") Date hasta);

    /**
     * Total recaudado: solo cuenta pagos APROBADOS.
     *
     * <p>Los pagos pendientes o rechazados no son ingresos. Incluirlos
     * inflaria los reportes con dinero que nunca entro.
     *
     * <p>El {@code CAST} no es decorativo: al comparar un parametro con NULL
     * ({@code :desde IS NULL}) Hibernate no logra inferir el tipo y falla al
     * arrancar. El cast le dice que es una marca de tiempo.
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p "
            + "WHERE p.estado = com.mycompany.sacejpa.Modelo.PagoEstado.APROBADO "
            + "AND (CAST(:desde AS timestamp) IS NULL OR p.fechaAprobacion >= :desde) "
            + "AND (CAST(:hasta AS timestamp) IS NULL OR p.fechaAprobacion <= :hasta)")
    BigDecimal sumTotalMontoRecaudado(@Param("desde") Date desde, @Param("hasta") Date hasta);

    /** Cantidad de pagos aprobados en el rango, para el promedio. */
    @Query("SELECT COUNT(p) FROM Pago p "
            + "WHERE p.estado = com.mycompany.sacejpa.Modelo.PagoEstado.APROBADO "
            + "AND (CAST(:desde AS timestamp) IS NULL OR p.fechaAprobacion >= :desde) "
            + "AND (CAST(:hasta AS timestamp) IS NULL OR p.fechaAprobacion <= :hasta)")
    Long countPagosConfirmados(@Param("desde") Date desde, @Param("hasta") Date hasta);

    /**
     * Pagos que siguen esperando resultado de la pasarela.
     *
     * <p>Sirve como tarea de conciliacion: si un webhook se perdio, estos pagos
     * se pueden reconsultar contra la pasarela y resolverlos.
     */
    List<Pago> findByEstadoOrderByFechaCreacionAsc(PagoEstado estado);
}