# Manual de usuario — Sistema SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 5. Manual de usuario — SACE (AleLeo Tours)
### 5.1 Propósito del manual
Este manual describe el uso del Sistema de Atención y Gestión de Solicitudes de Soporte (SACE) de la agencia AleLeo Tours (ON VACATION) para los tres perfiles del sistema: cliente, empleado de soporte y administrador. Aplica a la versión entregada el 21 de septiembre de 2026.

### 5.2 Acceso al sistema y requisitos
- Navegador moderno de escritorio (Chrome, Edge o Firefox) con conexión al servidor local o académico.
- Backend (API) iniciado en el puerto 8082 y frontend servido desde su carpeta (puerto por defecto 8091).
- Si el sistema no responde, verificar que el backend esté corriendo (sección 6 de este paquete de evidencias).
### 5.3 Perfiles y credenciales de acceso
| Perfil | Cómo ingresar | Credenciales de demostración |
|---|---|---|
| Cliente | Pestaña 'Cliente' de la página de login | Registrarse con correo y contraseña propios (ej.: carlos.captura@ejemplo.com) |
| Empleado de soporte | Pestaña 'Empleado' de la página de login | Cuenta asignada por el administrador |
| Administrador | Pestaña 'Empleado' de la página de login | admin@aleleotours.com / admin123 (semilla, solo desarrollo) |

### 5.4 Iniciar sesión
En http://localhost:8091 abrir la opción 'Ingresar'. La página de login tiene dos pestañas: 'Cliente' y 'Empleado'. Se deben seleccionar según el perfil, escribir el correo y la contraseña y pulsar 'Ingresar'. Si las credenciales son incorrectas el sistema muestra el mensaje correspondiente. Al ingresar, el menú superior cambia según el rol y se redirige al área principal del perfil (solicitudes para clientes, panel para asesores, administración para el administrador).

### 5.5 Sitio público: inicio y destinos
- Inicio (index): presenta la agencia, menú de navegación, ofertas destacadas y acceso a destinos.
- Destinos: catálogo con búsqueda por texto; cada destino muestra descripción y precio.
- No se requiere sesión para consultar destinos; sí se requiere para crear solicitudes.
### 5.6 Registro de un nuevo cliente
Desde 'Registrarse' se diligencian nombre (obligatorio), apellido (opcional), correo, teléfono y contraseña. La contraseña debe incluir al menos 8 caracteres con mayúscula, minúscula, número y un carácter especial. El registro inicia sesión automáticamente; si el correo ya existe se informa el error y no se crea una cuenta duplicada.

### 5.7 Área del cliente: mis solicitudes
- Crear solicitud: seleccionar categoría (RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE u OTRO), asunto y descripción.
- Estado de la solicitud: PENDIENTE, EN_PROCESO, RESUELTA o CANCELADA; el seguimiento se ve en el mismo listado.
- Chat con el asesor: abrir la solicitud y escribir mensajes; el asesor responde en el mismo hilo.
- Cancelar: solo el propietario puede cancelar su solicitud y no se elimina el registro.
- Pagar: las solicitudes RESERVADAS habilitan el botón 'Pagar'; el monto es el precio real del servicio desde la base de datos.
- Comprobante: tras pagar se descarga un PDF con sello de integridad (SHA-256), IVA 19 % y solo lo descarga el propietario.
### 5.8 Panel del empleado de soporte
- Ver las solicitudes asignadas y su detalle, responder mediante el chat y cambiar el estado.
- Alternar disponibilidad (DISPONIBLE / NO_DISPONIBLE) según la jornada.
- Consultar KPIs propios y del equipo según el permiso.
### 5.9 Panel de administración
- Gestión de solicitudes: ver todas, filtrar por estado/categoría, reasignar y actualizar.
- Gestión de clientes, empleados y servicios: crear, editar, activar/desactivar (CRUD).
- Preguntas frecuentes: mantener el catálogo que usa el chatbot.
- Reportes y KPIs: ventas consolidadas, conteos por categoría y estado, con exportación a PDF; acceso restringido a administradores.
### 5.10 Chatbot AleLeoBot
El botón flotante con el ícono de robot abre el asistente. Responde preguntas frecuentes y sugiere destinos con el precio tomado del catálogo real de la API; si el backend no responde, usa respaldos locales para no dejar al usuario sin atención. Ante una solicitud de soporte especializada, orienta al usuario hacia el canal humano.

### 5.11 Pago en línea con pasarela de pagos
Desde la solicitud, pulsa **Pagar**. El modal muestra el precio de la reserva **bloqueado** (lo fija AleLeo Tours, no se puede modificar) y te deja elegir el método: **PSE** (transferencia bancaria) o **Tarjeta débito/crédito**. Al confirmar, la pasarela procesa el cobro; verás una pantalla de «Procesando el pago...» mientras se confirma. Cuando el pago es aprobado, se descarga automáticamente tu **Comprobante PDF** con el desglose de IVA y el sello de integridad SHA-256.

Aspectos importantes del flujo:
- **El monto lo calcula el servidor** a partir del catálogo de servicios. Aunque intentes modificarlo, el sistema cobra el precio real.
- **Nunca debes ingresar el número de tu tarjeta en AleLeo Tours.** Ese dato se pide directamente en la página segura de la pasarela; SACE no lo recibe.
- Si la pasarela rechaza el pago o se cae la conexión, **no se te cobra nada** y la reserva queda disponible para reintentar.
- Una solicitud pagada una sola vez **no se puede volver a pagar**: el sistema lo bloquea con un índice único en la base de datos.
- Si cierras la ventana antes de la confirmación, el pago queda *pendiente*; puedes volver a «Mis solicitudes» y consultar su estado.

### 5.12 Solución de problemas frecuentes
- El sistema no carga: verificar que el backend (8082) y el frontend estén iniciados y que PostgreSQL esté en ejecución.
- No puedo iniciar sesión: revisar perfil (cliente/empleado), credenciales y estado activo de la cuenta.
- No veo una solicitud: solo se muestran las solicitudes del cliente autenticado (protección de propiedad).
- El pago falla: confirmar que la solicitud sea del cliente, esté en estado que permita pagar y no haya sido pagada antes.
- El comprobante no se visualiza: descargarlo desde la solicitud RESUELTA con el botón 'Comprobante PDF' (solo propietario).
- ¿Cómo contacto soporte?: usar el chat de la solicitud con el asesor o el formulario de consulta del sitio.