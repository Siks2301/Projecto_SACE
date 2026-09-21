package com.mycompany.sacejpa.Seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Capa de autenticacion/autorizacion para el API REST.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "usuarioAutenticado";
    private static final String ROL_ADMIN = "ADMINISTRADOR";
    private static final String ROL_CLIENTE = "CLIENTE";

    @Autowired
    private TokenServicio tokenServicio;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String metodo = request.getMethod();
        String path = normalizar(request.getRequestURI());

        if (esPublico(metodo, path)) {
            return true;
        }

        TokenServicio.Sesion sesion = autenticar(request);
        if (sesion == null) {
            rechazar(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "No autenticado. Vuelve a iniciar sesion para continuar.", request.getRequestURI());
            return false;
        }
        if (!autorizado(metodo, path, sesion)) {
            rechazar(response, HttpServletResponse.SC_FORBIDDEN,
                    "No tienes permisos para realizar esta operacion.", request.getRequestURI());
            return false;
        }

        request.setAttribute(ATRIBUTO_SESION, sesion);
        return true;
    }

    private TokenServicio.Sesion autenticar(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return tokenServicio.validar(token);
    }

    private boolean esPublico(String metodo, String path) {
        if ("OPTIONS".equalsIgnoreCase(metodo)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(metodo)) {
            return "/api/auth/login".equals(path) || "/api/auth/registro".equals(path);
        }
        if ("GET".equalsIgnoreCase(metodo)) {
            return "/api/servicios".equals(path)
                    || "/api/preguntas-frecuentes".equals(path)
                    || "/api/chatbots".equals(path);
        }
        return false;
    }

    private boolean autorizado(String metodo, String path, TokenServicio.Sesion sesion) {
        String tipo = sesion.tipoUsuario();
        boolean admin = ROL_ADMIN.equalsIgnoreCase(tipo);

        // Pagos: accesible para cualquier usuario autenticado (la validacion de propiedad se hace en controlador/servicio)
        if (matchea(path, "/api/pagos")) {
            return true;
        }

        // Reportes administrativos: exclusivo para admin
        if (matchea(path, "/api/reportes/admin") || matchea(path, "/api/reportes/kpis")
                || matchea(path, "/api/reportes/estados") || matchea(path, "/api/reportes/categorias")
                || matchea(path, "/api/reportes/tendencia")) {
            return admin;
        }

        // Reportes de empleado: admin o propio
        if (matchea(path, "/api/reportes/empleado")) {
            return true;
        }

        if (matchea(path, "/api/empleados")) {
            Long id = idDeSegmento(path, "/api/empleados");
            if ("PUT".equalsIgnoreCase(metodo) && id != null) {
                return admin || id.equals(sesion.id());
            }
            return admin;
        }

        if (matchea(path, "/api/clientes")) {
            if ("GET".equalsIgnoreCase(metodo)) {
                Long id = idDeSegmento(path, "/api/clientes");
                return admin || (id != null && id.equals(sesion.id()));
            }
            return admin;
        }

        if (matchea(path, "/api/servicios")
                || matchea(path, "/api/preguntas-frecuentes")
                || matchea(path, "/api/chatbots")) {
            return "GET".equalsIgnoreCase(metodo) || admin;
        }

        if (matchea(path, "/api/solicitudes")) {
            if ("PUT".equalsIgnoreCase(metodo)) {
                return idDeSegmento(path, "/api/solicitudes") != null;
            }
            if ("DELETE".equalsIgnoreCase(metodo)) {
                return !ROL_CLIENTE.equalsIgnoreCase(tipo);
            }
            return true;
        }

        if (matchea(path, "/api/mensajes") && "DELETE".equalsIgnoreCase(metodo)) {
            return !ROL_CLIENTE.equalsIgnoreCase(tipo);
        }

        return true;
    }

    private String normalizar(String path) {
        if (path == null) {
            return "";
        }
        String p = path;
        while (!p.isEmpty() && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? "/" : p;
    }

    private boolean matchea(String path, String prefijo) {
        return prefijo.equals(path) || path.startsWith(prefijo + "/");
    }

    private Long idDeSegmento(String path, String prefijo) {
        String resto = path.substring(prefijo.length());
        if (resto.isEmpty() || !resto.startsWith("/")) {
            return null;
        }
        try {
            return Long.parseLong(resto.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void rechazar(HttpServletResponse response, int status, String mensaje, String ruta) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + escaparJson(mensaje) + "\"}");
        response.flushBuffer();
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }
}
