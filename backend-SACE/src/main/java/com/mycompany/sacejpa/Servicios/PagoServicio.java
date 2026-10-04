package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.PagoDTOs;
import com.mycompany.sacejpa.Exceptions.AutorizacionException;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.MetodoPago;
import com.mycompany.sacejpa.Modelo.Pago;
import com.mycompany.sacejpa.Modelo.PagoEstado;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Modelo.Solicitud;
import com.mycompany.sacejpa.Pagos.Pasarela.PasarelaPagos;
import com.mycompany.sacejpa.Pagos.Pasarela.PasarelaPagosException;
import com.mycompany.sacejpa.Pagos.Pasarela.PagosConfig;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Pago;
import com.mycompany.sacejpa.Repositorio.Reposi_Solicitud;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio del modulo de pagos.
 *
 * <h2>Reglas de negocio que este servicio hace cumplir</h2>
 * <ol>
 *   <li><b>El precio lo decide SACE.</b> Se lee del catalogo de servicios
 *       vinculado a la solicitud. Nunca se lee del cuerpo de la peticion. En la
 *       version anterior este servicio tomaba {@code dto.getMonto()} y lo
 *       guardaba sin comparar con el catalogo: un cliente podia pagar $1 por un
 *       tour de dos millones y el sistema lo daba por bueno.</li>
 *   <li><b>Un pago aprobado por solicitud.</b> Se consulta en la base y, además,
 *       la base tiene un indice unico parcial que lo garantiza incluso si dos
 *       peticiones llegan al mismo tiempo.</li>
 *   <li><b>Un pago nace pendiente y solo la pasarela lo mueve.</b> Este servicio
 *       no se auto-aprueba: refleja lo que la pasarela informa.</li>
 *   <li><b>Un pago ya resuelto no se toca.</b> La maquina de estados de
 *       {@link PagoEstado} prohibe volver atras.</li>
 * </ol>
 *
 * <h2>Por que el monto se redondea a centavos enteros</h2>
 * Las pasarelas cobran en centavos enteros porque los numeros de coma flotante
 * no representan bien el dinero: {@code 0.1 + 0.2} no es {@code 0.3}. Si SACE
 * guardara decimales y la pasarela recibiera enteros, siempre habria una
 * diferencia de un peso que el cliente veria en su recibo y el sistema en el
 * reporte. Se redondea una sola vez, al calcular el monto.
 */
@Service
public class PagoServicio {

    private static final Logger log = LoggerFactory.getLogger(PagoServicio.class);

    /**
     * Fuente de aleatoriedad criptografica para la referencia.
     *
     * <p>No se usa {@code UUID.randomUUID()} ni {@code Random}: la referencia
     * es una clave de idempotencia y conviene que sea impredecible para que
     * nadie pueda adivinar la de otro pago.
     */
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Autowired
    private Reposi_Pago reposiPago;

    @Autowired
    private Reposi_Solicitud reposiSolicitud;

    @Autowired
    private Reposi_Cliente reposiCliente;

    @Autowired
    private PdfComprobanteServicio pdfComprobanteServicio;

    @Autowired
    private EmailNotificacionServicio emailNotificacionServicio;

    /** Implementacion de pasarela activa: Wompi o la simulada. */
    @Autowired
    private PasarelaPagos pasarela;

    @Autowired
    private PagosConfig pagosConfig;

    // ==================================================================
    // Consulta previa: el backend comunica el precio, no lo recibe
    // ==================================================================

    /**
     * Devuelve el precio que SACE cobra por una solicitud.
     *
     * <p>El frontend llena el campo monto con esta respuesta y lo deja
     * bloqueado. El usuario ve el valor correcto y, al mismo tiempo, el backend
     * conserva la autoridad: aunque alguien modifique el campo desde la
     * consola del navegador, el servidor seguira usando el precio del catalogo.
     *
     * @param solicitudId solicitud a cotizar
     * @param sesion      sesion del solicitante
     * @return cotizacion con el monto y los metodos disponibles
     */
    @Transactional(readOnly = true)
    public PagoDTOs.CotizacionDTO cotizar(Long solicitudId, TokenServicio.Sesion sesion) {
        Solicitud solicitud = obtenerSolicitudAutorizada(solicitudId, sesion);
        BigDecimal precio = precioDe(solicitud);
        boolean yaPagado = existePagoAprobado(solicitud.getId());

        List<String> metodos = pagosConfig.metodosHabilitados().stream()
                .map(MetodoPago::name)
                .collect(Collectors.toList());

        return new PagoDTOs.CotizacionDTO(
                solicitud.getId(),
                solicitud.getTitulo(),
                precio,
                "COP",
                metodos,
                yaPagado);
    }

