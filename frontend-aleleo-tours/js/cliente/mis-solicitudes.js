/* ==========================================================================
   AleLeo Tours - mis-solicitudes.js
   Gestion de reservas, solicitudes, pagos en linea con pasarela y descarga de comprobantes PDF.
   ========================================================================== */

'use strict';

let solicitudesCliente = [];
let pagosRealizados = [];
let catalogoServicios = [];
let filtroActual = 'TODAS';
let busquedaActual = '';

let chatClienteSolicitudActiva = null;
let chatClientePollingInterval = null;
let solicitudParaPago = null;

/* --- CARGA DE SOLICITUDES Y PAGOS DEL CLIENTE --- */
async function cargarMisSolicitudes() {
  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');
  if (!sesion) {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Debes iniciar sesión para consultar tus reservas.', 'info');
    }
    setTimeout(() => { window.location.href = 'login.html'; }, 1000);
    return;
  }

  const elSaludo = document.getElementById('usuario-saludo');
  if (elSaludo) elSaludo.textContent = sesion.nombre ? sesion.nombre.split(' ')[0] : 'Viajero';

  const contenedor = document.getElementById('contenedor-solicitudes');
  if (contenedor) {
    contenedor.innerHTML = `
      <div class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-2 text-muted">Consultando tus solicitudes y pagos en el servidor...</p>
      </div>
    `;
  }

  try {
    const [respSol, respSrv] = await Promise.all([
      fetch(`${API_BASE}/solicitudes`),
      fetch(`${API_BASE}/servicios`)
    ]);

    if (respSol.ok) {
      const todas = await respSol.json();
      solicitudesCliente = todas.filter(s => Number(s.clienteId) === Number(sesion.id));
    } else {
      solicitudesCliente = [];
    }

    // Catalogo de servicios: fuente del precio real para el modal de pago.
    if (respSrv.ok) {
      const servicios = await respSrv.json();
      if (Array.isArray(servicios)) catalogoServicios = servicios;
    }
  } catch (err) {
    solicitudesCliente = [];
  }

  // Pagos APROBADOS del cliente: alimentan el badge "Pago Aprobado" y los
  // botones de comprobante (antes se adivinaba por el estado RESUELTA y se
  // mostraban comprobantes fantasma en solicitudes sin ningun pago).
  //
  // El backend ahora responde un estado de transaccion real. Se aceptan
  // 'APROBADO' y el antiguo 'CONFIRMADO' para que las solicitudes ya pagadas
  // antes de la migracion no dejen de mostrar su comprobante.
  pagosRealizados = [];
  const pagosPorSolicitud = await Promise.all(
    solicitudesCliente
      .filter(s => !s.esLocal)
      .map(s => fetch(`${API_BASE}/pagos/solicitud/${s.id}`)
        .then(r => (r.ok ? r.json() : []))
        .catch(() => []))
  );
  pagosPorSolicitud.flat()
    .filter(p => p && esPagoAprobado(p.estado))
    .forEach(p => pagosRealizados.push(p));

  // Integrar reservas locales de respaldo
  const reservasLocales = JSON.parse(localStorage.getItem('aleleo_reservas') || '[]')
    .filter(r => r.usuario === sesion.correo || r.correo === sesion.correo);

  reservasLocales.forEach(rl => {
    const yaExiste = solicitudesCliente.some(s =>
      s.titulo && rl.destino && s.titulo.toLowerCase().includes(rl.destino.toLowerCase())
    );
    if (!yaExiste) {
      solicitudesCliente.push({
        id: rl.id,
        titulo: `Reserva: ${rl.destino}`,
        asunto: `Reserva para ${rl.pasajeros} persona(s)`,
        descripcion: `Plan: ${rl.titulo || rl.destino}.\n` +
                     `Fecha de viaje: ${rl.fecha}.\n` +
                     `Pasajeros: ${rl.pasajeros}.\n` +
                     `Total estimado: $ ${Number(rl.precio || 0).toLocaleString('es-CO')} COP.\n` +
                     `Contacto: ${rl.nombre} (${rl.correo}).`,
        estado: 'PENDIENTE',
        prioridad: 'MEDIA',
        categoria: 'RESERVA',
        fechaCreacion: rl.fechaReserva || new Date().toISOString(),
        esLocal: true,
        precioEstimado: Number(rl.precio) || 0
      });
    }
  });

  solicitudesCliente.sort((a, b) => new Date(b.fechaCreacion || 0) - new Date(a.fechaCreacion || 0));

  actualizarContadores();
  renderizarSolicitudes();
}

/* --- CONTADORES --- */
function actualizarContadores() {
  const todas = solicitudesCliente.length;
  const pendientes = solicitudesCliente.filter(s => normalizarEstado(s.estado) === 'PENDIENTE').length;
  const enProceso = solicitudesCliente.filter(s => normalizarEstado(s.estado) === 'EN_PROCESO').length;
  const resueltas = solicitudesCliente.filter(s => ['RESUELTA', 'APROBADA', 'CONFIRMADA'].includes(normalizarEstado(s.estado))).length;
  const canceladas = solicitudesCliente.filter(s => ['CANCELADA', 'RECHAZADA'].includes(normalizarEstado(s.estado))).length;

  document.getElementById('count-todas').textContent = todas;
  document.getElementById('count-pendientes').textContent = pendientes;
  document.getElementById('count-proceso').textContent = enProceso;
  document.getElementById('count-resueltas').textContent = resueltas;
  document.getElementById('count-canceladas').textContent = canceladas;
}

function normalizarEstado(estado) {
  return String(estado || 'PENDIENTE').toUpperCase();
}

