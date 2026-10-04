# Errores encontrados en AleLeo Tours — SACE

Medidos con inspección de DOM, pruebas reales en el navegador y consultas a la
base, no de forma teórica.
Fechas de verificación: 4 de octubre de 2026.

---

## 1. ERROR CORREGIDO HOY

### El pie del modal de pago se salía de la pantalla

**Dónde:** `frontend-aleleo-tours/mis-solicitudes.html`, modales
`modalPagoSolicitud` y `modalNuevaSolicitud`.

**Cómo se veía:** el modal crecía más que la pantalla y la parte de abajo quedaba
inalcanzable. En 1366×768 el botón "Pagar Ahora" quedaba en 789×825 px y la
pantalla acaba en 768: fuera. El pie completo entre 771 y 842. No había forma de
verlos ni de desplazarse hasta ellos.

**Por qué:** Bootstrap limita el alto de `.modal-content` y espera que
`.modal-body` sea el hijo flex que encoge y se desplaza
(`modal-dialog-scrollable`). Pero en estos dos modales el cuerpo y el pie están
envueltos en un `<form>` para poder validarlos y enviarlos, así que el hijo que
encoga es el `<form>`. En CSS el `min-height: auto` de un flex item solo se
convierte en 0 cuando su `overflow` no es `visible`; el `<form>` es un bloque con
`overflow: visible`, así que no podía reducirse y conservaba su alto natural
(cabecera 64 + cuerpo 656 + pie 71 = 791 px sobre 712 disponibles). El
`overflow: hidden` de `.modal-content` recortaba el excedente sin dejar
desplazamiento posible.

**Cómo se comprobó:** A/B sirviendo la versión con y sin el arreglo en dos
puertos. Antes: pie fuera de pantalla, 0 px desplazable. Después: pie dentro,
102 px desplazables en 1366×768. El `form` sigue siendo un `HTMLFormElement` y el
submit sigue llegando a su manejador.

**Alcance real:** exactamente 2 de los 8 formularios del proyecto. Preguntado al
DOM, no por regex: los otros 6 (`form-reserva`, `form-login`, `form-registro`,
`form-solicitud`, `form-cliente`, `form-empleado`, `form-servicio`) siguen con
`display: block`. Con los modales cerrados, los 98 elementos medidos de la página
dan exactamente las mismas medidas: el arreglo no movió nada fuera del modal.

**Estado:** corregido en `css/style.css`, commit `36e6291`, subido a `main`.

---

## 2. ERRORES CORREGIDOS EN LAS RONDAS ANTERIORES

### Interfaz pública
| # | Error | Detalle |
|---|---|---|
| 1 | Precios con desviación | La tarjeta mostraba un precio y al reservar se cobraba otro. Había tres fuentes de datos; se unificaron en `destinos-datos.js` |
| 2 | "Vuelos Incluidos" fijo | Etiqueta escrita a mano en las tarjetas, sin relación con el paquete real |
| 3 | Enlaces muertos | Guajira y Amazonas aparecían en el catálogo sin destino detrás |
| 4 | Badge "HOTEL 2X1" | promocional, no correspondía a ningún paquete del catálogo |
| 5 | Oración rota | Frase truncada en la introducción |
| 6 | 5 emojis + errata | "Breack" en lugar de "Break" |
| 7 | H2 con palabras clave | Texto optimizado para buscadores, no para leer |
| 8 | Teléfono duplicado | Dos números distintos en el pie |

### Áreas privadas (admin, empleado, cliente)
| # | Error | Detalle |
|---|---|---|
| 9 | 35 textos ilegibles | Fallos de contraste; bajados a 0 verificados en las 22 combinaciones de página × pantalla |
| 10 | Título de modal invisible | `.modal-title` con contraste **1.00** sobre su propio fondo en los 4 modales |
| 11 | 31 colores sueltos | Hexadecimales escritos a mano fuera del sistema de diseño |
| 12 | Desborde horizontal en móvil | En 5 páginas se salía de la pantalla |
| 13 | Encabezados todos del mismo nivel | Jerarquía plana en las 11 páginas |
| 14 | Llave Bre-B repetida | Aparecía en varios sitios, parecía un dato de pago expuesto en pantalla |
| 15 | Botones de acción dispares | Cuatro estilos distintos en `gestion-solicitudes` |
| 16 | Errores en consola | En 5 páginas públicas |

