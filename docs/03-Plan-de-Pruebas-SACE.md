# Plan de pruebas

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 3. Plan de pruebas
### 3.1 Objetivo y alcance
Verificar el cumplimiento funcional y de seguridad del sistema SACE de extremo a extremo: backend real (API en localhost:8082 contra PostgreSQL), frontend en entorno DOM simulado y regresión integral tras cada corrección.

### 3.2 Estrategia
- Pruebas funcionales E2E sobre la API real (PowerShell + Invoke-RestMethod).
- Pruebas de comportamiento del frontend ejecutando las funciones reales de los scripts en un DOM simulado (Node + vm).
- Pruebas de seguridad: autorización por propiedad, anti-doble-cobro, saneamiento (XSS) y precios tomados de la base de datos.
- Regresión completa por cada ronda de correcciones (0 fallos permitidos).
### 3.3 Entorno y herramientas
| Elemento | Detalle |
|---|---|
| Backend | API REST Spring Boot en http://localhost:8082/api (entorno real) |
| Base de datos | PostgreSQL local, base SACE_db (ddl-auto=update; semillas automáticas) |
| Frontend | JavaScript real ejecutado en Node con DOM simulado (vm) |
| Herramientas | PowerShell, Invoke-RestMethod, Node.js, guiones test_regression.ps1, test_fixes.ps1 y test_frontend_fixes.js |
| Control de cambios | Repositorio Git/GitHub (rama main + ramas de funcionalidad) |

### 3.4 Datos de prueba
- Administrador: admin@aleleotours.com / admin123 (semilla de DataInitializer).
- Clientes creados por corrida con dominio de prueba (victima/atacante…@test.com, contraseña Viaje2026!).
- Solicitudes por cada categoría (RESERVA, EQUIPAJE, CANCELADA, etc.) y estados (PENDIENTE, RESUELTA).
- Pagos: cobro en línea contra la pasarela (Wompi en sandbox o versión simulada). Se verifica que el monto lo calcula el backend desde el catálogo, que un `monto` enviado por el cliente se rechaza con 400, y que el reintento de la misma solicitud se bloquea por el índice único parcial.
- Casos negativos: credenciales erróneas, solicitudes ajenas, mensajes en solicitudes canceladas, sin sesión.
### 3.5 Casos de prueba
| ID | Caso de prueba | Resultado esperado |
|---|---|---|
| CP-01 | Registro de cliente (nombre/apellido opcional, correo duplicado, sin nombre) | 201 con apellido vacío legible; 400 si falta nombre; 409 si el correo existe |
| CP-02 | Inicio de sesión correcto e incorrecto; rol detectado | 2xx + token; 401 con credenciales malas |
| CP-03 | Lectura/escritura de solicitudes ajenas (IDOR) | 403 / 404; nunca 200 |
| CP-04 | Cancelación por el propietario y por un tercero | 200 + estado CANCELADA; 403 para el tercero |
| CP-05 | Mensajes: escribir y leer en hilo propio y ajeno | 200 en propio; 403 en ajeno; 400 si la solicitud está CANCELADA |
| CP-06 | Pago único y doble cobro | 201 el primero; 400 el segundo (ValidacionException explícita). Garantía final: índice único parcial `uq_pago_aprobado_por_solicitud` |
| CP-07 | Intento de imponer el precio: envío de `monto`, `amount` o `precio` en `POST /pagos` | 400 con el mensaje "El monto no lo puede elegir el cliente". El DTO no tiene dónde almacenarlo. Sin servicio con precio válido también se rechaza, nunca se cobra un cero |
| CP-08 | Comprobante PDF: descarga del propietario y de un tercero | 200 + application/pdf para el dueño; 403 para el tercero |
| CP-09 | Solicitud RESUELTA con pago: etiquetas y botones | Badge 'Pago Aprobado' + descargar; sin botón Pagar |
| CP-10 | Cancelación de solicitud inexistente y PUT rechazado | Aviso claro al usuario; se muestra el motivo real del servidor; sin DELETE de clientes |
| CP-11 | Reportes y KPIs (admin vs cliente) | 200 para admin; 403 para cliente |
| CP-12 | Saneamiento de datos del API en plantillas (XSS) | Los valores con HTML se escapan; no se ejecutan |
| CP-13 | Chatbot: catálogo desde API con respaldo local | El precio citado proviene de /api/servicios; respaldos solo si la API no responde |
| CP-14 | Sintaxis y compilación | node --check sin errores; mvn compile sin errores |

