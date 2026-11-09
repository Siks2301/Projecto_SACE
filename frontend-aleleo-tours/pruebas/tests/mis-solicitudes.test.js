/**
 * CP-31 a CP-35 - js/cliente/mis-solicitudes.js
 *
 * Reglas de presentacion que ve el cliente: normalizacion y traduccion del
 * estado de la solicitud, criterio de pago aprobado y formato de montos.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

const mod = cargarModulos(['js/cliente/mis-solicitudes.js'], {
  normalizarEstado: 'normalizarEstado',
  formatearTextoEstado: 'formatearTextoEstado',
  esPagoAprobado: 'esPagoAprobado',
  formatearPesos: 'formatearPesos'
});

describe('mis-solicitudes.js - estado de la solicitud', () => {
  test('CP-31: normalizarEstado pasa a mayusculas y usa PENDIENTE por defecto', () => {
    expect(mod.normalizarEstado('pendiente')).toBe('PENDIENTE');
    expect(mod.normalizarEstado('en_proceso')).toBe('EN_PROCESO');
    expect(mod.normalizarEstado('')).toBe('PENDIENTE');
    expect(mod.normalizarEstado(null)).toBe('PENDIENTE');
    expect(mod.normalizarEstado(undefined)).toBe('PENDIENTE');
  });

  test('CP-32: formatearTextoEstado distingue reserva de las demas categorias', () => {
    expect(mod.formatearTextoEstado('PENDIENTE', 'RESERVA')).toMatch(/^Pendiente de Pago/);
    expect(mod.formatearTextoEstado('PENDIENTE', 'reserva')).toMatch(/^Pendiente de Pago/);
    expect(mod.formatearTextoEstado('PENDIENTE', 'CONSULTA')).toMatch(/^Pendiente de Atenci/);
    expect(mod.formatearTextoEstado('PENDIENTE')).toMatch(/^Pendiente de Pago/);
  });

  test('CP-33: formatearTextoEstado traduce los estados finales y conserva los desconocidos', () => {
    expect(mod.formatearTextoEstado('EN_PROCESO')).toBe('En Proceso por un Asesor');
    expect(mod.formatearTextoEstado('RESUELTA')).toBe('Resuelta / Atendida');
    expect(mod.formatearTextoEstado('APROBADA')).toBe('Resuelta / Atendida');
    expect(mod.formatearTextoEstado('CONFIRMADA')).toBe('Resuelta / Atendida');
    expect(mod.formatearTextoEstado('CANCELADA')).toBe('Cancelada');
    expect(mod.formatearTextoEstado('RECHAZADA')).toBe('No Aprobada');
    expect(mod.formatearTextoEstado('EN_REVISION')).toBe('EN_REVISION');
  });
});

describe('mis-solicitudes.js - pagos', () => {
  test('CP-34: esPagoAprobado reconoce las tres variantes del backend', () => {
    expect(mod.esPagoAprobado('APROBADO')).toBe(true);
    expect(mod.esPagoAprobado('aprobado')).toBe(true);
    expect(mod.esPagoAprobado('APPROVED')).toBe(true);
    expect(mod.esPagoAprobado('CONFIRMADO')).toBe(true);

    expect(mod.esPagoAprobado('PENDIENTE')).toBe(false);
    expect(mod.esPagoAprobado('RECHAZADO')).toBe(false);
    expect(mod.esPagoAprobado('')).toBe(false);
    expect(mod.esPagoAprobado(null)).toBe(false);
  });

  test('CP-35: formatearPesos usa separador de miles colombiano y evita mostrar NaN', () => {
    expect(mod.formatearPesos(320000)).toBe('320.000');
    expect(mod.formatearPesos('480000')).toBe('480.000');
    expect(mod.formatearPesos(1234.5)).toBe('1.234,5');
    expect(mod.formatearPesos(0)).toBe('0');
    expect(mod.formatearPesos('no-es-numero')).toBe('0');
    expect(mod.formatearPesos(null)).toBe('0');
  });
});