### Datos y branding
| # | Error | Detalle |
|---|---|---|
| 17 | **Dos administradores con el mismo correo** | `admin@onvacation.com` estaba en dos personas (ids 2 y 4). El login usa `findByEmailIgnoreCase`, así que ambos entraban a la misma cuenta |
| 18 | Residuo de plantilla | Persona id 2 guardada con subtipo `Cliente` pero rol `ADMINISTRADOR`, documento `0000000000` de relleno y 2 solicitudes que eran el mensaje por defecto del chatbot |
| 19 | **`Persona.email` sin `UNIQUE`** | Causa raíz del #17: el correo no era clave única |
| 20 | **`ddl-auto=update` no crea índices únicos** | Puesto que Hibernate no añade índices a tablas que ya existen, `unique = true` en la entidad **no protege nada** en una base ya creada. Comprobado con una prueba de fuego: el duplicado entró sin error |
| 21 | `UNIQUE` sensible a mayúsculas | Un `UNIQUE (email)` en PostgreSQL acepta `ADMIN@ALELEOTOURS.COM` junto a `admin@aleleotours.com`, que en el login colisionan igual. El índice va sobre `lower(email)` |
| 22 | Dominio de la plantilla en la BD | 4 correos `@onvacation.com` |
| 23 | Dominio de la plantilla en el código | 17 apariciones de claves `onvacation_*` en 5 archivos JS |
| 24 | Correo personal en el pie | El del desarrollador, visible para cualquier visitante |

---

## 3. CORREGIDOS EN ESTA RONDA

### 3.1 Rutas absolutas de Windows guardadas en la base — CORREGIDO

**Los 16 de 16 registros de `pago` tenían una ruta absoluta de Windows** en
`url_pdf`, por ejemplo:

```
C:\Users\cesar\OneDrive\Escritorio\backend-SACE\uploads\comprobantes\recibo_pago_3.pdf
```

**Corrección de una cifra:** no eran 4 las rutas que apuntaban a una carpeta
inexistente, sino **14 de 16**. Solo las de los pagos 3 y 19 apuntaban al
directorio del proyecto actual; las otras 14 apuntaban a
`...\Escritorio\backend-SACE\...`, la carpeta anterior a la del proyecto, que ya
no existe (comprobado con `Test-Path`: `False`).

**Qué se hizo:**

1. `PdfComprobanteServicio.generarComprobantePdf()` ya no devuelve
   `archivoSalida.getAbsolutePath()`. Guarda la ruta **relativa** con separadores
   `/`: `uploads/comprobantes/recibo_pago_3.pdf`. Así el mismo valor sirve en
   Windows, en Linux y en un servidor.
2. `PdfComprobanteServicio.resolverRutaDeComprobante()` resuelve la ruta contra
   el disco al leer. Acepta la ruta relativa nueva y también la absoluta
   heredada: si la ruta absoluta no existe pero el PDF sí, lo localiza por nombre
   dentro del directorio de la aplicación, en vez de devolver un enlace roto.
3. `PagoServicio.obtenerArchivoPdfComprobante()` y
   `EmailNotificacionServicio` usan ese resolvedor, de modo que el adjunto del
   correo tampoco depende de la máquina.
4. `RutaComprobanteInitializer` convierte a relativas las filas que quedaran con
   ruta absoluta, al arrancar. Es idempotente.
5. `migracion_rutas_comprobantes.sql` hace lo mismo a mano, para cuando no se
   quiere depender del arranque.

**Verificado:** 0 de 16 filas conservan ruta absoluta; las 16 quedan como
`uploads/comprobantes/recibo_pago_<id>.pdf`, con el nombre del archivo
correspondiente al `id_pago`.

