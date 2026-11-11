-- =============================================================================
-- Numero de pasajeros por solicitud (cobro por persona).
-- =============================================================================
-- Por que este archivo existe:
--
-- El catalogo de servicios guarda el precio POR PERSONA, que es el que muestran
-- la portada y la pagina de destinos ("desde $320.000 por persona"). Pero el
-- pago cobraba una sola vez: una reserva para cuatro personas generaba un cobro
-- de $320.000 en lugar de $1.280.000.
--
-- El numero de viajeros solo existia dentro del texto libre del asunto y de la
-- descripcion ("Reserva para 4 persona(s)", "Pasajeros: 4"), donde no se puede
-- validar ni usar para calcular. Este script agrega la columna y rellena lo que
-- ya existe leyendo ese texto.
--
-- Ejecutar UNA sola vez (despues del despliegue del backend, o antes: el codigo
-- tolera que la columna no exista todavia? No: ejecutarlo junto al despliegue):
--     psql -U postgres -d SACE_db -h localhost -f migracion_pasajeros.sql
--
-- Es idempotente: si se ejecuta dos veces, la segunda no cambia nada.
-- =============================================================================

BEGIN;

-- ------------------------------------------------------------------ 1. Columna
ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS pasajeros integer;

-- ------------------------------------------------------------------ 2. Relleno
-- Se busca el numero junto a las palabras que ya usa el sistema. Se toman como
-- mucho dos digitos y luego se acota al rango real de la reserva (1..10).
WITH detectado AS (
    SELECT id,
           (regexp_match(
                coalesce(descripcion, '') || ' ' || coalesce(asunto, ''),
                '(?i)(?:pasajeros?|personas?|viajeros?)\s*[:=]?\s*([0-9]{1,2})'
           ))[1]::int AS viajeros
    FROM solicitud
    WHERE pasajeros IS NULL
)
UPDATE solicitud s
SET pasajeros = LEAST(10, GREATEST(1, d.viajeros))
FROM detectado d
WHERE s.id = d.id
  AND d.viajeros IS NOT NULL;

-- Lo que no se pudo leer del texto se cobra como una persona, igual que antes.
UPDATE solicitud SET pasajeros = 1 WHERE pasajeros IS NULL;

-- ------------------------------------------------------------------ 3. Reglas
ALTER TABLE solicitud ALTER COLUMN pasajeros SET DEFAULT 1;
ALTER TABLE solicitud ALTER COLUMN pasajeros SET NOT NULL;

ALTER TABLE solicitud DROP CONSTRAINT IF EXISTS solicitud_pasajeros_rango;
ALTER TABLE solicitud
    ADD CONSTRAINT solicitud_pasajeros_rango CHECK (pasajeros BETWEEN 1 AND 10);

COMMIT;

-- ------------------------------------------------------------------ 4. Verificar
-- 1) Ninguna solicitud debe quedar sin el dato:
--
--   SELECT count(*) FROM solicitud WHERE pasajeros IS NULL;          -- 0
--
-- 2) Lo que se rescato del texto historico:
--
--   SELECT id, pasajeros, left(coalesce(descripcion, ''), 60)
--   FROM solicitud ORDER BY id;
--
-- 3) La regla debe rechazar valores fuera de rango:
--
--   UPDATE solicitud SET pasajeros = 99 WHERE id = 1;                -- error CHECK
--
-- 4) Cuanto costaria hoy cada reserva (precio unitario x pasajeros):
--
--   SELECT s.id, s.pasajeros, sv.precio,
--          sv.precio * s.pasajeros AS total
--   FROM solicitud s
--   JOIN servicio sv ON sv.id = s.servicio_generado_id
--   ORDER BY s.id;