    /**
     * Configuracion de pagos que necesita el navegador antes de pintar el modal.
     *
     * @return pasarela activa, modo y metodos habilitados
     */
    public PagoDTOs.ConfiguracionPagosDTO configuracion() {
        List<PagoDTOs.MetodoPagoInfoDTO> metodos = pagosConfig.metodosHabilitados().stream()
                .map(m -> new PagoDTOs.MetodoPagoInfoDTO(m.name(), m.etiqueta()))
                .collect(Collectors.toList());

        // Las dos banderas las declara la propia pasarela, no se deducen aqui.
        return new PagoDTOs.ConfiguracionPagosDTO(
                pasarela.nombre(),
                pasarela.esSimulada(),
                pasarela.tieneCheckoutExterno(),
                metodos);
    }

    // ==================================================================
    // Creacion del pago
    // ==================================================================

    /**
     * Inicia un pago contra la pasarela.
     *
     * <p>El orden importa y es deliberado:
     * <ol>
     *   <li>validar Ownership y que no exista ya un cobro aprobado;</li>
     *   <li>calcular el precio desde el catalogo;</li>
     *   <li>persistir la orden en PENDIENTE <b>antes</b> de llamar a la
     *       pasarela, para que exista un registro aunque la llamada falle;</li>
     *   <li>llamar a la pasarela y guardar su identificador.</li>
     * </ol>
     *
     * <p>Se usa {@link Propagation#REQUIRES_NEW} porque la llamada externa puede
     * tardar varios segundos y no conviene tener abierta una transaccion de base
     * de datos mientras tanto: mantiene bloqueados registros que otros hilos
     * necesitan.
     *
     * @param dto    solicitud y metodo elegidos por el cliente
     * @param sesion sesion del solicitante
     * @param ipOrigen IP del cliente, para trazabilidad ante la pasarela
     * @return el pago recien creado, con su referencia y el estado de checkout
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            // Sin esto, el estado RECHAZADO que se registra cuando la pasarela
            // rechaza el cobro se perderia: Spring hace rollback ante cualquier
            // RuntimeException y la fila volveria a PENDIENTE. Guardar el
            // rechazo es justamente lo que deja rastro de lo que ocurrio.
            noRollbackFor = {ValidacionException.class, AutorizacionException.class})
    public PagoDTOs.PagoRespuestaDTO iniciarPago(
            PagoDTOs.CrearPagoDTO dto, TokenServicio.Sesion sesion, String ipOrigen) {

        if (dto == null || dto.getSolicitudId() == null) {
            throw new ValidacionException("Debes indicar la solicitud que vas a pagar.");
        }

        Solicitud solicitud = obtenerSolicitudAutorizada(dto.getSolicitudId(), sesion);

        // --- Regla 1: un solo cobro aprobado por solicitud ---
        if (existePagoAprobado(solicitud.getId())) {
            throw new ValidacionException(
                    "Esta solicitud ya tiene un pago aprobado. Si necesitas un ajuste, contacta a un asesor.");
        }

        // --- Regla 2: el metodo es lo unico que elige el cliente ---
        MetodoPago metodo = MetodoPago.desdeCodigo(dto.getMetodoPago());
        if (metodo == null) {
            throw new ValidacionException("El metodo de pago indicado no es valido.");
        }
        if (!pagosConfig.metodoHabilitado(metodo)) {
            throw new ValidacionException(
                    "El metodo de pago " + metodo.etiqueta() + " no esta disponible en este momento.");
        }

        // --- Regla 3: el monto sale del catalogo, nunca del cliente ---
        BigDecimal precio = precioDe(solicitud);
        Cliente cliente = clienteDe(solicitud, sesion);

        Pago pago = new Pago();
        pago.setSolicitud(solicitud);
        pago.setCliente(cliente);
        pago.setMonto(precio.setScale(2, RoundingMode.HALF_UP));
        pago.setMetodoPago(metodo);
        pago.setEstado(PagoEstado.PENDIENTE);
        pago.setPasarela(pasarela.nombre());
        pago.setReferencia(generarReferencia());
        pago.setFechaCreacion(new Date());
        pago.setCorreoNotificado(false);
        pago.setNotas(recortar(dto.getNotas(), 500));

        // Se guarda ANTES de llamar a la pasarela. Si la llamada falla, queda
        // una orden pendiente que se puede reconciliar; si se perdiera, el
        // cliente habria pagado sin que SACE tenga registro de nada.
        pago = reposiPago.saveAndFlush(pago);

        PasarelaPagos.OrdenPago orden = new PasarelaPagos.OrdenPago(
                pago.getReferencia(),
                solicitud.getId(),
                cliente.getEmail(),
                nombreCompleto(cliente),
                telefonoNormalizado(cliente),
                cliente.getNumeroDocumento(),
                cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().name() : null,
                pago.getMonto(),
                metodo,
                ipOrigen);

        try {
            PasarelaPagos.TransaccionPasarela transaccion = pasarela.crear(orden);

            pago.setIdTransaccionPasarela(transaccion.idTransaccion());
            pago.setCodigoAutorizacion(transaccion.codigoAutorizacion());
            if (transaccion.metodoTipo() != null) {
                log.debug("La pasarela uso el metodo {} para la referencia {}", transaccion.metodoTipo(), pago.getReferencia());
            }

            // La pasarela puede responder ya en estado terminal (por ejemplo, una
            // tarjeta rechazada al instante). Si es asi, se refleja de una vez en
            // vez de dejar el pago pendiente para siempre.
            if (transaccion.estado() != null && transaccion.estado().esFinal()) {
                aplicarEstado(pago, transaccion.estado());
                return finalizarSiAprobado(pago);
            }

            return convertirADto(pago, pasarela.urlCheckout(orden));

        } catch (PasarelaPagosException e) {
            // Fallo definitivo de la pasarela: no hay nada que cobrar y avisarle
            // al cliente que espere un cobro que nunca ocurrio seria peñoso.
            if (e.esRechazoDefinitivo()) {
                log.warn("La pasarela rechazo el pago de la solicitud {}: {}", solicitud.getId(), e.getMessage());
                marcarComo(pago, PagoEstado.RECHAZADO);
                throw new ValidacionException(e.getMessage());
            }

            // Fallo tecnico: la transaccion pudo crearse igual. El pago queda
            // PENDIENTE y la conciliacion lo resolvera consultando la pasarela.
            log.error("Fallo tecnico al cobrar la solicitud {}: {}", solicitud.getId(), e.getMessage());
            marcarComo(pago, PagoEstado.PENDIENTE);
            throw new ValidacionException(
                    "No pudimos comunicarnos con la pasarela de pagos. Tu reserva sigue pendiente y no se realizo ningun cobro.");
        }
    }

    // ==================================================================
    // Confirmacion: webhook y consulta de respaldo
    // ==================================================================

    /**
     * Procesa un evento de la pasarela.
     *
     * <p>Es idempotente a proposito: la pasarela reenvia el mismo evento varias
     * veces (es lo normal, no un error) y volver a aplicar un estado ya aplicado
     * no debe romper nada ni duplicar el envio del comprobante.
     *
     * @param transaccion transaccion reportada por la pasarela
     * @return el pago afectado, o null si la transaccion no es de SACE
     */
    @Transactional
    public Pago aplicarEventoDePasarela(PasarelaPagos.TransaccionPasarela transaccion) {
        if (transaccion == null || transaccion.idTransaccion() == null) {
            return null;
        }

        Optional<Pago> encontrado = transaccion.referencia() != null
                ? reposiPago.findByReferencia(transaccion.referencia())
                : reposiPago.findByIdTransaccionPasarela(transaccion.idTransaccion());

        if (encontrado.isEmpty() && transaccion.idTransaccion() != null) {
            encontrado = reposiPago.findByIdTransaccionPasarela(transaccion.idTransaccion());
        }
        if (encontrado.isEmpty()) {
            // Un evento de una transaccion que SACE no conoce no es un error
            // grave: podria ser de un intento que el usuario cancelo antes de
            // que SACE alcanzara a guardarlo.
            log.warn("Evento de pasarela para una transaccion desconocida: id={}", transaccion.idTransaccion());
            return null;
        }

        Pago pago = encontrado.get();

        if (pago.getEstado() != null && pago.getEstado().esFinal()) {
            log.debug("Evento repetido para un pago ya en estado {} (referencia {}). Se ignora.",
                    pago.getEstado(), pago.getReferencia());
            return pago;
        }

        aplicarEstado(pago, transaccion.estado());

        if (pago.estaAprobado()) {
            completarPagoAprobado(pago);
        }
        return pago;
    }

