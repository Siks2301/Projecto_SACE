# Plan y guía de despliegue

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_Actualizado: 10 de noviembre de 2026_

> **Qué es este documento.** El **plan** (qué se despliega, en qué orden y con qué
> variables) y la **guía paso a paso** para las tres rutas posibles. La evidencia
> de lo que se verificó y el detalle técnico de por qué funciona así están en
> [`06-Despliegue-Comprobante-y-Detalle-SACE.md`](06-Despliegue-Comprobante-y-Detalle-SACE.md).
> La plantilla completa de variables está en [`deploy/.env.example`](../deploy/.env.example).

---

## 4.1 Arquitectura objetivo

Dos escenarios con los mismos artefactos:

- **Local:** backend Spring Boot y PostgreSQL en el mismo equipo; el frontend se
  sirve con un servidor web ligero (`python -m http.server` o nginx).
- **Nube (opción A, la ejecutada):** los tres componentes separados según su
  naturaleza.

```
FRONTEND estático     Cloudflare Workers   CDN + HTTPS, sin build, costo $0
        │  fetch() a /api/...
        ▼
BACKEND Spring Boot   Render / Railway     contenedor desde el Dockerfile
        │  JDBC
        ▼
BASE DE DATOS         Neon / Railway       PostgreSQL 18 (SACE_db)
```

Separarlos no es una preferencia estética: el frontend es HTML/CSS/JS puro y un
CDN lo sirve gratis; el backend Java necesita un proceso vivo y memoria.
Publicarlos juntos obligaría a pagar servidor por un sitio que no lo necesita.

| Opción | Componentes | Costo | Cuándo conviene |
|---|---|---|---|
| A | Cloudflare Workers + Render + Neon | $0 | Entrega académica y demostración |
| B | Cloudflare Workers + Railway | ~5 USD/mes | Sin cold start y con volumen persistente |
| C | VPS con Docker Compose + nginx | ~4 EUR/mes | Control total, dominio propio, respaldos |

---

## 4.2 Dependencias y versiones

| Componente | Versión / detalle |
|---|---|
| Java (JDK) | 17 (`JAVA_HOME` configurado) |
| Maven | 3.8+ (compilación y empaquetado del backend) |
| Spring Boot | 3.2 (artefacto `com.mycompany:sacejpa:0.0.1-SNAPSHOT`) |
| PostgreSQL | 18+ (base `SACE_db` en el puerto 5432) |
| Frontend | HTML/CSS/JS puro + Bootstrap 5.3.3; sin build |
| Servidor estático | `python -m http.server` o nginx |
| Puertos | Backend 8082 · Frontend 8091 · PostgreSQL 5432 |

---

## 4.3 Variables de entorno

El backend lee toda su configuración de variables con el formato
`${NOMBRE:valor_por_defecto}`. La tabla **propiedad → variable → valor por
defecto** está en el documento 06, apartado 6.6.1; la plantilla completa y
comentada, en [`deploy/.env.example`](../deploy/.env.example).

Lo mínimo que hay que definir en producción:

| Variable | Para qué |
|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Conexión a PostgreSQL (en Neon, con `?sslmode=require`) |
| `SACE_TOKEN_SECRET` | Firma HMAC de los tokens de sesión |
| `SACE_ADMIN_PASSWORD` | Contraseña del administrador semilla (se rota al reiniciar) |
| `SACE_CORS_ORIGINS` | Origen exacto del frontend, sin barra final |
| `SACE_UPLOAD_DIR` | Ruta de los comprobantes PDF (ver 4.9) |
| `SACE_DDL_AUTO` | `update` en el primer despliegue, `none` después |
| `SACE_SHOW_SQL` | `false` en producción |
| `PAGOS_PASARELA` | `mock` o `wompi` (ver documento 09) |

---

## 4.4 Despliegue local (paso a paso)

**Paso 1 — Crear la base de datos vacía** (solo la primera vez):

```
CREATE DATABASE SACE_db;
```

**Paso 2 — Levantar el backend** (las tablas y los datos semilla se crean solos):

```
cd backend-SACE
mvn spring-boot:run      # queda en http://localhost:8082
```

**Paso 3 — Servir el frontend estático:**

```
cd frontend-aleleo-tours
python -m http.server 8091 --bind 127.0.0.1   # http://localhost:8091
```

