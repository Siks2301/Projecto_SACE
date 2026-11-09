/**
 * CP-13 a CP-21 - js/cliente/destinos-datos.js + js/cliente/destinos.js
 *
 * Es la fuente unica de verdad del catalogo: normalizacion de busquedas,
 * imagen y categorias por destino, mapeo del servicio del backend, etiquetas
 * del tipo de servicio, formato de precios y los limites de pasajeros.
 */
const { cargarModulos } = require('../helpers/cargar-frontend');

const mod = cargarModulos(
  ['js/cliente/destinos-datos.js', 'js/cliente/destinos.js'],
  {
    normalizarBusqueda: 'normalizarBusqueda',
    normalizarVarios: 'normalizarVarios',
    resolverImagen: 'resolverImagen',
    resolverCategorias: 'resolverCategorias',
    mapearServicio: 'mapearServicio',
    etiquetaTipoServicio: 'etiquetaTipoServicio',
    etiquetaCategorias: 'etiquetaCategorias',
    incluyeVuelos: 'incluyeVuelos',
    formatearPrecio: 'formatearPrecio',
    leerPasajeros: 'leerPasajeros',
    destinoCoincide: 'destinoCoincide',
    DESTINOS_DEFAULT: 'DESTINOS_DEFAULT',
    MIN_PASAJEROS_RESERVA: 'MIN_PASAJEROS_RESERVA',
    MAX_PASAJEROS_RESERVA: 'MAX_PASAJEROS_RESERVA'
  }
);

beforeEach(() => {
  document.body.innerHTML = '';
});

describe('destinos-datos.js - normalizacion de texto', () => {
  test('CP-13: pasa a minusculas, quita acentos y colapsa espacios', () => {
    expect(mod.normalizarBusqueda('  San   ANDRÉS ')).toBe('san andres');
    expect(mod.normalizarBusqueda('MEDELLÍN')).toBe('medellin');
    expect(mod.normalizarBusqueda('Cartagena de Indias')).toBe('cartagena de indias');
    expect(mod.normalizarBusqueda(null)).toBe('');
    expect(mod.normalizarBusqueda(undefined)).toBe('');
  });

  test('CP-14: normalizarVarios une campos y descarta los vacios', () => {
    expect(mod.normalizarVarios('Cartagena', '', 'PAQUETE TODO INCLUIDO'))
      .toBe('cartagena paquete todo incluido');
    expect(mod.normalizarVarios(null, '  ', undefined)).toBe('');
  });
});

describe('destinos-datos.js - imagen y categorias', () => {
  test('CP-15: resuelve la foto por destino y cae en Cartagena cuando no la conoce', () => {
    expect(mod.resolverImagen('San Andrés', 'Vuelo + Hotel')).toBe('img/San Andres.jpg');
    expect(mod.resolverImagen('Salento', 'Tour Cafetero')).toBe('img/Salento.jpg');
    expect(mod.resolverImagen('Cartagena', 'Paquete')).toBe('img/Cartagena.jpg');
    expect(mod.resolverImagen('Guajira', 'Plan nuevo')).toBe('img/Cartagena.jpg');
    expect(mod.resolverImagen(null, null)).toBe('img/Cartagena.jpg');
  });

  test('CP-16: clasifica el destino en las categorias del filtro', () => {
    expect(mod.resolverCategorias('Cartagena', 'Paquete', null)).toEqual(['playa']);
    expect(mod.resolverCategorias('San Andrés', 'Vuelo', null)).toEqual(['playa', 'isla']);
    expect(mod.resolverCategorias('Medellín', 'Tour', null)).toEqual(['ciudad']);
    expect(mod.resolverCategorias('Tayrona', 'Ecoturismo', null)).toEqual(['naturaleza']);
    // Destino desconocido: nunca devuelve lista vacia.
    expect(mod.resolverCategorias('Guajira', 'Plan', null)).toEqual(['playa', 'ciudad']);
  });

  test('CP-16b (OBS-01): el catalogo del backend pierde categorias frente a la copia local', () => {
    // La copia local declara Cartagena como playa + ciudad y Tayrona como
    // naturaleza + playa...
    const localCartagena = mod.DESTINOS_DEFAULT.find(d => d.nombre === 'Cartagena');
    const localTayrona = mod.DESTINOS_DEFAULT.find(d => d.nombre === 'Tayrona');
    expect(localCartagena.categorias).toEqual(['playa', 'ciudad']);
    expect(localTayrona.categorias).toEqual(['naturaleza', 'playa']);

    // ...pero resolverCategorias() solo reconoce las palabras clave de su
    // tabla, asi que al pintar el catalogo desde /api/servicios los filtros
    // "Ciudad" (Cartagena) y "Playa" (Tayrona) dejan de encontrarlas.
    const cartagenaBackend = mod.mapearServicio({
      id: 1,
      nombre: 'Paquete Vacacional Cartagena',
      destino: 'Cartagena',
      precio: 320000,
      tipoServicio: 'PAQUETE_TODO_INCLUIDO'
    });
    const tayronaBackend = mod.mapearServicio({
      id: 5,
      nombre: 'Ecoturismo Parque Tayrona',
      destino: 'Tayrona',
      precio: 195000,
      tipoServicio: 'PASADIA'
    });

    expect(cartagenaBackend.categorias).toEqual(['playa']);
    expect(tayronaBackend.categorias).toEqual(['naturaleza']);
  });
});

