# API REST — Endpoints del backend SACE (AleLeo Tours)

Resumen de la API REST del backend (`backend-SACE`, Java 17 · Spring Boot 3.2).
Base URL: `http://localhost:8082/api` · Formato: JSON · Autenticación: `Authorization: Bearer <token>`.

## Autenticación (`/auth`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/registro` | Registro de cliente; devuelve token (inicia sesión) |
| POST | `/auth/login` | Inicio de sesión de cliente o empleado; devuelve token y rol |

## Catálogo público

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/servicios` | Catálogo de servicios/destinos con precio real desde BD |
| GET | `/servicios/{id}` | Detalle de un servicio |
| GET | `/preguntas-frecuentes` | Catálogo de FAQs del chatbot |

## Solicitudes (`/solicitudes` — protección por propiedad)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/solicitudes` | Listado de solicitudes del cliente / asignadas al asesor |
| GET | `/solicitudes/{id}` | Detalle (solo propietario o asesor asignado) |
| POST | `/solicitudes` | Crear solicitud por categoría (RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE, OTRO) |
| PUT | `/solicitudes/{id}` | Actualizar estado/prioridad/asignación |
| DELETE | `/solicitudes/{id}` | Cancelación por el propietario (sin borrado físico de historial) |

## Mensajes (`/mensajes`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/mensajes/solicitud/{idSolicitud}` | Hilo de mensajes de una solicitud |
| POST | `/mensajes` | Enviar mensaje (texto o archivo adjunto) |
| PUT | `/mensajes/{id}/calificacion` | Calificar atención del cliente |
| GET | `/mensajes/{id}` | Detalle de un mensaje |

## Pagos (`/pagos`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/pagos` | Pagar una solicitud: monto real de BD, llave Bre-B, anti-doble-cobro |
| GET | `/pagos/comprobante/{idPago}` | Descargar comprobante PDF (solo propietario, sello SHA-256 + IVA) |
| GET | `/pagos/solicitud/{idSolicitud}` | Consultar el pago de una solicitud |

## Administración (CRUD, solo admin/roles autorizados)

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST/PUT/DELETE | `/clientes`, `/clientes/{id}` | Gestión de clientes |
| GET/POST/PUT/DELETE | `/empleados`, `/empleados/{id}` | Gestión de empleados |
| GET/POST/PUT/DELETE | `/servicios`, `/servicios/{id}` | Gestión del catálogo |
| GET/POST/PUT/DELETE | `/preguntas-frecuentes`, `/preguntas-frecuentes/{id}` | Gestión de FAQs |
| GET/POST/PUT/DELETE | `/chatbots`, `/chatbots/{id}` | Gestión del chatbot |

## Reportes y KPIs (`/reportes` — solo administradores)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/reportes/admin` | Consolidado de ventas y solicitudes |
| GET | `/reportes/empleado/{idEmpleado}` | Desempeño por asesor |
| GET | `/reportes/kpis` | Indicadores clave (KPIs) |
| GET | `/reportes/estados` | Conteo por estado |
| GET | `/reportes/categorias` | Conteo por categoría |
| GET | `/reportes/tendencia` | Tendencia temporal |
| GET | `/reportes/empleados` | Ranking de empleados |
| GET | `/reportes/pdf` | Exportación del reporte a PDF |

## Manejo de errores (GlobalExceptionHandler)

| Código | Caso |
|---|---|
| 400 | Validación de datos / reglas de negocio (doble cobro, mensaje en solicitud cancelada) |
| 401 | Credenciales incorrectas o token inválido/vencido |
| 403 | Acceso denegado (autorización por propiedad: recurso ajeno) |
| 404 | Recurso inexistente |
| 409 | Conflicto (correo duplicado en registro) |

El interceptor de seguridad protege `/api/**`: inyecta la sesión desde el token
firmado HMAC-SHA256 (24 h) y valida la propiedad de cada recurso antes de
procesar las operaciones sensibles (pagos, mensajes, cancelaciones, reportes).