**Paso 4 — Verificar:** abrir `http://localhost:8091`, iniciar sesión con el
administrador semilla (`admin@aleleotours.com` / `admin123`, solo desarrollo) y
comprobar los módulos del documento 03.

---

## 4.5 Despliegue en un servidor con el `.jar` (procedimiento previsto)

1. Empaquetar: `cd backend-SACE && mvn clean package` → genera
   `backend-SACE/target/sacejpa-0.0.1-SNAPSHOT.jar`.
2. Definir las variables de producción (`SACE_TOKEN_SECRET` fuerte, credenciales
   de BD, SMTP real).
3. Iniciar: `java -jar sacejpa-0.0.1-SNAPSHOT.jar`.
4. Publicar el frontend: copiar `frontend-aleleo-tours/` a la carpeta web del
   servidor (nginx: `root` + `index.html`).
5. Apuntar el frontend al backend: basta con la constante `API_FIJA` de
   `frontend-aleleo-tours/js/core/config.js`, que es el único archivo que conoce
   la dirección del API.
6. (Opcional) Registrar el proceso con systemd/PM2 y habilitar HTTPS.

---

## 4.6 Opción A — todo gratis (Cloudflare Workers + Render + Neon)

Así se desplegó realmente el 9 de noviembre de 2026. Antes de empezar: tener el
repositorio actualizado (`git push origin main`).

### A.1 Base de datos en Neon (PostgreSQL gratis, sin expiración)

1. Crear cuenta en <https://neon.tech> (o entrar con GitHub).
2. **Create a project** → nombre `SACE`.
3. En **Connection details** copiar host, puerto `5432`, base (`neondb`), usuario
   y contraseña.
4. La cadena JDBC completa es:
   `jdbc:postgresql://HOST:5432/NOMBRE_BD?sslmode=require`
5. En Neon la base se crea **vacía**: Hibernate crea las tablas al arrancar la API
   (`ddl-auto=update`) y el seed inserta el administrador, los 6 servicios
   turísticos y las preguntas frecuentes del chatbot.

### A.2 Backend en Render

1. Crear cuenta en <https://render.com> (GitHub permite entrar directo).
2. **New → Blueprint** → seleccionar el repositorio `Projecto_SACE`.
3. Render lee `render.yaml` y propone crear el servicio `sace-api`.
4. Completar las variables secretas: las de la tabla 4.3 (`SPRING_DATASOURCE_*`,
   `SACE_ADMIN_PASSWORD`, `SACE_CORS_ORIGINS`, `SACE_UPLOAD_DIR`) y, si se usan,
   las de `SPRING_MAIL_*`.
5. **Apply → Create resources**. El primer build tarda ~5 minutos.
6. Cuando termine, Render da una URL tipo `https://sace-api.onrender.com`.
   Verificar: `https://sace-api.onrender.com/api/salud` debe devolver
   `"estado":"OK"`.

> **Plan gratuito:** el servicio se apaga a los 15 minutos sin tráfico y la
> primera respuesta después tarda ~50 s. Para la sustentación, dejar una pestaña
> abierta. La opción B no tiene este problema.

### A.3 Frontend en Cloudflare (Workers con assets estáticos)

El repositorio ya trae `wrangler.jsonc` en la raíz, que le dice a Cloudflare que
publique la carpeta `frontend-aleleo-tours`.

1. Crear cuenta en <https://dash.cloudflare.com>.
2. **Workers & Pages → Create → Workers → Connect to Git**.
3. Seleccionar el repositorio `Projecto_SACE`.
4. Configuración del build:
   - Build command: `npx wrangler deploy`
   - Root directory: vacío
   - El nombre del Worker y el directorio de assets los toma de `wrangler.jsonc`
     (`aleleotours` y `frontend-aleleo-tours`).
5. **Deploy**. La URL queda como
   `https://aleleotours.<subdominio-de-tu-cuenta>.workers.dev`.
6. Comprobar que `workers.dev` está habilitado: **Worker → Settings → Domains &
   Routes → fila `workers.dev`**. Si está deshabilitado, **todas** las rutas
   responden `error code: 1042` aunque el deploy diga "Success".
7. (Opcional) Conectar el dominio: **Settings → Domains & Routes → Add custom
   domain** → `www.aleleotours.com`.
8. Ajustar el CORS: en Render, **Settings → Environment → `SACE_CORS_ORIGINS`** →
   el origen **exacto** del sitio. Guardar; Render redespliega solo.

