/* ==========================================================================
   AleLeo Tours - app.js
   Modulos: Sesion - Toast - Navegacion - Busqueda
   ========================================================================== */

'use strict';

const API_BASE = 'http://localhost:8082/api';

// Escapa caracteres HTML para evitar inyeccion (XSS) al renderizar datos que
// vienen del usuario o del backend. Se usa en panel-empleado.js y destinos.js
// (los otros modulos de gestion ya muestran datos de forma segura).
function escaperHtml(texto) {
  const mapa = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
    '/': '&#47;'
  };
  return String(texto == null ? '' : texto).replace(/[&<>"'/]/g, c => mapa[c]);
}

// Inyecta el token de sesion en todas las llamadas al API y redirige a login
// cuando el backend responde 401 (token vencido o invalido). Definido al cargar
// app.js, por lo que aplica a T O D O S los fetch del frontend.
(function protegerFetch() {
  const fetchOriginal = window.fetch.bind(window);
  window.fetch = async function (input, init) {
    const url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
    const sesion = Sesion.obtener();
    if (sesion && sesion.token && String(url).startsWith(API_BASE)) {
      init = init || {};
      init.headers = init.headers || {};
      if (!init.headers.Authorization) {
        init.headers.Authorization = 'Bearer ' + sesion.token;
      }
    }
    const resp = await fetchOriginal(input, init);
    if (resp && resp.status === 401) {
      const enLogin = /login(\.html)?$/.test(window.location.pathname);
      Sesion.cerrar();
      Sesion.actualizar();
      if (!enLogin) {
        window.location.href = 'login.html';
      }
    }
    return resp;
  };
})();

const Sesion = (() => {
  const KEY = 'onvacation_sesion';

  function obtener() {
    try { return JSON.parse(sessionStorage.getItem(KEY)); } catch { return null; }
  }

  function guardar(datos) {
    sessionStorage.setItem(KEY, JSON.stringify(datos));
  }

  function cerrar() {
    sessionStorage.removeItem(KEY);
  }

  function actualizar() {
    const sesion = obtener();
    const elSesion  = document.getElementById('nav-sesion');
    const elRegistro= document.getElementById('nav-registro');
    const elNombre  = document.getElementById('nav-nombre');
    const elSalir   = document.getElementById('nav-salir');
    const elMisReservas = document.getElementById('nav-mis-solicitudes');
    const elPanelEmpleado = document.getElementById('nav-panel-empleado');

    const elsAdmin  = [
      document.getElementById('nav-gestion-clientes'),
      document.getElementById('nav-gestion-empleados'),
      document.getElementById('nav-gestion-servicios'),
      document.getElementById('nav-gestion-solicitudes')
    ];

    const elReportes = document.getElementById('nav-reportes');
    const elDropdownAdmin = document.getElementById('nav-gestion-dropdown');

    if (sesion) {
      if (elSesion)   elSesion.style.display   = 'none';
      if (elRegistro) elRegistro.style.display  = 'none';
      if (elMisReservas) elMisReservas.style.display = sesion.tipoUsuario === 'CLIENTE' ? '' : 'none';
      if (elPanelEmpleado) {
        const esEmpleado = sesion.tipoUsuario && sesion.tipoUsuario !== 'CLIENTE';
        elPanelEmpleado.style.display = esEmpleado ? '' : 'none';
      }
      if (elReportes) {
        elReportes.style.display = sesion.tipoUsuario === 'ADMINISTRADOR' ? '' : 'none';
      }
      if (elDropdownAdmin) {
        if (sesion.tipoUsuario === 'ADMINISTRADOR') {
          elDropdownAdmin.classList.remove('d-none');
          elDropdownAdmin.style.display = 'inline-block';
        } else {
          elDropdownAdmin.classList.add('d-none');
          elDropdownAdmin.style.display = 'none';
        }
      }
      if (elNombre) {
        elNombre.innerHTML = '<i class="bi bi-person-fill" aria-hidden="true"></i> ' + (sesion.nombre ? sesion.nombre.split(' ')[0] : 'Usuario');
        elNombre.style.display = '';
      }
      if (elSalir) elSalir.style.display = '';
      elsAdmin.forEach(el => { if (el) el.style.display = (sesion.tipoUsuario === 'ADMINISTRADOR' && !elDropdownAdmin) ? '' : 'none'; });
    } else {
      if (elSesion)   elSesion.style.display   = '';
      if (elRegistro) elRegistro.style.display  = '';
      if (elMisReservas) elMisReservas.style.display = 'none';
      if (elPanelEmpleado) elPanelEmpleado.style.display = 'none';
      if (elReportes) elReportes.style.display = 'none';
      if (elDropdownAdmin) {
        elDropdownAdmin.classList.add('d-none');
        elDropdownAdmin.style.display = 'none';
      }
      if (elNombre)   elNombre.style.display    = 'none';
      if (elSalir)    elSalir.style.display     = 'none';
      elsAdmin.forEach(el => { if (el) el.style.display = 'none'; });
    }
  }

  return { obtener, guardar, cerrar, actualizar };
})();

const Toast = (() => {
  function mostrar(texto, tipo = 'info') {
    let contenedor = document.getElementById('toast-container');
    if (!contenedor) {
      contenedor = document.createElement('div');
      contenedor.id = 'toast-container';
      contenedor.setAttribute('aria-live', 'polite');
      contenedor.setAttribute('aria-atomic', 'true');
      document.body.appendChild(contenedor);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${tipo}`;
    toast.textContent = texto;
    toast.setAttribute('role', 'status');
    contenedor.appendChild(toast);

    setTimeout(() => {
      toast.remove();
    }, 3500);
  }

  return { mostrar };
})();

function mostrarMensaje(idEl, texto, tipo) {
  const el = document.getElementById(idEl);
  if (!el) return;
  el.textContent = texto;
  el.className = 'mensaje ' + (tipo === 'error' ? 'mensaje-error' : 'mensaje-ok');
  el.style.display = 'block';
  el.setAttribute('role', 'alert');
}

function ocultarMensaje(idEl) {
  const el = document.getElementById(idEl);
  if (!el) return;
  el.style.display = 'none';
}

function inicializarNavToggle() {
  const btn = document.querySelector('.nav-toggle');
  const nav = document.getElementById('nav-principal');
  if (!btn || !nav) return;

  btn.addEventListener('click', () => {
    const abierto = nav.classList.toggle('abierto');
    btn.setAttribute('aria-expanded', String(abierto));
    btn.innerHTML = abierto ? '<i class="bi bi-x-lg"></i>' : '<i class="bi bi-list"></i>';
  });

  document.addEventListener('click', e => {
    if (nav.classList.contains('abierto') && !nav.contains(e.target) && !btn.contains(e.target)) {
      nav.classList.remove('abierto');
      btn.setAttribute('aria-expanded', 'false');
      btn.innerHTML = '<i class="bi bi-list"></i>';
    }
  });
}

function inicializarNavScroll() {
  const header = document.querySelector('.franja');
  if (!header) return;

  let ultimoScrollY = window.scrollY;
  let ticking = false;

  window.addEventListener('scroll', () => {
    if (!ticking) {
      window.requestAnimationFrame(() => {
        const actualScrollY = window.scrollY;
        const navMovilAbierta = document.getElementById('nav-principal')?.classList.contains('abierto');

        if (actualScrollY > 80 && actualScrollY > ultimoScrollY && !navMovilAbierta) {
          header.classList.add('nav-oculta');
        } else if (actualScrollY < ultimoScrollY || actualScrollY <= 80) {
          header.classList.remove('nav-oculta');
        }

        ultimoScrollY = actualScrollY;
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });
}

function inicializarCerrarSesion() {
  const btnSalir = document.getElementById('nav-salir');
  if (!btnSalir) return;

  btnSalir.addEventListener('click', () => {
    Sesion.cerrar();
    Sesion.actualizar();
    Toast.mostrar('Sesi\u00f3n finalizada. \u00a1Hasta pronto!', 'info');
    setTimeout(() => { window.location.href = 'index.html'; }, 900);
  });
}

function inicializarLogin() {
  const form = document.getElementById('form-login');
  if (!form) return;

  form.addEventListener('submit', async e => {
    e.preventDefault();
    ocultarMensaje('msg-login');

    const email    = document.getElementById('login-correo').value.trim().toLowerCase();
    const password = document.getElementById('login-password').value;

    if (!email || !password) {
      mostrarMensaje('msg-login', 'Por favor ingresa tu correo y contrase\u00f1a.', 'error');
      return;
    }

    const btnSubmit = form.querySelector('button[type="submit"]');
    if (btnSubmit) {
      btnSubmit.disabled = true;
      btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Verificando...';
    }

    try {
      const resp = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, contrasenia: password })
      });

      const data = await resp.json().catch(() => ({}));

      if (!resp.ok) {
        mostrarMensaje('msg-login', data.error || 'Correo o contraseña incorrectos.', 'error');
        return;
      }

      // Validar coincidencia entre la pestaña activa y el perfil devuelto por el servidor
      const tabEmpleado = document.getElementById('tab-login-empleado');
      const esTabEmpleado = tabEmpleado && tabEmpleado.getAttribute('aria-selected') === 'true';

      if (esTabEmpleado && data.tipoUsuario === 'CLIENTE') {
        mostrarMensaje('msg-login', 'Acceso denegado: Esta cuenta pertenece a un Cliente. Por favor selecciona la pestaña "Cliente" para ingresar.', 'error');
        return;
      }

      if (!esTabEmpleado && data.tipoUsuario !== 'CLIENTE') {
        mostrarMensaje('msg-login', 'Acceso denegado: Esta cuenta es de Empleado/Administrador. Por favor selecciona la pestaña "Empleado" para ingresar.', 'error');
        return;
      }

      // El backend ya devuelve el estado real de la cuenta (antes esta rama leia
      // un campo que nunca llegaba y siempre caia en el default 'ACTIVA'). Como
      // ademas el servidor rechaza con 401 las cuentas inactivas/bloqueadas, este
      // bloque es una defensa extra: cualquier cuenta que no este ACTIVA no entra.
      const estadoAcceso = (data.estadoAcceso || 'ACTIVA').toUpperCase();
      if (estadoAcceso !== 'ACTIVA') {
        mostrarMensaje('msg-login', 'Tu cuenta esta inactiva o bloqueada. Contacta al administrador.', 'error');
        return;
      }

      mostrarMensaje('msg-login', '¡Bienvenido! Redirigiendo...', 'ok');
      Sesion.guardar({
        id: data.id,
        nombre: `${data.nombre} ${data.apellido || ''}`.trim(),
        correo: data.email,
        token: data.token,
        tipoUsuario: data.tipoUsuario,
        estadoDisponibilidad: data.estadoDisponibilidad || 'DISPONIBLE',
        estadoAcceso: estadoAcceso,
        departamento: data.departamento || 'Atención al Cliente',
        cargoEspecifico: data.cargoEspecifico || (data.tipoUsuario === 'ADMINISTRADOR' ? 'Administrador SACE' : 'Asesor de Viajes')
      });

      Toast.mostrar(`¡Bienvenido de nuevo, ${data.nombre}!`, 'ok');

      setTimeout(() => {
        if (data.tipoUsuario === 'ADMINISTRADOR') {
          window.location.href = 'gestion-solicitudes.html';
        } else if (data.tipoUsuario === 'CLIENTE') {
          window.location.href = 'mis-solicitudes.html';
        } else {
          window.location.href = 'panel-empleado.html';
        }
      }, 1000);
    } catch (err) {
      mostrarMensaje('msg-login', 'No se pudo conectar con el servidor SACE.', 'error');
    } finally {
      if (btnSubmit) {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = '<i class="bi bi-box-arrow-in-right" aria-hidden="true"></i> Iniciar Sesi\u00f3n';
      }
    }
  });

  agregarTogglePassword('login-password');
}

function validarFortalezaContrasenia(password) {
  const minLongitud = (password || '').length >= 8;
  const tieneMayuscula = /[A-Z]/.test(password || '');
  const tieneMinuscula = /[a-z]/.test(password || '');
  const tieneNumero = /[0-9]/.test(password || '');
  const tieneEspecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password || '');

  const esValida = minLongitud && tieneMayuscula && tieneMinuscula && tieneNumero && tieneEspecial;

  let mensajeError = '';
  if (!minLongitud) mensajeError = 'La contraseña debe tener al menos 8 caracteres.';
  else if (!tieneMayuscula) mensajeError = 'La contraseña debe incluir al menos una letra mayúscula (A-Z).';
  else if (!tieneMinuscula) mensajeError = 'La contraseña debe incluir al menos una letra minúscula (a-z).';
  else if (!tieneNumero) mensajeError = 'La contraseña debe incluir al menos un número (0-9).';
  else if (!tieneEspecial) mensajeError = 'La contraseña debe incluir al menos un carácter especial (ej: @, #, $, %, !).';

  return {
    esValida,
    minLongitud,
    tieneMayuscula,
    tieneMinuscula,
    tieneNumero,
    tieneEspecial,
    mensajeError
  };
}

function actualizarIndicadoresPassword(password) {
  const evalPass = validarFortalezaContrasenia(password);

  const reqs = [
    { id: 'req-longitud', ok: evalPass.minLongitud },
    { id: 'req-mayus', ok: evalPass.tieneMayuscula },
    { id: 'req-minus', ok: evalPass.tieneMinuscula },
    { id: 'req-numero', ok: evalPass.tieneNumero },
    { id: 'req-especial', ok: evalPass.tieneEspecial }
  ];

  reqs.forEach(r => {
    const el = document.getElementById(r.id);
    if (!el) return;
    const icono = el.querySelector('i');
    if (r.ok) {
      el.className = 'text-success fw-bold small mb-1';
      if (icono) icono.className = 'bi bi-check-circle-fill me-1';
    } else {
      el.className = 'text-muted small mb-1';
      if (icono) icono.className = 'bi bi-circle me-1';
    }
  });
}

function inicializarRegistro() {
  const form = document.getElementById('form-registro');
  if (!form) return;

  const inputPass = document.getElementById('reg-password');
  if (inputPass) {
    inputPass.addEventListener('input', e => {
      actualizarIndicadoresPassword(e.target.value);
    });
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    ocultarMensaje('msg-registro');

    const nombreCompleto = document.getElementById('reg-nombre').value.trim();
    const correo   = document.getElementById('reg-correo').value.trim().toLowerCase();
    const password = document.getElementById('reg-password').value;
    const confirmar= document.getElementById('reg-confirmar').value;

    if (!nombreCompleto || nombreCompleto.length < 2) {
      mostrarMensaje('msg-registro', 'El nombre debe tener al menos 2 caracteres.', 'error');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
      mostrarMensaje('msg-registro', 'Ingresa un correo electrónico válido.', 'error');
      return;
    }

    const valPass = validarFortalezaContrasenia(password);
    if (!valPass.esValida) {
      mostrarMensaje('msg-registro', valPass.mensajeError, 'error');
      return;
    }

    if (password !== confirmar) {
      mostrarMensaje('msg-registro', 'Las contraseñas no coinciden.', 'error');
      return;
    }

    const partes  = nombreCompleto.split(/\s+/);
    const nombre  = partes[0];
    const apellido = partes.slice(1).join(' '); // un solo nombre: no se duplica como apellido

    const btnSubmit = form.querySelector('button[type="submit"]');
    if (btnSubmit) {
      btnSubmit.disabled = true;
      btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Creando cuenta...';
    }

    try {
      const resp = await fetch(`${API_BASE}/auth/registro`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nombre, apellido, email: correo, contrasenia: password })
      });

      const data = await resp.json().catch(() => ({}));

      if (!resp.ok) {
        mostrarMensaje('msg-registro', data.error || 'No se pudo crear la cuenta.', 'error');
        return;
      }

      mostrarMensaje('msg-registro', '¡Cuenta creada exitosamente! Redirigiendo...', 'ok');
      Sesion.guardar({ id: data.id, nombre: nombreCompleto, correo: data.email, token: data.token, tipoUsuario: data.tipoUsuario || 'CLIENTE' });
      Toast.mostrar('¡Cuenta creada exitosamente!', 'ok');
      setTimeout(() => { window.location.href = 'destinos.html'; }, 1100);
    } catch (err) {
      mostrarMensaje('msg-registro', 'No se pudo conectar con el servidor. Intenta de nuevo.', 'error');
    } finally {
      if (btnSubmit) {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = '<i class="bi bi-person-check-fill" aria-hidden="true"></i> Crear cuenta';
      }
    }
  });

  agregarTogglePassword('reg-password');
  agregarTogglePassword('reg-confirmar');

  const camposProgreso = ['reg-nombre', 'reg-correo', 'reg-password', 'reg-confirmar'];
  function actualizarProgreso() {
    const llenos = camposProgreso.filter(id => document.getElementById(id)?.value.trim()).length;
    const pct = (llenos / camposProgreso.length) * 100;
    const barra = document.getElementById('barra-progreso');
    if (barra) {
      barra.style.width = pct + '%';
      barra.closest('[role=progressbar]')?.setAttribute('aria-valuenow', pct);
    }
    const txt = document.getElementById('progreso-texto');
    if (txt) txt.textContent = `${llenos} / ${camposProgreso.length}`;
  }
  camposProgreso.forEach(id => {
    document.getElementById(id)?.addEventListener('input', actualizarProgreso);
  });
}

function agregarTogglePassword(inputId) {
  const input = document.getElementById(inputId);
  if (!input) return;

  const wrapper = document.createElement('div');
  wrapper.style.cssText = 'position:relative;';
  input.parentNode.insertBefore(wrapper, input);
  wrapper.appendChild(input);

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.setAttribute('aria-label', 'Mostrar contrase\u00f1a');
  btn.style.cssText = `
    position:absolute; right:10px; top:50%; transform:translateY(-50%);
    background:none; border:none; cursor:pointer; padding:4px 6px;
    color:#94a3b8; font-size:16px; transition:color 0.2s;
  `;
  btn.innerHTML = '<i class="bi bi-eye"></i>';
  btn.addEventListener('click', () => {
    const visible = input.type === 'text';
    input.type = visible ? 'password' : 'text';
    btn.innerHTML = visible ? '<i class="bi bi-eye"></i>' : '<i class="bi bi-eye-slash"></i>';
    btn.setAttribute('aria-label', visible ? 'Mostrar contrase\u00f1a' : 'Ocultar contrase\u00f1a');
  });
  btn.addEventListener('mouseenter', () => btn.style.color = '#0ea5e9');
  btn.addEventListener('mouseleave', () => btn.style.color = '#94a3b8');
  wrapper.appendChild(btn);
}

function inicializarBuscador() {
  const btn = document.getElementById('btn-buscar');
  if (!btn) return;

  const inpFecha = document.getElementById('inp-fecha');
  if (inpFecha) inpFecha.min = new Date().toISOString().split('T')[0];

  btn.addEventListener('click', () => {
    const origen    = document.getElementById('sel-origen').value;
    const destino   = document.getElementById('sel-destino').value;
    const fecha     = document.getElementById('inp-fecha').value;
    const pasajeros = document.getElementById('inp-pasajeros').value;

    if (!origen)    { Toast.mostrar('Selecciona la ciudad de origen.', 'error'); return; }
    if (!destino)   { Toast.mostrar('Selecciona el destino.', 'error'); return; }
    if (origen === destino) { Toast.mostrar('El origen y destino no pueden ser iguales.', 'error'); return; }
    if (!fecha)     { Toast.mostrar('Selecciona una fecha de viaje.', 'error'); return; }

    const params = new URLSearchParams({ origen, destino, fecha, pasajeros, tipo: tipoPlanSeleccionado });
    window.location.href = 'destinos.html?' + params.toString();
  });

  document.querySelectorAll('#sel-origen,#sel-destino,#inp-fecha,#inp-pasajeros').forEach(el => {
    el.addEventListener('keydown', e => { if (e.key === 'Enter') btn.click(); });
  });
}

let tipoPlanSeleccionado = 'paquete';

function inicializarTabsBusqueda() {
  const tabs = document.querySelectorAll('.search-tab-item');
  if (!tabs.length) return;

  const textosCampos = {
    paquete: { destino: '\u00bfA d\u00f3nde quieres ir?', boton: '<i class="bi bi-search"></i> Buscar Plan' },
    hotel:   { destino: '\u00bfEn qu\u00e9 destino necesitas hotel?', boton: '<i class="bi bi-search"></i> Buscar Hoteles' },
    pasadia: { destino: '\u00bfQu\u00e9 pasad\u00eda o tour te interesa?', boton: '<i class="bi bi-search"></i> Buscar Pasad\u00eda' }
  };

  tabs.forEach(tab => {
    tab.addEventListener('click', () => activarTabBusqueda(tab));
    tab.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); activarTabBusqueda(tab); }
    });
  });

  function activarTabBusqueda(tabSeleccionado) {
    tabs.forEach(t => {
      const activo = t === tabSeleccionado;
      t.classList.toggle('active', activo);
      t.setAttribute('aria-selected', String(activo));
    });

    tipoPlanSeleccionado = tabSeleccionado.dataset.tipo || 'paquete';

    const textos = textosCampos[tipoPlanSeleccionado] || textosCampos.paquete;
    const primeraOpcionDestino = document.querySelector('#sel-destino option[value=""]');
    if (primeraOpcionDestino) primeraOpcionDestino.textContent = textos.destino;

    const btnBuscar = document.getElementById('btn-buscar');
    if (btnBuscar) btnBuscar.innerHTML = textos.boton;
  }
}

function inicializarTabsLogin() {
  const tabCliente = document.getElementById('tab-login-cliente');
  const tabEmpleado = document.getElementById('tab-login-empleado');
  if (!tabCliente || !tabEmpleado) return;

  const labelCorreo    = document.getElementById('label-login-correo');
  const inputCorreo    = document.getElementById('login-correo');
  const alertaDemo     = document.getElementById('alerta-demo-cliente');
  const alertaEmpleado = document.getElementById('alerta-nota-empleado');
  const enlaceRegistro = document.getElementById('enlace-registro-cliente');

  function activarTab(esEmpleado) {
    tabCliente.classList.toggle('activo', !esEmpleado);
    tabEmpleado.classList.toggle('activo', esEmpleado);
    tabCliente.setAttribute('aria-selected', String(!esEmpleado));
    tabEmpleado.setAttribute('aria-selected', String(esEmpleado));

    tabCliente.classList.toggle('btn-primary', !esEmpleado);
    tabCliente.classList.toggle('btn-outline-secondary', esEmpleado);
    tabCliente.classList.toggle('border-0', esEmpleado);
    tabCliente.style.background = esEmpleado ? '' : 'var(--color-primary)';
    tabCliente.style.border = esEmpleado ? '' : 'none';

    tabEmpleado.classList.toggle('btn-primary', esEmpleado);
    tabEmpleado.classList.toggle('btn-outline-secondary', !esEmpleado);
    tabEmpleado.classList.toggle('border-0', !esEmpleado);
    tabEmpleado.style.background = esEmpleado ? 'var(--color-primary)' : '';
    tabEmpleado.style.border = esEmpleado ? 'none' : '';

    if (labelCorreo) {
      labelCorreo.innerHTML = esEmpleado
        ? '<i class="bi bi-envelope-fill" aria-hidden="true"></i> Correo corporativo'
        : '<i class="bi bi-envelope-fill" aria-hidden="true"></i> Correo electr\u00f3nico';
    }
    if (inputCorreo) {
      inputCorreo.placeholder = esEmpleado ? 'admin@aleleotours.com' : 'tucorreo@email.com';
    }

    if (alertaDemo)     alertaDemo.style.display     = esEmpleado ? 'none' : '';
    if (alertaEmpleado) alertaEmpleado.style.display = esEmpleado ? '' : 'none';
    if (enlaceRegistro) enlaceRegistro.style.display = esEmpleado ? 'none' : '';
  }

  tabCliente.addEventListener('click', () => activarTab(false));
  tabEmpleado.addEventListener('click', () => activarTab(true));
}

document.addEventListener('DOMContentLoaded', () => {
  Sesion.actualizar();
  inicializarNavToggle();
  inicializarNavScroll();
  inicializarCerrarSesion();
  inicializarLogin();
  inicializarRegistro();
  inicializarBuscador();
  inicializarTabsLogin();
  inicializarTabsBusqueda();
});