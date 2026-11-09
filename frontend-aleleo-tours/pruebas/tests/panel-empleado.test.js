/**
 * CP-36 a CP-38 - js/empleado/panel-empleado.js
 *
 * Control de acceso del panel operativo: solo un empleado ACTIVO y DISPONIBLE
 * puede atender solicitudes; ademas se verifican los contadores del tablero.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

const mod = cargarModulos(['js/empleado/panel-empleado.js'], {
  esEmpleadoActivoYDisponible: 'esEmpleadoActivoYDisponible',
  calcularKPIs: 'calcularKPIs',
  __setSesion: 'v => { sesionEmpleado = v; }',
  __setSolicitudes: 'v => { solicitudesAsignadas = v; }'
});

beforeEach(() => {
  document.body.innerHTML = '';
  mod.__setSesion(null);
  mod.__setSolicitudes([]);
});

describe('panel-empleado.js - acceso del asesor', () => {
  test('CP-36: sin sesion o con la cuenta inactiva/bloqueada no hay acceso', () => {
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);

    mod.__setSesion({ estadoAcceso: 'INACTIVA', estadoDisponibilidad: 'DISPONIBLE' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);

    mod.__setSesion({ estadoAcceso: 'BLOQUEADA', estadoDisponibilidad: 'DISPONIBLE' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);

    mod.__setSesion({ estadoAcceso: 'SUSPENDIDO', estadoDisponibilidad: 'DISPONIBLE' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);
  });

  test('CP-37: solo ACTIVA + DISPONIBLE habilita el panel (sin distinguir mayusculas)', () => {
    mod.__setSesion({ estadoAcceso: 'ACTIVA', estadoDisponibilidad: 'DISPONIBLE' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(true);

    mod.__setSesion({ estadoAcceso: 'activa', estadoDisponibilidad: 'disponible' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(true);

    mod.__setSesion({ estadoAcceso: 'ACTIVA', estadoDisponibilidad: 'FUERA_DE_TURNO' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);

    mod.__setSesion({ estadoAcceso: 'ACTIVA', estadoDisponibilidad: 'OCUPADO' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(false);
  });

  test('CP-37b: una sesion sin estados declarados se asume ACTIVA y DISPONIBLE', () => {
    mod.__setSesion({ email: 'diana.lopez@aleleotours.com' });
    expect(mod.esEmpleadoActivoYDisponible()).toBe(true);
  });
});

describe('panel-empleado.js - contadores KPI', () => {
  test('CP-38: calcularKPIs cuenta asignadas, pendientes, en proceso y resueltas', () => {
    document.body.innerHTML = `
      <span id="kpi-asignadas"></span>
      <span id="kpi-pendientes"></span>
      <span id="kpi-proceso"></span>
      <span id="kpi-resueltas"></span>
    `;

    mod.__setSolicitudes([
      { id: 1, estado: 'PENDIENTE' },
      { id: 2, estado: 'PENDIENTE' },
      { id: 3, estado: 'EN_PROCESO' },
      { id: 4, estado: 'RESUELTA' },
      { id: 5, estado: 'APROBADA' },
      { id: 6, estado: 'CANCELADA' }
    ]);

    mod.calcularKPIs();

    expect(document.getElementById('kpi-asignadas').textContent).toBe('6');
    expect(document.getElementById('kpi-pendientes').textContent).toBe('2');
    expect(document.getElementById('kpi-proceso').textContent).toBe('1');
    // RESUELTA + APROBADA (+ CONFIRMADA) cuentan como atendidas
    expect(document.getElementById('kpi-resueltas').textContent).toBe('2');
  });

  test('CP-38b: sin solicitudes los contadores quedan en cero y no falla si faltan los nodos', () => {
    mod.calcularKPIs(); // sin ningun #kpi-* en el DOM

    document.body.innerHTML = '<span id="kpi-asignadas"></span><span id="kpi-pendientes"></span>';
    mod.calcularKPIs();
    expect(document.getElementById('kpi-asignadas').textContent).toBe('0');
    expect(document.getElementById('kpi-pendientes').textContent).toBe('0');
  });
});
