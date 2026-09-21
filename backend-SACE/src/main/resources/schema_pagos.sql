-- Script SQL de creacion de la tabla 'pago' para SACE / AleLeo Tours
CREATE TABLE IF NOT EXISTS pago (
    id_pago BIGSERIAL PRIMARY KEY,
    id_solicitud BIGINT NOT NULL,
    id_cliente BIGINT NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    fecha_pago TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_pago VARCHAR(50) NOT NULL,
    llave_destino VARCHAR(100) NOT NULL DEFAULT 'Bre-B @VXM301',
    estado VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADO',
    url_pdf VARCHAR(255),
    correo_notificado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pago_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitud(id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pago_cliente ON pago(id_cliente);
CREATE INDEX IF NOT EXISTS idx_pago_solicitud ON pago(id_solicitud);