/* --- RENDERIZAR LISTA DE SOLICITUDES & PAGOS --- */
function renderizarSolicitudes() {
  const contenedor = document.getElementById('contenedor-solicitudes');
  if (!contenedor) return;

  let filtradas = [...solicitudesCliente];

  if (filtroActual !== 'TODAS') {
    if (filtroActual === 'RESUELTA') {
      filtradas = filtradas.filter(s => ['RESUELTA', 'APROBADA', 'CONFIRMADA'].includes(normalizarEstado(s.estado)));
    } else if (filtroActual === 'CANCELADA') {
      filtradas = filtradas.filter(s => ['CANCELADA', 'RECHAZADA'].includes(normalizarEstado(s.estado)));
    } else {
      filtradas = filtradas.filter(s => normalizarEstado(s.estado) === filtroActual);
    }
  }

  if (busquedaActual) {
    const q = busquedaActual.toLowerCase();
    filtradas = filtradas.filter(s =>
      (s.titulo && s.titulo.toLowerCase().includes(q)) ||
      (s.asunto && s.asunto.toLowerCase().includes(q)) ||
      (s.descripcion && s.descripcion.toLowerCase().includes(q)) ||
      (s.categoria && s.categoria.toLowerCase().includes(q))
    );
  }

  if (filtradas.length === 0) {
    contenedor.innerHTML = `
      <div class="card p-5 text-center border-0 shadow-sm" style="border-radius: var(--radius-lg); background: var(--ds-gray-50);">
        <i class="bi bi-calendar-x text-muted" style="font-size: 3.5rem;"></i>
        <h2 class="mt-3 text-dark">No hay reservas ni solicitudes registradas</h2>
        <p class="text-muted mb-3">Aún no tienes solicitudes en esta categoría. Puedes explorar nuestros destinos y realizar tu primera reserva.</p>
        <div>
          <a href="destinos.html" class="btn btn-primary px-4 py-2 me-2">
            <i class="bi bi-map-fill me-1"></i> Explorar Destinos
          </a>
          <button class="btn btn-outline-info px-3 py-2" onclick="document.getElementById('btn-abrir-nueva-solicitud').click();">
            <i class="bi bi-plus-circle me-1"></i> Nueva Solicitud
          </button>
        </div>
      </div>
    `;
    return;
  }

  contenedor.innerHTML = '';

  filtradas.forEach(s => {
    const estado = normalizarEstado(s.estado);
    // "Pagado" = existe un pago APROBADO real para esta solicitud; el estado
    // RESUELTA por si solo no garantiza que se haya cobrado.
    const pagoConfirmado = pagosRealizados.find(p =>
      Number(p.solicitudId) === Number(s.id) &&
      esPagoAprobado(p.estado));
    const esPagado = !!pagoConfirmado;
    const esReserva = String(s.categoria || 'RESERVA').toUpperCase() === 'RESERVA';
    const fecha = s.fechaCreacion
      ? new Date(s.fechaCreacion).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      : 'Fecha no disponible';

    const card = document.createElement('div');
    card.className = 'card-solicitud';
    card.innerHTML = `
      <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-2">
        <div>
          <span class="badge-estado badge-${estado}">
            ${formatearTextoEstado(estado, s.categoria)}
          </span>
          ${esPagado ? '<span class="badge bg-success text-white ms-2"><i class="bi bi-shield-check me-1"></i> Pago Aprobado</span>' : ''}
          <span class="text-muted ms-2 small">#SOL-${s.id}</span>
        </div>
        <div class="text-muted small">
          <i class="bi bi-clock-history me-1"></i> ${fecha}
        </div>
      </div>

      <h2 class="h5 fw-bold text-dark mb-1">
        <i class="bi bi-geo-alt-fill text-danger me-1"></i> ${escaparHtml(s.titulo || 'Solicitud sin título')}
      </h2>

      ${s.asunto ? `<div class="text-primary small fw-semibold mb-2"><i class="bi bi-tag-fill me-1"></i> ${escaparHtml(s.asunto)}</div>` : ''}

      <div class="detalles-box">
        ${escaparHtml(s.descripcion || 'Sin información adicional.')}
      </div>

      <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mt-3 pt-2 border-top">
        <div class="d-flex align-items-center gap-2 flex-wrap">
          <button class="btn btn-info text-white btn-sm px-3 fw-semibold" onclick="abrirChatCliente(${s.id})">
            <i class="bi bi-chat-dots-fill me-1"></i> Chat con Asesor
          </button>

          <!-- Botones de Pago / Comprobante PDF -->
          ${esReserva && !esPagado && !['CANCELADA', 'RECHAZADA'].includes(estado) ? `
            <button class="btn btn-warning text-dark btn-sm px-3 fw-bold shadow-sm" onclick="abrirModalPago('${s.id}')">
              <i class="bi bi-credit-card-2-front-fill me-1"></i> Pagar
            </button>
          ` : ''}

          ${esPagado ? `
            <button class="btn btn-outline-success btn-sm px-3 fw-bold" onclick="descargarComprobantePdfPorSolicitud(${s.id})">
              <i class="bi bi-file-earmark-pdf-fill me-1"></i> Descargar Comprobante PDF
            </button>
            <button class="btn btn-outline-secondary btn-sm px-2" onclick="imprimirComprobantePdfPorSolicitud(${s.id})" title="Imprimir Recibo">
              <i class="bi bi-printer-fill"></i>
            </button>
          ` : ''}

          <span class="badge bg-light text-dark border ms-1">
            <i class="bi bi-folder-fill text-warning me-1"></i> ${escaparHtml(s.categoria || 'RESERVA')}
          </span>
        </div>

        <div>
          ${['PENDIENTE', 'EN_PROCESO'].includes(estado) ? `
            <button class="btn btn-outline-danger btn-sm" onclick="cancelarSolicitud(${s.id}, ${s.esLocal || false})">
              <i class="bi bi-x-circle me-1"></i> Cancelar
            </button>
          ` : ''}
        </div>
      </div>
    `;
    contenedor.appendChild(card);
  });
}

