package com.mycompany.sacejpa.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.sacejpa.DTOs.ReporteDTOs;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Servicio encargado de generar el Reporte Ejecutivo de Gestion y Recaudo como
 * un documento PDF formal (con membrete, tablas de desglose y pie de pagina),
 * en lugar de depender de la impresion del navegador sobre el dashboard HTML.
 * Sigue el mismo estilo visual que PdfComprobanteServicio para mantener
 * identidad corporativa consistente en todos los documentos emitidos por SACE.
 */
public class ReportePdfServicio {

    private static final DecimalFormat MONTO_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat FECHA_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh:mm a", new Locale("es", "CO"));

    private static final Color AZUL_OSCURO = new Color(10, 25, 47);
    private static final Color AZUL_PRIMARIO = new Color(13, 110, 253);
    private static final Color VERDE = new Color(25, 135, 84);
    private static final Color GRIS_TEXTO = new Color(100, 116, 139);
    private static final Color GRIS_BORDE = new Color(226, 232, 240);

    public byte[] generarReporteEjecutivo(
            ReporteDTOs.KpiResumenDTO kpis,
            List<ReporteDTOs.ConteoItemDTO> porEstado,
            List<ReporteDTOs.ConteoItemDTO> porCategoria,
            List<ReporteDTOs.ConteoItemDTO> porEmpleado,
            String desde,
            String hasta,
            String generadoPor
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 50, 54);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PiePaginaEvent());
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, AZUL_OSCURO);
        Font fontSubTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 168, 150));
        Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
        Font fontSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL_OSCURO);

        // 1. Membrete corporativo (igual identidad que el comprobante de pago)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65f, 35f});

        PdfPCell cellEmpresa = new PdfPCell();
        cellEmpresa.setBorder(Rectangle.NO_BORDER);
        cellEmpresa.addElement(new Paragraph("ALELEO TOURS S.A.S.", fontTitulo));
        cellEmpresa.addElement(new Paragraph("REPORTE EJECUTIVO DE GESTION Y RECAUDO", fontSubTitulo));
        cellEmpresa.addElement(new Paragraph("NIT: 901.452.879-1 | PBX: (601) 745-8899\nAv. El Dorado #68B-31, Bogota D.C. - Colombia", fontNormal));
        headerTable.addCell(cellEmpresa);

        PdfPCell cellFolio = new PdfPCell();
        cellFolio.setBackgroundColor(new Color(241, 245, 249));
        cellFolio.setPadding(10);
        cellFolio.setBorderColor(GRIS_BORDE);
        cellFolio.setBorderWidth(1.5f);
        String rangoStr = (desde == null || desde.isEmpty() ? "Inicio" : desde) + "  a  " + (hasta == null || hasta.isEmpty() ? "Hoy" : hasta);
        cellFolio.addElement(new Paragraph("PERIODO CUBIERTO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GRIS_TEXTO)));
        cellFolio.addElement(new Paragraph(rangoStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL_PRIMARIO)));
        cellFolio.addElement(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
        cellFolio.addElement(new Paragraph("GENERADO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, GRIS_TEXTO)));
        cellFolio.addElement(new Paragraph(FECHA_FORMAT.format(new Date()) + "\nPor: " + (generadoPor == null ? "Usuario Autorizado SACE" : generadoPor), fontNormal));
        headerTable.addCell(cellFolio);

        document.add(headerTable);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

        // 2. Resumen de solicitudes (KPIs operativos)
        document.add(new Paragraph("RESUMEN OPERATIVO DE SOLICITUDES", fontSeccion));
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        agregarKpiCelda(kpiTable, "Total Solicitudes", String.valueOf(kpis.getTotalSolicitudes()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Pendientes", String.valueOf(kpis.getSolicitudesPendientes()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "En Proceso", String.valueOf(kpis.getSolicitudesEnProceso()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Resueltas", String.valueOf(kpis.getSolicitudesResueltas()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Canceladas", String.valueOf(kpis.getSolicitudesCanceladas()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Efectividad", String.format(Locale.US, "%.1f%%", kpis.getPorcentajeResueltas()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Clientes Registrados", String.valueOf(kpis.getTotalClientes()), fontBold, fontNormal);
        agregarKpiCelda(kpiTable, "Servicios / Empleados", kpis.getTotalServicios() + " / " + kpis.getTotalEmpleados(), fontBold, fontNormal);
        document.add(kpiTable);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

        // 3. Resumen financiero (llave Bre-B @VXM301)
        document.add(new Paragraph("AUDITORIA DE RECAUDO FINANCIERO", fontSeccion));
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable finTable = new PdfPTable(3);
        finTable.setWidthPercentage(100);
        finTable.setWidths(new float[]{34f, 33f, 33f});
        BigDecimal recaudado = kpis.getTotalRecaudadoBreB() != null ? kpis.getTotalRecaudadoBreB() : BigDecimal.ZERO;
        BigDecimal promedio = kpis.getPromedioMontoPago() != null ? kpis.getPromedioMontoPago() : BigDecimal.ZERO;
        agregarFinCelda(finTable, "Total Ingresos", "$ " + MONTO_FORMAT.format(recaudado) + " COP", VERDE, fontNormal);
        agregarFinCelda(finTable, "Pagos Confirmados", String.valueOf(kpis.getTotalPagosConfirmados()), AZUL_PRIMARIO, fontNormal);
        agregarFinCelda(finTable, "Promedio por Transaccion", "$ " + MONTO_FORMAT.format(promedio) + " COP", AZUL_OSCURO, fontNormal);
        document.add(finTable);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

        // 4. Desgloses en tablas reales (esto es lo que un reporte de dashboard impreso no tiene)
        //    Se humanizan las etiquetas (EN_PROCESO -> "En Proceso") igual que ya hace el frontend,
        //    para que un documento formal no muestre nombres crudos de enum de la base de datos.
        List<ReporteDTOs.ConteoItemDTO> estadoLegible = humanizarEtiquetas(porEstado, this::formatearEstado);
        List<ReporteDTOs.ConteoItemDTO> categoriaLegible = humanizarEtiquetas(porCategoria, this::formatearCategoria);

        // Si hay solicitudes sin asesor asignado, se agrega esa fila: así la tabla de
        // asesores también suma 100% del total y no deja un porcentaje "perdido" sin explicar.
        List<ReporteDTOs.ConteoItemDTO> empleadoCompleto = new java.util.ArrayList<>(porEmpleado != null ? porEmpleado : List.of());
        long asignadas = empleadoCompleto.stream().mapToLong(ReporteDTOs.ConteoItemDTO::getCantidad).sum();
        if (kpis.getTotalSolicitudes() > asignadas) {
            empleadoCompleto.add(new ReporteDTOs.ConteoItemDTO("Sin Asesor Asignado", kpis.getTotalSolicitudes() - asignadas));
        }

        agregarTablaDesglose(document, "SOLICITUDES POR ESTADO", estadoLegible, fontSeccion, fontHeaderTabla, fontNormal, fontBold, kpis.getTotalSolicitudes());
        agregarTablaDesglose(document, "SOLICITUDES POR CATEGORIA", categoriaLegible, fontSeccion, fontHeaderTabla, fontNormal, fontBold, kpis.getTotalSolicitudes());
        agregarTablaDesglose(document, "RENDIMIENTO POR ASESOR", empleadoCompleto, fontSeccion, fontHeaderTabla, fontNormal, fontBold, kpis.getTotalSolicitudes());

        // 5. Firma / validez del documento
        document.add(new Paragraph(" "));
        PdfPTable firmaTable = new PdfPTable(2);
        firmaTable.setWidthPercentage(60);
        firmaTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell firmaCell = new PdfPCell();
        firmaCell.setBorder(Rectangle.TOP);
        firmaCell.setBorderColor(GRIS_TEXTO);
        firmaCell.setPaddingTop(4);
        firmaCell.addElement(new Paragraph("Firma y sello del Administrador responsable", FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_TEXTO)));
        firmaTable.addCell(firmaCell);
        PdfPCell vacio = new PdfPCell(new Phrase(""));
        vacio.setBorder(Rectangle.NO_BORDER);
        firmaTable.addCell(vacio);
        document.add(firmaTable);

        document.add(new Paragraph("\nDocumento emitido electronicamente por el Sistema SACE - AleLeo Tours. Uso interno administrativo.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));

        document.close();
        return baos.toByteArray();
    }

    private void agregarKpiCelda(PdfPTable table, String etiqueta, String valor, Font fontValor, Font fontEtiqueta) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBorderColor(GRIS_BORDE);
        cell.setBackgroundColor(new Color(248, 250, 252));
        Paragraph pValor = new Paragraph(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, AZUL_OSCURO));
        Paragraph pEtiqueta = new Paragraph(etiqueta.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, GRIS_TEXTO));
        cell.addElement(pValor);
        cell.addElement(pEtiqueta);
        table.addCell(cell);
    }

    private void agregarFinCelda(PdfPTable table, String etiqueta, String valor, Color colorValor, Font fontEtiqueta) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderColor(GRIS_BORDE);
        cell.addElement(new Paragraph(etiqueta.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, GRIS_TEXTO)));
        cell.addElement(new Paragraph(valor, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, colorValor)));
        table.addCell(cell);
    }

    private void agregarTablaDesglose(Document document, String titulo, List<ReporteDTOs.ConteoItemDTO> datos,
                                       Font fontSeccion, Font fontHeaderTabla, Font fontNormal, Font fontBold, long totalGeneral) throws DocumentException {
        document.add(new Paragraph(titulo, fontSeccion));
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55f, 20f, 25f});

        addCellHeader(table, "DETALLE", fontHeaderTabla);
        addCellHeader(table, "CANTIDAD", fontHeaderTabla);
        addCellHeader(table, "% DEL TOTAL", fontHeaderTabla);
        // Si la tabla no cabe completa en la pagina y se corta, repite este encabezado
        // en la pagina siguiente en vez de dejar filas sueltas sin contexto.
        table.setHeaderRows(1);

        if (datos == null || datos.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin registros en el periodo seleccionado.", fontNormal));
            vacio.setColspan(3);
            vacio.setPadding(8);
            vacio.setBorderColor(GRIS_BORDE);
            table.addCell(vacio);
        } else {
            for (ReporteDTOs.ConteoItemDTO item : datos) {
                double pct = totalGeneral > 0 ? (item.getCantidad() * 100.0 / totalGeneral) : 0.0;
                addCellBody(table, item.getEtiqueta() != null ? item.getEtiqueta() : "N/A", fontNormal, Element.ALIGN_LEFT);
                addCellBody(table, String.valueOf(item.getCantidad()), fontBold, Element.ALIGN_CENTER);
                addCellBody(table, String.format(Locale.US, "%.1f%%", pct), fontNormal, Element.ALIGN_CENTER);
            }
        }

        document.add(table);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
    }

    private interface Formateador {
        String formatear(String valor);
    }

    /** Aplica un formateador a una copia de la lista, sin mutar la lista original recibida del servicio. */
    private List<ReporteDTOs.ConteoItemDTO> humanizarEtiquetas(List<ReporteDTOs.ConteoItemDTO> datos, Formateador formateador) {
        if (datos == null) return List.of();
        List<ReporteDTOs.ConteoItemDTO> copia = new java.util.ArrayList<>();
        for (ReporteDTOs.ConteoItemDTO item : datos) {
            copia.add(new ReporteDTOs.ConteoItemDTO(formateador.formatear(item.getEtiqueta()), item.getCantidad()));
        }
        return copia;
    }

    private String formatearEstado(String est) {
        if (est == null) return "N/A";
        switch (est) {
            case "PENDIENTE": return "Pendiente";
            case "EN_PROCESO": return "En Proceso";
            case "RESUELTA": return "Resuelta";
            case "CANCELADA": return "Cancelada";
            default: return est;
        }
    }

    private String formatearCategoria(String cat) {
        if (cat == null) return "N/A";
        switch (cat) {
            case "COTIZACION": return "Cotizaciones";
            case "RESERVA": return "Reservas";
            case "CAMBIO_FECHA": return "Cambios de Fecha";
            case "CANCELACION_REEMBOLSO": return "Cancelacion / Reembolso";
            case "PQR_SERVICIO": return "PQR Servicio";
            case "CONSULTA_GENERAL": return "Consulta General";
            case "CONSULTA": return "Consulta";
            default: return cat.replace("_", " ");
        }
    }

    private void addCellHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(AZUL_OSCURO);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCellBody(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        cell.setBorderColor(GRIS_BORDE);
        table.addCell(cell);
    }

    /** Pie de pagina con numeracion real (Pagina X de Y), indispensable en un reporte formal. */
    private static class PiePaginaEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPCell cell = new PdfPCell(new Phrase(
                    "SACE - AleLeo Tours  |  Pagina " + writer.getPageNumber(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPTable footer = new PdfPTable(1);
            try {
                footer.setTotalWidth(document.getPageSize().getWidth() - 72);
            } catch (Exception ignored) {
            }
            footer.addCell(cell);
            footer.writeSelectedRows(0, -1, 36, 36, writer.getDirectContent());
        }
    }
}
