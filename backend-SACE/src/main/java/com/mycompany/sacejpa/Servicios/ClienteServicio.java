package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.ClienteDTOs;
import com.mycompany.sacejpa.Mapper.ClienteMapper;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Seguridad.ContraseniaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClienteServicio {

    @Autowired
    private Reposi_Cliente repositorioCliente;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Metodo que consulta y lista todos los clientes de la base de datos
    public List<ClienteDTOs> listarClientes() {
        return repositorioCliente.findAll().stream()
                .map(ClienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Metodo que busca un cliente por id
    public ClienteDTOs buscarCliente(Long id) {
        Cliente cliente = repositorioCliente.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + id));
        return ClienteMapper.toDTO(cliente);
    }

    // Metodo que inserta un nuevo cliente en la base de datos
    public ClienteDTOs insertarCliente(ClienteDTOs dto) {
        Cliente cliente = ClienteMapper.myEntity(dto);
        // Un cliente es SIEMPRE CLIENTE y nace ACTIVA; nunca se aceptan estos
        // campos desde el cuerpo para impedir autopromocion o autobloqueo.
        cliente.setTipoUsuario(Cliente.TipoUsuario.CLIENTE);
        if (cliente.getEstadoAcceso() == null) {
            cliente.setEstadoAcceso(Cliente.EstadoAcceso.ACTIVA);
        }
        cliente.setEmail(normalizarEmail(cliente.getEmail()));
        // La contrasenia la define el administrador en el formulario de alta.
        // Antes este campo no existia en el DTO, asi que el cliente se guardaba
        // sin hash: la UI pedia la clave y el backend la descartaba en silencio.
        // El cliente quedaba creado pero sin poder iniciar sesion (401). Se
        // valida la fortaleza y se guarda hasheada con BCrypt, igual que en
        // EmpleadoServicio.insertarEmpleado.
        if (dto.getContrasenia() != null && !dto.getContrasenia().isBlank()) {
            ContraseniaUtil.validar(dto.getContrasenia());
            cliente.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        }
        Cliente guardado = repositorioCliente.save(cliente);
        return ClienteMapper.toDTO(guardado);
    }

    // Metodo que permite actualizar un cliente existente
    public ClienteDTOs servactualiza(Long id, ClienteDTOs dto) {
        Optional<Cliente> existe = repositorioCliente.findById(id);
        if (existe.isPresent()) {
            Cliente cliente = existe.get();
            // null-guards: un PUT parcial nunca borra campos ajenos al edit.
            if (dto.getNombre() != null) {
                cliente.setNombre(dto.getNombre());
            }
            if (dto.getApellido() != null) {
                cliente.setApellido(dto.getApellido());
            }
            if (dto.getEmail() != null) {
                cliente.setEmail(normalizarEmail(dto.getEmail()));
            }
            if (dto.getTelefono() != null) {
                cliente.setTelefono(dto.getTelefono());
            }
            // tipoUsuario y estadoAcceso se conservan (los fija el servidor).
            if (dto.getTipoDocumento() != null) {
                cliente.setTipoDocumento(Cliente.TipoDocumento.valueOf(dto.getTipoDocumento()));
            }
            if (dto.getNumeroDocumento() != null) {
                cliente.setNumeroDocumento(dto.getNumeroDocumento());
            }
            if (dto.getHistorialConsultas() != null) {
                cliente.setHistorialConsultas(dto.getHistorialConsultas());
            }
            if (dto.getPreferenciasComunicacion() != null) {
                cliente.setPreferenciasComunicacion(dto.getPreferenciasComunicacion());
            }
            // Reasignacion de clave desde la pantalla de edicion. Si viene
            // vacia se conserva la existente: un PUT parcial nunca debe borrar
            // la contrasena y dejar al cliente sin poder entrar.
            if (dto.getContrasenia() != null && !dto.getContrasenia().isBlank()) {
                ContraseniaUtil.validar(dto.getContrasenia());
                cliente.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
            }
            Cliente actualizado = repositorioCliente.save(cliente);
            return ClienteMapper.toDTO(actualizado);
        } else {
            throw new RuntimeException("Cliente no encontrado con id: " + id);
        }
    }

    // Metodo que elimina un cliente de la base de datos
    public void eliminarCliente(Long id) {
        if (!repositorioCliente.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado con id: " + id);
        }
        repositorioCliente.deleteById(id);
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
