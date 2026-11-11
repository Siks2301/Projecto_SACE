# =====================================================================
# Imagen de produccion del backend SACE (Spring Boot 3.2 + Java 17)
# ---------------------------------------------------------------------
# CONTEXTO DE COMPILACION: la RAIZ del repositorio (Projecto_SACE/).
# Es deliberado: con la raiz como contexto, la misma imagen sirve para
# Render, Railway, Fly.io, Docker Compose y cualquier servidor con Docker
# sin cambiar una sola linea.
#
# Construir:
#   docker build -t sace-api .
#
# Ejecutar (con el .env de deploy/):
#   docker run -p 8082:8082 --env-file deploy/env-produccion.env sace-api
# =====================================================================


# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copia primero solo el pom.xml para que Maven resuelva las
# dependencias en una capa cacheada: mientras el pom no cambie, la
# descarga no se repite en cada compilacion.
COPY backend-SACE/pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY backend-SACE/src ./src
RUN mvn -B -ntp clean package -DskipTests


# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# fontconfig y una fuente TrueType basica: la generacion de comprobantes
# PDF usa java.awt, que debe correr en modo headless dentro del contenedor.
RUN apk add --no-cache fontconfig ttf-dejavu \
    && addgroup -S sace \
    && adduser -S sace -G sace

COPY --from=build /build/target/sacejpa-*.jar /app/app.jar

# Directorio de comprobantes PDF. En un PaaS hay que montarlo como volumen
# persistente (ver docs/04-Plan-de-Despliegue-SACE.md): el disco del contenedor es efimero y
# los comprobantes se perderian en cada redespliegue.
RUN mkdir -p /app/uploads/comprobantes && chown -R sace:sace /app

USER sace
EXPOSE 8082

# MaxRAMPercentage deja que la JVM tome el limite de memoria que asigna la
# plataforma, en lugar de pedir una cantidad fija que puede no existir.
ENV JAVA_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -XX:MaxRAMPercentage=75.0"

# Health check con la misma ruta que usan Render, Railway y el verificador
# post-despliegue del proyecto. El arranque puede tardar (migraciones de
# Hibernate), por eso el start-period es amplio.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${PORT:-8082}/api/salud" >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
