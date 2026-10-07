package com.mycompany.sacejpa.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.sacejpa.Modelo.Pago;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Solicitud;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Servicio encargado de la generacion de Comprobantes PDF estilo Recibo de Servicio Electrico / Factura.
 * Aplica principios defensivos de software para garantizar que ningun valor nulo o indefinido rompa la plantilla.
 */
@Service
public class PdfComprobanteServicio {

    private static final Logger log = LoggerFactory.getLogger(PdfComprobanteServicio.class);

    @Value("${app.upload.dir:uploads/comprobantes}")
    private String uploadDir;

    private static final DecimalFormat MONTO_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat FECHA_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", new Locale("es", "CO"));

    /**
     * Genera el PDF del comprobante y devuelve la ruta <b>relativa</b> que se
     * guarda en {@code pago.url_pdf}.
     *
     * <p>Guardar la ruta absoluta era un error de portability: la base quedaba
     * atada a la maquina y al usuario de Windows que la creo
     * ({@code C:\Users\cesar\...}). En otra maquina, en un servidor, o tras
     * cambiar de usuario, el comprobante no existia y el pago quedaba con un
     * enlace roto. Con la ruta relativa el archivo se resuelve en tiempo de
     * lectura contra el directorio de la aplicacion.
     */
    public String generarComprobantePdf(Pago pago) throws Exception {
        if (pago == null) {
            throw new IllegalArgumentException("El objeto Pago no puede ser nulo.");
        }

        File dir = getDirectorioUpload();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de comprobantes: " + dir.getAbsolutePath());
        }

