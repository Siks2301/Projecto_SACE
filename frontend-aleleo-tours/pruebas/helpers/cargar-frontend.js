/**
 * Carga los scripts REALES del frontend dentro del DOM de jsdom.
 *
 * Los archivos de js/ no son modulos: declaran funciones globales y se
 * encadenan entre si (por ejemplo destinos.js usa lo que define
 * destinos-datos.js). Para reproducir ese comportamiento:
 *
 *   1. Se concatenan los archivos en el mismo orden en que los carga el HTML.
 *   2. Se evaluan con window.eval en el ambito global del DOM de jsdom
 *      (igual que un <script> clasico).
 *   3. Se anade una linea final que publica en globalThis.__SACE__ las
 *      funciones que la prueba necesita, porque 'use strict' + eval deja
 *      las declaraciones en el ambito del propio script.
 *
 * No se modifica ni se copia el codigo de produccion: se ejecuta el mismo
 * archivo que ve el navegador.
 */
const fs = require('fs');
const path = require('path');

// pruebas/helpers -> pruebas -> frontend-aleleo-tours
const RAIZ_FRONTEND = path.resolve(__dirname, '..', '..');

function leerArchivo(relativa) {
  const completo = path.join(RAIZ_FRONTEND, relativa);
  if (!fs.existsSync(completo)) {
    throw new Error('No existe el script del frontend: ' + completo);
  }
  return fs.readFileSync(completo, 'utf8');
}

/**
 * @param {string[]} archivos rutas relativas a la raiz del frontend, en orden de carga
 * @param {Object<string,string>} expresiones alias -> expresion JS a exportar
 *        (se escribe entre parentesis, admite arrow functions)
 * @returns {Object} mapa de lo exportado
 */
function cargarModulos(archivos, expresiones = {}) {
  const codigo = archivos.map(leerArchivo).join('\n;\n');

  const entradas = Object.entries(expresiones)
    .map(([alias, expresion]) => `  ${alias}: (${expresion})`)
    .join(',\n');

  const exportador = entradas
    ? `\n;globalThis.__SACE__ = Object.assign(globalThis.__SACE__ || {}, {\n${entradas}\n});\n`
    : '\n;globalThis.__SACE__ = globalThis.__SACE__ || {};\n';

  globalThis.__SACE__ = {};
  window.eval(codigo + exportador);
  return globalThis.__SACE__;
}

module.exports = { cargarModulos, RAIZ_FRONTEND };
