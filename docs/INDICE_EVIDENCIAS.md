# Índice de evidencias — Rúbrica de evaluación SENA (9 criterios)

Proyecto: **SACE — Sistema de Atención y Gestión de Solicitudes de Soporte (AleLeo Tours)**
Ficha 3171149-B · Análisis y Desarrollo de Software · Entrega: 21 de septiembre de 2026

Este índice mapea cada criterio de la rúbrica oficial con las evidencias que lo
soportan. Las evidencias `.docx` (Norma APA 7) están en la carpeta local
`SACE_Entrega_Final/2_Evidencias_Documentales/`; en este repositorio se publican
las versiones `.md`, los diagramas, las capturas y el DDL del esquema.

## Autoevaluación contra la rúbrica

| # | Criterio (peso) | Evidencias | Nivel |
|---|---|---|---|
| 1 | Análisis y requerimientos (15 %) | [`01-Analisis-y-Requerimientos-SACE.md`](01-Analisis-y-Requerimientos-SACE.md): problema, objetivos, alcance, usuarios/roles, RF-01…14 y RNF-01…08 · Historias de usuario HU-01…HU-11 · Historia de necesidad | Completo (5) |
| 2 | Diseño del sistema (15 %) | Diagramas de casos de uso y de clases · **diagrama de arquitectura** · MER y modelo relacional ([`diagramas/`](diagramas/)) · 12 capturas de interfaz + índice ([`INDICE-CAPTURAS.md`](INDICE-CAPTURAS.md)) | Completo (5) |
| 3 | Desarrollo y funcionalidad (25 %) | Código backend [`backend-SACE/`](../backend-SACE) y frontend [`frontend-aleleo-tours/`](../frontend-aleleo-tours) · [`API-Endpoints-SACE.md`](API-Endpoints-SACE.md) · manejo de errores 400/401/403/404/409 · informes de backend y frontend | Completo (5) |
| 4 | Base de datos (10 %) | [`SACE_db_ddl.sql`](SACE_db_ddl.sql) (esquema completo, sin datos) · [`07-Modelo-de-Datos-y-Normalizacion-SACE.md`](07-Modelo-de-Datos-y-Normalizacion-SACE.md): relaciones, integridad y normalización 1FN/2FN/3FN · Diccionario de datos (xlsx local) | Completo (5) |
| 5 | Control de versiones y colaboración (10 %) | Repositorio público GitHub (Siks2301/Projecto_SACE) · rama `main` con commits y `merge --no-ff` · **commits por integrante** (Cesar/backend, Yerson/BD, Felipe/frontend) · `.gitignore` | Completo (5) |
| 6 | Gestión del proyecto (10 %) | [`02-Backlog-y-Gestion-del-Proyecto-SACE.md`](02-Backlog-y-Gestion-del-Proyecto-SACE.md) · backlog · 11 historias de usuario · tareas T1…T8 · sprints S1…S5 · seguimiento y cumplimiento | Completo (5) |
| 7 | Documentación (5 %) | README.md (requisitos, instalación, tecnologías, configuración y evidencias) · docs/*.md · [`05-Manual-de-Usuario-SACE.md`](05-Manual-de-Usuario-SACE.md) · INDICE_EVIDENCIAS.md | Completo (5) |
| 8 | Preparación para pruebas (5 %) | [`03-Plan-de-Pruebas-SACE.md`](03-Plan-de-Pruebas-SACE.md) · casos CP-01…CP-14 · datos de prueba · criterios de aceptación · **62 PASS / 0 FAIL** | Completo (5) |
| 9 | Preparación para el despliegue (5 %) | [`04-Plan-de-Despliegue-SACE.md`](04-Plan-de-Despliegue-SACE.md) · [`06-Comprobante-de-Despliegue-SACE.md`](06-Comprobante-de-Despliegue-SACE.md) con verificación real (servicios, salud HTTP, capturas) | Completo (5) |

## Recursos del repositorio

- `docs/diagramas/` — casos de uso, clases, **arquitectura**, modelo entidad-relación y modelo relacional (PNG).
- `docs/capturas/` — 15 capturas (12 de interfaz + 3 del despliegue) detalladas en [`INDICE-CAPTURAS.md`](INDICE-CAPTURAS.md).
- `docs/SACE_db_ddl.sql` — esquema completo de la base de datos (sin datos).
- [`docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md`](07-Modelo-de-Datos-y-Normalizacion-SACE.md) — modelo, integridad y normalización.
- [`docs/API-Endpoints-SACE.md`](API-Endpoints-SACE.md) — catálogo de endpoints del backend.
- [`docs/08-Guion-de-Presentacion-Final-SACE.md`](08-Guion-de-Presentacion-Final-SACE.md) — guion de sustentación: orden de exposición, reparto por integrante, demo en vivo y preguntas preparadas.

## Credenciales de demostración

- Administrador semilla: `admin@aleleotours.com` / `admin123` (solo desarrollo).
- Cliente de demostración: `carlos.captura@ejemplo.com` / `DemoClave2026!a`.
- Llave de pago del flujo académico: `Bre-B @VXM301`.