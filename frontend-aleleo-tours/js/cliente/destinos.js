/* ==========================================================================
   AleLeo Tours - destinos.js
   Modulos: Datos dinamicos (Backend SACE /api/servicios) - Galeria - Filtros - Modal - Reserva
   ========================================================================== */

'use strict';

// Limites de viajeros por reserva. El <input type="number"> declara min/max,
// pero el formulario usa "novalidate" y el JS los ignoraba, asi que un cliente
// podia reservar cualquier cantidad. Estos valores son la fuente de verdad.
const MIN_PASAJEROS_RESERVA = 1;
const MAX_PASAJEROS_RESERVA = 10;

// Lee el numero de viajeros y lo acota al rango permitido.
function leerPasajeros() {
  const input = document.getElementById('res-pasajeros');
  const n = parseInt(input && input.value, 10);
  if (!Number.isFinite(n)) return MIN_PASAJEROS_RESERVA;
  return Math.min(MAX_PASAJEROS_RESERVA, Math.max(MIN_PASAJEROS_RESERVA, n));
}

const DESTINOS_DEFAULT = [
  {
    id: 99,
    nombre: 'Plan de Prueba SACE',
    titulo: 'Destino de Prueba (Test $1.000)',
    pais: 'Colombia',
    descripcion: 'Plan especial de prueba para validar reservación y pagos Bre-B @VXM301 por solo $1.000 pesos.',
    precio: 1000,
    duracion: '1 día / 1 noche',
    tipoServicio: 'PASADIA',
    img: 'img/Cartagena.jpg',
    categorias: ['playa', 'ciudad']
  },
  {
    id: 1,
    nombre: 'Cartagena',
    titulo: 'Paquete Vacacional Cartagena',
    pais: 'Colombia',
    descripcion: 'Ciudad amurallada llena de historia, colores y playas del Caribe.',
    precio: 320000,
    duracion: '3 d\u00edas / 2 noches',
    tipoServicio: 'PAQUETE_TODO_INCLUIDO',
    img: 'img/Cartagena.jpg',
    categorias: ['playa', 'ciudad']
  },
  {
    id: 2,
    nombre: 'San Andr\u00e9s',
    titulo: 'Vuelo + Hotel San Andr\u00e9s',
    pais: 'Colombia',
    descripcion: 'El famoso mar de siete colores te espera en esta isla tropical paradis\u00edaca.',
    precio: 480000,
    duracion: '4 d\u00edas / 3 noches',
    tipoServicio: 'VUELO_HOTEL',
    img: 'img/San Andres.jpg',
    categorias: ['isla', 'playa']
  },
  {
    id: 3,
    nombre: 'Santa Marta',
    titulo: 'Experiencia Santa Marta y Playas',
    pais: 'Colombia',
    descripcion: 'Naturaleza, Parque Tayrona y playas v\u00edrgenes en una sola ciudad caribe\u00f1a.',
    precio: 270000,
    duracion: '3 d\u00edas / 2 noches',
    tipoServicio: 'TOUR_EXCURSION',
    img: 'img/Santa Marta.jpg',
    categorias: ['playa', 'naturaleza']
  },
  {
    id: 4,
    nombre: 'Medell\u00edn',
    titulo: 'Tour Medell\u00edn Innovadora y Caf\u00e9',
    pais: 'Colombia',
    descripcion: 'La ciudad de la eterna primavera, innovaci\u00f3n, cultura y gastronom\u00eda paisa.',
    precio: 210000,
    duracion: '3 d\u00edas / 2 noches',
    tipoServicio: 'TOUR_EXCURSION',
    img: 'img/Medellin.jpg',
    categorias: ['ciudad']
  },
  {
    id: 5,
    nombre: 'Tayrona',
    titulo: 'Ecoturismo Parque Tayrona',
    pais: 'Colombia',
    descripcion: 'Parque natural \u00fanico donde la selva tropical se encuentra con el mar Caribe.',
    precio: 195000,
    duracion: '2 d\u00edas / 1 noche',
    tipoServicio: 'PASADIA',
    img: 'img/Tayrona.jpg',
    categorias: ['naturaleza', 'playa']
  },
  {
    id: 6,
    nombre: 'Providencia',
    titulo: 'Para\u00edso Providencia Isla Virgen',
    pais: 'Colombia',
    descripcion: 'La isla m\u00e1s pura del Caribe colombiano, reserva mundial de la biosfera.',
    precio: 560000,
    duracion: '5 d\u00edas / 4 noches',
    tipoServicio: 'PAQUETE_TODO_INCLUIDO',
    img: 'img/Providencia.jpg',
    categorias: ['isla', 'naturaleza']
  }
];

