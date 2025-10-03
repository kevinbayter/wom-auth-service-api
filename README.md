# 🔐 WOM Auth Service API

Servicio de autenticación empresarial con JWT, refresh tokens, rate limiting y arquitectura orientada a servicios (SOA).

## 🚀 Estado del Proyecto

🚧 **En Desarrollo** - Prueba Técnica WOM

## 📋 Índice

- [Características](#características)
- [Stack Tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Inicio Rápido](#inicio-rápido)
- [Endpoints](#endpoints)
- [Documentación](#documentación)
- [Tests](#tests)
- [Monitoreo](#monitoreo)

## ✨ Características

- ✅ Autenticación con JWT (RS256)
- ✅ Refresh tokens con rotación automática
- ✅ Rate limiting para prevenir brute force
- ✅ Manejo de intentos fallidos y bloqueo de cuenta
- ✅ Bcrypt para hashing de contraseñas
- ✅ Preparado para 2FA (arquitectura extensible)
- ✅ Métricas Prometheus
- ✅ Health checks
- ✅ Documentación Swagger/OpenAPI
- ✅ Dockerizado

## 🛠️ Stack Tecnológico

### Backend
- **Java 8**
- **Spring Boot 2.7.18** (última compatible con Java 8)
- **Spring Security** (autenticación y autorización)
- **Spring Data JPA** (persistencia)
- **Spring Data Redis** (gestión de tokens)

### Base de Datos
- **PostgreSQL 14** (datos persistentes)
- **Redis 7** (tokens, rate limiting, blacklist)

### Seguridad
- **JWT (JJWT)** con RS256
- **BCrypt** (Spring Security)
- **Bucket4j** (rate limiting)

### Monitoreo
- **Spring Boot Actuator**
- **Micrometer** (métricas Prometheus)

### Testing
- **JUnit 5**
- **Mockito**
- **Testcontainers** (PostgreSQL + Redis reales)
- **JaCoCo** (cobertura >80%)

### DevOps
- **Docker & Docker Compose**
- **GitHub Actions** (CI/CD)

## 🏗️ Arquitectura

```
┌─────────────────┐
│  Angular Client │
└────────┬────────┘
         │ HTTP/REST
         ▼
┌─────────────────────────────────────┐
│     Auth Controller (REST API)      │
├─────────────────────────────────────┤
│  - POST /api/v1/auth/login          │
│  - POST /api/v1/auth/refresh        │
│  - POST /api/v1/auth/logout         │
│  - GET  /api/v1/auth/me             │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│       Service Layer (SOLID)         │
├─────────────────────────────────────┤
│  - AuthService (orquestación)       │
│  - UserService (lógica de usuario)  │
│  - JwtService  (tokens JWT)         │
│  - TokenService (refresh tokens)    │
└────────┬────────────────────────────┘
         │
         ▼
┌──────────────┐    ┌──────────────┐
│  PostgreSQL  │    │    Redis     │
│   (Usuarios) │    │   (Tokens)   │
└──────────────┘    └──────────────┘
```

## 🚀 Inicio Rápido

### Prerrequisitos

- Java 8+
- Docker & Docker Compose
- Maven 3.6+

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/wom-auth-service-api.git
cd wom-auth-service-api
```

### 2. Configurar Variables de Entorno

El archivo `.env` contiene todas las variables de entorno necesarias.
Ya está preconfigurado para desarrollo local con Docker.

```bash
# Revisar y ajustar si es necesario
cat .env
```

### 3. Levantar Servicios con Docker

```bash
# Solo PostgreSQL y Redis (para desarrollo local)
docker-compose up -d postgres redis

# O levantar todo (incluida la app)
docker-compose up -d
```

### 4. Ejecutar la Aplicación

```bash
# Con Maven
mvn clean spring-boot:run

# O construir JAR y ejecutar
mvn clean package
java -jar target/wom-auth-service-api-1.0.0.jar
```

### 5. Verificar que está Funcionando

- **Aplicación**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/prometheus

## 📡 Endpoints

### Autenticación

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "identifier": "user@example.com",
  "password": "yourpassword"
}
```

### Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### Logout

```http
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### Perfil de Usuario

```http
GET /api/v1/auth/me
Authorization: Bearer your-access-token
```

Ver documentación completa en **Swagger**: http://localhost:8080/swagger-ui.html

## 📚 Documentación

- **API Docs (OpenAPI)**: http://localhost:8080/v3/api-docs
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Plan de Desarrollo**: [PLAN_DESARROLLO.md](PLAN_DESARROLLO.md)
- **Reglas de Código**: [REGLAS.md](REGLAS.md)

## 🧪 Tests

### Ejecutar Tests

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con reporte de cobertura
mvn clean test jacoco:report

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

### Cobertura

- **Objetivo**: >80%
- **Herramienta**: JaCoCo
- El build falla si la cobertura es <80%

## 📊 Monitoreo

### Actuator Endpoints

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Métricas Personalizadas

- `auth_login_success_total` - Login exitoso
- `auth_login_failure_total` - Login fallido
- `auth_login_latency` - Latencia de login (P95, P99)
- `auth_refresh_latency` - Latencia de refresh

### Levantar Prometheus + Grafana

```bash
docker-compose --profile monitoring up -d
```

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## 🔒 Seguridad

- ✅ Contraseñas hasheadas con BCrypt
- ✅ JWT firmados con RS256 (claves asimétricas)
- ✅ Rotación automática de refresh tokens
- ✅ Detección de reutilización de tokens
- ✅ Rate limiting (5 intentos/minuto por IP)
- ✅ Bloqueo de cuenta tras 5 intentos fallidos
- ✅ CORS configurado
- ✅ Validación de todas las entradas

## 🎯 Decisiones de Diseño

### ¿Por qué PostgreSQL + Redis?

- **PostgreSQL**: Datos críticos y duraderos (usuarios, auditoría)
- **Redis**: Datos volátiles y alta velocidad (tokens, rate limiting)

### ¿Por qué RS256 en lugar de HS256?

- **RS256**: Permite verificación con clave pública sin exponer la privada
- **HS256**: Requiere la misma clave para firmar y verificar (riesgo de seguridad)

### ¿Por qué SOLID?

- **Mantenibilidad**: Código fácil de entender y modificar
- **Testeable**: Cada componente se prueba aisladamente
- **Extensible**: Preparado para 2FA sin refactoring masivo

## 🗺️ Roadmap

- [x] Autenticación básica con JWT
- [x] Refresh tokens con rotación
- [x] Rate limiting
- [x] Métricas Prometheus
- [ ] Autenticación de dos factores (2FA)
- [ ] OAuth2 / Social Login
- [ ] Auditoría completa
- [ ] Notificaciones por email

## 👨‍💻 Autor

**Kevin Bayter**  
GitHub: [@kevinbayter](https://github.com/kevinbayter)  

**Desarrollado para**: WOM (Prueba Técnica)  
**Fecha**: Octubre 2025  
**Versión**: 1.0.0

## 📄 Licencia

Este proyecto es parte de una prueba técnica para WOM.

---

⭐️ **Desarrollado con Clean Code, SOLID y mejores prácticas enterprise**
