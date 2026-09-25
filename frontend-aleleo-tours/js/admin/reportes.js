/* ==========================================================================
   AleLeo Tours - reportes.js
   Módulo Analítico y Reportes Estadísticos SACE
   ========================================================================== */

'use strict';

let chartEstadosInst = null;
let chartCategoriasInst = null;
let chartTendenciaInst = null;
let chartEmpleadosInst = null;
let autoRefreshTimer = null;

let filtroFechaDesde = '';
let filtroFechaHasta = '';

document.addEventListener('DOMContentLoaded', () => {
  // 1. Verificar autenticación y permisos de rol
  const sesion = typeof Sesion !== 'undefined' ? Sesion.obtener() : null;
  if (!sesion || !sesion.token) {
    window.location.href = 'login.html';
    return;
  }

  if (sesion.tipoUsuario !== 'ADMINISTRADOR') {
    if (typeof Toast !== 'undefined') Toast.mostrar('Acceso denegado. El Dashboard de Reportes y Estadísticas es exclusivo para Administradores.', 'error');
    setTimeout(() => { window.location.href = 'index.html'; }, 1000);
    return;
  }

  // 2. Configurar eventos de los botones de filtro por fecha
  configurarEventosFiltro();

  // 3. Configurar evento de exportar CSV
  const btnExportCsv = document.getElementById('btn-exportar-csv');
  if (btnExportCsv) {
    btnExportCsv.addEventListener('click', exportarReporteCSV);
  }

  // 4. Configurar switch de auto-refresco
  const toggleAuto = document.getElementById('toggle-autorefresh');
  if (toggleAuto) {
    toggleAuto.addEventListener('change', (e) => {
      if (e.target.checked) {
        if (typeof Toast !== 'undefined') Toast.mostrar('Auto-actualización activada (cada 30s).', 'info');
        autoRefreshTimer = setInterval(() => {
          cargarDashboard(filtroFechaDesde, filtroFechaHasta, true);
        }, 30000);
      } else {
        if (autoRefreshTimer) clearInterval(autoRefreshTimer);
        if (typeof Toast !== 'undefined') Toast.mostrar('Auto-actualización desactivada.', 'info');
      }
    });
  }

  // 5. Inicializar carga del Dashboard con todo el histórico
  cargarDashboard();
});

function configurarEventosFiltro() {
  const presetBtns = document.querySelectorAll('[data-preset]');
  const inputDesde = document.getElementById('filtro-fecha-desde');
  const inputHasta = document.getElementById('filtro-fecha-hasta');
  const btnAplicar = document.getElementById('btn-aplicar-fechas');

  presetBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      presetBtns.forEach(b => b.classList.remove('active-filter', 'btn-warning', 'text-dark'));
      presetBtns.forEach(b => b.classList.add('btn-outline-light'));

      e.target.classList.remove('btn-outline-light');
      e.target.classList.add('active-filter', 'btn-warning', 'text-dark');

      const preset = e.target.getAttribute('data-preset');
      const rango = calcularRangoPreset(preset);

      filtroFechaDesde = rango.desde;
      filtroFechaHasta = rango.hasta;

      if (inputDesde) inputDesde.value = filtroFechaDesde;
      if (inputHasta) inputHasta.value = filtroFechaHasta;

      cargarDashboard(filtroFechaDesde, filtroFechaHasta);
    });
  });

  if (btnAplicar) {
    btnAplicar.addEventListener('click', () => {
      filtroFechaDesde = inputDesde ? inputDesde.value : '';
      filtroFechaHasta = inputHasta ? inputHasta.value : '';

      presetBtns.forEach(b => {
        b.classList.remove('active-filter', 'btn-warning', 'text-dark');
        b.classList.add('btn-outline-light');
      });

      cargarDashboard(filtroFechaDesde, filtroFechaHasta);
    });
  }
}

