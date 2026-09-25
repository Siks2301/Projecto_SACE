# Guion de presentación final — SACE (AleLeo Tours)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 8. Guion de presentación final — SACE (AleLeo Tours)

### 8.1 Propósito del documento

Este documento es la guía oficial de la sustentación final del proyecto SACE ante el jurado. La sustentación se hace **mostrando el proyecto real en las herramientas de desarrollo donde se construyó**: el **backend desde IntelliJ IDEA**, el **frontend desde Visual Studio Code** y la **base de datos desde pgAdmin**, con la aplicación funcionando en vivo. El repositorio público en GitHub complementa y respalda la exposición — documentación, diagramas, capturas, DDL y el historial de commits del control de versiones —. El guion define el orden de exposición, el reparto por integrante, qué abrir en cada herramienta en cada punto de la rúbrica, el detalle de la demostración en vivo y las respuestas preparadas a las preguntas más probables del jurado. Su objetivo es que la sustentación sea fluida, demostrativa, y que cubra de manera explícita los nueve criterios de evaluación en el tiempo asignado.

**Repositorio público:** <https://github.com/Siks2301/Projecto_SACE>

---

### 8.2 Formato y tiempos sugeridos

La duración total recomendada es de 15 a 20 minutos, más la ronda de preguntas del jurado. Cada bloque tiene un responsable y un tiempo definido en la tabla siguiente; si el tiempo asignado por el instructor fuera menor, se recomienda compactar la sección 8.4 (desarrollo y funcionalidad) a 3 minutos y la etapa de diseño a 2 minutos.

| # | Bloque de la sustentación | Quién presenta | Tiempo | Qué se abre / herramienta |
|---|---|---|---|---|
| 1 | Apertura: equipo, empresa y problema | Cesar | 1 min | GitHub: raíz del repo (README) |
| 2 | Análisis y requerimientos (criterio 1) | Yerson | 2 min | GitHub: `docs/01-Analisis…` |
| 3 | Diseño del sistema (criterio 2) | Felipe | 2.5 min | GitHub: `docs/diagramas/` + `docs/capturas/` |
| 4 | Desarrollo y funcionalidad + demostración (criterio 3) | Cesar y Felipe | 4 min | **IntelliJ**: `backend-SACE/…` + **VS Code**: `frontend-aleleo-tours/` + demo en vivo |
| 5 | Base de datos y normalización (criterio 4) | Yerson | 1.5 min | **pgAdmin**: BD `SACE_db` + GitHub: `docs/07-…` |
| 6 | Control de versiones y colaboración (criterio 5) | Felipe | 1 min | GitHub: pestaña *Commits* y *Ramas* del repo |
| 7 | Gestión del proyecto (criterio 6) | Yerson | 1.5 min | GitHub: `docs/02-Backlog…` |
| 8 | Documentación (criterio 7) | Felipe | 1 min | GitHub: README + `docs/05-…` + `docs/INDICE_EVIDENCIAS.md` |
| 9 | Preparación para pruebas (criterio 8) | Cesar | 1 min | GitHub: `docs/03-Plan-de-Pruebas…` |
| 10 | Preparación para el despliegue (criterio 9) | Cesar | 1.5 min | GitHub: `docs/04-…` + `docs/06-…` + verificación en vivo |
| 11 | Cierre y preguntas del jurado | Todos | 2 min | GitHub: `docs/INDICE_EVIDENCIAS.md` |

---

### 8.3 Orden de exposición: qué enseñar en cada punto (backend en IntelliJ, frontend en VS Code, BD en pgAdmin, evidencias en GitHub)

#### Apertura — Cesar (1 min)