**Nota sobre un error propio:** la primera versión de
`RutaComprobanteInitializer` comparaba la ruta ya migrada contra
`'uploads/comprobantes//%'` (doble barra, por el modo en que se componía el
`LIKE` con parámetros). Ese patrón no coincide con nada, así que cada arranque
anteponía el prefijo otra vez. Tras tres reinicios quedaron rutas como
`uploads/comprobantes/comprobantescomprobantescomprobantesrecibo_pago_1.pdf`. Se
detectó al verificar la base y se reparó reconstruyendo la columna desde
`id_pago`, que es la única fuente fiable. La condición quedó en
`normalizar() + "/%"`.

### 3.2 Comprobantes PDF versionados en git — CORREGIDO

`backend-SACE/uploads/comprobantes/` **no estaba en `.gitignore`**, así que los
recibos se commiteaban: 15 PDF de comprobantes reales de clientes dentro del
historial, con nombre, correo y monto. Son documentos personales.

**Qué se hizo:** `backend-SACE/uploads/` agregado a `.gitignore` y los 15
archivos retirados del índice con `git rm -r --cached`.

**Lo que esto NO resuelve:** los PDF siguen en el historial de git. Quien clone
el repositorio anterior los sigue recibiendo. Para sacarlos de verdad hay que
reescribir el historial (`git filter-repo` o `filter-branch`) y forzar el push, o
publicar un repositorio nuevo. **Esa reescritura no se hizo**: es una operación
destructiva e irreversible sobre la línea de tiempo compartida, y el repositorio
tiene historial útil de commits. Queda como decisión pendiente del equipo.

`recibo_pago_3.pdf` faltaba en disco (se había perdido en una operación previa de
limpieza). Restaurado desde el commit que lo contenía, 2 544 bytes, cabecera PDF
válida.

### 3.3 `schema_email_unico.sql` era opcional y no tenía por qué serlo — CORREGIDO

El script existía y documentaba bien por qué era necesario, pero seguía siendo
un paso manual: quien desplegara solo el código se quedaba sin la restricción.
Se_prefiero que la garantía no dependa de que alguien recuerde correrlo.

**Qué se hizo:** `PersonaEmailConstraintInitializer` garantiza en cada arranque,
de forma idempotente, lo mismo que el script:

- `persona_email_unico` (UNIQUE simple), que es lo que declara la entidad;
- `persona_email_unico_lower` (UNIQUE sobre `lower(email)`), que es el que de
  verdad protege el login, porque `UNIQUE (email)` en PostgreSQL distingue
  mayúsculas y `findByEmailIgnoreCase` no;
- `email` NOT NULL, cuadrando con `nullable = false`.

Antes de tocar nada, registra en el log los ids de los correos duplicados
comparando en minúsculas, que es como compara el login. **No lanza excepción si
encuentra duplicados:** impedir el arranque dejaría el sistema caído, así que lo
reporta y sigue aplicando el resto. El comentario de `Persona.java` ahora explica
que `unique = true` no basta por sí solo y por qué el índice va sobre `lower()`.

**Verificado:** arranca y registra
`PersonaEmailConstraintInitializer: unicidad de persona.email verificada
(persona_email_unico, persona_email_unico_lower)`.

### 3.4 Cambios de base de datos que no viven en git — DOCUMENTADO

Las correcciones de datos se aplicaron a la base en vivo y además quedaron como
scripts idempotentes en `backend-SACE/src/main/resources/`:
`schema_datos_corporativos.sql`, `schema_email_unico.sql`,
`migracion_rutas_comprobantes.sql` y `migracion_pagos_wompi.sql`.

**Git no puede deshacer un `UPDATE` sobre datos.** En cualquier base nueva o
restaurada hay que correrlos, en este orden:

| Orden | Script | Qué hace |
|---|---|---|
| 1 | `schema_datos_corporativos.sql` | Limpieza de datos: corrige correos de la plantilla, el residuo de la persona id 2, dominios `@onvacation.com` |
| 2 | `schema_email_unico.sql` | Unicidad de `persona.email`, con el índice sobre `lower(email)` |
| 3 | `schema_pagos.sql` | Estructura del módulo de pagos |
| 4 | `migracion_pagos_wompi.sql` | Migra pagos al esquema de la pasarela |
| 5 | `migracion_rutas_comprobantes.sql` | Rutas de comprobante a relativas |

Si se despliega solo el código, se arrastran los datos corruptos y además una
tabla `persona` sin las restricciones nuevas.

