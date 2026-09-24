# Guion de presentación final — SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 8. Guion de presentación final — SACE (AleLeo Tours)

### 8.1 Propósito del documento

Este documento es la guía oficial de la sustentación final del proyecto SACE ante el jurado. **La sustentación se apoya en navegar el repositorio público en GitHub en vivo**: casi todas las evidencias del proyecto están versionadas ahí (código, documentación, diagramas, capturas, DDL e índices), por lo que la exposición recorre el repositorio explicando cada criterio con la evidencia abierta en pantalla. Define el orden de exposición, el reparto por integrante, qué proyectar en GitHub en cada punto de la rúbrica, el guion de la demostración en vivo y las respuestas preparadas a las preguntas más probables. Su objetivo es que la sustentación sea fluida, demostrativa y cubra explícitamente los nueve criterios de evaluación.

**Repositorio público:** <https://github.com/Siks2301/Projecto_SACE>

---

### 8.2 Formato y tiempos sugeridos

Duración total recomendada: de 15 a 20 minutos, más ronda de preguntas. Si el tiempo asignado es menor, compactar la sección 8.4 (desarrollo) a 3 minutos y el diseño a 2.

| # | Bloque de la sustentación | Quién presenta | Tiempo | Qué se abre en GitHub |
|---|---|---|---|---|
| 1 | Apertura: equipo, empresa y problema | Cesar | 1 min | Raíz del repo (README) |
| 2 | Análisis y requerimientos (criterio 1) | Yerson | 2 min | `docs/01-Analisis…` |
| 3 | Diseño del sistema (criterio 2) | Felipe | 2.5 min | `docs/diagramas/` + `docs/capturas/` |
| 4 | Desarrollo y funcionalidad + demostración (criterio 3) | Cesar y Felipe | 4 min | `backend-SACE/…` + `frontend-aleleo-tours/` + `API-Endpoints` |
| 5 | Base de datos y normalización (criterio 4) | Yerson | 1.5 min | `docs/SACE_db_ddl.sql` + `docs/07-…` |
| 6 | Control de versiones y colaboración (criterio 5) | Felipe | 1 min | Pestaña *Commits* y *Ramas* del repo |
| 7 | Gestión del proyecto (criterio 6) | Yerson | 1.5 min | `docs/02-Backlog…` |
| 8 | Documentación (criterio 7) | Felipe | 1 min | README + `docs/05-…` + `docs/INDICE_EVIDENCIAS.md` |
| 9 | Preparación para pruebas (criterio 8) | Cesar | 1 min | `docs/03-Plan-de-Pruebas…` |
| 10 | Preparación para el despliegue (criterio 9) | Cesar | 1.5 min | `docs/04-…` + `docs/06-…` |
| 11 | Cierre y preguntas del jurado | Todos | 2 min | `docs/INDICE_EVIDENCIAS.md` |

---

### 8.3 Orden de exposición: qué enseñar en cada punto (todo en GitHub)

#### Apertura — Cesar (1 min)

- Saludo: «Somos Cesar, Yerson y Felipe, aprendices de Análisis y Desarrollo de Software, ficha 3171149-B».
- Presentar la empresa de contexto: AleLeo Tours (On Vacation), agencia de viajes.
- Plantear el problema en una frase: «las solicitudes de los clientes llegan por canales dispersos — chat del sitio, correo y teléfono —, sin trazabilidad ni control», y proponer SACE como la solución.
- **En GitHub:** abrir la raíz del repositorio `Siks2301/Projecto_SACE`. El README es la portada: muestra las tres grandes carpetas (`backend-SACE`, `frontend-aleleo-tours`, `docs`) y decir «esto es lo que les vamos a mostrar: la API, el sitio y toda la evidencia del proyecto».

#### 1. Análisis y requerimientos — Yerson (2 min) — criterio 1 (15 %)

- **En GitHub:** `docs/01-Analisis-y-Requerimientos-SACE.md` (abrirlo en la vista *Preview* de GitHub).
- Proyectar: problema, objetivos general y específicos, y alcance (qué hace y qué no hace el sistema).
- Definir los usuarios/roles: cliente, empleado (asesor de soporte) y administrador.
- Nombrar los requerimientos funcionales RF-01…RF-14 (registro, login, destinos, solicitudes, chat, chatbot, pagos, reportes) y los no funcionales RNF-01…RNF-08 (seguridad, rendimiento, usabilidad, disponibilidad).
- Cerrar con: «el análisis deriva 11 historias de usuario (HU-01…HU-11) que planificamos en sprints» (enlaza con el criterio 6).