function calcularRangoPreset(preset) {
  const hoy = new Date();
  const format = (d) => d.toISOString().split('T')[0];

  if (preset === 'HOY') {
    const dStr = format(hoy);
    return { desde: dStr, hasta: dStr };
  } else if (preset === 'SEMANA') {
    const hace7 = new Date();
    hace7.setDate(hoy.getDate() - 6);
    return { desde: format(hace7), hasta: format(hoy) };
  } else if (preset === 'MES_ACTUAL') {
    const inicioMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    return { desde: format(inicioMes), hasta: format(hoy) };
  } else if (preset === 'MES_ANTERIOR') {
    const inicioMesAnt = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
    const finMesAnt = new Date(hoy.getFullYear(), hoy.getMonth(), 0);
    return { desde: format(inicioMesAnt), hasta: format(finMesAnt) };
  }

  // Default: HISTORICO (Sin filtro de fecha)
  return { desde: '', hasta: '' };
}

function construirQueryParams(desde, hasta) {
  const params = new URLSearchParams();
  if (desde) params.append('desde', desde);
  if (hasta) params.append('hasta', hasta);
  const q = params.toString();
  return q ? `?${q}` : '';
}

async function cargarDashboard(desde = '', hasta = '', silencioso = false) {
  try {
    const query = construirQueryParams(desde, hasta);

    await Promise.all([
      cargarKpis(query),
      cargarGraficoEstados(query),
      cargarGraficoCategorias(query),
      cargarGraficoTendencia(query),
      cargarGraficoEmpleados(query)
    ]);

    if (!silencioso && (desde || hasta) && typeof Toast !== 'undefined') {
      Toast.mostrar(`Filtro aplicado: ${desde || 'Inicio'} a ${hasta || 'Hoy'}`, 'success');
    }
  } catch (err) {
    console.error('Error al cargar el dashboard de reportes:', err);
    if (!silencioso && typeof Toast !== 'undefined') {
      Toast.mostrar('Error al obtener algunas estadísticas del backend SACE.', 'error');
    }
  }
}

// 1. Cargar Tarjetas KPI con datos reales y métricas de recaudo
async function cargarKpis(query = '') {
  let resp = await fetch(`${API_BASE}/reportes/admin${query}`);
  if (!resp.ok) {
    resp = await fetch(`${API_BASE}/reportes/kpis${query}`);
  }
  if (!resp.ok) return;
  const kpis = await resp.json();

  document.getElementById('kpi-total').textContent = kpis.totalSolicitudes || 0;
  document.getElementById('kpi-pendientes').textContent = kpis.solicitudesPendientes || 0;
  document.getElementById('kpi-proceso').textContent = kpis.solicitudesEnProceso || 0;
  document.getElementById('kpi-resueltas').textContent = kpis.solicitudesResueltas || 0;
  document.getElementById('kpi-canceladas').textContent = kpis.solicitudesCanceladas || 0;
  document.getElementById('kpi-efectividad').textContent = `${kpis.porcentajeResueltas || 0}%`;

  // Métricas de Recaudo Financiero
  const formatCOP = val => '$ ' + Number(val || 0).toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' COP';
  if (document.getElementById('kpi-recaudado-breb')) {
    document.getElementById('kpi-recaudado-breb').textContent = formatCOP(kpis.totalRecaudadoBreB);
  }
  if (document.getElementById('kpi-pagos-count')) {
    document.getElementById('kpi-pagos-count').textContent = kpis.totalPagosConfirmados || 0;
  }
  if (document.getElementById('kpi-promedio-pago')) {
    document.getElementById('kpi-promedio-pago').textContent = formatCOP(kpis.promedioMontoPago);
  }
}