    /**
     * Consulta el estado de un pago y reconcilia con la pasarela.
     *
     * <p>Es el respaldo del webhook y tambien la ruta que usa el navegador
     * mientras espera. Consulta siempre a la pasarela y no solo al estado local:
     * si el evento se perdio, el estado local sigue siendo PENDIENTE mientras
     * la pasarela ya sabe que aprobo.
     *
     * @param referencia  referencia del pago
     * @param sesion      sesion del solicitante
     * @return estado actualizado del pago
     */
    @Transactional
    public PagoDTOs.PagoRespuestaDTO consultarEstado(String referencia, TokenServicio.Sesion sesion) {
        Pago pago = reposiPago.findByReferencia(referencia)
                .orElseThrow(() -> new ValidacionException("El pago buscado no existe."));

        verificarAccesoAlPago(pago, sesion);

        // Si ya se resolvio, no hay nada que preguntar a la pasarela.
        if (pago.getEstado() != null && pago.getEstado().esFinal()) {
            return convertirADto(pago, null);
        }

        if (pago.getIdTransaccionPasarela() == null) {
            // La orden se creo pero la llamada a la pasarela no devolvio
            // identificador: no hay nada que consultar.
            return convertirADto(pago, null);
        }

        try {
            PasarelaPagos.TransaccionPasarela remota = pasarela.consultar(pago.getIdTransaccionPasarela());
            if (remota.estado() != null && remota.estado() != pago.getEstado()) {
                aplicarEstado(pago, remota.estado());
                if (pago.estaAprobado()) {
                    completarPagoAprobado(pago);
                }
            } else if (pago.estaAprobado()) {
                completarPagoAprobado(pago);
            }
        } catch (PasarelaPagosException e) {
            // Fallar la consulta no significa que el pago haya fallado. Se
            // devuelve el estado local tal cual y el navegador sigue esperando:
            // inventar un estado aqui seria peor que no tener dato.
            log.warn("No se pudo actualizar el estado del pago {}: {}", referencia, e.getMessage());
        }

        return convertirADto(pago, null);
    }

