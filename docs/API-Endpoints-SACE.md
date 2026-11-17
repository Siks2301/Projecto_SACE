# API REST — Endpoints del backend SACE (AleLeo Tours)

Resumen de la API REST del backend (`backend-SACE`, Java 17 · Spring Boot 3.2).
Base URL local: `http://localhost:8082/api` · Producción:
`https://sace-api.onrender.com/api` · Formato: JSON · Autenticación:
`Authorization: Bearer <token>`.

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

Los campos omitidos en `POST /solicitudes` los completa el servidor: `estado =
PENDIENTE`, `prioridad = MEDIA`, `fechaCreacion = ahora` y `pasajeros` rescatado
del texto o `1`. Un cliente no puede crear su solicitud como `RESUELTA`.

## Mensajes (`/mensajes`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/mensajes/solicitud/{idSolicitud}` | Hilo de mensajes de una solicitud |
| POST | `/mensajes` | Enviar mensaje (texto o archivo adjunto) |
| PUT | `/mensajes/{id}/calificacion` | Calificar atención del cliente |
| GET | `/mensajes/{id}` | Detalle de un mensaje |

## Pagos (`/pagos`)

Pagos en línea contra una pasarela real (Wompi) o una pasarela simulada según la
configuración. El monto **lo calcula el backend** desde el catálogo de servicios;
el cliente solo elige el método. Ver `docs/09-Integracion-Pasarela-Pagos-SACE.md`.

El catálogo guarda el precio **por persona**, así que el monto es
`precio unitario × solicitud.pasajeros` (1–10). `GET /pagos/cotizacion/{id}`
devuelve ese desglose en `monto`, `precioUnitario` y `pasajeros`; `POST /pagos`
solo recibe `solicitudId`, `metodoPago` y `notas`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/pagos/configuracion` | Pasarela activa y métodos de pago habilitados |
| GET | `/pagos/cotizacion/{idSolicitud}` | Total a cobrar (unitario × pasajeros) y su desglose |
| POST | `/pagos` | Inicia un cobro. Acepta **solo** `solicitudId`, `metodoPago` y `notas` |
| GET | `/pagos/estado/{referencia}` | Estado del pago; reconcilia con la pasarela antes de responder |
| GET | `/pagos/comprobante/{idPago}` | Descargar comprobante PDF (solo si está APROBADO; sello SHA-256 + IVA) |
| GET | `/pagos/solicitud/{idSolicitud}` | Consultar los pagos de una solicitud |
| DELETE | `/pagos/{id}` | **Solo admin.** Eliminar un pago (limpieza de datos de prueba) |
| POST | `/pagos/webhook/wompi` | Notificación de la pasarela. **Público**, autenticado por checksum SHA-256 |
| POST | `/pagos/webhook/verificacion` | Verificación de vida del endpoint |

> Enviar un campo `monto` en `POST /pagos` produce `400`: el servidor rechaza
> explícitamente que el cliente imponga el precio.

El `codigoAutorizacion` y el `idTransaccionPasarela` que devuelve la pasarela se
guardan al consultar el estado, en el webhook y en la conciliación. Por eso el
comprobante PDF muestra el código real (p. ej. `SIM-6814`) aunque el pago se
haya aprobado por polling y no por webhook.

## Administración (CRUD, solo admin/roles autorizados)

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST/PUT/DELETE | `/clientes`, `/clientes/{id}` | Gestión de clientes |
| GET/POST/PUT/DELETE | `/empleados`, `/empleados/{id}` | Gestión de empleados. El `PUT` normal ignora `tipoUsuario` |
| PUT | `/empleados/{id}/tipo` | **Solo admin.** Cambiar rol: `ADMINISTRADOR`, `ASESOR_VIAJES`, `COORDINADOR_DESTINO` o `CLIENTE` |
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