        String nombreArchivo = "recibo_pago_" + (pago.getId() != null ? pago.getId() : System.currentTimeMillis()) + ".pdf";
        File archivoSalida = new File(dir, nombreArchivo);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(archivoSalida));

        document.open();

        // 1. Fuentes
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(10, 25, 47));
        Font fontSubTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 168, 150));
        Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
        Font fontDestacado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(220, 53, 69));

        // 2. Encabezado Estilo Recibo
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65f, 35f});

        // Columna Izquierda: Logo y Empresa
        PdfPCell cellEmpresa = new PdfPCell();
        cellEmpresa.setBorder(Rectangle.NO_BORDER);
        Paragraph pEmpresa = new Paragraph("ALELEO TOURS S.A.S.", fontTitulo);
        Paragraph pSub = new Paragraph("FACTURA DE VENTA", fontSubTitulo);
        Paragraph pSub2 = new Paragraph("COMPROBANTE OFICIAL DE PAGO Y RESERVA", FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139)));
        Paragraph pNit = new Paragraph("NIT: 901.452.879-1 | PBX: (601) 745-8899\nAv. El Dorado #68B-31, Bogota D.C. - Colombia", fontNormal);
        cellEmpresa.addElement(pEmpresa);
        cellEmpresa.addElement(pSub);
        cellEmpresa.addElement(pSub2);
        cellEmpresa.addElement(pNit);
        headerTable.addCell(cellEmpresa);

        // Columna Derecha: Cuadro de Folio y Estado
        PdfPCell cellFolio = new PdfPCell();
        cellFolio.setBackgroundColor(new Color(241, 245, 249));
        cellFolio.setPadding(10);
        cellFolio.setBorderColor(new Color(203, 213, 225));
        cellFolio.setBorderWidth(1.5f);

        String folioStr = "REC-PAGO-" + sanitizar(pago.getId() != null ? String.format("%06d", pago.getId()) : "000000");
        String fechaStr = FECHA_FORMAT.format(pago.getFechaPago() != null ? pago.getFechaPago() : new Date());

        Paragraph pFolioLbl = new Paragraph("FOLIO UNICO DE TRANSACCION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139)));
        Paragraph pFolioVal = new Paragraph(folioStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(13, 110, 253)));
        Paragraph pFechaLbl = new Paragraph("FECHA & HORA DE CONFIRMACION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 116, 139)));
        Paragraph pFechaVal = new Paragraph(fechaStr, fontNormal);

        cellFolio.addElement(pFolioLbl);
        cellFolio.addElement(pFolioVal);
        cellFolio.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
        cellFolio.addElement(pFechaLbl);
        cellFolio.addElement(pFechaVal);
        headerTable.addCell(cellFolio);

        document.add(headerTable);
        document.add(new Paragraph(" "));

        // 3. Bloque Informativo: Cliente y Llave Destino
        Cliente cliente = pago.getCliente();
        Solicitud solicitud = pago.getSolicitud();

        String nombreCliente = cliente != null ? sanitizar(cliente.getNombre() + " " + (cliente.getApellido() != null ? cliente.getApellido() : "")) : "N/A";
        String emailCliente = cliente != null ? sanitizar(cliente.getEmail()) : "N/A";
        String telefonoCliente = cliente != null ? sanitizar(cliente.getTelefono()) : "N/A";
        String tipoDoc = cliente != null && cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().name() : null;
        String numDoc = cliente != null ? cliente.getNumeroDocumento() : null;
        String documentoCliente = (tipoDoc != null && numDoc != null && !numDoc.trim().isEmpty())
                ? sanitizar(tipoDoc + " " + numDoc)
                : "N/A";
        String idSolicitudStr = solicitud != null ? "#SOL-" + solicitud.getId() : "N/A";
        String tituloTour = solicitud != null ? sanitizar(solicitud.getTitulo()) : "Reserva / Servicio Turistico";
        String referencia = sanitizar(pago.getReferencia(), "S/N");
        String metodoPago = sanitizar(
                pago.getMetodoPago() != null ? pago.getMetodoPago().etiqueta() : null,
                "Metodo no especificado");
        String pasarela = sanitizar(pago.getPasarela(), "No especificada");
        String autorizacion = sanitizar(pago.getCodigoAutorizacion(), "No informado");

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{50f, 50f});

        // Box Cliente
        PdfPCell cClient = new PdfPCell();
        cClient.setBorderColor(new Color(226, 232, 240));
        cClient.setPadding(8);
        cClient.addElement(new Paragraph("DATOS DEL TITULAR / CLIENTE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(13, 110, 253))));
        cClient.addElement(new Paragraph("Nombre: " + nombreCliente, fontNormal));
        cClient.addElement(new Paragraph("Correo: " + emailCliente, fontNormal));
        cClient.addElement(new Paragraph("Telefono: " + telefonoCliente, fontNormal));
        cClient.addElement(new Paragraph("Documento: " + documentoCliente, fontNormal));
        infoTable.addCell(cClient);

        // Box Destino & Solicitud
        PdfPCell cDestino = new PdfPCell();
        cDestino.setBorderColor(new Color(226, 232, 240));
        cDestino.setPadding(8);
        cDestino.addElement(new Paragraph("DATOS DE LA TRANSACCION & RESERVA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(13, 110, 253))));
        cDestino.addElement(new Paragraph("Solicitud Asociada: " + idSolicitudStr, fontBold));
        cDestino.addElement(new Paragraph("REFERENCIA DE PAGO: " + referencia, fontDestacado));
        cDestino.addElement(new Paragraph("Metodo de Pago: " + metodoPago, fontNormal));
        cDestino.addElement(new Paragraph("Pasarela: " + pasarela, fontNormal));
        cDestino.addElement(new Paragraph("Codigo de Autorizacion: " + autorizacion, fontNormal));
        infoTable.addCell(cDestino);

        document.add(infoTable);
        document.add(new Paragraph(" "));

        // 4. Tabla Desglose (Estilo Factura de Servicio Electrico)
        PdfPTable itemTable = new PdfPTable(5);
        itemTable.setWidthPercentage(100);
        itemTable.setWidths(new float[]{12f, 42f, 8f, 18f, 20f});

        Color headerBg = new Color(10, 25, 47);
        addCellHeader(itemTable, "CODIGO", fontHeaderTabla, headerBg);
        addCellHeader(itemTable, "CONCEPTO / SERVICIO TURISTICO", fontHeaderTabla, headerBg);
        addCellHeader(itemTable, "CANT.", fontHeaderTabla, headerBg);
        addCellHeader(itemTable, "VLR. UNITARIO", fontHeaderTabla, headerBg);
        addCellHeader(itemTable, "VALOR TOTAL", fontHeaderTabla, headerBg);

        BigDecimal total = pago.getMonto() != null ? pago.getMonto() : BigDecimal.ZERO;
        BigDecimal subtotal = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP); // Base exacta: Total / 1.19 (IVA 19% incluido)
        BigDecimal iva = total.subtract(subtotal);
        BigDecimal valorUnitario = total; // CANT. siempre 1 => valor unitario == total

        addCellBody(itemTable, idSolicitudStr, fontNormal, Element.ALIGN_CENTER);
        // No se antepone una etiqueta fija de "Reserva de Tour / Plan": el titulo de la
        // solicitud ya trae su propia descripcion (p.ej. "Reserva: ...", "Cotizacion: ...")
        // y anteponer un texto fijo duplicaba la palabra y ademas era incorrecto para
        // solicitudes que no son de tipo Reserva (Cotizacion, Consulta, PQR, etc).
        addCellBody(itemTable, tituloTour + " (Pago en linea verificado por pasarela)", fontNormal, Element.ALIGN_LEFT);
        addCellBody(itemTable, "1", fontNormal, Element.ALIGN_CENTER);
        addCellBody(itemTable, "$ " + MONTO_FORMAT.format(valorUnitario) + " COP", fontNormal, Element.ALIGN_RIGHT);
        addCellBody(itemTable, "$ " + MONTO_FORMAT.format(total) + " COP", fontNormal, Element.ALIGN_RIGHT);

        document.add(itemTable);

        // 5. Tabla Resumen Totales
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(100);
        totalsTable.setWidths(new float[]{60f, 40f});

        PdfPCell cNote = new PdfPCell();
        cNote.setBorder(Rectangle.NO_BORDER);
        Paragraph pSeal = new Paragraph("SELLO DIGITAL DE CONFIRMACION DE PAGO SACE (SHA-256)\n" +
                "Hash: " + sha256Hex(folioStr + "|" + referencia + "|" + total.toPlainString()).substring(0, 16) + "-OK\n" +
                "Este documento cumple con lo dispuesto en el artículo 617 del Estatuto Tributario.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139)));
        cNote.addElement(pSeal);
        totalsTable.addCell(cNote);

        PdfPCell cTotals = new PdfPCell();
        cTotals.setBorder(Rectangle.NO_BORDER);
        PdfPTable innerTotals = new PdfPTable(2);
        innerTotals.setWidthPercentage(100);
        innerTotals.setWidths(new float[]{60f, 40f});

        addCellBody(innerTotals, "Subtotal:", fontNormal, Element.ALIGN_RIGHT);
        addCellBody(innerTotals, "$ " + MONTO_FORMAT.format(subtotal), fontNormal, Element.ALIGN_RIGHT);

        addCellBody(innerTotals, "IVA (19%):", fontNormal, Element.ALIGN_RIGHT);
        addCellBody(innerTotals, "$ " + MONTO_FORMAT.format(iva), fontNormal, Element.ALIGN_RIGHT);

        // Línea divisoria entre IVA y total
        PdfPCell div1 = new PdfPCell();
        div1.setBorder(Rectangle.NO_BORDER);
        div1.setFixedHeight(4);
        innerTotals.addCell(div1);
        PdfPCell div2 = new PdfPCell(new Paragraph(" ", fontNormal));
        div2.setBorder(Rectangle.BOTTOM);
        div2.setFixedHeight(4);
        innerTotals.addCell(div2);

        PdfPCell lblTotal = new PdfPCell(new Paragraph("TOTAL:", fontBold));
        lblTotal.setBorder(Rectangle.NO_BORDER);
        lblTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lblTotal.setPaddingTop(4);
        innerTotals.addCell(lblTotal);

        PdfPCell valTotal = new PdfPCell(new Paragraph("$ " + MONTO_FORMAT.format(total) + " COP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(25, 135, 84))));
        valTotal.setBorder(Rectangle.TOP);
        valTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valTotal.setPaddingTop(4);
        innerTotals.addCell(valTotal);

        cTotals.addElement(innerTotals);
        totalsTable.addCell(cTotals);

        document.add(totalsTable);

        // Footer Promocional y Legal
        document.add(new Paragraph("\n---------------------------------------------------------------------------------------------------------------------------------", fontNormal));
        Paragraph pFooter = new Paragraph("¡Gracias por viajar con AleLeo Tours! Si tienes dudas sobre tu reserva, contáctanos a soporte@aleleotours.com\n" +
                "Documento emitido electrónicamente por el Sistema SACE. Validez legal conforme a la regulación turística.", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        pFooter.setAlignment(Element.ALIGN_CENTER);
        document.add(pFooter);

        document.close();

        // Relativa al directorio de trabajo de la aplicacion, que es donde vive
        // 'uploadDir'. Ver resolverRutaDeComprobante().
        return construirRutaRelativa(nombreArchivo);
    }

    /** Directorio fisico donde se escriben los comprobantes. */
    private File getDirectorioUpload() {
        Path base = Paths.get(uploadDir);
        // Una ruta absoluta en la configuracion manda sobre el directorio de trabajo.
        return base.isAbsolute() ? base.toFile() : Paths.get(System.getProperty("user.dir"), uploadDir).toFile();
    }

    /**
     * Construye la ruta que se guarda en la base: relativa y siempre con
     * separadores {@code /}, para que el mismo valor sirva en Windows y en Linux.
     */
    private String construirRutaRelativa(String nombreArchivo) {
        String base = Paths.get(uploadDir).normalize().toString().replace('\\', '/');
        return base.endsWith("/") ? base + nombreArchivo : base + "/" + nombreArchivo;
    }

    /**
     * Resuelve la ruta guardada en la base contra el disco.
     *
     * <p>Acepta tanto la ruta relativa nueva como la absoluta heredada de datos
     * anteriores. Para la heredada se usa solo el nombre del archivo: si la ruta
     * absoluta ya no existe (otra maquina, otro usuario, carpeta eliminada) pero
     * el PDF esta en el directorio de trabajo, el comprobante sigue abriendose en
     * lugar de romperse.
     *
     * @return archivo existente, o {@code null} si no hay nada que abrir
     */
    public File resolverRutaDeComprobante(String rutaGuardada) {
        if (rutaGuardada == null || rutaGuardada.trim().isEmpty()) {
            return null;
        }
        String ruta = rutaGuardada.trim();

        // 1. La ruta tal cual, sea relativa o absoluta.
        Path directa = Paths.get(ruta);
        if (!directa.isAbsolute()) {
            directa = Paths.get(System.getProperty("user.dir"), ruta);
        }
        if (Files.isRegularFile(directa)) {
            return directa.toFile();
        }

        // 2. Ruta absoluta heredada: se recupera solo el nombre del archivo.
        int separador = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        if (separador >= 0 && separador < ruta.length() - 1) {
            String nombre = ruta.substring(separador + 1);
            // El nombre no debe poder escapar del directorio configurado.
            if (!nombre.contains("..")) {
                File porNombre = new File(getDirectorioUpload(), nombre);
                if (porNombre.isFile()) {
                    log.warn("La ruta del comprobante '{}' no es valida en esta maquina; "
                            + "se resolvio por nombre dentro de {}. Conviene ejecutar "
                            + "migracion_rutas_comprobantes.sql.", ruta, getDirectorioUpload().getAbsolutePath());
                    return porNombre;
                }
            }
        }

        log.warn("No se encontro el comprobante para la ruta '{}'.", ruta);
        return null;
    }

    /**
     * Resumen SHA-256 (hexadecimal en mayusculas) de la cadena canonica del
     * sello. A diferencia del antiguo String.hashCode() (no criptografico,
     * triviable de falsificar), SHA-256 es un hash criptografico real de
     * 64 digitos hex; aqui se usan los primeros 16 como huella visible.
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", 0xFF & b));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            // Nunca romper el comprobante por una falla del sello: respaldo deterministico
            return String.format("%016X", input.hashCode());
        }
    }

    private String sanitizar(String valor) {
        return sanitizar(valor, "N/A");
    }

    private String sanitizar(String valor, String porDefecto) {
        if (valor == null || valor.trim().isEmpty() || "null".equalsIgnoreCase(valor.trim()) || "undefined".equalsIgnoreCase(valor.trim())) {
            return porDefecto;
        }
        return valor.trim();
    }

    private void addCellHeader(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCellBody(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }
}
