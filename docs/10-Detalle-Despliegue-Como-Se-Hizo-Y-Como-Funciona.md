# Detalle del despliegue: cómo se hizo y cómo funciona

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_Actualizado: 4 de noviembre de 2026_

> **Para qué sirve este documento.** Explica el *por qué* y el *cómo funciona* de
> la configuración de despliegue. Cada documento tiene una función distinta y no
> se repiten entre sí:
>
> | Documento | Función |
> |---|---|
> | `04-Plan-de-Despliegue-SACE.md` | Qué se despliega y en qué orden (plan) |
> | `06-Comprobante-de-Despliegue-SACE.md` | Evidencia: salidas reales de la verificación |
> | **`10-...` (este)** | Detalle técnico: qué se cambió, por qué y cómo funciona |
> | `deploy/README.md` | Guía práctica: los comandos y pasos a ejecutar |
> | `deploy/.env.example` | Plantilla de todas las variables de entorno |

---

## 1. Objetivo del cambio

El proyecto funcionaba en un solo equipo: backend en `localhost:8082`, frontend
abierto como archivo o con `python -m http.server`, y PostgreSQL local. Para
publicarlo hacían falta cuatro cosas que **no existían**:

1. Que la configuración (base de datos, secretos, dominios, rutas) no estuviera
   escrita dentro del código, porque en la nube cambia y recompilar por cada
   cambio no es viable.
2. Que el frontend supiera a qué backend llamar sin editar la lógica de la
   aplicación.
3. Un mecanismo estándar de health check, para que las plataformas sepan si el
   servicio está vivo.
4. Una imagen de contenedor reproducible, que sirva igual en Render, Railway,
   Fly.io o un servidor propio.

El criterio que guió todas las decisiones fue: **el mismo artefacto debe servir
en desarrollo y en producción**; lo único que cambia son las variables de
entorno.

---

## 2. Estado anterior frente al estado actual

| Aspecto | Antes | Ahora |
|---|---|---|
| Puerto de la API | `8082` fijo en el código | `${PORT:8082}`: la plataforma lo inyecta |
| Credenciales de BD | `postgres/1234` en el archivo | `${SPRING_DATASOURCE_*}` |
| Orígenes CORS | `localhost:8088` y `localhost:8091` fijos | `${SACE_CORS_ORIGINS}`, lista por entorno |
| Contraseña del administrador | `admin123` fija en `DataInitializer` | `${SACE_ADMIN_PASSWORD}`, con rotación |
| URL del API en el frontend | `const API_BASE = 'http://localhost:8082/api'` | Resuelta por `js/core/config.js` en 4 niveles |
| Health check | No existía | `GET /api/salud` (público, sin token) |
| Empaquetado | Solo `.jar` local | `.jar` + imagen Docker multi-etapa |
| Despliegue | Manual, en un equipo | Tres rutas documentadas (A, B, C) |
| Secretos | En el repositorio | En variables de entorno; `deploy/.env` ignorado por Git |

---

## 3. Arquitectura y flujo de una petición

```
 Navegador
    │
    │  1. GET https://www.aleleotours.com/index.html
    ▼
┌──────────────────────────────────────────────────────────────┐
│ FRONTEND (estático)                    Cloudflare Pages      │
│   frontend-aleleo-tours/  →  CDN global, HTTPS, sin build    │
│   js/core/config.js resuelve la URL del API                  │
└───────────────────────┬──────────────────────────────────────┘
                        │  2. fetch(`${API_BASE}/servicios`)
                        │     con cabecera Origin
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ BACKEND (Spring Boot 3.2 / Java 17)    Render / Railway      │
│   AuthInterceptor → valida el token de sesión                │
│   WebConfig       → valida el Origin (CORS)                  │
│   Controlador     → lógica de negocio                        │
└───────────────────────┬──────────────────────────────────────┘
                        │  3. JDBC (pool Hikari)
                        ▼
┌──────────────────────────────────────────────────────────────┐
│ PostgreSQL 18                     Neon / Railway / volumen   │
│   SACE_db: 11 tablas                                         │
└──────────────────────────────────────────────────────────────┘
```

Decisiones de diseño relevantes:

- **Separar frontend y backend** no es una preferencia estética. El frontend es
  HTML/CSS/JS puro: un CDN lo sirve gratis, sin límite de visitas y con HTTPS
  automático. El backend es una aplicación Java que necesita un proceso vivo y
  memoria. Publicarlos juntos obligaría a pagar servidor por un sitio que no lo
  necesita.
