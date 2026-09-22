# Documento de análisis y requerimientos

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 1. Análisis de la situación y requerimientos
### 1.1 Contexto del proyecto
ON VACATION es una empresa del sector turístico que ofrece viajes y gestión de vuelos. Con el crecimiento del número de clientes aumentaron las solicitudes relacionadas con reservas, cambios de itinerario, cancelaciones y consultas sobre los servicios. La atención manual dispersa la información y dificulta el seguimiento. El proyecto, desarrollado como 'AleLeo Tours', entrega un Sistema de Atención y Gestión de Solicitudes (SACE) que centraliza el registro, la asignación, la comunicación y el seguimiento de cada solicitud, e incorpora un chatbot (AleLeoBot) para dar primera atención con preguntas frecuentes.

### 1.2 Planteamiento del problema
La principal problemática identificada es la dificultad para gestionar y realizar seguimiento eficiente a las solicitudes de los clientes. La ausencia de un sistema centralizado ocasiona que la información de las solicitudes, las interacciones con los clientes, los responsables de cada caso y los archivos relacionados se encuentren desorganizados; además, los empleados responden de forma repetitiva preguntas frecuentes que podrían automatizarse, aumentando la carga de trabajo y el tiempo de respuesta.

### 1.3 Objetivos
#### Objetivo general
Desarrollar un sistema web de gestión de solicitudes de soporte para la agencia de viajes que permita registrar, organizar, asignar, atender y dar seguimiento a los requerimientos de los clientes, automatizando la primera atención mediante un chatbot.

#### Objetivos específicos
- Implementar el registro y la autenticación de clientes, empleados y administradores con contraseñas cifradas y sesión por token.
- Permitir a los clientes crear y consultar solicitudes por categorías (RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE, OTRO) garantizando que solo el propietario las acceda.
- Proporcionar un canal de mensajería por solicitud entre cliente y asesor, con archivos adjuntos y control de propiedad.
- Implementar el chatbot AleLeoBot con preguntas frecuentes y catálogo de destinos consultado desde la API.
- Gestionar el pago de reservas con precio real de la base de datos, prevención de doble cobro y comprobante PDF con sello de integridad (SHA-256) e IVA.
- Brindar a empleados y administradores paneles de atención, CRUD de clientes/empleados/servicios/solicitudes y reportes con KPIs.
- Aplicar buenas prácticas de seguridad: autorización por propiedad, manejo global de errores HTTP y salida sin XSS.
### 1.4 Alcance del sistema
Incluye: sitio web público con catálogo y búsqueda de destinos; registro e inicio de sesión; área del cliente con sus solicitudes, cancelaciones, chat y pago; panel del asesor; paneles de administración (clientes, empleados, servicios, solicitudes, preguntas frecuentes); chatbot flotante; comprobantes de pago en PDF; reportes y KPIs.

No incluye: pasarela de pago real o facturación fiscal (DIAN), aplicación móvil nativa, multi-tenant, ni alta disponibilidad en nube pública.

### 1.5 Usuarios y roles
| Rol | Descripción | Funcionalidades principales |
|---|---|---|
| Cliente | Persona que usa los servicios turísticos y realiza solicitudes de soporte | Registro, login, crear/consultar/cancelar solicitudes, chat con asesor, pagar y descargar comprobante |
| Empleado de soporte | Asesor encargado de atender y dar seguimiento a solicitudes | Panel de atención, disponibilidad, KPIs, responder/chatear en solicitudes asignadas |
| Administrador | Configura y administra el sistema | CRUD de clientes, empleados, servicios y solicitudes; FAQs; reportes y KPIs |
| Chatbot (AleLeoBot) | Componente automático de primera atención | Responde FAQs, sugiere destinos con precio del catálogo y escala la solicitud a un asesor |

