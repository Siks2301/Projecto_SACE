# Plan de despliegue

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 4. Plan de despliegue
### 4.1 Arquitectura objetivo
Dos componentes desplegados en el mismo equipo (local) o servidor académico: el backend Spring Boot (API + PostgreSQL en la misma máquina) y el frontend estático servido por un servidor web ligero (python o nginx). El apartado 4.8 describe la variante en la nube, con los tres componentes separados.

### 4.2 Dependencias y versiones
| Componente | Versión / detalle |
|---|---|
| Java (JDK) | 17 (JAVA_HOME configurado) |
| Maven | 3.8+ (compilación y empaquetado del backend) |
| Spring Boot | 3.2 (artefacto com.mycompany:sacejpa:0.0.1-SNAPSHOT) |
| PostgreSQL | 18+ (base SACE_db en el puerto 5432) |
| Frontend | HTML/CSS/JS puro + Bootstrap 5.3.3 (CDN); sin build |
| Servidor estático | python -m http.server o nginx |
| Puertos | Backend 8082 · Frontend 8091 · PostgreSQL 5432 |

### 4.3 Configuración y variables de entorno
El backend centraliza su configuración en application.properties y admite sobrescritura por variables de entorno (formato ${NOMBRE:valor_por_defecto}), lo que permite cambiar la configuración sin tocar código:

| Variable | Valor por defecto | Uso |
|---|---|---|
| SACE_TOKEN_SECRET | Cl4veDesarrolloSACE_AleLeoTours_2024_CambiarEnProduccion | Firma de tokens (cambiar en producción) |
| PROPIETARIO_EMAIL | propietario@aleleotours.com | Destinatario de notificaciones |
| SPRING_MAIL_HOST / PORT | localhost / 1025 | Servidor SMTP (fail-safe activo) |
| SPRING_MAIL_USERNAME / PASSWORD | notificaciones@aleleotours.com / secret | Credenciales SMTP |
| spring.datasource.url | jdbc:postgresql://localhost:5432/SACE_db | Conexión a PostgreSQL (password 1234 en desarrollo) |
| server.port / app.upload.dir | 8082 / uploads/comprobantes | Puerto de la API y carpeta de comprobantes PDF |

La referencia completa de variables (con las que se añadieron para la nube) está en `deploy/.env.example` y en el documento **10-Detalle-Despliegue-Como-Se-Hizo-Y-Como-Funciona.md**, sección 12.

### 4.4 Procedimiento de despliegue local (paso a paso)
Paso 1 — Crear la base de datos vacía (solo la primera vez):

     CREATE DATABASE SACE_db;

Paso 2 — Levantar el backend (las tablas y los datos semilla se crean solos):

     cd backend-SACE

     mvn spring-boot:run      # queda en http://localhost:8082

Paso 3 — Servir el frontend estático:

     cd frontend-aleleo-tours

     python -m http.server 8091 --bind 127.0.0.1   # http://localhost:8091

Paso 4 — Verificación: abrir http://localhost:8091, iniciar sesión con el administrador semilla (admin@aleleotours.com / admin123) y comprobar los módulos (Plan de Pruebas, sección 3.6).

### 4.5 Despliegue en servidor (procedimiento previsto)
1. Empaquetar el backend:  mvn clean package  → genera backend-SACE/target/sacejpa-0.0.1-SNAPSHOT.jar.

2. Definir las variables de entorno de producción (SACE_TOKEN_SECRET fuerte, credenciales de BD, SMTP real).

3. Iniciar el backend:  java -jar sacejpa-0.0.1-SNAPSHOT.jar.

4. Publicar el frontend: copiar frontend-aleleo-tours/ a la carpeta web del servidor (nginx: root + index.html).

5. Configurar el frontend para apuntar al dominio del backend: basta con editar la constante `API_FIJA` de `frontend-aleleo-tours/js/core/config.js`, que es el único archivo del frontend que conoce la dirección del API.

6. (Opcional) Registrar el proceso con systemd/PM2 y habilitar HTTPS con certificado.

