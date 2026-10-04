-- =============================================================================
-- Correccion de las rutas de comprobantes en la tabla pago.
-- =============================================================================
-- Por que este archivo existe:
--
-- pago.url_pdf guardaba la ruta ABSOLUTA del PDF en el momento de generarlo,
-- algo como:
--     C:\Users\cesar\OneDrive\Escritorio\backend-SACE\uploads\comprobantes\recibo_pago_1.pdf
--
-- Eso ata la base de datos a un unico equipo y a un unico usuario de Windows.
-- En otra maquina, al desplegar en un servidor, o simplemente tras cambiar de
-- usuario, esas rutas dejan de existir y el comprobante da 404 aunque el pago
-- este APROBADO y el PDF este en disco.
--
-- La aplicacion ya guarda rutas RELATIVAS (uploads/comprobantes/...), que se
-- resuelven contra el directorio de trabajo. Este script trae a la misma forma
-- los registros que quedaron con la ruta absoluta.
--
-- Ejecutar UNA sola vez:
--     psql -U postgres -d SACE_db -h localhost -f migracion_rutas_comprobantes.sql
--
-- Es idempotente: si se ejecuta dos veces, la segunda no cambia nada.
-- =============================================================================

BEGIN;

-- ------------------------------------------------------------------ 1. Diagnostico
-- Debe listar las filas que todavia tienen ruta absoluta. Anotar los ids: son
-- los que quedaran con "verificar a mano" al final.
SELECT id_pago,
       estado,
       CASE
           WHEN url_pdf ~ '^[A-Za-z]:[\\/]'            THEN 'RUTA ABSOLUTA DE WINDOWS'
           WHEN url_pdf ~ '^/'                           THEN 'RUTA ABSOLUTA POSIX'
           ELSE 'ya relativa'
       END AS estado_ruta,
       url_pdf
FROM pago
WHERE url_pdf IS NOT NULL
ORDER BY id_pago;

-- ------------------------------------------------------------------ 2. Convertir
-- Se conserva el nombre del archivo y se descarta todo lo anterior a el: es la
-- unica parte que identifica el comprobante. Los separadores se normalizan a
-- '/' para que la ruta sirva igual en Windows y en Linux.
DO $$
DECLARE
  corregidas integer;
BEGIN
  WITH normalizada AS (
    SELECT id_pago,
           'uploads/comprobantes/'
             || substring(url_pdf from '[^\\/]+$') AS ruta_relativa
    FROM pago
    WHERE url_pdf IS NOT NULL
      AND url_pdf ~ '[\\/]'
      AND url_pdf NOT LIKE 'uploads/comprobantes/%'
  )
  UPDATE pago p
  SET url_pdf = n.ruta_relativa
  FROM normalizada n
  WHERE p.id_pago = n.id_pago;

  GET DIAGNOSTICS corregidas = ROW_COUNT;
  RAISE NOTICE 'Rutas convertidas a relativas: %', corregidas;
END $$;

COMMIT;

-- ------------------------------------------------------------------ 3. Verificar
-- 1) No debe quedar ninguna ruta absoluta:
--
--   SELECT count(*) FROM pago WHERE url_pdf ~ '^[A-Za-z]:[\\/]' OR url_pdf ~ '^/';
--
-- 2) Todas las rutas deben haber quedado con la misma forma:
--
--   SELECT DISTINCT url_pdf FROM pago
--   WHERE url_pdf IS NOT NULL AND url_pdf NOT LIKE 'uploads/comprobantes/%';
--
-- 3) Aviso importante: esto reescribe la RUTA en la base, pero no mueve los
--    archivos. Los PDF deben existir dentro de
--    backend-SACE/uploads/comprobantes/. Comprobado desde el sistema de archivos:
--
--    cd backend-SACE/uploads/comprobantes
--    dir *.pdf
--
--    Para ver, desde la base, que nombre espera cada pago:
--
--    SELECT id_pago, 'uploads/comprobantes/' || substring(url_pdf from '[^\\/]+$')
--    FROM pago WHERE url_pdf IS NOT NULL ORDER BY id_pago;
--
-- Si un archivo falta, la aplicacion lo regenera solo la proxima vez que se
-- descarga el comprobante: obtenerArchivoPdfComprobante() detecta la ausencia y
-- vuelve a generar el PDF desde los datos del pago.
