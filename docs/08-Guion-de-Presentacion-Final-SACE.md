# Guion de presentación final — SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 8. Guion de presentación final — SACE (AleLeo Tours)
### 8.1 Propósito del documento
Este documento es la guía oficial de la sustentación final del proyecto SACE ante el jurado. Define el orden de exposición, el reparto por integrante, qué proyectar en cada punto de la rúbrica, el guion de la demostración en vivo y las respuestas preparadas a las preguntas más probables. Su objetivo es que la sustentación sea fluida, demostrativa y cubra explícitamente los nueve criterios de evaluación.

### 8.2 Formato y tiempos sugeridos
Duración total recomendada: de 15 a 20 minutos, más ronda de preguntas. Si el tiempo asignado es menor, compactar la sección 8.4 (desarrollo) a 3 minutos y el diseño a 2.

| # | Bloque de la sustentación | Quién presenta | Tiempo |
|---|---|---|---|
| 1 | Apertura: equipo, empresa y problema | Cesar | 1 min |
| 2 | Análisis y requerimientos (criterio 1) | Yerson | 2 min |
| 3 | Diseño del sistema (criterio 2) | Felipe | 2.5 min |
| 4 | Desarrollo y funcionalidad + demostración (criterio 3) | Cesar y Felipe | 4 min |
| 5 | Base de datos y normalización (criterio 4) | Yerson | 1.5 min |
| 6 | Control de versiones y colaboración (criterio 5) | Felipe | 1 min |
| 7 | Gestión del proyecto (criterio 6) | Yerson | 1.5 min |
| 8 | Documentación (criterio 7) | Felipe | 1 min |
| 9 | Preparación para pruebas (criterio 8) | Cesar | 1 min |
| 10 | Preparación para el despliegue (criterio 9) | Cesar | 1.5 min |
| 11 | Cierre y preguntas del jurado | Todos | 2 min |