- **Un único dominio para el API** (`api.aleleotours.com`) evita problemas de
  CORS y da una URL estable y HTTPS para el webhook de Wompi.
- **El schema lo crea Hibernate** en el primer arranque. Es lo que permite que
  una base vacía (Neon) quede lista sin scripts manuales.

---

## 4. Cambios en el backend

### 4.1 `application.properties`: de valores fijos a variables de entorno

**Antes** el archivo contenía el puerto, el usuario y la contraseña de la base,
la clave de firma de tokens y las llaves de Wompi escritas literalmente. Ese
archivo viaja en el repositorio, así que publicar el proyecto equivalía a
publicar los secretos.

**Ahora** cada valor sensible usa la sintaxis `${VARIABLE:valor_por_defecto}` de
Spring:

| Propiedad | Variable de entorno | Valor por defecto |
|---|---|---|
| `server.port` | `PORT` | `8082` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/SACE_db` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `1234` |
| `spring.jpa.hibernate.ddl-auto` | `SACE_DDL_AUTO` | `update` |
| `spring.jpa.show-sql` | `SACE_SHOW_SQL` | `true` |
| `app.security.token.secreto` | `SACE_TOKEN_SECRET` | Clave de desarrollo |
| `app.cors.origins` | `SACE_CORS_ORIGINS` | `http://localhost:*,http://127.0.0.1:*` |
| `app.upload.dir` | `SACE_UPLOAD_DIR` | `uploads/comprobantes` |
| `app.admin.email` | `SACE_ADMIN_EMAIL` | `admin@aleleotours.com` |
| `app.admin.password` | `SACE_ADMIN_PASSWORD` | vacío |
| `app.pagos.pasarela` | `PAGOS_PASARELA` | `mock` |
| `app.pagos.wompi.*` | `WOMPI_*` | vacío |

Además se añadieron dos ajustes propios de producción:

- **`server.forward-headers-strategy=framework`**: detrás de nginx o Cloudflare,
  la petición original llega por HTTPS pero al contenedor le llega por HTTP. Sin
  esto, Spring generaría URLs `http://` y las redirecciones fallarían.
- **Pool de conexiones acotado** (`SACE_DB_POOL_SIZE`, por defecto 10): en una
  PaaS hay un solo contenedor y una base compartida; un pool grande agota los
  límites de conexiones del proveedor.

**Por qué tiene valor por defecto cada variable:** el proyecto debe seguir
funcionando sin configurar nada (`mvn spring-boot:run` y listo). Si un
desarrollador clona el repositorio y todas las variables fueran obligatorias, la
aplicación no arrancaría. En producción se definen todas, así que los valores por
defecto nunca se usan.

### 4.2 `WebConfig.java`: CORS por entorno

**Antes** los orígenes estaban escritos en el código (`localhost:8088`,
`localhost:8091`). Al publicar el sitio en `www.aleleotours.com`, el navegador
enviaría `Origin: https://www.aleleotours.com`, el backend no lo reconocería y la
bloquearía: es el error `Failed to fetch` que aparece en la consola y que no
tiene nada que ver con que la API esté caída.

**Ahora** la clase lee `${app.cors.origins}` con `@Value` y lo convierte en
arreglo con el método `dividirOrigenes()`, que separa por comas, elimina espacios
y descarta entradas vacías. Se usa `allowedOriginPatterns` (no `allowedOrigins`)
porque admite comodines como `http://localhost:*`, necesario en desarrollo donde
el puerto del frontend cambia.

Se mantiene `allowCredentials(true)` porque la sesión viaja en una cabecera, y
por eso **nunca** se puede usar el comodín `*` en producción: hay que listar los
dominios exactos.

### 4.3 `DataInitializer.java`: administrador semilla configurable

**Antes** creaba el administrador con `admin@aleleotours.com` / `admin123`
únicamente si no existía. En producción eso significa una cuenta con contraseña
conocida por cualquiera que haya visto el repositorio, y cambiarla obligaba a
entrar a la base de datos a mano.

**Ahora**:

- El correo y la contraseña se leen de `app.admin.email` y `app.admin.password`.
- Si el administrador **no existe**, se crea con esos valores.
- Si **ya existe** y la variable `SACE_ADMIN_PASSWORD` está definida, la
  contraseña se **rota** al valor recibido (comparando antes con
  `passwordEncoder.matches` para no reescribir el hash en cada arranque).