Para abrir la sustentación, Cesar saluda al jurado: «Somos Cesar, Yerson y Felipe, aprendices de Análisis y Desarrollo de Software, ficha 3171149-B». A continuación presenta la empresa de contexto: **AleLeo Tours**, una agencia de viajes inspirada en On Vacation, para la cual se desarrolló el sistema. Luego plantea el problema en una sola frase: «las solicitudes de los clientes llegan por canales dispersos — el chat del sitio, el correo y el teléfono —, sin trazabilidad ni control», y presenta **SACE** como la solución de atención y gestión de esas solicitudes.

**En GitHub:** abrir la raíz del repositorio `Siks2301/Projecto_SACE` y proyectar la pantalla del README. El README hace las veces de portada: muestra las tres grandes carpetas del proyecto (`backend-SACE`, `frontend-aleleo-tours` y `docs`). Cesar cierra la apertura presentando las herramientas: «vamos a enseñarles el sistema funcionando y el código en las mismas herramientas donde lo desarrollamos: el backend en IntelliJ, el frontend en Visual Studio Code y la base de datos en pgAdmin». Esta aclaración da coherencia a todo lo que viene después: cuando cada bloque diga *«en IntelliJ»*, *«en VS Code»* o *«en pgAdmin»*, Cesar, Yerson o Felipe alternan la ventana correspondiente.

---

#### 1. Análisis y requerimientos — Yerson (2 min) — criterio 1 (15 %)

**En GitHub:** Yerson abre el documento `docs/01-Analisis-y-Requerimientos-SACE.md` y lo proyecta en la vista *Preview* de GitHub, para que se vea formateado igual que un documento.

En este bloque explica, apoyándose en el documento, el problema identificado, los objetivos general y específicos del proyecto y el alcance (qué hace el sistema y qué no hace). Luego define los usuarios y roles que participan en el sistema: el **cliente**, que consulta el catálogo y crea solicitudes; el **empleado**, que cumple el rol de asesor de soporte; y el **administrador**, que gestiona solicitudes, clientes, empleados, servicios y reportes.

Después nombra los requerimientos funcionales RF-01…RF-14 (registro, login, catálogo de destinos, solicitudes, chat, chatbot, pagos y reportes, entre otros) y los requerimientos no funcionales RNF-01…RNF-08 (seguridad, rendimiento, usabilidad y disponibilidad). Cierra este bloque con una frase que además enlaza con el criterio 6: «el análisis deriva 11 historias de usuario, HU-01…HU-11, que planificamos y entregamos en sprints».

---

#### 2. Diseño del sistema — Felipe (2.5 min) — criterio 2 (15 %)

**En GitHub:** Felipe abre la carpeta `docs/diagramas/` y proyecta las imágenes en este orden: primero el **diagrama de casos de uso**, que muestra qué puede hacer cada rol; después el **diagrama de clases**, que muestra la estructura del sistema; luego el **diagrama de arquitectura**, que explica cómo se conectan el frontend, el backend y la base de datos; y por último el **modelo entidad relación** y el **modelo relacional**, que muestran cómo se organizan los datos. En GitHub basta hacer clic sobre cada imagen para ampliarla.

Felipe explica el diseño de la base de datos apoyándose en el modelo relacional: 11 tablas, la herencia de persona → cliente/empleado y las tablas puente que resuelven las relaciones muchos a muchos. Para el diseño de interfaces abre la carpeta `docs/capturas/` y pasa 6 u 8 capturas representativas (login, home del administrador, gestión de solicitudes, reportes, destinos y mis solicitudes con el chatbot), guiándose por el índice que aparece en `docs/INDICE-CAPTURAS.md`. Cierra diciendo: «todo esto se implementó y lo van a ver funcionando en la demostración».

---

#### 3. Desarrollo y funcionalidad + demostración — Cesar y Felipe (4 min) — criterio 3 (25 %)

Este es el bloque más extenso y el que muestra la aplicación de verdad. Se divide en tres momentos: el backend, el frontend y el mapa de funciones para la demostración.

