/**
 * CP-39 / CP-40 - js/admin/gestion-empleados.js
 *
 * El formulario de empleados solo pide la parte local del correo y completa
 * el dominio corporativo. Se prueba la normalizacion y el contrato del campo.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

const mod = cargarModulos(['js/admin/gestion-empleados.js'], {
  parteLocalEmpleado: 'parteLocalEmpleado',
  DOMINIO_EMPLEADO: 'DOMINIO_EMPLEADO'
});

describe('gestion-empleados.js - correo corporativo', () => {
  test('CP-39: parteLocalEmpleado deja solo el texto anterior a la arroba, en minusculas y sin espacios', () => {
    expect(mod.parteLocalEmpleado('Diana.Lopez@aleleotours.com')).toBe('diana.lopez');
    expect(mod.parteLocalEmpleado('  Diana.Lopez  ')).toBe('diana.lopez');
    expect(mod.parteLocalEmpleado('CARLOS@')).toBe('carlos');
    expect(mod.parteLocalEmpleado('a@b@c')).toBe('a');
  });

  test('CP-40: valores vacios o nulos no rompen el formulario', () => {
    expect(mod.parteLocalEmpleado('')).toBe('');
    expect(mod.parteLocalEmpleado(null)).toBe('');
    expect(mod.parteLocalEmpleado(undefined)).toBe('');
    expect(mod.DOMINIO_EMPLEADO).toBe('@aleleotours.com');
  });
});
