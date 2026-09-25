-- =============================================================================
-- Correccion de unicidad del correo en la tabla persona.
-- =============================================================================
-- Por que este archivo existe:
--
-- La entidad Persona.java declara @Column(name = "email", nullable = false,
-- unique = true). Eso basta para una base NUEVA, porque ddl-auto=update crea
-- la tabla desde cero respetando la unicidad. Pero sobre una base que YA
-- existe, Hibernate NO anade indices unicos: solo crea tablas y columnas que
-- falten. Por eso la restriccion hay que crearla a mano una vez.
--
-- Sin esta restriccion el bug vuelve: el login busca por correo con
-- findByEmailIgnoreCase, asi que dos personas con el mismo correo hacen que
-- ese login falle siempre para las dos. Sucedio con admin@onvacation.com,
-- que estaba duplicado en los ids 2 y 4.
--
-- Ejecutar UNA sola vez sobre una base ya existente:
--     psql -U postgres -d SACE_db -h localhost -f schema_email_unico.sql
--
-- Es idempotente: si la restriccion ya existe, no hace nada.
-- =============================================================================

BEGIN;

-- ------------------------------------------------------------------ 1. Limpieza
-- Sin esto el ALTER fallaria, porque hasta ahora la base permitia duplicados.
-- Solo se toca lo que de verdad estara repetido; el script avisa y aborta si
-- encuentra algo que no esperaba, para no borrar datos por sorpresa.
DO $$
DECLARE
  repetido record;
BEGIN
  FOR repetido IN
    SELECT lower(email) AS correo, array_agg(id ORDER BY id) AS ids
    FROM persona
    GROUP BY lower(email)
    HAVING count(*) > 1
  LOOP
    RAISE EXCEPTION
      'No se puede crear la restriccion: el correo % esta en las personas %. '
      'Resuelve el duplicado a mano y vuelve a ejecutar este script.',
      repetido.correo, repetido.ids;
  END LOOP;

  -- El indice unico es sensible a mayusculas, pero el login usa
  -- findByEmailIgnoreCase. Sin normalizar, Persona@x.com y persona@x.com
  -- passarian el indice y aun asi colisionarian en el login.
  IF EXISTS (SELECT 1 FROM persona WHERE email IS NULL OR btrim(email) = '') THEN
    RAISE EXCEPTION
      'No se puede crear la restriccion: hay personas con correo vacio o nulo.';
  END IF;

  RAISE NOTICE 'Sin duplicados ni correos vacios: se puede crear la restriccion.';
END $$;

-- ------------------------------------------------- 2. Normalizar a minusculas
-- Para que el indice unico cubra tambien las mayusculas, que es como compara
-- el login. Sin este paso, Persona@x.com y persona@x.com estarian permitidos.
-- lower() es estable con la configuracion regional, por eso se fija 'C'.
UPDATE persona SET email = lower(btrim(email));

---- ------------------------------------------------------- 3. Crear la restriccion
ALTER TABLE persona
  DROP CONSTRAINT IF EXISTS persona_email_unico;

ALTER TABLE persona
  ADD CONSTRAINT persona_email_unico UNIQUE (email);

-- Cuadra con nullable = false de la entidad.
ALTER TABLE persona
  ALTER COLUMN email SET NOT NULL;

-- ------------------------------------------------- 4. Indice sin distinguir mayusculas
-- Un UNIQUE (email) NO alcanza: en PostgreSQL es sensible a mayusculas, asi que
-- ADMIN@ALELEOTOURS.COM pasaria el indice al lado de admin@aleleotours.com.
-- Pero el login busca con findByEmailIgnoreCase, que genera
-- upper(email) = upper(?), de modo que los dos SI colisionarian en el login y
-- el fallo volveria exactamente igual. Por eso el indice va sobre lower(email).
--
-- PostgreSQL no admite expresiones dentro de un UNIQUE, solo en un indice, por
-- eso esto va aparte de la restriccion de arriba. La de arriba se mantiene
-- porque es la que declara la entidad y la que ddl-auto=update crearia en una
-- base nueva.
CREATE UNIQUE INDEX IF NOT EXISTS persona_email_unico_lower
  ON persona (lower(email));

COMMIT;

-- ------------------------------------------------------------------ 5. Verificar
-- Debe salir la restriccion:
--   SELECT conname, pg_get_constraintdef(oid)
--   FROM pg_constraint
--   WHERE conrelid = 'persona'::regclass AND conname = 'persona_email_unico';
--
-- Y este INSERT debe fallar con 'duplicate key value violates unique constraint':
--   BEGIN;
--   INSERT INTO persona (tipo_persona_discriminador, nombre, apellido, email,
--                         tipo_usuario, estado_acceso, contrasenia)
--   VALUES ('Empleado','Prueba','Duplicado','admin@aleleotours.com',
--           'ADMINISTRADOR','ACTIVA','x');
--   ROLLBACK;
