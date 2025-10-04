## 📋 Descripción

### Resumen
Esta PR implementa la documentación completa de la API con OpenAPI/Swagger, agregando anotaciones descriptivas a todos los endpoints y DTOs para facilitar la integración y pruebas de la API.

### Cambios Realizados

#### 🆕 Nuevos Archivos
- `OpenApiConfig.java` - Configuración de OpenAPI/Swagger con información del proyecto y esquema de seguridad Bearer JWT

#### ✏️ Modificaciones
- `AuthController.java` - Agregadas anotaciones @Operation, @ApiResponses, @Tag para documentar endpoints
- `LoginRequest.java` - Agregadas anotaciones @Schema para documentar campos
- `LoginResponse.java` - Agregadas anotaciones @Schema para documentar respuesta de autenticación
- `RefreshTokenRequest.java` - Agregadas anotaciones @Schema para documentar request de refresh
- `UserResponse.java` - Agregadas anotaciones @Schema para documentar respuesta de perfil de usuario

### 🎯 Funcionalidad Implementada

#### Documentación de Endpoints
- **POST /auth/login** - Autenticación de usuario con email/username y password
- **POST /auth/refresh** - Renovación de access token con refresh token
- **POST /auth/logout** - Cierre de sesión (blacklist de token)
- **POST /auth/logout-all** - Cierre de sesión en todos los dispositivos
- **GET /auth/me** - Obtener perfil del usuario autenticado

#### Características de Swagger UI
- ✅ Descripción detallada de cada endpoint
- ✅ Ejemplos de request/response para cada DTO
- ✅ Códigos HTTP documentados (200, 401, 403, 429)
- ✅ Esquema de seguridad Bearer JWT configurado
- ✅ Información del proyecto (versión, contacto, licencia)
- ✅ Servidores configurados (Development y Production)

### 🧪 Pruebas

#### Manual Testing
- [x] Swagger UI accesible en `http://localhost:8080/swagger-ui.html`
- [x] Todos los endpoints documentados correctamente
- [x] Esquemas de DTOs visibles y completos
- [x] Botón "Authorize" funcional para JWT
- [x] Ejemplos de request muestran datos válidos

#### Compilación
- [x] `./mvnw clean compile` - SUCCESS
- [x] 28 archivos Java compilados sin errores
- [x] Aplicación inicia sin problemas

### 📸 Capturas de Pantalla

**Swagger UI - Vista General**
![Swagger UI](docs/screenshots/swagger-ui-overview.png)

**Swagger UI - Endpoint Detail**
![Endpoint Documentation](docs/screenshots/swagger-endpoint-detail.png)

### 📚 Documentación

- La documentación de la API ahora está disponible en `/swagger-ui.html`
- OpenAPI spec JSON disponible en `/v3/api-docs`
- Todos los endpoints incluyen:
  - Descripción clara del propósito
  - Parámetros de entrada con validaciones
  - Posibles respuestas con códigos HTTP
  - Esquemas de datos detallados

### ✅ Checklist

- [x] Código compila sin errores
- [x] Aplicación inicia correctamente
- [x] Swagger UI accesible y funcional
- [x] Todos los endpoints documentados
- [x] DTOs con @Schema completos
- [x] Ejemplos de datos incluidos
- [x] Esquema de seguridad JWT configurado
- [x] Commit message sigue conventional commits
- [x] Sin archivos innecesarios en el commit

### 🔗 Referencias

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [FASE 7: Documentación Swagger](../PLAN_DESARROLLO.md#fase-7-documentación-con-swagger)

### 📝 Notas Adicionales

Esta PR cumple con la **FASE 7** del plan de desarrollo. La documentación completa de la API permite:

1. **Facilitar integración**: Desarrolladores frontend pueden ver exactamente qué endpoints están disponibles
2. **Testing rápido**: Se pueden probar los endpoints directamente desde Swagger UI
3. **Contratos claros**: Los esquemas de request/response están bien definidos
4. **Onboarding**: Nuevos desarrolladores pueden entender la API rápidamente

### 🚀 Próximos Pasos

Una vez aprobada esta PR, el siguiente paso será:
- **FASE 2: CI/CD Pipeline** - Configurar GitHub Actions para build y tests automáticos