    // ==================================================================
    // Comprobante y listado
    // ==================================================================

    /**
     * Devuelve el PDF del comprobante de un pago.
     *
     * <p>Solo existe comprobante de pagos aprobados: es el respaldo de que el
     * dinero entro. Generarlo para un pago rechazado daria un documento falso.
     *
     * @param idPago pago a descargar
     * @param sesion sesion del solicitante
     * @return archivo PDF
     */
    public File obtenerArchivoPdfComprobante(Long idPago, TokenServicio.Sesion sesion) {
        if (idPago == null) {
            throw new ValidacionException("El ID de pago es invalido.");
        }
        Pago pago = reposiPago.findById(idPago)
                .orElseThrow(() -> new ValidacionException("El comprobante de pago #" + idPago + " no existe."));

        verificarAccesoAlPago(pago, sesion);

        if (!pago.estaAprobado()) {
            throw new ValidacionException(
                    "Este pago aun no se ha aprobado, asi que todavia no hay comprobante que mostrar.");
        }

        String ruta = pago.getUrlPdf();
        File archivo = pdfComprobanteServicio.resolverRutaDeComprobante(ruta);

        if (archivo == null || !archivo.exists()) {
            // El archivo se perdio del disco pero el pago existe: se regenera
            // desde los datos guardados en la base.
            try {
                String nuevaRuta = pdfComprobanteServicio.generarComprobantePdf(pago);
                pago.setUrlPdf(nuevaRuta);
                reposiPago.save(pago);
                archivo = new File(nuevaRuta);
            } catch (Exception e) {
                log.error("No se pudo regenerar el comprobante del pago {}: {}", idPago, e.getMessage());
                throw new ValidacionException("No se pudo generar el comprobante en PDF: " + e.getMessage());
            }
        }

        return archivo;
    }

