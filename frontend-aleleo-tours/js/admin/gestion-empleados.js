/* ==========================================================================
   AleLeo Tours - gestion-empleados.js
   CRUD de Empleados contra el backend SACE (GET/POST/PUT/DELETE /api/empleados)
   ========================================================================== */

'use strict';

let empleadosCache = [];

async function cargarEmpleados() {
  const tbody = document.getElementById('tabla-empleados-body');
  tbody.innerHTML = '<tr><td colspan="7">Cargando...</td></tr>';

  try {
    const resp = await fetch(`${API_BASE}/empleados`);
    if (!resp.ok) throw new Error('Error al listar empleados');
    empleadosCache = await resp.json();
    renderizarTablaEmpleados(empleadosCache);
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">No se pudo conectar con el servidor.</td></tr>';
  }
}

function renderizarTablaEmpleados(lista) {
  const tbody = document.getElementById('tabla-empleados-body');
  tbody.innerHTML = '';

  if (!lista || lista.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No hay empleados registrados.</td></tr>';
    return;
  }

  lista.forEach(e => {
    const disp = e.estadoDisponibilidad || 'DISPONIBLE';
    const acceso = e.estadoAcceso || e.estado || 'ACTIVA';
    const dispClass = disp === 'DISPONIBLE' ? 'bg-success text-white' : 'bg-warning text-dark';
    const accesoClass = (acceso === 'ACTIVA' || acceso === 'ACTIVO') ? 'bg-success-subtle text-success border border-success' : 'bg-danger-subtle text-danger border border-danger';

    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${e.id}</td>
      <td>${escaparHtml(e.nombre)} ${escaparHtml(e.apellido || '')}</td>
      <td>${escaparHtml(e.email)}</td>
      <td>${escaparHtml(e.departamento || 'Atención')}</td>
      <td>${escaparHtml(e.cargoEspecifico || e.tipoUsuario || '')}</td>
      <td>
        <span class="badge ${dispClass} rounded-pill px-2 py-1">${disp}</span>
        <span class="badge ${accesoClass} rounded-pill px-2 py-1 ms-1">${acceso}</span>
      </td>
      <td>
        <button type="button" class="btn-success btn-sm-admin" data-editar="${e.id}" title="Editar" aria-label="Editar empleado">
          <i class="bi bi-pencil-fill" aria-hidden="true"></i>
        </button>
        <button type="button" class="btn-danger btn-sm-admin" data-eliminar="${e.id}" title="Eliminar" aria-label="Eliminar empleado">
          <i class="bi bi-trash-fill" aria-hidden="true"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  tbody.querySelectorAll('[data-editar]').forEach(btn => {
    btn.addEventListener('click', () => cargarEmpleadoEnFormulario(Number(btn.dataset.editar)));
  });
  tbody.querySelectorAll('[data-eliminar]').forEach(btn => {
    btn.addEventListener('click', () => eliminarEmpleado(Number(btn.dataset.eliminar)));
  });
}

function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto == null ? '' : String(texto);
  return div.innerHTML;
}

function cargarEmpleadoEnFormulario(id) {
  const empleado = empleadosCache.find(e => e.id === id);
  if (!empleado) return;

  document.getElementById('emp-id').value = empleado.id;
  if (document.getElementById('emp-nombre')) document.getElementById('emp-nombre').value = empleado.nombre || '';
  if (document.getElementById('emp-apellido')) document.getElementById('emp-apellido').value = empleado.apellido || '';
  if (document.getElementById('emp-email')) document.getElementById('emp-email').value = empleado.email || '';
  if (document.getElementById('emp-telefono')) document.getElementById('emp-telefono').value = empleado.telefono || '';
  if (document.getElementById('emp-departamento')) document.getElementById('emp-departamento').value = empleado.departamento || '';
  if (document.getElementById('emp-cargo')) document.getElementById('emp-cargo').value = empleado.cargoEspecifico || '';
  if (document.getElementById('emp-disponibilidad')) document.getElementById('emp-disponibilidad').value = empleado.estadoDisponibilidad || 'DISPONIBLE';
  if (document.getElementById('emp-tipo-usuario')) document.getElementById('emp-tipo-usuario').value = empleado.tipoUsuario || 'ASESOR_VIAJES';
  if (document.getElementById('emp-estado-acceso')) document.getElementById('emp-estado-acceso').value = empleado.estadoAcceso || empleado.estado || 'ACTIVA';
  if (document.getElementById('emp-tipo-documento')) document.getElementById('emp-tipo-documento').value = empleado.tipoDocumento || 'CC';
  if (document.getElementById('emp-numero-documento')) document.getElementById('emp-numero-documento').value = empleado.numeroDocumento || '';

  const inPass = document.getElementById('emp-contrasenia');
  if (inPass) inPass.value = '';

  document.getElementById('form-titulo').textContent = 'Editando empleado #' + empleado.id;
  document.getElementById('btn-eliminar').disabled = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function limpiarFormularioEmpleado() {
  document.getElementById('form-empleado').reset();
  document.getElementById('emp-id').value = '';
  document.getElementById('form-titulo').textContent = 'Nuevo empleado';
  document.getElementById('btn-eliminar').disabled = true;
  ocultarMensaje('msg-admin');
}

async function guardarEmpleado(e) {
  e.preventDefault();
  ocultarMensaje('msg-admin');

  const id = document.getElementById('emp-id').value;
  const esEdicion = !!id;

  const dto = {
    nombre:               document.getElementById('emp-nombre')?.value.trim() || '',
    apellido:             document.getElementById('emp-apellido')?.value.trim() || '',
    email:                document.getElementById('emp-email')?.value.trim().toLowerCase() || '',
    telefono:             document.getElementById('emp-telefono')?.value.trim() || '',
    departamento:         document.getElementById('emp-departamento')?.value.trim() || '',
    cargoEspecifico:      document.getElementById('emp-cargo')?.value.trim() || '',
    estadoDisponibilidad: document.getElementById('emp-disponibilidad')?.value || 'DISPONIBLE',
    tipoDocumento:        document.getElementById('emp-tipo-documento')?.value || 'CC',
    numeroDocumento:      document.getElementById('emp-numero-documento')?.value.trim() || ''
  };

  const pass = document.getElementById('emp-contrasenia')?.value;
  if (pass) dto.contrasenia = pass;

  if (!dto.nombre || !dto.email) {
    mostrarMensaje('msg-admin', 'Nombre y correo son obligatorios.', 'error');
    return;
  }
  if (!esEdicion && !pass) {
    mostrarMensaje('msg-admin', 'La contraseña es obligatoria al crear un empleado.', 'error');
    return;
  }
  if (pass && typeof validarFortalezaContrasenia === 'function') {
    const valPass = validarFortalezaContrasenia(pass);
    if (!valPass.esValida) {
      mostrarMensaje('msg-admin', valPass.mensajeError, 'error');
      return;
    }
  }

  const url = esEdicion ? `${API_BASE}/empleados/${id}` : `${API_BASE}/empleados`;
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
      mostrarMensaje('msg-admin', data.error || data.message || 'No se pudo guardar el empleado.', 'error');
      return;
    }

    Toast.mostrar(esEdicion ? 'Empleado actualizado exitosamente.' : 'Empleado creado con éxito.', 'ok');
    limpiarFormularioEmpleado();
    await cargarEmpleados();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  } finally {
    btn.disabled = false;
  }
}

