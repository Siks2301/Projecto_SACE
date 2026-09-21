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
- Pagos: llave 'Bre-B @VXM301', montos reales del catálogo y reintentos para validar anti-doble-cobro.
- Casos negativos: credenciales erróneas, solicitudes ajenas, mensajes en solicitudes canceladas, sin sesión.
### 3.5 Casos de prueba
| ID | Caso de prueba | Resultado esperado |
|---|---|---|
| CP-01 | Registro de cliente (nombre/apellido opcional, correo duplicado, sin nombre) | 201 con apellido vacío legible; 400 si falta nombre; 409 si el correo existe |
| CP-02 | Inicio de sesión correcto e incorrecto; rol detectado | 2xx + token; 401 con credenciales malas |
| CP-03 | Lectura/escritura de solicitudes ajenas (IDOR) | 403 / 404; nunca 200 |
| CP-04 | Cancelación por el propietario y por un tercero | 200 + estado CANCELADA; 403 para el tercero |
| CP-05 | Mensajes: escribir y leer en hilo propio y ajeno | 200 en propio; 403 en ajeno; 400 si la solicitud está CANCELADA |
| CP-06 | Pago único y doble cobro | 200 el primero; 400 el segundo (ValidacionException explícita) |
| CP-07 | Pago con monto inventado o sin servicio real | Modal sin apertura o rechazo; nunca montos fabricados (frontend) |
| CP-08 | Comprobante PDF: descarga del propietario y de un tercero | 200 + application/pdf para el dueño; 403 para el tercero |
| CP-09 | Solicitud RESUELTA con pago: etiquetas y botones | Badge 'Pago Confirmado' + descargar; sin botón Pagar |
| CP-10 | Cancelación de solicitud inexistente y PUT rechazado | Aviso claro al usuario; se muestra el motivo real del servidor; sin DELETE de clientes |
| CP-11 | Reportes y KPIs (admin vs cliente) | 200 para admin; 403 para cliente |
| CP-12 | Saneamiento de datos del API en plantillas (XSS) | Los valores con HTML se escapan; no se ejecutan |
| CP-13 | Chatbot: catálogo desde API con respaldo local | El precio citado proviene de /api/servicios; respaldos solo si la API no responde |
| CP-14 | Sintaxis y compilación | node --check sin errores; mvn compile sin errores |

### 3.6 Reporte de ejecución
Ejecutado el 21 de septiembre de 2026 con el backend real y la base de datos local:

| Suite | Checks | Resultado |
|---|---|---|
| test_regression.ps1 (auth, solicitudes, cancelar, mensajes, pagos+PDF, reportes, saneos) | 37 | PASS |
| test_fixes.ps1 (IDOR, doble cobro, apellido opcional, precios) | 12 | PASS |
| test_frontend_fixes.js (modal, badges, estados, cancelación, XSS) | 13 | PASS |
| TOTAL | 62 | 62 PASS / 0 FAIL |

### 3.7 Criterios de aceptación
- Cada historia de usuario (HU-01 … HU-11) tiene criterios de aceptación descritos en 'Historias_de_usuario.docx' y verificados por los CP correspondientes.
- La autorización por propiedad debe devolver 403 (o 404) y nunca exponer datos ajenos.
- El doble cobro debe rechazarse de forma explícita (400).
- La suite de regresión debe cerrar en 0 fallos antes de considerar una entrega.