Los pasos 1, 2 y 5 ya no dependen de ejecutar el script: los inicializadores los
garantizan en cada arranque. Los pasos 3 y 4 se crean solos por
`ddl-auto=update` y `PagoConstraintInitializer`, pero el script sigue siendo la
forma de auditar qué cambió.

### 3.5 Dependencias de maquetación desde internet — CORREGIDO

Bootstrap 5.3.3, Bootstrap Icons, Chart.js y la tipografía Outfit se cargaban
desde CDN. Sin conexión a internet las páginas perdían la grilla, los modales,
los iconos, los gráficos y la tipografía del sistema de diseño.

**Qué se hizo:** se descargaron a `frontend-aleleo-tours/vendor/` y se
reescribieron las 11 páginas para que apunten a archivos locales:

| Archivo local | Origen | Versión |
|---|---|---|
| `vendor/bootstrap/css/bootstrap.min.css` | jsDelivr | 5.3.3 |
| `vendor/bootstrap/js/bootstrap.bundle.min.js` | jsDelivr | 5.3.3 |
| `vendor/bootstrap-icons/bootstrap-icons.min.css` | jsDelivr | 1.11.3 |
| `vendor/bootstrap-icons/fonts/bootstrap-icons.woff2` + `.woff` | jsDelivr | 1.11.3 |
| `vendor/fonts/outfit-latin.woff2` + `outfit-latin-ext.woff2` | Google Fonts | Outfit v15 |
| `vendor/chartjs/chart.umd.min.js` | jsDelivr | 4.5.1 |

`vendor/fonts/fonts-local.css` reproduce los `@font-face` de Google, con las
mismas rutas `unicode-range`. Se conservan los dos archivos porque Outfit es una
variable font (un archivo cubre los pesos 300 a 800, lo que usa el design
system) y porque el rango `latin` es el que cubre los caracteres del sitio.

**Extra:** la imagen del hero venía de `images.unsplash.com`. Sin internet
tampoco se veía. Se descargó a `img/hero/hero-destino.jpg` y se actualizó la
referencia en `css/style.css`.

Chart.js estaba sin versión fija (`https://cdn.jsdelivr.net/npm/chart.js`
resolvía a la última publicada, 4.5.1). Se fijó a `4.5.1` para que la página no
cambie de comportamiento entre despliegues.

**Verificado:** 0 referencias a `cdn.jsdelivr`, `fonts.googleapis`,
`fonts.gstatic`, `unpkg` o `cdnjs` en las 11 páginas, el CSS y los JS. Las únicas
URL `http` que quedan son `http://localhost:8082/api` (el backend, por diseño) y
un `data:` SVG embebido.

### 3.6 El administrador creaba clientes que no podían iniciar sesión — CORREGIDO

**Cómo se encontró:** manejando el sistema como lo haría una persona, no leyendo
el código. Se creó un cliente desde "Gestión de clientes" con la contraseña
`Prueba2026!a`, la API respondió `200 OK`, el cliente apareció en el listado con
su correo… y al intentar iniciar sesión con esa clave devolvió **401**.

**Por qué:** `ClienteDTOs` **no tenía campo `contrasenia`**.
`frontend-aleleo-tours/js/admin/gestion-clientes.js` sí la envía
(`if (pass) dto.contrasenia = pass;`) y la pantalla incluso obliga a escribirla
al crear ("La contraseña es obligatoria al crear un cliente"). Pero el controlador
recibe el cuerpo directamente como `ClienteDTOs`:

```java
@PostMapping
public ClienteDTOs InsertarCliente(@RequestBody ClienteDTOs dto) { ... }
```

Jackson descarta en silencio lo que no corresponde a un campo del DTO. El
resultado: la contraseña se perdía sin error ni aviso, el cliente quedaba creado
con `persona.contrasenia` **vacía** y jamás podía entrar. Nadie se enteraba hasta
que el cliente llamaba diciendo que su cuenta no le funcionaba.

