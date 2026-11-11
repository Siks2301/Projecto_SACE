# Projecto_SACE — Sistema de Atención al Cliente (AleLeo Tours)

Proyecto académico **SENA** (ficha 3171149-B_) compuesto por dos partes:

| Carpeta | Descripción | Tecnología |
|---|---|---|
| [`backend-SACE/`](backend-SACE/) | API REST: autenticación, solicitudes (PQR/reservas), mensajes, pagos con comprobante PDF y reportes | Java 17 · Spring Boot 3.2 · JPA/Hibernate · PostgreSQL |
| [`frontend-aleleo-tours/`](frontend-aleleo-tours/) | Sitio estático: destinos, registro/login, panel de cliente, panel de empleado/admin y chatbot | HTML · CSS · Bootstrap 5.3 · JavaScript vanilla |

## Inicio rápido (local)

Requisitos: JDK 17, Maven 3.8+ y PostgreSQL 18+ en el puerto 5432.

```powershell
# 1) Base de datos vacía (solo la primera vez)
#    CREATE DATABASE SACE_db;   -> con psql o pgAdmin

# 2) Backend  ->  http://localhost:8082
cd backend-SACE
mvn spring-boot:run

# 3) Frontend ->  http://localhost:8091
cd frontend-aleleo-tours
python -m http.server 8091 --bind 127.0.0.1
```

Hibernate crea las tablas (`ddl-auto=update`) y `DataInitializer` siembra el
administrador, el catálogo y las preguntas frecuentes del chatbot.

Administrador semilla (**solo desarrollo**): `admin@aleleotours.com` / `admin123`.
En producción la contraseña se define con `SACE_ADMIN_PASSWORD`; no se usa la de
este README.

> El procedimiento completo, las variables de entorno y el despliegue en la nube
> (Cloudflare + Render + Neon), en Railway o en un VPS están en
> [`docs/04-Plan-de-Despliegue-SACE.md`](docs/04-Plan-de-Despliegue-SACE.md).

## Funcionalidades

- Registro/login de clientes (apellido opcional) y paneles de empleado y administrador.
- Gestión de solicitudes por categorías: RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE y OTRO.
- Chat de mensajes por solicitud y chatbot con catálogo desde la API.
- **Pago en línea** con pasarela real (Wompi, sandbox) o simulada, sin conexión:
  el precio lo calcula el backend desde el catálogo **por persona y lo multiplica
  por los viajeros de la reserva** (el cliente solo elige el método), confirmación
  por webhook firmado con SHA-256 y polling de respaldo,
  anti-doble-cobro con índice único parcial y comprobante PDF con sello SHA-256 e
  IVA 19 %. Detalle en [`docs/09-Integracion-Pasarela-Pagos-SACE.md`](docs/09-Integracion-Pasarela-Pagos-SACE.md).
- Reportes y KPIs restringidos a administradores.
- Autorización por propiedad (403) en pagos, cancelaciones, mensajes y reportes.

## Documentación

Todo el material académico está en [`docs/`](docs/), y el mapa completo (rúbrica,
capturas y diagramas) en [`docs/INDICE.md`](docs/INDICE.md).