// 2. Gráfico de Dona: Estado de Solicitudes
async function cargarGraficoEstados(query = '') {
  const resp = await fetch(`${API_BASE}/reportes/estados${query}`);
  if (!resp.ok) return;
  const datos = await resp.json();

  const labels = datos.map(item => formatearEstado(item.etiqueta));
  const valores = datos.map(item => item.cantidad);

  const coloresMap = {
    'PENDIENTE': '#F5A524',
    'EN_PROCESO': '#1994D7',
    'RESUELTA': '#12A06B',
    'CANCELADA': '#D93025'
  };

  const backgroundColors = datos.map(i => coloresMap[i.etiqueta] || '#6C6C6B');

  const ctx = document.getElementById('chartEstados').getContext('2d');
  if (chartEstadosInst) chartEstadosInst.destroy();

  chartEstadosInst = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: labels,
      datasets: [{
        data: valores,
        backgroundColor: backgroundColors,
        borderWidth: 2,
        borderColor: '#ffffff'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { font: { family: 'Outfit', size: 12, weight: '600' } }
        }
      },
      cutout: '65%'
    }
  });
}

// 3. Gráfico de Barras: Solicitudes por Categoría
async function cargarGraficoCategorias(query = '') {
  const resp = await fetch(`${API_BASE}/reportes/categorias${query}`);
  if (!resp.ok) return;
  const datos = await resp.json();

  const labels = datos.length > 0 ? datos.map(item => formatearCategoria(item.etiqueta)) : ['Sin registros'];
  const valores = datos.length > 0 ? datos.map(item => item.cantidad) : [0];


  const ctx = document.getElementById('chartCategorias').getContext('2d');
  if (chartCategoriasInst) chartCategoriasInst.destroy();

  chartCategoriasInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Cantidad de Solicitudes',
        data: valores,
        backgroundColor: 'rgba(25, 148, 215, 0.78)',
        borderColor: '#0C86C4',
        borderWidth: 2,
        borderRadius: 8
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1, font: { family: 'Outfit', size: 12 } }
        },
        x: {
          ticks: { font: { family: 'Outfit', size: 11, weight: '600' } }
        }
      }
    }
  });
}

// 4. Gráfico de Líneas: Tendencia Temporal
async function cargarGraficoTendencia(query = '') {
  const resp = await fetch(`${API_BASE}/reportes/tendencia${query}`);
  if (!resp.ok) return;
  const datos = await resp.json();

  const labels = datos.length > 0 ? datos.map(item => item.etiqueta) : ['Mes Actual'];
  const valores = datos.length > 0 ? datos.map(item => item.cantidad) : [0];

  const ctx = document.getElementById('chartTendencia').getContext('2d');
  if (chartTendenciaInst) chartTendenciaInst.destroy();

  chartTendenciaInst = new Chart(ctx, {
    type: 'line',
    data: {
      labels: labels,
      datasets: [{
        label: 'Solicitudes Registradas',
        data: valores,
        borderColor: '#12A06B',
        backgroundColor: 'rgba(18, 160, 107, 0.15)',
        fill: true,
        tension: 0.35,
        pointBackgroundColor: '#12A06B',
        pointRadius: 5
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1, font: { family: 'Outfit', size: 12 } }
        },
        x: {
          ticks: { font: { family: 'Outfit', size: 11 } }
        }
      }
    }
  });
}

// 5. Gráfico de Barras Horizontales: Rendimiento Asesores
async function cargarGraficoEmpleados(query = '') {
  const resp = await fetch(`${API_BASE}/reportes/empleados${query}`);
  if (!resp.ok) return;
  const datos = await resp.json();

  const labels = datos.length > 0 ? datos.map(item => item.etiqueta) : ['Sin Asignaciones'];
  const valores = datos.length > 0 ? datos.map(item => item.cantidad) : [0];


  const ctx = document.getElementById('chartEmpleados').getContext('2d');
  if (chartEmpleadosInst) chartEmpleadosInst.destroy();

  chartEmpleadosInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Solicitudes Atendidas',
        data: valores,
        backgroundColor: 'rgba(245, 165, 36, 0.82)',
        borderColor: '#F5A524',
        borderWidth: 2,
        borderRadius: 8
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: {
          beginAtZero: true,
          ticks: { stepSize: 1, font: { family: 'Outfit', size: 12 } }
        },
        y: {
          ticks: { font: { family: 'Outfit', size: 11, weight: '600' } }
        }
      }
    }
  });
}

