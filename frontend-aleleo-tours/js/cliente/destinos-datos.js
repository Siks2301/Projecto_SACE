/* ==========================================================================
   AleLeo Tours - destinos-datos.js
   Fuente unica de verdad de los destinos.

   Este archivo lo cargan TANTO la portada (index.html) COMO el catalogo
   (destinos.html). Antes cada pagina tenia su propia copia de los precios y
   se desincronizaron: la portada anunciaba 1.299.000 para Cartagena
   mientras la base de datos cobraba 320.000. Al compartir el modulo, las dos
   paginas leen exactamente los mismos numeros.
   ========================================================================== */

'use strict';

// --- Utilidades basicas ---
// Atajo para no repetir document.getElementById en todo el archivo.
const $ = id => document.getElementById(id);

// Normaliza texto: minusculas, sin acentos y sin espacios sobrantes.
// Un destino busca siempre con esta forma, asi que "medellin" y
// "medellin" (con o sin espacios) terminan siendo la misma cadena.
function normalizarBusqueda(texto) {
  return String(texto || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

// Normaliza varios campos a la vez y los une con un espacio, para poder
// buscar palabras clave sobre el conjunto ("nombre + titulo + tipo").
function normalizarVarios(...partes) {
  return partes.map(normalizarBusqueda).filter(Boolean).join(' ');
}

// Elige la foto segun el destino. Si el backend manda un destino nuevo del
// que no hay imagen, cae en la de Cartagena en vez de romper la tarjeta.
const IMAGENES_POR_NOMBRE = [
  [['cartagena'],                 'img/Cartagena.jpg'],
  [['san andres'],               'img/San Andres.jpg'],
  [['santa marta'],              'img/Santa Marta.jpg'],
  [['medellin'],                 'img/Medellin.jpg'],
  [['tayrona'],                  'img/Tayrona.jpg'],
  [['providencia'],              'img/Providencia.jpg'],
  [['salento', 'cocora', 'quindio'], 'img/Tayrona.jpg']
];

function resolverImagen(destino, nombre) {
  const texto = normalizarVarios(destino, nombre);
  for (const [claves, imagen] of IMAGENES_POR_NOMBRE) {
    if (claves.some(c => texto.includes(c))) return imagen;
  }
  return 'img/Cartagena.jpg';
}

// Clasifica el destino en las categorias que usa el filtro de la galeria.
const CATEGORIAS_POR_PALABRA = [
  ['playa',      ['playa', 'mar', 'cartagena', 'santa marta', 'san andres']],
  ['ciudad',     ['ciudad', 'medellin', 'bogota', 'cali']],
  ['naturaleza', ['naturaleza', 'tayrona', 'selva', 'ecoturismo', 'amazonas', 'salento', 'cocora', 'quindio']],
  ['isla',       ['isla', 'san andres', 'providencia']]
];

function resolverCategorias(destino, nombre, tipoServicio) {
  const texto = normalizarVarios(destino, nombre, tipoServicio);
  const cats = [];
  for (const [categoria, palabras] of CATEGORIAS_POR_PALABRA) {
    if (palabras.some(p => texto.includes(p))) cats.push(categoria);
  }
  return cats.length > 0 ? cats : ['playa', 'ciudad'];
}

// Traduce un servicio del backend al mismo formato que usa DESTINOS_DEFAULT,
// para que la portada y el filtro trabajen igual con los dos origenes.
function mapearServicio(s) {
  return {
    id: s.id,
    nombre: s.destino || s.nombre,
    titulo: s.nombre,
    pais: 'Colombia',
    descripcion: s.descripcion || 'Disfruta de este incre\u00edble plan con AleLeo Tours.',
    precio: Number(s.precio) || 0,
    duracion: s.duracion || 'Plan personalizado',
    tipoServicio: s.tipoServicio || 'PAQUETE_TODO_INCLUIDO',
    img: resolverImagen(s.destino, s.nombre),
    categorias: resolverCategorias(s.destino, s.nombre, s.tipoServicio)
  };
}

// Copia local. Solo se usa si el backend no responde, para que la pagina
// nunca quede vacia. Los precios de aqui son los mismos que hay en la base
// de datos: si cambian alla, hay que cambiarlos tambien.
const DESTINOS_DEFAULT = [
  {
    id: 7,
    nombre: 'Salento',
    titulo: 'Tour Cafetero Salento y Valle de Cocora',
    pais: 'Colombia',
    descripcion: 'Recorrido por las palmas de cera m\u00e1s altas del mundo y una finca cafetera tradicional en el Quind\u00edo.',
    precio: 230000,
    duracion: '2 d\u00edas / 1 noche',
    tipoServicio: 'TOUR_EXCURSION',
    img: 'img/Tayrona.jpg',
    categorias: ['naturaleza', 'ciudad']
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
    nombre: 'San Andrés',
    titulo: 'Vuelo + Hotel San Andrés',
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
    nombre: 'Medellín',
    titulo: 'Tour Medellín Innovadora y Café',
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

// --- Etiquetas legibles ---
// Antes cada tarjeta pintaba "Vuelos Incluidos" sin mirar el tipo de
// servicio, hasta en el tour de una finca cafetera que no incluye vuelo.
// Estas etiquetas se derivan del dato real del servicio.
const ETIQUETA_TIPO_SERVICIO = {
  PAQUETE_TODO_INCLUIDO: 'Todo incluido',
  VUELO_HOTEL: 'Vuelo + hotel',
  TOUR_EXCURSION: 'Tour',
  PASADIA: 'Pasad\u00eda'
};

const ETIQUETA_CATEGORIA = {
  playa: 'Playa',
  ciudad: 'Ciudad',
  naturaleza: 'Naturaleza',
  isla: 'Isla'
};

// Devuelve el texto del tipo de servicio, o cadena vacia si no hay tabla.
function etiquetaTipoServicio(tipoServicio) {
  return ETIQUETA_TIPO_SERVICIO[tipoServicio] || '';
}

// Une hasta dos categorias: "Playa y cultura", "Naturaleza y playa"...
function etiquetaCategorias(categorias) {
  if (!Array.isArray(categorias)) return '';
  return categorias.slice(0, 2).map(c => ETIQUETA_CATEGORIA[c] || c).join(' y ');
}

// Un servicio incluye tiquete aereo solo si su tipo lo dice. Un tour o una
// pasadia no llevan vuelo, asi que no deben anunciarlo.
function incluyeVuelos(tipoServicio) {
  return tipoServicio === 'PAQUETE_TODO_INCLUIDO' || tipoServicio === 'VUELO_HOTEL';
}

// --- Carga desde el backend ---
// Devuelve siempre una lista usable: si la API falla o devuelve algo raro,
// cae en la copia local. Nunca lanza, para que la pagina no se quede en blanco.
async function obtenerDestinos() {
  try {
    const resp = await fetch(`${API_BASE}/servicios`, {
      headers: { 'Accept': 'application/json' }
    });
    if (resp.ok) {
      const servicios = await resp.json();
      if (Array.isArray(servicios) && servicios.length > 0) {
        return servicios.map(mapearServicio);
      }
    }
  } catch (error) {
    // Sin backend (o sin red) se conserva la lista local. No hace falta avisar.
  }
  return [...DESTINOS_DEFAULT];
}

// Formatea un monto como lo ve un cliente: "$ 320.000 COP"
function formatearPrecio(v) {
  return '$ ' + Number(v || 0).toLocaleString('es-CO') + ' COP';
}
