-- =====================================================================
-- SACE / AleLeo Tours - Migracion del modulo de pagos a pasarela real
-- =====================================================================
-- CUANDO EJECUTARLO
--   Solo si la base de datos YA EXISTE y quieres conservar los pagos
--   historicos. Si vas a empezar de cero, usa schema_pagos.sql en su lugar.
--
-- POR QUE HACE FALTA
--   La tabla 'pago' cambio de forma. Spring Boot agrega columnas nuevas solo,
--   pero NO puede borrar columnas, renombrarlas ni convertir los valores del
--   estado. Ademas, hay filas con datos que ya no valen:
--
--     - estado = 'CONFIRMADO' : el sistema declaraba el pago exitoso sin que
--       ninguna pasarela lo hubiera confirmado. Esos pagos si se cobraron de
--       verdad, asi que se marcan como APROBADO en lugar de inventar otro valor.
--     - metodo_pago = 'TRANSFERENCIA' : valor unico del modelo viejo, que se
--       corresponde con PSE en el modelo nuevo.
--     - no tienen referencia: hay que generarsela, porque es la clave que
--       impide el doble cobro y la que la pasarela luego usara.
--     - hay solicitudes con VARIOS pagos 'CONFIRMADO': el modelo viejo no
--       impedia el doble cobro y se nota en los datos. El paso 7 los anula
--       (sin borrarlos) porque si no el indice unico no se puede crear.
--
-- COMO EJECUTARLO
--   psql -U postgres -d SACE_db -f migracion_pagos_wompi.sql
--
-- El script esta escrito para poder ejecutarse mas de una vez sin romper nada.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Columnas nuevas
-- ---------------------------------------------------------------------
-- "IF NOT EXISTS" evita el error si ya se habia ejecutado.
ALTER TABLE pago ADD COLUMN IF NOT EXISTS moneda    VARCHAR(3)   NOT NULL DEFAULT 'COP';
ALTER TABLE pago ADD COLUMN IF NOT EXISTS referencia VARCHAR(64);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS pasarela  VARCHAR(30)  NOT NULL DEFAULT 'MIGRADO';
ALTER TABLE pago ADD COLUMN IF NOT EXISTS id_transaccion_pasarela VARCHAR(64);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS codigo_autorizacion     VARCHAR(30);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_creacion  TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_aprobacion TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS notas VARCHAR(500);

-- ---------------------------------------------------------------------
-- 2. Reinterpretar la fecha unica que existia
-- ---------------------------------------------------------------------
-- Antes habia una sola columna, 'fecha_pago', que significaba "el dia que se
-- registro el pago". Ahora hay dos que significan cosas distintas:
--   fecha_creacion  = cuando se creo la orden de cobro
--   fecha_aprobacion= cuando la pasarela confirmo que el dinero entro
--
-- Para los pagos historicos ambas fechas coinciden. No es ideal, pero inventar
-- una fecha de aprobacion para un cobro que ocurrio antes de existir la pasarela
-- seria registrar un dato que el sistema no conocia.
--
-- Hay TRES estados posibles de esta tabla y el script debe comportarse bien en
-- los tres, porque ddl-auto=update deja la base en un estado intermedio:
--
--   A) solo 'fecha_pago'       -> se renombra a 'fecha_aprobacion'
--   B) solo 'fecha_aprobacion' -> no hay nada que hacer
--   C) AMBAS columnas          -> se copian los datos y se borra la vieja
--
-- El caso C es el que se da al ejecutar el script despues de que la aplicacion
-- ya arranco con la entidad nueva: Hibernate creo 'fecha_aprobacion' pero no toco
-- 'fecha_pago', que sigue con su NOT NULL. Sin este bloque la migracion falla y,
-- peor, la columna vieja sobrevive como dato duplicado y ambiguo.
--
-- Cada paso va dentro de un DO porque un 'SELECT ... WHERE column_name = X' no
-- evita que el SQL de dentro se valide al preparar la sentencia: si la columna
-- no existe, Postgres lanza error aunque la condicion sea falsa. Solo el SQL dinamico
-- (EXECUTE) se evalua en tiempo de ejecucion.
DO $$
BEGIN
    -- A) la vieja existe y la nueva todavia no: renombrar.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pago' AND column_name = 'fecha_pago'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pago' AND column_name = 'fecha_aprobacion'
    ) THEN
        EXECUTE 'ALTER TABLE pago RENAME COLUMN fecha_pago TO fecha_aprobacion';
    END IF;
END $$;