### 3.6 Casos de prueba del módulo de pagos (pasarela)

Verificación de lo específico de la pasarela. Se ejecutan en modo `mock`
(sin red) salvo los marcados como sandbox.

| Caso | Qué se prueba | Resultado esperado |
|---|---|---|
| CP-15 | `GET /api/pagos/configuracion` | 200 con la pasarela activa y los métodos habilitados; el modal no ofrece nada más |
| CP-16 | `GET /api/pagos/cotizacion/{id}` con servicio sin precio | 400 explicando que un asesor debe configurarlo; no se devuelve monto 0 |
| CP-17 | Webhook con checksum SHA-256 inválido | 401 y **ningún** cambio en `pago.estado` |
| CP-18 | Webhook con firma válida sobre referencia desconocida | 200 «ignorado»; no se crea ningún pago |
| CP-19 | Webhook repetido del mismo evento | 200 y sin efecto adicional; el PDF y el correo no se duplican |
| CP-20 | Transición ilegal (pago APROBADO → PENDIENTE) | Rechazada por la máquina de estados |
| CP-21 | Cobro en modo `mock` | Nace PENDIENTE → tras el retardo pasa a APROBADO → se descarga el PDF |
| CP-22 | `app.pagos.mock.resultado=RECHAZADO` | Estado RECHAZADO, mensaje de rechazo y **sin** comprobante |
| CP-23 | Falta la llave de integridad con `pasarela=wompi` | 400 explicando qué configurar; el pago queda PENDIENTE, no APROBADO |
| CP-24 | Pérdida del webhook | La conciliación (cada 5 min) o el polling de respaldo cierran el pago igual |

Cubiertos automáticamente por `pruebas/test_pagos_pasarela.ps1`: CP-15, CP-16,
CP-17, CP-21, y además el precio calculado por el servidor, el rechazo de
`monto`/`amount`/`precio`, el formato de la referencia, el IDOR y la firma del
webhook.

### 3.7 Reporte de ejecución
Ejecutado el 21 de septiembre de 2026 con el backend real y la base de datos local:

| Suite | Checks | Resultado |
|---|---|---|
| test_regression.ps1 (auth, solicitudes, cancelar, mensajes, pagos+PDF, reportes, saneos) | 37 | PASS |
| test_fixes.ps1 (IDOR, doble cobro, apellido opcional, precios) | 12 | PASS |
| test_frontend_fixes.js (modal, badges, estados, cancelación, XSS) | 13 | PASS |
| SUBTOTAL del diseño anterior | 62 | 62 PASS / 0 FAIL |
| **pruebas/test_pagos_pasarela.ps1** (módulo de pagos con pasarela, ver §3.6) | **30** | **30 PASS / 0 FAIL** |
| **TOTAL** | **92** | **92 PASS / 0 FAIL** |

> La suite de pagos se ejecutó el 3 de octubre de 2026 contra el backend real
> con la base migrada y `app.pagos.pasarela=mock`. No cubre el CP-24 (pérdida
> del webhook), que queda respaldado por diseño tanto por la conciliación
> programada cada 5 minutos como por el polling del frontend, y solo se dispararía
> de forma natural cortando la red en mitad de una transacción real contra
> Wompi.

### 3.7 Criterios de aceptación
- Cada historia de usuario (HU-01 … HU-11) tiene criterios de aceptación descritos en 'Historias_de_usuario.docx' y verificados por los CP correspondientes.
- La autorización por propiedad debe devolver 403 (o 404) y nunca exponer datos ajenos.
- El doble cobro debe rechazarse de forma explícita (400).
- La suite de regresión debe cerrar en 0 fallos antes de considerar una entrega.