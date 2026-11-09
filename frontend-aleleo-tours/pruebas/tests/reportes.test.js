/**
 * CP-24 a CP-27 - js/admin/reportes.js
 *
 * Logica pura del dashboard administrativo: rangos de fecha de los filtros
 * rapidos, construccion del query string y traduccion de estados/categorias.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

const mod = cargarModulos(['js/admin/reportes.js'], {
  calcularRangoPreset: 'calcularRangoPreset',
  construirQueryParams: 'construirQueryParams',
  formatearEstado: 'formatearEstado',
  formatearCategoria: 'formatearCategoria'
});

const MS_DIA = 24 * 60 * 60 * 1000;
const aFecha = (iso) => new Date(iso + 'T00:00:00Z').getTime();

beforeEach(() => {
  // Fecha fija: 15 de marzo de 2026 (mediodia UTC) para que el rango sea predecible.
  jest.useFakeTimers();
  jest.setSystemTime(new Date(Date.UTC(2026, 2, 15, 12, 0, 0)));
});

afterEach(() => {
  jest.useRealTimers();
});

describe('reportes.js - filtros de fecha', () => {
  test('CP-24: HOY devuelve el mismo dia en desde y hasta', () => {
    expect(mod.calcularRangoPreset('HOY')).toEqual({ desde: '2026-03-15', hasta: '2026-03-15' });
  });

  test('CP-25: SEMANA cubre los ultimos 7 dias incluido hoy', () => {
    const r = mod.calcularRangoPreset('SEMANA');
    expect(r.hasta).toBe('2026-03-15');
    expect((aFecha(r.hasta) - aFecha(r.desde)) / MS_DIA).toBe(6);
    expect(r.desde).toBe('2026-03-09');
  });

  test('CP-26: MES_ACTUAL empieza el dia 1 y MES_ANTERIOR cierra el mes completo', () => {
    expect(mod.calcularRangoPreset('MES_ACTUAL')).toEqual({ desde: '2026-03-01', hasta: '2026-03-15' });
    expect(mod.calcularRangoPreset('MES_ANTERIOR')).toEqual({ desde: '2026-02-01', hasta: '2026-02-28' });
  });

  test('CP-27: HISTORICO y cualquier preset desconocido van sin filtro de fecha', () => {
    expect(mod.calcularRangoPreset('HISTORICO')).toEqual({ desde: '', hasta: '' });
    expect(mod.calcularRangoPreset('OTRO')).toEqual({ desde: '', hasta: '' });
    expect(mod.calcularRangoPreset(undefined)).toEqual({ desde: '', hasta: '' });
  });

  test('CP-28: construirQueryParams solo agrega los extremos informados', () => {
    expect(mod.construirQueryParams('', '')).toBe('');
    expect(mod.construirQueryParams('2026-03-01', '')).toBe('?desde=2026-03-01');
    expect(mod.construirQueryParams('', '2026-03-15')).toBe('?hasta=2026-03-15');
    expect(mod.construirQueryParams('2026-03-01', '2026-03-15'))
      .toBe('?desde=2026-03-01&hasta=2026-03-15');
  });
});

describe('reportes.js - etiquetas legibles', () => {
  test('CP-29: formatearEstado traduce los cuatro estados y deja pasar lo desconocido', () => {
    expect(mod.formatearEstado('PENDIENTE')).toBe('Pendiente');
    expect(mod.formatearEstado('EN_PROCESO')).toBe('En Proceso');
    expect(mod.formatearEstado('RESUELTA')).toBe('Resuelta');
    expect(mod.formatearEstado('CANCELADA')).toBe('Cancelada');
    expect(mod.formatearEstado('NUEVO_ESTADO')).toBe('NUEVO_ESTADO');
  });

  test('CP-30: formatearCategoria traduce el catalogo y embellece las categorias nuevas', () => {
    expect(mod.formatearCategoria('COTIZACION')).toBe('Cotizaciones');
    expect(mod.formatearCategoria('RESERVA')).toBe('Reservas');
    expect(mod.formatearCategoria('CAMBIO_FECHA')).toBe('Cambios de Fecha');
    expect(mod.formatearCategoria('PQR_NUEVA')).toBe('PQR NUEVA');
    expect(mod.formatearCategoria(null)).toBe('Consulta General');
  });
});