#### 2. Diseño del sistema — Felipe (2.5 min) — criterio 2 (15 %)

- **En GitHub:** `docs/diagramas/` — abrir en este orden: *diagrama casos de uso* (qué puede hacer cada rol) → *diagrama de clases* (estructura del sistema) → *diagrama de arquitectura* (cómo se conectan frontend, backend y base de datos) → *modelo entidad relación* y *modelo relacional* (los datos). GitHub renderiza las imágenes PNG al hacer clic.
- Mostrar el diseño de base de datos en el *modelo relacional*: 11 tablas, herencia persona→cliente/empleado, tablas puente para las relaciones muchos a muchos.
- Mostrar el diseño de interfaces: `docs/capturas/` (pasar 6 u 8: login, home admin, gestión de solicitudes, reportes, destinos, mis solicitudes con el chatbot) usando `docs/INDICE-CAPTURAS.md` como índice.
- Cierre: «todo esto se implementó y lo van a ver funcionando en la demostración».

#### 3. Desarrollo y funcionalidad + demostración — Cesar y Felipe (4 min) — criterio 3 (25 %)

- **En GitHub (código):** `backend-SACE/src/main/java/com/mycompany/sacejpa/`
  - Cesar: arquitectura del backend (Java 17, Spring Boot 3.2) — mostrar la jerarquía de carpetas: `Controladores/` → `Servicios/` → `Repositorios/`; abrir un controlador y un servicio representativos (p. ej. `SolicitudController` y `SolicitudServicio`).
  - Cesar: API REST documentada — abrir `docs/API-Endpoints-SACE.md` (catálogo de endpoints).
  - Cesar: seguridad — abrir `Config/` (interceptor de token), `Exceptions/GlobalExceptionHandler.java` (400/401/403/404/409) y `util/` (firma HMAC-SHA256, BCrypt); autorización por propiedad (IDOR → 403).
  - Cesar: pagos — abrir `PagoServicio.java` y `src/main/resources/schema_pagos.sql`: monto desde BD, llave Bre-B @VXM301, anti-doble-cobro y comprobante PDF con sello SHA-256 e IVA 19 %.
- **En GitHub (frontend):** `frontend-aleleo-tours/`
  - Felipe: HTML/CSS/JavaScript + Bootstrap 5.3.3, sin frameworks; listar los `*.html` por rol (cliente: `index`, `destinos`, `mis-solicitudes`; empleado/admin: `panel-empleado`, `gestion-*`, `reportes`); abrir `js/core/app.js` (navegación central, inyección del token, cierre de sesión ante 401) y `js/components/chatbot.js`.
- **Cómo mostrar las funciones de la aplicación** (mapa función → demo en vivo → código en GitHub). Este mapa es la pauta para la demostración:

| Función | Cómo se muestra en la aplicación (demo en vivo) | Dónde está en GitHub (código) |
|---|---|---|
| Registro / login de cliente | `registro.html` (crear cuenta) y `login.html` (entrar con `carlos.captura@ejemplo.com`) | `AuthController.java`, `ClienteController.java`, `js/core/app.js` (`inicializarLogin`/`inicializarRegistro`) |
| Catálogo de destinos y búsqueda | En `index.html` seleccionar un destino → clic en **Buscar Plan** → `destinos.html` lista los planes filtrados | `ServicioController.java` (`GET /api/servicios`), `js/cliente/destinos.js` (`leerParametrosBusqueda`) |
| Crear y consultar solicitudes (PQR/reserva) | Login cliente → **Mis solicitudes** → nueva solicitud → aparece listada | `SolicitudController.java`, `SolicitudServicio.java`, `mis-solicitudes.html`, `js/cliente/mis-solicitudes.js` |
| Chat del hilo de la solicitud | Abrir una solicitud → escribir un mensaje en el hilo | `MensajeController.java`, `js/cliente/mis-solicitudes.js` |
| Chatbot AleLeoBot | Botón flotante → pregunta frecuente o consulta del catálogo | `ChatbotController.java`, `PreguntaFrecuenteController.java`, `js/components/chatbot.js` |
| Pago Bre-B + comprobante PDF | En la solicitud → **Pagar** → descargar el PDF (sello SHA-256 + IVA 19 %) | `PagoController.java`, `PagoServicio.java`, `src/main/resources/schema_pagos.sql` |
| Panel administrador (estados de solicitudes) | Login admin → **Gestión de solicitudes** → cambiar estado a EN_PROCESO/RESUELTA | `EmpleadoController.java`, `SolicitudController.java`, `js/admin/gestion-solicitudes.js` |
| Reportes y KPIs (solo admin) | Login admin → **Reportes** → KPIs y exportar el reporte PDF | `ReporteController.java`, `ReporteServicio.java`, `js/admin/reportes.js` |
| Seguridad: 401 y 403 | Probar con un token vencido (401) o abrir una solicitud ajena (403) | interceptor de token + `Exceptions/GlobalExceptionHandler.java` |

