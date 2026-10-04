-- ===========================================================================
-- Migracion: textos largos en columnas que estaban en varchar(255)
-- ===========================================================================
-- Por que: estas columnas guardan texto libre escrito por el usuario, pero
-- estaban declaradas como varchar(255). Al escribir mas de 255 caracteres,
-- PostgreSQL rechazaba el INSERT/UPDATE y la aplicacion respondia 409 con un
-- mensaje que decia "este registro tiene otros datos relacionados", cuando el
-- problema real era unicamente la longitud del texto. El mensaje era falso.
--
-- Se amplian a TEXT. Es un cambio seguro: en PostgreSQL pasar de varchar(n) a
-- text no recorta nada, no pierde datos y no rompe comparaciones ni indices
-- existentes sobre esas columnas (no habia indices sobre estos campos).
--
-- Las columnas NO tocadas (nombre, apellido, email, telefono, estado, tipo,
-- url_pdf, etc.) se quedan en varchar(255): esos valores son cortos por
-- definicion y el limite es una proteccion, no un estorbo.
--
-- Ejecutar:  psql -U postgres -d SACE_db -f migracion_textos_largos.sql
-- Es idempotente: se puede ejecutar mas de una vez sin efecto.
-- ===========================================================================

BEGIN;

-- --- Solicitudes: la descripcion es el requerimiento detallado del cliente --
ALTER TABLE solicitud ALTER COLUMN descripcion TYPE TEXT;
ALTER TABLE solicitud ALTER COLUMN asunto       TYPE TEXT;

-- --- Mensajes: el contenido es lo que escribe el cliente, el asesor o el
--- chatbot. El chatbot redacta respuestas largas por definicion. -----------
ALTER TABLE mensaje ALTER COLUMN contenido            TYPE TEXT;
ALTER TABLE mensaje ALTER COLUMN comentario_satisfaccion TYPE TEXT;

-- --- Servicios: la descripcion del paquete (incluye, no incluye...) -------
ALTER TABLE servicio ALTER COLUMN descripcion TYPE TEXT;

-- --- Cliente: historial de busquedas y preferencias comunicativas --------
ALTER TABLE cliente ALTER COLUMN historial_consultas      TYPE TEXT;
ALTER TABLE cliente ALTER COLUMN preferencias_comunicacion TYPE TEXT;

-- --- Preguntas frecuentes del chatbot: la respuesta puede ser larga -------
ALTER TABLE pregunta_frecuente ALTER COLUMN pregunta TYPE TEXT;
ALTER TABLE pregunta_frecuente ALTER COLUMN respuesta TYPE TEXT;

COMMIT;

-- ===========================================================================
-- Verificacion
-- ===========================================================================
-- Estas columnas ya no deben aparecer como varchar(255):
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND data_type = 'text'
ORDER BY table_name, column_name;
