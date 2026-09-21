/* ==========================================================================
   AleLeo Tours - gestion-clientes.js
   CRUD de Clientes contra el backend SACE (GET/POST/PUT/DELETE /api/clientes)
   ========================================================================== */

'use strict';

let clientesCache = [];

async function cargarClientes() {
  const tbody = document.getElementById('tabla-clientes-body');
  tbody.innerHTML = '<tr><td colspan="7">Cargando...</td></tr>';

  try {
    const resp = await fetch(`${API_BASE}/clientes`);
    if (!resp.ok) throw new Error('Error al listar clientes');
    clientesCache = await resp.json();
    renderizarTablaClientes(clientesCache);
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">No se pudo conectar con el servidor.</td></tr>';
  }
}

function renderizarTablaClientes(lista) {
  const tbody = document.getElementById('tabla-clientes-body');
  tbody.innerHTML = '';

  if (!lista || lista.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No hay clientes registrados.</td></tr>';
    return;
  }

  lista.forEach(c => {
    const estado = c.estadoAcceso || c.estado || 'ACTIVA';
    let estadoClass = 'bg-success-subtle text-success border border-success';
    if (estado === 'INACTIVA' || estado === 'INACTIVO') {
      estadoClass = 'bg-warning-subtle text-warning border border-warning';
    } else if (estado === 'BLOQUEADA' || estado === 'BLOQUEADO') {
      estadoClass = 'bg-danger-subtle text-danger border border-danger';
    }

    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold">${c.id}</td>
      <td>${escaparHtml(c.nombre)} ${escaparHtml(c.apellido || '')}</td>
      <td>${escaparHtml(c.email)}</td>
      <td>${escaparHtml(c.telefono || '–')}</td>
      <td><span class="badge bg-secondary-subtle text-dark border border-secondary">${escaparHtml(c.tipoUsuario || 'CLIENTE')}</span></td>
      <td><span class="badge ${estadoClass} rounded-pill px-2 py-1">${escaparHtml(estado)}</span></td>
      <td>
        <button type="button" class="btn-success btn-sm-admin" data-editar="${c.id}" title="Editar">
          <i class="bi bi-pencil-fill" aria-hidden="true"></i>
        </button>
        <button type="button" class="btn-danger btn-sm-admin" data-eliminar="${c.id}" title="Eliminar">
          <i class="bi bi-trash-fill" aria-hidden="true"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('[data-editar]').forEach(btn => {
    btn.addEventListener('click', () => cargarClienteEnFormulario(Number(btn.dataset.editar)));
  });
  tbody.querySelectorAll('[data-eliminar]').forEach(btn => {
    btn.addEventListener('click', () => eliminarCliente(Number(btn.dataset.eliminar)));
  });
}

function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto == null ? '' : String(texto);
  return div.innerHTML;
}

function cargarClienteEnFormulario(id) {
  const cliente = clientesCache.find(c => c.id === id);
  if (!cliente) return;

  const el = (elementId) => document.getElementById(elementId);

  if (el('cli-id')) el('cli-id').value = cliente.id;
  if (el('cli-nombre')) el('cli-nombre').value = cliente.nombre || '';
  if (el('cli-apellido')) el('cli-apellido').value = cliente.apellido || '';
  if (el('cli-email')) el('cli-email').value = cliente.email || '';
  if (el('cli-telefono')) el('cli-telefono').value = cliente.telefono || '';
  if (el('cli-tipo-usuario')) el('cli-tipo-usuario').value = cliente.tipoUsuario || 'CLIENTE';
  if (el('cli-estado-acceso')) el('cli-estado-acceso').value = cliente.estadoAcceso || cliente.estado || 'ACTIVA';
  if (el('cli-tipo-documento')) el('cli-tipo-documento').value = cliente.tipoDocumento || '';
  if (el('cli-numero-documento')) el('cli-numero-documento').value = cliente.numeroDocumento || '';
  if (el('cli-historial')) el('cli-historial').value = cliente.historialConsultas || cliente.historial || '';
  if (el('cli-preferencias')) el('cli-preferencias').value = cliente.preferenciasComunicacion || cliente.preferencias || '';

  const inPass = el('cli-password');
  if (inPass) inPass.value = '';

  if (el('form-titulo')) el('form-titulo').textContent = 'Editando cliente #' + cliente.id;
  if (el('btn-eliminar')) el('btn-eliminar').disabled = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function limpiarFormularioCliente() {
  const form = document.getElementById('form-cliente');
  if (form) form.reset();
  const el = (elementId) => document.getElementById(elementId);
  if (el('cli-id')) el('cli-id').value = '';
  if (el('form-titulo')) el('form-titulo').textContent = 'Registrar o Modificar Cliente';
  if (el('btn-eliminar')) el('btn-eliminar').disabled = true;
  ocultarMensaje('msg-admin');
}

async function guardarCliente(e) {
  e.preventDefault();
  ocultarMensaje('msg-admin');

  const el = (elementId) => document.getElementById(elementId);
  const id = el('cli-id')?.value;
  const esEdicion = !!id;

  const dto = {
    nombre: el('cli-nombre')?.value.trim() || '',
    apellido: el('cli-apellido')?.value.trim() || '',
    email: el('cli-email')?.value.trim().toLowerCase() || '',
    telefono: el('cli-telefono')?.value.trim() || '',
    tipoDocumento: el('cli-tipo-documento')?.value || '',
    numeroDocumento: el('cli-numero-documento')?.value.trim() || '',
    historialConsultas: el('cli-historial')?.value.trim() || '',
    preferenciasComunicacion: el('cli-preferencias')?.value.trim() || ''
  };

  const pass = el('cli-password')?.value;
  if (pass) dto.contrasenia = pass;

  if (!dto.nombre || !dto.email) {
    mostrarMensaje('msg-admin', 'Nombre y correo son obligatorios.', 'error');
    return;
  }
  if (!esEdicion && !pass) {
    mostrarMensaje('msg-admin', 'La contraseña es obligatoria al crear un cliente.', 'error');
    return;
  }
  if (pass && typeof validarFortalezaContrasenia === 'function') {
    const valPass = validarFortalezaContrasenia(pass);
    if (!valPass.esValida) {
      mostrarMensaje('msg-admin', valPass.mensajeError, 'error');
      return;
    }
  }

  const url = esEdicion ? `${API_BASE}/clientes/${id}` : `${API_BASE}/clientes`;
  const metodo = esEdicion ? 'PUT' : 'POST';

  const btn = el('btn-guardar');
  if (btn) btn.disabled = true;

  try {
    const resp = await fetch(url, {
      method: metodo,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dto)
    });

    if (!resp.ok) {
      const data = await resp.json().catch(() => ({}));
      mostrarMensaje('msg-admin', data.error || data.message || 'No se pudo guardar el cliente.', 'error');
      return;
    }

    Toast.mostrar(esEdicion ? 'Cliente actualizado exitosamente.' : 'Cliente creado con éxito.', 'ok');
    limpiarFormularioCliente();
    await cargarClientes();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  } finally {
    if (btn) btn.disabled = false;
  }
}

async function eliminarCliente(id) {
  if (!confirm('\u00bfEliminar este cliente? Esta acci\u00f3n no se puede deshacer.')) return;

  try {
    const resp = await fetch(`${API_BASE}/clientes/${id}`, { method: 'DELETE' });
    if (!resp.ok) {
      mostrarMensaje('msg-admin', 'No se pudo eliminar el cliente.', 'error');
      return;
    }
    Toast.mostrar('Cliente eliminado.', 'ok');
    if (document.getElementById('cli-id').value === String(id)) {
      limpiarFormularioCliente();
    }
    await cargarClientes();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('form-cliente');
  if (!form) return;

  const sesion = Sesion.obtener();
  if (!sesion || sesion.tipoUsuario !== 'ADMINISTRADOR') {
    Toast.mostrar('Acceso solo para administradores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1200);
    return;
  }

  form.addEventListener('submit', guardarCliente);
  document.getElementById('btn-limpiar').addEventListener('click', limpiarFormularioCliente);
  document.getElementById('btn-eliminar').addEventListener('click', () => {
    const id = document.getElementById('cli-id').value;
    if (id) eliminarCliente(Number(id));
  });

  await cargarClientes();
});