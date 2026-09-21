package com.mycompany.sacejpa.Config;

import com.mycompany.sacejpa.Modelo.Chatbot;
import com.mycompany.sacejpa.Modelo.Empleado;
import com.mycompany.sacejpa.Modelo.Persona;
import com.mycompany.sacejpa.Modelo.PreguntaFrecuente;
import com.mycompany.sacejpa.Modelo.Servicio;
import com.mycompany.sacejpa.Repositorio.Reposi_Chatbot;
import com.mycompany.sacejpa.Repositorio.Reposi_Empleado;
import com.mycompany.sacejpa.Repositorio.Reposi_PreguntaFrecuente;
import com.mycompany.sacejpa.Repositorio.Reposi_Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Inicializador de datos (Seed).
 * Si la base de datos PostgreSQL esta vacia o sin administrador, inserta:
 * 1. Un usuario Administrador (admin@aleleotours.com / admin123).
 * 2. Los paquetes y servicios turisticos base (Cartagena, San Andres, Santa Marta, Medellin, Tayrona, Providencia).
 * 3. Preguntas frecuentes para el Chatbot.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private Reposi_Empleado repositorioEmpleado;

    @Autowired
    private Reposi_Servicio repositorioServicio;

    @Autowired
    private Reposi_PreguntaFrecuente repositorioPreguntaFrecuente;

    @Autowired
    private Reposi_Chatbot repositorioChatbot;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        inicializarAdmin();
        inicializarServicios();
        inicializarPreguntasFrecuentesYChatbot();
    }

    private void inicializarAdmin() {
        String emailAdmin = "admin@aleleotours.com";
        if (repositorioEmpleado.findByEmailIgnoreCase(emailAdmin).isEmpty()) {
            Empleado admin = new Empleado();
            admin.setNombre("Administrador");
            admin.setApellido("General");
            admin.setEmail(emailAdmin);
            admin.setTelefono("3001234567");
            admin.setTipoUsuario(Persona.TipoUsuario.ADMINISTRADOR);
            admin.setEstadoAcceso(Persona.EstadoAcceso.ACTIVA);
            admin.setTipoDocumento(Persona.TipoDocumento.CC);
            admin.setNumeroDocumento("100000001");
            admin.setContrasenia(passwordEncoder.encode("admin123"));
            admin.setDepartamento("Tecnologia y Operaciones");
            admin.setCargoEspecifico("Administrador de Sistema");
            admin.setEstadoDisponibilidad(Empleado.EstadoDisponibilidad.DISPONIBLE);

            repositorioEmpleado.save(admin);
            System.out.println(">>> [SACE SEED] Usuario Administrador creado: " + emailAdmin + " / admin123");
        }
    }

    private void inicializarServicios() {
        if (repositorioServicio.count() == 0) {
            List<Servicio> serviciosIniciales = Arrays.asList(
                new Servicio(
                    "Paquete Vacacional Cartagena",
                    "Ciudad amurallada llena de historia, colores y playas del Caribe.",
                    Servicio.TipoServicio.PAQUETE_TODO_INCLUIDO,
                    BigDecimal.valueOf(320000),
                    "3 dias / 2 noches",
                    "Cartagena"
                ),
                new Servicio(
                    "Vuelo + Hotel San Andres",
                    "El famoso mar de siete colores te espera en esta isla tropical paradisiaca.",
                    Servicio.TipoServicio.VUELO_HOTEL,
                    BigDecimal.valueOf(480000),
                    "4 dias / 3 noches",
                    "San Andres"
                ),
                new Servicio(
                    "Experiencia Santa Marta y Playas",
                    "Naturaleza, Parque Tayrona y playas virgenes en una sola ciudad caribena.",
                    Servicio.TipoServicio.TOUR_EXCURSION,
                    BigDecimal.valueOf(270000),
                    "3 dias / 2 noches",
                    "Santa Marta"
                ),
                new Servicio(
                    "Tour Medellin Innovadora y Cafe",
                    "La ciudad de la eterna primavera, innovacion, cultura y gastronomia paisa.",
                    Servicio.TipoServicio.TOUR_EXCURSION,
                    BigDecimal.valueOf(210000),
                    "3 dias / 2 noches",
                    "Medellin"
                ),
                new Servicio(
                    "Ecoturismo Parque Tayrona",
                    "Parque natural unico donde la selva tropical se encuentra con el mar Caribe.",
                    Servicio.TipoServicio.PASADIA,
                    BigDecimal.valueOf(195000),
                    "2 dias / 1 noche",
                    "Tayrona"
                ),
                new Servicio(
                    "Paraiso Providencia Isla Virgen",
                    "La isla mas pura del Caribe colombiano, reserva mundial de la biosfera.",
                    Servicio.TipoServicio.PAQUETE_TODO_INCLUIDO,
                    BigDecimal.valueOf(560000),
                    "5 dias / 4 noches",
                    "Providencia"
                )
            );

            repositorioServicio.saveAll(serviciosIniciales);
            System.out.println(">>> [SACE SEED] 6 Servicios turisticos iniciales creados.");
        }
    }

    private void inicializarPreguntasFrecuentesYChatbot() {
        if (repositorioPreguntaFrecuente.count() == 0) {
            List<PreguntaFrecuente> faqs = Arrays.asList(
                new PreguntaFrecuente(
                    "Que incluye un paquete Todo Incluido?",
                    "Incluye tiquetes aereos, alojamiento en hoteles afiliados, alimentacion completa (desayuno, almuerzo, cena), bebidas ilimitadas y traslados aeropuerto-hotel.",
                    "todo incluido, paquete, alimentacion, hotel, comida",
                    "PAQUETES"
                ),
                new PreguntaFrecuente(
                    "Como puedo reservar un viaje?",
                    "Puedes ingresar a la seccion 'Destinos', elegir tu destino preferido, seleccionar la fecha y numero de pasajeros, y hacer clic en 'Confirmar Reserva'.",
                    "reservar, reserva, comprar, vuelo, viaje, ticket",
                    "RESERVAS"
                ),
                new PreguntaFrecuente(
                    "Que documentos necesito para viajar?",
                    "Para vuelos nacionales en Colombia necesitas tu documento de identidad original (Cedula de Ciudadania, Tarjeta de Identidad para menores o Cedula de Extranjeria / Pasaporte).",
                    "documentos, cedula, pasaporte, identificacion, menores",
                    "DOCUMENTOS"
                ),
                new PreguntaFrecuente(
                    "Cuales son las politicas de cancelacion?",
                    "Puedes solicitar cambios o cancelaciones hasta 48 horas antes del vuelo comunicandote con un asesor o mediante el panel de solicitudes.",
                    "cancelar, reprogramar, reembolso, politicas, cambio fecha",
                    "POLITICAS"
                )
            );

            List<PreguntaFrecuente> guardadas = repositorioPreguntaFrecuente.saveAll(faqs);
            System.out.println(">>> [SACE SEED] Preguntas frecuentes iniciales creadas.");

            if (repositorioChatbot.count() == 0) {
                Chatbot bot = new Chatbot();
                bot.setNombre("AleLeoBot");
                bot.setVersion("1.0.0");
                bot.setEstadoOperativo(Chatbot.EstadoOperativo.ACTIVO);
                bot.setPreguntasFrecuentesUsadas(guardadas);
                repositorioChatbot.save(bot);
                System.out.println(">>> [SACE SEED] Chatbot 'AleLeoBot' inicializado.");
            }
        }
    }
}