function formatearTextoEstado(estado, categoria) {
  switch (estado) {
    case 'PENDIENTE':
      // Solo las reservas deben dinero; las demas categorias esperan atencion.
      return String(categoria || 'RESERVA').toUpperCase() === 'RESERVA'
        ? 'Pendiente de Pago / Confirmación'
        : 'Pendiente de Atención';
    case 'EN_PROCESO': return 'En Proceso por un Asesor';
    case 'RESUELTA':
    case 'APROBADA':
    case 'CONFIRMADA': return 'Resuelta / Atendida';
    case 'CANCELADA': return 'Cancelada';
    case 'RECHAZADA': return 'No Aprobada';
    default: return estado;
  }
}

function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto == null ? '' : String(texto);
  return div.innerHTML;
}

/**
 * Indica si un estado de pago corresponde a un cobro aprobado.
 *
 * <p>Se aceptan los dos vocabularios porque conviven durante la transicion:
 * 'APROBADO' es el estado nuevo del backend y 'CONFIRMADO' el de los pagos
 * que quedaron registrados antes de integrar la pasarela. Sin esta
 * tolerancia, las reservas ya pagadas perderian su comprobante en pantalla el
 * dia que se despliegue el cambio.
 *
 * @param {string} estado estado devuelto por la API de pagos
 * @returns {boolean} true si el pago fue aprobado
 */
function esPagoAprobado(estado) {
  const normalizado = String(estado || '').toUpperCase();
  return normalizado === 'APROBADO' || normalizado === 'APPROVED' || normalizado === 'CONFIRMADO';
}

/* --- MODAL Y PROCESAMIENTO DE PAGO CON PASARELA (Wompi / simulada) --- */

/**
 * Configuracion de pagos cacheada en el navegador.
 *
 * <p>Se pide una sola vez y se reutiliza cada vez que se abre el modal. El
 * objetivo es no ofrecer un metodo de pago que el servidor vaya a rechazar: si
 * el backend tiene PSE y TARJETA habilitados, el selector muestra exactamente
 * esos dos, ni uno mas ni uno menos.
 */
let configPagosCacheada = null;

/** Maximo de intentos de consulta del estado antes de rendirse. */
const MAX_INTENTOS_ESTADO = 40;

/** Tiempo entre consultas del estado, en milisegundos. */
const INTERVALO_ESTADO_MS = 2500;

/**
 * Como se ve cada metodo de pago en el selector.
 *
 * <p>El texto esta en el codigo, no en el servidor, porque es presentacion: el
 * backend responde solo con los codigos validos.
 */
const PRESENTACION_METODOS = {
  PSE: {
    icono: 'bi-bank2',
    color: 'text-primary',
    titulo: 'PSE (transferencia bancaria)',
    detalle: 'Pagas desde tu banco en linea, sin salir de AleLeo Tours'
  },
  TARJETA: {
    icono: 'bi-credit-card-fill',
    color: 'text-success',
    titulo: 'Tarjeta Debito / Credito',
    detalle: 'Procesamiento seguro en la pasarela de pagos'
  },
  NEQUI: {
    icono: 'bi-phone',
    color: 'text-info',
    titulo: 'Nequi',
    detalle: 'Pagas desde tu billetera movil'
  }
};

/**
 * Pinta el selector de metodos de pago segun lo que habilito el servidor.
 *
 * @param {Array} metodos respuesta de GET /api/pagos/configuracion
 */
function pintarMetodosPago(metodos) {
  const contenedor = document.getElementById('pago-metodos-container');
  if (!contenedor) return;

  contenedor.innerHTML = '';

  if (!Array.isArray(metodos) || metodos.length === 0) {
    contenedor.innerHTML =
      '<div class="col-12"><div class="alert alert-warning mb-0 small">' +
      'No hay metodos de pago disponibles en este momento. Intenta mas tarde.</div></div>';
    return;
  }

  metodos.forEach((metodo, indice) => {
    const info = PRESENTACION_METODOS[metodo.codigo] || {
      icono: 'bi-cash-coin',
      color: 'text-primary',
      titulo: metodo.nombre || metodo.codigo,
      detalle: 'Pago en linea seguro'
    };

    const columna = document.createElement('div');
    // Con mas de dos metodos las tarjetas se vuelven estrechas: se reparten en
    // columnas mas angostas en vez de dejar dos en blanco.
    columna.className = metodos.length > 2 ? 'col-md-4' : 'col-md-6';

    const marcado = indice === 0 ? 'checked' : '';
    columna.innerHTML =
      '<div class="form-check card-select-metodo p-3 border rounded-3 bg-white shadow-sm cursor-pointer h-100">' +
      '<input class="form-check-input" type="radio" name="metodoPagoRadio" ' +
      'id="metodo-' + escaparHtml(metodo.codigo) + '" ' +
      'value="' + escaparHtml(metodo.codigo) + '" ' + marcado + '>' +
      '<label class="form-check-label fw-bold text-dark w-100 cursor-pointer ms-1" ' +
      'for="metodo-' + escaparHtml(metodo.codigo) + '">' +
      '<i class="bi ' + info.icono + ' ' + info.color + ' me-2 fs-5"></i> ' +
      escaparHtml(info.titulo) +
      '<small class="d-block text-muted fw-normal">' + escaparHtml(info.detalle) + '</small>' +
      '</label></div>';

    contenedor.appendChild(columna);
  });
}

/**
 * Carga (y cachea) la configuracion de pagos del servidor.
 *
 * @returns {Promise<object>} configuracion de pagos
 */
