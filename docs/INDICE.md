# Índice del proyecto — SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_Actualizado: 10 de noviembre de 2026_

Este es el **único índice** del proyecto. Mapea cada criterio de la rúbrica SENA
con su evidencia, lista las capturas y los diagramas, y dice qué documento es la
fuente de verdad de cada tema.

Las versiones `.docx` (Norma APA 7) están en la carpeta local
`SACE_Entrega_Final/2_Evidencias_Documentales/`; en el repositorio se publican las
versiones `.md`, los diagramas, las capturas y el DDL del esquema.

---

## Documentos

| # | Documento | Qué contiene |
|---|---|---|
| 01 | [`01-Analisis-y-Requerimientos-SACE.md`](01-Analisis-y-Requerimientos-SACE.md) | Contexto, problema, objetivos, alcance, usuarios/roles, RF-01…14, RNF-01…08 e historias de usuario |
| 02 | [`02-Backlog-y-Gestion-del-Proyecto-SACE.md`](02-Backlog-y-Gestion-del-Proyecto-SACE.md) | Metodología, backlog, sprints S1…S5, seguimiento y reparto del equipo |
| 03 | [`03-Plan-de-Pruebas-SACE.md`](03-Plan-de-Pruebas-SACE.md) | Estrategia, datos, CP-01…CP-24 y reporte de ejecución (92 PASS / 0 FAIL) |
| 03b | [`Actividad-01-Plan-de-Pruebas-Frontend-Jest.md`](Actividad-01-Plan-de-Pruebas-Frontend-Jest.md) | Actividad 1: plan de pruebas del frontend con Jest (7 suites, 48 casos, evidencias y guion del video) |
| 04 | [`04-Plan-de-Despliegue-SACE.md`](04-Plan-de-Despliegue-SACE.md) | Arquitectura, dependencias, variables y guía paso a paso (local, opción A, B y C) |
| 05 | [`05-Manual-de-Usuario-SACE.md`](05-Manual-de-Usuario-SACE.md) | Manual de uso por perfil: cliente, empleado y administrador |
| 06 | [`06-Despliegue-Comprobante-y-Detalle-SACE.md`](06-Despliegue-Comprobante-y-Detalle-SACE.md) | Evidencia del despliegue, decisiones técnicas, operación y diagnóstico |
| 07 | [`07-Modelo-de-Datos-y-Normalizacion-SACE.md`](07-Modelo-de-Datos-y-Normalizacion-SACE.md) | Modelo de datos, relaciones, integridad y normalización 1FN/2FN/3FN |
| 08 | [`08-Guion-de-Presentacion-Final-SACE.md`](08-Guion-de-Presentacion-Final-SACE.md) | Guion de sustentación: orden, reparto, demo en vivo y preguntas |
| 09 | [`09-Integracion-Pasarela-Pagos-SACE.md`](09-Integracion-Pasarela-Pagos-SACE.md) | Pasarela Wompi, flujo, modelo de datos, seguridad y modos `mock`/`wompi` |
| Anexo | [`Anexo-Errores-y-Correcciones.md`](Anexo-Errores-y-Correcciones.md) | Bitácora de hallazgos, correcciones y rondas de verificación manual |
| Anexo | [`API-Endpoints-SACE.md`](API-Endpoints-SACE.md) | Catálogo de endpoints de la API REST |
| Anexo | [`SACE_db_ddl.sql`](SACE_db_ddl.sql) | DDL del esquema completo de `SACE_db` (solo estructura) |

---

## Autoevaluación contra la rúbrica

