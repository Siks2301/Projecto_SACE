package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.PreguntaFrecuenteDTOs;
import com.mycompany.sacejpa.Mapper.PreguntaFrecuenteMapper;
import com.mycompany.sacejpa.Modelo.PreguntaFrecuente;
import com.mycompany.sacejpa.Repositorio.Reposi_PreguntaFrecuente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreguntaFrecuenteServicio {

    @Autowired
    private Reposi_PreguntaFrecuente repositorioPreguntaFrecuente;

    public List<PreguntaFrecuenteDTOs> listarPreguntas() {
        return repositorioPreguntaFrecuente.findAll().stream()
                .map(PreguntaFrecuenteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PreguntaFrecuenteDTOs buscarPregunta(Long id) {
        PreguntaFrecuente pregunta = repositorioPreguntaFrecuente.findById(id)
                .orElseThrow(() -> new RuntimeException("Pregunta frecuente no encontrada con id: " + id));
        return PreguntaFrecuenteMapper.toDTO(pregunta);
    }

    public PreguntaFrecuenteDTOs insertarPregunta(PreguntaFrecuenteDTOs dto) {
        PreguntaFrecuente pregunta = PreguntaFrecuenteMapper.myEntity(dto);
        PreguntaFrecuente guardada = repositorioPreguntaFrecuente.save(pregunta);
        return PreguntaFrecuenteMapper.toDTO(guardada);
    }

    public PreguntaFrecuenteDTOs servactualiza(Long id, PreguntaFrecuenteDTOs dto) {
        Optional<PreguntaFrecuente> existe = repositorioPreguntaFrecuente.findById(id);
        if (existe.isPresent()) {
            PreguntaFrecuente pregunta = existe.get();
            pregunta.setPregunta(dto.getPregunta());
            pregunta.setRespuesta(dto.getRespuesta());
            pregunta.setPalabrasClave(dto.getPalabrasClave());
            pregunta.setCategoria(dto.getCategoria());
            PreguntaFrecuente actualizada = repositorioPreguntaFrecuente.save(pregunta);
            return PreguntaFrecuenteMapper.toDTO(actualizada);
        } else {
            throw new RuntimeException("Pregunta frecuente no encontrada con id: " + id);
        }
    }

    public void eliminarPregunta(Long id) {
        if (!repositorioPreguntaFrecuente.existsById(id)) {
            throw new RuntimeException("Pregunta frecuente no encontrada con id: " + id);
        }
        repositorioPreguntaFrecuente.deleteById(id);
    }
}