async function obtenerConfiguracionPagos() {
  if (configPagosCacheada) return configPagosCacheada;

  try {
    const resp = await fetch(`${API_BASE}/pagos/configuracion`);
    if (!resp.ok) throw new Error('configuracion no disponible');
    configPagosCacheada = await resp.json();
  } catch (e) {
    // Si el backend no responde no se inventa la configuracion: se avisa y el
    // modal se cierra, porque sin metodos habilitados el pago no puede continuar.
    configPagosCacheada = null;
    throw new Error('No pudimos cargar la configuracion de pagos.');
  }

  pintarMetodosPago(configPagosCacheada.metodos);

  const nombreEl = document.getElementById('pago-pasarela-nombre');
  const detalleEl = document.getElementById('pago-pasarela-detalle');
  const badgeEl = document.getElementById('pago-modo-badge');
  const seguridadEl = document.getElementById('pago-seguridad-pasarela');

  const nombrePasarela = configPagosCacheada.pasarela === 'WOMPI'
    ? 'Wompi'
    : 'Pasarela de pruebas';

  if (nombreEl) nombreEl.textContent = nombrePasarela;
  if (detalleEl) {
    detalleEl.textContent = configPagosCacheada.modoSimulado
      ? 'Modo de demostracion: no se realiza ningun cobro real.'
      : 'Pago en linea verificado con firma digital.';
  }
  if (badgeEl) {
    badgeEl.textContent = configPagosCacheada.modoSimulado ? 'MODO PRUEBAS' : 'PAGO REAL';
  }
  if (seguridadEl) seguridadEl.textContent = nombrePasarela;

  return configPagosCacheada;
}

/**
 * Pide al servidor el precio de la solicitud y lo muestra bloqueado.
 *
 * <p>Este es el cambio de fondo respecto a la version anterior: el monto ya no
 * se calcula en el navegador. Antes se armaba a mano con el precio local, el
 * precio del catalogo o un "total estimado" del texto, y ese valor se enviaba
 * al servidor, que lo aceptaba sin comparar nada. Ahora el unico que dice
 * cuanto vale una reserva es el backend.
 *
 * @param {number} solicitudId id de la solicitud a cotizar
 * @returns {Promise<number>} precio en pesos
 */
async function cargarPrecioDesdeServidor(solicitudId) {
  const resp = await fetch(`${API_BASE}/pagos/cotizacion/${solicitudId}`);
  const data = await resp.json().catch(() => ({}));

  if (!resp.ok) {
    throw new Error(data.mensaje || data.error || 'No pudimos obtener el precio de la reserva.');
  }

  if (data.yaPagado) {
    throw new Error('Esta solicitud ya tiene un pago aprobado. No se puede volver a pagar.');
  }

  const monto = Number(data.monto);
  if (!Number.isFinite(monto) || monto <= 0) {
    throw new Error('Esta solicitud aun no tiene un precio asignado. Un asesor debe configurarlo.');
  }

  return monto;
}

/**
 * Abre el modal de pago.
 *
 * <p>Se pide el precio al servidor antes de mostrar nada. Si la solicitud no
 * tiene precio, el modal no se abre: es preferible un mensaje claro a mostrar
 * un formulario que va a fallar al enviar.
 *
 * @param {number} solicitudId id de la solicitud a pagar
 */
async function abrirModalPago(solicitudId) {
  solicitudParaPago = solicitudesCliente.find(s => String(s.id) === String(solicitudId));
  if (!solicitudParaPago) {
    // Nunca se paga "la primera de la lista": si no se encuentra la solicitud
    // seleccionada se aborta para no cobrar la reserva equivocada.
    solicitudParaPago = null;
    if (typeof Toast !== 'undefined') Toast.mostrar('No se encontró la solicitud seleccionada.', 'error');
    return;
  }

  const modalEl = document.getElementById('modalPagoSolicitud');
  if (!modalEl) return;
  const modalBs = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);

  ocultarMensaje('msg-modal-pago');
  setProgresoPago(false);
  resetearBotonPago();

  document.getElementById('pago-solicitud-id').value = solicitudParaPago.id;
  document.getElementById('pago-tour-titulo').textContent = solicitudParaPago.titulo || 'Reserva de Viaje';
  document.getElementById('pago-solicitud-badge').textContent = `Solicitud #SOL-${solicitudParaPago.id}`;

  // Se muestra de inmediato para que el usuario vea que la accion arranco, y
  // se rellena el precio en cuanto responde el servidor.
  document.getElementById('pago-monto-input').value = 'Calculando...';
  modalBs.show();

  try {
    // Primero la configuracion (metodos habilitados y pasarela activa).
    await obtenerConfiguracionPagos();

    // Despues el precio. En este orden porque el selector de metodos es
    // independiente del monto y ambos necesitan estar listos antes de que el
    // usuario pulse "Pagar".
    const monto = await cargarPrecioDesdeServidor(solicitudParaPago.id);

    document.getElementById('pago-monto-input').value = formatearPesos(monto);
    document.getElementById('pago-monto-origina').textContent =
      'Precio fijado por AleLeo Tours segun el catalogo de servicios.';
    document.getElementById('pago-notas-input').value = '';

  } catch (error) {
    // Se cierra el modal: no se cobra nada y el motivo queda claro.
    modalBs.hide();
    if (typeof Toast !== 'undefined') {
      Toast.mostrar(error.message || 'No pudimos abrir el pago.', 'error');
    }
  }
}

/**
 * Formatea un monto en pesos colombianos.
 *
 * @param {number} monto valor en pesos
 * @returns {string} texto con separadores de miles
 */
