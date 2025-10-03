# Build stage
FROM maven:3.8.6-openjdk-8-slim AS build

WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY src ./src

# Construir la aplicación (saltando tests para build más rápido)
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:8-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR desde el stage de build
COPY --from=build /app/target/*.jar app.jar

# Crear directorio para claves
RUN mkdir -p /app/keys

# Cambiar ownership
RUN chown -R spring:spring /app

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Punto de entrada
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Xms256m", \
    "-Xmx512m", \
    "-jar", \
    "app.jar"]