- Si la variable está vacía, se mantiene la contraseña actual y solo se informa
  por consola.

Eso convierte la rotación de credenciales en una operación de plataforma: se
cambia la variable y se reinicia el servicio. No se toca SQL.

### 4.4 `SaludController.java`: `GET /api/salud`

Endpoint nuevo, público y de solo lectura:

```json
{
  "estado": "OK",
  "aplicacion": "SACE - Sistema de Atencion al Cliente (AleLeo Tours)",
  "version": "1.0.0",
  "baseDatos": "conectada",
  "momento": "2026-11-05T00:09:48Z"
}
```

Dos decisiones deliberadas:

- **Siempre devuelve HTTP 200**, aunque la base falle. El estado real viaja en el
  campo `baseDatos`. Si devolviera 503 por un fallo transitorio de la base, la
  plataforma podría reiniciar un contenedor que se habría recuperado solo.
- **Es público** (registrado en `AuthInterceptor` junto a `/api/servicios`,
  `/api/preguntas-frecuentes` y `/api/chatbots`) porque los health checks de las
  plataformas no tienen token, y porque permite diagnosticar abriendo una URL en
  el navegador. No expone datos del negocio: solo el estado del servicio.

---

## 5. Cambios en el frontend

**Antes** la dirección del backend estaba incrustada en `js/core/app.js`:

```js
const API_BASE = 'http://localhost:8082/api';
```

Publicar el sitio en un dominio real dejaba todas las llamadas apuntando a
`localhost`, es decir, al equipo de cada visitante. Cambiarla exigía editar el
archivo y volver a subir todo el sitio.

**Ahora** existe `js/core/config.js`, que se carga **antes** de `app.js` en los
11 archivos HTML, y resuelve la dirección en cuatro niveles:

| Orden | Fuente | Para qué |
|---|---|---|
| 1 | `window.SACE_API_BASE` | Override en caliente desde la consola del navegador |
| 2 | `API_FIJA` (constante del propio archivo) | Valor explícito por entorno |
| 3 | Detección por dominio | Si el sitio no es localhost, asume `https://api.<dominio>/api` |
| 4 | Respaldo | `http://localhost:8082/api` |

La detección del nivel 3 es la que permite que, **con el backend publicado en
`api.aleleotours.com` y el frontend en `www.aleleotours.com`, no haya que editar
ningún archivo**: la API se deduce del dominio del sitio, quitando el `www.`.

En `app.js` la constante conserva un respaldo a `localhost` para que, si el
script de configuración no llegara a cargarse (por ejemplo, una página nueva a la
que se le olvide incluirlo), el desarrollo local siga funcionando.

**Sobre la inyección en los 11 HTML:** se hizo con un script de PowerShell que
respeta la codificación (UTF-8 sin BOM) y los finales de línea (CRLF) del
proyecto, y se verificó con `git diff`: exactamente una línea añadida por archivo,
sin cambios colaterales.

---

## 6. La imagen de contenedor (`Dockerfile`)

El archivo está en la **raíz del repositorio** (`Projecto_SACE/Dockerfile`), no
dentro de `backend-SACE/`. Es intencional: con la raíz como contexto de
compilación, la misma imagen se construye igual en Render, Railway, Fly.io y
Docker Compose, sin rutas especiales por plataforma.

### 6.1 Etapa de compilación

```
FROM maven:3.9-eclipse-temurin-17 AS build
COPY backend-SACE/pom.xml .
RUN mvn dependency:go-offline
COPY backend-SACE/src ./src
RUN mvn clean package -DskipTests
```

El `pom.xml` se copia **solo** primero. Docker cachea esa capa: mientras las
dependencias no cambien, un redespliegue no las vuelve a descargar. Es la
diferencia entre un build de 3 minutos y uno de 20 segundos.

Se usa una **imagen JDK con Maven solo en esta etapa**. La etapa final no lleva
ni compilador ni Maven.

### 6.2 Etapa de ejecución

```
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache fontconfig ttf-dejavu && adduser -S sace
COPY --from=build /build/target/sacejpa-*.jar /app/app.jar
USER sace
```

Cada elemento responde a un problema concreto:

| Elemento | Motivo |
|---|---|
| Imagen **JRE** y no JDK | Menos superficie de ataque y ~200 MB menos |
| `fontconfig` + `ttf-dejavu` | Los comprobantes PDF usan `java.awt`; sin fuentes, la generación falla dentro del contenedor |
| `-Djava.awt.headless=true` | No hay pantalla en un contenedor; sin este flag AWT intenta abrir una |
| Usuario `sace` (no root) | Si el proceso se compromete, no tiene privilegios de root |
| `-XX:MaxRAMPercentage=75.0` | La JVM toma el 75 % del límite de memoria que asigne la plataforma, en lugar de una cifra fija que puede no existir |
| `HEALTHCHECK ... /api/salud` | Docker y las plataformas comprueban el servicio con el mismo endpoint |
| `--start-period=90s` | Hibernate puede tardar en crear el esquema; sin margen, el contenedor se marcaría como caído al arrancar |
| `ENTRYPOINT ["sh","-c","exec java ..."]` | El `exec` hace que la JVM sea el proceso 1 y reciba las señales de parada (SIGTERM), para un apagado limpio |

### 6.3 `.dockerignore`

Sin este archivo, el contexto de compilación incluiría `target/` (decenas de MB),
`docs/`, las evidencias documentales, los comprobantes PDF de clientes y `.git/`.
Además de ser lento, subiría datos personales al demonio de Docker.

---

## 7. Orquestación con Docker Compose y nginx (opción C)

`deploy/docker-compose.yml` levanta los tres componentes en un servidor propio:

| Servicio | Imagen | Publica puerto | Nota |
|---|---|---|---|
| `db` | `postgres:18-alpine` | **No** | Solo accesible por la red interna |
| `api` | Construida desde `../Dockerfile` | **No** | Se expone únicamente por nginx |
| `web` | `nginx:1.27-alpine` | `80` y `443` | Sirve el frontend y hace de proxy |
| `certbot` | `certbot/certbot` | — | Perfil opcional, para renovar certificados |

Dos decisiones de seguridad:

- **La base de datos no publica el puerto 5432.** Si lo publicara, quedaría
  expuesta a internet. Para entrar por consola se usa
  `docker compose exec db psql -U sac_user -d SACE_db`.
- **La API no publica el 8082.** Todo el tráfico entra por nginx, que aplica
  HTTPS, compresión y cabeceras de seguridad. Un solo punto de entrada es más
  fácil de auditar.

`deploy/nginx.conf` define dos dominios: el del sitio (`aleleotours.com`,
`www.aleleotours.com`) y el de la API (`api.aleleotours.com`). Incluye:

- Redirección 301 de HTTP a HTTPS, salvo `/.well-known/acme-challenge/`, que debe
  responder por HTTP para que Let's Encrypt valide el dominio.
- `proxy_set_header X-Forwarded-Proto $scheme` (y `Host`, `X-Real-IP`,
  `X-Forwarded-For`). Es la contraparte del
  `server.forward-headers-strategy=framework` del backend: sin ambos, Spring no
  sabe que la petición original era HTTPS.
- Compresión gzip de CSS, JS, JSON y SVG.
- Cabeceras `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` y
  `Strict-Transport-Security`.
- Caché de 7 días para recursos estáticos y `no-cache` para los `.html`, para que
  un redespliegue se vea de inmediato.

Los datos que deben sobrevivir a un `docker compose down` están en volúmenes
nombrados: `pgdata` (base de datos) y `comprobantes` (PDF).

---

## 8. Persistencia de los comprobantes PDF

Los comprobantes son la parte del sistema más fácil de romper al desplegar,
porque en una PaaS **el disco del contenedor es efímero**: lo que se escribe se
pierde en el siguiente redespliegue.

Cómo quedó resuelto:

1. `app.upload.dir` acepta una ruta **absoluta** (`SACE_UPLOAD_DIR`), que apunta a
   un volumen montado: `/data/comprobantes` en Railway, `/var/data/comprobantes`
   en Render con disco, `/app/uploads/comprobantes` en Docker Compose.
2. En la base de datos se guarda solo la ruta **relativa** del archivo.
3. `PdfComprobanteServicio.resolverRutaDeComprobante()` distingue ambos casos:
   si la ruta configurada es absoluta, la usa tal cual; si es relativa, la
   resuelve contra el directorio de trabajo.
4. El servicio crea el directorio si no existe (`mkdirs()`), así que un volumen
   recién montado y vacío no provoca un error al generar el primer PDF.