**Backend (Cesar).** Tiene abierto el proyecto `backend-SACE` en **IntelliJ IDEA**. Muestra el árbol de proyecto (`src/main/java/com/mycompany/sacejpa/`) y explica la arquitectura del backend: Java 17 con Spring Boot 3.2, organizado en capas de `Controladores/` → `Servicios/` → `Repositorios/`. En IntelliJ es muy natural recorrer esa estructura en el árbol del proyecto y saltar entre capas con «Ir a implementación» / Ctrl+clic. Abre un controlador y un servicio representativos, por ejemplo `SolicitudController.java` y `SolicitudServicio.java`, para mostrar cómo se atiende una petición. Después abre `docs/API-Endpoints-SACE.md`, el catálogo de endpoints que documenta toda la API REST. Luego muestra la parte de seguridad: la carpeta `Config/` con el interceptor del token, el manejo de errores en `Exceptions/GlobalExceptionHandler.java` (que responde 400, 401, 403, 404 y 409 con mensajes claros) y la utilidad de firma HMAC-SHA256 y cifrado BCrypt; explica ahí la autorización por propiedad, que devuelve 403 ante accesos a recursos ajenos (protección contra IDOR). Por último muestra los pagos: `PagoServicio.java` y `src/main/resources/schema_pagos.sql`, explicando que el monto se toma de la base de datos, que se usa la llave Bre-B @VXM301 y que se genera un comprobante PDF con sello de integridad SHA-256 e IVA 19 %, además del anti-doble-cobro. Se aprovecha la pestaña **Run** de IntelliJ (o la consola) para mostrar el backend en ejecución: «Started … on port 8082».

**Frontend (Felipe).** Tiene abierta la carpeta `frontend-aleleo-tours/` en **Visual Studio Code** y explica que el sitio está hecho con HTML, CSS y JavaScript puro con Bootstrap 5.3.3, sin frameworks. Recorre los archivos `.html` por rol: para el cliente están `index`, `destinos` y `mis-solicitudes`; para el empleado y el administrador están `panel-empleado`, `gestion-clientes`, `gestion-empleados`, `gestion-servicios`, `gestion-solicitudes` y `reportes`. Después abre `js/core/app.js`, que centraliza la navegación, inyecta el token en cada petición y cierra la sesión cuando el servidor responde 401, y `js/components/chatbot.js`, que implementa el chat del widget. En VS Code puede apoyarse en la barra lateral del explorador y en el modo *Go to File* (Ctrl+P) para saltar entre archivos con fluidez.

**Cómo mostrar las funciones de la aplicación.** La siguiente tabla es el mapa que guía la demostración: para cada función indica cómo se muestra en vivo en la aplicación y dónde está su código en IntelliJ o en VS Code.

| Función | Cómo se muestra en la aplicación (demo en vivo) | Dónde está el código (IntelliJ / VS Code) |
|---|---|---|
| Registro / login de cliente | `registro.html` (crear cuenta) y `login.html` (entrar con `carlos.captura@ejemplo.com`) | IntelliJ: `AuthController.java`, `ClienteController.java`; VS Code: `js/core/app.js` (`inicializarLogin`/`inicializarRegistro`) |
| Catálogo de destinos y búsqueda | En `index.html` seleccionar un destino → clic en **Buscar Plan** → `destinos.html` lista los planes filtrados | IntelliJ: `ServicioController.java` (`GET /api/servicios`); VS Code: `js/cliente/destinos.js` (`leerParametrosBusqueda`) |
| Crear y consultar solicitudes (PQR/reserva) | Login cliente → **Mis solicitudes** → nueva solicitud → aparece listada | IntelliJ: `SolicitudController.java`, `SolicitudServicio.java`; VS Code: `mis-solicitudes.html`, `js/cliente/mis-solicitudes.js` |
| Chat del hilo de la solicitud | Abrir una solicitud → escribir un mensaje en el hilo | IntelliJ: `MensajeController.java`; VS Code: `js/cliente/mis-solicitudes.js` |
| Chatbot AleLeoBot | Botón flotante → pregunta frecuente o consulta del catálogo | IntelliJ: `ChatbotController.java`, `PreguntaFrecuenteController.java`; VS Code: `js/components/chatbot.js` |
| Pago Bre-B + comprobante PDF | En la solicitud → **Pagar** → descargar el PDF (sello SHA-256 + IVA 19 %) | IntelliJ: `PagoController.java`, `PagoServicio.java`, `src/main/resources/schema_pagos.sql` |
| Panel administrador (estados de solicitudes) | Login admin → **Gestión de solicitudes** → cambiar estado a EN_PROCESO/RESUELTA | IntelliJ: `EmpleadoController.java`, `SolicitudController.java`; VS Code: `js/admin/gestion-solicitudes.js` |
| Reportes y KPIs (solo admin) | Login admin → **Reportes** → KPIs y exportar el reporte PDF | IntelliJ: `ReporteController.java`, `ReporteServicio.java`; VS Code: `js/admin/reportes.js` |
| Seguridad: 401 y 403 | Probar con un token vencido (401) o abrir una solicitud ajena (403) | IntelliJ: interceptor de token + `Exceptions/GlobalExceptionHandler.java` |

