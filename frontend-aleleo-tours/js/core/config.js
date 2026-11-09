/* ==========================================================================
   AleLeo Tours - config.js
   --------------------------------------------------------------------------
   Configuracion de entorno del frontend. Es el UNICO archivo que hay que
   editar para apuntar el sitio a un backend distinto, y por eso se
   separo de app.js: antes la URL de la API vivia incrustada en el codigo
   y cambiar de servidor obligaba a recompilar y redesplegar todo el sitio.

   El orden de resolucion es:
     1. window.SACE_API_BASE  -> override en caliente (pruebas desde la
        consola del navegador, sin tocar archivos).
     2. API_FIJA             -> lo editas aqui. Prioridad sobre la
        deteccion automatica.
     3. Deteccion por dominio -> si el sitio esta en un dominio real
        (no localhost), la API se asume en https://api.<dominio>/api.
        Ejemplo: www.aleleotours.com -> https://api.aleleotours.com/api
     4. Desarrollo local     -> http://localhost:8082/api
   ========================================================================== */

'use strict';

(function () {

  // --- 2) Valor fijo por entorno. Edita SOLO esta linea al desplegar. ------
  // Produccion actual (Render): el backend vive en sace-api.onrender.com.
  // Cuando se registre el dominio propio y la API quede en api.aleleotours.com,
  // basta con dejar esta cadena vacia ('') y la deteccion por dominio del
  // punto 3 la resolvera sola.
  const API_FIJA = 'https://sace-api.onrender.com/api';

  // Dominios de desarrollo: nunca se les deduce una API remota.
  const ES_LOCAL = /^(localhost|127\.0\.0\.1|0\.0\.0\.0|\[::1\]|::1)$/i;

  function limpiar(url) {
    return String(url).trim().replace(/\/+$/, '');
  }

  function resolver() {
    // 1) Override en caliente.
    if (typeof window !== 'undefined' && window.SACE_API_BASE) {
      return limpiar(window.SACE_API_BASE);
    }

    // 2) Valor fijo declarado en este archivo.
    if (API_FIJA) {
      return limpiar(API_FIJA);
    }

    // 3) Deteccion automatica por dominio.
    const host = (typeof window !== 'undefined' && window.location) ? window.location.hostname : '';
    if (host && !ES_LOCAL.test(host)) {
      const base = host.replace(/^www\./i, '');
      return 'https://api.' + base + '/api';
    }

    // 4) Desarrollo local.
    return 'http://localhost:8082/api';
  }

  window.SACE_API_BASE = resolver();
})();