DO $$
BEGIN
    -- C) siguen conviviendo: copiar el dato a las dos columnas nuevas y
    -- eliminar la vieja, que ya seria un duplicado ambiguo.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pago' AND column_name = 'fecha_pago'
    ) THEN
        EXECUTE 'UPDATE pago SET fecha_creacion = COALESCE(fecha_creacion, fecha_pago)';
        EXECUTE 'UPDATE pago SET fecha_aprobacion = COALESCE(fecha_aprobacion, fecha_pago)';
        EXECUTE 'ALTER TABLE pago DROP COLUMN fecha_pago';
    END IF;
END $$;

-- La columna puede venir NOT NULL (era la fecha unica del modelo viejo) y un
-- pago pendiente no tiene fecha de aprobacion. Se relaja la restriccion.
ALTER TABLE pago ALTER COLUMN fecha_aprobacion DROP NOT NULL;

-- ---------------------------------------------------------------------
-- 3. Traducir los estados del modelo viejo al nuevo
-- ---------------------------------------------------------------------
-- 'CONFIRMADO' -> 'APROBADO'. Eran cobros reales, solo que sin pasarela que
-- los confirmara. Se conservan como aprobados para no perder la contabilidad.
UPDATE pago SET estado = 'APROBADO' WHERE estado = 'CONFIRMADO';
UPDATE pago SET estado = 'PENDIENTE' WHERE estado IS NULL;

-- ---------------------------------------------------------------------
-- 4. Traducir los metodos de pago
-- ---------------------------------------------------------------------
-- El modelo viejo escribia el metodo de tres formas distintas segun quien
-- hubiera hecho el INSERT: 'TRANSFERENCIA', 'BRE-B' o 'TARJETA'. Las dos
-- primeras significan lo mismo (transferencia bancaria), que es exactamente lo
-- que hoy se llama PSE. Sin esta traduccion, esos pagos quedarian con un valor
-- de metodo que el enum no reconoce y el sistema no podria ni leerlos ni
-- mostrarlos.
UPDATE pago SET metodo_pago = 'PSE'
    WHERE metodo_pago IN ('TRANSFERENCIA', 'BRE-B', 'BRE_B', 'BREB');
UPDATE pago SET metodo_pago = 'TARJETA' WHERE metodo_pago IS NULL;

-- ---------------------------------------------------------------------
-- 5. Generar la referencia de los pagos que no la tienen
-- ---------------------------------------------------------------------
-- Es OBLIGATORIA a partir de ahora porque es la clave de idempotencia. Se
-- rellena a partir del id para que sea determinista: si el script se repite,
-- genera la misma referencia y no se duplica nada.
UPDATE pago
SET referencia = 'LEGADO-' || LPAD(id_pago::TEXT, 9, '0')
WHERE referencia IS NULL;

-- ---------------------------------------------------------------------
-- 6. Quitar la columna de la llave de destino
-- ---------------------------------------------------------------------
-- Es el cambio con mas consecuencias simbolicas: el modelo viejo tenia
-- hardcodeada una "Bre-B @VXM301" que se escribia en cada comprobante. Ya no
-- aplica: la cuenta que recibe el dinero es la de la pasarela.
--
-- Antes de borrar, el dato se conserva por duplicado:
--   1. en la tabla pago_llave_destino_backup, para consultas SQL puntuales;
--   2. dentro de 'notas' de cada pago, para que siga siendo consultable desde
--      la aplicacion sin tener que volverse a la base.
-- La copia no se sobreescribe si ya existe: volver a ejecutar el script no debe
-- destruir el respaldo de la primera vez.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pago' AND column_name = 'llave_destino'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                       WHERE table_name = 'pago_llave_destino_backup') THEN
            EXECUTE 'CREATE TABLE pago_llave_destino_backup AS '
                    || 'SELECT id_pago, llave_destino FROM pago';
        END IF;

        EXECUTE 'UPDATE pago SET notas = COALESCE(notas, '''') '
                || '|| ''[Llave historica: '' || llave_destino || '']'' '
                || 'WHERE notas IS NULL OR notas NOT LIKE ''%[Llave historica:%''';

        EXECUTE 'ALTER TABLE pago DROP COLUMN llave_destino';
    END IF;
END $$;