Después de este mapa se pasa a la demostración en vivo, cuyo guion paso a paso está en la sección 8.5.

---

#### 4. Base de datos — Yerson (1.5 min) — criterio 4 (10 %)

**En pgAdmin:** Yerson abre la base de datos en **pgAdmin**, conectada a `SACE_db` (localhost:5432). Muestra el árbol de objetos: `Databases → SACE_db → Schemas → public → Tables` y recorre las tablas del sistema, señalando las principales: `cliente`, `empleado`, `solicitud`, `mensaje`, `servicio`, `pago`, `chatbot` y `pregunta_frecuente`. Hace clic en una tabla representativa (por ejemplo `solicitud` o `pago`) y muestra sus **columnas** (tipos, `numeric(12,2)` para el dinero), su **llave primaria** y sus **llaves foráneas** en la pestaña *Constraints*. Adicionalmente, abre `docs/SACE_db_ddl.sql` (la estructura completa, generada con `pg_dump --schema-only`, sin datos) en el explorador o en IntelliJ, y el documento `docs/07-Modelo-de-Datos-y-Normalizacion-SACE.md`.

Con las tablas y sus constraints en pantalla (pgAdmin), explica la integridad de los datos: claves primarias por identidad, claves foráneas que mantienen las relaciones, restricciones CHECK para los estados, prioridades y tipos, y tipos numéricos precisos para el dinero (`numeric(12,2)`). Luego explica la normalización: la base de datos está normalizada hasta 3FN — en 1FN los atributos son atómicos y existen tablas puente para las relaciones muchos a muchos; en 2FN no hay dependencias parciales; y en 3FN los catálogos están separados en tablas propias, como `servicio`, `pregunta_frecuente` y `chatbot`, que se referencian por llave foránea. Cierra aclarando la correspondencia entre los requerimientos y las tablas (por ejemplo, lo relacionado con pagos, RF-11/RF-12, vive en la tabla `pago`, y el chatbot, RF-08, usa `chatbot` y `pregunta_frecuente`), y que la base de datos se crea sola con la configuración `ddl-auto=update` más el `DataInitializer`, que referencia en IntelliJ en `backend-SACE/src/main/java/com/mycompany/sacejpa/Config/DataInitializer.java`. Como toque extra, puede ejecutar una consulta simple en el *Query Tool* de pgAdmin (por ejemplo `SELECT * FROM pago;`) para mostrar los datos reales del sistema en ejecución.

---

#### 5. Control de versiones y colaboración — Felipe (1 min) — criterio 5 (10 %)