- Demostración en vivo (guion detallado en 8.5, con los mismos pasos del mapa).

#### 4. Base de datos — Yerson (1.5 min) — criterio 4 (10 %)

- **En GitHub:** abrir en este orden: `docs/diagramas/modelo entidad relación.png` → `modelo relacional.png` → `docs/SACE_db_ddl.sql` (estructura completa generada con `pg_dump --schema-only`) → `docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md`.
- Explicar la integridad: claves primarias por identidad, claves foráneas, restricciones CHECK (estados, prioridades, tipos) y tipos numéricos precisos para dinero (`numeric(12,2)`).
- Normalización: 1FN (atributos atómicos y tablas puente), 2FN (sin dependencias parciales) y 3FN (catálogos separados: servicio, pregunta_frecuente, chatbot).
- Aclarar la correspondencia RF ↔ tablas (p. ej. RF-11/12 pagos → tabla `pago`; RF-08 chatbot → `chatbot`/`pregunta_frecuente`) y que la BD se crea sola (`ddl-auto=update` + `DataInitializer` — referenciar `backend-SACE/src/main/java/com/mycompany/sacejpa/Config/DataInitializer.java`).

#### 5. Control de versiones y colaboración — Felipe (1 min) — criterio 5 (10 %)

- **En GitHub:** pestaña **Commits** del repositorio — mostrar el historial con la autoría de cada integrante: Felipe (interfaz y capturas), Yerson (modelo de datos y normalización), Cesar (backend, API y despliegue).
- **En GitHub:** pestaña **Ramas** o la gráfica de la red (`/network`) — rama `main` con `merge --no-ff` de ramas de funcionalidad (p. ej. `feature/documentacion`).
- Abrir `.gitignore` en la raíz y aclarar que la base de datos **no se sube** (solo el DDL en `docs/SACE_db_ddl.sql`).

#### 6. Gestión del proyecto — Yerson (1.5 min) — criterio 6 (10 %)

- **En GitHub:** `docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md`.
- Proyectar: backlog priorizado, 11 historias de usuario, tareas T1…T8.
- Mostrar la planificación por sprints S1…S5 (qué se entregó en cada uno) y el seguimiento/cumplimiento (métricas y estado real al cierre).
- Distribución del equipo: Cesar (backend/seguridad/pagos/PDF), Yerson (base de datos/modelos), Felipe (frontend) — apoyarse también en la tabla "Equipo y distribución" del README.

#### 7. Documentación — Felipe (1 min) — criterio 7 (5 %)

- **En GitHub:** volver al README de la raíz (requisitos, instalación paso a paso, tecnologías: PostgreSQL 18, Java 17, Spring Boot, Maven, configuración).
- Abrir `docs/05-Manual-de-Usuario-SACE.md` (manual por perfil: cliente, empleado, administrador).
- Abrir `docs/INDICE_EVIDENCIAS.md` — mapeo de los nueve criterios de la rúbrica con sus evidencias (autoevaluación); este índice es la "llave" de la sustentación.

#### 8. Preparación para pruebas — Cesar (1 min) — criterio 8 (5 %)

- **En GitHub:** `docs/03-Plan-de-Pruebas-SACE.md`.
- Proyectar: 14 casos (CP-01…CP-14), datos de prueba y criterios de aceptación.
- Resultado ejecutado: 62 pasos PASS / 0 FAIL, cubriendo funcionalidad, seguridad (token, 403), pagos y reportería.

#### 9. Preparación para el despliegue — Cesar (1.5 min) — criterio 9 (5 %)

- **En GitHub:** `docs/04-Plan-de-Despliegue-SACE.md` (configuración real, dependencias: PostgreSQL 18, JDK 17, Maven; variables de entorno; arquitectura de despliegue local).
- Abrir `docs/06-Comprobante-de-Despliegue-SACE.md` con la verificación real: servicios escuchando en 8082 (API) y 8091 (frontend), `GET /api/servicios` → 200, login admin con token.
- Abrir las capturas de despliegue en `docs/capturas/` (`01_Despliegue_Login.png`, `02_Despliegue_Home.png`, `03_Despliegue_Destinos.png`).
- Si el ambiente lo permite, repetir en vivo: encender el backend, ver el log y consultar un recurso.

