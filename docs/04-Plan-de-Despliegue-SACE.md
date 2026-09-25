# Plan de despliegue

_Proyecto académico AleLeo Tours (SENA) · Ficha / Grupo: 3171149-B_
_Cesar Leonardo Ramírez Montejo · Yerson Alexei Torres Garcia · Felipe Gonzales Quitero_
_21 de septiembre de 2026_

## 4. Plan de despliegue
### 4.1 Arquitectura objetivo
Dos componentes desplegados en el mismo equipo (local) o servidor académico: el backend Spring Boot (API + PostgreSQL en la misma máquina) y el frontend estático servido por un servidor web ligero (python o nginx).

### 4.2 Dependencias y versiones
| Componente | Versión / detalle |
|---|---|
| Java (JDK) | 17 (JAVA_HOME configurado) |
| Maven | 3.8+ (compilación y empaquetado del backend) |
| Spring Boot | 3.2 (artefacto com.mycompany:sacejpa:0.0.1-SNAPSHOT) |
| PostgreSQL | 18+ (base SACE_db en el puerto 5432) |
| Frontend | HTML/CSS/JS puro + Bootstrap 5.3.3 (CDN); sin build |
| Servidor estático | python -m http.server o nginx |
| Puertos | Backend 8082 · Frontend 8091 · PostgreSQL 5432 |

### 4.3 Configuración y variables de entorno
El backend centraliza su configuración en application.properties y admite sobrescritura por variables de entorno (formato ${NOMBRE:valor_por_defecto}), lo que permite cambiar la configuración sin tocar código:

| Variable | Valor por defecto | Uso |
|---|---|---|
| SACE_TOKEN_SECRET | Cl4veDesarrolloSACE_AleLeoTours_2024_CambiarEnProduccion | Firma de tokens (cambiar en producción) |
| PROPIETARIO_EMAIL | propietario@aleleotours.com | Destinatario de notificaciones |
| SPRING_MAIL_HOST / PORT | localhost / 1025 | Servidor SMTP (fail-safe activo) |
| SPRING_MAIL_USERNAME / PASSWORD | notificaciones@aleleotours.com / secret | Credenciales SMTP |
| spring.datasource.url | jdbc:postgresql://localhost:5432/SACE_db | Conexión a PostgreSQL (password 1234 en desarrollo) |
| server.port / app.upload.dir | 8082 / uploads/comprobantes | Puerto de la API y carpeta de comprobantes PDF |

### 4.4 Procedimiento de despliegue local (paso a paso)
Paso 1 — Crear la base de datos vacía (solo la primera vez):

     CREATE DATABASE SACE_db;

Paso 2 — Levantar el backend (las tablas y los datos semilla se crean solos):

     cd backend-SACE

     mvn spring-boot:run      # queda en http://localhost:8082

Paso 3 — Servir el frontend estático:

     cd frontend-aleleo-tours

     python -m http.server 8091 --bind 127.0.0.1   # http://localhost:8091

Paso 4 — Verificación: abrir http://localhost:8091, iniciar sesión con el administrador semilla (admin@aleleotours.com / admin123) y comprobar los módulos (Plan de Pruebas, sección 3.6).

### 4.5 Despliegue en servidor (procedimiento previsto)
1. Empaquetar el backend:  mvn clean package  → genera backend-SACE/target/sacejpa-0.0.1-SNAPSHOT.jar.

2. Definir las variables de entorno de producción (SACE_TOKEN_SECRET fuerte, credenciales de BD, SMTP real).

3. Iniciar el backend:  java -jar sacejpa-0.0.1-SNAPSHOT.jar.

4. Publicar el frontend: copiar frontend-aleleo-tours/ a la carpeta web del servidor (nginx: root + index.html).

5. Configurar el frontend para apuntar al dominio del backend (variable de configuración de la API en js/core/app.js).

6. (Opcional) Registrar el proceso con systemd/PM2 y habilitar HTTPS con certificado.

### 4.6 Verificación post-despliegue
- GET /api/servicios responde JSON (catálogo público).
- Login de administrador y de un cliente recién registrado.
- Flujo completo: crear solicitud → pagar → descargar comprobante PDF.
- Reportes accesibles solo para administradores (403 para otros roles).
- Suite de regresión de humo: CP-02, CP-04, CP-06, CP-08, CP-11 del Plan de Pruebas.
### 4.7 Recomendaciones de producción
- Cambiar SACE_TOKEN_SECRET, la contraseña de PostgreSQL y las credenciales SMTP antes de exponer el sistema.
- Rotar las credenciales del administrador semilla (admin123) tras el primer ingreso.
- Habilitar HTTPS y no publicar los comprobantes PDF fuera de su ruta protegida.
- Mantener el modelo de datos con back-ups regulares (pg_dump).