5. Si un PDF falta en el disco, se **regenera** desde los datos del pago: la base
   es la fuente de verdad y el archivo es un derivado.

---

## 9. Seguridad aplicada al despliegue

| Medida | Implementación |
|---|---|
| Secretos fuera del repositorio | Variables de entorno; `deploy/.env`, `*.pem` y `*.key` en `.gitignore` |
| Contenedor sin privilegios | Usuario `sace` en el `Dockerfile` |
| Superficie de rutas públicas mínima | Solo `/api/salud`, `/api/servicios`, `/api/preguntas-frecuentes`, `/api/chatbots` (GET) y `/api/auth/*` y webhook |
| Autorización por rol | `AuthInterceptor` valida el token y el rol; el resto devuelve 401/403 |
| CORS restringido | Lista explícita de dominios por entorno, con credenciales |
| HTTPS obligatorio | Redirección 301 y HSTS en nginx; TLS automático en Cloudflare |
| Webhook de pagos | Firma SHA-256 verificada con `WOMPI_EVENTS_SECRET` (ver documento 09) |
| Rotación de credenciales | `SACE_ADMIN_PASSWORD` rota la clave del administrador al reiniciar |
| Caché y cabeceras del sitio | `frontend-aleleo-tours/_headers` (Cloudflare Pages) |

---

## 10. Cómo funciona el despliegue, de principio a fin

Secuencia real de un primer despliegue (opción A):

1. **Se publica el código.** `git push` a `main`.
2. **Render detecta el cambio** y construye la imagen con el `Dockerfile`: Maven
   compila, se genera el `.jar` y se copia a una imagen JRE Alpine.
3. **Arranca el contenedor.** La plataforma inyecta `PORT` y las variables de
   conexión.
4. **Spring lee la configuración** de las variables de entorno, no del archivo.
5. **Hikari abre el pool** contra PostgreSQL (Neon). En Neon la cadena lleva
   `?sslmode=require` porque el proveedor exige TLS.
6. **Hibernate crea el esquema** (11 tablas) porque `SACE_DDL_AUTO=update` y la
   base está vacía.
7. **El seed inserta los datos base**: el administrador (con la contraseña de
   `SACE_ADMIN_PASSWORD`), los 6 paquetes turísticos, las preguntas frecuentes y
   el chatbot *AleLeoBot*.
8. **`/api/salud` responde 200**, la plataforma marca el servicio como sano y le
   empieza a enviar tráfico.
9. **Cloudflare Pages sirve el sitio.** `config.js` detecta
   `www.aleleotours.com` y resuelve `https://api.aleleotours.com/api`.
10. **El navegador llama al API.** El `Origin` está en `SACE_CORS_ORIGINS`, el
    backend responde con `Access-Control-Allow-Origin` y la aplicación funciona.

Comprobado en la verificación del apartado 14: si el `Origin` no está en la
lista, el backend responde **403** y el navegador bloquea la petición.

---

## 11. Operación y mantenimiento

**Creación del esquema.** `SACE_DDL_AUTO=update` es necesario en el primer
despliegue, porque es Hibernate quien crea las tablas en la base vacía. Una vez
estable el esquema conviene pasar a `none`: con `update`, un cambio accidental en
una entidad puede alterar la estructura de la base de producción sin revisión.

**Rotación de secretos.**

| Secreto | Cómo se rota |
|---|---|
| `SACE_TOKEN_SECRET` | Se cambia y se reinicia: invalida todas las sesiones abiertas |
| `SACE_ADMIN_PASSWORD` | Se cambia y se reinicia: el seed rota el hash |
| Credenciales de BD | Se rotan en el proveedor y se actualizan las variables |
| Llaves de Wompi | Se cambian en el panel y se actualiza la variable; el modo (`mock`/`wompi`) se controla con `PAGOS_PASARELA` |

**Respaldos.** Con independencia de dónde corra el sistema, `pg_dump` programado
(por ejemplo a las 3:00 AM con cron). En Docker Compose está documentado en
`deploy/README.md`.

**Registros.** En producción `SACE_SHOW_SQL=false`: volcar cada sentencia SQL
llena el log y puede exponer datos personales.

**Correo.** El servicio de notificaciones tiene *fail-safe*: si no hay SMTP
configurado, la operación de negocio continúa y solo se registra el aviso. Por
eso `SPRING_MAIL_*` es opcional para la entrega académica.