**En GitHub:** Felipe abre la pestaña **Commits** del repositorio y muestra el historial de commits con la autoría de cada integrante: Felipe trabajó la interfaz y las capturas; Yerson, el modelo de datos y la normalización; y Cesar, el backend, la API y el despliegue. Después abre la pestaña **Ramas** (o la gráfica de red del repositorio) para mostrar la rama principal `main` con `merge --no-ff` de las ramas de funcionalidad, por ejemplo `feature/documentacion`. Para terminar, abre el archivo `.gitignore` en la raíz y aclara que la base de datos **no se sube** al repositorio: solo se adjunta el DDL de estructura en `docs/SACE_db_ddl.sql`.

---

#### 6. Gestión del proyecto — Yerson (1.5 min) — criterio 6 (10 %)

**En GitHub:** Yerson abre `docs/02-Backlog-y-Gestion-del-Proyecto-SACE.md` y proyecta el backlog priorizado con las 11 historias de usuario y las tareas T1…T8. Luego muestra la planificación por sprints S1…S5: qué se entregó en cada uno y el seguimiento y cumplimiento con las métricas y el estado real al cierre del proyecto. Para cerrar, recuerda la distribución del equipo — Cesar en backend, seguridad, pagos y PDF; Yerson en base de datos y modelos; Felipe en frontend — y se puede apoyar en la tabla «Equipo y distribución» del README para mostrar lo mismo en un solo vistazo.

---

#### 7. Documentación — Felipe (1 min) — criterio 7 (5 %)

**En GitHub:** Felipe vuelve al README de la raíz y muestra los requisitos y la instalación paso a paso: las tecnologías (PostgreSQL 18, Java 17, Spring Boot y Maven), la configuración de variables de entorno y cómo levantar el backend y el frontend. Después abre `docs/05-Manual-de-Usuario-SACE.md`, el manual de uso por perfil (cliente, empleado y administrador), y por último abre `docs/INDICE_EVIDENCIAS.md`, que mapea los nueve criterios de la rúbrica con sus evidencias; este índice es, en palabras de Felipe, la «llave» de toda la sustentación, porque demuestra con qué evidencia se cubre cada punto evaluado.

---

#### 8. Preparación para pruebas — Cesar (1 min) — criterio 8 (5 %)

**En GitHub:** Cesar abre `docs/03-Plan-de-Pruebas-SACE.md` y proyecta el plan de pruebas: los 14 casos de prueba (CP-01…CP-14), los datos de prueba y los criterios de aceptación. Luego muestra el resultado de la ejecución: **62 pasos PASS y 0 FAIL**, cubriendo funcionalidad, seguridad (token y 403 por propiedad), pagos y reportería. Si el jurado pregunta sobre alguna prueba específica, se amplía la fila correspondiente del documento.

---

#### 9. Preparación para el despliegue — Cesar (1.5 min) — criterio 9 (5 %)

**En GitHub:** Cesar abre `docs/04-Plan-de-Despliegue-SACE.md` y muestra la configuración real del despliegue: las dependencias (PostgreSQL 18, JDK 17 y Maven), las variables de entorno y la arquitectura de despliegue local del proyecto. Después abre `docs/06-Comprobante-de-Despliegue-SACE.md`, donde está la verificación real: los servicios escuchando en el puerto 8082 (la API) y en el 8091 (el frontend), el `GET /api/servicios` respondiendo 200 y el login del administrador con su token. Para terminar, abre las capturas de despliegue que están en `docs/capturas/` (`01_Despliegue_Login.png`, `02_Despliegue_Home.png` y `03_Despliegue_Destinos.png`). Si el ambiente lo permite, hace la verificación en vivo: enciende el backend, muestra el log y consulta un recurso de la API.

---

#### Cierre — todos (2 min)

**En GitHub:** se abre `docs/INDICE_EVIDENCIAS.md` y se recorre la tabla de autoevaluación con los nueve criterios de la rúbrica y su evidencia. Cada integrante hace un resumen de una frase de su módulo — Cesar sobre el backend y la seguridad, Yerson sobre la base de datos, Felipe sobre el frontend — y el equipo cierra con un «Quedamos atentos a sus preguntas». Cada integrante responde las preguntas que corresponden a su módulo.

