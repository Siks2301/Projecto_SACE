# Projecto_SACE — Sistema de Atención al Cliente (AleLeo Tours)

Proyecto académico **SENA** compuesto por dos partes:

| Carpeta | Descripción | Tecnología |
|---|---|---|
| [`backend-SACE/`](backend-SACE/) | API REST del sistema: autenticación, solicitudes (PQR/reservas), mensajes, pagos con comprobante PDF y reportes | Java 17 · Spring Boot 3.2 · JPA/Hibernate · PostgreSQL |
| [`frontend-aleleo-tours/`](frontend-aleleo-tours/) | Sitio estático de la agencia: destinos, registro/login, panel de cliente, panel de empleado/admin y chatbot | HTML · CSS · Bootstrap · JavaScript vanilla |

## Requisitos

- JDK 17
- Maven 3.8+
- PostgreSQL 14+ (base de datos `SACE_db`, usuario `postgres`, contraseña `1234`, puerto `5432`)

## Ejecutar el backend

```powershell
cd backend-SACE
mvn spring-boot:run
```

Queda escuchando en **http://localhost:8082**.

La configuración está en `backend-SACE/src/main/resources/application.properties`.

## Ejecutar el frontend

Es un sitio 100 % estático; basta con servir la carpeta:

```powershell
cd frontend-aleleo-tours
python -m http.server 8090 --bind 127.0.0.1
```

Abrir **http://localhost:8090**. El frontend asume el backend en `localhost:8082`.

## Usuario administrador por defecto

- Email: `admin@aleleotours.com`
- Contraseña: `admin123`

## Funcionalidades

- Registro/login de clientes (apellido opcional) y panel de empleados/administradores.
- Gestión de solicitudes por categorías: RESERVA, CONSULTA, CAMBIO_FECHA, EQUIPAJE, OTRO.
- Chat de mensajes por solicitud y chatbot con catálogo desde la API.
- Pago con llave **Bre-B**, precio tomado del servicio real en BD, anti-doble-cobro y comprobante PDF con sello SHA-256 + IVA 19 %.
- Reportes y KPIs restringidos a administradores.
- Autorización por propiedad (403) en pagos, cancelaciones, mensajes y reportes.

## Estructura

```
Projecto_SACE/
├── backend-SACE/            # API Spring Boot
│   ├── pom.xml
│   └── src/main/java/com/mycompany/sacejpa/
└── frontend-aleleo-tours/   # Sitio estático
    ├── *.html
    ├── css/
    ├── js/
    └── img/
```