---

## 12. Referencia de variables de entorno

La plantilla completa y comentada está en `deploy/.env.example`. Resumen:

| Variable | Obligatoria en producción | Uso |
|---|---|---|
| `PORT` | No (la inyecta la plataforma) | Puerto de la API |
| `SPRING_DATASOURCE_URL` | Sí | Cadena JDBC completa |
| `SPRING_DATASOURCE_USERNAME` | Sí | Usuario de la base |
| `SPRING_DATASOURCE_PASSWORD` | Sí | Contraseña de la base |
| `SACE_TOKEN_SECRET` | Sí | Firma HMAC de los tokens de sesión |
| `SACE_ADMIN_PASSWORD` | Sí | Contraseña del administrador semilla |
| `SACE_ADMIN_EMAIL` | No | Correo del administrador semilla |
| `SACE_CORS_ORIGINS` | Sí | Dominios del frontend, separados por comas |
| `SACE_UPLOAD_DIR` | Sí | Ruta absoluta de los comprobantes PDF |
| `SACE_DDL_AUTO` | Sí | `update` en el primer despliegue, `none` después |
| `SACE_SHOW_SQL` | Sí | `false` en producción |
| `SACE_DB_POOL_SIZE` / `SACE_DB_POOL_MIN` | No | Tamaño del pool de conexiones |
| `SACE_TOKEN_HORAS` | No | Vigencia del token |
| `PAGOS_PASARELA` | Sí | `mock` o `wompi` |
| `PAGOS_METODOS` | No | Métodos habilitados (por defecto `PSE,TARJETA`) |
| `WOMPI_BASE_URL`, `WOMPI_PUB_KEY`, `WOMPI_PRV_KEY`, `WOMPI_INTEGRITY_KEY`, `WOMPI_EVENTS_SECRET` | Solo con `wompi` | Llaves y ambiente de la pasarela |
| `SPRING_MAIL_*`, `PROPIETARIO_EMAIL` | No | Notificaciones por correo |

---

## 13. Errores comunes y cómo se diagnostican

**1. `Failed to fetch` en el navegador, pero la API responde en Postman.**
Es CORS. El `Origin` del sitio no está en `SACE_CORS_ORIGINS`. Solución: agregar
el dominio exacto (con `https://`, sin barra final), recordando que
`www.aleleotours.com` y `aleleotours.com` son orígenes distintos. Verificado:
un origen no listado devuelve **403**.

**2. `Connection refused` o `net::ERR_CONNECTION_REFUSED`.**
La `API_BASE` apunta a un host o puerto equivocado. Se comprueba abriendo
`https://<backend>/api/salud` directamente en el navegador.

**3. La primera visita tarda ~50 segundos.**
Plan gratuito de Render: el servicio se apaga a los 15 minutos sin tráfico. No es
un error. Para la sustentación, mantener una pestaña abierta o usar la opción B/C.

**4. Los comprobantes PDF desaparecen tras un redespliegue.**
Disco efímero. Falta el volumen persistente o `SACE_UPLOAD_DIR` no apunta a él.

**5. `Connection to ... failed` al conectar a Neon.**
Falta `?sslmode=require` en la cadena JDBC. Neon rechaza conexiones sin TLS.

**6. El administrador no puede entrar con la contraseña esperada.**
`SACE_ADMIN_PASSWORD` no está definida (se usó `admin123`) o el servicio no se ha
reiniciado desde que se cambió la variable. El log de arranque lo dice
explícitamente: `[SACE SEED] Contrasena del administrador rotada...`.

**7. `docker build` se corta con `rpc error ... EOF`.**
Ocurrió en este equipo y vale documentarlo porque no es un error del proyecto:
el demonio de Docker se quedó sin memoria durante `mvn dependency:go-offline`. El
equipo tiene 7,7 GB de RAM y solo ~1 GB libre; Docker Desktop reserva cerca de la
mitad para su máquina virtual y Maven dentro del contenedor necesita 1-2 GB más.
El *builder* muere y el cliente recibe `EOF`. No es un fallo del `Dockerfile`:
**en Render o Railway la imagen se construye en sus servidores** (8-16 GB), así
que el build local no es un requisito del despliegue. Alternativas si se insiste
en construir en local: liberar memoria, limitar la VM de Docker en `.wslconfig`,
o compilar el `.jar` fuera del contenedor y usar una imagen que solo lo copie.

