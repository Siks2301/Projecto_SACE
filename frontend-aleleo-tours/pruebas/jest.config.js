/**
 * Configuracion de Jest para el frontend de SACE (AleLeo Tours).
 *
 * El frontend es 100 % estatico (no tiene build ni modulos ES): los archivos
 * de js/ se cargan con <script> y exponen funciones globales. Por eso las
 * pruebas levantan un DOM real con jsdom y evaluan los scripts tal como los
 * carga el navegador (ver helpers/cargar-frontend.js).
 */
module.exports = {
  testEnvironment: 'jsdom',
  testEnvironmentOptions: {
    url: 'http://localhost:8091/'
  },
  roots: ['<rootDir>/tests'],
  testMatch: ['**/*.test.js'],
  verbose: true,
  clearMocks: true,
  restoreMocks: true,
  testTimeout: 10000
};