> Cloudflare Pages también sirve para este proyecto (mismo directorio, build
> vacío). La diferencia es solo la URL final: `*.pages.dev` en vez de
> `*.workers.dev`.

### A.4 Apuntar el frontend al backend

Hay dos formas:

**Opción 1 (recomendada): dominio propio para la API.** En Render →
**Settings → Custom Domains** → `api.aleleotours.com`. Cloudflare crea el CNAME.
Con esto `config.js` detecta `https://api.aleleotours.com/api` y no hay que tocar
código.

**Opción 2: subdominio de la plataforma.** Editar
`frontend-aleleo-tours/js/core/config.js`:

```js
const API_FIJA = 'https://sace-api.onrender.com/api';
```

y subir el cambio (`git add -A && git commit -m "chore(deploy): apuntar la API al backend desplegado" && git push`).
Cloudflare redespliega al recibir el commit.

### A.5 Lista de verificación

- [ ] `https://TU-BACKEND/api/salud` → `"estado":"OK"`, `"baseDatos":"conectada"`.
- [ ] El sitio carga y muestra los 6 destinos (vienen de la API: si aparecen, la
      API responde y el CORS está bien).
- [ ] Registro e inicio de sesión de un cliente.
- [ ] Inicio de sesión del administrador con `SACE_ADMIN_PASSWORD`.
- [ ] Crear solicitud → pagar (pasarela `mock`) → descargar el comprobante PDF.
- [ ] Panel de reportes con gráficas.
- [ ] El comprobante PDF sigue disponible tras un redespliegue.

---

## 4.7 Opción B — pagar lo justo (Cloudflare Workers + Railway)

Railway da API, PostgreSQL y volumen persistente juntos, sin cold start y sin
caducidad (~5 USD/mes con el crédito inicial).

1. Cuenta en <https://railway.app> → **New Project → Deploy from GitHub repo** →
   `Projecto_SACE`. Railway detecta `railway.json` y construye con el
   `Dockerfile` de la raíz.
2. **+ New → Database → PostgreSQL**. Añadir al servicio web:
   ```
   SPRING_DATASOURCE_URL      = ${{PostgreSQL.DATABASE_URL}}
   SPRING_DATASOURCE_USERNAME = ${{PostgreSQL.PGUSER}}
   SPRING_DATASOURCE_PASSWORD = ${{PostgreSQL.PGPASSWORD}}
   ```
3. **Volumes → New Volume** → nombre `comprobantes`, mount path `/data`. Definir
   `SACE_UPLOAD_DIR=/data/comprobantes`.
4. El usuario `sace` del contenedor necesita permiso de escritura sobre el
   volumen. Como *Custom Start Command*:
   ```
   sh -c "mkdir -p /data/comprobantes && chown -R sace:sace /data && exec java $JAVA_OPTS -jar /app/app.jar"
   ```
   Si Railway no inyecta `JAVA_OPTS`, usar `java -jar /app/app.jar` y cambiar el
   dueño del volumen desde el panel.
5. Resto de variables: `SACE_TOKEN_SECRET` (`openssl rand -base64 48`),
   `SACE_ADMIN_PASSWORD`, `SACE_CORS_ORIGINS`, `SACE_SHOW_SQL=false`,
   `SACE_DDL_AUTO=update`, `PAGOS_PASARELA=mock`.
6. **Settings → Networking → Generate Domain** (o *Custom Domain* para
   `api.aleleotours.com`) y apuntar el frontend como en A.4.

---

## 4.8 Opción C — servidor propio (Hetzner, DigitalOcean, OVH)

Un solo servidor con los tres componentes, orquestado por
`deploy/docker-compose.yml` y `deploy/nginx.conf`.

1. Contratar un VPS (Hetzner CX22 ~3,79 EUR/mes; DigitalOcean desde 24 USD/mes).
2. Instalar Docker:
   ```
   curl -fsSL https://get.docker.com | sh
   sudo apt install -y docker-compose-plugin
   ```
3. Subir el repositorio:
   ```
   git clone https://github.com/Siks2301/Projecto_SACE.git
   cd Projecto_SACE
   ```
4. Preparar los secretos:
   ```
   cp deploy/.env.example deploy/.env
   nano deploy/.env      # cambiar contraseñas y secretos
   ```