| Documento | Contenido |
|---|---|
| [`docs/01-Analisis-y-Requerimientos-SACE.md`](docs/01-Analisis-y-Requerimientos-SACE.md) | Contexto, problema, objetivos, alcance, usuarios/roles, RF y RNF |
| [`docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md`](docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md) | Backlog, sprints, seguimiento y distribución del equipo |
| [`docs/03-Plan-de-Pruebas-SACE.md`](docs/03-Plan-de-Pruebas-SACE.md) | Estrategia, CP-01…CP-24, datos y reporte (92 PASS / 0 FAIL) |
| [`docs/Actividad-01-Plan-de-Pruebas-Frontend-Jest.md`](docs/Actividad-01-Plan-de-Pruebas-Frontend-Jest.md) | Actividad 1: Jest, 7 suites, 48 casos, evidencias y guion del video |
| [`docs/04-Plan-de-Despliegue-SACE.md`](docs/04-Plan-de-Despliegue-SACE.md) | Arquitectura, variables y guía paso a paso (local, nube y VPS) |
| [`docs/05-Manual-de-Usuario-SACE.md`](docs/05-Manual-de-Usuario-SACE.md) | Manual de uso por perfil |
| [`docs/06-Despliegue-Comprobante-y-Detalle-SACE.md`](docs/06-Despliegue-Comprobante-y-Detalle-SACE.md) | Evidencia real del despliegue, decisiones técnicas y operación |
| [`docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md`](docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md) | Modelo, relaciones, integridad y normalización 1FN/2FN/3FN |
| [`docs/08-Guion-de-Presentacion-Final-SACE.md`](docs/08-Guion-de-Presentacion-Final-SACE.md) | Guion de sustentación, reparto y demo en vivo |
| [`docs/09-Integracion-Pasarela-Pagos-SACE.md`](docs/09-Integracion-Pasarela-Pagos-SACE.md) | Pasarela Wompi, webhook, seguridad y modos `mock`/`wompi` |
| [`docs/Anexo-Errores-y-Correcciones.md`](docs/Anexo-Errores-y-Correcciones.md) | Bitácora de hallazgos y correcciones |
| [`docs/API-Endpoints-SACE.md`](docs/API-Endpoints-SACE.md) | Catálogo de endpoints de la API REST |

Las versiones `.docx` (Norma APA 7) están en la carpeta local
`SACE_Entrega_Final/2_Evidencias_Documentales/`, junto con las historias de
usuario, los informes, el diccionario de datos y las fichas de calidad.

### Pruebas automatizadas del frontend

[`frontend-aleleo-tours/pruebas/`](frontend-aleleo-tours/pruebas/) contiene la
suite de Jest 29 + jsdom. No requiere backend ni base de datos: carga los scripts
reales de `js/` en un DOM simulado.

```powershell
cd frontend-aleleo-tours/pruebas
npm ci
npx jest --verbose
```

Resultado: **7 suites y 48 casos aprobados, 0 fallos**. El plan completo, las
capturas y el guion del video están en
[`docs/Actividad-01-Plan-de-Pruebas-Frontend-Jest.md`](docs/Actividad-01-Plan-de-Pruebas-Frontend-Jest.md).

## Equipo y distribución del trabajo

| Integrante | Aporte principal |
|---|---|
| Cesar Leonardo Ramírez Montejo | Backend y seguridad: API REST, token, interceptor, pagos/comprobante PDF, pruebas E2E y despliegue |
| Yerson Alexei Torres Garcia | Base de datos: modelo ER, modelo relacional, diccionario de datos, normalización y consultas |
| Felipe Gonzales Quitero | Frontend: páginas, componentes (chatbot) e integración con la API |

Cada integrante tiene **commits con su autoría** sobre su módulo (evidencia de
participación en el repositorio).

## Flujo de trabajo en Git

- Rama principal `main` con commits descriptivos y `merge --no-ff` de ramas de
  funcionalidad.
- Commits por integrante sobre su módulo.
- `.gitignore` para excluir artefactos de build, IDE y secretos.
- Identidad principal: [Siks2301](https://github.com/Siks2301).

## Estructura

```
Projecto_SACE/
├── backend-SACE/            # API Spring Boot
│   ├── pom.xml
│   └── src/main/java/com/mycompany/sacejpa/
├── frontend-aleleo-tours/   # Sitio estático
│   ├── *.html
│   ├── css/
│   ├── js/
│   └── img/
├── deploy/                  # Docker Compose, nginx y plantilla de variables
├── docs/                    # Documentación académica y evidencias
├── Dockerfile               # Imagen de producción multi-etapa
├── render.yaml              # Blueprint de Render
├── railway.json             # Configuración de Railway
└── wrangler.jsonc           # Frontend en Cloudflare Workers
```