let DESTINOS = [...DESTINOS_DEFAULT];
let destinoSeleccionado = null;
let filtroActivo = 'todos';

function resolverImagen(destino, nombre) {
  const texto = `${destino || ''} ${nombre || ''}`.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  if (texto.includes('cartagena')) return 'img/Cartagena.jpg';
  if (texto.includes('san andres')) return 'img/San Andres.jpg';
  if (texto.includes('santa marta')) return 'img/Santa Marta.jpg';
  if (texto.includes('medellin')) return 'img/Medellin.jpg';
  if (texto.includes('tayrona')) return 'img/Tayrona.jpg';
  if (texto.includes('providencia')) return 'img/Providencia.jpg';
  return 'img/Cartagena.jpg';
}

function resolverCategorias(destino, nombre, tipoServicio) {
  const texto = `${destino || ''} ${nombre || ''} ${tipoServicio || ''}`.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  const cats = [];
  if (texto.includes('playa') || texto.includes('mar') || texto.includes('cartagena') || texto.includes('santa marta') || texto.includes('san andres')) {
    cats.push('playa');
  }
  if (texto.includes('ciudad') || texto.includes('medellin') || texto.includes('bogota') || texto.includes('cali')) {
    cats.push('ciudad');
  }
  if (texto.includes('naturaleza') || texto.includes('tayrona') || texto.includes('selva') || texto.includes('ecoturismo') || texto.includes('amazonas')) {
    cats.push('naturaleza');
  }
  if (texto.includes('isla') || texto.includes('san andres') || texto.includes('providencia')) {
    cats.push('isla');
  }
  return cats.length > 0 ? cats : ['playa', 'ciudad'];
}

async function cargarDestinosDesdeBackend() {
  const galeria = document.getElementById('galeria-destinos');
  if (galeria) {
    galeria.innerHTML = '<div class="text-center py-5 w-100" style="grid-column: 1/-1;"><div class="spinner-border text-primary" role="status"></div><p class="mt-2 text-muted">Cargando destinos en tiempo real...</p></div>';
  }

  try {
    const resp = await fetch(`${API_BASE}/servicios`, {
      headers: { 'Accept': 'application/json' }
    });

    if (resp.ok) {
      const serviciosBackend = await resp.json();
      if (Array.isArray(serviciosBackend) && serviciosBackend.length > 0) {
        const backendMapped = serviciosBackend.map(s => ({
          id: s.id,
          nombre: s.destino || s.nombre,
          titulo: s.nombre,
          pais: 'Colombia',
          descripcion: s.descripcion || 'Disfruta de este increíble plan con AleLeo Tours.',
          precio: Number(s.precio) || 0,
          duracion: s.duracion || 'Plan personalizado',
          tipoServicio: s.tipoServicio || 'PAQUETE_TODO_INCLUIDO',
          img: resolverImagen(s.destino, s.nombre),
          categorias: resolverCategorias(s.destino, s.nombre, s.tipoServicio)
        }));
        DESTINOS = [DESTINOS_DEFAULT[0], ...backendMapped];
      } else {
        DESTINOS = [...DESTINOS_DEFAULT];
      }
    } else {
      DESTINOS = [...DESTINOS_DEFAULT];
    }
  } catch (error) {
    DESTINOS = [...DESTINOS_DEFAULT];
  }

  leerParametrosBusqueda();
}

function formatearPrecio(v) {
  return '$ ' + Number(v || 0).toLocaleString('es-CO') + ' COP';
}

function mostrarMensajeModal(texto, tipo) {
  const el = document.getElementById('msg-reserva');
  if (!el) return;
  el.textContent = texto;
  el.className = 'mensaje ' + (tipo === 'error' ? 'mensaje-error' : 'mensaje-ok');
  el.style.display = 'block';
  el.setAttribute('role', 'alert');
}

