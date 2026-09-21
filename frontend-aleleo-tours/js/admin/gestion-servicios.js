/* ==========================================================================
   AleLeo Tours - gestion-servicios.js
   CRUD de Servicios contra el backend SACE (GET/POST/PUT/DELETE /api/servicios)
   ========================================================================== */

'use strict';

let serviciosCache = [];

async function cargarServicios() {
  const tbody = document.getElementById('tabla-servicios-body');
  tbody.innerHTML = '<tr><td colspan="7">Cargando...</td></tr>';

  try {
    const resp = await fetch(`${API_BASE}/servicios`);
    if (!resp.ok) throw new Error('Error al listar servicios');
    serviciosCache = await resp.json();
    renderizarTablaServicios(serviciosCache);
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">No se pudo conectar con el servidor.</td></tr>';
  }
}

function renderizarTablaServicios(lista) {
  const tbody = document.getElementById('tabla-servicios-body');
  tbody.innerHTML = '';

  if (!lista || lista.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No hay servicios registrados.</td></tr>';
    return;
  }

  lista.forEach(s => {
    const precioFmt = s.precio != null
      ? '$ ' + Number(s.precio).toLocaleString('es-CO') + ' COP'
      : '–';

    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold">${s.id}</td>
      <td>${escaparHtml(s.nombre)}</td>
      <td><span class="badge bg-secondary-subtle text-dark border border-secondary">${escaparHtml(s.tipoServicio || 'PAQUETE')}</span></td>
      <td>${escaparHtml(s.destino || '–')}</td>
      <td>${escaparHtml(s.duracion || '–')}</td>
      <td class="fw-bold text-success">${precioFmt}</td>
      <td>
        <button type="button" class="btn-success btn-sm-admin" data-editar="${s.id}" title="Editar">
          <i class="bi bi-pencil-fill" aria-hidden="true"></i>
        </button>
        <button type="button" class="btn-danger btn-sm-admin" data-eliminar="${s.id}" title="Eliminar">
          <i class="bi bi-trash-fill" aria-hidden="true"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('[data-editar]').forEach(btn => {
    btn.addEventListener('click', () => cargarServicioEnFormulario(Number(btn.dataset.editar)));
  });
  tbody.querySelectorAll('[data-eliminar]').forEach(btn => {
    btn.addEventListener('click', () => eliminarServicio(Number(btn.dataset.eliminar)));
  });
}

function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto == null ? '' : String(texto);
  return div.innerHTML;
}

function cargarServicioEnFormulario(id) {
  const servicio = serviciosCache.find(s => s.id === id);
  if (!servicio) return;

  document.getElementById('srv-id').value          = servicio.id;
  document.getElementById('srv-nombre').value      = servicio.nombre || '';
  document.getElementById('srv-descripcion').value = servicio.descripcion || '';
  document.getElementById('srv-tipo').value        = servicio.tipoServicio || 'PAQUETE_TODO_INCLUIDO';
  document.getElementById('srv-precio').value      = servicio.precio != null ? servicio.precio : '';
  document.getElementById('srv-destino').value     = servicio.destino || '';
  document.getElementById('srv-duracion').value    = servicio.duracion || '';

  document.getElementById('form-titulo').textContent = 'Editando servicio #' + servicio.id;
  document.getElementById('btn-eliminar').disabled = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function limpiarFormularioServicio() {
  document.getElementById('form-servicio').reset();
  document.getElementById('srv-id').value = '';
  document.getElementById('form-titulo').textContent = 'Nuevo servicio';
  document.getElementById('btn-eliminar').disabled = true;
  ocultarMensaje('msg-admin');
}

async function guardarServicio(e) {
  e.preventDefault();
  ocultarMensaje('msg-admin');

  const id = document.getElementById('srv-id').value;
  const esEdicion = !!id;

  const precioNum = parseFloat(document.getElementById('srv-precio').value);

  const dto = {
    nombre:       document.getElementById('srv-nombre').value.trim(),
    descripcion:  document.getElementById('srv-descripcion').value.trim(),
    tipoServicio: document.getElementById('srv-tipo').value,
    precio:       isNaN(precioNum) ? 0 : precioNum,
    destino:      document.getElementById('srv-destino').value.trim(),
    duracion:     document.getElementById('srv-duracion').value.trim()
  };

  if (!dto.nombre) {
    mostrarMensaje('msg-admin', 'El nombre del servicio es obligatorio.', 'error');
    return;
  }
  if (dto.precio < 0) {
    mostrarMensaje('msg-admin', 'El precio no puede ser negativo.', 'error');
    return;
  }

  const url = esEdicion ? `${API_BASE}/servicios/${id}` : `${API_BASE}/servicios`;
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
      mostrarMensaje('msg-admin', data.error || data.message || 'No se pudo guardar el servicio.', 'error');
      return;
    }

    Toast.mostrar(esEdicion ? 'Servicio actualizado exitosamente.' : 'Servicio creado con \u00e9xito.', 'ok');
    limpiarFormularioServicio();
    await cargarServicios();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function eliminarServicio(id) {
  if (!confirm('\u00bfEliminar este servicio? Esta acci\u00f3n no se puede deshacer.')) return;

  try {
    const resp = await fetch(`${API_BASE}/servicios/${id}`, { method: 'DELETE' });
    if (!resp.ok) {
      mostrarMensaje('msg-admin', 'No se pudo eliminar el servicio.', 'error');
      return;
    }
    Toast.mostrar('Servicio eliminado.', 'ok');
    if (document.getElementById('srv-id').value === String(id)) {
      limpiarFormularioServicio();
    }
    await cargarServicios();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('form-servicio');
  if (!form) return;

  const sesion = Sesion.obtener();
  if (!sesion || sesion.tipoUsuario !== 'ADMINISTRADOR') {
    Toast.mostrar('Acceso solo para administradores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1200);
    return;
  }

  form.addEventListener('submit', guardarServicio);
  document.getElementById('btn-limpiar').addEventListener('click', limpiarFormularioServicio);
  document.getElementById('btn-eliminar').addEventListener('click', () => {
    const id = document.getElementById('srv-id').value;
    if (id) eliminarServicio(Number(id));
  });

  await cargarServicios();
});