async function eliminarEmpleado(id) {
  if (!confirm('\u00bfEliminar este empleado? Esta acci\u00f3n no se puede deshacer.')) return;

  try {
    const resp = await fetch(`${API_BASE}/empleados/${id}`, { method: 'DELETE' });
    if (!resp.ok) {
      mostrarMensaje('msg-admin', 'No se pudo eliminar el empleado.', 'error');
      return;
    }
    Toast.mostrar('Empleado eliminado.', 'ok');
    if (document.getElementById('emp-id').value === String(id)) {
      limpiarFormularioEmpleado();
    }
    await cargarEmpleados();
  } catch (err) {
    mostrarMensaje('msg-admin', 'No se pudo conectar con el servidor.', 'error');
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('form-empleado');
  if (!form) return;

  const sesion = Sesion.obtener();
  if (!sesion || sesion.tipoUsuario !== 'ADMINISTRADOR') {
    Toast.mostrar('Acceso solo para administradores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1200);
    return;
  }

  form.addEventListener('submit', guardarEmpleado);
  document.getElementById('btn-limpiar').addEventListener('click', limpiarFormularioEmpleado);
  document.getElementById('btn-eliminar').addEventListener('click', () => {
    const id = document.getElementById('emp-id').value;
    if (id) eliminarEmpleado(Number(id));
  });

  await cargarEmpleados();
});