---

### 8.4 Reparto de responsabilidades por integrante

La siguiente tabla resume quién domina cada módulo, en qué bloques habla, con qué herramienta lo muestra y qué tipo de preguntas responde. Esta distribución también es evidencia del trabajo colaborativo.

| Integrante | Módulo que domina | Habla en | Entrega / herramienta que muestra | Responde preguntas de |
|---|---|---|---|---|
| Cesar Leonardo Ramírez Montejo | Backend, seguridad, pagos/PDF, pruebas y despliegue | Apertura, desarrollo (1.ª parte), pruebas, despliegue, cierre | IntelliJ: `backend-SACE/…`, `API-Endpoints`; GitHub: `03-Plan-de-Pruebas`, `04/06-Despliegue` | Token/seguridad, pagos Bre-B, comprobante, 62/62 pruebas, despliegue |
| Yerson Alexei Torres Garcia | Base de datos y modelos | Análisis, base de datos, gestión | pgAdmin: BD `SACE_db`; GitHub: `01-Analisis…`, `SACE_db_ddl.sql`, `07-Modelo de Datos…`, `02-Backlog…` | Problema/alcance/requerimientos, MER/relacional/DDL, normalización, backlog y sprints |
| Felipe Gonzales Quitero | Frontend e interfaces | Diseño, desarrollo (demo), control de versiones, documentación | VS Code: `frontend-aleleo-tours/`; GitHub: `docs/diagramas/`, `docs/capturas/`, Commits/Ramas, README | Interfaz/capturas, chatbot, GitHub/commits, README/manual |

---

### 8.5 Guion de la demostración en vivo (5 minutos)

#### Antes de empezar

Antes de iniciar la sustentación hay que dejar el ambiente listo para no perder tiempo en el momento del demo. La pantalla debe tener preparadas las tres herramientas con el proyecto abierto: **IntelliJ IDEA** con el proyecto `backend-SACE` (y el backend en ejecución en la pestaña *Run*, donde se espera el mensaje «Started … on port 8082», que confirma que la base de datos se creó sola), **Visual Studio Code** con la carpeta `frontend-aleleo-tours` abierta y **pgAdmin** conectado a la base `SACE_db`. El frontend se sirve en el puerto 8091 y se abre `http://localhost:8091` en una ventana de incógnito, para que la sesión empiece limpia. Conviene tener abierta en otra pestaña el repositorio `https://github.com/Siks2301/Projecto_SACE`, por si hace falta proyectar algo adicional de último minuto (además se usa como plan B). También se tienen a la mano las credenciales de demostración — `admin@aleleotours.com` / `admin123` y `carlos.captura@ejemplo.com` / `DemoClave2026!a` — y la llave Bre-B @VXM301.

#### Secuencia

La secuencia de la demostración sigue el mapa «Cómo mostrar las funciones» de la sección 8.3 (bloque 3): cada paso demuestra una función real de la aplicación y se apoya en la API del backend.

- **Paso 1 (Felipe):** desde el home público muestra el catálogo de destinos, que es una consulta real al backend (`GET /api/servicios`), y hace una búsqueda por destino con el buscador del sitio.
- **Paso 2 (Felipe):** hace login con la cuenta del cliente de demostración y muestra el menú de cliente.
- **Paso 3 (Felipe):** crea una solicitud (por ejemplo, de categoría CONSULTA o RESERVA) y la ve aparecer en «Mis solicitudes».
- **Paso 4 (Felipe):** usa el chatbot AleLeoBot con una pregunta frecuente y con una consulta del catálogo, para mostrar que responde con datos reales.
- **Paso 5 (Cesar):** envía un mensaje en el hilo de la solicitud creada, mostrando el chat entre cliente y asesor.
- **Paso 6 (Cesar):** paga la solicitud con Bre-B, descarga el comprobante PDF (con sello SHA-256 e IVA 19 %) y luego intenta pagar de nuevo la misma solicitud para evidenciar el anti-doble-cobro con el mensaje 400.
- **Paso 7 (Cesar):** cierra sesión, hace login con el administrador y muestra la gestión de solicitudes (cambia el estado a EN_PROCESO y luego a RESUELTA), los reportes y KPIs, y la exportación del reporte PDF.
- **Paso 8 (Cesar):** muestra el manejo de errores: con un token inválido la API responde 401, y al intentar acceder a un recurso de otro cliente la API responde 403.

