package com.mycompany.sacejpa.DTOs;

import java.math.BigDecimal;
import java.util.List;

public class ServicioDTOs {
    private Long id;
    private String nombre;
    private String descripcion;
    private String tipoServicio;
    private BigDecimal precio;
    private String duracion;
    private String destino;
    private List<Long> clientesIds;
    private List<Long> solicitudesQueLoGeneraronIds;

    public ServicioDTOs() {
    }

    public ServicioDTOs(Long id, String nombre, String descripcion, String tipoServicio, BigDecimal precio,
            String duracion, String destino, List<Long> clientesIds, List<Long> solicitudesQueLoGeneraronIds) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoServicio = tipoServicio;
        this.precio = precio;
        this.duracion = duracion;
        this.destino = destino;
        this.clientesIds = clientesIds;
        this.solicitudesQueLoGeneraronIds = solicitudesQueLoGeneraronIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoServicio() {
        return tipoServicio;
    }

    public void setTipoServicio(String tipoServicio) {
        this.tipoServicio = tipoServicio;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public List<Long> getClientesIds() {
        return clientesIds;
    }

    public void setClientesIds(List<Long> clientesIds) {
        this.clientesIds = clientesIds;
    }

    public List<Long> getSolicitudesQueLoGeneraronIds() {
        return solicitudesQueLoGeneraronIds;
    }

    public void setSolicitudesQueLoGeneraronIds(List<Long> solicitudesQueLoGeneraronIds) {
        this.solicitudesQueLoGeneraronIds = solicitudesQueLoGeneraronIds;
    }

    @Override
    public String toString() {
        return "ServicioDTOs{" + "id=" + id + ", nombre='" + nombre + '\'' + ", precio=" + precio + "}";
    }
}
