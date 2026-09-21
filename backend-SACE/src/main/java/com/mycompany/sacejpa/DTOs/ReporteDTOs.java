package com.mycompany.sacejpa.DTOs;

import java.math.BigDecimal;
import java.util.List;

public class ReporteDTOs {

    public static class KpiResumenDTO {
        private long totalSolicitudes;
        private long solicitudesPendientes;
        private long solicitudesEnProceso;
        private long solicitudesResueltas;
        private long solicitudesCanceladas;
        private double porcentajeResueltas;
        private long totalClientes;
        private long totalServicios;
        private long totalEmpleados;
        private BigDecimal totalRecaudadoBreB;
        private long totalPagosConfirmados;
        private BigDecimal promedioMontoPago;

        public KpiResumenDTO() {}

        public KpiResumenDTO(long totalSolicitudes, long solicitudesPendientes, long solicitudesEnProceso,
                             long solicitudesResueltas, long solicitudesCanceladas, double porcentajeResueltas,
                             long totalClientes, long totalServicios, long totalEmpleados) {
            this.totalSolicitudes = totalSolicitudes;
            this.solicitudesPendientes = solicitudesPendientes;
            this.solicitudesEnProceso = solicitudesEnProceso;
            this.solicitudesResueltas = solicitudesResueltas;
            this.solicitudesCanceladas = solicitudesCanceladas;
            this.porcentajeResueltas = porcentajeResueltas;
            this.totalClientes = totalClientes;
            this.totalServicios = totalServicios;
            this.totalEmpleados = totalEmpleados;
            this.totalRecaudadoBreB = BigDecimal.ZERO;
            this.totalPagosConfirmados = 0;
            this.promedioMontoPago = BigDecimal.ZERO;
        }

        public long getTotalSolicitudes() { return totalSolicitudes; }
        public void setTotalSolicitudes(long totalSolicitudes) { this.totalSolicitudes = totalSolicitudes; }

        public long getSolicitudesPendientes() { return solicitudesPendientes; }
        public void setSolicitudesPendientes(long solicitudesPendientes) { this.solicitudesPendientes = solicitudesPendientes; }

        public long getSolicitudesEnProceso() { return solicitudesEnProceso; }
        public void setSolicitudesEnProceso(long solicitudesEnProceso) { this.solicitudesEnProceso = solicitudesEnProceso; }

        public long getSolicitudesResueltas() { return solicitudesResueltas; }
        public void setSolicitudesResueltas(long solicitudesResueltas) { this.solicitudesResueltas = solicitudesResueltas; }

        public long getSolicitudesCanceladas() { return solicitudesCanceladas; }
        public void setSolicitudesCanceladas(long solicitudesCanceladas) { this.solicitudesCanceladas = solicitudesCanceladas; }

        public double getPorcentajeResueltas() { return porcentajeResueltas; }
        public void setPorcentajeResueltas(double porcentajeResueltas) { this.porcentajeResueltas = porcentajeResueltas; }

        public long getTotalClientes() { return totalClientes; }
        public void setTotalClientes(long totalClientes) { this.totalClientes = totalClientes; }

        public long getTotalServicios() { return totalServicios; }
        public void setTotalServicios(long totalServicios) { this.totalServicios = totalServicios; }

        public long getTotalEmpleados() { return totalEmpleados; }
        public void setTotalEmpleados(long totalEmpleados) { this.totalEmpleados = totalEmpleados; }

        public BigDecimal getTotalRecaudadoBreB() { return totalRecaudadoBreB; }
        public void setTotalRecaudadoBreB(BigDecimal totalRecaudadoBreB) { this.totalRecaudadoBreB = totalRecaudadoBreB; }

        public long getTotalPagosConfirmados() { return totalPagosConfirmados; }
        public void setTotalPagosConfirmados(long totalPagosConfirmados) { this.totalPagosConfirmados = totalPagosConfirmados; }

        public BigDecimal getPromedioMontoPago() { return promedioMontoPago; }
        public void setPromedioMontoPago(BigDecimal promedioMontoPago) { this.promedioMontoPago = promedioMontoPago; }
    }

    public static class ConteoItemDTO {
        private String etiqueta;
        private long cantidad;

        public ConteoItemDTO() {}

        public ConteoItemDTO(String etiqueta, long cantidad) {
            this.etiqueta = etiqueta;
            this.cantidad = cantidad;
        }

        public String getEtiqueta() { return etiqueta; }
        public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

        public long getCantidad() { return cantidad; }
        public void setCantidad(long cantidad) { this.cantidad = cantidad; }
    }

    public static class ReporteEmpleadoDTO {
        private Long idEmpleado;
        private String nombreEmpleado;
        private String cargo;
        private String departamento;
        private long totalAsignadas;
        private long pendientes;
        private long enProceso;
        private long resueltas;
        private long canceladas;
        private double porcentajeEfectividad;
        private List<SolicitudDTOs> solicitudes;

        public ReporteEmpleadoDTO() {}

        public Long getIdEmpleado() { return idEmpleado; }
        public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }

        public String getNombreEmpleado() { return nombreEmpleado; }
        public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }

        public String getCargo() { return cargo; }
        public void setCargo(String cargo) { this.cargo = cargo; }

        public String getDepartamento() { return departamento; }
        public void setDepartamento(String departamento) { this.departamento = departamento; }

        public long getTotalAsignadas() { return totalAsignadas; }
        public void setTotalAsignadas(long totalAsignadas) { this.totalAsignadas = totalAsignadas; }

        public long getPendientes() { return pendientes; }
        public void setPendientes(long pendientes) { this.pendientes = pendientes; }

        public long getEnProceso() { return enProceso; }
        public void setEnProceso(long enProceso) { this.enProceso = enProceso; }

        public long getResueltas() { return resueltas; }
        public void setResueltas(long resueltas) { this.resueltas = resueltas; }

        public long getCanceladas() { return canceladas; }
        public void setCanceladas(long canceladas) { this.canceladas = canceladas; }

        public double getPorcentajeEfectividad() { return porcentajeEfectividad; }
        public void setPorcentajeEfectividad(double porcentajeEfectividad) { this.porcentajeEfectividad = porcentajeEfectividad; }

        public List<SolicitudDTOs> getSolicitudes() { return solicitudes; }
        public void setSolicitudes(List<SolicitudDTOs> solicitudes) { this.solicitudes = solicitudes; }
    }
}