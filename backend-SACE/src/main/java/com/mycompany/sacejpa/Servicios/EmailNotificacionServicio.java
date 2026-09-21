package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.Modelo.Pago;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * Servicio de Notificaciones por Correo Electrónico con Control de Errores (Fail-Safe).
 * Si la red o el SMTP fallan, el pago NO se interrumpe y se registra correo_notificado = false.
 */
@Service
public class EmailNotificacionServicio {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacionServicio.class);
    private static final DecimalFormat MONTO_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat FECHA_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.propietario.email:propietario@aleleotours.com}")
    private String propietarioEmail;

    public boolean notificarPagoAPropietario(Pago pago, String rutaPdf) {
        if (pago == null) return false;

        Long idSolicitud = (pago.getSolicitud() != null) ? pago.getSolicitud().getId() : 0L;
        String llave = (pago.getLlaveDestino() != null) ? pago.getLlaveDestino() : "Bre-B @VXM301";
        String asunto = "[NUEVO PAGO CONFIRMADO] Comprobante - Llave " + llave + " - Solicitud #" + idSolicitud;

        try {
            if (mailSender == null) {
                log.warn("[FAIL-SAFE EMAIL] JavaMailSender no configurado. Se omite el envio de correo para el pago #{}", pago.getId());
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(propietarioEmail);
            helper.setSubject(asunto);

            String clienteNombre = pago.getCliente() != null ? (pago.getCliente().getNombre() + " " + (pago.getCliente().getApellido() != null ? pago.getCliente().getApellido() : "")) : "Cliente Registrado";
            String clienteCorreo = pago.getCliente() != null ? pago.getCliente().getEmail() : "N/A";
            String tourTitulo = pago.getSolicitud() != null ? pago.getSolicitud().getTitulo() : "Reserva de Tour";
            String montoFormatted = "$ " + MONTO_FORMAT.format(pago.getMonto() != null ? pago.getMonto() : 0) + " COP";
            String fechaFormatted = FECHA_FORMAT.format(pago.getFechaPago() != null ? pago.getFechaPago() : new java.util.Date());

            String cuerpoHtml = "<div style='font-family: Arial, sans-serif; padding: 20px; background: #f8fafc; color: #0f172a;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; padding: 25px; border-radius: 16px; border: 1px solid #cbd5e1;'>" +
                    "<h2 style='color: #0a192f; margin-top: 0;'><span style='color:#ffb703;'>ALELEO TOURS</span> — Nuevo Pago Recibido</h2>" +
                    "<p style='font-size: 15px;'>Se ha confirmado exitosamente una nueva transaccion asociada a la <strong>Llave de Destino: " + llave + "</strong>.</p>" +
                    "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>" +
                    "<table style='width: 100%; font-size: 14px; border-collapse: collapse;'>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>ID Pago:</td><td style='font-weight: bold;'>#" + pago.getId() + "</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Solicitud:</td><td style='font-weight: bold;'>#" + idSolicitud + " — " + tourTitulo + "</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Cliente:</td><td>" + clienteNombre + " (" + clienteCorreo + ")</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Monto Abonado:</td><td style='font-weight: bold; color: #198754; font-size: 16px;'>" + montoFormatted + "</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Metodo de Pago:</td><td>" + (pago.getMetodoPago() != null ? pago.getMetodoPago() : "TRANSFERENCIA") + "</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Fecha & Hora:</td><td>" + fechaFormatted + "</td></tr>" +
                    "<tr><td style='padding: 6px 0; color: #64748b;'>Llave Destino:</td><td style='font-weight: bold; color: #dc3545;'>" + llave + "</td></tr>" +
                    "</table>" +
                    "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>" +
                    "<p style='font-size: 13px; color: #64748b;'>Se adjunta el Comprobante Oficial en formato PDF (Estilo Recibo de Servicio Electrico).</p>" +
                    "</div></div>";

            helper.setText(cuerpoHtml, true);

            if (rutaPdf != null) {
                File pdfFile = new File(rutaPdf);
                if (pdfFile.exists()) {
                    FileSystemResource res = new FileSystemResource(pdfFile);
                    helper.addAttachment("Comprobante_Pago_Solicitud_" + idSolicitud + ".pdf", res);
                }
            }

            mailSender.send(message);
            log.info("[EMAIL EXITOSO] Notificacion enviada al propietario ({}) para el pago #{}", propietarioEmail, pago.getId());
            return true;

        } catch (Exception e) {
            log.error("[FAIL-SAFE EMAIL] No se pudo enviar el correo al propietario ({}) para el pago #{}: {}", propietarioEmail, pago.getId(), e.getMessage());
            return false;
        }
    }
}