function formatearPesos(monto) {
  const numero = Number(monto) || 0;
  return numero.toLocaleString('es-CO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
}

/** Vuelve el boton de pago a su estado normal. */
function resetearBotonPago() {
  const btn = document.getElementById('btn-confirmar-pago');
  if (btn) {
    btn.disabled = false;
    btn.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i> Pagar Ahora';
  }
}

/** Muestra u oculta la pantalla de "Procesando el pago...". */
function setProgresoPago(visible, titulo, detalle) {
  const caja = document.getElementById('pago-progreso');
  if (!caja) return;

  caja.classList.toggle('d-none', !visible);
  if (visible) {
    const t = document.getElementById('pago-progreso-titulo');
    const d = document.getElementById('pago-progreso-detalle');
    if (t && titulo) t.textContent = titulo;
    if (d && detalle) d.textContent = detalle;
  }
}

/**
 * Muestra el progreso del cobro con un boton de reintento.
 *
 * <p>El boton aparece siempre: si la pasarela esta lenta y el usuario cierra el
 * modal, la reserva NO se pierde. El pago queda en estado PENDIENTE y se puede
 * volver a intentar, que es exactamente como funciona un cobro en la vida real.
 *
 * @param {string} referencia referencia del pago pendiente
 */
function mostrarPagoPendiente(referencia) {
  const caja = document.getElementById('pago-progreso');
  if (!caja) return;

  caja.classList.remove('d-none');
  caja.innerHTML =
    '<div class="d-flex flex-column flex-md-row align-items-center justify-content-between gap-3 p-3 rounded-3" ' +
    'style="background:var(--ds-gray-50); border:1px solid #cbd5e1;">' +
    '<div>' +
    '<strong class="d-block text-dark"><i class="bi bi-hourglass-split me-1"></i> ' +
    'Pago en proceso de confirmacion</strong>' +
    '<small class="text-muted">Referencia <code>' + escaparHtml(referencia || '-') + '</code>. ' +
    'La pasarela aun no confirma el cobro. Puedes cerrar esta ventana: no se te cobrara nada hasta que se apruebe.</small>' +
    '</div>' +
    '<button type="button" class="btn btn-sm btn-outline-primary flex-shrink-0" id="pago-reintentar">' +
    '<i class="bi bi-arrow-repeat me-1"></i> Consultar de nuevo</button>' +
    '</div>';

  const btnReintentar = document.getElementById('pago-reintentar');
  if (btnReintentar) {
    btnReintentar.addEventListener('click', () => consultarEstadoPago(referencia, true));
  }
}

/**
 * Consulta el estado del pago hasta que la pasarela responda.
 *
 * <p>El polling se hace contra el BACKEND, nunca contra la pasarela. Wompi no
 * permite que el navegador consulte su API directamente, y ademas el frontend
 * no tiene las credenciales para firmar las peticiones. Ademas asi funciona
 * cuando el webhook se pierde: el backend reconcilia antes de responder.
 *
 * @param {string} referencia referencia del pago
 * @param {boolean} [mostrarFeedback] si se muestra el spinner de espera
 * @returns {Promise<object|null>} el pago cuando queda en estado final
 */
async function consultarEstadoPago(referencia, mostrarFeedback) {
  if (mostrarFeedback) {
    setProgresoPago(true, 'Consultando el estado del pago...', 'Un momento, estamos verificando con la pasarela.');
  }

  for (let intento = 0; intento < MAX_INTENTOS_ESTADO; intento++) {
    try {
      const resp = await fetch(`${API_BASE}/pagos/estado/${encodeURIComponent(referencia)}`);
      const data = await resp.json().catch(() => ({}));

      if (!resp.ok) {
        // Un fallo puntual no cancela la espera: puede ser el servidor
        // reiniciandose. Se reintenta hasta agotar los intentos.
        console.warn(`Consulta de estado fallida (intento ${intento + 1}):`, data.mensaje || data.error);
        await dormir(INTERVALO_ESTADO_MS);
        continue;
      }

      if (data.comprobanteDisponible) {
        setProgresoPago(false);
        return data;
      }

      if (data.estado === 'RECHAZADO') {
        setProgresoPago(false);
        return data;
      }

      if (data.estado === 'ERROR' || data.estado === 'ANULADO') {
        setProgresoPago(false);
        return data;
      }

      setProgresoPago(true, 'Procesando el pago...',
        'La pasarela aun responde PENDIENTE. No cierres esta ventana.');

    } catch (e) {
      console.warn('Error de red consultando el estado:', e);
    }

    await dormir(INTERVALO_ESTADO_MS);
  }

  // Se agotaron los intentos sin respuesta concluyente. No se afirma que el
  // pago haya fallado: solo se informa que la confirmacion sigue pendiente.
  setProgresoPago(false);
  return null;
}

/** Promesa que espera un numero de milisegundos. */
function dormir(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Procesa el pago de una solicitud.
 *
 * <p>El cuerpo de la peticion tiene exactamente dos datos: que solicitud se
 * paga y con que metodo. NO incluye el monto, porque el backend ya sabe cuanto
 * vale. Antes se enviaba el monto y el servidor lo aceptaba sin verificarlo.
 *
 * @param {Event} [e] evento de submit del formulario
 */
async function procesarPagoSolicitud(e) {
  if (e) e.preventDefault();
  ocultarMensaje('msg-modal-pago');
  setProgresoPago(false);

  const rawSolicitudId = document.getElementById('pago-solicitud-id').value;
  const metodoPago = document.querySelector('input[name="metodoPagoRadio"]:checked')?.value;
  const notas = document.getElementById('pago-notas-input').value.trim();

  if (!rawSolicitudId) {
    mostrarMensaje('msg-modal-pago', 'No se ha seleccionado una solicitud válida.', 'error');
    return;
  }

  if (!metodoPago) {
    mostrarMensaje('msg-modal-pago', 'Selecciona un método de pago para continuar.', 'error');
    return;
  }

  const btnConfirmar = document.getElementById('btn-confirmar-pago');
  if (btnConfirmar) {
    btnConfirmar.disabled = true;
    btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Enviando a la pasarela...';
  }

  try {
    let targetSolicitudId = Number(rawSolicitudId);

    // Si la solicitud es local (reserva en localStorage o ID no persistido en
    // backend), se registra primero para poder cobrarla.
    if (solicitudParaPago && (solicitudParaPago.esLocal || isNaN(targetSolicitudId) || targetSolicitudId > 1000000000)) {
      targetSolicitudId = await persistirSolicitudLocal();
    }

    if (!targetSolicitudId || isNaN(targetSolicitudId) || targetSolicitudId <= 0) {
      mostrarMensaje('msg-modal-pago', 'No se encontró la solicitud en el servidor. Intenta nuevamente.', 'error');
      return;
    }

    // OJO: no se envia monto. El servidor lo calcula desde el catalogo.
    const payloadPago = {
      solicitudId: targetSolicitudId,
      metodoPago: metodoPago,
      notas: notas || null
    };

    const resp = await fetch(`${API_BASE}/pagos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payloadPago)
    });

    const data = await resp.json().catch(() => ({}));

    if (!resp.ok) {
      // El backend responde {error, mensaje}. Se lee "mensaje" primero porque
      // "error" trae el nombre de la excepcion ("Bad Request"), que no le
      // sirve de nada a un usuario.
      mostrarMensaje('msg-modal-pago', data.mensaje || data.error || 'No se pudo procesar el pago.', 'error');
      return;
    }

    // Camino 1: la pasarela tiene checkout externo y devuelve una URL.
    if (data.urlCheckout) {
      if (typeof Toast !== 'undefined') {
        Toast.mostrar('Redirigiendo a la pasarela de pagos...', 'info');
      }
      window.location.href = data.urlCheckout;
      return;
    }

    // Camino 2: el pago ya quedo aprobado de inmediato (no es lo habitual).
    if (data.comprobanteDisponible) {
      await finalizarPagoExitoso(data);
      return;
    }

    // Camino 3: queda pendiente. Se espera la confirmacion de la pasarela.
    setProgresoPago(true, 'Procesando el pago...',
      'La pasarela esta procesando tu cobro. Esto toma unos segundos.');

    const resultado = await consultarEstadoPago(data.referencia, false);

    if (!resultado) {
      mostrarPagoPendiente(data.referencia);
      await cargarMisSolicitudes();
      return;
    }

    if (resultado.comprobanteDisponible) {
      await finalizarPagoExitoso(resultado);
      return;
    }

    // La pasarela respondio con un estado negativo.
    const motivo = resultado.estado === 'RECHAZADO'
      ? 'La pasarela rechazo el pago. Intenta con otro método.'
      : 'No se pudo completar el pago. Intenta de nuevo.';

    mostrarMensaje('msg-modal-pago', motivo, 'error');
    await cargarMisSolicitudes();

  } catch (err) {
    mostrarMensaje('msg-modal-pago', 'Error al conectar con el servidor SACE.', 'error');
  } finally {
    if (btnConfirmar) {
      btnConfirmar.disabled = false;
      btnConfirmar.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i> Pagar Ahora';
    }
  }
}

/**
 * Persiste en el backend una solicitud que solo existia en el navegador.
 *
 * @returns {Promise<number|null>} id asignado por el servidor, o null
 */
async function persistirSolicitudLocal() {
  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');
  if (!sesion || !sesion.id) return null;

  try {
    const respSol = await fetch(`${API_BASE}/solicitudes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fechaCreacion: new Date().toISOString(),
        titulo: solicitudParaPago.titulo || 'Reserva de Viaje',
        asunto: solicitudParaPago.asunto || 'Reserva para viaje SACE',
        descripcion: solicitudParaPago.descripcion || 'Reserva de viaje',
        estado: 'PENDIENTE',
        prioridad: 'MEDIA',
        categoria: 'RESERVA',
        clienteId: sesion.id
      })
    });

    if (!respSol.ok) return null;

    const nueva = await respSol.json();
    solicitudParaPago.id = nueva.id;
    solicitudParaPago.esLocal = false;
    return nueva.id;
  } catch (eSol) {
    console.warn('Advertencia registrando solicitud local:', eSol);
    return null;
  }
}

