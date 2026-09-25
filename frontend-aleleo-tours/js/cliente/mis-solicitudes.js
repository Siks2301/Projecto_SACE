/* ==========================================================================
   AleLeo Tours - mis-solicitudes.js
   Gestion de reservas, solicitudes, Pagos con Bre-B @VXM301 y Descarga de Comprobantes PDF.
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
  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');
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

  // Pagos CONFIRMADOS del cliente: alimentan el badge "Pago Confirmado" y los
  // botones de comprobante (antes se adivinaba por el estado RESUELTA y se
  // mostraban comprobantes fantasma en solicitudes sin ningun pago).
  pagosRealizados = [];
  const pagosPorSolicitud = await Promise.all(
    solicitudesCliente
      .filter(s => !s.esLocal)
      .map(s => fetch(`${API_BASE}/pagos/solicitud/${s.id}`)
        .then(r => (r.ok ? r.json() : []))
        .catch(() => []))
  );
  pagosPorSolicitud.flat()
    .filter(p => p && String(p.estado || '').toUpperCase() === 'CONFIRMADO')
    .forEach(p => pagosRealizados.push(p));

  // Integrar reservas locales de respaldo
  const reservasLocales = JSON.parse(localStorage.getItem('onvacation_reservas') || '[]')
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
    // "Pagado" = existe un pago CONFIRMADO real para esta solicitud; el estado
    // RESUELTA por si solo no garantiza que se haya cobrado.
    const pagoConfirmado = pagosRealizados.find(p =>
      Number(p.solicitudId) === Number(s.id) &&
      String(p.estado || '').toUpperCase() === 'CONFIRMADO');
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
          ${esPagado ? '<span class="badge bg-success text-white ms-2"><i class="bi bi-shield-check me-1"></i> Pago Confirmado</span>' : ''}
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

/* --- MODAL Y PROCESAMIENTO DE PAGO CON Bre-B @VXM301 --- */
function abrirModalPago(solicitudId) {
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

  document.getElementById('pago-solicitud-id').value = solicitudParaPago.id;
  document.getElementById('pago-tour-titulo').textContent = solicitudParaPago.titulo || 'Reserva de Viaje';
  document.getElementById('pago-solicitud-badge').textContent = `Solicitud #SOL-${solicitudParaPago.id}`;

  // Precio fijo con fuente real (antes: el primer numero de la descripcion o
  // 150.000 inventados). Orden: precio de la reserva local -> precio del
  // servicio vinculado en BD -> "Total estimado" explicito de la descripcion.
  let montoEstimado = 0;
  if (solicitudParaPago.precioEstimado) {
    montoEstimado = Number(solicitudParaPago.precioEstimado) || 0;
  }
  if (!montoEstimado && solicitudParaPago.servicioGeneradoId) {
    const srv = catalogoServicios.find(c => Number(c.id) === Number(solicitudParaPago.servicioGeneradoId));
    if (srv) montoEstimado = Number(srv.precio) || 0;
  }
  if (!montoEstimado && solicitudParaPago.descripcion) {
    const match = solicitudParaPago.descripcion.match(/total estimado[^$\d]*\$?\s*([\d.,]+)/i);
    if (match) {
      const parsed = parseFloat(match[1].replace(/\./g, '').replace(',', '.'));
      if (parsed > 0) montoEstimado = parsed;
    }
  }
  if (!montoEstimado) {
    // Sin precio definido no se abre el pago: un asesor debe fijar el valor.
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Esta solicitud aún no tiene un precio definido. Un asesor confirmará el valor antes del pago.', 'info');
    }
    solicitudParaPago = null;
    return;
  }

  const montoInput = document.getElementById('pago-monto-input');
  montoInput.value = montoEstimado;
  montoInput.readOnly = true; // Precio fijo: el cliente no puede pagar lo que quiera
  document.getElementById('pago-notas-input').value = '';
  ocultarMensaje('msg-modal-pago');

  modalBs.show();
}

