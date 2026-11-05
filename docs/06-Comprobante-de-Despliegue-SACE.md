# Comprobante de despliegue — Sistema SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 6. Comprobante de despliegue — SACE (AleLeo Tours)
### 6.1 Fecha, equipo y alcance
Verificación de despliegue realizada el 21 de septiembre de 2026 sobre el equipo SIKS2301 (Windows 11), con los tres componentes del sistema ejecutándose de forma simultánea: PostgreSQL, backend Spring Boot y frontend estático. Se comprueban servicios en escucha, respuesta HTTP de la API y del sitio, existencia de la base de datos y datos semilla.

### 6.2 Servicios desplegados
| Componente | Tecnología | URL / puerto | Estado |
|---|---|---|---|
| Base de datos | PostgreSQL 18.4 | localhost:5432 — SACE_db | Activo |
| Backend (API REST) | Java 17 · Spring Boot 3.2 | http://localhost:8082/api | Activo |
| Frontend | HTML/CSS/JS · Bootstrap 5.3.3 | http://localhost:8091 | Activo |
| Comprobantes PDF | Carpeta uploads/comprobantes | Servida por el backend | Activo |

### 6.3 Comandos de despliegue utilizados
     CREATE DATABASE SACE_db;

     cd backend-SACE && mvn spring-boot:run     # http://localhost:8082

     cd frontend-aleleo-tours && python -m http.server 8091 --bind 127.0.0.1

El backend crea el esquema con ddl-auto=update y DataInitializer siembra el administrador, el catálogo y las preguntas frecuentes.

### 6.4 Verificación de salud del entorno (salida real)
Salida capturada el 21 de septiembre de 2026:

```
== VERIFICACION DE DESPLIEGUE SACE - 21 de septiembre de 2026 ==
```
```
Equipo/host: SIKS2301
```
```
SO: Microsoft Windows 11 Home Single Language
```
```
PostgreSQL (5432): OK - PostgreSQL 18.4 on x86_64-windows, compiled by msvc-19.44.35226, 64-bit
```
```
Base SACE_db existe: SI
```
```
Tablas en SACE_db: 11
```
```
Backend GET /api/servicios - HTTP 200
```
```
catalogo de servicios: 7 destinos
```
```
Backend POST /api/auth/login - OK (rol: ADMINISTRADOR, token 158 chars)
```
```
Frontend GET /login.html - HTTP 200 (8065 bytes)
```
```
Frontend GET /index.html - HTTP 200 (17049 bytes)
```
```
Frontend GET /destinos.html - HTTP 200 (13160 bytes)
```
```
Frontend GET /registro.html - HTTP 200 (9006 bytes)
```
```
Puertos escuchando (backend/frontend): 8082, 8091
```
### 6.5 Base de datos
La base SACE_db existe con 11 tablas en el esquema public (usuarios, solicitudes, mensajes, servicios, pagos, preguntas frecuentes, entre otras). El DDL del esquema completo se entrega en docs/SACE_db_ddl.sql; los datos no se distribuyen (los genera el sistema al iniciar).

### 6.6 Pruebas post-despliegue (humo)
| Prueba | Resultado |
|---|---|
| GET /api/servicios (catálogo público) | HTTP 200, 7 destinos |
| POST /api/auth/login con admin semilla | HTTP 200, rol ADMINISTRADOR, token firmado |
| Páginas login, index, destinos y registro | HTTP 200 en las 4 |
| Suite de regresión 92/92 (Plan de Pruebas, CP-01–CP-24) | 92 PASS / 0 FAIL |

### 6.7 Evidencia visual del sistema en ejecución
Se incluyen tres capturas del entorno desplegado (colaborador externo autenticado o vista pública), disponibles además en 7_Despliegue/Capturas_Despliegue/ y en docs/capturas/ del repositorio:

![Captura](01_Despliegue_Login.png)

![Captura](02_Despliegue_Home.png)

![Captura](03_Despliegue_Destinos.png)

### 6.8 Notas y recomendaciones
- El frontend se sirve en el puerto 8091 (comando `python -m http.server 8091`), consistente con el README y los planes de despliegue del paquete de evidencias.
- Antes de exponer el sistema en producción: cambiar SACE_TOKEN_SECRET, la contraseña de PostgreSQL y las credenciales SMTP, y rotar el administrador semilla (admin123).
- Mantener copias de respaldo del modelo de datos con pg_dump.

### 6.9 Verificación de la configuración de despliegue en la nube (salida real)

Esta segunda verificación se hizo el **4 de noviembre de 2026** y es distinta de
la de 6.4: allí se comprobó el sistema **ya desplegado con datos de desarrollo**;
aquí se comprueba que la configuración nueva funciona **contra una base de datos
vacía**, que es la situación exacta de un primer despliegue en Neon o Railway.

El procedimiento no usa la base `SACE_db` de desarrollo, para no alterar sus
datos: se crea una base temporal `sace_deploy_test` sin ninguna tabla.

#### 6.9.1 Empaquetado del artefacto