// EXPORTAR A EXCEL (CSV COMPATIBLE CON EXCEL / GOOGLE SHEETS)
async function exportarReporteCSV() {
  try {
    const query = construirQueryParams(filtroFechaDesde, filtroFechaHasta);
    const sesion = typeof Sesion !== 'undefined' ? Sesion.obtener() : null;
    const fechaActual = new Date().toLocaleString('es-CO');

    let respKpis = await fetch(`${API_BASE}/reportes/admin${query}`);
    if (!respKpis.ok) respKpis = await fetch(`${API_BASE}/reportes/kpis${query}`);

    const [respEstados, respCats, respEmps] = await Promise.all([
      fetch(`${API_BASE}/reportes/estados${query}`),
      fetch(`${API_BASE}/reportes/categorias${query}`),
      fetch(`${API_BASE}/reportes/empleados${query}`)
    ]);

    const kpis = respKpis.ok ? await respKpis.json() : {};
    const estados = respEstados.ok ? await respEstados.json() : [];
    const categorias = respCats.ok ? await respCats.json() : [];
    const empleados = respEmps.ok ? await respEmps.json() : [];

    let csvContent = '\uFEFF'; // BOM para que Excel abra UTF-8 correctamente con tildes

    csvContent += '===================================================\n';
    csvContent += 'REPORTE EJECUTIVO DE GESTIÓN Y RECAUDO SACE - ALELEO TOURS\n';
    csvContent += `Generado el: ${fechaActual}\n`;
    csvContent += `Generado por: ${sesion && sesion.nombre ? sesion.nombre : 'Administrador'}\n`;
    csvContent += `Rango de Fechas: ${filtroFechaDesde || 'Inicio'} a ${filtroFechaHasta || 'Hoy'}\n`;
    csvContent += '===================================================\n\n';

    // 1. Resumen Ejecutivo KPIs
    csvContent += '--- RESUMEN DE INDICADORES Y AUDITORÍA DE RECAUDO ---\n';
    csvContent += 'Métrica,Valor\n';
    csvContent += `Total Ingresos Recaudados,"$ ${Number(kpis.totalRecaudadoBreB || 0).toLocaleString('es-CO', { minimumFractionDigits: 2 })} COP"\n`;
    csvContent += `Total Pagos Confirmados,${kpis.totalPagosConfirmados || 0}\n`;
    csvContent += `Ticket Promedio por Transacción,"$ ${Number(kpis.promedioMontoPago || 0).toLocaleString('es-CO', { minimumFractionDigits: 2 })} COP"\n`;
    csvContent += `Total Solicitudes,${kpis.totalSolicitudes || 0}\n`;
    csvContent += `Solicitudes Pendientes,${kpis.solicitudesPendientes || 0}\n`;
    csvContent += `Solicitudes En Proceso,${kpis.solicitudesEnProceso || 0}\n`;
    csvContent += `Solicitudes Resueltas,${kpis.solicitudesResueltas || 0}\n`;
    csvContent += `Solicitudes Canceladas,${kpis.solicitudesCanceladas || 0}\n`;
    csvContent += `Porcentaje de Efectividad,${kpis.porcentajeResueltas || 0}%\n`;
    csvContent += `Total Clientes Registrados,${kpis.totalClientes || 0}\n`;
    csvContent += `Total Empleados,${kpis.totalEmpleados || 0}\n`;
    csvContent += `Total Servicios Disponibles,${kpis.totalServicios || 0}\n\n`;

    // 2. Distribución por Estado
    csvContent += '--- DISTRIBUCIÓN POR ESTADO ---\n';
    csvContent += 'Estado,Cantidad\n';
    estados.forEach(e => {
      csvContent += `"${formatearEstado(e.etiqueta)}",${e.cantidad}\n`;
    });
    csvContent += '\n';

    // 3. Distribución por Categoría
    csvContent += '--- SOLICITUDES POR CATEGORÍA ---\n';
    csvContent += 'Categoría,Cantidad\n';
    categorias.forEach(c => {
      csvContent += `"${formatearCategoria(c.etiqueta)}",${c.cantidad}\n`;
    });
    csvContent += '\n';

    // 4. Rendimiento de Asesores
    csvContent += '--- RENDIMIENTO DE ASESORES ---\n';
    csvContent += 'Nombre Asesor,Solicitudes Atendidas\n';
    empleados.forEach(emp => {
      csvContent += `"${emp.etiqueta}",${emp.cantidad}\n`;
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    const fechaNombre = new Date().toISOString().slice(0,10);

    link.setAttribute('href', url);
    link.setAttribute('download', `Reporte_Ejecutivo_SACE_${fechaNombre}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Reporte en Excel (CSV) descargado exitosamente.', 'success');
    }
  } catch (err) {
    console.error('Error al exportar reporte a CSV:', err);
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Error al generar la descarga del reporte CSV.', 'error');
    }
  }
}

// Descarga el Reporte Ejecutivo como PDF real generado en el backend
// (membrete, tablas de desglose, firma y numeración de páginas), en vez
// de depender de la impresión del navegador sobre el dashboard HTML.
async function descargarReportePdf() {
  const btn = document.getElementById('btn-descargar-pdf');
  const textoOriginal = btn ? btn.innerHTML : '';
  try {
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split"></i> Generando PDF...';
    }

    const query = construirQueryParams(filtroFechaDesde, filtroFechaHasta);
    const resp = await fetch(`${API_BASE}/reportes/pdf${query}`);

    if (!resp.ok) {
      throw new Error('El servidor respondió con estado ' + resp.status);
    }

    const blob = await resp.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    const fechaNombre = new Date().toISOString().slice(0, 10);
    a.href = url;
    a.download = `Reporte_Ejecutivo_SACE_${fechaNombre}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Reporte PDF generado y descargado exitosamente.', 'success');
    }
  } catch (err) {
    console.error('Error al descargar el reporte PDF:', err);
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('No se pudo generar el reporte PDF. Intenta de nuevo.', 'error');
    }
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = textoOriginal;
    }
  }
}

