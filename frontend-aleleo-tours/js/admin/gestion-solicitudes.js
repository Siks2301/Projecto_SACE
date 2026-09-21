/* ==========================================================================
   AleLeo Tours - gestion-solicitudes.js
   CRUD de Solicitudes y Chat en Vivo con el Cliente
   ========================================================================== */

'use strict';

let solicitudesCache = [];
let clientesCacheSol = [];
let empleadosCacheSol = [];
let chatbotsCacheSol = [];
let serviciosCacheSol = [];

let chatSolicitudActiva = null;
let chatPollingInterval = null;

/* --- CARGAR SELECTS RELACIONADOS --- */
async function cargarOpcionesRelacionadas() {
  try {
    const [resCli, resEmp, resBot, resSer] = await Promise.all([
      fetch(`${API_BASE}/clientes`).catch(() => ({ ok: false })),
      fetch(`${API_BASE}/empleados`).catch(() => ({ ok: false })),
      fetch(`${API_BASE}/chatbots`).catch(() => ({ ok: false })),
      fetch(`${API_BASE}/servicios`).catch(() => ({ ok: false }))
    ]);

    if (resCli.ok) clientesCacheSol = await resCli.json();
    if (resEmp.ok) empleadosCacheSol = await resEmp.json();
    if (resBot.ok) chatbotsCacheSol = await resBot.json();
    if (resSer.ok) serviciosCacheSol = await resSer.json();

    poblarSelect('sol-cliente', clientesCacheSol, c => `${c.nombre} ${c.apellido || ''} (${c.email})`);
    poblarSelect('sol-empleado', empleadosCacheSol, e => `${e.nombre} ${e.apellido || ''} - ${e.cargoEspecifico || e.tipoUsuario}`);
    poblarSelect('sol-chatbot', chatbotsCacheSol, b => `${b.nombre} (v${b.version || '1.0'})`);
    poblarSelect('sol-servicio', serviciosCacheSol, s => `${s.nombre} - $${Number(s.precio).toLocaleString('es-CO')}`);
  } catch (err) {
    console.warn('Error al cargar opciones relacionadas:', err);
  }
}

function poblarSelect(idSelect, lista, fnTexto) {
  const sel = document.getElementById(idSelect);
  if (!sel) return;
  const valActual = sel.value;
  sel.innerHTML = '<option value="">Sin asignar</option>';
  lista.forEach(item => {
    const opt = document.createElement('option');
    opt.value = item.id;
    opt.textContent = fnTexto(item);
    sel.appendChild(opt);
  });
  if (valActual) sel.value = valActual;
}

/* --- CARGAR Y RENDERIZAR TABLA --- */
async function cargarSolicitudes() {
  const tbody = document.getElementById('tabla-solicitudes-body');
  tbody.innerHTML = '<tr><td colspan="7">Cargando...</td></tr>';

  try {
    const resp = await fetch(`${API_BASE}/solicitudes`);
    if (!resp.ok) throw new Error('Error al listar solicitudes');
    solicitudesCache = await resp.json();
    solicitudesCache.sort((a, b) => new Date(b.fechaCreacion || 0) - new Date(a.fechaCreacion || 0));
    renderizarTablaSolicitudes(solicitudesCache);
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">No se pudo conectar con el servidor.</td></tr>';
  }
}

function nombreClientePorId(id) {
  const c = clientesCacheSol.find(c => c.id === id);
  return c ? `${c.nombre} ${c.apellido || ''}`.trim() : (id ? `#${id}` : '\u2013');
}

function nombreEmpleadoPorId(id) {
  const e = empleadosCacheSol.find(e => e.id === id);
  return e ? `${e.nombre} ${e.apellido || ''}`.trim() : (id ? `#${id}` : '\u2013');
}

