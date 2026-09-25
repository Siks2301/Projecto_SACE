/* ==========================================================================
   AleLeo Tours - panel-empleado.js
   Módulo Operativo del Asesor: Disponibilidad - Dashboard KPI - Consola Chat SACE
   ========================================================================== */

'use strict';

let sesionEmpleado = null;
let solicitudesAsignadas = [];
let solicitudSeleccionada = null;
let serviciosDisponibles = [];
let filtroEstadoActivo = 'TODAS';

document.addEventListener('DOMContentLoaded', () => {
  verificarAccesoEmpleado();
  inicializarEventos();
  cargarDatosIniciales();
});

function esEmpleadoActivoYDisponible() {
  if (!sesionEmpleado) return false;
  const estadoAcceso = (sesionEmpleado.estadoAcceso || sesionEmpleado.estado || 'ACTIVA').toUpperCase();
  if (['INACTIVA', 'INACTIVO', 'BLOQUEADA', 'SUSPENDIDO'].includes(estadoAcceso)) {
    return false;
  }
  const disp = (sesionEmpleado.estadoDisponibilidad || 'DISPONIBLE').toUpperCase();
  return disp === 'DISPONIBLE';
}

function actualizarEstadoUIEmpleado() {
  const activo = esEmpleadoActivoYDisponible();
  const banner = document.getElementById('alerta-empleado-inactivo');
  const txtEstado = document.getElementById('alerta-empleado-estado');

  const disp = sesionEmpleado ? (sesionEmpleado.estadoDisponibilidad || 'FUERA_DE_TURNO') : 'FUERA_DE_TURNO';
  const textoEstado = String(disp).replace(/_/g, ' ');

  if (banner) {
    if (!activo) {
      banner.classList.remove('d-none');
      if (txtEstado) txtEstado.textContent = textoEstado;
    } else {
      banner.classList.add('d-none');
    }
  }

  // Controles en la consola modal de atención
  const btnEnviar = document.getElementById('chat-empleado-enviar-btn');
  const inputChat = document.getElementById('chat-empleado-input');
  const btnEstado = document.getElementById('btn-cambiar-estado-solicitud');
  const selEstado = document.getElementById('sol-nuevo-estado');
  const btnVincular = document.getElementById('btn-vincular-servicio');
  const selServicio = document.getElementById('sol-nuevo-servicio');

  if (btnEnviar) btnEnviar.disabled = !activo;
  if (inputChat) {
    inputChat.disabled = !activo;
    if (!activo) inputChat.placeholder = '⚠️ Cambia tu disponibilidad a DISPONIBLE para enviar respuestas...';
    else inputChat.placeholder = 'Escribe tu respuesta para el cliente...';
  }
  if (btnEstado) btnEstado.disabled = !activo;
  if (selEstado) selEstado.disabled = !activo;
  if (btnVincular) btnVincular.disabled = !activo;
  if (selServicio) selServicio.disabled = !activo;

  // Botones en las tarjetas de atención
  const btnsAtender = document.querySelectorAll('.card-solicitud-empleado button');
  btnsAtender.forEach(btn => {
    btn.disabled = !activo;
    if (!activo) {
      btn.title = 'Debes estar en estado DISPONIBLE para atender solicitudes.';
    } else {
      btn.removeAttribute('title');
    }
  });
}