// Función para imprimir reporte limpio y profesional
function imprimirReporte() {
  const elFecha = document.getElementById('print-fecha-impresion');
  const elUsuario = document.getElementById('print-usuario-impresion');
  const sesion = typeof Sesion !== 'undefined' ? Sesion.obtener() : null;

  if (elFecha) {
    const ahora = new Date();
    let rStr = 'Generado: ' + ahora.toLocaleDateString('es-CO') + ' ' + ahora.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    if (filtroFechaDesde || filtroFechaHasta) {
      rStr += ` | Rango: ${filtroFechaDesde || 'Inicio'} a ${filtroFechaHasta || 'Hoy'}`;
    }
    elFecha.textContent = rStr;
  }

  if (elUsuario) {
    elUsuario.textContent = 'Generado por: ' + (sesion && sesion.nombre ? sesion.nombre : 'Usuario Autorizado SACE');
  }

  if (chartEstadosInst) chartEstadosInst.resize();
  if (chartCategoriasInst) chartCategoriasInst.resize();
  if (chartTendenciaInst) chartTendenciaInst.resize();
  if (chartEmpleadosInst) chartEmpleadosInst.resize();

  setTimeout(() => {
    window.print();
  }, 100);
}

// Auxiliares para formatear nombres de estados y categorías
function formatearEstado(est) {
  const mapa = {
    'PENDIENTE': 'Pendiente',
    'EN_PROCESO': 'En Proceso',
    'RESUELTA': 'Resuelta',
    'CANCELADA': 'Cancelada'
  };
  return mapa[est] || est;
}

function formatearCategoria(cat) {
  const mapa = {
    'COTIZACION': 'Cotizaciones',
    'RESERVA': 'Reservas',
    'CAMBIO_FECHA': 'Cambios de Fecha',
    'CANCELACION_REEMBOLSO': 'Cancelación / Reembolso',
    'PQR_SERVICIO': 'PQR Servicio',
    'CONSULTA_GENERAL': 'Consulta General'
  };
  return mapa[cat] || (cat ? cat.replace(/_/g, ' ') : 'Consulta General');
}