function renderizarDestinos(lista) {
  const galeria  = document.getElementById('galeria-destinos');
  const contador = document.getElementById('resultado-count');
  if (!galeria) return;

  galeria.innerHTML = '';

  if (contador) {
    contador.textContent = lista.length === 0
      ? 'No hay destinos en esta categor\u00eda.'
      : lista.length + ' destino' + (lista.length !== 1 ? 's' : '') + ' encontrado' + (lista.length !== 1 ? 's' : '');
  }

  if (lista.length === 0) {
    galeria.innerHTML = `
      <div style="text-align:center; color:var(--color-muted); grid-column:1/-1; padding:40px 20px;">
        <i class="bi bi-geo-alt-fill" style="font-size:3rem; color:var(--color-primary); opacity:0.6;"></i>
        <p class="mt-2" style="font-size:1.1rem;">No encontramos destinos para esta categor\u00eda.</p>
        <button class="btn btn-outline-primary btn-sm mt-2" onclick="aplicarFiltro('todos')">Ver todos los destinos</button>
      </div>
    `;
    return;
  }

  lista.forEach(d => {
    const card = document.createElement('article');
    card.className = 'destino-card';
    card.setAttribute('role', 'listitem');
    card.setAttribute('tabindex', '0');
    card.setAttribute('aria-label', `${d.nombre}, ${d.pais}. Desde ${formatearPrecio(d.precio)}`);
    card.innerHTML = `
      <div class="destino-img-wrapper">
        <span class="badge-card-promo">TODO INCLUIDO</span>
        <img src="${escaperHtml(d.img)}" alt="Paisaje de ${escaperHtml(d.nombre)}, ${escaperHtml(d.pais)}" loading="lazy">
        ${d.duracion ? `<span class="badge-card-tag"><i class="bi bi-clock-history"></i> ${escaperHtml(d.duracion)}</span>` : ''}
      </div>
      <div class="destino-card-body">
        <h3>${escaperHtml(d.nombre)}, <small style="font-weight:normal; font-size:16px; color:var(--color-muted)">${escaperHtml(d.pais)}</small></h3>
        ${d.titulo && d.titulo !== d.nombre ? `<p style="font-weight:700; color:var(--color-primary-dark); margin-bottom:6px; font-size:14px;"><i class="bi bi-tag-fill text-warning"></i> ${escaperHtml(d.titulo)}</p>` : ''}
        <p>${escaperHtml(d.descripcion)}</p>
        <div class="destino-price-box">
          <div>
            <div class="price-label">Desde por persona</div>
            <div class="price-value">${formatearPrecio(d.precio)}</div>
          </div>
          <span class="badge bg-success-subtle text-success border border-success fw-bold px-2 py-1">Vuelos Incluidos</span>
        </div>
        <button class="btn-reservar-card"
                onclick="event.stopPropagation(); abrirModal(Number(${d.id}))"
                aria-label="Reservar plan a ${escaperHtml(d.nombre)}">
          <i class="bi bi-calendar-check-fill" aria-hidden="true"></i> Reservar Ahora
        </button>
      </div>
    `;
    galeria.appendChild(card);
  });
}

function abrirModal(id) {
  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');
  if (!sesion) {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Debes iniciar sesi\u00f3n para reservar tu viaje.', 'info');
    }
    setTimeout(() => { window.location.href = 'login.html'; }, 1000);
    return;
  }

  destinoSeleccionado = DESTINOS.find(d => d.id === id);
  if (!destinoSeleccionado) return;

  document.getElementById('modal-titulo').innerHTML = `<i class="bi bi-airplane-fill text-primary"></i> Reservar a ${escaperHtml(destinoSeleccionado.nombre)}`;
  document.getElementById('modal-sub').textContent = `${destinoSeleccionado.titulo || destinoSeleccionado.nombre} \u00b7 Desde ${formatearPrecio(destinoSeleccionado.precio)} por persona`;

  document.getElementById('res-nombre').value    = sesion.nombre || '';
  document.getElementById('res-correo').value    = sesion.correo || '';
  document.getElementById('res-fecha').value     = '';
  document.getElementById('res-pasajeros').value = 1;
  document.getElementById('res-fecha').min       = new Date().toISOString().split('T')[0];
  document.getElementById('res-solicitud').checked = true;

  actualizarTotal();
  document.getElementById('msg-reserva').style.display = 'none';
  document.getElementById('overlay-reserva').classList.add('abierto');

  setTimeout(() => document.getElementById('res-fecha').focus(), 100);
}

function cerrarModal() {
  const overlay = document.getElementById('overlay-reserva');
  if (overlay) overlay.classList.remove('abierto');
  destinoSeleccionado = null;
  const totalDiv = document.getElementById('total-precio');
  if (totalDiv) totalDiv.style.display = 'none';
}

function actualizarTotal() {
  if (!destinoSeleccionado) return;
  const input = document.getElementById('res-pasajeros');
  const pasajeros = leerPasajeros();
  // Refleja en el campo el valor ya acotado, para que el total mostrado y lo
  // que se enviara coincidan (si escriben 999, se ajusta a 10).
  if (input && input.value !== '' && String(pasajeros) !== input.value) {
    input.value = pasajeros;
  }
  const total = destinoSeleccionado.precio * pasajeros;
  const totalValor = document.getElementById('total-valor');
  const totalPrecio = document.getElementById('total-precio');
  if (totalValor) totalValor.textContent = formatearPrecio(total);
  if (totalPrecio) totalPrecio.style.display = 'block';
}