#### Plan B si falla la conexión, el demo o el internet

Si la demostración local fallara, las capturas ya tomadas en el propio repositorio permiten avanzar igual: se proyectan desde `docs/capturas/` (12 de interfaz y 3 del despliegue), que están indexadas en `docs/INDICE-CAPTURAS.md`. Si el problema es el internet y GitHub no carga, se usan las **copias locales** de las evidencias: los `.md` de la carpeta `docs/` (se abren en IntelliJ o VS Code con vista previa Markdown, o en el explorador de archivos) y las imágenes de `docs/diagramas/` y `docs/capturas/`, que son las mismas evidencias que están versionadas en el repositorio. En cualquier caso, el código siempre se puede mostrar desde las herramientas locales (IntelliJ y VS Code) sin depender de internet.

---

### 8.6 Preguntas probables del jurado y respuestas preparadas

La siguiente tabla agrupa las preguntas que con más frecuencia hace el jurado, junto con la respuesta sugerida para cada una. Las respuestas son breves, técnicas y siempre apuntan a la evidencia, mostrándola en la herramienta correspondiente (IntelliJ, VS Code, pgAdmin) o en GitHub.

| Pregunta | Respuesta sugerida |
|---|---|
| ¿Cómo evitan que un cliente pague dos veces la misma solicitud? | El pago referencia `id_solicitud`; antes de procesarlo se valida que no exista un pago activo o reciente para esa solicitud (anti-doble-cobro) y el monto se toma de la base de datos, no del cliente. |
| ¿"Pago con Bre-B" es un pago real? | Es un flujo académico simulado: el monto lo calcula el backend, la llave es Bre-B @VXM301 y se genera un comprobante PDF con sello de integridad SHA-256 e IVA 19 %. No hay pasarela real ni facturación DIAN, porque eso está fuera del alcance académico. |
| ¿Un cliente puede ver solicitudes de otro cliente? | No. El interceptor de seguridad valida la propiedad del recurso con el token (autorización por propiedad) y devuelve 403 ante cualquier intento, lo que protege contra ataques IDOR. |
| ¿Qué pasa si el token vence o no se envía? | El interceptor rechaza la petición con 401; el frontend detecta el 401, limpia la sesión y redirige al login para volver a autenticarse. |
| ¿La base de datos no se entrega? ¿Cómo van a ver el sistema? | El backend crea el esquema automáticamente con `ddl-auto=update` y el `DataInitializer` siembra los datos iniciales (administrador, catálogo y preguntas frecuentes). Se entrega el DDL completo en `docs/SACE_db_ddl.sql`, junto con el diccionario de datos y las evidencias de normalización. |
| ¿Por qué la BD está normalizada y hasta qué forma? | La base de datos está normalizada hasta 3FN: la 1FN se cumple con atributos atómicos y tablas puente para las relaciones M:N; la 2FN, sin dependencias parciales; y la 3FN, con catálogos separados (`servicio`, `pregunta_frecuente`, `chatbot`) referenciados por llave foránea. |
| ¿El chatbot contesta de verdad? | Sí. AleLeoBot responde con las preguntas frecuentes y el catálogo de destinos consultados por API; si no resuelve la consulta, deriva la solicitud al equipo humano. |
| ¿Cuáles son los roles y cómo se controlan? | Cliente, empleado y administrador. El tipo de usuario viaja en el token firmado con HMAC-SHA256 (vigencia de 24 horas) y las contraseñas se almacenan cifradas con BCrypt. |
| ¿Quién hizo qué? (trabajo colaborativo) | Trabajamos por módulos: Cesar en backend, seguridad, pagos/PDF, pruebas y despliegue; Yerson en base de datos y modelos; Felipe en frontend e interfaces. Cada aporte está respaldado por commits con autoría en el repositorio. |
| ¿Qué pasa si falla el envío del correo de notificación? | Es fail-safe: el proceso no se bloquea, la notificación se reintenta y el estado del pago o de la solicitud queda registrado en la base de datos de todos modos. |
| ¿Todo lo que muestran está en GitHub? | Sí: el código completo (backend-SACE y frontend-aleleo-tours), la documentación (docs/01…08), los diagramas, las capturas, el DDL y los índices están versionados en el repositorio público. En la sustentación ese mismo código se muestra desde las herramientas de desarrollo — el backend en IntelliJ IDEA, el frontend en Visual Studio Code y la base de datos en pgAdmin —, y GitHub queda como respaldo y para el control de versiones. Solamente quedan fuera, por decisión, la base de datos (porque se autogenera) y los archivos `.docx` finales, que están en la carpeta de entrega. |