function renderizarTablaSolicitudes(lista) {
  const tbody = document.getElementById('tabla-solicitudes-body');
  tbody.innerHTML = '';

  if (!lista || lista.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No hay solicitudes registradas.</td></tr>';
    return;
  }

  lista.forEach(s => {
    const tr = document.createElement('tr');
    const badgeClase = s.estado === 'RESUELTA' ? 'badge bg-success' :
                       s.estado === 'EN_PROCESO' ? 'badge bg-info' :
                       s.estado === 'CANCELADA' ? 'badge bg-danger' : 'badge bg-warning text-dark';

    tr.innerHTML = `
      <td><strong>#${s.id}</strong></td>
      <td>${escaparHtml(s.titulo)}</td>
      <td>${escaparHtml(nombreClientePorId(s.clienteId))}</td>
      <td><span class="${badgeClase}">${escaparHtml(s.estado || 'PENDIENTE')}</span></td>
      <td>${escaparHtml(s.prioridad || '')}</td>
      <td>${escaparHtml(nombreEmpleadoPorId(s.empleadoAsignadoId))}</td>
      <td>
        <button type="button" class="btn btn-sm btn-info text-white" data-chat="${s.id}" title="Abrir Chat con el Cliente">
          <i class="bi bi-chat-dots-fill"></i> Chat
        </button>
        <button type="button" class="btn-success btn-sm-admin ms-1" data-editar="${s.id}" title="Editar Solicitud">
          <i class="bi bi-pencil-fill" aria-hidden="true"></i>
        </button>
        <button type="button" class="btn-danger btn-sm-admin ms-1" data-eliminar="${s.id}" title="Eliminar Solicitud">
          <i class="bi bi-trash-fill" aria-hidden="true"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('[data-chat]').forEach(btn => {
    btn.addEventListener('click', () => abrirChatConCliente(Number(btn.dataset.chat)));
  });
  tbody.querySelectorAll('[data-editar]').forEach(btn => {
    btn.addEventListener('click', () => cargarSolicitudEnFormulario(Number(btn.dataset.editar)));
  });
  tbody.querySelectorAll('[data-eliminar]').forEach(btn => {
    btn.addEventListener('click', () => eliminarSolicitud(Number(btn.dataset.eliminar)));
  });
}

function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto == null ? '' : String(texto);
  return div.innerHTML;
}

/* --- FORMULARIO: CARGAR PARA EDITAR --- */
function cargarSolicitudEnFormulario(id) {
  const solicitud = solicitudesCache.find(s => s.id === id);
  if (!solicitud) return;

  document.getElementById('sol-id').value          = solicitud.id;
  document.getElementById('sol-titulo').value       = solicitud.titulo || '';
  document.getElementById('sol-asunto').value       = solicitud.asunto || '';
  document.getElementById('sol-descripcion').value  = solicitud.descripcion || '';
  document.getElementById('sol-estado').value       = solicitud.estado || 'PENDIENTE';
  document.getElementById('sol-prioridad').value    = solicitud.prioridad || 'MEDIA';
  document.getElementById('sol-categoria').value    = solicitud.categoria || '';
  document.getElementById('sol-cliente').value      = solicitud.clienteId || '';
  document.getElementById('sol-empleado').value     = solicitud.empleadoAsignadoId || '';
  document.getElementById('sol-chatbot').value      = solicitud.chatbotAsignadoId || '';
  document.getElementById('sol-servicio').value     = solicitud.servicioGeneradoId || '';

  document.getElementById('form-titulo').textContent = 'Editando solicitud #' + solicitud.id;
  document.getElementById('btn-eliminar').disabled = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function limpiarFormularioSolicitud() {
  document.getElementById('form-solicitud').reset();
  document.getElementById('sol-id').value = '';
  document.getElementById('form-titulo').textContent = 'Nueva solicitud';
  document.getElementById('btn-eliminar').disabled = true;
  ocultarMensaje('msg-admin');
}

async function guardarSolicitud(e) {
  e.preventDefault();
  ocultarMensaje('msg-admin');

  const id = document.getElementById('sol-id').value;
  const esEdicion = !!id;

  const solicitudExistente = esEdicion ? solicitudesCache.find(s => s.id === Number(id)) : null;
  const fechaCreacion = esEdicion
    ? (solicitudExistente ? solicitudExistente.fechaCreacion : new Date().toISOString())
    : new Date().toISOString();

  const dto = {
    fechaCreacion,
    titulo: document.getElementById('sol-titulo').value.trim(),
    asunto: document.getElementById('sol-asunto').value.trim() || null,
    descripcion: document.getElementById('sol-descripcion').value.trim() || null,
    estado: document.getElementById('sol-estado').value,
    prioridad: document.getElementById('sol-prioridad').value,
    categoria: document.getElementById('sol-categoria').value || null,
    clienteId: document.getElementById('sol-cliente').value || null,
    empleadoAsignadoId: document.getElementById('sol-empleado').value || null,
    chatbotAsignadoId: document.getElementById('sol-chatbot').value || null,
    servicioGeneradoId: document.getElementById('sol-servicio').value || null
  };

  if (!dto.titulo) {
    mostrarMensaje('msg-admin', 'El t\u00edtulo es obligatorio.', 'error');
    return;
  }

  const url = esEdicion ? `${API_BASE}/solicitudes/${id}` : `${API_BASE}/solicitudes`;
  const metodo = esEdicion ? 'PUT' : 'POST';

  const btn = document.getElementById('btn-guardar');
  btn.disabled = true;

  try {
    const resp = await fetch(url, {
      method: metodo,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dto)
    });

    if (!resp.ok) {
      const data = await resp.json().catch(() => ({}));
      mostrarMensaje('msg-admin', data.error || data.message || 'No se pudo guardar la solicitud.', 'error');
      return;
    }

    Toast.mostrar(esEdicion ? 'Solicitud actualizada exitosamente.' : 'Solicitud creada con \u00e9xito.', 'ok');
    limpiarFormularioSolicitud();
    await cargarSolicitudes();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function eliminarSolicitud(id) {
  if (!confirm('\u00bfEliminar esta solicitud? Esta acci\u00f3n no se puede deshacer.')) return;

  try {
    const resp = await fetch(`${API_BASE}/solicitudes/${id}`, { method: 'DELETE' });
    if (!resp.ok) {
      mostrarMensaje('msg-admin', 'No se pudo eliminar la solicitud.', 'error');
      return;
    }
    Toast.mostrar('Solicitud eliminada.', 'ok');
    if (document.getElementById('sol-id').value === String(id)) {
      limpiarFormularioSolicitud();
    }
    await cargarSolicitudes();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  }
}

/* --- CHAT CON EL CLIENTE (MODAL ADMIN) --- */
async function abrirChatConCliente(id) {
  const solicitud = solicitudesCache.find(s => s.id === id);
  if (!solicitud) return;

  chatSolicitudActiva = solicitud;

  const modalEl = document.getElementById('modalChatAdmin');
  const modalBs = new bootstrap.Modal(modalEl);

  document.getElementById('chat-admin-subtitulo').textContent = `Solicitud #SOL-${solicitud.id} \u00b7 ${solicitud.titulo}`;
  document.getElementById('chat-admin-cliente').textContent = `Cliente: ${nombreClientePorId(solicitud.clienteId)}`;
  document.getElementById('chat-admin-asunto').textContent = `Asunto: ${solicitud.asunto || solicitud.descripcion || 'Consulta de viaje'}`;

  const selectEstado = document.getElementById('chat-admin-estado-select');
  if (selectEstado) selectEstado.value = solicitud.estado || 'PENDIENTE';

  await cargarMensajesChatAdmin(solicitud.id);
  modalBs.show();

  if (chatPollingInterval) clearInterval(chatPollingInterval);
  chatPollingInterval = setInterval(() => {
    cargarMensajesChatAdmin(solicitud.id, true);
  }, 3500);

  modalEl.addEventListener('hidden.bs.modal', () => {
    if (chatPollingInterval) clearInterval(chatPollingInterval);
    chatSolicitudActiva = null;
  }, { once: true });
}

async function cargarMensajesChatAdmin(solicitudId, mantenerScroll = false) {
  const contenedor = document.getElementById('chat-admin-mensajes');
  if (!contenedor) return;

  // Re-consultar estado actualizado de la solicitud
  try {
    const respSol = await fetch(`${API_BASE}/solicitudes/${solicitudId}`);
    if (respSol.ok) {
      const dataSol = await respSol.json();
      if (dataSol && dataSol.estado) {
        if (chatSolicitudActiva) chatSolicitudActiva.estado = dataSol.estado;
        const selectEstado = document.getElementById('chat-admin-estado-select');
        if (selectEstado) selectEstado.value = dataSol.estado;
      }
    }
  } catch (e) {
    // Mantener estado local
  }

  const esCancelada = chatSolicitudActiva && ['CANCELADA', 'RECHAZADA'].includes(String(chatSolicitudActiva.estado).toUpperCase());
  const btnEnviar = document.getElementById('chat-admin-enviar-btn');
  const inputChat = document.getElementById('chat-admin-input');

  if (esCancelada) {
    if (btnEnviar) btnEnviar.disabled = true;
    if (inputChat) {
      inputChat.disabled = true;
      inputChat.placeholder = '⚠️ Esta solicitud ha sido cancelada por el cliente.';
    }
  } else {
    if (btnEnviar) btnEnviar.disabled = false;
    if (inputChat) {
      inputChat.disabled = false;
      inputChat.placeholder = 'Escribe una respuesta para el cliente...';
    }
  }

  try {
    const resp = await fetch(`${API_BASE}/mensajes/solicitud/${solicitudId}`);
    if (!resp.ok) return;

    const mensajes = await resp.json();
    contenedor.innerHTML = '';

    if (esCancelada) {
      const divCancel = document.createElement('div');
      divCancel.className = 'alert alert-danger border-2 p-2 mb-2 small fw-bold d-flex align-items-center gap-2';
      divCancel.innerHTML = '<i class="bi bi-x-circle-fill fs-5"></i> Solicitud Cancelada por el Cliente. No se pueden enviar más mensajes.';
      contenedor.appendChild(divCancel);
    }

    if (chatSolicitudActiva && chatSolicitudActiva.descripcion) {
      const divInicial = document.createElement('div');
      divInicial.className = 'alert alert-light border p-2 mb-2 small text-muted';
      divInicial.innerHTML = `<strong>📋 Requerimiento Inicial:</strong><br>${escaparHtml(chatSolicitudActiva.descripcion)}`;
      contenedor.appendChild(divInicial);
    }

    if (!mensajes || mensajes.length === 0) {
      const divVacio = document.createElement('div');
      divVacio.className = 'text-center py-4 text-muted small';
      divVacio.innerHTML = '<i class="bi bi-chat-dots" style="font-size:2rem;"></i><p class="mt-2">No hay mensajes en este hilo aún. Escribe el primer mensaje para responder al cliente.</p>';
      contenedor.appendChild(divVacio);
      return;
    }

    mensajes.forEach(m => {
      const esAsesor = (m.remitente || '').toLowerCase().includes('asesor') || (m.remitente || '').toLowerCase().includes('admin');
      const esSistema = (m.remitente || '').toLowerCase().includes('sistema') || m.tipo === 'SISTEMA';

      const divMsg = document.createElement('div');

      if (esSistema) {
        divMsg.className = 'w-100 text-center my-2';
        divMsg.innerHTML = `
          <span class="badge bg-danger-subtle text-danger border border-danger-subtle px-3 py-2 rounded-pill small">
            <i class="bi bi-exclamation-octagon-fill me-1"></i> ${escaparHtml(m.contenido)}
          </span>
        `;
      } else {
        divMsg.className = esAsesor ? 'align-self-end bg-primary text-white p-2 px-3 rounded-3' : 'align-self-start bg-white border p-2 px-3 rounded-3 shadow-sm';
        divMsg.style.maxWidth = '75%';

        const hora = m.fecha ? new Date(m.fecha).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }) : '';

        divMsg.innerHTML = `
          <div class="small fw-bold mb-1 ${esAsesor ? 'text-info' : 'text-primary'}">${escaparHtml(m.remitente || 'Usuario')}</div>
          <div>${escaparHtml(m.contenido)}</div>
          <div class="text-end small opacity-75 mt-1" style="font-size: 10px;">${hora}</div>
        `;
      }
      contenedor.appendChild(divMsg);
    });

    if (!mantenerScroll) {
      contenedor.scrollTop = contenedor.scrollHeight;
    }
  } catch (err) {
    console.warn('Error al cargar mensajes de chat:', err);
  }
}

