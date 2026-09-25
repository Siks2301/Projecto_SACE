# Projecto_SACE — Sistema de Atención al Cliente (AleLeo Tours)

Proyecto académico **SENA** compuesto por dos partes:

| Carpeta | Descripción | Tecnología |
|---|---|---|
| [`backend-SACE/`](backend-SACE/) | API REST del sistema: autenticación, solicitudes (PQR/reservas), mensajes, pagos con comprobante PDF y reportes | Java 17 · Spring Boot 3.2 · JPA/Hibernate · PostgreSQL |
| [`frontend-aleleo-tours/`](frontend-aleleo-tours/) | Sitio estático de la agencia: destinos, registro/login, panel de cliente, panel de empleado/admin y chatbot | HTML · CSS · Bootstrap · JavaScript vanilla |

## Requisitos

- JDK 17
- Maven 3.8+
- PostgreSQL 18+ (base de datos `SACE_db`, usuario `postgres`, contraseña `1234`, puerto `5432`)

## Crear la base de datos (una sola vez)

La BD **no está incluida en el repo** (no es necesaria): solo se crea vacía y el
proyecto la llena solo. Con PostgreSQL corriendo:

```sql
CREATE DATABASE SACE_db;
```

Al arrancar el backend, Hibernate crea todas las tablas (`ddl-auto=update`) y
`DataInitializer` inserta los datos iniciales: el administrador, el catálogo de
servicios/destinos y las preguntas frecuentes del chatbot.
(`backend-SACE/src/main/resources/schema_pagos.sql` queda como referencia de la tabla `pago`;
`schema_email_unico.sql` y `schema_datos_corporativos.sql` son las correcciones de datos
aplicadas sobre la base: correo único en `persona`, dominio `@aleleotours.com` y un solo administrador.)

## Ejecutar el backend

```powershell
cd backend-SACE
mvn spring-boot:run
```

Queda escuchando en **http://localhost:8082**.

La configuración está en `backend-SACE/src/main/resources/application.properties`.

## Ejecutar el frontend

Es un sitio 100 % estático; basta con servir la carpeta:

```powershell
cd frontend-aleleo-tours
python -m http.server 8091 --bind 127.0.0.1
```

Abrir **http://localhost:8091**. El frontend asume el backend en `localhost:8082`.

## Usuario administrador por defecto

- Email: `admin@aleleotours.com`
- Contraseña: `admin123`

## Funcionalidades

- Registro/login de clientes (apellido opcional) y panel de empleados/administradores.
- Gestión de solicitudes por categorías: RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE, OTRO.
- Chat de mensajes por solicitud y chatbot con catálogo desde la API.
- Pago con llave **Bre-B**, precio tomado del servicio real en BD, anti-doble-cobro y comprobante PDF con sello SHA-256 + IVA 19 %.
- Reportes y KPIs restringidos a administradores.
- Autorización por propiedad (403) en pagos, cancelaciones, mensajes y reportes.

## Documentación del proyecto (evidencias académicas)

Los documentos de entrega están en [`docs/`](docs/) (versiones `.md`) y sus
versiones `.docx` están listas en la carpeta local de evidencias.
El [`INDICE_EVIDENCIAS.md`](docs/INDICE_EVIDENCIAS.md) mapea cada criterio de
la rúbrica de 9 puntos con su evidencia.

| Documento | Contenido |
|---|---|
| [`docs/01-Analisis-y-Requerimientos-SACE.md`](docs/01-Analisis-y-Requerimientos-SACE.md) | Contexto, problema, objetivos, alcance, usuarios/roles, RF y RNF |
| [`docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md`](docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md) | Backlog, sprints, seguimiento y distribución del equipo |
| [`docs/03-Plan-de-Pruebas-SACE.md`](docs/03-Plan-de-Pruebas-SACE.md) | Estrategia, casos de prueba, datos y reporte de ejecución (62 PASS/0 FAIL) |
| [`docs/04-Plan-de-Despliegue-SACE.md`](docs/04-Plan-de-Despliegue-SACE.md) | Dependencias, variables de entorno y procedimiento de despliegue |
| [`docs/05-Manual-de-Usuario-SACE.md`](docs/05-Manual-de-Usuario-SACE.md) | Manual de uso por perfil: cliente, empleado y administrador |
| [`docs/06-Comprobante-de-Despliegue-SACE.md`](docs/06-Comprobante-de-Despliegue-SACE.md) | Verificación real del despliegue (servicios, salud HTTP, capturas) |
| [`docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md`](docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md) | Modelo de datos, relaciones, integridad y normalización 1FN/2FN/3FN |
| [`docs/08-Guion-de-Presentacion-Final-SACE.md`](docs/08-Guion-de-Presentacion-Final-SACE.md) | Guion de sustentación: orden, reparto por integrante, demo en vivo y Q&A |
| [`docs/API-Endpoints-SACE.md`](docs/API-Endpoints-SACE.md) | Catálogo de endpoints de la API REST |
| [`docs/INDICE-CAPTURAS.md`](docs/INDICE-CAPTURAS.md) | Índice de las 15 capturas (interfaz + despliegue) |

Además: `Historias_de_usuario.docx` (HU-01…HU-11), `Historia_necesidad.docx`,
diagramas UML (casos de uso, clases, MER, modelo relacional), diccionario de
datos e `Informe-01-Backend` e `Informe-02-Frontend`.

### Evidencias visuales y base de datos

| Recurso | Contenido |
|---|---|
| [`docs/diagramas/`](docs/diagramas/) | Diagramas en PNG: casos de uso, clases, **arquitectura del sistema**, modelo entidad-relación y modelo relacional |
| [`docs/capturas/`](docs/capturas/) | 15 capturas de pantalla (12 interfaz + 3 despliegue) detalladas en [`INDICE-CAPTURAS.md`](docs/INDICE-CAPTURAS.md) |
| [`docs/SACE_db_ddl.sql`](docs/SACE_db_ddl.sql) | DDL del esquema completo de `SACE_db` (solo estructura, sin datos) generado con `pg_dump --schema-only` |

## Equipo y distribución del trabajo

| Integrante | Aporte principal |
|---|---|
| Cesar Leonardo Ramírez Montejo | Backend y seguridad: API REST, token, interceptor, pagos/comprobante PDF, pruebas E2E y despliegue |
| Yerson Alexei Torres Garcia | Base de datos: modelo ER, modelo relacional, diccionario de datos, normalización y consultas |
| Felipe Gonzales Quitero | Frontend: páginas, componentes (chatbot) e integración con la API |

Cada integrante tiene **commits con su autoría** sobre su módulo (evidencia de
participación en el repositorio): Cesar (backend/API/despliegue), Yerson
(modelo de datos y normalización, DDL) y Felipe (interfaces y capturas).

## Flujo de trabajo en Git (evidencia de control de versiones)

- Rama principal `main` con commits descriptivos y `merge --no-ff` de ramas de
  funcionalidad (p. ej. `feature/documentacion`).
- Commits por integrante sobre su módulo (evidencia de participación).
- `.gitignore` para excluir artefactos de build e IDE.
- Identidad principal: [Siks2301](https://github.com/Siks2301) —
  `cesarleonardo.rm2301@gmail.com`.

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
└── docs/                    # Evidencias académicas (markdown)
```
