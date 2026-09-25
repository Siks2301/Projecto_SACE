-- =============================================================================
-- Migracion de datos: identidad corporativa de AleLeo Tours.
-- =============================================================================
-- Acompana a schema_email_unico.sql, que crea la restriccion de unicidad. Este
-- script limpia los datos que hoy la violan y quita el dominio de la plantilla
-- anterior.
--
-- El problema concreto: el proyecto se llamo "On Vacation" durante el
-- desarrollo y al cambiar la marca a AleLeo Tours quedaron rastros en la base
-- y en las claves de almacenamiento del frontend. La marca solo se cambio en
-- las pantallas.
--
-- Que corrige:
--   1. admin@onvacation.com estaba en DOS personas (ids 2 y 4), y por eso ese
--      login devolvia 401 siempre: el backend busca por correo con
--      findByEmailIgnoreCase y encuentra la primera de las dos.
--   2. El resto de correos @onvacation.com.
--
-- Es idempotente: se puede ejecutar mas de veces. Si algo ya esta corregido, no
-- hace nada. Si encuentra un duplicado que no es el conocido, PARA y avisa en
-- vez de borrar a ciegas.
--
-- Ejecutar sobre la base ya migrada:
--     psql -U postgres -d SACE_db -h localhost -f schema_datos_corporativos.sql
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. Un unico administrador.
-- =============================================================================
-- El id 4 y el id 13 son la MISMA persona: mismo nombre, mismo documento
-- 100000001, mismo cargo y mismo departamento. El id 4 es la copia vieja, con
-- el dominio anterior, y es el que tiene las 19 solicitudes asignadas. El id 13
-- lo crea el DataInitializer del backend (admin@aleleotours.com) y es con el que
-- se trabaja.
--
-- Se conserva el id 13 porque el DataInitializer solo crea el administrador si
-- no encuentra ese correo: si se renombrara, el proximo arranque del servidor
-- crearia un cuarto administrador. Las solicitudes se le pasan al 13 para no
-- perder el historial.
DO $$
DECLARE
  v4 integer;
  v13 integer;
  sols integer;
BEGIN
  SELECT id INTO v4  FROM persona WHERE email = 'admin@onvacation.com' AND numero_documento = '100000001';
  SELECT id INTO v13 FROM persona WHERE email = 'admin@aleleotours.com';

  IF v13 IS NULL THEN
    RAISE EXCEPTION
      'No existe admin@aleleotours.com. Arrancaria el backend para que el '
      'DataInitializer lo cree, y despues se vuelve a ejecutar este script.';
  END IF;

  IF v4 IS NOT NULL THEN
    IF v4 = v13 THEN
      RAISE NOTICE 'El administrador ya esta unificado. No hay nada que hacer.';
    ELSE
      UPDATE solicitud SET empleado_asignado_id = v13 WHERE empleado_asignado_id = v4;
      GET DIAGNOSTICS sols = ROW_COUNT;
      DELETE FROM empleado WHERE id = v4;
      DELETE FROM persona  WHERE id = v4;
      RAISE NOTICE 'Unificado el administrador duplicado: % solicitudes pasaron al id %.', sols, v13;
    END IF;
  END IF;
END $$;

-- =============================================================================
-- 2. Fuera el residuo 'Admin Principal'.
-- =============================================================================
-- El id 2 repite admin@onvacation.com pero no es un duplicado del 13, asi que
-- no se puede fusionar. Es residuo de plantilla: esta guardado con el subtipo
-- Cliente aunque su rol dice ADMINISTRADOR, su documento es 0000000000 de
-- relleno, y sus 2 solicitudes (5 y 6) son el mensaje por defecto del chatbot.
-- Se borra su cascada.
DO $$
DECLARE
  v2 integer;
BEGIN
  SELECT id INTO v2 FROM persona
  WHERE email = 'admin@onvacation.com' AND numero_documento = '0000000000';

  IF v2 IS NOT NULL THEN
    DELETE FROM mensaje   WHERE solicitud_id IN (SELECT id FROM solicitud WHERE cliente_id = v2);
    DELETE FROM solicitud WHERE cliente_id = v2;
    DELETE FROM cliente   WHERE id = v2;
    DELETE FROM persona   WHERE id = v2;
    RAISE NOTICE 'Residuo Admin Principal (id %) eliminado.', v2;
  ELSE
    RAISE NOTICE 'El residuo Admin Principal ya no existe.';
  END IF;
END $$;

-- =============================================================================
-- 3. Dominio @onvacation.com -> @aleleotours.com
-- =============================================================================
-- Se hace por sufijo, no con una lista de ids, para que tambien cubra a quien
-- se agregue despues. lower() porque el indice de unicidad es sobre lower(email)
-- y el login compara sin distinguir mayusculas.
UPDATE persona
SET email = lower(split_part(email, '@', 1)) || '@aleleotours.com'
WHERE lower(email) LIKE '%@onvacation.com';

-- =============================================================================
-- 4. Comprobaciones. Si algo falla, se aborta y no se aplica nada.
-- =============================================================================
DO $$
DECLARE
  restantes integer;
  sueltos   integer;
BEGIN
  SELECT count(*) INTO restantes
  FROM (SELECT lower(email) FROM persona GROUP BY lower(email) HAVING count(*) > 1) d;
  IF restantes > 0 THEN
    RAISE EXCEPTION 'Quedan % correo(s) duplicado(s). No se aplica la migracion.', restantes;
  END IF;

  SELECT count(*) INTO sueltos FROM persona WHERE email ILIKE '%onvacation%';
  IF sueltos > 0 THEN
    RAISE EXCEPTION 'Quedan % persona(s) con correo @onvacation.com.', sueltos;
  END IF;

  SELECT count(*) INTO sueltos FROM solicitud s
  WHERE s.empleado_asignado_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM persona p WHERE p.id = s.empleado_asignado_id);
  IF sueltos > 0 THEN
    RAISE EXCEPTION 'Quedan % solicitud(es) con empleado huerfano.', sueltos;
  END IF;

  SELECT count(*) INTO sueltos FROM solicitud s
  WHERE s.cliente_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM persona p WHERE p.id = s.cliente_id);
  IF sueltos > 0 THEN
    RAISE EXCEPTION 'Quedan % solicitud(es) con cliente huerfano.', sueltos;
  END IF;

  RAISE NOTICE 'Comprobaciones OK. Sin duplicados, sin @onvacation y sin referencias huerfanas.';
END $$;

COMMIT;

-- ------------------------------------------------------------------ Verificar
--   SELECT id, tipo_usuario, nombre, apellido, email FROM persona
--   WHERE email ILIKE '%aleleotours%' ORDER BY id;
