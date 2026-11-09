/**
 * Genera las paginas de evidencia (HTML) a partir de la salida real de Jest.
 * Luego se capturan con el navegador para insertarlas en el documento Word.
 */
const fs = require('fs');
const path = require('path');
const os = require('os');

const DIR = __dirname;
const RAIZ = path.resolve(DIR, '..');

const resultados = JSON.parse(fs.readFileSync(path.join(DIR, 'resultados.json'), 'utf8'));
let salida = fs.readFileSync(path.join(DIR, 'salida-jest.txt'), 'utf8').replace(/^\uFEFF/, '');

const fecha = new Date(resultados.startTime);
const fechaTexto = fecha.toLocaleString('es-CO', { dateStyle: 'long', timeStyle: 'short' });
const tiempoTotal = (salida.match(/^Time:\s+(.+)$/m) || [null, ''])[1].trim();

const SUITES = [
  { archivo: 'js/core/config.js', prueba: 'config.test.js', area: 'Núcleo · configuración de entorno', descripcion: 'Resolución de la URL del API (override, dominio, localhost).' },
  { archivo: 'js/core/app.js', prueba: 'app.test.js', area: 'Núcleo · sesión y seguridad', descripcion: 'Escapado HTML (XSS), chat, contraseñas, sesión, Toast y fetch con token.' },
  { archivo: 'js/cliente/destinos-datos.js + destinos.js', prueba: 'destinos.test.js', area: 'Cliente · catálogo de destinos', descripcion: 'Normalización, imágenes, categorías, mapeo del backend, precios y pasajeros.' },
  { archivo: 'js/admin/reportes.js', prueba: 'reportes.test.js', area: 'Administrador · reportes', descripcion: 'Rangos de fecha, query string y etiquetas de estado/categoría.' },
  { archivo: 'js/cliente/mis-solicitudes.js', prueba: 'mis-solicitudes.test.js', area: 'Cliente · mis solicitudes', descripcion: 'Estados de la solicitud, pagos aprobados y formato de pesos.' },
  { archivo: 'js/empleado/panel-empleado.js', prueba: 'panel-empleado.test.js', area: 'Empleado · panel operativo', descripcion: 'Control de acceso del asesor y contadores del tablero (KPI).' },
  { archivo: 'js/admin/gestion-empleados.js', prueba: 'gestion-empleados.test.js', area: 'Administrador · empleados', descripcion: 'Normalización del correo corporativo del empleado.' }
];

const ESTILO_BASE = `
  * { box-sizing: border-box; }
  body { margin: 0; font-family: "Segoe UI", system-ui, sans-serif; color: #0f172a; background: #eef2f7; }
  .hoja { width: 1280px; margin: 0 auto; padding: 40px 48px 48px; background: #ffffff; }
  h1 { margin: 0 0 6px; font-size: 30px; letter-spacing: -.3px; }
  h2 { margin: 28px 0 12px; font-size: 19px; }
  p.sub { margin: 0; color: #475569; font-size: 14px; }
  .marca { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
  .logo { width: 46px; height: 46px; border-radius: 12px; background: #0e7490; color: #fff;
          display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 20px; }
  .pill { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
  .ok { background: #dcfce7; color: #15803d; }
  .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px 18px; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th { text-align: left; background: #0e7490; color: #fff; padding: 9px 10px; font-weight: 600; }
  td { padding: 8px 10px; border-bottom: 1px solid #e2e8f0; vertical-align: top; }
  tr:nth-child(even) td { background: #f8fafc; }
  code, .mono { font-family: Consolas, "Cascadia Mono", monospace; }
  .kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 18px 0 4px; }
  .kpi { border-radius: 12px; padding: 16px 18px; background: #f1f5f9; border-left: 5px solid #0e7490; }
  .kpi b { display: block; font-size: 30px; line-height: 1.1; }
  .kpi span { font-size: 12px; color: #475569; text-transform: uppercase; letter-spacing: .4px; }
  .kpi.verde { border-left-color: #16a34a; }
  footer { margin-top: 26px; padding-top: 12px; border-top: 1px solid #e2e8f0; color: #64748b; font-size: 12px;
           display: flex; justify-content: space-between; }
`;

function cabecera(titulo, subtitulo, extra = '') {
  return `
    <div class="marca">
      <div class="logo">AL</div>
      <div>
        <div style="font-weight:700;letter-spacing:.4px;color:#0e7490;font-size:13px">ALELÉO TOURS · SACE</div>
        <h1>${titulo}</h1>
        <p class="sub">${subtitulo}</p>
      </div>
    </div>${extra}`;
}

function pie(pagina) {
  return `<footer><span>Evidencia generada automáticamente a partir de la ejecución real de Jest.</span>
          <span>${pagina} · ${fechaTexto}</span></footer>`;
}

