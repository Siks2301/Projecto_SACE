package com.mycompany.sacejpa.Repositorio;

import com.mycompany.sacejpa.Modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface Reposi_Pago extends JpaRepository<Pago, Long> {

    List<Pago> findByClienteId(Long clienteId);

    List<Pago> findBySolicitudId(Long solicitudId);

    @Query("SELECT p FROM Pago p WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta")
    List<Pago> findEntreFechas(@Param("desde") Date desde, @Param("hasta") Date hasta);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'CONFIRMADO' AND (CAST(:desde AS timestamp) IS NULL OR p.fechaPago >= :desde) AND (CAST(:hasta AS timestamp) IS NULL OR p.fechaPago <= :hasta)")
    BigDecimal sumTotalMontoRecaudado(@Param("desde") Date desde, @Param("hasta") Date hasta);

    @Query("SELECT COUNT(p) FROM Pago p WHERE p.estado = 'CONFIRMADO' AND (CAST(:desde AS timestamp) IS NULL OR p.fechaPago >= :desde) AND (CAST(:hasta AS timestamp) IS NULL OR p.fechaPago <= :hasta)")
    Long countPagosConfirmados(@Param("desde") Date desde, @Param("hasta") Date hasta);
}
