package com.mycompany.sacejpa.Controladores;

import com.mycompany.sacejpa.DTOs.ClienteDTOs;
import com.mycompany.sacejpa.DTOs.LoginRequestDTO;
import com.mycompany.sacejpa.DTOs.LoginResponseDTO;
import com.mycompany.sacejpa.DTOs.RegistroClienteDTO;
import com.mycompany.sacejpa.Exceptions.ValidacionException;
import com.mycompany.sacejpa.Seguridad.TokenServicio;
import com.mycompany.sacejpa.Servicios.AuthServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthServicio authServicio;

    @Autowired
    private TokenServicio tokenServicio;

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody RegistroClienteDTO dto) {
        try {
            ClienteDTOs creado = authServicio.registrarCliente(dto);
            // El frontend inicia sesion automaticamente tras registrarse, por eso
            // se devuelve el mismo formato que /login (con token firmado).
            LoginResponseDTO respuesta = new LoginResponseDTO(
                    creado.getId(), creado.getNombre(), creado.getApellido(),
                    creado.getEmail(), creado.getTipoUsuario());
            // El registro auto-inicia sesion igual que /login, asi que la
            // respuesta trae el mismo estado que el login.
            respuesta.setEstadoAcceso(creado.getEstadoAcceso());
            respuesta.setToken(tokenServicio.emitir(
                    creado.getId(), creado.getEmail(), creado.getTipoUsuario()));
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (ValidacionException e) {
            // Regla de negocio incumplida (p. ej. contrasena debil): 400.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        try {
            LoginResponseDTO respuesta = authServicio.login(dto);
            return ResponseEntity.ok(respuesta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}
