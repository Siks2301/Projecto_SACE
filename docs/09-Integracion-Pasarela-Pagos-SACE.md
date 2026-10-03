# 09 — Integración de pasarela de pagos (SACE / AleLeo Tours)

> Documento técnico del módulo de pagos. Describe la arquitectura, el flujo de
> cobro, las decisiones de seguridad y cómo operar el sistema en los dos
> ambientes soportados.

---

## 1. ¿Qué cambió y por qué?

La versión anterior del módulo de pagos **no tenía pasarela de pagos**. El
formulario pedía un monto, el backend lo guardaba tal cual, marcaba el pago como
`CONFIRMADO` de inmediato y generaba el comprobante PDF. En la práctica el
sistema **declaraba un cobro exitoso que nunca había ocurrido**.

Tres problemas concretos de ese diseño:

| Problema | Consecuencia |
|---|---|
| El cliente enviaba el monto y el servidor lo aceptaba sin comparar | Cualquiera podía pagar $1 por un tour de dos millones |
| El pago nacía `CONFIRMADO` | El sistema afirmaba un cobro exitoso sin preguntar a nadie |
| La cuenta de destino era una cadena fija en el código (`Bre-B @VXM301`) | La clave de cobro no era un dato de negocio sino un texto hardcodeado |

La versión nueva corrige los tres integrating una **pasarela de pagos real**.

---

## 2. Pasarela elegida: Wompi

| Criterio | Wompi | Mercado Pago | PayU | ePayco |
|---|---|---|---|---|
| Sandbox sin empresa constituida | Sí | Sí | Requiere onboarding | Sí |
| Idioma de la documentación | Español | Español | Español | Español |
| Verificación criptográfica del webhook | Checksum SHA-256 | Firma `x-signature` | Firma propia | Checksum |
| Alcance del proyecto | Colombia | Regional | Colombia | Colombia |

**Wompi** es la del Banco de Colombia. Se eligió porque el sandbox es gratuito y
no exige cámara de comercio ni RUT (eso es lo que descarta a PayU para un
proyecto académico), y porque su documentación está en español.