    /** Lista los pagos de una solicitud, validando que el solicitante sea el dueno. */
    @Transactional(readOnly = true)
    public List<PagoDTOs.PagoRespuestaDTO> obtenerPagosPorSolicitud(Long solicitudId, TokenServicio.Sesion sesion) {
        Solicitud solicitud = obtenerSolicitudAutorizada(solicitudId, sesion);
        return reposiPago.findBySolicitudId(solicitud.getId()).stream()
                .map(p -> convertirADto(p, null))
                .collect(Collectors.toList());
    }

    /**
     * Concilia pagos que quedaron pendientes.
     *
     * <p>Existe porque los eventos se pierden: si un webhook no llego, el pago
     * se quedaria PENDIENTE para siempre aunque la pasarela lo hubiera aprobado.
     * Esta tarea los reconsulta y los cierra. Pensada para un
     * {@code @Scheduled}, pero expuesta como metodo publico para poder invocarla
     * desde un endpoint de administracion durante la demo.
     *
     * @return numero de pagos que quedaron reconciliados
     */
    @Transactional
    public int reconciliarPendientes() {
        List<Pago> pendientes = reposiPago.findByEstadoOrderByFechaCreacionAsc(PagoEstado.PENDIENTE);
        int resueltos = 0;

        for (Pago pago : pendientes) {
            if (pago.getIdTransaccionPasarela() == null) {
                continue;
            }
            try {
                PasarelaPagos.TransaccionPasarela remota =
                        pasarela.consultar(pago.getIdTransaccionPasarela());
                if (remota.estado() != null && remota.estado() != pago.getEstado()) {
                    aplicarEstado(pago, remota.estado());
                    if (pago.estaAprobado()) {
                        completarPagoAprobado(pago);
                    }
                    resueltos++;
                }
            } catch (PasarelaPagosException e) {
                log.warn("No se pudo conciliar el pago {}: {}", pago.getReferencia(), e.getMessage());
            }
        }

        if (resueltos > 0) {
            log.info("Conciliacion: {} pagos pendientes quedaron resueltos.", resueltos);
        }
        return resueltos;
    }

    // ==================================================================
    // Utilidades internas
    // ==================================================================