El mismo hueco existía en el camino de actualización: editar un cliente tampoco
permitía reasignarle la clave. El lado de empleados **sí** estaba bien
(`EmpleadoDTOs` sí tiene el campo y `EmpleadoServicio` lo hashea), lo que hace el
fallo más difícil de ver: funcionaba en una pantalla y no en la otra.

**Qué se hizo** (en `ClienteDTOs.java` y `ClienteServicio.java`):

1. `ClienteDTOs` recibe `contrasenia`, **solo de entrada**.
2. `ClienteServicio.insertarCliente()` valida la fortaleza con `ContraseniaUtil` y
   guarda el hash con BCrypt, igual que `EmpleadoServicio.insertarEmpleado()`.
3. `ClienteServicio.servactualiza()` permite reasignar la clave, pero **solo si
   viene con contenido**: un `PUT` parcial no borra la contraseña existente.
4. El getter va anotado `@JsonProperty(access = WRITE_ONLY)`, para que el campo
   ni siquiera aparezca en las respuestas. Sin eso el JSON devolvía
   `"contrasenia": null`: no filtraba el hash, pero anunciaba el campo y dejaba la
   seguridad en manos de que el mapper no se equivocara.

**Verificado** contra la API y contra la base, con el cliente recién creado:

| Prueba | Resultado |
|---|---|
| Crear cliente con contraseña | `200`, id 89, `persona.contrasenia` con hash `$2a$10$…` |
| Login con esa contraseña | `200`, id 89 |
| Login con contraseña incorrecta | `401` |
| Alta con contraseña de 3 caracteres | `400` (rechazada por `ContraseniaUtil`) |
| Editar y cambiar la clave | `200`; la nueva entra, la vieja da `401` |
| `PUT` parcial sin contraseña | `200` y el login sigue funcionando (no se pierde la clave) |
| JSON de respuesta | sin el campo `contrasenia` |
| Borrar cliente | `200` |

### 3.7 Los nombres de campo equivocados se descartan en silencio — DOCUMENTADO

La causa de raíz del 3.6 es de diseño, y por eso conviene nombrarla: los
controladores usan el DTO de la entidad como cuerpo de la petición
(`@RequestBody ClienteDTOs dto`, `@RequestBody ServicioDTOs dto`). **No hay
validación de campos desconocidos**, así que una errata en el nombre se pierde
sin generar el menor error.

Comprobado en esta ronda: al crear un servicio enviando `precioBase` y
`duracionDias` (en vez de `precio` y `duracion`) la API responde `200` y crea el
servicio **con el precio en null**. Más tarde la cotización lo rechaza con
*"Esta solicitud no tiene un servicio asignado con precio, por lo que no se puede
cobrar"* — un error desconcertante, a tres pasos de distancia de la errata.

**El frontend sí usa los nombres correctos** (`precio`, `duracion`, `destino`,
`tipoServicio` en `gestion-servicios.js`), así que hoy no hay ningún dato roto por
esto. Queda anotado porque es la clase de fallo que produjo el 3.6: una pantalla
pide un campo, el backend lo ignora, y el síntoma aparece mucho más tarde y en un
sitio que nada tiene que ver con el campo mal escrito.

Alternativa para cerrarlo de verdad: configurar Jackson con
`FAIL_ON_UNKNOWN_PROPERTIES` activo, para que un nombre equivocado se rechace en
el borde, en lugar de aceptarse en silencio. No se aplicó porque cambiaría el
comportamiento de todas las peticiones del sistema y conviene hacerlo con pruebas
de contrato encima, no de madrugada.

### 3.8 Un 401 en cualquier `fetch` cierra la sesión entera — DOCUMENTADO

`js/core/app.js` envuelve a `fetch` con un parche global: si cualquier respuesta
viene con `401`, llama a `Sesion.cerrar()` y, si no está en la página de login,
redirige allí.

Se detectó porque, durante las pruebas, un intento de login con contraseña
equivocada **tumbó la sesión del navegador que estaba autenticado como
administrador**: se volvió a la pantalla de login y el `sessionStorage` quedó
vacío, aunque el token siguiera vigente. El parche tiene una salvedad
(`enLogin`) para no redirigir, pero **no** para no borrar la sesión, y esa
salvedad únicamente se aplica cuando ya se está en `login.html`.

