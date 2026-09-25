/* ==========================================================================
   AleLeo Tours - destinos.js
   Modulos: Datos dinamicos (Backend SACE /api/servicios) - Galeria - Filtros - Modal - Reserva
   ========================================================================== */

'use strict';

// Este archivo usa lo que define destinos-datos.js (cargado antes en el HTML):
// $, normalizarBusqueda, normalizarVarios, resolverImagen, resolverCategorias,
// mapearServicio, obtenerDestinos, formatearPrecio, DESTINOS_DEFAULT,
// etiquetaTipoServicio, etiquetaCategorias e incluyeVuelos.
//
// Ahi vive la lista de destinos y la carga desde la API, para que la portada
// (index.html) y este catalogo muestren exactamente los mismos precios.

// Limites de viajeros por reserva. El <input type="number"> declara min/max,
// pero el formulario usa "novalidate" y el JS los ignoraba, asi que un cliente
// podia reservar cualquier cantidad. Estos valores son la fuente de verdad.
const MIN_PASAJEROS_RESERVA = 1;
const MAX_PASAJEROS_RESERVA = 10;

// Lee el numero de viajeros y lo acota al rango permitido.
function leerPasajeros() {
  const n = parseInt(($('res-pasajeros') || {}).value, 10);
  if (!Number.isFinite(n)) return MIN_PASAJEROS_RESERVA;
  return Math.min(MAX_PASAJEROS_RESERVA, Math.max(MIN_PASAJEROS_RESERVA, n));
}

// La lista de destinos, la normalizacion de textos y la carga desde la API
// viven en destinos-datos.js. Este archivo solo se ocupa de pintar la
// galeria, manejar los filtros y el modal de reserva.

// Estado de la pagina. Se parte de la copia local y obtenerDestinos() la
// reemplaza por la de la API si responde.
let DESTINOS = [...DESTINOS_DEFAULT];
let destinoSeleccionado = null;
let filtroActivo = 'todos';

// Pide los destinos al backend y repinta la galeria. obtenerDestinos() nunca
// lanza: si la API falla devuelve la copia local, asi que la pagina siempre
// queda usable.
async function cargarDestinosDesdeBackend() {
  const galeria = $('galeria-destinos');
  if (galeria) {
    galeria.innerHTML = '<div class="galeria-carga text-center py-5 w-100"><div class="spinner-border text-primary" role="status"></div><p class="mt-2 text-muted">Cargando destinos en tiempo real...</p></div>';
  }

  DESTINOS = await obtenerDestinos();

  leerParametrosBusqueda();
}

function mostrarMensajeModal(texto, tipo) {
  const el = $('msg-reserva');
  if (!el) return;
  el.textContent = texto;
  el.className = 'mensaje ' + (tipo === 'error' ? 'mensaje-error' : 'mensaje-ok');
  el.style.display = 'block';
  el.setAttribute('role', 'alert');
}

