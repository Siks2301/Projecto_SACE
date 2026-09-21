package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.ReporteDTOs;
import com.mycompany.sacejpa.DTOs.SolicitudDTOs;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Mapper.SolicitudMapper;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Modelo.Pago;
import com.mycompany.sacejpa.Modelo.Solicitud;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Empleado;
import com.mycompany.sacejpa.Repositorio.Reposi_Pago;
import com.mycompany.sacejpa.Repositorio.Reposi_Servicio;
import com.mycompany.sacejpa.Repositorio.Reposi_Solicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteServicio {

    @Autowired
    private Reposi_Solicitud reposiSolicitud;

    @Autowired
    private Reposi_Cliente reposiCliente;

    @Autowired
    private Reposi_Empleado reposiEmpleado;

    @Autowired
    private Reposi_Servicio reposiServicio;

    @Autowired
    private Reposi_Pago reposiPago;

    private Date parseFechaInicio(String desdeStr) {
        if (desdeStr == null || desdeStr.trim().isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(desdeStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseFechaFin(String hastaStr) {
        if (hastaStr == null || hastaStr.trim().isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date base = sdf.parse(hastaStr.trim());
            Calendar cal = Calendar.getInstance();
            cal.setTime(base);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Solicitud> filtrarPorFechas(List<Solicitud> lista, String desde, String hasta) {
        Date fInicio = parseFechaInicio(desde);
        Date fFin = parseFechaFin(hasta);

        if (fInicio == null && fFin == null) return lista;

        List<Solicitud> filtradas = new ArrayList<>();
        for (Solicitud s : lista) {
            if (s.getFechaCreacion() == null) continue;
            boolean valido = true;
            if (fInicio != null && s.getFechaCreacion().before(fInicio)) {
                valido = false;
            }
            if (fFin != null && s.getFechaCreacion().after(fFin)) {
                valido = false;
            }
            if (valido) {
                filtradas.add(s);
            }
        }
        return filtradas;
    }

    public ReporteDTOs.KpiResumenDTO obtenerKpisGeneral(String desde, String hasta) {
        List<Solicitud> todas = reposiSolicitud.findAll();
        List<Solicitud> solicitudes = filtrarPorFechas(todas, desde, hasta);

        long total = solicitudes.size();
        long pendientes = solicitudes.stream().filter(s -> s.getEstado() == Solicitud.Estado.PENDIENTE).count();
        long enProceso = solicitudes.stream().filter(s -> s.getEstado() == Solicitud.Estado.EN_PROCESO).count();
        long resueltas = solicitudes.stream().filter(s -> s.getEstado() == Solicitud.Estado.RESUELTA).count();
        long canceladas = solicitudes.stream().filter(s -> s.getEstado() == Solicitud.Estado.CANCELADA).count();

        double pctResueltas = total > 0 ? (double) resueltas / total * 100 : 0.0;
        pctResueltas = Math.round(pctResueltas * 10.0) / 10.0;

        long totalClientes = reposiCliente.count();
        long totalServicios = reposiServicio.count();
        long totalEmpleados = reposiEmpleado.count();

        ReporteDTOs.KpiResumenDTO dto = new ReporteDTOs.KpiResumenDTO(
                total, pendientes, enProceso, resueltas, canceladas,
                pctResueltas, totalClientes, totalServicios, totalEmpleados
        );

        // Metricas Financieras de la Llave Bre-B @VXM301
        Date fInicio = parseFechaInicio(desde);
        Date fFin = parseFechaFin(hasta);
        try {
            BigDecimal recaudado = reposiPago.sumTotalMontoRecaudado(fInicio, fFin);
            Long cantPagos = reposiPago.countPagosConfirmados(fInicio, fFin);
            BigDecimal promedio = (cantPagos != null && cantPagos > 0) ? recaudado.divide(new BigDecimal(cantPagos), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            dto.setTotalRecaudadoBreB(recaudado != null ? recaudado : BigDecimal.ZERO);
            dto.setTotalPagosConfirmados(cantPagos != null ? cantPagos : 0);
            dto.setPromedioMontoPago(promedio);
        } catch (Exception e) {
            dto.setTotalRecaudadoBreB(BigDecimal.ZERO);
            dto.setTotalPagosConfirmados(0);
            dto.setPromedioMontoPago(BigDecimal.ZERO);
        }

        return dto;
    }

    public ReporteDTOs.ReporteEmpleadoDTO obtenerReporteEmpleado(Long idEmpleado) {
        if (idEmpleado == null) {
            throw new ValidacionException("El ID de empleado no es valido.");
        }
        Empleado emp = reposiEmpleado.findById(idEmpleado)
                .orElseThrow(() -> new ValidacionException("Empleado #" + idEmpleado + " no encontrado."));

        List<Solicitud> asignadas = reposiSolicitud.findAll().stream()
                .filter(s -> s.getEmpleadoAsignado() != null && s.getEmpleadoAsignado().getId().equals(idEmpleado))
                .collect(Collectors.toList());

        long total = asignadas.size();
        long pendientes = asignadas.stream().filter(s -> s.getEstado() == Solicitud.Estado.PENDIENTE).count();
        long enProceso = asignadas.stream().filter(s -> s.getEstado() == Solicitud.Estado.EN_PROCESO).count();
        long resueltas = asignadas.stream().filter(s -> s.getEstado() == Solicitud.Estado.RESUELTA).count();
        long canceladas = asignadas.stream().filter(s -> s.getEstado() == Solicitud.Estado.CANCELADA).count();

        double pctEfectividad = total > 0 ? (double) resueltas / total * 100 : 0.0;
        pctEfectividad = Math.round(pctEfectividad * 10.0) / 10.0;

        List<SolicitudDTOs> dtoList = asignadas.stream()
                .map(SolicitudMapper::toDTO)
                .collect(Collectors.toList());

        ReporteDTOs.ReporteEmpleadoDTO dto = new ReporteDTOs.ReporteEmpleadoDTO();
        dto.setIdEmpleado(emp.getId());
        dto.setNombreEmpleado(emp.getNombre() + " " + (emp.getApellido() != null ? emp.getApellido() : ""));
        dto.setCargo(emp.getCargoEspecifico() != null ? emp.getCargoEspecifico() : "Asesor de Viajes");
        dto.setDepartamento(emp.getDepartamento() != null ? emp.getDepartamento() : "Atención al Cliente");
        dto.setTotalAsignadas(total);
        dto.setPendientes(pendientes);
        dto.setEnProceso(enProceso);
        dto.setResueltas(resueltas);
        dto.setCanceladas(canceladas);
        dto.setPorcentajeEfectividad(pctEfectividad);
        dto.setSolicitudes(dtoList);

        return dto;
    }

    public List<ReporteDTOs.ConteoItemDTO> obtenerSolicitudesPorEstado(String desde, String hasta) {
        List<Solicitud> todas = reposiSolicitud.findAll();
        List<Solicitud> solicitudes = filtrarPorFechas(todas, desde, hasta);
        Map<String, Long> mapa = new LinkedHashMap<>();

        mapa.put("PENDIENTE", 0L);
        mapa.put("EN_PROCESO", 0L);
        mapa.put("RESUELTA", 0L);
        mapa.put("CANCELADA", 0L);

        for (Solicitud s : solicitudes) {
            if (s.getEstado() != null) {
                String nombre = s.getEstado().name();
                mapa.put(nombre, mapa.getOrDefault(nombre, 0L) + 1);
            }
        }

        List<ReporteDTOs.ConteoItemDTO> resultado = new ArrayList<>();
        mapa.forEach((key, val) -> resultado.add(new ReporteDTOs.ConteoItemDTO(key, val)));
        return resultado;
    }

    public List<ReporteDTOs.ConteoItemDTO> obtenerSolicitudesPorCategoria(String desde, String hasta) {
        List<Solicitud> todas = reposiSolicitud.findAll();
        List<Solicitud> solicitudes = filtrarPorFechas(todas, desde, hasta);
        Map<String, Long> mapa = new HashMap<>();

        for (Solicitud s : solicitudes) {
            String cat = (s.getCategoria() != null && !s.getCategoria().trim().isEmpty())
                    ? s.getCategoria()
                    : "CONSULTA_GENERAL";
            mapa.put(cat, mapa.getOrDefault(cat, 0L) + 1);
        }

        List<ReporteDTOs.ConteoItemDTO> resultado = new ArrayList<>();
        mapa.forEach((key, val) -> resultado.add(new ReporteDTOs.ConteoItemDTO(key, val)));
        resultado.sort((a, b) -> Long.compare(b.getCantidad(), a.getCantidad()));
        return resultado;
    }

    public List<ReporteDTOs.ConteoItemDTO> obtenerTendenciaSolicitudes(String desde, String hasta) {
        List<Solicitud> todas = reposiSolicitud.findAll();
        List<Solicitud> solicitudes = filtrarPorFechas(todas, desde, hasta);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Map<String, Long> mapa = new TreeMap<>();

        for (Solicitud s : solicitudes) {
            if (s.getFechaCreacion() != null) {
                String mes = sdf.format(s.getFechaCreacion());
                mapa.put(mes, mapa.getOrDefault(mes, 0L) + 1);
            }
        }

        List<ReporteDTOs.ConteoItemDTO> resultado = new ArrayList<>();
        mapa.forEach((key, val) -> resultado.add(new ReporteDTOs.ConteoItemDTO(key, val)));
        return resultado;
    }

    public List<ReporteDTOs.ConteoItemDTO> obtenerRendimientoEmpleados(String desde, String hasta) {
        List<Solicitud> todas = reposiSolicitud.findAll();
        List<Solicitud> solicitudes = filtrarPorFechas(todas, desde, hasta);
        Map<String, Long> mapa = new HashMap<>();

        for (Solicitud s : solicitudes) {
            if (s.getEmpleadoAsignado() != null) {
                Empleado emp = s.getEmpleadoAsignado();
                String nombreCompleto = emp.getNombre() + " " + (emp.getApellido() != null ? emp.getApellido() : "");
                mapa.put(nombreCompleto.trim(), mapa.getOrDefault(nombreCompleto.trim(), 0L) + 1);
            }
        }

        List<ReporteDTOs.ConteoItemDTO> resultado = new ArrayList<>();
        mapa.forEach((key, val) -> resultado.add(new ReporteDTOs.ConteoItemDTO(key, val)));
        resultado.sort((a, b) -> Long.compare(b.getCantidad(), a.getCantidad()));
        return resultado;
    }
}