En uso normal el síntoma es leve: un `401` casi siempre significa token vencido, y
cerrar la sesión es lo correcto. Se documenta por dos razones: porque apareció de
verdad durante las pruebas, y porque hace que cualquier prueba automática de
credenciales se enturbie con el estado de la sesión de la página.

### 4.1 Dos bases de datos con el mismo nombre en distinta caja — ALTO

PostgreSQL existe **`SACE_db`** (la real: 41 personas, 28 solicitudes) y
**`sace_db`** en minúsculas (5 personas, 3 solicitudes, `@example.com` y
`@sace.com` de aspecto de datos de demostración).

El riesgo: PostgreSQL pliega a minúsculas los identificadores sin comillas pero
**trata los nombres de base como distinguibles**. Quien escriba la URL en
minúsculas —"en un `application.properties` nuevo, en un `.env`, en pgAdmin, en
un comando"— entra sin ningún aviso a la base de demostración y no ve los datos
reales.

`application.properties` hoy apunta a `SACE_db`, la correcta. Verificado.

**No se borró la base sobrante porque no hay permiso.**

### 4.2 25 de 36 clientes son cuentas de prueba de seguridad — MEDIO

Pruebas de penetración anteriores quedaron en la base de producción, con nombres
como `victima1298@test.com` / "Vic Tima" y `atacante1298@test.com` / "Ata Cante",
además de `test.valido@test.com` y `prueba.funcional.*`.

Hoy son **25 de 36 clientes**. Ruido importante en cualquier evaluación: un
revisor que abra la base ve tresquarters de clientes que no son clientes.

**No se borraron por si son evidencia que todavía se necesita.**

### 4.3 Dos cuentas activas que nadie puede entrar — BAJO

| id | correo | estado | clave | login |
|---|---|---|---|---|
| 3 | `cliente@aleleotours.com` | ACTIVA | con hash | **401** |
| 21 | `sebastiandavidmolinarivera2380@gmail.com` | ACTIVA | con hash | **401** |

Las dos existen, están activas y tienen hash, pero la contraseña no se conoce. No
es posible recuperarlas (solo hay hash) y **no se reseteó ninguna sin permiso**.

### 4.4 Historial de git con datos personales — MEDIO

Ver 3.2. Los 15 comprobantes PDF con nombre, correo y monto de clientes siguen
en el historial del repositorio. Quitarlo del índice no los borra de ahí.

### 4.5 Basura acumulada en disco por pagos no completados — BAJO

Cada intento de pago escribe un PDF en `uploads/comprobantes/` aunque quede
`PENDIENTE`.

**No es una fuga**: la descarga al usuario está condicionada a
`comprobanteDisponible` y solo se ofrece con el pago `APROBADO`. Es acumulación
de archivos sin nada que los referencie.

Detalle actual: hay 16 comprobantes en disco y 16 pagos, sin huérfanos. Los
pagos 6, 7 y 8 están `ANULADO` y conservaron su PDF.

### 4.6 `JAVA_HOME` apunta a un JRE, no a un JDK — BAJO

A nivel de sistema `JAVA_HOME` es un JRE. Hay que sobreescribirlo a
`C:\Program Files\Java\jdk-17` en cada sesión antes de compilar con Maven.

Verificado en esta ronda: con `JAVA_HOME` apuntando al JRE del sistema, `mvn
compile` falla; con
`$env:JAVA_HOME="C:\Program Files\Java\jdk-17"` compila sin errores.

### 4.7 El puerto ya coincide — CERRADO, se deja nota

Estaba documentado que `application.properties` declaraba `8083` mientras el
backend corría en `8082`. **Hoy el archivo declara `server.port=8082`** y
`frontend-aleleo-tours/js/core/app.js` usa `http://localhost:8082/api`. Se
buscó `8083` en todo el repositorio: la única aparición está en este documento.
No queda nada que corregir.

---

## 5. PRUEBAS REALES DE FUNCIONAMIENTO (104 comprobaciones)

Después de corregir lo anterior se manejó el sistema completo en el navegador, con
el frontend en el `8091` y el backend en el `8082`, como lo haría una persona:
escribiendo en los formularios, cambiando de pestaña en el login y pulsando los
botones. **Resultado: 104 comprobaciones, 0 fallos.**