function renderizarDestinos(lista) {
  const galeria  = $('galeria-destinos');
  const contador = $('resultado-count');
  if (!galeria) return;

  galeria.innerHTML = '';

  if (contador) {
    contador.textContent = lista.length === 0
      ? 'No hay destinos en esta categor\u00eda.'
      : lista.length + ' destino' + (lista.length !== 1 ? 's' : '') + ' encontrado' + (lista.length !== 1 ? 's' : '');
  }

  if (lista.length === 0) {
    galeria.innerHTML = `
      <div class="galeria-estado">
        <i class="bi bi-geo-alt-fill galeria-estado-icono" aria-hidden="true"></i>
        <p class="galeria-estado-texto">No encontramos destinos para esta categor\u00eda.</p>
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

    // Las dos etiquetas salen del dato real del servicio. Antes iban fijas
    // ("TODO INCLUIDO" y "Vuelos Incluidos") y se mostraban igual en un tour
    // de finca cafetera que en un paquete con tiquete aereo.
    const promo = etiquetaTipoServicio(d.tipoServicio);
    const categorias = etiquetaCategorias(d.categorias);

    card.innerHTML = `
      <div class="destino-img-wrapper">
        ${promo ? `<span class="badge-card-promo">${escaperHtml(promo.toUpperCase())}</span>` : ''}
        <img src="${escaperHtml(d.img)}" alt="Paisaje de ${escaperHtml(d.nombre)}, ${escaperHtml(d.pais)}" loading="lazy">
        ${d.duracion ? `<span class="badge-card-tag"><i class="bi bi-clock-history"></i> ${escaperHtml(d.duracion)}</span>` : ''}
      </div>
      <div class="destino-card-body">
        <h3>${escaperHtml(d.nombre)}, <small class="destino-card-pais">${escaperHtml(d.pais)}</small></h3>
        ${d.titulo && d.titulo !== d.nombre ? `<p class="destino-card-titulo"><i class="bi bi-tag-fill text-warning"></i> ${escaperHtml(d.titulo)}</p>` : ''}
        <p>${escaperHtml(d.descripcion)}</p>
        ${categorias ? `<p class="destino-card-categorias">${escaperHtml(categorias)}</p>` : ''}
        <div class="destino-price-box">
          <div>
            <div class="price-label">Desde por persona</div>
            <div class="price-value">${formatearPrecio(d.precio)}</div>
          </div>
          ${incluyeVuelos(d.tipoServicio)
            ? '<span class="badge-card-incluye">Vuelo incluido</span>'
            : ''}
        </div>
        <button class="btn-reservar-card"
                onclick="event.stopPropagation(); abrirModal(Number(${d.id}))"
                aria-label="Reservar ${escaperHtml(d.nombre)}">
          <i class="bi bi-calendar-check-fill" aria-hidden="true"></i> Reservar
        </button>
      </div>
    `;
    galeria.appendChild(card);
  });
}

function abrirModal(id) {
  const sesion = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');
  if (!sesion) {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar('Debes iniciar sesi\u00f3n para reservar tu viaje.', 'info');
    }
    setTimeout(() => { window.location.href = 'login.html'; }, 1000);
    return;
  }

  destinoSeleccionado = DESTINOS.find(d => d.id === id);
  if (!destinoSeleccionado) return;

  $('modal-titulo').innerHTML = `<i class="bi bi-airplane-fill text-primary"></i> Reservar a ${escaperHtml(destinoSeleccionado.nombre)}`;
  $('modal-sub').textContent = `${destinoSeleccionado.titulo || destinoSeleccionado.nombre} \u00b7 Desde ${formatearPrecio(destinoSeleccionado.precio)} por persona`;

  $('res-nombre').value    = sesion.nombre || '';
  $('res-correo').value    = sesion.correo || '';
  $('res-fecha').value     = '';
  $('res-pasajeros').value = 1;
  $('res-fecha').min       = new Date().toISOString().split('T')[0];
  $('res-solicitud').checked = true;

  actualizarTotal();
  $('msg-reserva').style.display = 'none';
  $('overlay-reserva').classList.add('abierto');

  setTimeout(() => $('res-fecha').focus(), 100);
}

function cerrarModal() {
  const overlay = $('overlay-reserva');
  if (overlay) overlay.classList.remove('abierto');
  destinoSeleccionado = null;
  const totalDiv = $('total-precio');
  if (totalDiv) totalDiv.style.display = 'none';
}

function actualizarTotal() {
  if (!destinoSeleccionado) return;
  const input = $('res-pasajeros');
  const pasajeros = leerPasajeros();
  // Refleja en el campo el valor ya acotado, para que el total mostrado y lo
  // que se enviara coincidan (si escriben 999, se ajusta a 10).
  if (input && input.value !== '' && String(pasajeros) !== input.value) {
    input.value = pasajeros;
  }
  const total = destinoSeleccionado.precio * pasajeros;
  const totalValor = $('total-valor');
  const totalPrecio = $('total-precio');
  if (totalValor) totalValor.textContent = formatearPrecio(total);
  if (totalPrecio) totalPrecio.style.display = 'block';
}

function inicializarFormularioReserva() {
  const inpPasajeros = $('res-pasajeros');
  if (inpPasajeros) {
    inpPasajeros.addEventListener('input', actualizarTotal);
  }

  const form = $('form-reserva');
  if (!form) return;

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    if (!destinoSeleccionado) return;

    const fecha       = $('res-fecha').value;
    const pasajerosRaw = parseInt($('res-pasajeros').value, 10);
    const pasajeros   = leerPasajeros();
    const nombre      = $('res-nombre').value.trim();
    const correo      = $('res-correo').value.trim();
    const comoSolicitud = $('res-solicitud').checked;

    if (!fecha) {
      mostrarMensajeModal('Por favor selecciona una fecha para tu viaje.', 'error');
      return;
    }
    if (Number.isFinite(pasajerosRaw)
        && (pasajerosRaw < MIN_PASAJEROS_RESERVA || pasajerosRaw > MAX_PASAJEROS_RESERVA)) {
      mostrarMensajeModal(
        `Puedes reservar entre ${MIN_PASAJEROS_RESERVA} y ${MAX_PASAJEROS_RESERVA} viajeros por reserva. Para grupos m\u00e1s grandes, d\u00e9janos tu solicitud y un asesor te ayudar\u00e1.`,
        'error');
      return;
    }
    if (new Date(fecha + 'T12:00:00') <= new Date()) {
      mostrarMensajeModal('La fecha del viaje debe ser posterior al d\u00eda de hoy.', 'error');
      return;
    }

    const total    = destinoSeleccionado.precio * pasajeros;
    const reservas = JSON.parse(localStorage.getItem('aleleo_reservas') || '[]');
    const sesion   = Sesion.obtener ? Sesion.obtener() : JSON.parse(sessionStorage.getItem('aleleo_sesion') || 'null');

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
    localStorage.setItem('aleleo_reservas', JSON.stringify(reservas));

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
          mensajeFinal = '\u00a1Reserva confirmada en el sistema! Un asesor AleLeo Tours se contactar\u00e1 contigo.';
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
  const btnCerrar = $('cerrar-modal');
  const btnCancelar = $('btn-cancelar');
  const overlay = $('overlay-reserva');

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

// Un destino coincide si el texto buscado aparece en su nombre o titulo
// (normalizados), o si el nombre del destino aparece dentro de la busqueda
// (ej. "Parque Tayrona" -> coincide con el plan "Tayrona").
function destinoCoincide(destino, buscado) {
  if (!buscado) return false;
  const nombre = normalizarBusqueda(destino.nombre);
  const titulo = normalizarBusqueda(destino.titulo);
  const haystack = `${nombre} ${titulo}`.trim();

  // Coincidencia directa (busqueda completa dentro del plan, o al reves).
  if (haystack.includes(buscado) || (nombre && buscado.includes(nombre))) return true;

  // Coincidencia por palabras: TODAS las palabras significativas de la
  // busqueda deben aparecer en el plan (ej. "cartagena de indias" -> Cartagena).
  const palabras = buscado.split(/\s+/).filter(w => w.length > 3);
  if (palabras.length === 0) return false;
  return palabras.every(p => haystack.includes(p));
}

function leerParametrosBusqueda() {
  const params    = new URLSearchParams(window.location.search);
  const origen    = params.get('origen');
  const destino   = params.get('destino');
  const fecha     = params.get('fecha');
  const pasajeros = params.get('pasajeros');
  const tipo      = params.get('tipo');

  // El banner resume una busqueda de vuelo completa (origen, fecha,
  // pasajeros). Un enlace de pie de pagina que solo trae ?destino= no es una
  // busqueda de vuelo, asi que en ese caso no se muestra: se verian guiones
  // vacios donde deberia ir el origen o la fecha.
  const mostrarBanner = () => {
    if (!origen) return;
    const banner = $('banner-busqueda');
    if (banner) {
      banner.classList.remove('oculto');
      const bOrigen = $('b-origen');
      const bDestino = $('b-destino');
      const bFecha = $('b-fecha');
      const bPasajeros = $('b-pasajeros');

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
  };

  let listaBase = DESTINOS;
  const tiposServicio = mapearTipoServicio(tipo);
  if (tiposServicio) {
    const filtradaPorTipo = DESTINOS.filter(d => tiposServicio.includes(d.tipoServicio));
    listaBase = filtradaPorTipo.length > 0 ? filtradaPorTipo : DESTINOS;
    if (typeof Toast !== 'undefined') {
      const etiquetas = { hotel: 'Solo Hoteles', pasadia: 'Pasad\u00edas y Tours', paquete: 'Vuelos + Hotel Todo Incluido' };
      Toast.mostrar(`Mostrando planes de: ${etiquetas[tipo] || 'Todos los planes'}`, 'info');
    }
  }

  if (!destino) {
    mostrarBanner();
    renderizarDestinos(listaBase);
    return;
  }

  mostrarBanner();

  const buscado = normalizarBusqueda(destino);
  // 1) Filtra primero contra TODOS los destinos, para que un destino valido
  //    no se pierda aunque su tipo de servicio no coincida con la pestana.
  let resultado = DESTINOS.filter(d => destinoCoincide(d, buscado));

  // 2) Si ademas hay pestana de tipo y el destino existe en ella, respeta el
  //    tipo; si no, conserva solo el destino (nunca muestra todo sin filtrar).
  if (resultado.length > 0 && tiposServicio) {
    const porTipo = resultado.filter(d => tiposServicio.includes(d.tipoServicio));
    if (porTipo.length > 0) resultado = porTipo;
  }

  if (resultado.length === 0) {
    if (typeof Toast !== 'undefined') {
      Toast.mostrar(`No encontramos planes para "${destino}". Prueba con otro destino.`, 'error');
    }
    renderizarDestinos(resultado);
    return;
  }

  renderizarDestinos(resultado);
}

document.addEventListener('DOMContentLoaded', () => {
  inicializarFiltros();
  inicializarCierreModal();
  inicializarFormularioReserva();
  cargarDestinosDesdeBackend();
});
