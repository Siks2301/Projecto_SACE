package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.EmpleadoDTOs;
import com.mycompany.sacejpa.Mapper.EmpleadoMapper;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Repositorio.Reposi_Empleado;
import com.mycompany.sacejpa.Seguridad.ContraseniaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmpleadoServicio {

    @Autowired
    private Reposi_Empleado repositorioEmpleado;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<EmpleadoDTOs> listarEmpleados() {
        return repositorioEmpleado.findAll().stream()
                .map(EmpleadoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public EmpleadoDTOs buscarEmpleado(Long id) {
        Empleado empleado = repositorioEmpleado.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));
        return EmpleadoMapper.toDTO(empleado);
    }

    public EmpleadoDTOs insertarEmpleado(EmpleadoDTOs dto) {
        Empleado empleado = EmpleadoMapper.myEntity(dto);
        // Los campos de privilegio se fijan SIEMPRE en el servidor, nunca desde
        // el cuerpo de la peticion, para impedir que un usuario cree cuentas
        // admin o cuentas activas por su cuenta. El rol solo podria asignarse
        // mediante una ruta dedicada protegida (endpoint futuro) o en BD.
        if (empleado.getTipoUsuario() == null) {
            empleado.setTipoUsuario(Empleado.TipoUsuario.ASESOR_VIAJES);
        }
        if (empleado.getEstadoAcceso() == null) {
            empleado.setEstadoAcceso(Empleado.EstadoAcceso.ACTIVA);
        }
        // Si el administrador definió una contraseña de acceso, se hashea.
        // Si no, el empleado queda registrado pero sin poder iniciar sesión
        // hasta que se le asigne una (misma logica que servactualiza).
        if (dto.getContrasenia() != null && !dto.getContrasenia().isBlank()) {
            ContraseniaUtil.validar(dto.getContrasenia());
            empleado.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        }
        Empleado guardado = repositorioEmpleado.save(empleado);
        return EmpleadoMapper.toDTO(guardado);
    }

    public EmpleadoDTOs servactualiza(Long id, EmpleadoDTOs dto) {
        Optional<Empleado> existe = repositorioEmpleado.findById(id);
        if (existe.isPresent()) {
            Empleado empleado = existe.get();
            // null-guards: un PUT parcial (p. ej. solo disponibilidad) NO debe
            // borrar email/telefono/documento al venir vacios. Antes este codigo
            // nulificaba el correo del empleado y le impedia volver a loguearse.
            if (dto.getNombre() != null) {
                empleado.setNombre(dto.getNombre());
            }
            if (dto.getApellido() != null) {
                empleado.setApellido(dto.getApellido());
            }
            if (dto.getEmail() != null) {
                empleado.setEmail(dto.getEmail().trim().toLowerCase());
            }
            if (dto.getTelefono() != null) {
                empleado.setTelefono(dto.getTelefono());
            }
            // tipoUsuario y estadoAcceso NO se leen del cuerpo (ver insertarEmpleado).
            if (dto.getTipoDocumento() != null) {
                empleado.setTipoDocumento(Empleado.TipoDocumento.valueOf(dto.getTipoDocumento()));
            }
            if (dto.getNumeroDocumento() != null) {
                empleado.setNumeroDocumento(dto.getNumeroDocumento());
            }
            if (dto.getDepartamento() != null) {
                empleado.setDepartamento(dto.getDepartamento());
            }
            if (dto.getCargoEspecifico() != null) {
                empleado.setCargoEspecifico(dto.getCargoEspecifico());
            }
            if (dto.getEstadoDisponibilidad() != null) {
                empleado.setEstadoDisponibilidad(Empleado.EstadoDisponibilidad.valueOf(dto.getEstadoDisponibilidad()));
            }
            // Contraseña opcional: si se envía en blanco/null se conserva la
            // que ya tenía (para no borrarla al editar solo otros campos).
            if (dto.getContrasenia() != null && !dto.getContrasenia().isBlank()) {
                ContraseniaUtil.validar(dto.getContrasenia());
                empleado.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
            }
            Empleado actualizado = repositorioEmpleado.save(empleado);
            return EmpleadoMapper.toDTO(actualizado);
        } else {
            throw new RuntimeException("Empleado no encontrado con id: " + id);
        }
    }

    public void eliminarEmpleado(Long id) {
        if (!repositorioEmpleado.existsById(id)) {
            throw new RuntimeException("Empleado no encontrado con id: " + id);
        }
        repositorioEmpleado.deleteById(id);
    }
}