function verificarAccesoEmpleado() {
  sesionEmpleado = typeof Sesion !== 'undefined' && Sesion.obtener
    ? Sesion.obtener()
    : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');

  if (!sesionEmpleado) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Debes iniciar sesión para acceder al Panel de Asesores.', 'error');
    setTimeout(() => { window.location.href = 'login.html'; }, 1000);
    return;
  }

  if (sesionEmpleado.tipoUsuario === 'CLIENTE') {
    if (typeof Toast !== 'undefined') Toast.mostrar('Acceso denegado. Este panel es exclusivo para empleados y asesores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1200);
    return;
  }

  const estadoAcceso = (sesionEmpleado.estadoAcceso || sesionEmpleado.estado || 'ACTIVA').toUpperCase();
  if (['INACTIVA', 'INACTIVO', 'BLOQUEADA', 'SUSPENDIDO'].includes(estadoAcceso)) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Tu cuenta de empleado se encuentra inactiva o bloqueada. Contacta al administrador.', 'error');
    setTimeout(() => { window.location.href = 'login.html'; }, 1500);
    return;
  }

  // Actualizar UI del Header de bienvenida
  const elNombre = document.getElementById('emp-nombre-saludo');
  const elDepto = document.getElementById('emp-depto-label');
  const elCargo = document.getElementById('emp-cargo-label');

  if (elNombre) elNombre.textContent = sesionEmpleado.nombre || 'Asesor';
  if (elDepto) elDepto.textContent = sesionEmpleado.departamento || 'Atención al Cliente';
  if (elCargo) elCargo.textContent = sesionEmpleado.cargoEspecifico || (sesionEmpleado.tipoUsuario === 'ADMINISTRADOR' ? 'Administrador SACE' : 'Asesor de Viajes');

  const selDisp = document.getElementById('sel-disponibilidad');
  if (selDisp && sesionEmpleado.estadoDisponibilidad) {
    selDisp.value = sesionEmpleado.estadoDisponibilidad;
  }

  actualizarEstadoUIEmpleado();
}

function inicializarEventos() {
  // Cambio de estado de disponibilidad
  const selDisp = document.getElementById('sel-disponibilidad');
  if (selDisp) {
    selDisp.addEventListener('change', actualizarDisponibilidadEmpleado);
  }

  // Filtros por estado
  const pills = document.querySelectorAll('.filtro-pill');
  pills.forEach(pill => {
    pill.addEventListener('click', (e) => {
      pills.forEach(p => p.classList.remove('activo'));
      const btn = e.currentTarget;
      btn.classList.add('activo');
      filtroEstadoActivo = btn.dataset.filtro;
      renderizarSolicitudesEmpleado();
    });
  });

  // Buscador por texto
  const buscador = document.getElementById('buscador-empleado-solicitudes');
  if (buscador) {
    buscador.addEventListener('input', renderizarSolicitudesEmpleado);
  }

  // Envío de mensaje desde el chat de atención
  const btnEnviar = document.getElementById('chat-empleado-enviar-btn');
  const inputChat = document.getElementById('chat-empleado-input');

  if (btnEnviar) {
    btnEnviar.addEventListener('click', enviarMensajeAsesor);
  }
  if (inputChat) {
    inputChat.addEventListener('keyup', (e) => {
      if (e.key === 'Enter') enviarMensajeAsesor();
    });
  }

  // Cambiar estado de solicitud
  const btnEstado = document.getElementById('btn-cambiar-estado-solicitud');
  if (btnEstado) {
    btnEstado.addEventListener('click', cambiarEstadoSolicitud);
  }

  // Vincular servicio
  const btnVincular = document.getElementById('btn-vincular-servicio');
  if (btnVincular) {
    btnVincular.addEventListener('click', vincularServicioSolicitud);
  }

  // Maximizar pantalla completa modal de atención
  const btnFullscreen = document.getElementById('btn-toggle-fullscreen');
  if (btnFullscreen) {
    btnFullscreen.addEventListener('click', () => {
      const modalEl = document.getElementById('modalAtencionEmpleado');
      const modalDialog = modalEl ? modalEl.querySelector('.modal-dialog') : null;
      if (modalEl && modalDialog) {
        const esFullscreen = modalDialog.classList.toggle('modal-fullscreen');
        modalDialog.classList.toggle('modal-dialog-centered', !esFullscreen);
        modalDialog.classList.toggle('modal-dialog-scrollable', !esFullscreen);
        modalEl.classList.toggle('modal-is-fullscreen', esFullscreen);
        btnFullscreen.innerHTML = esFullscreen
          ? '<i class="bi bi-fullscreen-exit me-1"></i> Restaurar'
          : '<i class="bi bi-arrows-fullscreen me-1"></i> Maximizar';
      }
    });
  }
}

async function cargarDatosIniciales() {
  await Promise.all([
    cargarServiciosDisponibles(),
    cargarSolicitudesAsignadas()
  ]);
}

