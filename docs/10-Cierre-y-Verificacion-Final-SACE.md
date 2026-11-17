# 10 — Cierre y verificación final (17 de noviembre de 2026)

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_

Este documento recoge el estado final del sistema después de la prueba
end-to-end en producción, las correcciones derivadas de ella y la configuración
de las cuentas de acceso. Los documentos 03 a 09 explican el diseño; aquí se
registra **qué quedó desplegado y verificado**.

---

## 10.1 Entorno vigente

| Pieza | Plataforma | URL / dato |
|---|---|---|
| Frontend | Cloudflare Workers (assets estáticos) | `https://aleleotours.aleleo.workers.dev` |
| Backend | Render (`sace-api`, Docker, plan free, oregon) | `https://sace-api.onrender.com/api` |
| Base de datos | Neon, PostgreSQL | conexión con `?sslmode=require` |
| Pasarela de pagos | Simulada (`SIMULADA`) | `PAGOS_PASARELA=mock`, métodos `PSE` y `TARJETA` |

El despliegue es automático: cada `git push` a `main` reconstruye la imagen con
el `Dockerfile` (Maven compila desde `backend-SACE/src`). El arranque en frío
tarda aproximadamente 4–5 minutos; durante ese tiempo el servicio puede seguir
sirviendo la versión anterior, por lo que hay que esperar antes de dar por
buena una verificación.

### Commits finales

| Commit | Contenido |
|---|---|
| `34246f5` | Valores iniciales de la solicitud completados en servidor |
| `dfd51d2` | Código de autorización guardado al consultar la pasarela |
| `f053b29` | El seeder no sobrescribe claves existentes; endpoint de borrado de pagos |
| `f672bb3` | Ruta administrativa para cambiar roles; seeder apunta al administrador real |

`main` y `origin/main` quedaron alineados en `f672bb3`, con el árbol de trabajo
limpio. `mvn test`: **11/11 pruebas correctas** (`CobroPorPasajerosTest` 8,
`ValoresInicialesSolicitudTest` 3).

---

## 10.2 Cuentas de acceso en producción

| Correo | Nombre | Rol | Contraseña | Uso |
|---|---|---|---|---|
| `admin@aleleotours.com` | Felipe Gonzales | `ADMINISTRADOR` | `Empleado123$` | Panel de administración |
| `yeito203@aleleotours.com` | Yerson Torres | `ASESOR_VIAJES` | `Empleado123$` | Panel de asesor |

En el frontend ambos perfiles entran por la pestaña **Empleado**. La API espera
el campo `contrasenia` (no `password`) en `POST /api/auth/login`.

**Comportamiento del seeder a partir de `f672bb3`:**

- El correo administrador es `admin@aleleotours.com` (`app.admin.email`).
- Si la cuenta no existe, se crea con la clave de `SACE_ADMIN_PASSWORD`.
- Si ya existe, **nunca se le cambia la contraseña**. La rotación automática
  fue retirada porque sobrescribió la clave de un empleado cuando el correo
  configurado apuntaba a esa cuenta.
- Para cambiar un rol o una clave se usan los endpoints autenticados
  `PUT /api/empleados/{id}/tipo` y `PUT /api/empleados/{id}`.

> Los tokens emitidos antes de un cambio de rol conservan el rol antiguo hasta
> que vencen (24 h). Basta con volver a iniciar sesión para refrescarlos.

---

## 10.3 Correcciones aplicadas

### 10.3.1 Solicitudes: valores iniciales en servidor

Un `POST /api/solicitudes` que omitía `estado`, `fechaCreacion`, `prioridad` o
`pasajeros` guardaba `NULL`. Ahora `Solicitud.aplicarValoresIniciales()`,
invocada desde `@PrePersist`, completa:

- `estado = PENDIENTE`
- `prioridad = MEDIA`
- `fechaCreacion = fecha actual`
- `pasajeros = 1` o el número rescatado del texto (`"Pasajeros: 2"`,
  `"Reserva para 2 personas"`)

Además, `SolicitudServicio` fuerza `PENDIENTE` al crear, de modo que un cliente
no puede declarar su propia solicitud como `RESUELTA`. Casos de prueba
`CP-V1` a `CP-V3` en `ValoresInicialesSolicitudTest`.

### 10.3.2 Pagos: código de autorización persistido

En el flujo normal `PENDIENTE → APROBADO`, el comprobante decía
"Código de Autorización: No informado" aunque la pasarela sí lo devolvía.
`PagoServicio.sincronizarDatosDePasarela()` ahora guarda `codigoAutorizacion` e
`idTransaccionPasarela` en los tres caminos: consulta de estado, webhook y
conciliación. El PDF ya muestra, por ejemplo, `SIM-6814`.

### 10.3.3 Cuentas administrativas

El seeder apuntaba a `admin@aleleotours.com`, que en producción pertenecía a un
asesor, y le rotaba la clave con `SACE_ADMIN_PASSWORD`. Se corrigió el correo,
se eliminó la rotación y se invirtieron los roles: Felipe es el administrador y
Yerson es el asesor. Se añadió una ruta dedicada para cambiar roles porque el
`PUT /api/empleados/{id}` ignora `tipoUsuario` a propósito (impide que un
empleado se autoasigne privilegios).

### 10.3.4 Endpoints nuevos (solo administradores)

| Método | Ruta | Uso |
|---|---|---|
| PUT | `/api/empleados/{id}/tipo` | Cambiar `tipoUsuario` (`ADMINISTRADOR`, `ASESOR_VIAJES`, `COORDINADOR_DESTINO`, `CLIENTE`) |
| DELETE | `/api/pagos/{id}` | Limpieza administrativa de pagos de prueba |

---

## 10.4 Verificación end-to-end en producción

Ejecutada contra `https://sace-api.onrender.com/api` con una cuenta de prueba.
Resultado final: **todos los flujos críticos pasaron**.

| Verificación | Resultado |
|---|---|
| Salud del API y BD conectada | OK |
| Registro / login y rol `CLIENTE` | OK |
| Catálogo público con 6 servicios | OK |
| Solicitud sin `estado` nace `PENDIENTE`, con fecha y prioridad `MEDIA` | OK |
| Asignación automática de asesor y 2 pasajeros persistidos | OK |
| Cotización $320.000 × 2 = $640.000 | OK |
| Pago nace `PENDIENTE` y se aprueba tras el retardo de la pasarela | OK |
| Comprobante PDF con sello SHA-256, IVA y `Codigo de Autorizacion: SIM-6814` | OK |
| Segundo pago sobre la misma solicitud rechazado (400) | OK |
| Cliente no puede descargar reportes administrativos (403) | OK |
| Administrador listando empleados (200) / asesor (403) | OK |

Dos fallos del primer script eran **suposiciones del verificador**, no del
sistema: la cabecera del PDF se corrompía al convertir la respuesta con
codificación de texto en vez de escribir bytes, y el estado se comprobó antes
de que Render terminara el redespliegue. Con la descarga binaria correcta
(pypdf) y tras esperar el despliegue, ambos puntos quedaron confirmados.

### Limpieza

Se eliminaron los registros creados por las pruebas: pagos 3, 4 y 5;
solicitudes 4, 5, 6, 7 y 8; clientes 4, 5 y 6. Producción quedó con los datos
originales (1 cliente y 3 solicitudes) y sin pagos de prueba.
