/**
 * @jest-environment jsdom
 * @jest-environment-options {"url": "http://localhost:8091/login.html"}
 *
 * CP-05 a CP-12 - js/core/app.js
 *
 * Nucleo del frontend: saneamiento de HTML (anti-XSS), regla de autoria del
 * chat, validacion de contrasenas, manejo de sesion, avisos Toast y el
 * interceptor de fetch que inyecta el token y expulsa al usuario en 401.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

// app.js envuelve window.fetch al cargarse: debe existir antes del eval.
const fetchBase = jest.fn(async () => ({ status: 200, ok: true, json: async () => ({}) }));
window.fetch = fetchBase;

const app = cargarModulos(['js/core/app.js'], {
  API_BASE: 'API_BASE',
  escaperHtml: 'escaperHtml',
  mensajeEsDelCliente: 'mensajeEsDelCliente',
  validarFortalezaContrasenia: 'validarFortalezaContrasenia',
  Sesion: 'Sesion',
  Toast: 'Toast'
});

beforeEach(() => {
  sessionStorage.clear();
  document.body.innerHTML = '';
});

describe('app.js - escaperHtml (prevencion de XSS)', () => {
  test('CP-05: escapa los seis caracteres peligrosos de HTML', () => {
    expect(app.escaperHtml('<a href="x">Link</a>'))
      .toBe('&lt;a href=&quot;x&quot;&gt;Link&lt;&#47;a&gt;');
    expect(app.escaperHtml("O'Reilly & Cia"))
      .toBe('O&#39;Reilly &amp; Cia');
  });

  test('CP-06: tolera null, undefined y numeros sin romper la interfaz', () => {
    expect(app.escaperHtml(null)).toBe('');
    expect(app.escaperHtml(undefined)).toBe('');
    expect(app.escaperHtml(2026)).toBe('2026');
    expect(app.escaperHtml('<img src=x onerror=alert(1)>'))
      .toBe('&lt;img src=x onerror=alert(1)&gt;');
  });
});

describe('app.js - mensajeEsDelCliente (autoria de burbujas del chat)', () => {
  test('CP-07: identifica cliente, equipo y remitentes desconocidos', () => {
    expect(app.mensajeEsDelCliente('CLIENTE')).toBe(true);
    expect(app.mensajeEsDelCliente('cliente-1')).toBe(true);
    expect(app.mensajeEsDelCliente('ASESOR_VIAJES')).toBe(false);
    expect(app.mensajeEsDelCliente('Empleado')).toBe(false);
    expect(app.mensajeEsDelCliente('Administrador')).toBe(false);
    expect(app.mensajeEsDelCliente('')).toBe(true);
    expect(app.mensajeEsDelCliente(null)).toBe(true);
  });
});

describe('app.js - validarFortalezaContrasenia', () => {
  test('CP-08: acepta una contrasena con los cinco requisitos', () => {
    const r = app.validarFortalezaContrasenia('Sace2026!Segura');
    expect(r.esValida).toBe(true);
    expect(r.mensajeError).toBe('');
    expect(r.minLongitud && r.tieneMayuscula && r.tieneMinuscula && r.tieneNumero && r.tieneEspecial).toBe(true);
  });

  test('CP-09: rechaza cada requisito incumplido con un mensaje distinto', () => {
    expect(app.validarFortalezaContrasenia('Ab1!').mensajeError).toMatch(/8 caracteres/);
    expect(app.validarFortalezaContrasenia('abcdef1!').mensajeError).toMatch(/may.scula/);
    expect(app.validarFortalezaContrasenia('ABCDEF1!').mensajeError).toMatch(/min.scula/);
    expect(app.validarFortalezaContrasenia('Abcdefg!').mensajeError).toMatch(/n.mero/);
    expect(app.validarFortalezaContrasenia('Abcdefg1').mensajeError).toMatch(/car.cter especial/);
  });
});

describe('app.js - Sesion', () => {
  test('CP-10: guarda, lee y cierra la sesion en sessionStorage', () => {
    expect(app.Sesion.obtener()).toBeNull();

    app.Sesion.guardar({ token: 'tkn-123', tipoUsuario: 'CLIENTE', nombre: 'Cesar Ramirez' });
    const sesion = app.Sesion.obtener();

    expect(sesion.token).toBe('tkn-123');
    expect(sesion.tipoUsuario).toBe('CLIENTE');
    expect(JSON.parse(sessionStorage.getItem('aleleo_sesion')).token).toBe('tkn-123');

    app.Sesion.cerrar();
    expect(app.Sesion.obtener()).toBeNull();
    expect(sessionStorage.getItem('aleleo_sesion')).toBeNull();
  });

  test('CP-10b: si sessionStorage tiene basura no lanza y devuelve null', () => {
    sessionStorage.setItem('aleleo_sesion', '{no-es-json');
    expect(app.Sesion.obtener()).toBeNull();
  });
});

describe('app.js - Toast', () => {
  test('CP-11: crea el contenedor, pinta el aviso y lo retira a los 3,5 s', () => {
    jest.useFakeTimers();
    app.Toast.mostrar('Solicitud creada', 'success');

    const contenedor = document.getElementById('toast-container');
    expect(contenedor).not.toBeNull();
    expect(contenedor.getAttribute('aria-live')).toBe('polite');

    const toast = contenedor.querySelector('.toast');
    expect(toast.className).toBe('toast toast-success');
    expect(toast.textContent).toBe('Solicitud creada');
    expect(toast.getAttribute('role')).toBe('status');

    jest.advanceTimersByTime(3499);
    expect(contenedor.querySelector('.toast')).not.toBeNull();
    jest.advanceTimersByTime(1);
    expect(contenedor.querySelector('.toast')).toBeNull();

    jest.useRealTimers();
  });

  test('CP-11b: reutiliza un unico contenedor para varios avisos y usa tipo info por defecto', () => {
    app.Toast.mostrar('Uno');
    app.Toast.mostrar('Dos', 'error');

    const contenedores = document.querySelectorAll('#toast-container');
    expect(contenedores).toHaveLength(1);
    const avisos = contenedores[0].querySelectorAll('.toast');
    expect(avisos).toHaveLength(2);
    expect(avisos[0].className).toBe('toast toast-info');
    expect(avisos[1].className).toBe('toast toast-error');
  });
});

describe('app.js - protegerFetch (token y sesion vencida)', () => {
  test('CP-12: inyecta el token solo en las llamadas al API propia', async () => {
    app.Sesion.guardar({ token: 'jwt-abc', tipoUsuario: 'CLIENTE' });
    fetchBase.mockClear();

    await window.fetch(app.API_BASE + '/solicitudes');
    await window.fetch('https://otro-sitio.com/datos');

    const [llamadaApi, llamadaExterna] = fetchBase.mock.calls;
    expect(llamadaApi[1].headers.Authorization).toBe('Bearer jwt-abc');
    // A un dominio ajeno se le pasa el init tal cual (sin cabecera Authorization).
    expect(llamadaExterna[1]?.headers?.Authorization).toBeUndefined();
  });

  test('CP-12b: respeta un Authorization que ya venia en la peticion', async () => {
    app.Sesion.guardar({ token: 'jwt-abc', tipoUsuario: 'CLIENTE' });
    fetchBase.mockClear();

    await window.fetch(app.API_BASE + '/solicitudes', { headers: { Authorization: 'Bearer manual' } });

    expect(fetchBase.mock.calls[0][1].headers.Authorization).toBe('Bearer manual');
  });

  test('CP-12c: ante un 401 borra la sesion', async () => {
    app.Sesion.guardar({ token: 'jwt-vencido', tipoUsuario: 'CLIENTE' });
    fetchBase.mockImplementationOnce(async () => ({ status: 401, ok: false }));

    const resp = await window.fetch(app.API_BASE + '/solicitudes');

    expect(resp.status).toBe(401);
    expect(app.Sesion.obtener()).toBeNull();
  });
});
