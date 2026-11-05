package com.mycompany.sacejpa.Config;

import com.mycompany.sacejpa.Seguridad.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion web de SACE: CORS, interceptores y el bean de cifrado de claves.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Rutas que la pasarela de pagos debe poder llamar sin token de SACE.
     *
     * <p>Wompi no tiene cuenta de usuario en SACE: solo puede hacer un POST a una
     * URL publica. La seguridad de estos endpoints no se apoya en la sesion sino
     * en la verificacion criptografica del evento, que ocurre dentro del propio
     * controlador (ver WebhookPagoController).
     *
     * <p>La excepcion es deliberadamente lo mas estrecha posible: solo la ruta
     * del webhook, no todo {@code /api/pagos}. Una excepcion amplia seria un
     * agujero disfrazado de funcionalidad.
     */
    private static final String[] RUTAS_PUBLICAS = {
            "/api/pagos/webhook/**"
    };

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * Origenes permitidos para el navegador, separados por coma.
     *
     * <p>Se leen de {@code app.cors.origins} (variable de entorno
     * {@code SACE_CORS_ORIGINS}) porque en produccion el frontend se
     * publica en un dominio propio o en Cloudflare Pages, no en
     * {@code localhost}. Fijar los origenes en el codigo obligaria a
     * recompilar y redesplegar la API cada vez que cambia la direccion
     * del sitio.
     *
     * <p>El valor por defecto cubre el desarrollo local.
     */
    @Value("${app.cors.origins:http://localhost:*,http://127.0.0.1:*}")
    private String corsOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(RUTAS_PUBLICAS);
    }

    /**
     * CORS para el frontend "AleLeo Tours".
     *
     * <p>Se usan patrones en lugar de {@code allowedOrigins} porque el puerto del
     * frontend cambia segun como se levante (8080 en produccion, otro en
     * desarrollo). Se restringe a localhost: abrirlo a cualquier origen permitiria
     * que una pagina ajena llamara a la API con la sesion del usuario.
     *
     * <p>En produccion se sustituye la lista por los dominios reales mediante la
     * variable {@code SACE_CORS_ORIGINS}; el patron {@code *} sigue siendo valido
     * como ultimo recurso, pero deja de ser seguro.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(dividirOrigenes(corsOrigins))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Convierte la lista de origenes separada por comas en un arreglo, ignorando
     * los espacios sobrantes y las entradas vacias.
     */
    private String[] dividirOrigenes(String lista) {
        if (lista == null || lista.trim().isEmpty()) {
            return new String[]{"http://localhost:*", "http://127.0.0.1:*"};
        }
        return java.util.Arrays.stream(lista.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}