async function cargarServiciosDisponibles() {
  try {
    const resp = await fetch(`${API_BASE}/servicios`);
    if (resp.ok) {
      serviciosDisponibles = await resp.json();
      const selServicio = document.getElementById('sol-nuevo-servicio');
      if (selServicio) {
        selServicio.innerHTML = '<option value="">Seleccionar plan turístico...</option>';
        serviciosDisponibles.forEach(s => {
          const opt = document.createElement('option');
          opt.value = s.id;
          opt.textContent = `${s.nombre} - $${Number(s.precio).toLocaleString('es-CO')}`;
          selServicio.appendChild(opt);
        });
      }
    }
  } catch (err) {
    console.warn('Servicios no cargados desde backend backend:', err);
  }
}

async function actualizarDisponibilidadEmpleado() {
  const selDisp = document.getElementById('sel-disponibilidad');
  if (!selDisp || !sesionEmpleado) return;

  const nuevaDisp = selDisp.value;
  sesionEmpleado.estadoDisponibilidad = nuevaDisp;
  
  if (typeof Sesion !== 'undefined' && Sesion.guardar) {
    Sesion.guardar(sesionEmpleado);
  }

  actualizarEstadoUIEmpleado();

  try {
    const resp = await fetch(`${API_BASE}/empleados/${sesionEmpleado.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      // Solo se envia el campo que cambia: reenviar la sesion completa llegaba
      // sin email/telefono y el PUT parcial anterior los nulificaba en BD.
      body: JSON.stringify({
        estadoDisponibilidad: nuevaDisp
      })
    });

    if (resp.ok) {
      if (typeof Toast !== 'undefined') Toast.mostrar(`Disponibilidad actualizada a: ${nuevaDisp}`, 'ok');
    }
  } catch (err) {
    if (typeof Toast !== 'undefined') Toast.mostrar(`Estado de disponibilidad guardado localmente: ${nuevaDisp}`, 'info');
  }
}

async function cargarSolicitudesAsignadas() {
  const contenedor = document.getElementById('contenedor-solicitudes-empleado');
  if (contenedor) {
    contenedor.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div><p class="mt-2 text-muted">Cargando solicitudes asignadas...</p></div>';
  }

  // Intentar cargar métricas operativas directas desde /api/reportes/empleado/{idEmpleado}
  if (sesionEmpleado && sesionEmpleado.id && sesionEmpleado.tipoUsuario !== 'ADMINISTRADOR') {
    try {
      const respRep = await fetch(`${API_BASE}/reportes/empleado/${sesionEmpleado.id}`);
      if (respRep.ok) {
        const repEmpleado = await respRep.json();
        if (repEmpleado && repEmpleado.solicitudes) {
          solicitudesAsignadas = repEmpleado.solicitudes;
          if (document.getElementById('kpi-asignadas')) document.getElementById('kpi-asignadas').textContent = repEmpleado.totalAsignadas || 0;
          if (document.getElementById('kpi-pendientes')) document.getElementById('kpi-pendientes').textContent = repEmpleado.pendientes || 0;
          if (document.getElementById('kpi-proceso')) document.getElementById('kpi-proceso').textContent = repEmpleado.enProceso || 0;
          if (document.getElementById('kpi-resueltas')) document.getElementById('kpi-resueltas').textContent = repEmpleado.resueltas || 0;

          renderizarSolicitudesEmpleado();
          return;
        }
      }
    } catch (e) {
      console.warn('Endpoint de reportes de empleado no disponible, usando fallback:', e);
    }
  }

  try {
    const resp = await fetch(`${API_BASE}/solicitudes`);
    if (resp.ok) {
      const todas = await resp.json();

      // Si es admin muestra todas; si es asesor filtra por su id de empleado
      if (sesionEmpleado.tipoUsuario === 'ADMINISTRADOR') {
        solicitudesAsignadas = todas;
      } else {
        solicitudesAsignadas = todas.filter(s =>
          (s.empleadoAsignado && (Number(s.empleadoAsignado.id) === Number(sesionEmpleado.id) || Number(s.empleadoAsignado) === Number(sesionEmpleado.id))) ||
          (s.empleadoAsignadoId && Number(s.empleadoAsignadoId) === Number(sesionEmpleado.id)) ||
          s.estado === 'PENDIENTE'
        );
      }
    } else {
      cargarSolicitudesLocal();
    }
  } catch (err) {
    cargarSolicitudesLocal();
  }

  calcularKPIs();
  renderizarSolicitudesEmpleado();
}

function cargarSolicitudesLocal() {
  const locales = JSON.parse(localStorage.getItem('onvacation_reservas') || '[]');
  solicitudesAsignadas = locales.map(r => ({
    id: r.id,
    fechaCreacion: r.fechaReserva || new Date().toISOString(),
    titulo: r.titulo || `Reserva: ${r.destino}`,
    asunto: `Viaje a ${r.destino}`,
    descripcion: `Reserva para ${r.pasajeros || 1} persona(s) a ${r.destino}. Fecha: ${r.fecha || 'Por definir'}. Contacto: ${r.nombre} (${r.correo}).`,
    estado: 'PENDIENTE',
    prioridad: 'MEDIA',
    categoria: 'RESERVA',
    cliente: { nombre: r.nombre || 'Cliente', email: r.correo || r.usuario, telefono: 'Por especificar' }
  }));
}

function calcularKPIs() {
  const asignadas = solicitudesAsignadas.length;
  const pendientes = solicitudesAsignadas.filter(s => s.estado === 'PENDIENTE').length;
  const proceso = solicitudesAsignadas.filter(s => s.estado === 'EN_PROCESO').length;
  const resueltas = solicitudesAsignadas.filter(s => s.estado === 'RESUELTA' || s.estado === 'APROBADA' || s.estado === 'CONFIRMADA').length;

  if (document.getElementById('kpi-asignadas')) document.getElementById('kpi-asignadas').textContent = asignadas;
  if (document.getElementById('kpi-pendientes')) document.getElementById('kpi-pendientes').textContent = pendientes;
  if (document.getElementById('kpi-proceso')) document.getElementById('kpi-proceso').textContent = proceso;
  if (document.getElementById('kpi-resueltas')) document.getElementById('kpi-resueltas').textContent = resueltas;
}

function renderizarSolicitudesEmpleado() {
  const contenedor = document.getElementById('contenedor-solicitudes-empleado');
  const buscador = document.getElementById('buscador-empleado-solicitudes');
  const query = buscador ? buscador.value.toLowerCase().trim() : '';

  if (!contenedor) return;

  let filtradas = solicitudesAsignadas;

  if (filtroEstadoActivo !== 'TODAS') {
    filtradas = filtradas.filter(s => s.estado === filtroEstadoActivo);
  }

  if (query) {
    filtradas = filtradas.filter(s => 
      (s.titulo && s.titulo.toLowerCase().includes(query)) ||
      (s.cliente && s.cliente.nombre && s.cliente.nombre.toLowerCase().includes(query)) ||
      (s.categoria && s.categoria.toLowerCase().includes(query))
    );
  }

  if (filtradas.length === 0) {
    contenedor.innerHTML = `
      <div class="text-center py-5 bg-white rounded-4 border shadow-sm">
        <i class="bi bi-inbox text-muted" style="font-size: 3rem;"></i>
        <h2 class="mt-2 text-dark fw-bold">No tienes atenciones en esta categoría</h2>
        <p class="text-muted small">Cambia el filtro de búsqueda o el estado para visualizar otras solicitudes.</p>
      </div>
    `;
    actualizarEstadoUIEmpleado();
    return;
  }

  contenedor.innerHTML = '';

  filtradas.forEach(s => {
    const clienteNombre = s.cliente ? (s.cliente.nombre ? `${s.cliente.nombre} ${s.cliente.apellido || ''}` : 'Cliente') : 'Cliente Registrado';
    const clienteContacto = s.cliente ? (s.cliente.email || 'Sin correo') : 'Sin correo';
    const fecha = new Date(s.fechaCreacion || Date.now()).toLocaleDateString('es-CO', { year: 'numeric', month: 'short', day: 'numeric' });

    let estadoClass = 'bg-warning text-dark';
    if (s.estado === 'EN_PROCESO') estadoClass = 'bg-info text-white';
    if (s.estado === 'RESUELTA' || s.estado === 'CONFIRMADA') estadoClass = 'bg-success text-white';
    if (s.estado === 'CANCELADA') estadoClass = 'bg-danger text-white';

    const card = document.createElement('div');
    card.className = 'card-solicitud-empleado';
    card.innerHTML = `
      <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
        <div>
          <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
            <span class="badge ${estadoClass} fw-bold px-3 py-1 rounded-pill">${escaperHtml(s.estado || 'PENDIENTE')}</span>
            <span class="badge bg-secondary-subtle text-secondary border fw-semibold">${escaperHtml(s.categoria || 'GENERAL')}</span>
            <span class="badge bg-warning-subtle text-warning-emphasis border fw-semibold">Prioridad: ${escaperHtml(s.prioridad || 'MEDIA')}</span>
            <small class="text-muted ms-2"><i class="bi bi-calendar3"></i> ${fecha}</small>
          </div>
          <h2 class="h5 fw-bold text-dark mb-1 mt-2">${escaperHtml(s.titulo || 'Solicitud de Viaje')}</h2>
          <p class="text-muted small mb-0"><i class="bi bi-person-fill text-primary"></i> <strong>${escaperHtml(clienteNombre)}</strong> (${escaperHtml(clienteContacto)})</p>
        </div>

        <div>
          <button class="btn btn-primary fw-bold px-4 py-2" onclick="abrirConsolaAtencion(${s.id})" style="background:var(--grad-primary); border:none;">
            <i class="bi bi-headset me-1"></i> Atender / Chat
          </button>
        </div>
      </div>
    `;
    contenedor.appendChild(card);
  });

  actualizarEstadoUIEmpleado();
}

let chatEmpleadoPollingInterval = null;

async function abrirConsolaAtencion(id) {
  if (!esEmpleadoActivoYDisponible()) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Debes cambiar tu disponibilidad a DISPONIBLE para atender solicitudes.', 'error');
    return;
  }

  solicitudSeleccionada = solicitudesAsignadas.find(s => s.id === id);
  if (!solicitudSeleccionada) return;

  // Cargar info en el modal
  document.getElementById('atencion-subtitulo').textContent = `Solicitud #SOL-${solicitudSeleccionada.id}`;
  
  const clienteNombre = solicitudSeleccionada.cliente ? `${solicitudSeleccionada.cliente.nombre || ''} ${solicitudSeleccionada.cliente.apellido || ''}` : 'Cliente Registrado';
  const clienteContacto = solicitudSeleccionada.cliente ? `${solicitudSeleccionada.cliente.email || ''} · ${solicitudSeleccionada.cliente.telefono || ''}` : 'Sin contacto registrado';

  const elNombre = document.getElementById('cliente-info-nombre');
  const elContacto = document.getElementById('cliente-info-contacto');
  const elTitulo = document.getElementById('sol-info-titulo');
  const elCategoria = document.getElementById('sol-info-categoria');
  const elPrioridad = document.getElementById('sol-info-prioridad');
  const elDesc = document.getElementById('sol-info-descripcion');

  if (elNombre) elNombre.textContent = clienteNombre;
  if (elContacto) elContacto.textContent = clienteContacto;
  if (elTitulo) elTitulo.textContent = solicitudSeleccionada.titulo || 'Sin Título';
  if (elCategoria) elCategoria.textContent = solicitudSeleccionada.categoria || 'GENERAL';
  if (elPrioridad) elPrioridad.textContent = solicitudSeleccionada.prioridad || 'MEDIA';
  if (elDesc) elDesc.textContent = solicitudSeleccionada.descripcion || 'Sin descripción adicional.';

  document.getElementById('sol-nuevo-estado').value = solicitudSeleccionada.estado || 'PENDIENTE';

  if (document.getElementById('sol-nuevo-servicio')) {
    document.getElementById('sol-nuevo-servicio').value = solicitudSeleccionada.servicioGenerado ? solicitudSeleccionada.servicioGenerado.id : '';
  }

  // Cargar Mensajes iniciales
  await cargarMensajesChat(solicitudSeleccionada.id);

  // Abrir Modal Bootstrap e iniciar sondeo en tiempo real
  const modalEl = document.getElementById('modalAtencionEmpleado');
  if (modalEl) {
    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
    modalInstance.show();

    if (chatEmpleadoPollingInterval) clearInterval(chatEmpleadoPollingInterval);
    chatEmpleadoPollingInterval = setInterval(() => {
      if (solicitudSeleccionada) {
        cargarMensajesChat(solicitudSeleccionada.id, true);
      }
    }, 3500);

    modalEl.addEventListener('hidden.bs.modal', () => {
      if (chatEmpleadoPollingInterval) clearInterval(chatEmpleadoPollingInterval);
    }, { once: true });
  }
}

async function cargarMensajesChat(solicitudId, mantenerScroll = false) {
  const contenedorMsg = document.getElementById('chat-empleado-mensajes');
  if (!contenedorMsg) return;

  if (!mantenerScroll) {
    contenedorMsg.innerHTML = '<div class="text-center py-4 text-muted small"><div class="spinner-border spinner-border-sm text-primary"></div> Cargando hilo de chat...</div>';
  }

  // 1. Re-verificar estado actualizado de la solicitud en backend
  let estadoActual = solicitudSeleccionada ? solicitudSeleccionada.estado : 'PENDIENTE';
  try {
    const respSol = await fetch(`${API_BASE}/solicitudes/${solicitudId}`);
    if (respSol.ok) {
      const dataSol = await respSol.json();
      if (dataSol && dataSol.estado) {
        estadoActual = dataSol.estado;
        if (solicitudSeleccionada) solicitudSeleccionada.estado = estadoActual;
      }
    }
  } catch (e) {
    // Mantener estado actual
  }

  // 2. Controlar la alerta de cancelación e inhabilitar controles si fue cancelada
  const bannerCancelada = document.getElementById('alerta-solicitud-cancelada-modal');
  const btnEnviar = document.getElementById('chat-empleado-enviar-btn');
  const inputChat = document.getElementById('chat-empleado-input');
  const btnEstado = document.getElementById('btn-cambiar-estado-solicitud');
  const selEstado = document.getElementById('sol-nuevo-estado');
  const btnVincular = document.getElementById('btn-vincular-servicio');
  const selServicio = document.getElementById('sol-nuevo-servicio');

  const esCancelada = ['CANCELADA', 'RECHAZADA'].includes(String(estadoActual).toUpperCase());

  if (bannerCancelada) {
    if (esCancelada) bannerCancelada.classList.remove('d-none');
    else bannerCancelada.classList.add('d-none');
  }

  if (esCancelada) {
    if (btnEnviar) btnEnviar.disabled = true;
    if (inputChat) {
      inputChat.disabled = true;
      inputChat.placeholder = '⚠️ Esta solicitud ha sido cancelada por el cliente.';
    }
    if (btnEstado) btnEstado.disabled = true;
    if (selEstado) {
      selEstado.value = estadoActual;
      selEstado.disabled = true;
    }
    if (btnVincular) btnVincular.disabled = true;
    if (selServicio) selServicio.disabled = true;
  }

  // 3. Cargar mensajes
  let mensajes = [];
  try {
    const resp = await fetch(`${API_BASE}/mensajes`);
    if (resp.ok) {
      const todosMensajes = await resp.json();
      mensajes = todosMensajes.filter(m => (m.solicitud && m.solicitud.id === solicitudId) || m.solicitudId === solicitudId);
    }
  } catch (err) {
    mensajes = [];
  }

  if (mensajes.length === 0) {
    contenedorMsg.innerHTML = `
      <div class="text-center py-4 text-muted small">
        <i class="bi bi-chat-left-text opacity-50 fs-2"></i>
        <p class="mt-1">Inicia la conversación enviando un mensaje al cliente.</p>
      </div>
    `;
    return;
  }

  contenedorMsg.innerHTML = '';

  mensajes.forEach(m => {
    const esEmpleado = m.remitente && (m.remitente.includes(sesionEmpleado.nombre) || m.remitente.includes('Asesor'));
    const esSistema = m.remitente && (m.remitente.includes('Sistema') || m.tipo === 'SISTEMA');

    const msgDiv = document.createElement('div');

    if (esSistema) {
      msgDiv.className = 'w-100 text-center my-2';
      msgDiv.innerHTML = `
        <span class="badge bg-danger-subtle text-danger border border-danger-subtle px-3 py-2 rounded-pill small">
          <i class="bi bi-exclamation-octagon-fill me-1"></i> ${escaperHtml(m.contenido)}
        </span>
      `;
    } else {
      msgDiv.className = `d-flex flex-column ${esEmpleado ? 'align-items-end' : 'align-items-start'} mb-2`;
      msgDiv.innerHTML = `
        <div class="p-3 rounded-4 shadow-sm text-dark ${esEmpleado ? 'bg-primary-subtle text-primary-emphasis border border-primary-subtle' : 'bg-white border'}" style="max-width: 82%;">
          <div class="fw-bold small mb-1 ${esEmpleado ? 'text-primary' : 'text-dark'}">${escaperHtml(m.remitente || 'Usuario')}</div>
          <div class="small">${escaperHtml(m.contenido)}</div>
        </div>
        <small class="text-muted px-1 mt-1" style="font-size:10px;">${new Date(m.fecha || Date.now()).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}</small>
      `;
    }
    contenedorMsg.appendChild(msgDiv);
  });

  if (!mantenerScroll) {
    contenedorMsg.scrollTop = contenedorMsg.scrollHeight;
  }
}

async function enviarMensajeAsesor() {
  if (!esEmpleadoActivoYDisponible()) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Debes estar en estado DISPONIBLE para enviar respuestas.', 'error');
    return;
  }

  const inputChat = document.getElementById('chat-empleado-input');
  if (!inputChat || !solicitudSeleccionada) return;

  const texto = inputChat.value.trim();
  if (!texto) return;

  const nuevoMensaje = {
    fecha: new Date().toISOString(),
    remitente: `Asesor: ${sesionEmpleado.nombre}`,
    contenido: texto,
    tipo: 'TEXTO',
    solicitudId: solicitudSeleccionada.id
  };

  inputChat.value = '';

  try {
    const resp = await fetch(`${API_BASE}/mensajes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(nuevoMensaje)
    });

    if (resp.ok) {
      if (typeof Toast !== 'undefined') Toast.mostrar('Mensaje enviado al cliente.', 'ok');
    }
  } catch (err) {
    console.warn('Guardado local de mensaje:', err);
  }

  await cargarMensajesChat(solicitudSeleccionada.id);
}

async function cambiarEstadoSolicitud() {
  if (!esEmpleadoActivoYDisponible()) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Debes estar en estado DISPONIBLE para cambiar el estado de la solicitud.', 'error');
    return;
  }

  const selEstado = document.getElementById('sol-nuevo-estado');
  if (!selEstado || !solicitudSeleccionada) return;

  const nuevoEstado = selEstado.value;
  solicitudSeleccionada.estado = nuevoEstado;

  try {
    const resp = await fetch(`${API_BASE}/solicitudes/${solicitudSeleccionada.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...solicitudSeleccionada,
        estado: nuevoEstado
      })
    });

    if (resp.ok) {
      if (typeof Toast !== 'undefined') Toast.mostrar(`Estado de la solicitud actualizado a: ${nuevoEstado}`, 'ok');
    }
  } catch (err) {
    if (typeof Toast !== 'undefined') Toast.mostrar(`Estado guardado localmente: ${nuevoEstado}`, 'info');
  }

  calcularKPIs();
  renderizarSolicitudesEmpleado();
}

async function vincularServicioSolicitud() {
  if (!esEmpleadoActivoYDisponible()) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Debes estar en estado DISPONIBLE para vincular servicios.', 'error');
    return;
  }

  const selServicio = document.getElementById('sol-nuevo-servicio');
  if (!selServicio || !solicitudSeleccionada) return;

  const servicioId = selServicio.value;
  if (!servicioId) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Selecciona un plan del catálogo para vincular.', 'info');
    return;
  }

  try {
    const resp = await fetch(`${API_BASE}/solicitudes/${solicitudSeleccionada.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...solicitudSeleccionada,
        servicioGeneradoId: Number(servicioId)
      })
    });

    if (resp.ok) {
      if (typeof Toast !== 'undefined') Toast.mostrar('Servicio vinculado exitosamente a la solicitud.', 'ok');
    }
  } catch (err) {
    if (typeof Toast !== 'undefined') Toast.mostrar('Servicio vinculado localmente.', 'info');
  }
}
