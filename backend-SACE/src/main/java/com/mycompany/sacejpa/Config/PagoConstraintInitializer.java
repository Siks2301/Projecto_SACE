package com.mycompany.sacejpa.Config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PagoConstraintInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PagoConstraintInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void init() {
        try {
            // Garantiza que exista la referencia unica.
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_referencia " +
                "ON pago(referencia)"
            );

            // Regla de negocio: una solicitud, un solo pago APROBADO.
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_aprobado_por_solicitud " +
                "ON pago(id_solicitud) " +
                "WHERE estado = 'APROBADO'"
            );

            // Indices auxiliares que ayudan a las consultas; idempotentes.
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_pago_estado ON pago(estado)"
            );

            logger.info("PagoConstraintInitializer: indices de integridad del modulo de pagos verificados (uq_pago_referencia, uq_pago_aprobado_por_solicitud, idx_pago_estado).");
        } catch (Exception e) {
            logger.warn("PagoConstraintInitializer: no se pudieron garantizar los indices del modulo de pagos. " +
                        "Esto solo afecta a un despliegue limpio sin haber ejecutado migracion_pagos_wompi.sql. " +
                        "Motivo: {}", e.getMessage());
        }
    }
}
