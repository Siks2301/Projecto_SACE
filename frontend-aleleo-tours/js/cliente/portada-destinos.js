/* ==========================================================================
   AleLeo Tours - portada-destinos.js
   Pinta las tarjetas de "Destinos destacados" de la portada.

   Estas tarjetas antes vivian escritas a mano en index.html, con los precios
   metidos a dedo. El resultado era que la portada vendia Cartagena a
   1.299.000 mientras el catalogo y la base de datos la tenian a 320.000.
   Ahora se leen del mismo origen que el catalogo (destinos-datos.js), asi
   que es imposible que se vuelvan a desincronizar.
   ========================================================================== */

'use strict';

// Cuantas tarjetas caben en la portada. El catalogo muestra los 7.
const MAX_DESTINOS_PORTADA = 4;

// Construye el HTML de una tarjeta. Mismo esqueleto que el catalogo, pero sin
// el boton de reservar: en la portada el boton lleva al catalogo completo.
function tarjetaPortada(d) {
  const promo = etiquetaTipoServicio(d.tipoServicio);
  const categorias = etiquetaCategorias(d.categorias);

  return `
    <article class="destino-card">
      <div class="destino-img-wrapper">
        ${promo ? `<span class="badge-card-promo">${escaperHtml(promo.toUpperCase())}</span>` : ''}
        <img src="${escaperHtml(d.img)}" alt="${escaperHtml(d.titulo || d.nombre)}, ${escaperHtml(d.pais)}" loading="lazy">
        ${categorias ? `<span class="badge-card-tag">${escaperHtml(categorias.toUpperCase())}</span>` : ''}
      </div>
      <div class="destino-card-body">
        <h3>${escaperHtml(d.nombre)}</h3>
        <p>${escaperHtml(d.descripcion)}</p>
        <div class="destino-price-box">
          <div>
            <div class="price-label">Desde por persona</div>
            <div class="price-value">${formatearPrecio(d.precio)}</div>
          </div>
          ${incluyeVuelos(d.tipoServicio)
            ? '<span class="badge-card-incluye">Vuelo incluido</span>'
            : ''}
        </div>
        <a class="btn-reservar-card" href="destinos.html?destino=${encodeURIComponent(d.nombre)}">
          Reservar
        </a>
      </div>
    </article>
  `;
}

async function renderizarDestinosPortada() {
  const contenedor = $('destinos-grid');
  if (!contenedor) return;

  contenedor.innerHTML = `
    <div class="galeria-carga text-center py-5 w-100">
      <div class="spinner-border text-primary" role="status"></div>
      <p class="mt-2 text-muted">Cargando destinos...</p>
    </div>
  `;

  const destinos = await obtenerDestinos();
  const destacados = destinos.slice(0, MAX_DESTINOS_PORTADA);

  if (destacados.length === 0) {
    contenedor.innerHTML = '';
    return;
  }

  contenedor.innerHTML = destacados.map(tarjetaPortada).join('');
}

document.addEventListener('DOMContentLoaded', renderizarDestinosPortada);