### 1.6 Requerimientos funcionales
| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-01 | Registro de clientes con nombre obligatorio, apellido opcional, correo y documento únicos y contraseña cifrada (BCrypt). | Alta |
| RF-02 | Inicio de sesión con token firmado HMAC-SHA256 (24 h) y detección del rol (cliente, empleado, administrador). | Alta |
| RF-03 | Catálogo público de servicios/destinos consultable desde la API sin autenticación. | Alta |
| RF-04 | Creación de solicitudes por categorías (RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE, OTRO) vinculadas al cliente autenticado. | Alta |
| RF-05 | Consulta del listado y detalle de las solicitudes del propio cliente (protección IDOR). | Alta |
| RF-06 | Cancelación de solicitudes por su propietario con reglas de estado y sin eliminar registros del cliente. | Alta |
| RF-07 | Chat de mensajes por solicitud entre cliente y asesor, con validación de propiedad en lectura y escritura. | Alta |
| RF-08 | Chatbot AleLeoBot con preguntas frecuentes y destinos obtenidos de la API (respaldos locales si el backend no responde). | Media |
| RF-09 | Panel del empleado: ver solicitudes asignadas, alternar disponibilidad y atender en vivo. | Media |
| RF-10 | CRUD de administración: clientes, empleados, servicios, solicitudes y preguntas frecuentes. | Media |
| RF-11 | Pago de una solicitud con monto tomado del servicio real en BD, llave 'Bre-B @VXM301', prevención de doble cobro y validación del propietario. | Alta |
| RF-12 | Comprobante PDF del pago: sello SHA-256 de folio|llave|monto, desglose con IVA (base exacta /1.19) y descarga exclusiva del propietario. | Alta |
| RF-13 | Reportes y KPIs (ventas consolidadas, conteos por categoría/estado) restringidos a administradores, con exportación PDF. | Media |
| RF-14 | Notificación por correo con tope de fallo (fail-safe): si el SMTP no está disponible, el flujo continúa sin romperse. | Baja |

### 1.7 Requerimientos no funcionales
| ID | Requerimiento | Criterio |
|---|---|---|
| RNF-01 | Seguridad | Contraseñas con BCrypt; token firmado HMAC con expiración; interceptor sobre /api/**; autorización por propiedad responde 403 y las validaciones 400; respuestas sin XSS. |
| RNF-02 | Manejo de errores | Mensajes claros y códigos HTTP coherentes (400/401/403/404/409) centralizados en un GlobalExceptionHandler. |
| RNF-03 | Rendimiento | Consultas con Spring Data JPA y paginación/compactación de listas en las respuestas del API. |
| RNF-04 | Usabilidad | Diseño responsive con Bootstrap 5.3.3, mensajes visuales (toasts) y accesibilidad básica. |
| RNF-05 | Compatibilidad | Navegadores modernos de escritorio (Chrome, Edge, Firefox). |
| RNF-06 | Mantenibilidad | Arquitectura por capas (Controlador, Servicio, Repositorio, Entidad) con DTOs, mappers y excepciones centralizadas. |
| RNF-07 | Configuración | Parámetros configurables por variables de entorno (SACE_TOKEN_SECRET, PROPIETARIO_EMAIL, SPRING_MAIL_*, etc.). |
| RNF-08 | Disponibilidad | Entorno local/desarrollo con niveles de fallo controlados; correo con fail-safe. |

### 1.8 Restricciones y supuestos
Restricción tecnológica: Java 17 + Spring Boot, PostgreSQL local y frontend en JavaScript puro (sin frameworks SPA).

Suposición: las pruebas y la ejecución se hacen en un entorno local de desarrollo (localhost).

El sistema NO está conectado a pasarelas de pago reales (Bre-B es la llave de verificación del comprobante en el flujo académico); la facturación electrónica fiscal (DIAN) queda fuera del alcance de esta fase.

El sistema NO aborda despliegues multi-tenant ni alta disponibilidad en producción: el despliegue previsto es local o servidor académico con un solo entorno.
