# Projecto_SACE — Sistema de Atención al Cliente (AleLeo Tours)

Proyecto académico **SENA** compuesto por dos partes:

| Carpeta | Descripción | Tecnología |
|---|---|---|
| [`backend-SACE/`](backend-SACE/) | API REST del sistema: autenticación, solicitudes (PQR/reservas), mensajes, pagos con comprobante PDF y reportes | Java 17 · Spring Boot 3.2 · JPA/Hibernate · PostgreSQL |
| [`frontend-aleleo-tours/`](frontend-aleleo-tours/) | Sitio estático de la agencia: destinos, registro/login, panel de cliente, panel de empleado/admin y chatbot | HTML · CSS · Bootstrap · JavaScript vanilla |

## Requisitos

- JDK 17
- Maven 3.8+
- PostgreSQL 14+ (base de datos `SACE_db`, usuario `postgres`, contraseña `1234`, puerto `5432`)

## Crear la base de datos (una sola vez)

La BD **no está incluida en el repo** (no es necesaria): solo se crea vacía y el
proyecto la llena solo. Con PostgreSQL corriendo:

```sql
CREATE DATABASE SACE_db;
```

Al arrancar el backend, Hibernate crea todas las tablas (`ddl-auto=update`) y
`DataInitializer` inserta los datos iniciales: el administrador, el catálogo de
servicios/destinos y las preguntas frecuentes del chatbot.
(`backend-SACE/src/main/resources/schema_pagos.sql` queda como referencia de la tabla `pago`.)

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
python -m http.server 8090 --bind 127.0.0.1
```

Abrir **http://localhost:8090**. El frontend asume el backend en `localhost:8082`.

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
versiones `.docx` están listas en la carpeta local de evidencias:

| Documento | Contenido |
|---|---|
| [`docs/01-Analisis-y-Requerimientos-SACE.md`](docs/01-Analisis-y-Requerimientos-SACE.md) | Contexto, problema, objetivos, alcance, usuarios/roles, RF y RNF |
| [`docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md`](docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md) | Backlog, sprints, seguimiento y distribución del equipo |
| [`docs/03-Plan-de-Pruebas-SACE.md`](docs/03-Plan-de-Pruebas-SACE.md) | Estrategia, casos de prueba, datos y reporte de ejecución (62 PASS/0 FAIL) |
| [`docs/04-Plan-de-Despliegue-SACE.md`](docs/04-Plan-de-Despliegue-SACE.md) | Dependencias, variables de entorno y procedimiento de despliegue |

Además: `Historias_de_usuario.docx` (HU-01…HU-11), `Historia_necesidad.docx`,
diagramas UML (casos de uso, clases, MER, modelo relacional), diccionario de
datos e `Informe-01-Backend` e `Informe-02-Frontend`.

## Equipo y distribución del trabajo

| Integrante | Aporte principal |
|---|---|
| Cesar Leonardo Ramírez Montejo | Backend y seguridad: API REST, token, interceptor, pagos/comprobante PDF, pruebas E2E |
| Yerson Alexei Torres Garcia | Base de datos: modelo ER, modelo relacional, diccionario de datos y consultas |
| Felipe Gonzales Quitero | Frontend: páginas, componentes (chatbot) e integración con la API |

## Flujo de trabajo en Git (evidencia de control de versiones)

- Rama principal `main` con commits descriptivos y `merge --no-ff` de ramas de
  funcionalidad (p. ej. `feature/documentacion`).
- `.gitignore` para excluir artefactos de build e IDE.
- Identidad: [Siks2301](https://github.com/Siks2301) — `cesarleonardo.rm2301@gmail.com`.

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