5. Levantar:
   ```
   cd deploy && docker compose up -d --build
   ```
6. Comprobar:
   ```
   docker compose ps
   docker compose logs -f api
   curl http://localhost/api/salud
   ```
7. Certificados HTTPS (Let's Encrypt):
   ```
   cd deploy
   docker compose run --rm certbot certonly --webroot -w /var/www/certbot \
     -d aleleotours.com -d www.aleleotours.com -d api.aleleotours.com \
     --agree-tos -m CORREO@ejemplo.com
   # luego descomentar el bloque 'ssl' de deploy/nginx.conf
   docker compose exec web nginx -s reload
   ```
8. Respaldo de la base (cron, 3:00 AM):
   ```
   0 3 * * * cd /home/usuario/Projecto_SACE/deploy && \
     docker compose exec -T db pg_dump -U sac_user SACE_db > \
     /home/usuario/backups/sace_$(date +\%Y\%m\%d).sql
   ```
9. En Cloudflare: apuntar el DNS del dominio a la IP del servidor.

---

## 4.9 Verificación post-despliegue

- `GET /api/salud` responde `{"estado":"OK","baseDatos":"conectada"}`.
- `GET /api/servicios` responde JSON (catálogo público).
- Login de administrador y de un cliente recién registrado.
- Flujo completo: crear solicitud → pagar → descargar comprobante PDF.
- Reportes accesibles solo para administradores (403 para otros roles).
- Suite de humo: CP-02, CP-04, CP-06, CP-08 y CP-11 del documento 03.

**Comprobantes PDF.** `SACE_UPLOAD_DIR` acepta una ruta absoluta. En un plan con
disco persistente apunta al montaje; en el plan gratuito de Render no hay disco,
así que se usa `/tmp/comprobantes` y el PDF se regenera desde la base si se
pierde (documento 06, apartado 6.10).

---

## 4.10 Recomendaciones de producción

- Cambiar `SACE_TOKEN_SECRET`, la contraseña de PostgreSQL y las credenciales
  SMTP antes de exponer el sistema.
- Rotar la contraseña del administrador semilla tras el primer ingreso
  (`SACE_ADMIN_PASSWORD`).
- Habilitar HTTPS y no publicar los comprobantes PDF fuera de su ruta protegida.
- Pasar `SACE_DDL_AUTO` a `none` cuando el esquema esté estable.
- Mantener respaldos regulares con `pg_dump`.

---

## 4.11 Riesgos del despliegue en la nube y dónde se tratan

| Riesgo | Consecuencia | Dónde se resuelve |
|---|---|---|
| Disco efímero de la PaaS | Se pierden los comprobantes PDF en cada redespliegue | 4.9 y documento 06, apartado 6.10 |
| La base gratuita de Render caduca a los 30 días | Pérdida de datos | Se usa Neon, que no caduca |
| Cold start del plan gratuito de Render (~50 s) | La primera visita tarda | Documento 06, apartado 6.14 |
| CORS desalineado | `Failed to fetch` en el navegador | Documento 06, apartado 6.14 |
| `SACE_DDL_AUTO=update` permanente | Hibernate modifica el esquema en producción | 4.10 y documento 06, apartado 6.13 |
| Wompi exige HTTPS y URL pública estable | El webhook firmado no se puede verificar | Documento 09 |
| Ausencia de respaldos | Pérdida total ante un fallo | 4.8, paso 8 |

---

## 4.12 Qué cambió en el proyecto para poder desplegarlo

| Área | Archivos | Efecto |
|---|---|---|
| Configuración del backend | `application.properties`, `WebConfig.java`, `DataInitializer.java` | El mismo `.jar` sirve en local y en producción |
| Salud del servicio | `SaludController.java`, `AuthInterceptor.java` | `GET /api/salud` público, para health checks |
| Configuración del frontend | `js/core/config.js`, `js/core/app.js`, 11 `.html` | Cambiar de backend sin recompilar |
| Imagen de contenedor | `Dockerfile`, `.dockerignore` | Build reproducible en cualquier plataforma |
| Plataformas PaaS | `render.yaml`, `railway.json` | Servicio creado con un clic |
| Servidor propio | `deploy/docker-compose.yml`, `nginx.conf`, `.env.example` | Opción C, con HTTPS y volumen persistente |

El **por qué** de cada cambio y la evidencia de las verificaciones están en el
documento 06.
