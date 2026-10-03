-- =====================================================================
-- SACE / AleLeo Tours - Esquema de la tabla 'pago' con pasarela de pagos
-- =====================================================================
-- Este script crea la tabla desde cero. Si la base ya existe, NO lo ejecutes:
-- usa en su lugar migracion_pagos_wompi.sql, que conserva los pagos historicos.
--
-- Cambios frente al esquema anterior:
--   1. Se elimina 'llave_destino'. Ya no existe una "@VXM301" fija: la cuenta
--      que recibe el dinero es la de la pasarela, no un dato de negocio que
--      viva en el codigo.
--   2. 'estado' pasa a ser un estado real de transaccion (PENDIENTE, APROBADO,
--      RECHAZADO, ERROR, ANULADO). Antes valia siempre 'CONFIRMADO' porque el
--      sistema no consultaba a ninguna pasarela.
--   3. Se agrega 'referencia', que sirve de clave de idempotencia.
--   4. Se guardan los datos de trazabilidad de la pasarela.
--   5. 'fecha_pago' se parte en 'fecha_creacion' y 'fecha_aprobacion'.
-- =====================================================================

CREATE TABLE IF NOT EXISTS pago (
    id_pago             BIGSERIAL PRIMARY KEY,

    -- Relaciones. Ambas son obligatorias: un pago sin solicitud o sin cliente
    -- no tiene sentido de negocio.
    id_solicitud        BIGINT NOT NULL,
    id_cliente          BIGINT NOT NULL,

    -- Monto. Lo calcula el backend desde el precio del servicio; el cliente
    -- nunca lo elige. NUMERIC (y no FLOAT) porque los flotantes no representan
    -- bien el dinero.
    monto               NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    moneda              VARCHAR(3)   NOT NULL DEFAULT 'COP',

    -- Estado de la transaccion.
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                        CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO','ERROR','ANULADO')),

    -- Metodo elegido por el cliente. Es lo unico que el cliente decide.
    metodo_pago         VARCHAR(30)  NOT NULL
                        CHECK (metodo_pago IN ('PSE','TARJETA','NEQUI')),

    -- Referencia unica de la transaccion. Cumple dos funciones:
    --   1. es el identificador que la pasarela conoce;
    --   2. es la clave de idempotencia que impide cobrar dos veces.
    referencia          VARCHAR(64)  NOT NULL UNIQUE,

    -- Trazabilidad de la pasarela.
    pasarela            VARCHAR(30)  NOT NULL DEFAULT 'WOMPI',
    id_transaccion_pasarela VARCHAR(64) UNIQUE,
    codigo_autorizacion VARCHAR(30),

    -- Fechas separadas: una orden nace antes de que se sepa si se cobrara.
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_aprobacion    TIMESTAMP,

    -- Comprobante y notificaciones.
    url_pdf             VARCHAR(255),
    correo_notificado   BOOLEAN      NOT NULL DEFAULT FALSE,
    notas               VARCHAR(500),

    CONSTRAINT fk_pago_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitud(id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_cliente   FOREIGN KEY (id_cliente)   REFERENCES cliente(id)   ON DELETE CASCADE
);

-- Indices para las consultas mas frecuentes del modulo.
CREATE INDEX IF NOT EXISTS idx_pago_cliente   ON pago(id_cliente);
CREATE INDEX IF NOT EXISTS idx_pago_solicitud ON pago(id_solicitud);
CREATE INDEX IF NOT EXISTS idx_pago_estado    ON pago(estado);

-- =====================================================================
-- REGLA DE NEGOCIO IMPUESTA POR LA BASE DE DATOS
-- =====================================================================
-- Una solicitud no puede tener mas de UN pago aprobado.
--
-- Un indice unico PARCIAL: en PostgreSQL solo evalua la condicion WHERE, asi
-- que permite los N pagos PENDIENTE que uno quiera, pero solo deja pasar el
-- primero que llegue a APROBADO.
--
-- Esto no es una mejora cosmetics. El codigo Java ya consultaba si habia un pago
-- confirmado antes de insertar, pero entre esa consulta y el INSERT hay una
-- ventana: dos peticiones simultaneas (doble clic, pestanas duplicadas) podian
-- pasar las dos la validacion y cobrar dos veces. Aqui la garantia la impone la
-- base, que es donde no hay ventana.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_aprobado_por_solicitud
    ON pago(id_solicitud)
    WHERE estado = 'APROBADO';

-- =====================================================================
-- COMENTARIOS DE NEGOCIO (aparecen en la consola de la base)
-- =====================================================================
COMMENT ON TABLE pago IS
    'Ordenes de cobro de SACE. El monto lo fija el catalogo de servicios; el cliente solo elige el metodo.';
COMMENT ON COLUMN pago.referencia IS
    'Clave de idempotencia. Unica por pago; impide que un reintento genere un segundo cobro.';
COMMENT ON COLUMN pago.estado IS
    'PENDIENTE al crearse; solo la pasarela lo mueve a un estado final.';
COMMENT ON COLUMN pago.pasarela IS
    'WOMPI en produccion, SIMULADA cuando se trabaja sin conexion.';