async function enviarRespuestaAsesor() {
  if (!chatSolicitudActiva) return;

  const input = document.getElementById('chat-admin-input');
  const texto = input.value.trim();
  if (!texto) return;

  const sesion = Sesion.obtener ? Sesion.obtener() : null;
  const nombreAsesor = sesion ? `${sesion.nombre} (Asesor AleLeo Tours)` : 'Asesor AleLeo Tours';
  const btn = document.getElementById('chat-admin-enviar-btn');

  btn.disabled = true;

  try {
    const resp = await fetch(`${API_BASE}/mensajes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        remitente: nombreAsesor,
        contenido: texto,
        tipo: 'TEXTO',
        fecha: new Date().toISOString(),
        solicitudId: chatSolicitudActiva.id
      })
    });

    if (resp.ok) {
      input.value = '';
      await cargarMensajesChatAdmin(chatSolicitudActiva.id);

      const selectEstado = document.getElementById('chat-admin-estado-select');
      if (selectEstado && chatSolicitudActiva.estado === 'PENDIENTE') {
        selectEstado.value = 'EN_PROCESO';
        selectEstado.dispatchEvent(new Event('change'));
      }
    }
  } catch (err) {
    console.error('Error al enviar mensaje:', err);
  } finally {
    btn.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('form-solicitud');
  if (!form) return;

  const sesion = Sesion.obtener ? Sesion.obtener() : null;
  if (!sesion || sesion.tipoUsuario !== 'ADMINISTRADOR') {
    Toast.mostrar('Acceso solo para administradores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1200);
    return;
  }

  form.addEventListener('submit', guardarSolicitud);
  document.getElementById('btn-limpiar').addEventListener('click', limpiarFormularioSolicitud);
  document.getElementById('btn-eliminar').addEventListener('click', () => {
    const id = document.getElementById('sol-id').value;
    if (id) eliminarSolicitud(Number(id));
  });

  const chatEnviarBtn = document.getElementById('chat-admin-enviar-btn');
  const chatInput = document.getElementById('chat-admin-input');
  if (chatEnviarBtn) chatEnviarBtn.addEventListener('click', enviarRespuestaAsesor);
  if (chatInput) {
    chatInput.addEventListener('keydown', e => {
      if (e.key === 'Enter') enviarRespuestaAsesor();
    });
  }

  const selectEstadoChat = document.getElementById('chat-admin-estado-select');
  if (selectEstadoChat) {
    selectEstadoChat.addEventListener('change', async function () {
      if (!chatSolicitudActiva) return;
      const nuevoEstado = this.value;
      try {
        await fetch(`${API_BASE}/solicitudes/${chatSolicitudActiva.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            ...chatSolicitudActiva,
            estado: nuevoEstado
          })
        });
        chatSolicitudActiva.estado = nuevoEstado;
        Toast.mostrar(`Estado de solicitud actualizado a ${nuevoEstado}.`, 'ok');
        await cargarSolicitudes();
      } catch (err) {
        console.warn('Error al actualizar estado:', err);
      }
    });
  }

  await cargarOpcionesRelacionadas();
  await cargarSolicitudes();
});