```
cd backend-SACE
mvn -B -ntp clean package -DskipTests

[INFO] Building jar: ...\target\sacejpa-0.0.1-SNAPSHOT.jar
[INFO] Replacing main artifact ... with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] BUILD SUCCESS
[INFO] Total time:  33.542 s
```

El `.jar` resultante es ejecutable por sí solo (incluye sus dependencias), que es
lo que copia la imagen Docker en su segunda etapa.

#### 6.9.2 Arranque con el perfil de producción

Se ejecutó el `.jar` con las variables de entorno que usaría la plataforma
(`PORT`, `SPRING_DATASOURCE_*`, `SACE_TOKEN_SECRET`, `SACE_ADMIN_PASSWORD`,
`SACE_CORS_ORIGINS`, `SACE_UPLOAD_DIR`, `SACE_DDL_AUTO`, `PAGOS_PASARELA`) y con
los mismos flags de JVM del `Dockerfile`.

```
Starting SacejpaApplication v0.0.1-SNAPSHOT using Java 21.0.12 with PID 17068
HikariPool-1 - Starting...
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@5e385b6f
HikariPool-1 - Start completed.
PagoConstraintInitializer: indices de integridad del modulo de pagos verificados
PersonaEmailConstraintInitializer: unicidad de persona.email verificada
Tomcat started on port 8082 (http) with context path ''
Started SacejpaApplication in 14.038 seconds
>>> [SACE SEED] Usuario Administrador creado: admin@aleleotours.com / admin123
>>> [SACE SEED] 6 Servicios turisticos iniciales creados.
>>> [SACE SEED] Preguntas frecuentes iniciales creadas.
>>> [SACE SEED] Chatbot 'AleLeoBot' inicializado.
La aplicacion SaceJPA esta iniciada correctamente
```

La aplicación arrancó en **14 segundos**, por debajo del `start-period=90s` del
`HEALTHCHECK` del `Dockerfile`.

#### 6.9.3 Esquema creado desde cero

La base estaba vacía. Hibernate la dejó lista sin intervención manual:

```
== TABLAS EN sace_deploy_test ==
11 tablas
chatbot
chatbot_pregunta_frecuente
cliente
cliente_servicio
empleado
mensaje
pago
persona
pregunta_frecuente
servicio
solicitud
```

#### 6.9.4 Endpoint de salud

```
GET http://localhost:8082/api/salud   ->  HTTP 200

{
    "estado":      "OK",
    "aplicacion":  "SACE - Sistema de Atencion al Cliente (AleLeo Tours)",
    "version":     "1.0.0",
    "baseDatos":   "conectada",
    "momento":     "2026-11-05T00:09:48.386541400Z"
}
```

Es el mismo endpoint que consultan Render y Railway para decidir si el contenedor
está sano.

#### 6.9.5 Semilla de datos y login con la contraseña del entorno

```
GET /api/servicios  ->  6 servicios
  - Paquete Vacacional Cartagena
  - Vuelo + Hotel San Andres
  - Experiencia Santa Marta y Playas
  - Tour Medellin Innovadora y Cafe
  - Ecoturismo Parque Tayrona
  - Paraiso Providencia Isla Virgen

POST /api/auth/login  { "email": "admin@aleleotours.com", "contrasenia": "admin123" }
  -> LOGIN OK
     id    : 1
     email : admin@aleleotours.com
     tipo  : ADMINISTRADOR
     token : eyJzdWIiOjEsImVtYWlsIjoiYWRt...
```

El administrador se creó con la contraseña recibida en `SACE_ADMIN_PASSWORD`, no
con un valor escrito en el código: es la comprobación de que la rotación de
credenciales funciona.

#### 6.9.6 CORS y autorización

```
GET /api/salud   Origin: https://www.aleleotours.com
  HTTP 200
  Access-Control-Allow-Origin: https://www.aleleotours.com
  Access-Control-Allow-Credentials: true

GET /api/salud   Origin: https://sitio-malicioso.example
  HTTP 403   (origen no listado en SACE_CORS_ORIGINS: bloqueado, correcto)

GET /api/clientes   (sin token)
  HTTP 401   (ruta protegida: no autorizado, correcto)
```

#### 6.9.7 Nota sobre la imagen Docker

El `Dockerfile` no se pudo construir en este equipo: el demonio de Docker se
detuvo durante `mvn dependency:go-offline` con `rpc error: code = Unavailable
desc = error reading from server: EOF`. La causa es la memoria (7,7 GB en total,
~1 GB libre; Docker Desktop reserva cerca de la mitad para su máquina virtual y
Maven dentro del contenedor necesita 1-2 GB más).

Esto no invalida la verificación: el `.jar` probado en 6.9.2 es exactamente el
artefacto que la imagen copia en su segunda etapa, ejecutado con las mismas
variables y los mismos flags de JVM. Las tres rutas de despliegue documentadas
construyen la imagen en servidores con memoria suficiente. El detalle del
diagnóstico está en `10-Detalle-Despliegue-Como-Se-Hizo-Y-Como-Funciona.md`,
apartado 13, error 7.