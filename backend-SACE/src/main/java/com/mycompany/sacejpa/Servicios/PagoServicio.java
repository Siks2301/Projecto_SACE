package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.PagoDTOs;
import com.mycompany.sacejpa.Exceptions.AutorizacionException;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Pago;
import com.mycompany.sacejpa.Modelo.Solicitud;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Pago;
import com.mycompany.sacejpa.Repositorio.Reposi_Solicitud;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServicio {

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

    public static final String LLAVE_DESTINO_OFICIAL = "Bre-B @VXM301";

    @Transactional
    public PagoDTOs.PagoRespuestaDTO procesarPago(PagoDTOs.CrearPagoDTO dto, TokenServicio.Sesion sesion) {
        if (dto == null) {
            throw new ValidacionException("Los datos del pago no pueden ser nulos.");
        }
        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El monto del pago debe ser mayor a cero (0).");
        }
        if (dto.getSolicitudId() == null) {
            throw new ValidacionException("El ID de la solicitud es obligatorio.");
        }

        Solicitud solicitud = reposiSolicitud.findById(dto.getSolicitudId())
                .orElseThrow(() -> new ValidacionException("La solicitud #" + dto.getSolicitudId() + " no existe."));

        // Validacion de propiedad: un cliente solo puede pagar sus propias
        // solicitudes (administradores y asesores pueden operar sobre cualquier
        // una). Sin esto, cualquier cliente podia pagar y resolver la solicitud
        // de otro usuario.
        if (sesion != null && "CLIENTE".equalsIgnoreCase(sesion.tipoUsuario())
                && (solicitud.getCliente() == null
                        || !sesion.id().equals(solicitud.getCliente().getId()))) {
            throw new AutorizacionException("Solo puedes pagar tus propias solicitudes.");
        }

        // Anti doble cobro: si la solicitud ya tiene un pago CONFIRMADO no se
        // admiten mas pagos (el precio fijo se cancela una sola vez).
        boolean yaPagada = reposiPago.findBySolicitudId(solicitud.getId()).stream()
                .anyMatch(p -> p.getEstado() != null && "CONFIRMADO".equalsIgnoreCase(p.getEstado()));
        if (yaPagada) {
            throw new ValidacionException(
                    "Esta solicitud ya tiene un pago confirmado. Cualquier ajuste adicional lo gestiona un asesor.");
        }

        Cliente cliente = solicitud.getCliente();
        if (cliente == null && sesion != null) {
            cliente = reposiCliente.findById(sesion.id()).orElse(null);
        }
        if (cliente == null) {
            throw new ValidacionException("No se pudo asociar un cliente valido a este pago.");
        }

        // Sanitizar llave de destino obligatoria
        String llave = (dto.getLlaveDestino() != null && !dto.getLlaveDestino().trim().isEmpty())
                ? dto.getLlaveDestino().trim()
                : LLAVE_DESTINO_OFICIAL;
        if (!LLAVE_DESTINO_OFICIAL.equalsIgnoreCase(llave)) {
            llave = LLAVE_DESTINO_OFICIAL;
        }

        String metodo = (dto.getMetodoPago() != null && !dto.getMetodoPago().trim().isEmpty())
                ? dto.getMetodoPago().trim().toUpperCase()
                : "TRANSFERENCIA";

        // Crear pago
        Pago pago = new Pago();
        pago.setSolicitud(solicitud);
        pago.setCliente(cliente);
        pago.setMonto(dto.getMonto());
        pago.setFechaPago(new Date());
        pago.setMetodoPago(metodo);
        pago.setLlaveDestino(llave);
        pago.setEstado("CONFIRMADO");

        pago = reposiPago.save(pago);

        // 1. Generar comprobante PDF fisico
        String rutaPdf = null;
        try {
            rutaPdf = pdfComprobanteServicio.generarComprobantePdf(pago);
            pago.setUrlPdf(rutaPdf);
        } catch (Exception e) {
            System.err.println("Error al generar PDF de comprobante: " + e.getMessage());
        }

        // 2. Enviar correo al propietario con Fail-Safe
        boolean correoEnviado = false;
        try {
            correoEnviado = emailNotificacionServicio.notificarPagoAPropietario(pago, rutaPdf);
        } catch (Exception e) {
            System.err.println("Error en motor de correo: " + e.getMessage());
        }
        pago.setCorreoNotificado(correoEnviado);

        // 3. Actualizar estado de la solicitud a RESUELTA / CONFIRMADA
        solicitud.setEstado(Solicitud.Estado.RESUELTA);
        reposiSolicitud.save(solicitud);

        pago = reposiPago.save(pago);

        return convertirADto(pago);
    }

    public File obtenerArchivoPdfComprobante(Long idPago, TokenServicio.Sesion sesion) {
        if (idPago == null) {
            throw new ValidacionException("El ID de pago es invalido.");
        }
        Pago pago = reposiPago.findById(idPago)
                .orElseThrow(() -> new ValidacionException("El comprobante de pago #" + idPago + " no existe."));

        // Validacion de seguridad: Debe ser Admin, Empleado o el Cliente dueño del pago
        if (sesion != null) {
            boolean esAdmin = "ADMINISTRADOR".equalsIgnoreCase(sesion.tipoUsuario());
            boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(sesion.tipoUsuario());
            boolean esDueno = pago.getCliente() != null && pago.getCliente().getId().equals(sesion.id());

            if (!esAdmin && !esEmpleado && !esDueno) {
                throw new AutorizacionException("Acceso denegado: No tienes permisos para acceder a este comprobante.");
            }
        }

        String ruta = pago.getUrlPdf();
        File archivo = (ruta != null && !ruta.trim().isEmpty()) ? new File(ruta) : null;

        if (archivo == null || !archivo.exists()) {
            try {
                String nuevaRuta = pdfComprobanteServicio.generarComprobantePdf(pago);
                pago.setUrlPdf(nuevaRuta);
                reposiPago.save(pago);
                archivo = new File(nuevaRuta);
            } catch (Exception e) {
                throw new ValidacionException("No se pudo generar el archivo PDF del comprobante: " + e.getMessage());
            }
        }

        return archivo;
    }

    public List<PagoDTOs.PagoRespuestaDTO> obtenerPagosPorSolicitud(Long solicitudId, TokenServicio.Sesion sesion) {
        // Validacion de propiedad: un cliente solo puede consultar los pagos de
        // sus propias solicitudes; administradores y asesores pueden consultar
        // cualquiera. Antes la sesion se recibia y se ignoraba (fuga de datos).
        if (sesion != null && "CLIENTE".equalsIgnoreCase(sesion.tipoUsuario())) {
            Solicitud solicitud = reposiSolicitud.findById(solicitudId)
                    .orElseThrow(() -> new ValidacionException("La solicitud #" + solicitudId + " no existe."));
            boolean esDueno = solicitud.getCliente() != null
                    && sesion.id().equals(solicitud.getCliente().getId());
            if (!esDueno) {
                throw new AutorizacionException("No tienes permiso para consultar los pagos de esta solicitud.");
            }
        }
        List<Pago> lista = reposiPago.findBySolicitudId(solicitudId);
        return lista.stream().map(this::convertirADto).collect(Collectors.toList());
    }

    private PagoDTOs.PagoRespuestaDTO convertirADto(Pago p) {
        String clienteNombre = p.getCliente() != null ? (p.getCliente().getNombre() + " " + (p.getCliente().getApellido() != null ? p.getCliente().getApellido() : "")).trim() : "N/A";
        String clienteEmail = p.getCliente() != null ? p.getCliente().getEmail() : "N/A";
        String solicitudTitulo = p.getSolicitud() != null ? p.getSolicitud().getTitulo() : "Reserva";
        Long solicitudId = p.getSolicitud() != null ? p.getSolicitud().getId() : null;
        Long clienteId = p.getCliente() != null ? p.getCliente().getId() : null;

        return new PagoDTOs.PagoRespuestaDTO(
                p.getId(),
                solicitudId,
                solicitudTitulo,
                clienteId,
                clienteNombre,
                clienteEmail,
                p.getMonto(),
                p.getFechaPago(),
                p.getMetodoPago(),
                p.getLlaveDestino(),
                p.getEstado(),
                p.getUrlPdf(),
                p.getCorreoNotificado()
        );
    }
}
