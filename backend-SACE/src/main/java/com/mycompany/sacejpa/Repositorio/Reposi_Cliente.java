package com.mycompany.sacejpa.Repositorio;

import com.mycompany.sacejpa.Modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Reposi_Cliente extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmail(String email);

    // Busqueda insensible a mayusculas/minusculas: evita que el login falle
    // si el correo se guardo con una capitalizacion distinta a la que el
    // usuario escribe (p. ej. registros creados manualmente en la BD o desde
    // el panel de administracion, que no normalizan el email).
    Optional<Cliente> findByEmailIgnoreCase(String email);

}