### 4.6 Verificación post-despliegue
- GET /api/salud responde `{"estado":"OK","baseDatos":"conectada"}` (health check de la plataforma).
- GET /api/servicios responde JSON (catálogo público).
- Login de administrador y de un cliente recién registrado.
- Flujo completo: crear solicitud → pagar → descargar comprobante PDF.
- Reportes accesibles solo para administradores (403 para otros roles).
- Suite de regresión de humo: CP-02, CP-04, CP-06, CP-08, CP-11 del Plan de Pruebas.

### 4.7 Recomendaciones de producción
- Cambiar SACE_TOKEN_SECRET, la contraseña de PostgreSQL y las credenciales SMTP antes de exponer el sistema.
- Rotar las credenciales del administrador semilla (admin123) tras el primer ingreso.
- Habilitar HTTPS y no publicar los comprobantes PDF fuera de su ruta protegida.
- Mantener el modelo de datos con back-ups regulares (pg_dump).

### 4.8 Configuración lista para despliegue en la nube (resumen)

El proyecto quedó preparado para publicarse en la nube **sin recompilar**: lo que cambia entre entornos (base de datos, secretos, dominios, rutas de almacenamiento) se lee de variables de entorno, y el frontend resuelve solo la dirección del backend.

**Arquitectura objetivo** — se separan las tres piezas según su naturaleza:

```
FRONTEND estatico     Cloudflare Pages    CDN + HTTPS, sin build, costo $0
        |  fetch() a /api/...
        v
BACKEND Spring Boot   Render / Railway    contenedor construido desde el Dockerfile
        |  JDBC
        v
BASE DE DATOS         Neon / Railway      PostgreSQL 18 (SACE_db)
```

**Qué cambió en el proyecto** (el *por qué* y el *cómo funciona* están en el documento 10):

| Área | Archivos | Efecto |
|---|---|---|
| Configuración del backend | `application.properties`, `WebConfig.java`, `DataInitializer.java` | El mismo `.jar` sirve en local y en producción |
| Salud del servicio | `SaludController.java`, `AuthInterceptor.java` | `GET /api/salud` público, para health checks |
| Configuración del frontend | `js/core/config.js` (nuevo), `js/core/app.js`, 11 `.html` | Cambiar de backend sin recompilar |
| Imagen de contenedor | `Dockerfile`, `.dockerignore` | Build reproducible en cualquier plataforma |
| Plataformas PaaS | `render.yaml`, `railway.json` | Servicio creado con un clic |
| Servidor propio | `deploy/docker-compose.yml`, `nginx.conf`, `.env.example` | Opción C, con HTTPS y volumen persistente |

**Las tres rutas de despliegue** (procedimiento paso a paso en `deploy/README.md`):

| Opción | Componentes | Costo | Cuándo conviene |
|---|---|---|---|
| A | Cloudflare Pages + Render + Neon | $0 | Entrega académica y demostración |
| B | Cloudflare Pages + Railway | ~5 USD/mes | Sin cold start y con volumen persistente |
| C | VPS con Docker Compose + nginx | ~4 EUR/mes | Control total, dominio propio, respaldos |

**Verificación realizada**: el 4 de noviembre de 2026 se ejecutó el `.jar` contra una base de datos vacía usando las variables de entorno de producción. La salida real (health check, esquema creado, semilla, CORS, login) está en `06-Comprobante-de-Despliegue-SACE.md`, sección 6.9.

### 4.9 Riesgos del despliegue en la nube y dónde se tratan

| Riesgo | Consecuencia | Dónde se resuelve |
|---|---|---|
| Disco efímero de la PaaS | Se pierden los comprobantes PDF en cada redespliegue | `deploy/README.md` (volumen) y documento 10, sección 8 |
| La base gratuita de Render caduca a los 30 días | Pérdida de datos | Se usa Neon, que no caduca |
| Cold start del plan gratuito de Render (~50 s) | La primera visita tarda | Documento 10, sección 13 |
| CORS desalineado | `Failed to fetch` en el navegador | Documento 10, sección 13 |
| `SACE_DDL_AUTO=update` permanente | Hibernate modifica el esquema en producción | Documento 10, sección 11 |
| Wompi exige HTTPS y URL pública estable | El webhook firmado no se puede verificar | Documento 09 y documento 10, sección 11 |
| Ausencia de respaldos | Pérdida total ante un fallo | `deploy/README.md` (pg_dump programado) |