#### Cierre — todos (2 min)

- **En GitHub:** `docs/INDICE_EVIDENCIAS.md` — recorrer la tabla de autoevaluación: los nueve criterios de la rúbrica con su evidencia.
- Resumen de una frase por módulo y «Quedamos atentos a sus preguntas»; cada integrante responde las de su módulo.

---

### 8.4 Reparto de responsabilidades por integrante

| Integrante | Módulo que domina | Habla en | Entrega de GitHub que muestra | Responde preguntas de |
|---|---|---|---|---|
| Cesar Leonardo Ramírez Montejo | Backend, seguridad, pagos/PDF, pruebas y despliegue | Apertura, desarrollo (1.ª parte), pruebas, despliegue, cierre | `backend-SACE/…`, `API-Endpoints`, `03-Plan-de-Pruebas`, `04/06-Despliegue` | Token/seguridad, pagos Bre-B, comprobante, 62/62 pruebas, despliegue |
| Yerson Alexei Torres Garcia | Base de datos y modelos | Análisis, base de datos, gestión | `01-Analisis…`, `SACE_db_ddl.sql`, `07-Modelo de Datos…`, `02-Backlog…` | Problema/alcance/requerimientos, MER/relacional/DDL, normalización, backlog y sprints |
| Felipe Gonzales Quitero | Frontend e interfaces | Diseño, desarrollo (demo), control de versiones, documentación | `frontend-aleleo-tours/`, `docs/diagramas/`, `docs/capturas/`, Commits/Ramas, README | Interfaz/capturas, chatbot, GitHub/commits, README/manual |

---

### 8.5 Guion de la demostración en vivo (5 minutos)

#### Antes de empezar

- Tener abierto en otra pestaña el repositorio **https://github.com/Siks2301/Projecto_SACE** por si hace falta (Plan B).
- Encender el backend (Java 17 + Maven): esperar el log «Started … on port 8082» (la base de datos se crea sola).
- Encender el frontend en el puerto 8091 y abrir http://localhost:8091 en modo incógnito.
- Tener a la mano las credenciales: admin@aleleotours.com / admin123 y carlos.captura@ejemplo.com / DemoClave2026!a, y la llave Bre-B @VXM301.

#### Secuencia

La secuencia sigue el mapa «Cómo mostrar las funciones» de la sección 8.3 (bloque 3): cada paso demuestra una función real de la aplicación apoyándose en la API del backend.

- Paso 1 (Felipe): home público → catálogo de destinos (consulta real al backend: `GET /api/servicios`) y búsqueda por destino.
- Paso 2 (Felipe): login del cliente de demostración → menú de cliente.
- Paso 3 (Felipe): crear una solicitud (p. ej. categoría CONSULTA o RESERVA) y verla en «Mis solicitudes».
- Paso 4 (Felipe): usar el chatbot AleLeoBot con una pregunta frecuente y una consulta del catálogo.
- Paso 5 (Cesar): enviar un mensaje en el hilo de la solicitud.
- Paso 6 (Cesar): pagar la solicitud con Bre-B → descargar el comprobante PDF (sello SHA-256 + IVA 19 %) e intentar pagar de nuevo para evidenciar el anti-doble-cobro (mensaje 400).
- Paso 7 (Cesar): cerrar sesión; login del administrador → gestión de solicitudes (cambiar estado a EN_PROCESO/RESUELTA), reportes y KPIs, y exportación del reporte PDF.
- Paso 8 (Cesar): mostrar el manejo de errores (p. ej. token inválido → 401; acceso a recurso ajeno → 403).

#### Plan B si falla la conexión, el demo o el internet

- Si el demo local falla, proyectar las capturas desde el propio repo: `docs/capturas/` (12 de interfaz + 3 del despliegue), indexadas en `docs/INDICE-CAPTURAS.md`.
- Si el internet fallara, usar las **copias locales** de las evidencias: abrir los `.md` de `docs/` y las capturas desde `docs/diagramas/` y `docs/capturas/` en el explorador local (siguen siendo las mismas evidencias del repo).

---

### 8.6 Preguntas probables del jurado y respuestas preparadas