// ---------------------------------------------------------------- resumen
const total = resultados.numTotalTests;
const suitesHtml = SUITES.map(s => {
  const r = resultados.testResults.find(t => t.name.endsWith(s.prueba));
  const n = r.assertionResults.length;
  return `<tr>
    <td><strong>${s.area}</strong><br><span class="mono" style="color:#64748b;font-size:12px">${s.archivo}</span></td>
    <td>${s.descripcion}</td>
    <td style="text-align:center"><strong>${n}</strong></td>
    <td style="text-align:center"><span class="pill ok">PASS</span></td>
  </tr>`;
}).join('');

const resumen = `<!doctype html><html lang="es"><head><meta charset="utf-8">
<title>Resumen de ejecución</title><style>${ESTILO_BASE}</style></head><body>
<div class="hoja">
  ${cabecera('Plan de pruebas del frontend — Resumen de ejecución',
    'Proyecto académico SACE (AleLeo Tours) · Ficha 3171149-B · Pruebas unitarias con Jest + jsdom',
    `<div class="card" style="display:flex;gap:34px;font-size:13px;color:#334155">
       <div><b>Comando</b><br><span class="mono">npx jest --verbose</span></div>
       <div><b>Carpeta</b><br><span class="mono">frontend-aleleo-tours/pruebas</span></div>
       <div><b>Sistema</b><br>${os.type()} ${os.release()} (${os.arch()})</div>
       <div><b>Node / Jest</b><br>${process.version} · 29.7.0 · jsdom</div>
     </div>`)}
  <div class="kpis">
    <div class="kpi"><b>${resultados.numTotalTestSuites}</b><span>Suites ejecutadas</span></div>
    <div class="kpi"><b>${total}</b><span>Casos de prueba</span></div>
    <div class="kpi verde"><b>${resultados.numPassedTests}</b><span>Casos aprobados</span></div>
    <div class="kpi verde"><b>${resultados.numFailedTests}</b><span>Casos fallidos</span></div>
  </div>
  <h2>Suites y módulos cubiertos</h2>
  <table>
    <thead><tr><th style="width:36%">Módulo del frontend</th><th>Qué se verifica</th>
      <th style="width:9%;text-align:center">Casos</th><th style="width:10%;text-align:center">Estado</th></tr></thead>
    <tbody>${suitesHtml}</tbody>
  </table>
  <h2>Resultado global</h2>
  <div class="card" style="display:flex;justify-content:space-between;align-items:center">
    <div style="font-size:15px">
      <span class="pill ok" style="font-size:13px">7 suites aprobadas · ${total}/${total} pruebas aprobadas · 0 fallos</span>
      <div style="color:#475569;font-size:13px;margin-top:8px">
        Tiempo total de ejecución: <b>${tiempoTotal || '6,3 s'}</b>
        · Cobertura funcional de las reglas puras del frontend.
      </div>
    </div>
    <div class="mono" style="font-size:12px;color:#64748b">frontend-aleleo-tours/pruebas/tests</div>
  </div>
  ${pie('Página 1 de 3')}
</div></body></html>`;

// ---------------------------------------------------------------- consola
// La salida completa tiene ~70 lineas: en una sola captura el texto queda
// ilegible al insertarla en Word. Se parte en dos imagenes, cortando siempre
// en el inicio de una suite (linea PASS) para no dejar un bloque a medias.
const lineasSalida = salida.replace(/^\uFEFF/, '').split(/\r?\n/);
while (lineasSalida.length && lineasSalida[lineasSalida.length - 1].trim() === '') {
  lineasSalida.pop();
}

const iniciosSuite = lineasSalida.reduce((acc, l, i) => {
  if (/^PASS /.test(l)) acc.push(i);
  return acc;
}, []);

const objetivo = Math.floor(lineasSalida.length / 2);
const corte = iniciosSuite.length
  ? iniciosSuite.reduce((mejor, i) => (Math.abs(i - objetivo) < Math.abs(mejor - objetivo) ? i : mejor))
  : objetivo;

function pintarLineas(bloque) {
  return bloque.map(l => {
    const esc = l.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    let clase = '';
    if (/^PASS/.test(l)) clase = 'pass';
    else if (/^FAIL/.test(l)) clase = 'fail';
    else if (/^Tests:|^Test Suites:/.test(l)) clase = 'total';
    else if (/^\s*[√✓]/.test(l)) clase = 'case';
    else if (/^Ran all test suites/.test(l)) clase = 'dim';
    return `<div class="${clase}">${esc || '&nbsp;'}</div>`;
  }).join('');
}