    /**
     * Calcula el precio a cobrar de una solicitud.
     *
     * <p>Usa el precio del servicio generado y, si no lo hay, el precio del
     * servicio principal del cliente. Si tampoco existe, no hay forma honesta de
     * saber cuanto vale: se rechaza la operacion en vez de inventar un cero.
     *
     * @param solicitud solicitud a cotizar
     * @return precio en pesos
     */
    private BigDecimal precioDe(Solicitud solicitud) {
        Servicio servicio = solicitud.getServicioGenerado();

        if (servicio == null || servicio.getPrecio() == null || servicio.getPrecio().signum() <= 0) {
            throw new ValidacionException(
                    "Esta solicitud no tiene un servicio asignado con precio, por lo que no se puede cobrar. "
                            + "Contacta a un asesor para que la registre.");
        }

        return servicio.getPrecio().setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Carga la solicitud y verifica que el solicitante tenga permiso sobre ella.
     *
     * <p>Un cliente solo ve sus propias solicitudes; administradores y asesores
     * pueden operar sobre cualquiera.
     */
    private Solicitud obtenerSolicitudAutorizada(Long solicitudId, TokenServicio.Sesion sesion) {
        if (solicitudId == null) {
            throw new ValidacionException("El ID de la solicitud es obligatorio.");
        }
        Solicitud solicitud = reposiSolicitud.findById(solicitudId)
                .orElseThrow(() -> new ValidacionException("La solicitud #" + solicitudId + " no existe."));

        if (sesion != null && "CLIENTE".equalsIgnoreCase(sesion.tipoUsuario())
                && (solicitud.getCliente() == null
                || !sesion.id().equals(solicitud.getCliente().getId()))) {
            throw new AutorizacionException("Solo puedes operar sobre tus propias solicitudes.");
        }
        return solicitud;
    }

    /** Verifica que quien consulta un pago sea su dueno, un asesor o un admin. */
    private void verificarAccesoAlPago(Pago pago, TokenServicio.Sesion sesion) {
        if (sesion == null) {
            // Sin sesion no hay dueno que validar. El interceptor deberia haber
            // bloqueado el acceso, pero esta comprobacion no depende de el: si
            // alguien la invocara desde otro punto, seguiria negandose.
            throw new AutorizacionException("Acceso denegado: debes iniciar sesion.");
        }
        boolean esAdmin = "ADMINISTRADOR".equalsIgnoreCase(sesion.tipoUsuario());
        boolean esAsesor = "EMPLEADO".equalsIgnoreCase(sesion.tipoUsuario());
        boolean esDueno = pago.getCliente() != null && pago.getCliente().getId().equals(sesion.id());

        if (!esAdmin && !esAsesor && !esDueno) {
            throw new AutorizacionException("No tienes permisos para consultar este pago.");
        }
    }

    /** Resuelve el cliente que figuro como pagador. */
    private Cliente clienteDe(Solicitud solicitud, TokenServicio.Sesion sesion) {
        Cliente cliente = solicitud.getCliente();
        if (cliente == null && sesion != null) {
            cliente = reposiCliente.findById(sesion.id()).orElse(null);
        }
        if (cliente == null) {
            throw new ValidacionException("No se pudo asociar un cliente valido a este pago.");
        }
        return cliente;
    }

    /** Indica si la solicitud ya tiene un cobro aprobado. */
    private boolean existePagoAprobado(Long solicitudId) {
        return reposiPago.findFirstBySolicitudIdAndEstado(solicitudId, PagoEstado.APROBADO).isPresent();
    }

    /**
     * Cambia el estado del pago respetando la maquina de estados.
     *
     * @throws ValidacionException si la transicion no es legal
     */
    private void aplicarEstado(Pago pago, PagoEstado destino) {
        if (destino == null) {
            return;
        }
        PagoEstado actual = pago.getEstado() == null ? PagoEstado.PENDIENTE : pago.getEstado();

        if (actual == destino) {
            return;
        }
        if (!actual.puedeTransicionarA(destino)) {
            log.warn("Transicion de estado no permitida: {} -> {} (pago {})",
                    actual, destino, pago.getReferencia());
            return;
        }

        pago.setEstado(destino);
        if (destino.esAprobado()) {
            pago.setFechaAprobacion(new Date());
        }
        log.info("Pago {} paso de {} a {}", pago.getReferencia(), actual, destino);
    }

    /** Atajo para marcar un estado sin pasar por la maquina de estados. */
    private void marcarComo(Pago pago, PagoEstado estado) {
        pago.setEstado(estado);
        reposiPago.save(pago);
    }

    /**
     * Cierra el pago si la pasarela lo aprobo ya en el momento de crearlo.
     *
     * @return respuesta completa con el comprobante si quedo aprobado, o el
     *         estado actual si la pasarela respondio con un rechazo
     */
    private PagoDTOs.PagoRespuestaDTO finalizarSiAprobado(Pago pago) {
        if (pago.estaAprobado()) {
            completarPagoAprobado(pago);
            return convertirADto(pago, null);
        }
        return convertirADto(pago, null);
    }

    /**
     * Genera el comprobante, envia el correo y cierra la solicitud.
     *
     * <p>Los tres pasos son tolerantes a fallos: si el PDF no se genera o el
     * correo no sale, el pago sigue siendo valido y simplemente se reintenta
     * cuando el cliente descargue el comprobante. Un cobro confirmado jamas
     * debe revertirse por un problema de correo.
     */
    private void completarPagoAprobado(Pago pago) {
        if (pago.estaAprobado() && pago.getFechaAprobacion() == null) {
            pago.setFechaAprobacion(new Date());
        }

        // 1. Comprobante PDF
        if (pago.getUrlPdf() == null || pago.getUrlPdf().isBlank()) {
            try {
                pago.setUrlPdf(pdfComprobanteServicio.generarComprobantePdf(pago));
            } catch (Exception e) {
                log.error("No se pudo generar el comprobante del pago {}: {}", pago.getReferencia(), e.getMessage());
            }
        }

        // 2. Correo al cliente (fail-safe)
        if (!Boolean.TRUE.equals(pago.getCorreoNotificado())) {
            try {
                boolean enviado = emailNotificacionServicio.notificarPagoAPropietario(pago, pago.getUrlPdf());
                pago.setCorreoNotificado(enviado);
            } catch (Exception e) {
                log.error("Fallo el envio del comprobante por correo (pago {}): {}", pago.getReferencia(), e.getMessage());
            }
        }

        // 3. La solicitud queda resuelta
        Solicitud solicitud = pago.getSolicitud();
        if (solicitud != null && solicitud.getEstado() != Solicitud.Estado.RESUELTA) {
            solicitud.setEstado(Solicitud.Estado.RESUELTA);
            reposiSolicitud.save(solicitud);
        }

        reposiPago.save(pago);
    }

    /**
     * Genera una referencia unica e impredecible.
     *
     * <p>Formato: {@code PAGO-XXXXX} con cinco caracteres sin caracteres
     * ambiguos (se excluyen 0/O y 1/I/L) para que sea dictable por telefono
     * cuando un asesor la lea en voz alta.
     *
     * @return referencia nueva
     */
    private String generarReferencia() {
        StringBuilder sb = new StringBuilder("PAGO-");
        for (int i = 0; i < 5; i++) {
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private String nombreCompleto(Cliente cliente) {
        String nombre = cliente.getNombre() == null ? "" : cliente.getNombre().trim();
        String apellido = cliente.getApellido() == null ? "" : cliente.getApellido().trim();
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? "Cliente AleLeo Tours" : completo;
    }

    /** Telefono solo con digitos, como lo espera la pasarela. */
    private String telefonoNormalizado(Cliente cliente) {
        String tel = cliente.getTelefono();
        if (tel == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : tel.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() <= max ? limpio : limpio.substring(0, max);
    }

    /** Proyecta la entidad a la respuesta que consume el navegador. */
    private PagoDTOs.PagoRespuestaDTO convertirADto(Pago p, String urlCheckout) {
        Cliente cliente = p.getCliente();
        Solicitud solicitud = p.getSolicitud();
        PagoEstado estado = p.getEstado() == null ? PagoEstado.PENDIENTE : p.getEstado();

        String nombreCliente = cliente == null ? "N/A"
                : (cliente.getNombre() + " " + (cliente.getApellido() != null ? cliente.getApellido() : "")).trim();

        return new PagoDTOs.PagoRespuestaDTO(
                p.getId(),
                p.getReferencia(),
                solicitud != null ? solicitud.getId() : null,
                solicitud != null ? solicitud.getTitulo() : "Reserva",
                cliente != null ? cliente.getId() : null,
                nombreCliente,
                cliente != null ? cliente.getEmail() : "N/A",
                p.getMonto(),
                p.getMoneda(),
                p.getMetodoPago(),
                p.getMetodoPago() != null ? p.getMetodoPago().etiqueta() : null,
                estado,
                estado.etiqueta(),
                p.getPasarela(),
                p.getCodigoAutorizacion(),
                p.getFechaCreacion(),
                p.getFechaAprobacion(),
                urlCheckout,
                p.getUrlPdf(),
                p.getCorreoNotificado(),
                estado.esAprobado());
    }
}