| Pregunta | Respuesta sugerida |
|---|---|
| ¿Cómo evitan que un cliente pague dos veces la misma solicitud? | El pago referencia id_solicitud; antes de procesar se valida que no exista un pago activo/reciente para esa solicitud (anti-doble-cobro) y el monto se toma de la base de datos, no del cliente. |
| ¿"Pago con Bre-B" es un pago real? | Es un flujo académico simulado: el monto lo calcula el backend, la llave es Bre-B @VXM301 y se genera un comprobante PDF con sello de integridad SHA-256 e IVA 19 %. No hay pasarela real ni facturación DIAN (fuera del alcance). |
| ¿Un cliente puede ver solicitudes de otro cliente? | No. El interceptor de seguridad valida la propiedad del recurso con el token (autorización por propiedad) y devuelve 403 ante un intento (se protege contra IDOR). |
| ¿Qué pasa si el token vence o no se envía? | El interceptor rechaza con 401; el frontend detecta el 401, limpia la sesión y redirige al login para volver a autenticarse. |
| ¿La base de datos no se entrega? ¿Cómo van a ver el sistema? | El backend crea el esquema automáticamente (ddl-auto=update) y DataInitializer siembra los datos iniciales (admin, catálogo, FAQs). Entregamos el DDL completo (`docs/SACE_db_ddl.sql`), el diccionario de datos y las evidencias de normalización. |
| ¿Por qué la BD está normalizada y hasta qué forma? | Hasta 3FN: 1FN con atributos atómicos y tablas puente para las M:N; 2FN sin dependencias parciales; 3FN con catálogos separados (servicio, pregunta_frecuente, chatbot) referenciados por FK. |
| ¿El chatbot contesta de verdad? | Sí: AleLeoBot responde con las preguntas frecuentes y el catálogo de destinos consultados por API; si no resuelve, deriva la solicitud al equipo humano. |
| ¿Cuáles son los roles y cómo se controlan? | Cliente, empleado y administrador; el tipo de usuario viaja en el token firmado HMAC-SHA256 (24 h) y las contraseñas se almacenan cifradas (BCrypt). |
| ¿Quién hizo qué? (trabajo colaborativo) | Trabajo por módulos: Cesar (backend, seguridad, pagos/PDF, pruebas y despliegue), Yerson (base de datos y modelos), Felipe (frontend e interfaces), con commits de autoría por integrante en el repositorio. |
| ¿Qué pasa si falla el envío del correo de notificación? | Es fail-safe: el proceso no se bloquea; la notificación se reintenta y el estado del pago/solicitud queda registrado en la base de datos. |
| ¿Todo lo que muestran está en GitHub? | Sí: el código completo (backend-SACE y frontend-aleleo-tours), la documentación (docs/01…08), diagramas, capturas, DDL e índices están versionados en el repositorio público; solo quedan fuera por decisión la base de datos (se autogenera) y las evidencias .docx finales, que están en la carpeta de entrega. |

---

### 8.7 Consejos para una presentación perfecta

- **Navegar GitHub como presentación:** ensayar la ruta completa de clics (raíz → docs/ → diagramas/ → capturas/ → código → Commits) al menos dos veces; saber exactamente dónde hace clic cada integrante.
- Verificar que el repositorio sea **público** y cargue rápido; abrirlo en modo incógnito para evitar sesiones/caché raras. Si el salón no tiene internet, tener copias locales listas (Plan B).
- Ensayar la demo completa al menos dos veces con las credenciales reales y cronometrando; nadie lee diapositivas.
- Hablar en primera persona del plural («nosotros diseñamos…») y con confianza: cada integrante expone su módulo.
- Usar frases de enlace entre bloques: «Ahora Yerson les mostrará cómo está diseñada la base de datos que soporta todo esto» (y él abre `docs/SACE_db_ddl.sql`).
- Proyectar en pantalla completa (F11); en GitHub, ampliar las imágenes de diagramas/capturas haciendo clic sobre ellas.
- Explicar lo técnico con una analogía cuando haga falta (el interceptor es «el portero que revisa la identificación en cada puerta»).
- Ser honestos en el alcance: el pago es un flujo académico simulado (no hay pasarela real ni DIAN); la BD no se entrega porque se autogenera.
- Cada integrante responde las preguntas de su módulo; si no saben, derivar sin inventar («eso lo maneja X, lo desarrolló…»); tener a mano la pestaña del repo para mostrar la evidencia.
- Cerrar con `docs/INDICE_EVIDENCIAS.md` (los 9 criterios con su evidencia) y agradecer al jurado por su tiempo.