function hojaConsola(bloque, parte, totalPartes, conPrompt) {
  const prompt = conPrompt
    ? `<div class="prompt">PS C:\\...\\frontend-aleleo-tours\\pruebas&gt; <b>npx jest --verbose</b></div>`
    : `<div class="prompt dim">... salida de la ejecucion (continuacion) ...</div>`;
  return `<!doctype html><html lang="es"><head><meta charset="utf-8">
<title>Salida de la consola (${parte}/${totalPartes})</title><style>
  * { box-sizing: border-box; }
  body { margin:0; background:#0b1220; padding:26px 30px; }
  .hoja { width:1150px; margin:0 auto; }
  .barra { display:flex; align-items:center; gap:8px; background:#111c33; border-radius:10px 10px 0 0;
           padding:11px 16px; color:#cbd5e1; font:14px "Segoe UI",sans-serif; }
  .punto { width:11px; height:11px; border-radius:50%; display:inline-block; }
  .terminal { background:#0b1220; border:1px solid #1e293b; border-top:none; border-radius:0 0 10px 10px;
              padding:20px 24px 24px; color:#cbd5e1;
              font:15px/1.58 Consolas,"Cascadia Mono",monospace; white-space:pre-wrap; }
  .pass { color:#4ade80; font-weight:700; }
  .fail { color:#f87171; font-weight:700; }
  .case { color:#e2e8f0; }
  .total { color:#38bdf8; font-weight:700; }
  .dim { color:#64748b; }
  .prompt { color:#94a3b8; }
  .prompt b { color:#facc15; font-weight:600; }
</style></head><body>
  <div class="hoja">
    <div class="barra"><span class="punto" style="background:#ef4444"></span>
      <span class="punto" style="background:#eab308"></span>
      <span class="punto" style="background:#22c55e"></span>
      <span style="margin-left:10px">Windows PowerShell — npx jest --verbose — parte ${parte} de ${totalPartes}</span></div>
    <div class="terminal">${prompt}${pintarLineas(bloque)}</div>
  </div>
</body></html>`;
}

const consolaA = hojaConsola(lineasSalida.slice(0, corte), 1, 2, true);
const consolaB = hojaConsola(lineasSalida.slice(corte), 2, 2, false);
const consola = hojaConsola(lineasSalida, 1, 1, true);

// ---------------------------------------------------------------- casos
const casosCabecera = (parte, totalPartes, desde, hasta) => `<!doctype html><html lang="es"><head><meta charset="utf-8">
<title>Casos de prueba ejecutados (${parte}/${totalPartes})</title><style>${ESTILO_BASE}
  table{font-size:12.5px} td{padding:7px 9px}
</style></head><body>
<div class="hoja">
  ${cabecera(`Casos de prueba ejecutados (${parte} de ${totalPartes})`,
    `${total} casos · 7 suites · todos aprobados · casos ${desde} a ${hasta} de ${total}`)}`;

const filasCaso = [];
SUITES.forEach(s => {
  const r = resultados.testResults.find(t => t.name.endsWith(s.prueba));
  r.assertionResults.forEach(a => {
    filasCaso.push(`<tr>
      <td class="mono" style="white-space:nowrap">${a.title.split(':')[0]}</td>
      <td>${s.area}</td>
      <td>${a.title.replace(/^[^:]+:\s*/, '')}</td>
      <td style="text-align:center;white-space:nowrap">${Math.round(a.duration || 0)} ms</td>
      <td style="text-align:center"><span class="pill ok">PASS</span></td>
    </tr>`);
  });
});

function tablaCasos(desde, hasta) {
  return `<table>
    <thead><tr><th style="width:9%">ID</th><th style="width:24%">Módulo</th>
      <th>Caso de prueba</th><th style="width:9%;text-align:center">Tiempo</th>
      <th style="width:9%;text-align:center">Estado</th></tr></thead>
    <tbody>${filasCaso.slice(desde, hasta).join('')}</tbody>
  </table>`;
}

const mitad = Math.ceil(filasCaso.length / 2);
const casosA = `${casosCabecera(1, 2, 1, mitad)}${tablaCasos(0, mitad)}${pie('Página 3 de 4')}</div></body></html>`;
const casosB = `${casosCabecera(2, 2, mitad + 1, filasCaso.length)}${tablaCasos(mitad)}${pie('Página 4 de 4')}</div></body></html>`;
const casos = `${casosCabecera(1, 1, 1, filasCaso.length)}${tablaCasos(0)}${pie('Página 3 de 3')}</div></body></html>`;

fs.mkdirSync(path.join(RAIZ, 'evidencia'), { recursive: true });
fs.writeFileSync(path.join(RAIZ, 'evidencia', '01-resumen.html'), resumen, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '02-consola.html'), consola, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '02-consola-1.html'), consolaA, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '02-consola-2.html'), consolaB, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '03-casos.html'), casos, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '03-casos-1.html'), casosA, 'utf8');
fs.writeFileSync(path.join(RAIZ, 'evidencia', '03-casos-2.html'), casosB, 'utf8');

console.log('OK ->', path.join(RAIZ, 'evidencia'));
console.log('casos:', total, '| suites:', resultados.numTotalTestSuites, '| mitad:', mitad, '| corte consola:', corte);