| # | Criterio (peso) | Evidencias | Nivel |
|---|---|---|---|
| 1 | Análisis y requerimientos (15 %) | Documento 01: problema, objetivos, alcance, usuarios/roles, RF-01…14, RNF-01…08 e historias HU-01…HU-11 | Completo (5) |
| 2 | Diseño del sistema (15 %) | [`diagramas/`](diagramas/) (casos de uso, clases, arquitectura, MER y modelo relacional) · 15 capturas de interfaz y despliegue (ver abajo) | Completo (5) |
| 3 | Desarrollo y funcionalidad (25 %) | Código [`backend-SACE/`](../backend-SACE) y [`frontend-aleleo-tours/`](../frontend-aleleo-tours) · [`API-Endpoints-SACE.md`](API-Endpoints-SACE.md) · manejo de errores 400/401/403/404/409 · informes de backend y frontend | Completo (5) |
| 4 | Base de datos (10 %) | [`SACE_db_ddl.sql`](SACE_db_ddl.sql) · documento 07 (relaciones, integridad y normalización) · `schema_email_unico.sql` y `schema_datos_corporativos.sql` · diccionario de datos (xlsx local) | Completo (5) |
| 5 | Control de versiones y colaboración (10 %) | Repositorio público GitHub (`Siks2301/Projecto_SACE`) · rama `main` con commits y `merge --no-ff` · commits por integrante · `.gitignore` | Completo (5) |
| 6 | Gestión del proyecto (10 %) | Documento 02: backlog, 11 historias de usuario, tareas T1…T8, sprints S1…S5 y seguimiento | Completo (5) |
| 7 | Documentación (5 %) | `README.md` · estos documentos · documento 05 (manual de usuario) · este índice | Completo (5) |
| 8 | Preparación para pruebas (5 %) | Documento 03 (CP-01…CP-24, datos y criterios) · Actividad 1 (Jest) · anexo de errores | Completo (5) |
| 9 | Preparación para el despliegue (5 %) | Documento 04 (plan y guía) · documento 06 (evidencia real, incluido el despliegue del 9-10 de noviembre de 2026 en Render + Neon + Cloudflare) | Completo (5) |

---

## Capturas de interfaz (12)

Tomadas el 25 de septiembre de 2026 sobre el sistema ejecutándose con datos reales
del backend, a 1440 px de ancho y página completa.

| # | Archivo | Pantalla |
|---|---|---|
| 01 | [`01_Login.png`](capturas/01_Login.png) | Inicio de sesión (pestañas Cliente / Empleado) |
| 02 | [`02_Index_Home_Admin.png`](capturas/02_Index_Home_Admin.png) | Inicio con menú de administrador |
| 03 | [`03_Gestion_Solicitudes.png`](capturas/03_Gestion_Solicitudes.png) | Gestión de solicitudes (tabla con datos) |
| 04 | [`04_Gestion_Clientes.png`](capturas/04_Gestion_Clientes.png) | Gestión de clientes |
| 05 | [`05_Gestion_Empleados.png`](capturas/05_Gestion_Empleados.png) | Gestión de empleados |
| 06 | [`06_Gestion_Servicios.png`](capturas/06_Gestion_Servicios.png) | Gestión de servicios del catálogo |
| 07 | [`07_Reportes.png`](capturas/07_Reportes.png) | Reportes y KPIs del administrador |
| 08 | [`08_Panel_Empleado.png`](capturas/08_Panel_Empleado.png) | Panel del asesor de soporte |
| 09 | [`09_Index_Home_Cliente.png`](capturas/09_Index_Home_Cliente.png) | Inicio con menú de cliente |
| 10 | [`10_Destinos.png`](capturas/10_Destinos.png) | Catálogo de destinos con búsqueda |
| 11 | [`11_Mis_Solicitudes.png`](capturas/11_Mis_Solicitudes.png) | Mis solicitudes con chatbot AleLeoBot |
| 12 | [`12_Registro_Cliente.png`](capturas/12_Registro_Cliente.png) | Formulario de registro de cliente |

## Capturas del despliegue (3)

| # | Archivo | Captura |
|---|---|---|
| 13 | [`01_Despliegue_Login.png`](capturas/01_Despliegue_Login.png) | Login del sistema en ejecución |
| 14 | [`02_Despliegue_Home.png`](capturas/02_Despliegue_Home.png) | Home del sitio en ejecución |
| 15 | [`03_Despliegue_Destinos.png`](capturas/03_Despliegue_Destinos.png) | Catálogo consultado desde el despliegue |

## Diagramas

[`diagramas/`](diagramas/) contiene: casos de uso, clases, arquitectura, modelo
entidad-relación y modelo relacional (PNG).

---

## Credenciales de demostración

> Solo para desarrollo local. En producción la contraseña del administrador se
> define con la variable `SACE_ADMIN_PASSWORD` y **no** es la de este documento.

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador (semilla local) | `admin@aleleotours.com` | `admin123` |
| Cliente de demostración | `cesarleonardo.rm2301@gmail.com` | `DemoClave2026!a` |

La pasarela de pagos se controla con `PAGOS_PASARELA` (`mock` por defecto o
`wompi`). El monto lo calcula el backend desde el catálogo. Ver documento 09.
