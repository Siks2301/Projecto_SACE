package com.mycompany.sacejpa.Servicios;

import com.mycompany.sacejpa.DTOs.ClienteDTOs;
import com.mycompany.sacejpa.DTOs.LoginRequestDTO;
import com.mycompany.sacejpa.DTOs.LoginResponseDTO;
import com.mycompany.sacejpa.DTOs.RegistroClienteDTO;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Mapper.ClienteMapper;
import com.mycompany.sacejpa.Modelo.Cliente;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Modelo.Persona;
import com.mycompany.sacejpa.Repositorio.Reposi_Cliente;
import com.mycompany.sacejpa.Repositorio.Reposi_Empleado;
import com.mycompany.sacejpa.Seguridad.ContraseniaUtil;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServicio {

    @Autowired
    private Reposi_Cliente repositorioCliente;

    @Autowired
    private Reposi_Empleado repositorioEmpleado;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenServicio tokenServicio;

    // Registra un nuevo cliente. Lanza RuntimeException si el email ya existe.
    // El correo se normaliza (trim + minusculas) antes de guardar y de
    // consultar, para que login y registro siempre comparen el mismo valor
    // sin importar como haya escrito el correo el usuario o el administrador.
    public ClienteDTOs registrarCliente(RegistroClienteDTO dto) {
        // Validacion servidor: un POST directo no puede saltarse las reglas que
        // el formulario ya muestra en el cliente.
        // El apellido es opcional: quien se registra con un solo nombre no debe
        // terminar con el nombre duplicado como apellido en el frontend.
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new ValidacionException("El nombre es obligatorio.");
        }
        String emailNormalizado = normalizarEmail(dto.getEmail());
        if (emailNormalizado == null || emailNormalizado.isBlank()) {
            throw new ValidacionException("El correo electronico es obligatorio.");
        }
        ContraseniaUtil.validar(dto.getContrasenia());

        if (repositorioCliente.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta registrada con ese correo.");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setApellido(dto.getApellido() == null ? "" : dto.getApellido().trim());
        cliente.setEmail(emailNormalizado);
        cliente.setTelefono(dto.getTelefono());
        cliente.setTipoUsuario(Cliente.TipoUsuario.CLIENTE);
        cliente.setEstadoAcceso(Cliente.EstadoAcceso.ACTIVA);
        cliente.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));

        Cliente guardado = repositorioCliente.save(cliente);
        return ClienteMapper.toDTO(guardado);
    }

    // Verifica credenciales. Busca primero en Cliente y, si no encuentra el
    // correo ahi, en Empleado (un mismo correo no deberia repetirse entre las
    // dos tablas). Lanza RuntimeException si el email no existe en ninguna,
    // la contrasenia no coincide, o la cuenta no esta activa.
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String emailNormalizado = normalizarEmail(dto.getEmail());
        // Credenciales vacias (o un POST sin el cuerpo): se trata igual que un
        // intento fallido y respondemos 401 generico. Sin esto, contrasenia
        // null reventaria dentro de BCrypt con un 400 enganoso.
        if (emailNormalizado == null || emailNormalizado.isBlank()
                || dto.getContrasenia() == null || dto.getContrasenia().isBlank()) {
            throw new RuntimeException("Correo o contrasenia incorrectos.");
        }

        Cliente cliente = repositorioCliente.findByEmailIgnoreCase(emailNormalizado).orElse(null);
        if (cliente != null) {
            return validarYConstruir(cliente.getContrasenia(), dto.getContrasenia(),
                    cliente.getEstadoAcceso(), null, cliente.getId(), cliente.getNombre(),
                    cliente.getApellido(), cliente.getEmail(),
                    cliente.getTipoUsuario() != null ? cliente.getTipoUsuario().name() : null);
        }

        Empleado empleado = repositorioEmpleado.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new RuntimeException("Correo o contrasenia incorrectos."));

        if (empleado.getContrasenia() == null) {
            // Empleado registrado sin contraseña asignada aun por un administrador.
            throw new RuntimeException("Esta cuenta todavia no tiene una contrasenia de acceso asignada.");
        }

        return validarYConstruir(empleado.getContrasenia(), dto.getContrasenia(),
                empleado.getEstadoAcceso(), empleado.getEstadoDisponibilidad(),
                empleado.getId(), empleado.getNombre(), empleado.getApellido(), empleado.getEmail(),
                empleado.getTipoUsuario() != null ? empleado.getTipoUsuario().name() : null);
    }

    private LoginResponseDTO validarYConstruir(String hashGuardado, String contraseniaIngresada,
            Persona.EstadoAcceso estadoAcceso, Empleado.EstadoDisponibilidad estadoDisponibilidad,
            Long id, String nombre, String apellido, String email, String tipoUsuario) {
        // Primero la contrasena y luego el estado: no se delata si la cuenta
        // existe o si esta bloqueada cuando la contrasena es incorrecta.
        if (!passwordEncoder.matches(contraseniaIngresada, hashGuardado)) {
            throw new RuntimeException("Correo o contrasenia incorrectos.");
        }
        if (estadoAcceso != Persona.EstadoAcceso.ACTIVA) {
            throw new RuntimeException("Tu cuenta esta inactiva o bloqueada. Contacta al administrador.");
        }
        LoginResponseDTO respuesta = new LoginResponseDTO(id, nombre, apellido, email, tipoUsuario);
        respuesta.setEstadoAcceso(estadoAcceso != null ? estadoAcceso.name() : null);
        respuesta.setEstadoDisponibilidad(estadoDisponibilidad != null ? estadoDisponibilidad.name() : null);
        respuesta.setToken(tokenServicio.emitir(id, email, tipoUsuario));
        return respuesta;
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