-- ---------------------------------------------------------------------
-- 7. Resolver los cobros duplicados que YA existen
-- ---------------------------------------------------------------------
-- ESTE PASO NO ES OPCIONAL. Es el motivo mas comun de que esta migracion falle.
--
-- El paso 3 acaba de convertir todos los 'CONFIRMADO' en 'APROBADO'. Si una
-- solicitud tenia mas de un pago confirmado --y la tiene: hay solicitudes con
-- cuatro--, entonces el indice unico del paso 8 no se puede crear porque
-- violaria su propia restriccion. El script aborta y, al estar en una
-- transaccion, no cambia absolutamente nada.
--
-- Que existan esos duplicados no es hipotesis: es la prueba de que el modelo
-- viejo era INCAPAZ de impedir el doble cobro. Consultaba si la solicitud
-- tenia un pago confirmado y luego insertaba; entre ese SELECT y el INSERT hay
-- una ventana en la que dos clics simultaneos (o dos pestanas, o el boton
-- pulsado dos veces) alcanzan a publicar dos filas. Por eso el indice del paso 8
-- es indispensable y no es solo una buena practica.
--
-- Que se hace con ellos, y por que:
--   - Se CONSERVA el mas antiguo (menor id_pago) como APROBADO. Es el que tiene
--     el comprobante original y el que corresponde al cobro real.
--   - Los demas pasan a ANULADO, no se borran. Borrarlos destruiria evidencia
--     (incluidos los PDF ya emitidos) y alteraria los totales historiques sin
--     dejar rastro. Anularlos deja el libro honesto: el dinero entro una vez.
--   - Se escribe el motivo en 'notas', para que la decision sea auditable.
--
-- Si la base de datos tiene historial real de cobros, conviene revisar a mano
-- cuales quedaron APROBADO antes de continuar.
UPDATE pago p
SET estado = 'ANULADO',
    notas  = COALESCE(p.notas, '')
            || ' [ANULADO EN MIGRACION: cobro duplicado de la misma solicitud. '
            || 'Se conserva solo el registro mas antiguo como APROBADO.]'
WHERE p.estado = 'APROBADO'
  AND p.id_pago <> (
      SELECT MIN(p2.id_pago) FROM pago p2
      WHERE p2.id_solicitud = p.id_solicitud AND p2.estado = 'APROBADO'
  );

-- ---------------------------------------------------------------------
-- 8. Restricciones y indices
-- ---------------------------------------------------------------------
-- Estas dos columnas se crean arriba como nulables a proposito: si se declararan
-- NOT NULL desde el principio, el ALTER fallaria en cuanto la base tuviera
-- alguna fila historica. Ahora que el paso 2 y el paso 5 ya rellenaron los
-- huecos, si se endurece la restriccion.
ALTER TABLE pago ALTER COLUMN referencia    SET NOT NULL;
ALTER TABLE pago ALTER COLUMN fecha_creacion SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_referencia ON pago(referencia);
CREATE INDEX        IF NOT EXISTS idx_pago_estado    ON pago(estado);

-- La regla de negocio empujada a la base: una solicitud, un solo pago aprobado.
-- Ver la explicacion completa en schema_pagos.sql.
CREATE UNIQUE INDEX IF NOT EXISTS uq_pago_aprobado_por_solicitud
    ON pago(id_solicitud)
    WHERE estado = 'APROBADO';

-- El indice por defecto de id_transaccion_pasarela no sirve para nada si todos
-- los valores son NULL (que es el caso tras la migracion), asi que se omite a
-- proposito. Se creara solo cuando la pasarela empiece a poblarlo.

-- ---------------------------------------------------------------------
-- 9. Vista de compatibilidad
-- ---------------------------------------------------------------------
-- Mientras el frontend antiguo siga leyendo 'fecha_pago' y contando pagos
-- 'CONFIRMADO', esta vista evita romperlo. Es una red de seguridad para que la
-- migracion pueda hacerse sin detener el sistema; el codigo nuevo ya no la usa.
CREATE OR REPLACE VIEW pago_legacy AS
SELECT
    p.*,
    COALESCE(p.fecha_aprobacion, p.fecha_creacion) AS fecha_pago,
    CASE WHEN p.estado = 'APROBADO' THEN 'CONFIRMADO' ELSE p.estado::TEXT END AS estado_legacy
FROM pago p;

COMMIT;

-- =====================================================================
-- VERIFICACION (ejecutar despues del COMMIT)
-- =====================================================================
-- 1) Los estados viejos deben haber desaparecido:
--      SELECT estado, COUNT(*) FROM pago GROUP BY estado;
--      -> Debe verse APROBADO y ANULADO. Ningun 'CONFIRMADO'.
--
-- 2) Todo pago debe tener referencia:
--      SELECT COUNT(*) FROM pago WHERE referencia IS NULL;
--      -> Debe ser 0.
--
-- 3) La regla de un solo cobro por solicitud debe cumplirse:
--      SELECT id_solicitud, COUNT(*) FROM pago WHERE estado = 'APROBADO'
--        GROUP BY id_solicitud HAVING COUNT(*) > 1;
--      -> No debe devolver filas. Si devolviera alguna, el paso 7 no se aplico
--         y el indice del paso 8 tampoco habria podido crearse.
--
-- 4) Revisar que cobros anulados quedaron con su motivo:
--      SELECT id_pago, id_solicitud, estado, notas FROM pago
--       WHERE estado = 'ANULADO';
--      -> Cada fila debe explicar por que fue anulada.