---

## 14. Verificación realizada

La salida real, con los comandos y sus resultados, está en
`06-Comprobante-de-Despliegue-SACE.md` §6.9. Resumen de lo comprobado el 4 de
noviembre de 2026:

| Comprobación | Resultado |
|---|---|
| `mvn clean package -DskipTests` | `BUILD SUCCESS`, `.jar` ejecutable generado |
| Arranque con variables de entorno de producción | `Started SacejpaApplication in 14.0 s` |
| Esquema creado desde cero en base vacía | **11 tablas** |
| Semilla de datos | 6 paquetes turísticos, preguntas frecuentes y chatbot |
| Administrador creado con `SACE_ADMIN_PASSWORD` | Login correcto, token emitido |
| `GET /api/salud` | `"estado":"OK"`, `"baseDatos":"conectada"` |
| CORS con origen permitido | `Access-Control-Allow-Origin` devuelto |
| CORS con origen no permitido | **403** |
| Ruta protegida sin token | **401** |

La prueba se hizo sobre una base **temporal y vacía** (`sace_deploy_test`), no
sobre la base de desarrollo, para reproducir el escenario real de Neon y no
alterar los datos locales.

**Sobre la imagen Docker:** no se pudo construir en este equipo por el problema de
memoria descrito en el error 7 del apartado anterior. Esto no compromete el
despliegue: las tres rutas documentadas construyen la imagen en servidores con
memoria suficiente, y la verificación del apartado 13 valida el mismo `.jar` con
el mismo perfil de configuración que usará el contenedor.

---

## 15. Inventario de archivos

**Creados**

| Archivo | Función |
|---|---|
| `Dockerfile` | Imagen de producción multi-etapa (raíz del repositorio) |
| `.dockerignore` | Reduce y depura el contexto de compilación |
| `render.yaml` | Blueprint de Render |
| `railway.json` | Configuración de build y health check en Railway |
| `deploy/docker-compose.yml` | Orquestación para servidor propio |
| `deploy/nginx.conf` | Proxy inverso, HTTPS, compresión, cabeceras |
| `deploy/.env.example` | Plantilla de variables de entorno |
| `deploy/README.md` | Guía práctica de despliegue (opciones A, B y C) |
| `frontend-aleleo-tours/js/core/config.js` | Resolución de la URL del API |
| `frontend-aleleo-tours/_headers` | Cabeceras y caché para Cloudflare Pages |
| `backend-SACE/.../Controladores/SaludController.java` | Endpoint `/api/salud` |
| `docs/10-Detalle-Despliegue-Como-Se-Hizo-Y-Como-Funciona.md` | Este documento |

**Modificados**

| Archivo | Cambio |
|---|---|
| `backend-SACE/src/main/resources/application.properties` | Placeholders `${VAR:default}` en todos los valores de entorno |
| `backend-SACE/.../Config/WebConfig.java` | CORS leído de `app.cors.origins` |
| `backend-SACE/.../Config/DataInitializer.java` | Administrador configurable, con rotación de contraseña |
| `backend-SACE/.../Seguridad/AuthInterceptor.java` | `/api/salud` como ruta pública |
| `frontend-aleleo-tours/js/core/app.js` | `API_BASE` deja de estar incrustada |
| `frontend-aleleo-tours/*.html` (11 archivos) | Carga de `js/core/config.js` antes de `app.js` |
| `docs/04-Plan-de-Despliegue-SACE.md` | Resumen del despliegue en la nube y riesgos (4.8 y 4.9) |
| `docs/06-Comprobante-de-Despliegue-SACE.md` | Evidencia de la verificación (6.9) |
| `docs/INDICE_EVIDENCIAS.md` | Referencias a este documento y a `deploy/README.md` |
| `.gitignore` | Excluye `deploy/.env`, `deploy/certbot/`, `*.pem`, `*.key` |

---

## 16. Conclusión

El proyecto pasó de "funciona en el equipo de quien lo programó" a "se puede
publicar en cualquier plataforma": la configuración sale del código, el frontend
se adapta al dominio, el estado del servicio es observable y el empaquetado es
reproducible. Ninguno de esos cambios altera el funcionamiento local: un
desarrollador sigue clonando el repositorio, creando la base y ejecutando
`mvn spring-boot:run` sin configurar nada, porque todos los valores nuevos tienen
un valor por defecto pensado para desarrollo.
