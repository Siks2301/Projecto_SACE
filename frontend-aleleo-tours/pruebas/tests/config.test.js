/**
 * CP-01 / CP-02 / CP-03 / CP-04 - js/core/config.js
 *
 * Configuracion del entorno: el frontend debe resolver la URL del API en el
 * orden override -> valor fijo -> dominio -> localhost. Se prueba cada rama
 * cargando el archivo real en un contexto aislado (vm) con distintas
 * ubicaciones simuladas.
 *
 * El valor fijo (API_FIJA) se lee del propio archivo: cuando apunta a un
 * backend desplegado tiene prioridad sobre cualquier host, y las ramas de
 * dominio/localhost solo se alcanzan si esa constante queda vacia. Para
 * probarlas se evalua una copia del mismo codigo con API_FIJA = ''.
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const RUTA_CONFIG = path.resolve(__dirname, '..', '..', 'js', 'core', 'config.js');
const CODIGO = fs.readFileSync(RUTA_CONFIG, 'utf8');

// Valor real configurado para el despliegue actual (puede ser '').
const API_FIJA = (CODIGO.match(/const\s+API_FIJA\s*=\s*'([^']*)'/) || [null, ''])[1];

// Misma logica sin valor fijo, para ejercitar las ramas 3 y 4.
const CODIGO_SIN_FIJA = CODIGO.replace(
  /const\s+API_FIJA\s*=\s*'[^']*';/,
  "const API_FIJA = '';"
);

function resolverApi({ hostname = 'localhost', override = null, codigo = CODIGO } = {}) {
  const window = { location: { hostname } };
  if (override !== null) {
    window.SACE_API_BASE = override;
  }
  const sandbox = { window, console };
  vm.createContext(sandbox);
  vm.runInContext(codigo, sandbox);
  return window.SACE_API_BASE;
}

describe('config.js - resolucion de la URL del API', () => {
  test('CP-01: API_FIJA se aplica a cualquier host cuando esta definida', () => {
    if (!API_FIJA) return; // despliegue sin valor fijo: lo cubren CP-02 y CP-04
    expect(API_FIJA).toMatch(/^https?:\/\//);
    expect(resolverApi({ hostname: 'localhost' })).toBe(API_FIJA);
    expect(resolverApi({ hostname: 'www.aleleotours.com' })).toBe(API_FIJA);
    expect(resolverApi({ hostname: 'aleleotours.aleleo.workers.dev' })).toBe(API_FIJA);
  });

  test('CP-02: sin valor fijo, un dominio real deduce https://api.<dominio>/api', () => {
    expect(resolverApi({ hostname: 'www.aleleotours.com', codigo: CODIGO_SIN_FIJA }))
      .toBe('https://api.aleleotours.com/api');
    expect(resolverApi({ hostname: 'aleleotours.com', codigo: CODIGO_SIN_FIJA }))
      .toBe('https://api.aleleotours.com/api');
  });

  test('CP-03: window.SACE_API_BASE tiene prioridad (override en caliente) y se limpian las barras finales', () => {
    expect(resolverApi({ hostname: 'www.aleleotours.com', override: 'https://api.pruebas.com/api///' }))
      .toBe('https://api.pruebas.com/api');
    expect(resolverApi({ hostname: 'localhost', override: '  http://192.168.1.7:8082/api  ' }))
      .toBe('http://192.168.1.7:8082/api');
  });

  test('CP-04: sin valor fijo ni host utilizable cae en el backend local del puerto 8082', () => {
    expect(resolverApi({ hostname: 'localhost', codigo: CODIGO_SIN_FIJA }))
      .toBe('http://localhost:8082/api');
    expect(resolverApi({ hostname: '127.0.0.1', codigo: CODIGO_SIN_FIJA }))
      .toBe('http://localhost:8082/api');
  });
});
