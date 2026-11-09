/**
 * CP-01 / CP-02 / CP-03 / CP-04 - js/core/config.js
 *
 * Configuracion del entorno: el frontend debe resolver la URL del API en el
 * orden override -> valor fijo -> dominio -> localhost. Se prueba cada rama
 * cargando el archivo real en un contexto aislado (vm) con distintas
 * ubicaciones simuladas.
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const RUTA_CONFIG = path.resolve(__dirname, '..', '..', 'js', 'core', 'config.js');
const CODIGO = fs.readFileSync(RUTA_CONFIG, 'utf8');

function resolverApi({ hostname = 'localhost', override = null } = {}) {
  const window = { location: { hostname } };
  if (override !== null) {
    window.SACE_API_BASE = override;
  }
  const sandbox = { window, console };
  vm.createContext(sandbox);
  vm.runInContext(CODIGO, sandbox);
  return window.SACE_API_BASE;
}

describe('config.js - resolucion de la URL del API', () => {
  test('CP-01: en localhost usa el backend de desarrollo en el puerto 8082', () => {
    expect(resolverApi({ hostname: 'localhost' })).toBe('http://localhost:8082/api');
    expect(resolverApi({ hostname: '127.0.0.1' })).toBe('http://localhost:8082/api');
  });

  test('CP-02: en un dominio real deduce https://api.<dominio>/api', () => {
    expect(resolverApi({ hostname: 'www.aleleotours.com' })).toBe('https://api.aleleotours.com/api');
    expect(resolverApi({ hostname: 'aleleotours.com' })).toBe('https://api.aleleotours.com/api');
  });

  test('CP-03: window.SACE_API_BASE tiene prioridad (override en caliente) y se limpian las barras finales', () => {
    expect(resolverApi({ hostname: 'www.aleleotours.com', override: 'https://api.pruebas.com/api///' }))
      .toBe('https://api.pruebas.com/api');
    expect(resolverApi({ hostname: 'localhost', override: '  http://192.168.1.7:8082/api  ' }))
      .toBe('http://192.168.1.7:8082/api');
  });

  test('CP-04: sin window (entorno sin navegador) cae en el valor local por defecto', () => {
    const sandbox = { window: undefined, console };
    vm.createContext(sandbox);
    // config.js hace window.SACE_API_BASE = ...; exponemos un window vacio
    sandbox.window = {};
    vm.runInContext(CODIGO, sandbox);
    expect(sandbox.window.SACE_API_BASE).toBe('http://localhost:8082/api');
  });
});