---

### 8.7 Consejos para una presentación perfecta

- **Ensayar la ruta completa en las tres herramientas:** practicar al menos dos veces la secuencia de clics en IntelliJ (árbol de proyecto, pestaña Run), en VS Code (explorador de archivos, Ctrl+P) y en pgAdmin (árbol de objetos, Query Tool), además de la ruta en GitHub (raíz → docs/ → diagramas/ → capturas/ → Commits), hasta saber exactamente qué abre cada integrante y en qué orden.
- Verificar que el repositorio sea **público** y que cargue rápido; abrirlo en modo incógnito para evitar sesiones o caché que cambien lo que se proyecta. Si el salón no tiene internet, tener listas las copias locales (plan B) y las herramientas ya abiertas.
- Ensayar la demo completa al menos dos veces con las credenciales reales y cronometrando; en la sustentación nadie debe leer las diapositivas ni los documentos, sino mostrar y explicar.
- Hablar en primera persona del plural («nosotros diseñamos…») y con seguridad: cada integrante expone su módulo y no interrumpe al compañero.
- Usar frases de enlace entre bloques para que la presentación fluya, por ejemplo: «Ahora Yerson les mostrará cómo está diseñada la base de datos que soporta todo esto» (y restar protagonismo a la pantalla abriendo pgAdmin sobre la base `SACE_db`).
- Proyectar en pantalla completa (F11); en las herramientas, agrandar la letra del código cuando haga falta (en VS Code *Ctrl + +* y en IntelliJ *Ctrl +.*; en pgAdmin ampliar el panel de columnas o usar el Query Tool). En GitHub, ampliar las imágenes de diagramas y capturas haciendo clic sobre ellas, en lugar de acercar la cámara o agrandar la letra.
- Explicar lo técnico con una analogía cuando haga falta: el interceptor del token es «el portero que revisa la identificación en cada puerta»; la normalización es «organizar un closet para que cada cosa tenga su lugar».
- Ser honestos en el alcance: el pago es un flujo académico simulado (no hay pasarela real ni DIAN) y la base de datos no se entrega porque se autogenera.
- Cada integrante responde las preguntas de su módulo; si no sabe algo, deriva sin inventar: «eso lo maneja X, que lo desarrolló», y se apoya en el repositorio para mostrar la evidencia.
- Cerrar siempre con `docs/INDICE_EVIDENCIAS.md` (los 9 criterios con su evidencia), agradecer al jurado por su tiempo y quedar atentos a sus preguntas.