function copiarLlaveTransferencia() {
  const llave = 'Bre-B @VXM301';
  navigator.clipboard.writeText(llave).then(() => {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('¡Llave Bre-B @VXM301 copiada al portapapeles!', 'ok');
    }
  }).catch(() => {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Llave: Bre-B @VXM301', 'info');
    }
  });
}

async function procesarPagoSolicitud(e) {
  if (e) e.preventDefault();
  ocultarMensaje('msg-modal-pago');

  const rawSolicitudId = document.getElementById('pago-solicitud-id').value;
  const montoVal = parseFloat(document.getElementById('pago-monto-input').value);
  const metodoPago = document.querySelector('input[name="metodoPagoRadio"]:checked')?.value || 'TRANSFERENCIA';
  const notas = document.getElementById('pago-notas-input').value.trim();

  if (!rawSolicitudId) {
    mostrarMensaje('msg-modal-pago', 'No se ha seleccionado una solicitud válida.', 'error');
    return;
  }

  if (isNaN(montoVal) || montoVal <= 0) {
    mostrarMensaje('msg-modal-pago', 'El monto a abonar debe ser mayor a cero (0).', 'error');
    return;
  }

  const btnConfirmar = document.getElementById('btn-confirmar-pago');
  if (btnConfirmar) {
    btnConfirmar.disabled = true;
    btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Generando Comprobante...';
  }

  try {
    const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');
    let targetSolicitudId = Number(rawSolicitudId);

    // Si la solicitud es local (reserva en localStorage o ID no persistido en backend), la creamos primero en BD
    if (solicitudParaPago && (solicitudParaPago.esLocal || isNaN(targetSolicitudId) || targetSolicitudId > 1000000000)) {
      if (sesion && sesion.id) {
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

          if (respSol.ok) {
            const nuevaSolBackend = await respSol.json();
            targetSolicitudId = nuevaSolBackend.id;
            solicitudParaPago.id = targetSolicitudId;
            solicitudParaPago.esLocal = false;
          }
        } catch (eSol) {
          console.warn('Advertencia registrando solicitud local:', eSol);
        }
      }
    }

    if (isNaN(targetSolicitudId) || targetSolicitudId <= 0) {
      mostrarMensaje('msg-modal-pago', 'Error: No se encontró la solicitud en el servidor. Intenta nuevamente.', 'error');
      return;
    }

    const payloadPago = {
      solicitudId: targetSolicitudId,
      monto: montoVal,
      metodoPago: metodoPago,
      llaveDestino: 'Bre-B @VXM301',
      notas: notas
    };

    const resp = await fetch(`${API_BASE}/pagos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payloadPago)
    });

    const data = await resp.json().catch(() => ({}));

    if (!resp.ok) {
      mostrarMensaje('msg-modal-pago', data.error || data.mensaje || 'No se pudo procesar el pago en el servidor.', 'error');
      return;
    }

    if (typeof Toast !== 'undefined') {
      Toast.mostrar('¡Pago confirmado! Se ha generado tu recibo PDF.', 'ok');
    }

    const modalEl = document.getElementById('modalPagoSolicitud');
    const modalBs = bootstrap.Modal.getInstance(modalEl);
    if (modalBs) modalBs.hide();

    if (data.idPago) {
      setTimeout(() => {
        descargarComprobantePdf(data.idPago);
      }, 500);
    }

    await cargarMisSolicitudes();

  } catch (err) {
    mostrarMensaje('msg-modal-pago', 'Error al conectar con el servidor SACE.', 'error');
  } finally {
    if (btnConfirmar) {
      btnConfirmar.disabled = false;
      btnConfirmar.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i> Confirmar Pago & Generar PDF';
    }
  }
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

  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');
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
    const reservasLocales = JSON.parse(localStorage.getItem('onvacation_reservas') || '[]')
      .filter(r => r.id !== id);
    localStorage.setItem('onvacation_reservas', JSON.stringify(reservasLocales));
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
      const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');
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

  const btnCopiar = document.getElementById('btn-copiar-llave');
  if (btnCopiar) {
    btnCopiar.addEventListener('click', copiarLlaveTransferencia);
  }

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