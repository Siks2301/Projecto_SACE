# =====================================================================
# Despliegue de SACE en la nube
# ---------------------------------------------------------------------
# Guia paso a paso para publicar el proyecto. Tres caminos posibles,
# de mas sencillo a mas control:
#
#   Opcion A (gratis, recomendada)  Cloudflare Pages + Render + Neon
#   Opcion B (pago, la mas comoda)  Cloudflare Pages + Railway
#   Opcion C (control total)        Servidor propio con Docker Compose
#
# Antes de empezar, dos preparaciones:
#   1. Tener el dominio (aleleotours.com). Se registra en Cloudflare
#      Registrar, que lo vende a coste y de paso te da DNS + SSL.
#   2. Verificar que el repositorio este actualizado:
#        git push origin main
# =====================================================================


# =====================================================================
# ESTRUCTURA DEL DESPLIEGUE
# =====================================================================

    Navegador
        |
        v
    +---------------------------------------------------------------+
    |  FRONTEND (estatico)                 Cloudflare Pages          |
    |  frontend-aleleo-tours/  ->  https://www.aleleotours.com     |
    |  Sin servidor, sin build, CDN global, HTTPS automatico         |
    +------------------------------+--------------------------------+
                                   |  fetch() a /api/...
                                   v
    +---------------------------------------------------------------+
    |  BACKEND (Spring Boot)                Render / Railway          |
    |  Dockerfile -> sacejpa-0.0.1-SNAPSHOT.jar                      |
    |  https://api.aleleotours.com                                  |
    +------------------------------+--------------------------------+
                                   |  JDBC
                                   v
    +---------------------------------------------------------------+
    |  BASE DE DATOS PostgreSQL              Neon / Railway          |
    |  SACE_db                                                     |
    +---------------------------------------------------------------+

    Por que separarlos: el frontend es estatico y no necesita servidor
    (lo sirve un CDN gratis e ilimitado); el backend es una aplicacion
    Java que necesita proceso vivo. Mezclarlos obliga a pagar servidor
    por un sitio que no lo necesita.


# =====================================================================
# OPCION A - TODO GRATIS (Cloudflare Pages + Render + Neon)
# =====================================================================

## A.1 Base de datos en Neon (PostgreSQL gratis, sin expiracion)

1. Crear cuenta en https://neon.tech (o iniciar sesion con GitHub).
2. "Create a project" -> nombre `SACE`.
3. En "Connection details" copiar:
      - Host        (ej. ep-xxx-xxx.us-east-2.aws.neon.tech)
      - Port        5432
      - Database    neondb  (o el nombre que te de)
      - User        (el que te muestre)
      - Password    (la que te muestre)
4. La cadena JDBC completa es:
      jdbc:postgresql://HOST:5432/NOMBRE_BD?sslmode=require
5. Importante: en Neon la base se crea VACIA. SACE_db se arma sola:
   Hibernate crea las tablas al arrancar la API (ddl-auto=update) y el
   seed inserta el administrador, los 6 servicios turisticos y las
   preguntas frecuentes del chatbot.

## A.2 Backend en Render

1. Crear cuenta en https://render.com (GitHub permite entrar directo).
2. "New" -> "Blueprint" -> seleccionar el repositorio `Projecto_SACE`.
3. Render lee `render.yaml` automaticamente y propone crear el
   servicio `sace-api`.
4. Completar las variables que Render marca como secretas:

   | Variable                   | Valor                                        |
   |----------------------------|----------------------------------------------|
   | SPRING_DATASOURCE_URL     | jdbc:postgresql://HOST:5432/neondb?sslmode=require |
   | SPRING_DATASOURCE_USERNAME| usuario de Neon                              |
   | SPRING_DATASOURCE_PASSWORD| contrasena de Neon                            |
   | SACE_ADMIN_PASSWORD        | una contrasena que tu recuerdes              |
   | SACE_CORS_ORIGINS          | https://www.aleleotours.com,https://aleleotours.com,https://TU-PROYECTO.pages.dev |
   | SPRING_MAIL_HOST          | (opcional)                                   |
   | SPRING_MAIL_PORT          | (opcional)                                   |
   | SPRING_MAIL_USERNAME      | (opcional)                                   |
   | SPRING_MAIL_PASSWORD      | (opcional)                                   |

5. "Apply" -> "Create resources". El primer build tarda ~5 minutos.
6. Cuando termine, Render da una URL tipo `https://sace-api.onrender.com`.
   Verifica: `https://sace-api.onrender.com/api/salud` debe devolver JSON
   con `"estado":"OK"`.

   OJO con el plan gratis: el servicio se apaga a los 15 minutos sin
   trafico y la primera respuesta despues tarda ~50 segundos. Para la
   sustentacion,Deja una pestaña abierta o haz una peticion cada poco
   mientras preparas la demo. (La opcion B no tiene este problema.)

## A.3 Frontend en Cloudflare Pages

1. Crear cuenta en https://dash.cloudflare.com (o iniciar sesion con
   GitHub).