describe('destinos-datos.js - mapeo del servicio del backend', () => {
  test('CP-17: mapearServicio traduce el DTO a la tarjeta del catalogo', () => {
    const tarjeta = mod.mapearServicio({
      id: 1,
      nombre: 'Paquete Vacacional Cartagena',
      destino: 'Cartagena',
      descripcion: 'Ciudad amurallada',
      precio: 320000,
      duracion: '3 días / 2 noches',
      tipoServicio: 'PAQUETE_TODO_INCLUIDO'
    });

    expect(tarjeta).toEqual({
      id: 1,
      nombre: 'Cartagena',
      titulo: 'Paquete Vacacional Cartagena',
      pais: 'Colombia',
      descripcion: 'Ciudad amurallada',
      precio: 320000,
      duracion: '3 días / 2 noches',
      tipoServicio: 'PAQUETE_TODO_INCLUIDO',
      img: 'img/Cartagena.jpg',
      categorias: ['playa']
    });
  });

  test('CP-18: mapearServicio aplica valores por defecto si el backend manda datos incompletos', () => {
    const tarjeta = mod.mapearServicio({ id: 99, precio: 'no-es-numero' });

    expect(tarjeta.precio).toBe(0);
    expect(tarjeta.duracion).toBe('Plan personalizado');
    expect(tarjeta.tipoServicio).toBe('PAQUETE_TODO_INCLUIDO');
    expect(tarjeta.descripcion).toMatch(/^Disfruta de este/);
    expect(tarjeta.img).toBe('img/Cartagena.jpg');
  });
});

describe('destinos-datos.js - etiquetas y precios', () => {
  test('CP-19: etiquetaTipoServicio solo etiqueta los tipos conocidos e incluyeVuelos solo a los aereos', () => {
    expect(mod.etiquetaTipoServicio('TOUR_EXCURSION')).toBe('Tour');
    expect(mod.etiquetaTipoServicio('VUELO_HOTEL')).toBe('Vuelo + hotel');
    expect(mod.etiquetaTipoServicio('DESCONOCIDO')).toBe('');

    expect(mod.incluyeVuelos('PAQUETE_TODO_INCLUIDO')).toBe(true);
    expect(mod.incluyeVuelos('VUELO_HOTEL')).toBe(true);
    expect(mod.incluyeVuelos('TOUR_EXCURSION')).toBe(false);
    expect(mod.incluyeVuelos('PASADIA')).toBe(false);
    expect(mod.incluyeVuelos(undefined)).toBe(false);
  });

  test('CP-20: formatearPrecio usa el formato de pesos colombianos', () => {
    expect(mod.formatearPrecio(320000)).toBe('$ 320.000 COP');
    expect(mod.formatearPrecio('480000')).toBe('$ 480.000 COP');
    expect(mod.formatearPrecio(0)).toBe('$ 0 COP');
    expect(mod.formatearPrecio(null)).toBe('$ 0 COP');
  });

  test('CP-20b: etiquetaCategorias une como maximo dos categorias', () => {
    expect(mod.etiquetaCategorias(['playa', 'ciudad'])).toBe('Playa y Ciudad');
    expect(mod.etiquetaCategorias(['naturaleza', 'playa', 'isla'])).toBe('Naturaleza y Playa');
    expect(mod.etiquetaCategorias([])).toBe('');
    expect(mod.etiquetaCategorias('playa')).toBe('');
  });
});

describe('destinos.js - reserva y busqueda', () => {
  function inputPasajeros(valor) {
    document.body.innerHTML = `<input id="res-pasajeros" value="${valor}">`;
  }

  test('CP-21: leerPasajeros acota el valor al rango 1-10 y usa 1 si no es numero', () => {
    inputPasajeros('7');
    expect(mod.leerPasajeros()).toBe(7);

    inputPasajeros('0');
    expect(mod.leerPasajeros()).toBe(mod.MIN_PASAJEROS_RESERVA);

    inputPasajeros('-5');
    expect(mod.leerPasajeros()).toBe(1);

    inputPasajeros('99');
    expect(mod.leerPasajeros()).toBe(mod.MAX_PASAJEROS_RESERVA);

    inputPasajeros('abc');
    expect(mod.leerPasajeros()).toBe(1);

    document.body.innerHTML = '';
    expect(mod.leerPasajeros()).toBe(1);
  });

  test('CP-22: destinoCoincide acepta nombre, titulo y palabras sueltas (sin acentos)', () => {
    const cartagena = { nombre: 'Cartagena', titulo: 'Paquete Vacacional Cartagena' };
    const medellin = { nombre: 'Medellín', titulo: 'Tour Medellín Innovadora y Café' };

    expect(mod.destinoCoincide(cartagena, 'cartagena')).toBe(true);
    expect(mod.destinoCoincide(cartagena, 'cartagena de indias')).toBe(true);
    expect(mod.destinoCoincide(cartagena, 'vacacional')).toBe(true);
    expect(mod.destinoCoincide(medellin, 'medellin')).toBe(true);
    expect(mod.destinoCoincide(cartagena, 'medellin')).toBe(false);
    expect(mod.destinoCoincide(cartagena, '')).toBe(false);
    expect(mod.destinoCoincide(cartagena, null)).toBe(false);
  });

  test('CP-23: la copia local del catalogo mantiene los 7 destinos y precios enteros', () => {
    expect(mod.DESTINOS_DEFAULT).toHaveLength(7);
    mod.DESTINOS_DEFAULT.forEach(d => {
      expect(typeof d.precio).toBe('number');
      expect(d.precio).toBeGreaterThan(0);
      expect(d.img).toMatch(/^img\/.+\.jpg$/);
      expect(Array.isArray(d.categorias)).toBe(true);
    });
  });
});
