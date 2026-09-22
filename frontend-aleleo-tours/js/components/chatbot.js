/* ==========================================================================
   AleLeo Tours - chatbot.js
   AleLeoBot IA
   ========================================================================== */

'use strict';

(function () {

    /* ============================================================
       1. CONFIGURACIÓN
       ============================================================ */

    const CONFIG = {
        apiBase: typeof API_BASE !== 'undefined'
            ? API_BASE
            : 'http://localhost:8082/api',

        umbralCoincidencia: 3,
        intentosFalloParaEscalar: 2,
        intervaloPollingMs: 4000,
        maxIntervaloPollingMs: 15000,
        maxMensajeChars: 1000,
        nombreBot: 'AleLeoBot'
    };


    /* ============================================================
       2. UTILIDADES
       ============================================================ */

    function normalizar(texto) {

        return String(texto || '')
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/[¿?¡!.,;:()"']/g, ' ')
            .replace(/\s+/g, ' ')
            .trim();
    }


    function tokenizar(texto) {

        return normalizar(texto)
            .split(' ')
            .filter(function (token) {
                return token.length > 1;
            });
    }


    function escaparHtml(texto) {

        var div = document.createElement('div');

        div.textContent = texto == null
            ? ''
            : String(texto);

        return div.innerHTML;
    }


    function formatearCOP(valor) {

        try {

            return new Intl.NumberFormat('es-CO', {
                style: 'currency',
                currency: 'COP',
                maximumFractionDigits: 0
            }).format(valor);

        } catch (error) {

            return '$' + valor;
        }
    }


    function valorSeguro(valor) {

        return valor == null ? '' : String(valor);
    }


    async function fetchConTimeout(url, opciones, tiempo) {

        tiempo = tiempo || 10000;

        var controller = new AbortController();

        var timer = setTimeout(function () {
            controller.abort();
        }, tiempo);

        try {

            opciones = opciones || {};
            opciones.signal = controller.signal;

            return await fetch(url, opciones);

        } finally {

            clearTimeout(timer);
        }
    }


    async function leerJsonSeguro(response) {

        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }


    function extraerArray(datos) {

        if (Array.isArray(datos)) {
            return datos;
        }

        if (datos && Array.isArray(datos.content)) {
            return datos.content;
        }

        if (datos && Array.isArray(datos.data)) {
            return datos.data;
        }

        if (datos && Array.isArray(datos.result)) {
            return datos.result;
        }

        return [];
    }


    /* ============================================================
       3. LEVENSHTEIN
       ============================================================ */

    function distanciaLevenshtein(a, b) {

        if (a === b) {
            return 0;
        }

        var la = a.length;
        var lb = b.length;

        if (la === 0) {
            return lb;
        }

        if (lb === 0) {
            return la;
        }

        if (Math.abs(la - lb) > 3) {
            return 99;
        }

        var anterior = [];

        var actual = [];

        var i;
        var j;

        for (j = 0; j <= lb; j++) {
            anterior[j] = j;
        }

        for (i = 1; i <= la; i++) {

            actual[0] = i;

            for (j = 1; j <= lb; j++) {

                var costo = a[i - 1] === b[j - 1]
                    ? 0
                    : 1;

                actual[j] = Math.min(
                    anterior[j] + 1,
                    actual[j - 1] + 1,
                    anterior[j - 1] + costo
                );
            }

            var temporal = anterior;
            anterior = actual;
            actual = temporal;
        }

        return anterior[lb];
    }


    function palabraCoincide(usuario, clave) {

        if (usuario === clave) {
            return true;
        }

        if (
            clave.length >= 5 &&
            usuario.startsWith(clave.substring(0, 5))
        ) {
            return true;
        }

        var tolerancia = 0;

        if (clave.length >= 5 && clave.length <= 7) {
            tolerancia = 1;
        }

        if (clave.length > 7) {
            tolerancia = 2;
        }

        return distanciaLevenshtein(usuario, clave) <= tolerancia;
    }


    /* ============================================================
       4. SINÓNIMOS
       ============================================================ */

    var SINONIMOS = {

        reservar: [
            'reserva',
            'reservacion',
            'comprar',
            'agendar',
            'apartar',
            'adquirir',
            'book'
        ],

        cancelar: [
            'cancelacion',
            'anular',
            'reembolso',
            'devolucion',
            'reprogramar',
            'aplazar'
        ],

        documentos: [
            'cedula',
            'pasaporte',
            'identificacion',
            'papeles',
            'registro civil'
        ],

        equipaje: [
            'maleta',
            'valija',
            'morral',
            'bodega',
            'cabina',
            'kilos',
            'peso'
        ],

        precio: [
            'costo',
            'vale',
            'cuesta',
            'tarifa',
            'valor',
            'cuanto'
        ],

        pago: [
            'pagar',
            'tarjeta',
            'credito',
            'debito',
            'pse',
            'cuotas',
            'financiar'
        ],

        paquete: [
            'todo incluido',
            'combo',
            'plan',
            'promocion'
        ],

        asesor: [
            'humano',
            'persona',
            'agente',
            'representante',
            'operador',
            'ejecutivo'
        ],

        horario: [
            'hora',
            'atencion',
            'abierto'
        ]
    };


    function expandirConSinonimos(tokens) {

        var resultado = new Set(tokens);

        tokens.forEach(function (token) {

            Object.keys(SINONIMOS).forEach(function (clave) {

                var grupo = [clave].concat(SINONIMOS[clave]);

                grupo.forEach(function (variante) {

                    var primeraPalabra =
                        normalizar(variante).split(' ')[0];

                    if (
                        palabraCoincide(
                            token,
                            primeraPalabra
                        )
                    ) {

                        grupo.forEach(function (item) {

                            resultado.add(
                                normalizar(item).split(' ')[0]
                            );

                        });
                    }
                });
            });
        });

        return Array.from(resultado);
    }


    /* ============================================================
       5. BASE DE CONOCIMIENTO
       ============================================================ */

    var FAQS_DEFAULT = [

        {
            pregunta: '¿Qué incluye un paquete Todo Incluido?',
            respuesta:
                'Nuestros paquetes Todo Incluido cubren tiquetes aéreos ida y vuelta, alojamiento en hoteles, comidas, bebidas y traslados aeropuerto-hotel.',
            palabrasClave:
                'todo incluido paquete comida alimentacion bebidas hotel que incluye',
            categoria: 'PAQUETES'
        },

        {
            pregunta: '¿Cómo puedo reservar un viaje?',
            respuesta:
                'Ve a la sección Destinos, elige el destino que deseas visitar, selecciona la fecha y la cantidad de pasajeros y confirma la reserva.',
            palabrasClave:
                'reservar reserva comprar paso a paso viajar agendar',
            categoria: 'RESERVAS'
        },

        {
            pregunta: '¿Qué documentos necesito para viajar?',
            respuesta:
                'Para vuelos nacionales en Colombia necesitas tu documento de identidad original. Para menores de edad pueden aplicar documentos adicionales.',
            palabrasClave:
                'documentos cedula pasaporte identificacion menores viajar',
            categoria: 'DOCUMENTOS'
        },

        {
            pregunta: '¿Cuáles son las políticas de cancelación?',
            respuesta:
                'Puedes solicitar cambios o cancelaciones de acuerdo con las condiciones de tu reserva. Para casos específicos puedes comunicarte con un asesor.',
            palabrasClave:
                'cancelar cancelacion cambio fecha reembolso politicas reprogramar',
            categoria: 'POLITICAS'
        },

        {
            pregunta: '¿Qué equipaje puedo llevar?',
            respuesta:
                'El equipaje permitido depende del paquete y las condiciones del viaje. Puedes consultar las condiciones específicas de tu reserva.',
            palabrasClave:
                'equipaje maleta peso kilos bodega cabina',
            categoria: 'EQUIPAJE'
        },

        {
            pregunta: '¿Qué métodos de pago aceptan?',
            respuesta:
                'Aceptamos diferentes métodos de pago, incluyendo tarjetas y otros medios disponibles durante el proceso de reserva.',
            palabrasClave:
                'pago pagar tarjeta credito debito pse cuotas financiar',
            categoria: 'PAGOS'
        },

        {
            pregunta: '¿Cuál es el horario de atención?',
            respuesta:
                'El asistente virtual está disponible las 24 horas. Para atención personalizada puedes solicitar un asesor humano.',
            palabrasClave:
                'horario hora atencion abierto disponibilidad',
            categoria: 'GENERAL'
        }
    ];


    var DESTINOS_RESPALDO = [

        {
            nombre: 'Plan de Prueba SACE',
            precio: 1000,
            duracion: '1 día / 1 noche'
        },

        {
            nombre: 'Cartagena',
            precio: 320000,
            duracion: '3 días / 2 noches'
        },

        {
            nombre: 'San Andrés',
            precio: 480000,
            duracion: '4 días / 3 noches'
        },

        {
            nombre: 'Santa Marta',
            precio: 270000,
            duracion: '3 días / 2 noches'
        },

        {
            nombre: 'Medellín',
            precio: 210000,
            duracion: '3 días / 2 noches'
        },

        {
            nombre: 'Tayrona',
            precio: 195000,
            duracion: '2 días / 1 noche'
        },

        {
            nombre: 'Providencia',
            precio: 560000,
            duracion: '5 días / 4 noches'
        }
    ];


    var KB = {

        faqs: FAQS_DEFAULT.slice(),

        destinos: DESTINOS_RESPALDO.slice()
    };


    /* ============================================================
       6. CARGAR DATOS DEL BACKEND
       ============================================================ */

    async function cargarFAQs() {

        try {

            var response = await fetchConTimeout(
                CONFIG.apiBase + '/preguntas-frecuentes'
            );

            if (!response.ok) {
                return;
            }

            var datos = await leerJsonSeguro(response);

            var lista = extraerArray(datos);

            if (lista.length > 0) {
                KB.faqs = lista;
            }

        } catch (error) {

            console.warn(
                'AleLeoBot: usando FAQs locales.'
            );
        }
    }


    async function cargarDestinos() {

        try {

            // Fuente real: el catalogo del backend (/api/servicios). Se consulta
            // SIEMPRE primero; los arrays locales solo sirven de respaldo para
            // no citar precios que difieren de la base de datos.
            var response = await fetchConTimeout(
                CONFIG.apiBase + '/servicios'
            );

            if (response.ok) {

                var datos = await leerJsonSeguro(response);

                var lista = extraerArray(datos);

                if (lista.length > 0) {
                    KB.destinos = lista;
                    return;
                }
            }

        } catch (error) {

            console.warn(
                'AleLeoBot: usando destinos locales.'
            );
        }

        // Respaldo local solo si el backend no respondio: primero el catalogo
        // vivo de la pagina de destinos (si esta cargada), luego la lista
        // estatica. KB.destinos conserva DESTINOS_RESPALDO como ultima opcion.
        if (
            typeof DESTINOS !== 'undefined' &&
            Array.isArray(DESTINOS) &&
            DESTINOS.length > 0
        ) {

            KB.destinos = DESTINOS;

        } else if (
            typeof DESTINOS_DEFAULT !== 'undefined' &&
            Array.isArray(DESTINOS_DEFAULT) &&
            DESTINOS_DEFAULT.length > 0
        ) {

            KB.destinos = DESTINOS_DEFAULT;
        }
    }


    /* ============================================================
       7. ESTADO
       ============================================================ */

    var estado = {

        abierto: false,

        escribiendo: false,

        fallosSeguidos: 0,

        ultimaCategoria: null,

        esperandoConfirmacionEscalar: false,

        solicitudActivaId: null,

        ultimoMensajeId: 0,

        intervaloPolling: null,

        intervaloPollingActual:
            CONFIG.intervaloPollingMs
    };


    /* ============================================================
       8. SESIÓN
       ============================================================ */

    function obtenerSesion() {

        try {

            if (
                typeof Sesion !== 'undefined' &&
                Sesion &&
                typeof Sesion.obtener === 'function'
            ) {

                return Sesion.obtener();
            }

        } catch (error) {

            console.warn(
                'No se pudo obtener Sesion.'
            );
        }


        try {

            var sesion =
                sessionStorage.getItem(
                    'onvacation_sesion'
                );

            return sesion
                ? JSON.parse(sesion)
                : null;

        } catch (error) {

            return null;
        }
    }


    /* ============================================================
       9. DETECCIÓN
       ============================================================ */

    function detectarSaludo(texto) {

        return /^(hola|hey|buenas|buenos dias|buenas tardes|buenas noches|que tal|hi|hello)\b/
            .test(texto);
    }


    function detectarDespedida(texto) {

        return /^(chao|adios|gracias|muchas gracias|bye|hasta luego|nos vemos)\b/
            .test(texto);
    }


    function detectarSolicitudAsesor(tokens) {

        var claves = [
            'asesor',
            'humano',
            'persona',
            'hablar',
            'agente',
            'representante',
            'operador',
            'ejecutivo'
        ];

        return tokens.some(function (token) {

            return claves.some(function (clave) {

                return palabraCoincide(
                    token,
                    clave
                );
            });
        });
    }


    function detectarConfirmacion(texto) {

        return /^(si|s|claro|dale|ok|obvio|de una|vale|correcto|afirmativo)\b/
            .test(texto);
    }


    function detectarNegacion(texto) {

        return /^(no|nop|nel|negativo|para nada)\b/
            .test(texto);
    }


    /* ============================================================
       10. DESTINOS
       ============================================================ */

    function buscarDestinoMencionado(tokens) {

        var mejor = null;

        var mejorPuntaje = 0;

        KB.destinos.forEach(function (destino) {

            var nombre =
                destino.nombre ||
                destino.name ||
                destino.destino ||
                '';

            var nombreTokens =
                tokenizar(nombre);

            var puntaje = 0;

            nombreTokens.forEach(function (nombreToken) {

                if (
                    tokens.some(function (token) {

                        return palabraCoincide(
                            token,
                            nombreToken
                        );
                    })
                ) {

                    puntaje++;
                }
            });


            if (puntaje > mejorPuntaje) {

                mejorPuntaje = puntaje;

                mejor = destino;
            }
        });


        return mejor;
    }


    function esConsultaDePrecio(tokens) {

        var claves = [
            'precio',
            'costo',
            'vale',
            'cuesta',
            'tarifa',
            'valor',
            'cuanto'
        ];

        return tokens.some(function (token) {

            return claves.some(function (clave) {

                return palabraCoincide(
                    token,
                    clave
                );
            });
        });
    }


    /* ============================================================
       11. FAQ
       ============================================================ */

    function buscarMejorFAQ(tokens) {

        var mejor = null;

        var mejorPuntaje = 0;


        KB.faqs.forEach(function (faq) {

            var preguntaTokens =
                tokenizar(faq.pregunta || '');

            var palabrasTokens =
                tokenizar(faq.palabrasClave || '');

            var respuestaTokens =
                tokenizar(faq.respuesta || '');

            var puntaje = 0;


            tokens.forEach(function (token) {

                if (token.length <= 2) {
                    return;
                }


                if (
                    palabrasTokens.some(function (palabra) {

                        return palabraCoincide(
                            token,
                            palabra
                        );
                    })
                ) {

                    puntaje += 3;
                }


                if (
                    preguntaTokens.some(function (palabra) {

                        return palabraCoincide(
                            token,
                            palabra
                        );
                    })
                ) {

                    puntaje += 2;
                }


                if (
                    respuestaTokens.some(function (palabra) {

                        return palabraCoincide(
                            token,
                            palabra
                        );
                    })
                ) {

                    puntaje++;
                }
            });


            if (
                estado.ultimaCategoria &&
                faq.categoria === estado.ultimaCategoria
            ) {

                puntaje++;
            }


            if (puntaje > mejorPuntaje) {

                mejorPuntaje = puntaje;

                mejor = faq;
            }
        });


        return {
            faq: mejor,
            puntaje: mejorPuntaje
        };
    }


    /* ============================================================
       12. CREAR CHAT
       ============================================================ */

    function inyectarWidget() {

        if (
            document.getElementById(
                'chatbot-toggle-btn'
            )
        ) {

            return;
        }


        var boton =
            document.createElement('button');

        boton.id =
            'chatbot-toggle-btn';

        boton.className =
            'chatbot-toggle-btn';

        boton.type = 'button';

        boton.setAttribute(
            'aria-label',
            'Abrir asistente virtual'
        );

        boton.innerHTML =
            '<i class="bi bi-robot"></i>';

        document.body.appendChild(boton);


        var panel =
            document.createElement('div');

        panel.id =
            'chatbot-panel';

        panel.className =
            'chatbot-panel';

        panel.innerHTML = `

            <div class="chatbot-header">

                <div class="chatbot-header-info">

                    <div class="chatbot-avatar">
                        <i class="bi bi-robot"></i>
                    </div>

                    <div class="chatbot-header-text">

                        <h3>
                            AleLeoBot
                            <span class="chatbot-ia-badge">
                                IA
                            </span>
                        </h3>

                        <p>
                            <span
                                class="status-dot"
                                id="chatbot-status-dot">
                            </span>

                            <span id="chatbot-status-text">
                                Asistente Virtual · En línea
                            </span>
                        </p>

                    </div>

                </div>


                <div class="chatbot-header-actions">

                    <button
                        class="chatbot-reset-btn"
                        id="chatbot-reset-btn"
                        type="button">

                        <i class="bi bi-arrow-clockwise"></i>

                    </button>


                    <button
                        class="chatbot-close-btn"
                        id="chatbot-close-btn"
                        type="button">

                        <i class="bi bi-x-lg"></i>

                    </button>

                </div>

            </div>


            <div
                class="chatbot-messages"
                id="chatbot-messages">
            </div>


            <div class="chatbot-footer">

                <input
                    type="text"
                    class="chatbot-input"
                    id="chatbot-input"
                    placeholder="Escribe tu consulta aquí..."
                    autocomplete="off">


                <button
                    class="chatbot-send-btn"
                    id="chatbot-send-btn"
                    type="button">

                    <i class="bi bi-send-fill"></i>

                </button>

            </div>
        `;


        document.body.appendChild(panel);


        boton.addEventListener(
            'click',
            toggleChat
        );


        document
            .getElementById('chatbot-close-btn')
            .addEventListener(
                'click',
                toggleChat
            );


        document
            .getElementById('chatbot-reset-btn')
            .addEventListener(
                'click',
                reiniciarConversacion
            );


        var input =
            document.getElementById(
                'chatbot-input'
            );


        var sendButton =
            document.getElementById(
                'chatbot-send-btn'
            );


        sendButton.addEventListener(
            'click',
            enviarMensajeUsuario
        );


        input.addEventListener(
            'keydown',
            function (event) {

                if (event.key === 'Enter') {

                    enviarMensajeUsuario();
                }
            }
        );


        document
            .getElementById('chatbot-messages')
            .addEventListener(
                'click',
                onClickMensajes
            );
    }


    /* ============================================================
       13. ABRIR / CERRAR
       ============================================================ */

    function toggleChat() {

        var panel =
            document.getElementById(
                'chatbot-panel'
            );

        if (!panel) {
            return;
        }


        estado.abierto =
            !estado.abierto;


        panel.classList.toggle(
            'abierto',
            estado.abierto
        );


        if (estado.abierto) {

            var input =
                document.getElementById(
                    'chatbot-input'
                );

            if (input) {
                input.focus();
            }


            var mensajes =
                document.getElementById(
                    'chatbot-messages'
                );


            if (
                mensajes &&
                mensajes.children.length === 0
            ) {

                mostrarBienvenida();
            }
        }
    }


    /* ============================================================
       14. MENSAJES
       ============================================================ */

    function agregarMensaje(
        tipo,
        contenido
    ) {

        var mensajes =
            document.getElementById(
                'chatbot-messages'
            );

        if (!mensajes) {
            return;
        }


        var fila =
            document.createElement('div');

        fila.className =
            'chatbot-msg-row chatbot-msg-' +
            tipo;


        var hora =
            new Date().toLocaleTimeString(
                'es-CO',
                {
                    hour: '2-digit',
                    minute: '2-digit'
                }
            );


        var icono = '';

        if (tipo === 'bot') {
            icono =
                '<i class="bi bi-robot"></i>';
        }

        if (tipo === 'user') {
            icono =
                '<i class="bi bi-person-fill"></i>';
        }

        if (tipo === 'asesor') {
            icono =
                '<i class="bi bi-headset"></i>';
        }


        fila.innerHTML = `

            <div class="chatbot-msg-avatar">
                ${icono}
            </div>

            <div class="chatbot-msg-content">

                <div class="chatbot-msg-bubble">
                    ${contenido}
                </div>

                <div class="msg-meta">
                    ${hora}
                </div>

            </div>
        `;


        mensajes.appendChild(fila);

        mensajes.scrollTop =
            mensajes.scrollHeight;
    }


    function agregarChips(botones) {

        var mensajes =
            document.getElementById(
                'chatbot-messages'
            );

        if (!mensajes) {
            return;
        }


        var contenedor =
            document.createElement('div');

        contenedor.className =
            'chatbot-chips mt-2';


        botones.forEach(function (boton) {

            var elemento =
                document.createElement('button');

            elemento.type = 'button';

            elemento.className =
                boton.transfer
                    ? 'chatbot-chip chatbot-chip-transfer'
                    : 'chatbot-chip';


            elemento.textContent =
                boton.label;


            if (boton.faq) {

                elemento.dataset.faq =
                    boton.faq;
            }


            if (boton.transfer) {

                elemento.dataset.transfer =
                    'true';
            }


            if (boton.href) {

                elemento.dataset.href =
                    boton.href;
            }


            contenedor.appendChild(
                elemento
            );
        });


        mensajes.appendChild(
            contenedor
        );

        mensajes.scrollTop =
            mensajes.scrollHeight;
    }


    /* ============================================================
       15. ESCRIBIENDO
       ============================================================ */

    function mostrarEscribiendo(
        mostrar
    ) {

        var mensajes =
            document.getElementById(
                'chatbot-messages'
            );

        if (!mensajes) {
            return;
        }


        var indicador =
            document.getElementById(
                'chatbot-typing-indicator'
            );


        if (mostrar) {

            if (!indicador) {

                indicador =
                    document.createElement(
                        'div'
                    );

                indicador.id =
                    'chatbot-typing-indicator';

                indicador.className =
                    'chatbot-typing';

                indicador.innerHTML =
                    '<span></span>' +
                    '<span></span>' +
                    '<span></span>';

                mensajes.appendChild(
                    indicador
                );
            }

        } else {

            if (indicador) {

                indicador.remove();
            }
        }
    }


    /* ============================================================
       16. BIENVENIDA
       ============================================================ */

    function mostrarBienvenida() {

        var sesion =
            obtenerSesion();


        var nombre =
            sesion &&
                sesion.nombre
                ? sesion.nombre.split(' ')[0]
                : 'viajero';


        agregarMensaje(
            'bot',

            '¡Hola, <strong>' +
            escaparHtml(nombre) +
            '</strong>!<br><br>' +

            'Soy <strong>AleLeoBot</strong>, ' +
            'el asistente virtual de AleLeo Tours.<br><br>' +

            'Puedo ayudarte con destinos, precios, ' +
            'reservas, documentos, equipaje y más.'
        );


        agregarChips([

            {
                label:
                    '¿Qué destinos tienen?',
                faq:
                    'destinos disponibles'
            },

            {
                label:
                    '¿Cómo reservar?',
                faq:
                    'como reservar'
            },

            {
                label:
                    'Documentos para viajar',
                faq:
                    'documentos'
            },

            {
                label:
                    'Hablar con un asesor',
                transfer:
                    true
            }

        ]);
    }


    /* ============================================================
       17. CLICK EN BOTONES
       ============================================================ */

    function onClickMensajes(event) {

        var chip =
            event.target.closest(
                '.chatbot-chip'
            );

        if (!chip) {
            return;
        }


        if (
            chip.dataset.transfer ===
            'true'
        ) {

            escalarAAsesorHumano(
                'Solicitud de atención personalizada desde el chat.'
            );

            return;
        }


        if (chip.dataset.href) {

            window.location.href =
                chip.dataset.href;

            return;
        }


        if (chip.dataset.faq) {

            procesarConsulta(
                chip.dataset.faq
            );
        }
    }


    /* ============================================================
       18. ENVIAR MENSAJE
       ============================================================ */

    function enviarMensajeUsuario() {

        var input =
            document.getElementById(
                'chatbot-input'
            );

        if (!input) {
            return;
        }


        if (estado.escribiendo) {
            return;
        }


        var texto =
            input.value.trim();


        if (!texto) {
            return;
        }


        texto =
            texto.substring(
                0,
                CONFIG.maxMensajeChars
            );


        input.value = '';


        agregarMensaje(
            'user',
            escaparHtml(texto)
        );


        if (estado.solicitudActivaId) {

            enviarMensajeASolicitud(
                texto
            );

            return;
        }


        estado.escribiendo = true;

        mostrarEscribiendo(true);


        var demora =
            Math.min(
                1000,
                300 + texto.length * 10
            );


        setTimeout(
            function () {

                try {

                    procesarConsulta(
                        texto
                    );

                } catch (error) {

                    console.error(
                        'Error procesando mensaje:',
                        error
                    );

                    agregarMensaje(
                        'bot',
                        'Lo siento, ocurrió un error. Intenta nuevamente.'
                    );

                } finally {

                    estado.escribiendo =
                        false;

                    mostrarEscribiendo(
                        false
                    );
                }

            },
            demora
        );
    }


    /* ============================================================
       18.5. MOTOR DE INTENCIONES MEJORADO
       Tolera errores ortográficos, sinónimos y distintas formas de preguntar.
       ============================================================ */

    var INTENCIONES = [
        { id: 'DESTINOS', claves: ['destino', 'destinos', 'lugar', 'lugares', 'ciudad', 'ciudades', 'viajar', 'vacaciones', 'opciones', 'planes'] },
        { id: 'PRECIO', claves: ['precio', 'precios', 'costo', 'costos', 'cuesta', 'vale', 'valor', 'tarifa', 'barato', 'economico', 'presupuesto', 'cuanto'] },
        { id: 'RESERVA', claves: ['reservar', 'reserva', 'reservacion', 'comprar', 'apartar', 'separar', 'agendar', 'adquirir', 'pasos'] },
        { id: 'DOCUMENTOS', claves: ['documento', 'documentos', 'cedula', 'pasaporte', 'identificacion', 'papeles', 'requisitos', 'menor', 'menores'] },
        { id: 'EQUIPAJE', claves: ['equipaje', 'maleta', 'maletas', 'morral', 'bodega', 'cabina', 'kilos', 'peso', 'llevar'] },
        { id: 'PAGOS', claves: ['pago', 'pagar', 'tarjeta', 'credito', 'debito', 'pse', 'cuotas', 'financiar', 'efectivo'] },
        { id: 'CANCELACION', claves: ['cancelar', 'cancelacion', 'anular', 'reembolso', 'devolucion', 'cambiar', 'cambio', 'reprogramar', 'aplazar'] },
        { id: 'TODO_INCLUIDO', claves: ['paquete', 'paquetes', 'todo', 'incluido', 'incluye', 'comida', 'bebidas', 'traslados', 'combo', 'plan'] },
        { id: 'HOTEL', claves: ['hotel', 'hoteles', 'alojamiento', 'hospedaje', 'habitacion', 'hospedar'] },
        { id: 'VUELOS', claves: ['vuelo', 'vuelos', 'avion', 'tiquete', 'tiquetes', 'aereo', 'aerea', 'aeropuerto'] },
        { id: 'DURACION', claves: ['duracion', 'dura', 'dias', 'dia', 'noches', 'noche', 'tiempo'] },
        { id: 'HORARIO', claves: ['horario', 'hora', 'atencion', 'abierto', 'atienden', 'disponible'] },
        { id: 'CONTACTO', claves: ['contacto', 'telefono', 'celular', 'correo', 'email', 'direccion'] },
        { id: 'PROMOCIONES', claves: ['promocion', 'promociones', 'oferta', 'ofertas', 'descuento', 'descuentos', 'rebaja'] },
        { id: 'NINOS', claves: ['nino', 'ninos', 'niño', 'niños', 'bebe', 'bebes', 'menor', 'menores', 'infante'] },
        { id: 'MASCOTAS', claves: ['mascota', 'mascotas', 'perro', 'gato', 'animal'] },
        { id: 'TRANSPORTE', claves: ['transporte', 'traslado', 'traslados', 'aeropuerto', 'recogen', 'recogida'] },
        { id: 'ALIMENTACION', claves: ['comida', 'comidas', 'alimentacion', 'desayuno', 'almuerzo', 'cena', 'bebida', 'bebidas'] },
        { id: 'SEGURIDAD', claves: ['seguro', 'seguridad', 'emergencia', 'accidente', 'medico', 'asistencia'] },
        { id: 'ASESOR', claves: ['asesor', 'humano', 'persona', 'agente', 'representante', 'operador', 'ejecutivo'] }
    ];

    var PALABRAS_VACIAS = new Set([
        'que', 'como', 'cual', 'cuales', 'cuando', 'donde', 'porque', 'por', 'para', 'con', 'sin', 'del', 'las', 'los', 'una', 'uno', 'unos', 'unas', 'esta', 'este', 'esto', 'quiero', 'necesito', 'puedo', 'podria', 'me', 'mi', 'se', 'es', 'son', 'hay', 'tienen', 'tiene', 'de', 'el', 'la', 'y', 'o', 'a', 'en', 'un', 'al'
    ]);

    function tokenizarUtil(texto) {
        return tokenizar(texto).filter(function (t) {
            return t.length > 2 && !PALABRAS_VACIAS.has(t);
        });
    }

    function similitudToken(a, b) {
        if (a === b) return 1;
        if (a.length >= 4 && b.length >= 4 && (a.startsWith(b) || b.startsWith(a))) return 0.9;
        var d = distanciaLevenshtein(a, b);
        var max = Math.max(a.length, b.length);
        if (!max) return 0;
        return Math.max(0, 1 - (d / max));
    }

    function puntuarIntencion(tokens, claves) {
        var total = 0;
        tokens.forEach(function (token) {
            var mejor = 0;
            claves.forEach(function (clave) {
                mejor = Math.max(mejor, similitudToken(token, normalizar(clave)));
            });
            if (mejor >= 0.72) total += mejor;
        });
        return total;
    }

    function detectarIntencionGeneral(texto) {
        var t = normalizar(texto);
        if (/^(hola|hey|buenas|buenos dias|buenas tardes|buenas noches|hi|hello)\b/.test(t)) return 'SALUDO';
        if (/^(gracias|muchas gracias|chao|adios|bye|hasta luego|nos vemos)\b/.test(t)) return 'DESPEDIDA';
        if (/\b(todos los precios|precios de todos|cuanto cuestan todos|lista de precios|mostrar precios)\b/.test(t)) return 'TODOS_PRECIOS';
        if (/\b(que puedes hacer|en que ayudas|ayudame|tengo dudas|tengo preguntas|que sabes|informacion general)\b/.test(t)) return 'INFORMACION';

        var tokens = expandirConSinonimos(tokenizarUtil(texto));
        var mejor = null, mejorPuntaje = 0;
        INTENCIONES.forEach(function (intencion) {
            var p = puntuarIntencion(tokens, intencion.claves);
            if (p > mejorPuntaje) { mejorPuntaje = p; mejor = intencion.id; }
        });
        return mejorPuntaje >= 0.72 ? mejor : null;
    }

    function listarDestinos(conPrecio) {
        var lista = Array.isArray(KB.destinos) && KB.destinos.length ? KB.destinos : DESTINOS_RESPALDO;
        var salida = conPrecio ? '<strong>Destinos y precios</strong><br><br>' : '<strong>Destinos disponibles</strong><br><br>';
        lista.forEach(function (d) {
            var nombre = d.nombre || d.name || d.destino || 'Destino';
            var precio = d.precio ?? d.precioDesde ?? d.valor ?? d.costo;
            var duracion = d.duracion ?? d.duration ?? '';
            salida += '<strong>' + escaparHtml(nombre) + '</strong>';
            if (conPrecio && precio != null && precio !== '') salida += ' — ' + formatearCOP(Number(precio));
            if (duracion) salida += ' · ' + escaparHtml(duracion);
            salida += '<br>';
        });
        return salida;
    }

    function responderIntencionGeneral(intencion) {
        switch (intencion) {
            case 'SALUDO': return '¡Hola! Soy <strong>AleLeoBot</strong>. Pregúntame por destinos, precios, reservas, hoteles, vuelos, documentos, equipaje, pagos, cancelaciones, promociones y más.';
            case 'DESPEDIDA': return '¡Con gusto! Cuando necesites ayuda con tu viaje, aquí estaré.';
            case 'INFORMACION': return 'Puedo ayudarte con <strong>destinos, precios, reservas, paquetes, hoteles, vuelos, documentos, equipaje, pagos, cancelaciones, promociones, niños, mascotas, traslados y alimentación</strong>. Escribe tu pregunta con tus propias palabras.';
            case 'TODOS_PRECIOS': return listarDestinos(true);
            case 'DESTINOS': return listarDestinos(false) + '<br>Escribe un destino y te mostraré precio y duración.';
            case 'PRECIO': return 'Dime el destino que te interesa y te indicaré el precio disponible. También puedes escribir <strong>“muéstrame todos los precios”</strong>.';
            case 'RESERVA': return 'Para reservar: elige un destino, selecciona fecha y número de pasajeros y confirma la compra. También puedes escribir: <strong>“Quiero reservar Cartagena para 2 personas”</strong>.';
            case 'DOCUMENTOS': return 'Para vuelos nacionales en Colombia normalmente necesitas tu documento de identidad original. Los menores pueden requerir documentación adicional. Para viajes internacionales, revisa pasaporte, visas y requisitos del destino.';
            case 'EQUIPAJE': return 'El equipaje permitido depende de la tarifa, aerolínea y paquete. Revisa peso, dimensiones, equipaje de cabina y bodega antes de viajar.';
            case 'PAGOS': return 'Las formas de pago dependen de las opciones habilitadas en la reserva. Normalmente pueden incluir tarjeta, PSE u otros medios disponibles en el sistema.';
            case 'CANCELACION': return 'Los cambios, reprogramaciones, cancelaciones y reembolsos dependen de las condiciones de la reserva y del proveedor. Para un caso concreto, solicita un asesor.';
            case 'TODO_INCLUIDO': return 'Un paquete Todo Incluido puede incluir vuelos ida y vuelta, alojamiento, comidas, bebidas y traslados. El contenido exacto depende del paquete seleccionado.';
            case 'HOTEL': return 'El hotel depende del destino y del paquete. Escribe el destino para mostrarte la información disponible.';
            case 'VUELOS': return 'Algunos paquetes incluyen tiquetes aéreos ida y vuelta. La disponibilidad, horarios y condiciones dependen del destino y de la reserva.';
            case 'DURACION': return 'La duración depende del destino. Escribe, por ejemplo, <strong>“¿Cuánto dura el viaje a San Andrés?”</strong>.';
            case 'HORARIO': return 'AleLeoBot está disponible las 24 horas. La atención humana puede depender del horario del equipo de servicio.';
            case 'CONTACTO': return 'Para datos de contacto específicos, revisa la sección de contacto de AleLeo Tours o solicita un asesor desde este chat.';
            case 'PROMOCIONES': return 'Las promociones pueden cambiar. Consulta los destinos y precios disponibles en la página o pregunta por un destino específico.';
            case 'NINOS': return 'Los requisitos y tarifas para niños dependen de la edad, el proveedor y el destino. Los menores también pueden necesitar documentación adicional.';
            case 'MASCOTAS': return 'Las políticas para mascotas dependen de la aerolínea, hotel y paquete. Para confirmar un caso específico, solicita un asesor.';
            case 'TRANSPORTE': return 'Algunos paquetes incluyen traslados entre aeropuerto y hotel. Debes revisar qué incluye exactamente el paquete seleccionado.';
            case 'ALIMENTACION': return 'Los planes Todo Incluido pueden cubrir comidas y bebidas. La cantidad de servicios y condiciones dependen del hotel y del paquete.';
            case 'SEGURIDAD': return 'Para seguros o asistencia en viaje, revisa las condiciones de tu paquete. Si se trata de una emergencia real durante un viaje, comunícate con los servicios de emergencia y con el proveedor de asistencia correspondiente.';
            case 'ASESOR': return null;
            default: return null;
        }
    }

    function generarRespuestaContextual(texto, tokens) {
        var destino = buscarDestinoMencionado(tokens);
        if (destino) {
            var nombre = destino.nombre || destino.name || destino.destino || 'Destino';
            var precio = destino.precio ?? destino.precioDesde ?? destino.valor ?? destino.costo;
            var duracion = destino.duracion ?? destino.duration ?? '';
            var intencion = detectarIntencionGeneral(texto);

            if (intencion === 'DURACION') {
                return '<strong>' + escaparHtml(nombre) + '</strong>: ' + (duracion ? escaparHtml(duracion) : 'duración por confirmar según el paquete.');
            }
            if (intencion === 'PRECIO' || esConsultaDePrecio(tokens)) {
                return '<strong>' + escaparHtml(nombre) + '</strong>: desde <strong>' + (precio != null ? formatearCOP(Number(precio)) : 'precio por consultar') + '</strong>' + (duracion ? '<br>' + escaparHtml(duracion) : '') + '.';
            }
            return '<strong>' + escaparHtml(nombre) + '</strong><br><br>Precio desde: <strong>' + (precio != null ? formatearCOP(Number(precio)) : 'Consultar') + '</strong>' + (duracion ? '<br>Duración: ' + escaparHtml(duracion) : '') + '<br><br>Puedes preguntarme por precio, duración, reserva, hotel, vuelo o equipaje para este destino.';
        }
        return null;
    }

    /* ============================================================
       19. PROCESAR CONSULTA
       ============================================================ */

    function procesarConsulta(texto) {
        var normalizado = normalizar(texto);
        var tokens = tokenizar(texto);

        if (!normalizado) return;

        if (detectarSolicitudAsesor(tokens)) {
            escalarAAsesorHumano(texto);
            return;
        }

        var contextual = generarRespuestaContextual(texto, tokens);
        if (contextual) {
            estado.fallosSeguidos = 0;
            agregarMensaje('bot', contextual);
            return;
        }

        var intencion = detectarIntencionGeneral(texto);
        if (intencion === 'ASESOR') {
            escalarAAsesorHumano(texto);
            return;
        }

        var respuestaIntencion = responderIntencionGeneral(intencion);
        if (respuestaIntencion) {
            estado.fallosSeguidos = 0;
            agregarMensaje('bot', respuestaIntencion);
            return;
        }

        var tokensExpandidos = expandirConSinonimos(tokenizarUtil(texto));
        var resultado = buscarMejorFAQ(tokensExpandidos);
        if (resultado.faq && resultado.puntaje >= 2) {
            estado.fallosSeguidos = 0;
            estado.ultimaCategoria = resultado.faq.categoria || null;
            agregarMensaje('bot', '<strong>' + escaparHtml(resultado.faq.pregunta || 'Información') + '</strong><br><br>' + escaparHtml(valorSeguro(resultado.faq.respuesta)));
            return;
        }

        estado.fallosSeguidos++;
        agregarMensaje('bot',
            'Entendí tu mensaje, pero no tengo un dato confiable para responderlo exactamente.<br><br>' +
            'Si la pregunta es sobre <strong>viajes</strong>, intenta incluir el destino o el tema (precio, hotel, vuelo, reserva, equipaje, pago, documentos, cancelación, etc.). ' +
            'Si necesitas información específica de una reserva, puedo conectarte con un asesor.'
        );
        agregarChips([
            { label: 'Ver destinos', faq: 'destinos disponibles' },
            { label: 'Ver precios', faq: 'mostrar todos los precios' },
            { label: 'Hablar con un asesor', transfer: true }
        ]);
    }

    /* ============================================================
       20. ESCALAR A ASESOR
       ============================================================ */

    async function escalarAAsesorHumano(
        motivo
    ) {

        var sesion =
            obtenerSesion();


        mostrarEscribiendo(true);


        var nombre =
            sesion &&
                sesion.nombre
                ? sesion.nombre
                : 'Cliente Visitante';


        var correo =
            sesion &&
                sesion.correo
                ? sesion.correo
                : 'No especificado';


        try {

            var response =
                await fetchConTimeout(

                    CONFIG.apiBase +
                    '/solicitudes',

                    {

                        method: 'POST',

                        headers: {
                            'Content-Type':
                                'application/json'
                        },

                        body:
                            JSON.stringify({

                                fechaCreacion:
                                    new Date().toISOString(),

                                titulo:
                                    'Consulta Chat: ' +
                                    String(motivo)
                                        .substring(0, 60),

                                asunto:
                                    'Asistencia en vivo - ' +
                                    nombre,

                                descripcion:
                                    'Consulta transferida desde AleLeoBot. ' +
                                    'Cliente: ' +
                                    nombre +
                                    ' (' +
                                    correo +
                                    '). ' +
                                    'Pregunta: ' +
                                    motivo,

                                estado:
                                    'PENDIENTE',

                                prioridad:
                                    'ALTA',

                                categoria:
                                    'CONSULTA',

                                clienteId:
                                    sesion
                                        ? sesion.id
                                        : null
                            })
                    }
                );


            if (!response.ok) {

                throw new Error(
                    'No se pudo crear la solicitud'
                );
            }


            var solicitud =
                await leerJsonSeguro(
                    response
                );


            if (
                !solicitud ||
                solicitud.id == null
            ) {

                throw new Error(
                    'El servidor no devolvió el ID'
                );
            }


            estado.solicitudActivaId =
                Number(
                    solicitud.id
                );


            sessionStorage.setItem(
                'onvacation_chat_solicitud_id',
                String(
                    estado.solicitudActivaId
                )
            );


            /* MENSAJE INICIAL */

            try {

                await fetchConTimeout(

                    CONFIG.apiBase +
                    '/mensajes',

                    {

                        method: 'POST',

                        headers: {
                            'Content-Type':
                                'application/json'
                        },

                        body:
                            JSON.stringify({

                                remitente:
                                    nombre +
                                    ' (Cliente)',

                                contenido:
                                    motivo,

                                tipo:
                                    'TEXTO',

                                fecha:
                                    new Date().toISOString(),

                                solicitudId:
                                    estado.solicitudActivaId
                            })
                    }
                );

            } catch (error) {

                console.warn(
                    'No se pudo guardar el mensaje inicial.'
                );
            }


            mostrarEscribiendo(false);


            var estadoTexto =
                document.getElementById(
                    'chatbot-status-text'
                );


            if (estadoTexto) {

                estadoTexto.innerHTML =
                    '<strong>Ticket #SOL-' +
                    estado.solicitudActivaId +
                    '</strong> · Asesor asignado';
            }


            agregarMensaje(
                'bot',

                '<strong>Ticket #SOL-' +
                escaparHtml(
                    String(
                        estado.solicitudActivaId
                    )
                ) +
                ' creado.</strong><br><br>' +

                'Te hemos conectado con nuestro equipo de atención. ' +
                'Un asesor te responderá aquí.'
            );


            iniciarPollingMensajes();


        } catch (error) {

            console.error(
                'Error creando solicitud:',
                error
            );


            mostrarEscribiendo(false);


            agregarMensaje(
                'bot',

                'No pudimos conectar con el asesor en este momento. ' +
                'Por favor intenta nuevamente.'
            );
        }
    }


    /* ============================================================
       21. MENSAJE AL ASESOR
       ============================================================ */

    async function enviarMensajeASolicitud(
        contenido
    ) {

        if (!estado.solicitudActivaId) {
            return;
        }


        var sesion =
            obtenerSesion();


        var remitente =
            sesion &&
                sesion.nombre
                ? sesion.nombre +
                ' (Cliente)'
                : 'Cliente';


        try {

            await fetchConTimeout(

                CONFIG.apiBase +
                '/mensajes',

                {

                    method: 'POST',

                    headers: {
                        'Content-Type':
                            'application/json'
                    },

                    body:
                        JSON.stringify({

                            remitente:
                                remitente,

                            contenido:
                                contenido,

                            tipo:
                                'TEXTO',

                            fecha:
                                new Date().toISOString(),

                            solicitudId:
                                estado.solicitudActivaId
                        })
                }
            );

        } catch (error) {

            console.error(
                'Error enviando mensaje:',
                error
            );
        }
    }


    /* ============================================================
       22. POLLING
       ============================================================ */

    function iniciarPollingMensajes() {

        detenerPollingMensajes();

        estado.intervaloPollingActual =
            CONFIG.intervaloPollingMs;

        programarSiguientePolling();
    }


    function programarSiguientePolling() {

        estado.intervaloPolling =
            setTimeout(

                async function () {

                    if (
                        !estado.solicitudActivaId
                    ) {

                        return;
                    }


                    try {

                        var response =
                            await fetchConTimeout(

                                CONFIG.apiBase +
                                '/mensajes/solicitud/' +
                                estado.solicitudActivaId,

                                {},
                                10000
                            );


                        if (response.ok) {

                            var datos =
                                await leerJsonSeguro(
                                    response
                                );


                            var mensajes =
                                extraerArray(
                                    datos
                                );


                            mensajes.forEach(
                                function (mensaje) {

                                    var id =
                                        Number(
                                            mensaje.id
                                        );


                                    if (
                                        Number.isFinite(id) &&
                                        id >
                                        estado.ultimoMensajeId
                                    ) {

                                        estado.ultimoMensajeId =
                                            id;


                                        var remitente =
                                            valorSeguro(
                                                mensaje.remitente
                                            ).toLowerCase();


                                        if (
                                            !remitente.includes(
                                                'cliente'
                                            )
                                        ) {

                                            agregarMensaje(

                                                'asesor',

                                                '<strong>' +
                                                escaparHtml(
                                                    mensaje.remitente ||
                                                    'Asesor'
                                                ) +
                                                ':</strong><br>' +

                                                escaparHtml(
                                                    mensaje.contenido
                                                )
                                            );
                                        }
                                    }
                                }
                            );
                        }

                    } catch (error) {

                        console.warn(
                            'Error consultando mensajes:',
                            error
                        );
                    }


                    if (
                        estado.solicitudActivaId
                    ) {

                        estado.intervaloPollingActual =
                            Math.min(

                                estado.intervaloPollingActual +
                                1000,

                                CONFIG.maxIntervaloPollingMs
                            );


                        programarSiguientePolling();
                    }

                },

                estado.intervaloPollingActual
            );
    }


    function detenerPollingMensajes() {

        if (
            estado.intervaloPolling
        ) {

            clearTimeout(
                estado.intervaloPolling
            );

            estado.intervaloPolling =
                null;
        }
    }


    /* ============================================================
       23. REINICIAR
       ============================================================ */

    function reiniciarConversacion() {

        var mensajes =
            document.getElementById(
                'chatbot-messages'
            );


        if (mensajes) {

            mensajes.innerHTML = '';
        }


        estado.fallosSeguidos = 0;

        estado.ultimaCategoria = null;

        estado.esperandoConfirmacionEscalar =
            false;


        mostrarBienvenida();
    }


    /* ============================================================
       24. INICIAR
       ============================================================ */

    async function iniciarAleLeoBot() {

        try {

            // Crear el botón y el panel primero. Así el chatbot aparece
            // incluso si las APIs de preguntas o destinos fallan.
            inyectarWidget();

            await Promise.allSettled([
                cargarFAQs(),
                cargarDestinos()
            ]);

            var ticket =
                sessionStorage.getItem(
                    'onvacation_chat_solicitud_id'
                );

            if (
                ticket &&
                Number(ticket) > 0
            ) {
                estado.solicitudActivaId = Number(ticket);
                iniciarPollingMensajes();
            }

        } catch (error) {

            console.error(
                'Error iniciando AleLeoBot:',
                error
            );
        }
    }


    // Funciona tanto si el script se carga antes como después de DOMContentLoaded.
    if (document.readyState === 'loading') {
        document.addEventListener(
            'DOMContentLoaded',
            iniciarAleLeoBot,
            { once: true }
        );
    } else {
        iniciarAleLeoBot();
    }


    /* ============================================================
       25. LIMPIAR POLLING
       ============================================================ */

    window.addEventListener(
        'pagehide',
        detenerPollingMensajes
    );


    document.addEventListener(
        'visibilitychange',
        function () {

            if (document.hidden) {

                detenerPollingMensajes();

            } else if (
                estado.solicitudActivaId &&
                !estado.intervaloPolling
            ) {

                iniciarPollingMensajes();
            }
        }
    );


})();