**Referencia técnica:** [docs.wompi.co](https://docs.wompi.co) — secciones
*Ambientes y llaves*, *Transacciones*, *Eventos* y *Widget & Checkout Web*.

---

## 3. Arquitectura del flujo

```
┌────────────────────┐         ┌──────────────────────────┐        ┌──────────────┐
│  Frontend          │  1      │  SACE Backend            │  2     │   Wompi      │
│  (JavaScript)      │────────▶│                          │───────▶│  (sandbox)   │
│                    │ cotiza  │  Calcula el monto desde  │ crea    │              │
│  Muestra el monto  │         │  el catálogo de servicios│ transac │              │
│  bloqueado         │         │                          │         │              │
│                    │         │  Guarda el pago como     │         │              │
│  Elige el MÉTODO   │  3      │  PENDIENTE               │  4      │              │
│  (nunca el monto)  │────────▶│                          │◀───────│              │
│                    │ inicia  │  PENDIENTE ──▶ APROBADO  │ webhook │              │
│  Sondea el estado  │         │                      │   APPROVED              │
│  cada 2,5 s        │  5      │  Genera PDF + correo    │         └──────────────┘
└────────────────────┘◀────────│  Resuelve la solicitud  │
        polling de respaldo    └──────────────────────────┘
```

### Los cinco pasos, en detalle

1. **`GET /api/pagos/cotizacion/{id}`** — El cliente pregunta cuánto vale la
   reserva. El backend responde con el precio del catálogo. El formulario lo
   muestra **bloqueado**.
2. **`POST /api/pagos`** — El cliente envía **solo** el id de la solicitud y el
   método de pago. El backend vuelve a calcular el precio (nunca confía en el
   frontend), guarda la orden como `PENDIENTE` **antes** de llamar a la
   pasarela, y luego crea la transacción.
3. **`GET /api/pagos/estado/{referencia}`** — El navegador sondea el estado.
   El backend consulta a Wompi y responde.
4. **`POST /api/pagos/webhook/wompi`** — La pasarela notifica el cambio de
   estado. El backend verifica la firma criptográfica y, si es válida, refleja
   el estado.
5. **Conciliación periódica** — Cada 5 minutos una tarea reconsulta los pagos
   que siguen `PENDIENTE` y cierra los que la pasarela ya resolvió.

### ¿Por qué el polling va contra el backend y no contra Wompi?

La documentación de Wompi es explícita: el navegador **no puede** consultar la
API de transacciones directamente, y aunque pudiera, no tiene las credenciales
para firmar la petición. Además, centralizar por el backend trae dos ventajas que
se explican bien en una sustentación:

- Si el **webhook se perdió**, el estado local dice `PENDIENTE` pero la pasarela
  ya sabe que aprobó. Como el polling reconsulta, el usuario igual ve su pago
  confirmado.
- **La clave privada nunca sale del servidor.**

---

## 4. Modelo de datos

```sql
CREATE TABLE pago (
    id_pago                 BIGSERIAL PRIMARY KEY,
    id_solicitud            BIGINT NOT NULL REFERENCES solicitud(id),
    id_cliente              BIGINT NOT NULL REFERENCES cliente(id),

    monto                   NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    moneda                  VARCHAR(3)   NOT NULL DEFAULT 'COP',

    estado                  VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado IN
                                   ('PENDIENTE','APROBADO','RECHAZADO','ERROR','ANULADO')),
    metodo_pago             VARCHAR(30)  NOT NULL
                            CHECK (metodo_pago IN ('PSE','TARJETA','NEQUI')),

    referencia              VARCHAR(64)  NOT NULL UNIQUE,
    pasarela                VARCHAR(30)  NOT NULL DEFAULT 'WOMPI',
    id_transaccion_pasarela VARCHAR(64)  UNIQUE,
    codigo_autorizacion     VARCHAR(30),

    fecha_creacion          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_aprobacion        TIMESTAMP,
    url_pdf                 VARCHAR(255),
    correo_notificado       BOOLEAN      NOT NULL DEFAULT FALSE,
    notas                   VARCHAR(500)
);

-- REGLA DE NEGOCIO IMPUESTA POR LA BASE DE DATOS
CREATE UNIQUE INDEX uq_pago_aprobado_por_solicitud
    ON pago(id_solicitud)
    WHERE estado = 'APROBADO';
```

### El índice único parcial

Es la pieza más importante del modelo. En PostgreSQL un índice único con
`WHERE` solo evalúa esa condición, así que:

- permite **todos** los pagos `PENDIENTE` que haga falta;
- pero deja pasar **solo el primero** que llega a `APROBADO`.

**Por qué importa:** el código Java ya consultaba si había un pago confirmado
antes de insertar, pero entre esa consulta (`SELECT`) y el `INSERT` hay una
ventana de tiempo. Dos peticiones simultáneas —un doble clic, dos pestañas— podían
pasar ambas la validación y cobrar dos veces. **Con el índice, esa ventana
desaparece**: la garantía la impone la base, que es donde no hay carrera posible.

Es un argumento fuerte para la sustentación: *la regla de negocio no se
implementa solo en el código, sino en la capa que no puede sufrir condiciones de
carrera*.

### Máquina de estados

```
                    ┌──────────────▶ APROBADO    (la pasarela confirmó el cobro)
                    │
   (se crea) ──▶ PENDIENTE ──┬──▶ RECHAZADO   (la pasarela rechazó el cobro)
                    │       ├──▶ ERROR      (fallo técnico; se puede reintentar)
                    │       └──▶ ANULADO    (cancelado)
                    │
                    └── desde un estado final NO hay salida
```

La regla la implementa `PagoEstado.puedeTransicionarA()`:

- Solo se sale de `PENDIENTE`.
- Un pago `APROBADO` no vuelve a `PENDIENTE` ni se reprocesa.
- Aplicar un evento repetido **no rompe nada** (las pasarelas reenvían avisos
  por diseño): si el estado ya es final, se ignora.

### Idempotencia: la referencia

Cada pago tiene una `referencia` única con formato `PAGO-XXXXX` (alfabeto sin
caracteres ambiguos: sin `0/O` ni `1/I/L`, para que un asesor pueda dictarla por
teléfono).

Cumple dos funciones:

1. **Ante la pasarela** es el identificador de la transacción.
2. **Ante SACE** es la clave de idempotencia. Si el navegador reintenta o el
   usuario hace doble clic, el sistema reconoce que ya existe una orden para esa
   solicitud.

---

## 5. Seguridad

### 5.1 El monto no lo elige el cliente

Es el cambio de fondo y el más importante. El DTO de entrada **no tiene campo
`monto`**:

```java
public static class CrearPagoDTO {
    private Long solicitudId;
    private String metodoPago;   // lo único que el usuario decide
    private String notas;
}
```

No es que el servidor descarte el monto al llegar: **no existe dónde almacenarlo**.
Y si alguien lo envía de todos modos, el deserializador lanza un error explícito
en lugar de ignorarlo en silencio, dejando rastro del intento en el log:

```java
@JsonAnySetter
public void registrarCampoDesconocido(String clave, Object valor) {
    if ("monto".equalsIgnoreCase(clave)) {
        throw new IllegalArgumentException(
            "El monto no lo puede elegir el cliente: se calcula con el precio del servicio.");
    }
}
```

El precio sale de `Solicitud.servicioGenerado.precio`. Si no hay servicio con
precio, **el pago se rechaza**: es preferible un mensaje claro a inventar un cero
o permitir que alguien pague cualquier cantidad.

### 5.2 El webhook es público, pero autenticado

Parece contradictorio: la pasarela no tiene cuenta de usuario en SACE, así que el
endpoint no puede exigir token. La seguridad se traslada por completo al cuerpo
del mensaje:

```
checksum = SHA256( valores firmados + timestamp + secreto de eventos )
```

El backend **recalcula** ese hash y lo compara con el que trae el evento. Tres
defensas encadenadas:

1. **Firma inválida** → `401`, no se aplica ningún cambio.
2. **Referencia desconocida** → se ignora el evento.
3. **Transición ilegal** → la máquina de estados la rechaza.

Sin esto, cualquiera que unknowingly la URL podría "aprobar" un pago, que es
equivalente a regalar viajes.

**El detalle que importa:** la lista de campos firmados **no está en el código**;
se copia de `signature.properties`, que llega dentro del propio evento. Si
estuviera hardcodeada, un atacante podría alterar el monto o el estado del evento
sin que cambiara el checksum.

### 5.3 PCI-DSS: SACE nunca ve la tarjeta

El número de tarjeta, el CVV y la fecha de vencimiento se digitan **dentro del
checkout de la pasarela**. A SACE solo llega un identificador opaco. Gracias a
eso el proyecto queda fuera del alcance de PCI-DSS, que es exactamente como
funcionan los sistemas reales.

### 5.4 Dinero en enteros

Wompi cobra en **centavos enteros** (`amount_in_cents`). El motivo no es capricho:
los números de coma flotante no representan bien el dinero —`0.1 + 0.2` no es
`0.3`— y una diferencia de un peso en cada venta se acumula. El redondeo se hace
**una sola vez**, al calcular el monto.

### 5.5 La ruta pública es la más estrecha posible

```java
private static final String[] RUTAS_PUBLICAS = {
    "/api/pagos/webhook/**"
};
```

Solo la ruta del webhook queda fuera del interceptor de autenticación. Una
exclusión amplia (por ejemplo, todo `/api/pagos`) sería un agujero disfrazado de
funcionalidad.

---

## 6. Modos de operación

El módulo tiene dos modos, seleccionados con `app.pagos.pasarela`.

### Modo `mock` (por defecto)

Pasarela simulada, sin internet y sin llaves. Reproduce el comportamiento
observable de una real: nace `PENDIENTE` y se resuelve tras un retardo.

```properties
app.pagos.pasarela=mock
app.pagos.mock.retardo-ms=3000
app.pagos.mock.resultado=APROBADO
```

**Por qué existe:** en una sustentación no se puede depender de que el aula tenga
internet. Si la demo se cae porque falló el wifi del salón, el jurado se queda
sin ver el módulo de pagos.

El retardo de 3 segundos no es un adorno: es lo que hace visible el estado
*"Procesando el pago..."* en pantalla, que es la parte más interesante de la demo.

`app.pagos.mock.resultado` permite ensayar también el camino del **rechazo**
sin tocar una línea de código.

### Modo `wompi` (sandbox real)

```properties
app.pagos.pasarela=wompi
app.pagos.wompi.base-url=https://sandbox.wompi.co/v1
app.pagos.wompi.clave-publica=pub_test_...
app.pagos.wompi.clave-integridad=test_integrity_...
app.pagos.wompi.secreto-eventos=test_events_...
```

Las llaves se leen del **entorno**, nunca del archivo de configuración (que
viaja en el repositorio):

```powershell
$env:PAGOS_PASARELA     = "wompi"
$env:WOMPI_PUB_KEY      = "pub_test_..."
$env:WOMPI_PRV_KEY      = "prv_test_..."
$env:WOMPI_INTEGRITY_KEY= "test_integrity_..."
$env:WOMPI_EVENTS_SECRET= "test_events_..."
```

### Plan B para el día de la sustentación

1. Levantar el backend en modo `mock`. La demo funciona sin conexión.
2. Si hay internet y tiempo, cambiar a `wompi` y mostrar el flujo real.
3. Tener a la mano un `ngrok` por si se quiere demostrar el webhook en vivo.

---

## 7. Endpoints

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `GET` | `/api/pagos/configuracion` | Pasarela activa y métodos habilitados | Sí |
| `GET` | `/api/pagos/cotizacion/{idSolicitud}` | Precio que SACE cobra | Sí |
| `POST` | `/api/pagos` | Inicia un cobro | Sí |
| `GET` | `/api/pagos/estado/{referencia}` | Estado del pago (reconcilia con la pasarela) | Sí |
| `GET` | `/api/pagos/comprobante/{idPago}` | Comprobante PDF | Sí + propietario |
| `GET` | `/api/pagos/solicitud/{idSolicitud}` | Pagos de una solicitud | Sí + propietario |
| `POST` | `/api/pagos/webhook/wompi` | Notificación de la pasarela | **Firma SHA-256** |
| `POST` | `/api/pagos/webhook/verificacion` | Verificación de vida del endpoint | No |

### Cuerpo de `POST /api/pagos`

**Entrada** (lo único que envía el cliente):

```json
{
  "solicitudId": 42,
  "metodoPago": "TARJETA",
  "notas": "pago con tarjeta terminada en 4242"
}
```

**Salida**:

```json
{
  "idPago": 17,
  "referencia": "PAGO-K3M7Q",
  "solicitudId": 42,
  "monto": 2450000.00,
  "moneda": "COP",
  "metodoPago": "TARJETA",
  "metodoPagoEtiqueta": "Tarjeta de credito o debito",
  "estado": "PENDIENTE",
  "estadoEtiqueta": "Pendiente",
  "pasarela": "WOMPI",
  "comprobanteDisponible": false,
  "urlCheckout": null
}
```

> **`estado: PENDIENTE` no significa error.** Significa que la orden se creó y la
> pasarela todavía no respondió. El comprobante solo se puede descargar cuando
> `comprobanteDisponible` es `true`, es decir, cuando el estado es `APROBADO`.

---

## 8. Migración de datos

Si la base ya existe, hay que ejecutar **`migracion_pagos_wompi.sql` una vez**:

```powershell
psql -U postgres -d SACE_db -f src/main/resources/migracion_pagos_wompi.sql
```

Qué hace:

1. Agrega las columnas nuevas (nulables a propósito, para no fallar si hay
   filas históricas).
2. Reinterpreta `fecha_pago`: la pasa a `fecha_aprobacion` y crea
   `fecha_creacion`.
3. Traduce `estado = 'CONFIRMADO'` → `'APROBADO'` (eran cobros reales).
4. Traduce los métodos: `TRANSFERENCIA` y `BRE-B` → `'PSE'`.
5. Genera la `referencia` de los pagos heredados.
6. **Elimina `llave_destino`** (guardando antes copia en
   `pago_llave_destino_backup` y dentro de `notas`).
7. **Resuelve los cobros duplicados que ya existían** (ver más abajo).
8. Endurece las restricciones y crea el índice único parcial.
9. Crea la vista de compatibilidad `pago_legacy`.

El script es idempotente: se puede ejecutar más de una vez sin romper nada.

### 8.1 El paso 7 y por qué no es opcional

Este es el paso que hace que la migración falle si se lo quita, y su motivo es
la mejor evidencia que hay del problema que se estaba corrigiendo.

**Los datos de la base demostraban el doble cobro.** Al ejecutar la migración,
la solicitud 21 tenía **cuatro** pagos `CONFIRMADO` por $320.000, $320.000,
$320.000 y $150.000, todos al mismo cliente y con menos de media hora de
diferencia. Como el paso 3 los convierte a todos en `APROBADO`, el índice único
parcial del paso 8 ya no se podía crear y el script abortaba — sin cambiar
nada, por estar dentro de una transacción.

Eso es exactamente la ventana de carrera que tenía el modelo anterior:
consultaba si la solicitud tenía un pago confirmado y, si no lo tenía,
insertaba. Entre ese `SELECT` y el `INSERT` caben dos clics. La base de datos
no tenía nada que impedirlo.

**Qué se hizo con los duplicados:** se conserva el más antiguo (menor
`id_pago`) como `APROBADO` y los otros pasan a `ANULADO`, con el motivo escrito
en `notas`. No se borran, porque borrar destruiría evidencia —incluidos los
comprobantes PDF ya emitidos— y alteraría los totales históricos sin dejar
rastro.

```
 pago 5  solicitud 21  $320.000  APROBADO   <- el cobro real, conserva el PDF
 pago 6  solicitud 21  $320.000  ANULADO    [ANULADO EN MIGRACION: cobro duplicado...]
 pago 7  solicitud 21  $320.000  ANULADO    [ANULADO EN MIGRACION: cobro duplicado...]
 pago 8  solicitud 21  $150.000  ANULADO    [ANULADO EN MIGRACION: cobro duplicado...]
```

> **Nota sobre los montos históricos:** los importes guardados no coinciden con
> el precio de catálogo de los servicios (hay pagos de $1.000 y de $2.025,98) y
> la mayoría de las solicitudes antiguas ni siquiera tiene servicio vinculado.
> Son residuos de las pruebas manuales del modelo anterior. La migración **no
> los corrige**: inventar importes históricos sería fabricar datos financieros.
> Si se quiere una base limpia para la sustentación, lo correcto es regenerar
> las solicitudes de prueba, no reescribir la tabla `pago`.

**Verificación posterior:**

```sql
SELECT estado, COUNT(*) FROM pago GROUP BY estado;
-- Debe verse APROBADO y ANULADO. Ningún 'CONFIRMADO'.

SELECT COUNT(*) FROM pago WHERE referencia IS NULL;
-- Debe ser 0.

SELECT id_solicitud FROM pago WHERE estado = 'APROBADO'
GROUP BY id_solicitud HAVING COUNT(*) > 1;
-- No debe devolver filas.
```

> Si la base tiene datos de prueba y se puede reiniciar, es más simple usar
> `schema_pagos.sql` desde cero.

---

## 9. Pruebas manuales sugeridas

| # | Prueba | Resultado esperado |
|---|---|---|
| 1 | Abrir el modal de pago | El monto aparece **bloqueado**, con el valor del servidor |
| 2 | Enviar `{"monto": 1, "solicitudId": X, "metodoPago": "TARJETA"}` con curl | `400` — *"El monto no lo puede elegir el cliente"* |
| 3 | Pagar normalmente (modo `mock`) | Pantalla "Procesando…" → aprobado → descarga el PDF |
| 4 | Pagar dos veces la misma solicitud | `400` — ya tiene un pago aprobado |
| 5 | Cambiar `app.pagos.mock.resultado=RECHAZADO` y pagar | Mensaje de rechazo, **sin** comprobante |
| 6 | Enviar un evento con checksum falso a `/webhook/wompi` | `401`, sin cambios en la base |
| 7 | Detener la app, pagar en `mock`, no resolver, arrancar | La conciliación cierra el pago solo |
| 8 | Con `wompi` en sandbox, usar la tarjeta de prueba `4242…` | `APPROVED` + webhook recibido |

---

## 10. Estructura del código

```
Pagos/
├── Pasarela/
│   ├── PasarelaPagos.java          Interfaz del contrato (patrón Strategy)
│   ├── WompiPasarela.java          Implementación HTTP contra la API
│   ├── MockPasarela.java           Implementación simulada (sin red)
│   ├── WompiDTOs.java              Payloads JSON de la pasarela
│   ├── WompiFirmaServicio.java     SHA-256: firma de integridad y checksum
│   ├── PasarelaPagosException.java Rechozo definitivo vs. fallo técnico
│   └── PagosConfig.java            Selección de pasarela y métodos habilitados
├── TareaConciliacionPagos.java     Reconciliación periódica
Modelo/
├── PagoEstado.java                 Enum + reglas de transición
└── MetodoPago.java                 Enum PSE / TARJETA / NEQUI
Controladores/
├── PagoController.java             Endpoints del usuario autenticado
└── WebhookPagoController.java      Endpoint público con verificación de firma
```

**El patrón Strategy es lo que hace el módulo cambiable.** `PagoServicio` inyecta
la *interfaz* `PasarelaPagos`, así que no sabe si del otro lado hay una llamada
HTTP o una simulación en memoria. Cambiar de pasarela es cambiar una línea de
configuración.