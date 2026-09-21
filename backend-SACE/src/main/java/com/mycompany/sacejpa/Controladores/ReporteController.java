package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.ReporteDTOs;
import com.mycompany.sacejpa.Exceptions.AutorizacionException;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Seguridad.AuthInterceptor;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import com.mycompany.sacejpa.Servicios.ReporteServicio;
import com.mycompany.sacejpa.Servicios.ReportePdfServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteServicio reporteServicio;

    private final ReportePdfServicio reportePdfServicio = new ReportePdfServicio();

    private void validarSoloAdmin(HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion) request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (sesion == null || !"ADMINISTRADOR".equalsIgnoreCase(sesion.tipoUsuario())) {
            throw new AutorizacionException("Acceso denegado: El modulo de reportes administrativos es exclusivo para Administradores.");
        }
    }

    private void validarAdminOEmpleadoPropio(Long idEmpleado, HttpServletRequest request) {
        TokenServicio.Sesion sesion = (TokenServicio.Sesion) request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        if (sesion == null) {
            throw new ValidacionException("Acceso denegado: Debes iniciar sesion para consultar tus metricas.");
        }
        boolean esAdmin = "ADMINISTRADOR".equalsIgnoreCase(sesion.tipoUsuario());
        boolean esPropio = idEmpleado != null && idEmpleado.equals(sesion.id());

        if (!esAdmin && !esPropio) {
            throw new AutorizacionException("Acceso denegado: No tienes permiso para consultar los reportes de este empleado.");
        }
    }

    /**
     * GET /api/reportes/admin?desde={fecha}&hasta={fecha}
     * Endpoint analitico que devuelve KPIs (totales, promedios, ingresos a Bre-B @VXM301) filtrados por rango de fechas.
     */
    @GetMapping("/admin")
    public ReporteDTOs.KpiResumenDTO obtenerReporteAdmin(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerKpisGeneral(desde, hasta);
    }

    /**
     * GET /api/reportes/empleado/{idEmpleado}
     * Endpoint operativo con asignaciones y metricas del empleado.
     */
    @GetMapping("/empleado/{idEmpleado}")
    public ReporteDTOs.ReporteEmpleadoDTO obtenerReporteEmpleado(
            @PathVariable Long idEmpleado,
            HttpServletRequest request) {
        validarAdminOEmpleadoPropio(idEmpleado, request);
        return reporteServicio.obtenerReporteEmpleado(idEmpleado);
    }

    @GetMapping("/kpis")
    public ReporteDTOs.KpiResumenDTO obtenerKpis(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerKpisGeneral(desde, hasta);
    }

    @GetMapping("/estados")
    public List<ReporteDTOs.ConteoItemDTO> obtenerPorEstado(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerSolicitudesPorEstado(desde, hasta);
    }

    @GetMapping("/categorias")
    public List<ReporteDTOs.ConteoItemDTO> obtenerPorCategoria(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerSolicitudesPorCategoria(desde, hasta);
    }

    @GetMapping("/tendencia")
    public List<ReporteDTOs.ConteoItemDTO> obtenerTendencia(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerTendenciaSolicitudes(desde, hasta);
    }

    @GetMapping("/empleados")
    public List<ReporteDTOs.ConteoItemDTO> obtenerRendimientoEmpleados(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);
        return reporteServicio.obtenerRendimientoEmpleados(desde, hasta);
    }

    /**
     * GET /api/reportes/pdf?desde={fecha}&hasta={fecha}
     * Genera y descarga el Reporte Ejecutivo como documento PDF formal
     * (membrete, tablas de desglose, firma y numeracion de paginas),
     * en lugar de depender de la impresion del navegador sobre el dashboard.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            HttpServletRequest request) {
        validarSoloAdmin(request);

        TokenServicio.Sesion sesion = (TokenServicio.Sesion) request.getAttribute(AuthInterceptor.ATRIBUTO_SESION);
        String generadoPor = sesion != null ? sesion.tipoUsuario() + " (ID " + sesion.id() + ")" : "Usuario Autorizado SACE";

        try {
            ReporteDTOs.KpiResumenDTO kpis = reporteServicio.obtenerKpisGeneral(desde, hasta);
            List<ReporteDTOs.ConteoItemDTO> porEstado = reporteServicio.obtenerSolicitudesPorEstado(desde, hasta);
            List<ReporteDTOs.ConteoItemDTO> porCategoria = reporteServicio.obtenerSolicitudesPorCategoria(desde, hasta);
            List<ReporteDTOs.ConteoItemDTO> porEmpleado = reporteServicio.obtenerRendimientoEmpleados(desde, hasta);

            byte[] pdf = reportePdfServicio.generarReporteEjecutivo(kpis, porEstado, porCategoria, porEmpleado, desde, hasta, generadoPor);

            String nombreArchivo = "Reporte_Ejecutivo_SACE_" + System.currentTimeMillis() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            throw new ValidacionException("Error al generar el PDF del reporte ejecutivo: " + e.getMessage());
        }
    }
}