function inicializarFormularioReserva() {
  const inpPasajeros = document.getElementById('res-pasajeros');
  if (inpPasajeros) {
    inpPasajeros.addEventListener('input', actualizarTotal);
  }

  const form = document.getElementById('form-reserva');
  if (!form) return;

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!destinoSeleccionado) return;

    const fecha       = document.getElementById('res-fecha').value;
    const pasajerosRaw = parseInt(document.getElementById('res-pasajeros').value, 10);
    const pasajeros   = leerPasajeros();
    const nombre      = document.getElementById('res-nombre').value.trim();
    const correo      = document.getElementById('res-correo').value.trim();
    const comoSolicitud = document.getElementById('res-solicitud').checked;

    if (!fecha) {
      mostrarMensajeModal('Por favor selecciona una fecha para tu viaje.', 'error');
      return;
    }
    if (Number.isFinite(pasajerosRaw)
        && (pasajerosRaw < MIN_PASAJEROS_RESERVA || pasajerosRaw > MAX_PASAJEROS_RESERVA)) {
      mostrarMensajeModal(
        `Puedes reservar entre ${MIN_PASAJEROS_RESERVA} y ${MAX_PASAJEROS_RESERVA} viajeros por reserva. Para grupos más grandes, déjanos tu solicitud y un asesor te ayudará.`,
        'error');
      return;
    }
    if (new Date(fecha + 'T12:00:00') <= new Date()) {
      mostrarMensajeModal('La fecha del viaje debe ser posterior al d\u00eda de hoy.', 'error');
      return;
    }

    const total    = destinoSeleccionado.precio * pasajeros;
    const reservas = JSON.parse(localStorage.getItem('onvacation_reservas') || '[]');
    const sesion   = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('onvacation_sesion') || 'null');

    reservas.push({
      id:           Date.now(),
      servicioId:   destinoSeleccionado.id,
      destino:      destinoSeleccionado.nombre,
      titulo:       destinoSeleccionado.titulo,
      pais:         destinoSeleccionado.pais,
      precio:       total,
      fecha,
      pasajeros,
      usuario:      sesion ? sesion.correo : 'invitado',
      nombre,
      correo,
      fechaReserva: new Date().toISOString()
    });
    localStorage.setItem('onvacation_reservas', JSON.stringify(reservas));

    const btnConfirmar = document.querySelector('#form-reserva button[type="submit"]');
    let mensajeFinal = '\u00a1Reserva confirmada exitosamente! Te enviamos los detalles a tu correo.';

    if (comoSolicitud && sesion) {
      if (btnConfirmar) {
        btnConfirmar.disabled = true;
        btnConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Procesando...';
      }

      try {
        const payloadSolicitud = {
          fechaCreacion: new Date().toISOString(),
          titulo: `Reserva: ${destinoSeleccionado.nombre}`,
          asunto: `Reserva para ${pasajeros} persona(s) - ${destinoSeleccionado.nombre}`,
          descripcion: `Plan: ${destinoSeleccionado.titulo || destinoSeleccionado.nombre}.\n` +
                       `Destino: ${destinoSeleccionado.nombre} (${destinoSeleccionado.pais}).\n` +
                       `Fecha de viaje: ${fecha}.\n` +
                       `Pasajeros: ${pasajeros}.\n` +
                       `Total estimado: ${formatearPrecio(total)}.\n` +
                       `Contacto: ${nombre} (${correo}).`,
          estado: 'PENDIENTE',
          prioridad: 'MEDIA',
          categoria: 'RESERVA',
          clienteId: sesion.id,
          servicioGeneradoId: destinoSeleccionado.id
        };

        const resp = await fetch(`${API_BASE}/solicitudes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payloadSolicitud)
        });

        if (resp.ok) {
          mensajeFinal = '¡Reserva confirmada en el sistema! Un asesor AleLeo Tours se contactará contigo.';
        } else {
          mensajeFinal = 'Reserva registrada localmente. Un asesor procesar\u00e1 los detalles pronto.';
        }
      } catch (err) {
        mensajeFinal = 'Reserva guardada. Sincronizaremos con el servidor tan pronto haya conexi\u00f3n.';
      } finally {
        if (btnConfirmar) {
          btnConfirmar.disabled = false;
          btnConfirmar.innerHTML = '<i class="bi bi-check-circle-fill" aria-hidden="true"></i> Confirmar Reserva';
        }
      }
    }

    mostrarMensajeModal(mensajeFinal, 'ok');

    if (typeof Toast !== 'undefined') {
      Toast.mostrar(`\u00a1Reserva a ${destinoSeleccionado.nombre} confirmada!`, 'ok');
    }

    setTimeout(cerrarModal, 2000);
  });
}

function aplicarFiltro(categoria) {
  filtroActivo = categoria;
  document.querySelectorAll('.btn-filtro').forEach(btn => {
    const activo = btn.dataset.filtro === categoria;
    btn.classList.toggle('activo', activo);
    btn.setAttribute('aria-pressed', String(activo));
  });

  const filtrados = categoria === 'todos'
    ? DESTINOS
    : DESTINOS.filter(d => d.categorias && d.categorias.includes(categoria));

  renderizarDestinos(filtrados);
}

function inicializarFiltros() {
  document.querySelectorAll('.btn-filtro').forEach(btn => {
    btn.addEventListener('click', function () {
      aplicarFiltro(this.dataset.filtro);
    });
  });
}

function inicializarCierreModal() {
  const btnCerrar = document.getElementById('cerrar-modal');
  const btnCancelar = document.getElementById('btn-cancelar');
  const overlay = document.getElementById('overlay-reserva');

  if (btnCerrar) btnCerrar.addEventListener('click', cerrarModal);
  if (btnCancelar) btnCancelar.addEventListener('click', cerrarModal);
  if (overlay) {
    overlay.addEventListener('click', function (e) {
      if (e.target === this) cerrarModal();
    });
  }

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') cerrarModal();
  });
}

function mapearTipoServicio(tipo) {
  switch (tipo) {
    case 'hotel':   return ['VUELO_HOTEL'];
    case 'pasadia': return ['PASADIA', 'TOUR_EXCURSION'];
    case 'paquete': return ['PAQUETE_TODO_INCLUIDO', 'VUELO_HOTEL'];
    default:        return null;
  }
}

function leerParametrosBusqueda() {
  const params    = new URLSearchParams(window.location.search);
  const origen    = params.get('origen');
  const destino   = params.get('destino');
  const fecha     = params.get('fecha');
  const pasajeros = params.get('pasajeros');
  const tipo      = params.get('tipo');

  let listaBase = DESTINOS;
  const tiposServicio = mapearTipoServicio(tipo);
  if (tiposServicio) {
    const filtradaPorTipo = DESTINOS.filter(d => tiposServicio.includes(d.tipoServicio));
    listaBase = filtradaPorTipo.length > 0 ? filtradaPorTipo : DESTINOS;
    if (typeof Toast !== 'undefined') {
      const etiquetas = { hotel: 'Solo Hoteles 2x1', pasadia: 'Pasad\u00edas y Tours', paquete: 'Vuelos + Hotel Todo Incluido' };
      Toast.mostrar(`Mostrando planes de: ${etiquetas[tipo] || 'Todos los planes'}`, 'info');
    }
  }

  if (origen || destino) {
    const banner = document.getElementById('banner-busqueda');
    if (banner) {
      banner.classList.remove('oculto');
      const bOrigen = document.getElementById('b-origen');
      const bDestino = document.getElementById('b-destino');
      const bFecha = document.getElementById('b-fecha');
      const bPasajeros = document.getElementById('b-pasajeros');

      if (bOrigen) bOrigen.textContent = origen || '\u2013';
      if (bDestino) bDestino.textContent = destino || '\u2013';
      if (bFecha) {
        bFecha.textContent = fecha
          ? new Date(fecha + 'T12:00:00').toLocaleDateString('es-CO', { day:'2-digit', month:'short', year:'numeric' })
          : '\u2013';
      }
      if (bPasajeros) {
        bPasajeros.textContent = (pasajeros || 1) + (Number(pasajeros) === 1 ? ' pasajero' : ' pasajeros');
      }
    }

    if (destino) {
      const match = listaBase.filter(d =>
        d.nombre.toLowerCase().includes(destino.toLowerCase()) ||
        (d.titulo && d.titulo.toLowerCase().includes(destino.toLowerCase()))
      );
      if (match.length > 0) {
        renderizarDestinos(match);
        return;
      }
    }
  }

  renderizarDestinos(listaBase);
}

document.addEventListener('DOMContentLoaded', () => {
  inicializarFiltros();
  inicializarCierreModal();
  inicializarFormularioReserva();
  cargarDestinosDesdeBackend();
});