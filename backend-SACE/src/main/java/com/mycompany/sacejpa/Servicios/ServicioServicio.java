package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.ServicioDTOs;
import com.mycompany.sacejpa.Mapper.ServicioMapper;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Repositorio.Reposi_Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Nota: la clase se llama "ServicioServicio" (servicio de negocio de la
// entidad Servicio) para evitar el choque de nombres con la anotacion
// @Service y con la propia entidad Servicio del Modelo.
@Service
public class ServicioServicio {

    @Autowired
    private Reposi_Servicio repositorioServicio;

    public List<ServicioDTOs> listarServicios() {
        return repositorioServicio.findAll().stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ServicioDTOs buscarServicio(Long id) {
        Servicio servicio = repositorioServicio.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
        return ServicioMapper.toDTO(servicio);
    }

    public ServicioDTOs insertarServicio(ServicioDTOs dto) {
        Servicio servicio = ServicioMapper.myEntity(dto);
        Servicio guardado = repositorioServicio.save(servicio);
        return ServicioMapper.toDTO(guardado);
    }

    public ServicioDTOs servactualiza(Long id, ServicioDTOs dto) {
        Optional<Servicio> existe = repositorioServicio.findById(id);
        if (existe.isPresent()) {
            Servicio servicio = existe.get();
            // null-guards: un PUT parcial nunca debe pisar campos no enviados.
            // Antes, omitir "precio" lo sobreescribia con 0.0 (perdida de datos).
            if (dto.getNombre() != null) {
                servicio.setNombre(dto.getNombre());
            }
            if (dto.getDescripcion() != null) {
                servicio.setDescripcion(dto.getDescripcion());
            }
            if (dto.getTipoServicio() != null) {
                servicio.setTipoServicio(Servicio.TipoServicio.valueOf(dto.getTipoServicio()));
            }
            if (dto.getPrecio() != null) {
                servicio.setPrecio(dto.getPrecio());
            }
            if (dto.getDuracion() != null) {
                servicio.setDuracion(dto.getDuracion());
            }
            if (dto.getDestino() != null) {
                servicio.setDestino(dto.getDestino());
            }
            Servicio actualizado = repositorioServicio.save(servicio);
            return ServicioMapper.toDTO(actualizado);
        } else {
            throw new RuntimeException("Servicio no encontrado con id: " + id);
        }
    }

    public void eliminarServicio(Long id) {
        if (!repositorioServicio.existsById(id)) {
            throw new RuntimeException("Servicio no encontrado con id: " + id);
        }
        repositorioServicio.deleteById(id);
    }
}