Lo que se ejercitó de verdad:

| Área | Qué se probó |
|---|---|
| Login | Las tres cuentas por el formulario, incluida la pestaña correcta de perfil; contraseña y correo incorrectos; API sin token |
| Administrador | Las 10 páginas con su sesión; alta, edición, cambio de clave, borrado y lectura de clientes, empleados y servicios; los 7 reportes, incluido el PDF; el desplegable de gestión |
| Empleado | Panel, reporte individual, cambio de estado de una solicitud, y que **no** acceda a gestión ni al reporte de otro |
| Cliente | Alta de solicitud, verla en su panel, cadena completa de cotización y pago, descarga del comprobante, botones de la interfaz |
| Permisos | 403 comprobados uno a uno: empleado y cliente bloqueados en `/reportes/*`, cliente bloqueado en `/clientes` y `/empleados`; intento de autopromoción a administrador por `PUT` rechazado |
| Anti-IDOR | Un cliente no lee la solicitud de otro (`404`) ni descarga el comprobante de otro (`403`) |
| Pagos | Cotización y pago bloqueados con un motivo claro cuando no hay servicio con precio; cotización correcta de $2.345.678 al asociar el servicio; pago `201` con referencia `PAGO-…`; consulta de estado por referencia |
| Comprobante | Descarga real: `200`, `application/pdf`, cabecera `%PDF-`, con la **ruta relativa** guardada en la base (confirma el 3.1) |
| Chatbot | Botón flotante visible, apertura, escritura y envío con Enter, respuesta real del bot, preguntas sugeridas, reinicio, cierre y que no robe los clics |
| Registro público | Formulario de `registro.html` enviado de verdad; el cliente resultante ya puede iniciar sesión |
| Maquetación | Las 11 páginas cargan con la fuente `Outfit`, Bootstrap y Chart.js locales, sin una sola petición a internet |

Además se comprobó que los gráficos de `reportes.html` se pintan: 4 `canvas` y
`window.Chart` definido, con 4 gráficos dibujados a partir de los datos reales de
la base.

Dos detalles que quedaron como **comportamiento conocido**, no como fallo:

- Al crear una solicitud, el campo `estado` viene `null` en la respuesta. No
  rompe nada porque la interfaz agrupa por estado al recargar, pero es un valor
  que debería venir informado.
- El chatbot no se cierra solo al pulsar fuera del panel. Se cierra con su
  botón, que sí funciona.

---

## 6. RESUMEN PARA LA SUSTENTACIÓN

| # | Hallazgo | Severidad | Estado |
|---|---|---|---|
| 1 | Modal de pago fuera de pantalla | ALTA | Corregido (`36e6291`) |
| 2 | Rutas absolutas de Windows en 16 de 16 comprobantes | ALTA | Corregido |
| 3 | **Clientes creados por el administrador sin contraseña usable** | **ALTA** | **Corregido** |
| 4 | Dos bases `SACE_db` / `sace_db` | ALTA | **Abierto** |
| 5 | Comprobantes PDF de clientes en git | MEDIA | Corregido en el índice; **historial abierto** |
| 6 | `unique = true` inoperante sobre base existente | ALTA | Corregido |
| 7 | Cambios de datos fuera de git | MEDIA | Documentado con orden de scripts |
| 8 | Maquetación desde CDN | MEDIA | Corregido |
| 9 | 25 de 36 clientes son pruebas de seguridad | MEDIA | **Abierto** |
| 10 | Campos con nombre equivocado se descartan en silencio | MEDIA | Documentado (ver 3.7) |
| 11 | Dos cuentas activas con contraseña desconocida | BAJA | **Abierto** |
| 12 | Archivos PDF de pago no completados sin referencia | BAJO | **Abierto** |
| 13 | `JAVA_HOME` apunta a un JRE | BAJA | **Abierto** (documentado el procedimiento) |
| 14 | Un `401` en cualquier `fetch` cierra la sesión | BAJA | Documentado (ver 3.8) |
| 15 | Puerto 8083 vs 8082 | BAJA | Cerrado: el archivo ya dice 8082 |