2. "Workers & Pages" -> "Create" -> "Pages" -> "Connect to Git".
3. Seleccionar el repositorio `Projecto_SACE`.
4. Configuracion del build:
      - Framework preset : None
      - Build command    : (dejar vacio; el sitio no tiene build)
      - Build output     : frontend-aleleo-tours
      - Root directory   : (dejar vacio)
5. "Save and Deploy". En 30 segundos estara vivo en
   `https://TU-PROYECTO.pages.dev`.
6. Conectar el dominio: "Custom domains" -> "Add a domain" ->
   `www.aleleotours.com`. Cloudflare crea el registro DNS solo.
   Repite con `aleleotours.com` si quieres el dominio sin www.
7. Ajustar el backend: en Render, "Settings" -> "Environment" ->
   "SACE_CORS_ORIGINS" -> agregar `https://www.aleleotours.com`.
   Guardar. Render redespliega solo.

## A.4 Apuntar el frontend al backend

El frontend resuelve solo la URL de la API (ver
`frontend-aleleo-tours/js/core/config.js`), pero solo si el sitio esta
en un dominio propio: detecta `https://api.<tu-dominio>/api`.

Hay dos formas de resolverlo:

**Opcion 1 (recomendada): dominio propio para la API.**
En Render -> "Settings" -> "Custom Domains" -> `api.aleleotours.com`.
Cloudflare crea el registro CNAME. Con esto el frontend detecta
`https://api.aleleotours.com/api` automaticamente y no hay que tocar
codigo.

**Opcion 2: subdominio de la plataforma.**
Edita `frontend-aleleo-tours/js/core/config.js` y pon la URL fija:

    const API_FIJA = 'https://sace-api.onrender.com/api';

Despues sube el cambio:

    git add -A && git commit -m "chore(deploy): apuntar la API al backend desplegado" && git push

Cloudflare Pages redespliega solo en cuanto llega el commit.

## A.5 Lista de verificacion

- [ ] `https://TU-BACKEND/api/salud` -> `"estado":"OK"`, `"baseDatos":"conectada"`
- [ ] El sitio carga y muestra los 6 destinos (vienen de la API, no estan
      en el HTML: si aparecen, la API responde y el CORS esta bien)
- [ ] Registro e inicio de sesion de un cliente
- [ ] Inicio de sesion del administrador con SACE_ADMIN_PASSWORD
- [ ] Crear solicitud -> pagar (pasarela mock) -> descargar comprobante PDF
- [ ] Panel de reportes con graficas
- [ ] El comprobante PDF sigue disponible tras un redespliegue
      (esto NO pasa en el plan gratis de Render: el disco es efimero.
      Si importa, usa la opcion B con volumen, o la opcion C.)


# =====================================================================
# OPCION B - PAGAR LO JUSTO (Cloudflare Pages + Railway)
# =====================================================================

Railway te da la API, PostgreSQL y un volumen persistente todo junto,
sin cold start y sin caducidad. Cuesta ~5 USD/mes con el credito
inicial.

## B.1 Crear el proyecto

1. Cuenta en https://railway.app (iniciar sesion con GitHub).
2. "New Project" -> "Deploy from GitHub repo" -> `Projecto_SACE`.
3. Railway detecta `railway.json` y construye la imagen con el
   Dockerfile de la raiz. El servicio queda accesible en un dominio
   `*.up.railway.app`.

## B.2 Anadir PostgreSQL

1. "+ New" -> "Database" -> "PostgreSQL".
2. Railway genera las variables `DATABASE_URL`, `PGHOST`, etc.
   Anade a mano en el servicio web:
      SPRING_DATASOURCE_URL     = ${{PostgreSQL.DATABASE_URL}}
      SPRING_DATASOURCE_USERNAME= ${{PostgreSQL.PGUSER}}
      SPRING_DATASOURCE_PASSWORD= ${{PostgreSQL.PGPASSWORD}}

## B.3 Volumen persistente para los comprobantes

1. "Volumes" -> "New Volume" -> nombre `comprobantes`, mount path `/data`.
2. En el servicio web: SACE_UPLOAD_DIR = `/data/comprobantes`.
3. La API debe poder escribir ahi: el usuario del contenedor (`sace`)
   no tiene permisos sobre el volumen si el volumen se crea como root.
   Solucion: en el servicio web, "Deploy" -> "Custom Start Command":

       sh -c "mkdir -p /data/comprobantes && chown -R sace:sace /data && exec java $JAVA_OPTS -jar /app/app.jar"

   (Ojo: si Railway no inyecta JAVA_OPTS en el arranque, deja el comando
   como `java -jar /app/app.jar` y cambia el dueo del volumen con el
   panel.)

## B.4 Variables restantes

   | Variable            | Valor                                              |
   |---------------------|----------------------------------------------------|
   | SACE_TOKEN_SECRET   | openssl rand -base64 48                            |
   | SACE_ADMIN_PASSWORD | tu contrasena                                       |
   | SACE_CORS_ORIGINS   | https://www.aleleotours.com,https://TU.pages.dev    |
   | SACE_SHOW_SQL       | false                                              |
   | SACE_DDL_AUTO       | update                                             |
   | PAGOS_PASARELA      | mock                                               |