/**
 * Cierra el flujo con exito: avisa, descarga el comprobante y refresca la lista.
 *
 * @param {object} pago respuesta del backend con el pago aprobado
 */
async function finalizarPagoExitoso(pago) {
  setProgresoPago(false);

  if (typeof Toast !== 'undefined') {
    Toast.mostrar('¡Pago aprobado! Tu comprobante se está descargando.', 'ok');
  }

  const modalEl = document.getElementById('modalPagoSolicitud');
  const modalBs = bootstrap.Modal.getInstance(modalEl);
  if (modalBs) modalBs.hide();

  if (pago.idPago) {
    await new Promise(resolve => setTimeout(resolve, 400));
    descargarComprobantePdf(pago.idPago);
  }

  await cargarMisSolicitudes();
}

/* --- DESCARGAR E IMPRIMIR COMPROBANTE PDF CON TOKEN BEARER --- */
async function descargarComprobantePdf(idPago) {
  try {
    const sesion = Sesion.obtener ? Sesion.obtener() : null;
    const token = sesion ? sesion.token : '';

    const resp = await fetch(`${API_BASE}/pagos/comprobante/${idPago}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (!resp.ok) {
      if (typeof Toast !== 'undefined') Toast.mostrar('No se pudo acceder al comprobante PDF.', 'error');
      return;
    }

    const blob = await resp.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `comprobante_pago_${idPago}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

  } catch (err) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Error al descargar el PDF.', 'error');
  }
}

async function descargarComprobantePdfPorSolicitud(solicitudId) {
  try {
    const resp = await fetch(`${API_BASE}/pagos/solicitud/${solicitudId}`);
    if (resp.ok) {
      const pagos = await resp.json();
      if (pagos && pagos.length > 0) {
        await descargarComprobantePdf(pagos[0].idPago);
        return;
      }
    }
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Esta solicitud no tiene pagos registrados, por lo que no existe comprobante.', 'info');
    }
  } catch (e) {
    if (typeof Toast !== 'undefined') Toast.mostrar('No se pudo consultar el comprobante.', 'error');
  }
}