### 8.3 Orden de exposición: qué enseñar en cada punto
#### Apertura — Cesar (1 min)
- Saludo: «Somos Cesar, Yerson y Felipe, aprendices de Análisis y Desarrollo de Software, ficha 3171149-B».
- Presentar la empresa de contexto: AleLeo Tours (On Vacation), agencia de viajes.
- Plantear el problema en una frase: «las solicitudes de los clientes llegan por canales dispersos — chat del sitio, correo y teléfono —, sin trazabilidad ni control», y proponer SACE como la solución.
- Proyectar el diagrama de arquitectura como mapa de lo que van a ver.
#### 1. Análisis y requerimientos — Yerson (2 min) — criterio 1 (15 %)
- Proyectar 01-Análisis: problema, objetivos general y específicos, y alcance (qué hace y qué no hace el sistema).
- Definir los usuarios/roles: cliente, empleado (asesor de soporte) y administrador.
- Nombrar los requerimientos funcionales RF-01…RF-14 (registro, login, destinos, solicitudes, chat, chatbot, pagos, reportes) y los no funcionales RNF-01…RNF-08 (seguridad, rendimiento, usabilidad, disponibilidad).
- Cerrar con: «el análisis deriva 11 historias de usuario (HU-01…HU-11) que planificamos en sprints» (enlaza con el criterio 6).
#### 2. Diseño del sistema — Felipe (2.5 min) — criterio 2 (15 %)
- Proyectar en este orden: diagrama de casos de uso (qué puede hacer cada rol) → diagrama de clases (estructura del sistema) → diagrama de arquitectura (cómo se conectan frontend, backend y base de datos) → MER y modelo relacional (los datos).
- Mostrar el diseño de base de datos: 11 tablas, herencia persona→cliente/empleado, tablas puente para las relaciones muchos a muchos.
- Mostrar el diseño de interfaces con las 12 capturas (pasar 6 u 8: login, home admin, gestión de solicitudes, reportes, destinos, mis solicitudes con el chatbot).
- Cierre: «todo esto se implementó y lo van a ver funcionando en la demostración».
#### 3. Desarrollo y funcionalidad + demostración — Cesar y Felipe (4 min) — criterio 3 (25 %)
- Cesar: arquitectura del backend (Java 17, Spring Boot 3.2): capa de controladores, servicios y repositorios; API REST documentada en API-Endpoints-SACE.md.
- Cesar: seguridad — token firmado HMAC-SHA256 (24 h), contraseñas cifradas (BCrypt), interceptor que protege /api/** y autorización por propiedad (IDOR → 403); manejo de errores: 400/401/403/404/409 con respuestas claras.
- Cesar: pagos — monto tomado de la base de datos, llave Bre-B @VXM301, anti-doble-cobro y comprobante PDF con sello SHA-256 e IVA 19 %.
- Felipe: frontend — HTML/CSS/JavaScript + Bootstrap 5.3.3, sin frameworks; páginas por rol, navegación central js/core/app.js, inyección del token en cada petición y cierre de sesión ante 401.
- Demostración en vivo (guion detallado en 8.5).
#### 4. Base de datos — Yerson (1.5 min) — criterio 4 (10 %)
- Proyectar: MER → modelo relacional → DDL (SACE_db_ddl.sql) → 07 Modelo de datos y normalización.
- Explicar la integridad: claves primarias por identidad, claves foráneas, restricciones CHECK (estados, prioridades, tipos) y tipos numéricos precisos para dinero (numeric(12,2)).
- Normalización: 1FN (atributos atómicos y tablas puente), 2FN (sin dependencias parciales) y 3FN (catálogos separados: servicio, pregunta_frecuente, chatbot).
- Aclarar la correspondencia RF ↔ tablas (p. ej. RF-11/12 pagos → tabla pago; RF-08 chatbot → chatbot/pregunta_frecuente) y que la BD se crea sola con ddl-auto=update + DataInitializer.
#### 5. Control de versiones y colaboración — Felipe (1 min) — criterio 5 (10 %)
- Abrir GitHub en vivo: repositorio público Siks2301/Projecto_SACE, rama main, merge --no-ff de ramas de funcionalidad.
- Mostrar el historial de commits con autoría de cada integrante: Felipe (interfaz y capturas), Yerson (modelo de datos y normalización), Cesar (backend, API y despliegue).
- Mencionar el .gitignore y que la base de datos NO se sube (solo el DDL).
#### 6. Gestión del proyecto — Yerson (1.5 min) — criterio 6 (10 %)
- Proyectar 02-Backlog: backlog priorizado, 11 historias de usuario, tareas T1…T8.
- Mostrar la planificación por sprints S1…S5 (qué se entregó en cada uno) y el seguimiento/cumplimiento (métricas y estado real al cierre).
- Distribución del equipo: Cesar (backend/seguridad/pagos/PDF), Yerson (base de datos/modelos), Felipe (frontend).
#### 7. Documentación — Felipe (1 min) — criterio 7 (5 %)
- README: requisitos, instalación paso a paso, tecnologías (PostgreSQL 18, Java 17, Spring Boot, Maven), configuración (variables de entorno) y evidencias.
- docs/01…08 (versiones .md de las evidencias) y el 05 Manual de usuario por perfil (cliente, empleado, administrador).
- INDICE_EVIDENCIAS.md: mapeo de los nueve criterios de la rúbrica con sus evidencias (autoevaluación).
#### 8. Preparación para pruebas — Cesar (1 min) — criterio 8 (5 %)
- Proyectar 03-Plan de Pruebas: 14 casos (CP-01…CP-14), datos de prueba y criterios de aceptación.
- Resultado ejecutado: 62 pasos PASS / 0 FAIL, cubriendo funcionalidad, seguridad (token, 403), pagos y reportería.
#### 9. Preparación para el despliegue — Cesar (1.5 min) — criterio 9 (5 %)
- Proyectar 04-Plan de Despliegue: configuración real, dependencias (PostgreSQL 18, JDK 17, Maven) y variables de entorno; arquitectura de despliegue local.
- Proyectar 06-Comprobante de Despliegue con la verificación real: servicios escuchando en 8082 (API) y 8091 (frontend), GET /api/servicios → 200, login admin con token y las 3 capturas del despliegue.
- Si el ambiente lo permite, repetir en vivo: encender el backend, ver el log y consultar un recurso.
#### Cierre — todos (2 min)
- Resumen en una diapositiva: los nueve criterios de la rúbrica con su evidencia (tabla de autoevaluación).
- «Quedamos atentos a sus preguntas»; cada integrante responde las de su módulo.
### 8.4 Reparto de responsabilidades por integrante
| Integrante | Módulo que domina | Habla en | Responde preguntas de |
|---|---|---|---|
| Cesar Leonardo Ramírez Montejo | Backend, seguridad, pagos/PDF, pruebas y despliegue | Apertura, desarrollo (1.ª parte), pruebas, despliegue, cierre | Token/seguridad, pagos Bre-B, comprobante, 62/62 pruebas, despliegue |
| Yerson Alexei Torres Garcia | Base de datos y modelos | Análisis, base de datos, gestión | Problema/alcance/requerimientos, MER/relacional/DDL, normalización, backlog y sprints |
| Felipe Gonzales Quitero | Frontend e interfaces | Diseño, desarrollo (demo), control de versiones, documentación | Interfaz/capturas, chatbot, GitHub/commits, README/manual |

### 8.5 Guion de la demostración en vivo (5 minutos)
#### Antes de empezar
- Encender el backend (Java 17 + Maven): esperar el log «Started … on port 8082» (la base de datos se crea sola).
- Encender el frontend en el puerto 8091 y abrir http://localhost:8091 en modo incógnito.
- Tener a la mano las credenciales: admin@aleleotours.com / admin123 y carlos.captura@ejemplo.com / DemoClave2026!a, y la llave Bre-B @VXM301.
#### Secuencia
- Paso 1 (Felipe): home público → catálogo de destinos (consulta real al backend: GET /api/servicios) y búsqueda.
- Paso 2 (Felipe): login del cliente de demostración → menú de cliente.
- Paso 3 (Felipe): crear una solicitud (p. ej. categoría CONSULTA o RESERVA) y verla en «Mis solicitudes».
- Paso 4 (Felipe): usar el chatbot AleLeoBot con una pregunta frecuente y una consulta del catálogo.
- Paso 5 (Cesar): enviar un mensaje en el hilo de la solicitud.
- Paso 6 (Cesar): pagar la solicitud con Bre-B → descargar el comprobante PDF (sello SHA-256 + IVA 19 %) e intentar pagar de nuevo para evidenciar el anti-doble-cobro (mensaje 400).
- Paso 7 (Cesar): cerrar sesión; login del administrador → gestión de solicitudes (cambiar estado a EN_PROCESO/RESUELTA), reportes y KPIs, y exportación del reporte PDF.
- Paso 8 (Cesar): mostrar el manejo de errores (p. ej. token inválido → 401; acceso a recurso ajeno → 403).
#### Plan B si falla la conexión o el demo
- Proyectar las 15 capturas previamente tomadas (12 de interfaz + 3 del despliegue), indexadas en INDICE-CAPTURAS.md, y el comprobante de despliegue con la salida real de salud HTTP.
### 8.6 Preguntas probables del jurado y respuestas preparadas
| Pregunta | Respuesta sugerida |
|---|---|
| ¿Cómo evitan que un cliente pague dos veces la misma solicitud? | El pago referencia id_solicitud; antes de procesar se valida que no exista un pago activo/reciente para esa solicitud (anti-doble-cobro) y el monto se toma de la base de datos, no del cliente. |
| ¿“Pago con Bre-B” es un pago real? | Es un flujo académico simulado: el monto lo calcula el backend, la llave es Bre-B @VXM301 y se genera un comprobante PDF con sello de integridad SHA-256 e IVA 19 %. No hay pasarela real ni facturación DIAN (fuera del alcance). |
| ¿Un cliente puede ver solicitudes de otro cliente? | No. El interceptor de seguridad valida la propiedad del recurso con el token (autorización por propiedad) y devuelve 403 ante un intento (se protege contra IDOR). |
| ¿Qué pasa si el token vence o no se envía? | El interceptor rechaza con 401; el frontend detecta el 401, limpia la sesión y redirige al login para volver a autenticarse. |
| ¿La base de datos no se entrega? ¿Cómo van a ver el sistema? | El backend crea el esquema automáticamente (ddl-auto=update) y DataInitializer siembra los datos iniciales (admin, catálogo, FAQs). Entregamos el DDL completo, el diccionario de datos y las evidencias de normalización. |
| ¿Por qué la BD está normalizada y hasta qué forma? | Hasta 3FN: 1FN con atributos atómicos y tablas puente para las M:N; 2FN sin dependencias parciales; 3FN con catálogos separados (servicio, pregunta_frecuente, chatbot) referenciados por FK. |
| ¿El chatbot contesta de verdad? | Sí: AleLeoBot responde con las preguntas frecuentes y el catálogo de destinos consultados por API; si no resuelve, deriva la solicitud al equipo humano. |
| ¿Cuáles son los roles y cómo se controlan? | Cliente, empleado y administrador; el tipo de usuario viaja en el token firmado HMAC-SHA256 (24 h) y las contraseñas se almacenan cifradas (BCrypt). |
| ¿Quién hizo qué? (trabajo colaborativo) | Trabajo por módulos: Cesar (backend, seguridad, pagos/PDF, pruebas y despliegue), Yerson (base de datos y modelos), Felipe (frontend e interfaces), con commits de autoría por integrante en el repositorio. |
| ¿Qué pasa si falla el envío del correo de notificación? | Es fail-safe: el proceso no se bloquea; la notificación se reintenta y el estado del pago/solicitud queda registrado en la base de datos. |

### 8.7 Consejos para una presentación perfecta
- Ensayar la demo completa al menos dos veces con las credenciales reales y cronometrando; nadie lee diapositivas.
- Hablar en primera persona del plural («nosotros diseñamos…») y con confianza: cada integrante expone su módulo.
- Usar frases de enlace entre bloques: «Ahora Yerson les mostrará cómo está diseñada la base de datos que soporta todo esto».
- Proyectar en pantalla completa (F11); las tablas con letra pequeña se explican ampliándolas, no se leen.
- Explicar lo técnico con una analogía cuando haga falta (el interceptor es «el portero que revisa la identificación en cada puerta»).
- Ser honestos en el alcance: el pago es un flujo académico simulado (no hay pasarela real ni DIAN); la BD no se entrega porque se autogenera.
- Cada integrante responde las preguntas de su módulo; si no saben, derivar sin inventar («eso lo maneja X, lo desarrolló…»); tener a mano los documentos para mostrar la evidencia.
- Cerrar con el resumen de los 9 criterios cumplidos y agradecer al jurado por su tiempo.