## B.5 Dominio

En Railway: "Settings" -> "Networking" -> "Generate Domain" o
"Custom Domain" para `api.aleleotours.com`. Luego, en el frontend,
apunta `API_FIJA` o rely en la deteccion automatica.


# =====================================================================
# OPCION C - SERVIDOR PROPIO (Hetzner, DigitalOcean, OVH)
# =====================================================================

Un solo servidor con los tres componentes. Nada mas que pagar.

1. Contratar un VPS (Hetzner CX22 ~3,79 EUR/mes es de lo mas barato y
   esta en Europa; DigitalOcean 24 USD/mes el mas facil).
2. Instalar Docker:
       curl -fsSL https://get.docker.com | sh
       sudo apt install -y docker-compose-plugin
3. Subir el repositorio:
       git clone https://github.com/Siks2301/Projecto_SACE.git
       cd Projecto_SACE
4. Preparar los secretos:
       cp deploy/.env.example deploy/.env
       nano deploy/.env      # cambiar contrasenas y secretos
5. Levantar:
       cd deploy && docker compose up -d --build
6. Comprobar:
       docker compose ps
       docker compose logs -f api
       curl http://localhost/api/salud
7. Certificados HTTPS (Let's Encrypt):
       cd deploy
       docker compose run --rm certbot certonly --webroot -w /var/www/certbot \
         -d aleleotours.com -d www.aleleotours.com -d api.aleleotours.com \
         --agree-tos -m CORREO@ejemplo.com
       # luego descomentar el bloque 'ssl' de deploy/nginx.conf
       docker compose exec web nginx -s reload
8. Backup de la base de datos (programado con cron, 3:00 AM):
       0 3 * * * cd /home/usuario/Projecto_SACE/deploy && \
         docker compose exec -T db pg_dump -U sac_user SACE_db > \
         /home/usuario/backups/sace_$(date +\%Y\%m\%d).sql
9. En Cloudflare: apuntar el DNS del dominio a la IP del servidor.


# =====================================================================
# VARIABLES QUE EL SISTEMA ENTENDE
# =====================================================================

Referencia completa en `deploy/.env.example`. Las que importan:

| Variable                   | Para que sirve                                              |
|----------------------------|-------------------------------------------------------------|
| PORT                       | Puerto de la API. La plataforma lo inyecta.                 |
| SPRING_DATASOURCE_URL     | Cadena JDBC de PostgreSQL.                                   |
| SPRING_DATASOURCE_USERNAME| Usuario de la base.                                          |
| SPRING_DATASOURCE_PASSWORD| Contrasena de la base.                                       |
| SACE_TOKEN_SECRET          | Firma de los tokens de sesion.                               |
| SACE_ADMIN_PASSWORD        | Contrasena del administrador semilla (se rota al reiniciar). |
| SACE_CORS_ORIGINS          | Dominios del frontend, separados por comas.                  |
| SACE_UPLOAD_DIR            | Ruta ABSOLUTA de los comprobantes PDF.                       |
| SACE_SHOW_SQL              | `false` en produccion.                                       |
| SACE_DDL_AUTO              | `update` en el primer despliegue; `none` despues.            |
| SACE_DB_POOL_SIZE          | Conexiones maximas al pool.                                  |
| PAGOS_PASARELA             | `mock` o `wompi`.                                            |
| WOMPI_*                    | Llaves y ambiente de la pasarela de pagos.                   |
| SPRING_MAIL_*              | Servidor SMTP de las notificaciones.                         |


# =====================================================================
# TRAMPAS CONOCIDAS
# =====================================================================

1. **El disco de un PaaS es efimero.** Sin volumen persistente, los
   comprobantes PDF desaparecen en cada redespliegue. Se arregla con un
   volumen (Railway) o un disco (Render de pago) o una VPS (opcion C).

2. **La base free de Render expira a los 30 dias.** Por eso la opcion A
   usa Neon: no caduca.

3. **CORS mal configurado = "Failed to fetch" en el navegador.** Si las
   peticiones fallan en la consola del navegador pero la API responde en
   Postman, el problema SIEMPRE es `SACE_CORS_ORIGINS`: debe incluir el
   dominio exacto del frontend, con https y sin barra final.

4. **Vercel/Netlify NO sirven para el backend.** Son plataformas de
   frontend. El backend Spring Boot necesita un contenedor o una VM.

5. **El frontend no tiene build.** No hay `npm install` ni `npm run
   build`: es HTML, CSS y JS servidos tal cual. Por eso en Cloudflare
   Pages el "build command" va vacio y el "output directory" es
   `frontend-aleleo-tours`.

6. **El dominio `aleleotours.com` parece no estar registrado.** La
   consulta RDAP no lo encontro. Registralo antes de seguir: sin dominio
   propio no hay HTTPS para el webhook de Wompi ni deteccion automatica
   del backend.

7. **La primera respuesta del plan gratis de Render tarda ~50 s.** Con
   las variables del pool y el arranque de Hibernate, el health check
   tiene margen (`start-period=90s` en el Dockerfile), pero la demo en
   vivo necesita el servicio despierto.