async function imprimirComprobantePdfPorSolicitud(solicitudId) {
  try {
    const resp = await fetch(`${API_BASE}/pagos/solicitud/${solicitudId}`);
    if (resp.ok) {
      const pagos = await resp.json();
      if (pagos && pagos.length > 0) {
        const idPago = pagos[0].idPago;
        const sesion = Sesion.obtener ? Sesion.obtener() : null;
        const token = sesion ? sesion.token : '';

        const respPdf = await fetch(`${API_BASE}/pagos/comprobante/${idPago}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (respPdf.ok) {
          const blob = await respPdf.blob();
          const pdfUrl = URL.createObjectURL(blob);
          const printWindow = window.open(pdfUrl, '_blank');
          if (printWindow) {
            printWindow.focus();
          }
          return;
        }
      }
    }
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Esta solicitud no tiene pagos registrados, por lo que no existe comprobante.', 'info');
    }
  } catch (e) {
    console.warn('Error al imprimir comprobante:', e);
  }
}

/* --- CHAT CON EL ASESOR (MODAL CLIENTE) --- */
async function abrirChatCliente(solicitudId) {
  const solicitud = solicitudesCliente.find(s => s.id === solicitudId);
  if (!solicitud) return;

  chatClienteSolicitudActiva = solicitud;

  const modalEl = document.getElementById('modalChatCliente');
  const modalBs = new bootstrap.Modal(modalEl);

  document.getElementById('chat-cliente-subtitulo').textContent = `Solicitud #SOL-${solicitud.id}`;
  document.getElementById('chat-cliente-titulo').textContent = solicitud.titulo || 'Consulta de Viaje';
  document.getElementById('chat-cliente-estado-badge').innerHTML = `Estado: <strong>${formatearTextoEstado(normalizarEstado(solicitud.estado), solicitud.categoria)}</strong>`;

  await cargarMensajesChatCliente(solicitud.id);
  modalBs.show();

  if (chatClientePollingInterval) clearInterval(chatClientePollingInterval);
  chatClientePollingInterval = setInterval(() => {
    cargarMensajesChatCliente(solicitud.id, true);
  }, 3500);

  modalEl.addEventListener('hidden.bs.modal', () => {
    if (chatClientePollingInterval) clearInterval(chatClientePollingInterval);
    chatClienteSolicitudActiva = null;
  }, { once: true });
}

async function cargarMensajesChatCliente(solicitudId, mantenerScroll = false) {
  const contenedor = document.getElementById('chat-cliente-mensajes');
  if (!contenedor) return;

  try {
    const resp = await fetch(`${API_BASE}/mensajes/solicitud/${solicitudId}`);
    if (!resp.ok) return;

    const mensajes = await resp.json();
    contenedor.innerHTML = '';

    if (chatClienteSolicitudActiva && chatClienteSolicitudActiva.descripcion) {
      const divInicial = document.createElement('div');
      divInicial.className = 'alert alert-light border p-2 mb-2 small text-muted';
      divInicial.innerHTML = `<strong>📋 Tu Solicitud Inicial:</strong><br>${escaparHtml(chatClienteSolicitudActiva.descripcion)}`;
      contenedor.appendChild(divInicial);
    }

    if (!mensajes || mensajes.length === 0) {
      const divVacio = document.createElement('div');
      divVacio.className = 'text-center py-4 text-muted small';
      divVacio.innerHTML = '<i class="bi bi-chat-dots text-info" style="font-size:2rem;"></i><p class="mt-2">Tu consulta está en cola. Un asesor te responderá aquí en breve.</p>';
      contenedor.appendChild(divVacio);
      return;
    }

    mensajes.forEach(m => {
      const esCliente = (m.remitente || '').toLowerCase().includes('cliente') || !(m.remitente || '').toLowerCase().includes('asesor');
      const divMsg = document.createElement('div');
      divMsg.className = esCliente ? 'align-self-end bg-primary text-white p-2 px-3 rounded-3' : 'align-self-start bg-white border p-2 px-3 rounded-3 shadow-sm';
      divMsg.style.maxWidth = '75%';

      const hora = m.fecha ? new Date(m.fecha).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }) : '';

      divMsg.innerHTML = `
        <div class="small fw-bold mb-1 ${esCliente ? 'text-info' : 'text-success'}">${escaparHtml(m.remitente || 'Asesor')}</div>
        <div>${escaparHtml(m.contenido)}</div>
        <div class="text-end small opacity-75 mt-1" style="font-size: 10px;">${hora}</div>
      `;
      contenedor.appendChild(divMsg);
    });

    if (!mantenerScroll) {
      contenedor.scrollTop = contenedor.scrollHeight;
    }
  } catch (err) {
    console.warn('Error al cargar mensajes del cliente:', err);
  }
}

async function enviarMensajeCliente() {
  if (!chatClienteSolicitudActiva) return;

  const input = document.getElementById('chat-cliente-input');
  const texto = input.value.trim();
  if (!texto) return;

  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');
  const nombreCliente = sesion ? `${sesion.nombre} (Cliente)` : 'Cliente';
  const btn = document.getElementById('chat-cliente-enviar-btn');

  btn.disabled = true;

  try {
    const resp = await fetch(`${API_BASE}/mensajes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        remitente: nombreCliente,
        contenido: texto,
        tipo: 'TEXTO',
        fecha: new Date().toISOString(),
        solicitudId: chatClienteSolicitudActiva.id
      })
    });

    if (resp.ok) {
      input.value = '';
      await cargarMensajesChatCliente(chatClienteSolicitudActiva.id);
    }
  } catch (err) {
    console.error('Error al enviar mensaje del cliente:', err);
  } finally {
    btn.disabled = false;
  }
}

/* --- CANCELAR SOLICITUD / RESERVA --- */
async function cancelarSolicitud(id, esLocal) {
  if (!confirm('¿Estás seguro de que deseas cancelar esta reserva/solicitud?')) return;

  if (esLocal) {
    const reservasLocales = JSON.parse(localStorage.getItem('aleleo_reservas') || '[]')
      .filter(r => r.id !== id);
    localStorage.setItem('aleleo_reservas', JSON.stringify(reservasLocales));
    Toast.mostrar('Reserva cancelada con éxito.', 'info');
    await cargarMisSolicitudes();
    return;
  }

  const solicitud = solicitudesCliente.find(s => s.id === id);
  if (!solicitud) {
    Toast.mostrar('No se encontró la solicitud a cancelar.', 'error');
    return;
  }

  try {
    const resp = await fetch(`${API_BASE}/solicitudes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...solicitud,
        estado: 'CANCELADA'
      })
    });

    if (resp.ok) {
      fetch(`${API_BASE}/mensajes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          remitente: 'Sistema SACE',
          contenido: 'El cliente ha cancelado esta solicitud de viaje.',
          tipo: 'SISTEMA',
          fecha: new Date().toISOString(),
          solicitudId: id
        })
      }).catch(() => {});

      Toast.mostrar('Tu solicitud ha sido cancelada.', 'ok');
      await cargarMisSolicitudes();
    } else {
      // No se intenta DELETE: el interceptor lo bloquea para clientes y el
      // servidor explica el motivo real (propiedad, estado o permiso), asi
      // que se muestra tal cual en vez de un error generico.
      const data = await resp.json().catch(() => ({}));
      Toast.mostrar(data.mensaje || data.error || 'No se pudo cancelar la solicitud.', 'error');
    }
  } catch (err) {
    Toast.mostrar('Error al conectar con el servidor.', 'error');
  }
}

/* --- FORMULARIO: CREAR NUEVA SOLICITUD --- */
function inicializarFormularioNuevaSolicitud() {
  const btnAbrir = document.getElementById('btn-abrir-nueva-solicitud');
  const modalEl = document.getElementById('modalNuevaSolicitud');
  const form = document.getElementById('form-nueva-solicitud');

  if (btnAbrir && modalEl) {
    const modalBs = new bootstrap.Modal(modalEl);
    btnAbrir.addEventListener('click', () => {
      form.reset();
      ocultarMensaje('msg-nueva-solicitud');
      modalBs.show();
    });
  }

  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');
      if (!sesion) return;

      const titulo = document.getElementById('nueva-sol-titulo').value.trim();
      const categoria = document.getElementById('nueva-sol-categoria').value;
      const prioridad = document.getElementById('nueva-sol-prioridad').value;
      const descripcion = document.getElementById('nueva-sol-descripcion').value.trim();
      const btnSubmit = document.getElementById('btn-enviar-solicitud');

      if (!titulo || !descripcion) {
        mostrarMensaje('msg-nueva-solicitud', 'Por favor completa todos los campos requeridos.', 'error');
        return;
      }

      if (btnSubmit) {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Enviando...';
      }

      try {
        const payloadSolicitud = {
          fechaCreacion: new Date().toISOString(),
          titulo,
          asunto: `Consulta de cliente: ${sesion.nombre}`,
          descripcion: `${descripcion}\n\nContacto: ${sesion.nombre} (${sesion.correo}).`,
          estado: 'PENDIENTE',
          prioridad,
          categoria,
          clienteId: sesion.id
        };

        const resp = await fetch(`${API_BASE}/solicitudes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payloadSolicitud)
        });

        if (resp.ok) {
          const nuevaSol = await resp.json();
          await fetch(`${API_BASE}/mensajes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              remitente: `${sesion.nombre} (Cliente)`,
              contenido: descripcion,
              tipo: 'TEXTO',
              fecha: new Date().toISOString(),
              solicitudId: nuevaSol.id
            })
          });

          Toast.mostrar('¡Tu solicitud ha sido enviada a un asesor!', 'ok');
          const modalBs = bootstrap.Modal.getInstance(modalEl);
          if (modalBs) modalBs.hide();
          form.reset();
          await cargarMisSolicitudes();
        } else {
          mostrarMensaje('msg-nueva-solicitud', 'No se pudo registrar la solicitud en el servidor.', 'error');
        }
      } catch (err) {
        mostrarMensaje('msg-nueva-solicitud', 'Error al conectar con el servidor.', 'error');
      } finally {
        if (btnSubmit) {
          btnSubmit.disabled = false;
          btnSubmit.innerHTML = '<i class="bi bi-send-fill me-1"></i> Enviar Solicitud';
        }
      }
    });
  }
}

/* --- FILTROS Y EVENTOS DE BUSQUEDA --- */
function inicializarFiltrosYBusqueda() {
  document.querySelectorAll('.filtro-pill').forEach(btn => {
    btn.addEventListener('click', function() {
      document.querySelectorAll('.filtro-pill').forEach(b => b.classList.remove('activo'));
      this.classList.add('activo');
      filtroActual = this.dataset.filtro;
      renderizarSolicitudes();
    });
  });

  const inpBuscador = document.getElementById('buscador-reservas');
  if (inpBuscador) {
    inpBuscador.addEventListener('input', function() {
      busquedaActual = this.value.trim();
      renderizarSolicitudes();
    });
  }

  // El boton de "copiar llave" se elimino junto con la llave Bre-B fija: el pago
  // ya no se hace copiando una clave a mano, sino a traves de la pasarela.

  const formPago = document.getElementById('form-procesar-pago');
  if (formPago) {
    formPago.addEventListener('submit', procesarPagoSolicitud);
  }

  const chatEnviarBtn = document.getElementById('chat-cliente-enviar-btn');
  const chatInput = document.getElementById('chat-cliente-input');
  if (chatEnviarBtn) chatEnviarBtn.addEventListener('click', enviarMensajeCliente);
  if (chatInput) {
    chatInput.addEventListener('keydown', e => {
      if (e.key === 'Enter') enviarMensajeCliente();
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  inicializarFiltrosYBusqueda();
  inicializarFormularioNuevaSolicitud();
  cargarMisSolicitudes();
});
