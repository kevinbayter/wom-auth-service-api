# 📬 Colección de Postman - WOM Auth Service API

## 📦 Archivos Incluidos

Este proyecto incluye una colección completa de Postman con manejo automático de tokens y tests integrados:

- **`WOM_Auth_Service_API.postman_collection.json`** - Colección de requests
- **`WOM_Auth_Service_API.postman_environment.json`** - Variables de entorno

---

## ✨ Características

✅ **Manejo automático de tokens**
- Login guarda `access_token` y `refresh_token` automáticamente
- Refresh actualiza tokens dinámicamente con Token Rotation
- No necesitas copiar/pegar tokens manualmente

✅ **Tests automáticos en cada request**
- Validación de status codes
- Validación de estructura de respuesta
- Verificación de Token Rotation
- Logs detallados en consola de Postman

✅ **15 requests organizados**
- 5 endpoints de autenticación
- 2 endpoints de monitoreo
- 3 casos de error

---

## 🚀 Inicio Rápido

### Paso 1: Importar en Postman

1. Abrir **Postman Desktop** o **Postman Web**
2. Click en **Import** (esquina superior izquierda)
3. Arrastrar o seleccionar los 2 archivos JSON:
   - `WOM_Auth_Service_API.postman_collection.json`
   - `WOM_Auth_Service_API.postman_environment.json`
4. Click en **Import**

### Paso 2: Activar el Environment

1. En Postman, click en el dropdown de environments (arriba a la derecha)
2. Seleccionar **"WOM Auth Service - Local"**
3. ✅ Verificar que `base_url` esté en `http://localhost:8080`

### Paso 3: Ejecutar Requests

**Orden recomendado para primera prueba:**

1. **Authentication → 1. Login**
   - Click en el request
   - Click en **Send** (o `Cmd/Ctrl + Enter`)
   - ✅ Los tokens se guardan automáticamente
   - Ver logs en la consola de Postman

2. **Authentication → 3. Get Current User**
   - ✅ Usa el `access_token` guardado automáticamente
   - Muestra información del usuario autenticado

3. **Authentication → 2. Refresh Token**
   - ✅ Usa el `refresh_token` guardado automáticamente
   - ✅ Actualiza ambos tokens (Token Rotation)
   - Ver logs de rotación en consola

4. **Authentication → 4. Logout**
   - Invalida los tokens actuales
   - Para continuar, ejecutar "1. Login" nuevamente

---

## 📁 Estructura de la Colección

### 1. Authentication (5 requests)

| Request | Método | Endpoint | Descripción |
|---------|--------|----------|-------------|
| 1. Login | POST | `/auth/login` | Autentica usuario y guarda tokens |
| 2. Refresh Token | POST | `/auth/refresh` | Renueva tokens (Token Rotation) |
| 3. Get Current User | GET | `/auth/me` | Obtiene perfil del usuario |
| 4. Logout | POST | `/auth/logout` | Cierra sesión (invalida tokens) |
| 5. Logout All Devices | POST | `/auth/logout-all` | Cierra sesión en todos los dispositivos |

### 2. Health & Monitoring (2 requests)

| Request | Método | Endpoint | Descripción |
|---------|--------|----------|-------------|
| Health Check | GET | `/actuator/health` | Verifica estado de la app |
| Prometheus Metrics | GET | `/actuator/prometheus` | Métricas en formato Prometheus |

### 3. Error Cases (3 requests)

| Request | Descripción |
|---------|-------------|
| Login - Invalid Credentials | Login con contraseña incorrecta (401) |
| Get User - No Token | Acceso sin autenticación (401) |
| Refresh - Invalid Token | Refresh con token inválido (401) |

---

## 🔧 Variables de Entorno

El environment **"WOM Auth Service - Local"** incluye:

| Variable | Valor por Defecto | Tipo | Descripción |
|----------|-------------------|------|-------------|
| `base_url` | `http://localhost:8080` | string | URL base de la API |
| `access_token` | (vacío) | secret | Se actualiza automáticamente |
| `refresh_token` | (vacío) | secret | Se actualiza automáticamente |
| `user_email` | `admin@test.com` | string | Email de prueba |
| `user_password` | `password` | secret | Contraseña de prueba |

### Cómo editar variables:

1. Click en el ícono de ojo (👁️) al lado del environment
2. Ver valores actuales
3. Click en "Edit" para modificar
4. Guardar cambios

---

## 🧪 Scripts Automáticos

Cada request incluye **Pre-request Scripts** y **Test Scripts**:

### Ejemplo: Login

**Pre-request Script:**
```javascript
console.log("🔐 Iniciando login...");
```

**Test Script:**
```javascript
// Validar status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Guardar tokens automáticamente
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("access_token", jsonData.accessToken);
    pm.environment.set("refresh_token", jsonData.refreshToken);
    
    console.log("✅ Access Token guardado");
    console.log("✅ Refresh Token guardado");
}
```

### Ejemplo: Refresh Token (Token Rotation)

**Test Script:**
```javascript
// Validar Token Rotation
pm.test("Token Rotation: New refresh token is different", function () {
    var jsonData = pm.response.json();
    var oldRefreshToken = pm.environment.get("refresh_token");
    pm.expect(jsonData.refreshToken).to.not.eql(oldRefreshToken);
    console.log("✅ Token Rotation funcionando");
});

// Actualizar tokens con nuevos valores
pm.environment.set("access_token", jsonData.accessToken);
pm.environment.set("refresh_token", jsonData.refreshToken);
```

---

## 🎯 Ejecutar Toda la Colección

Puedes ejecutar todos los requests automáticamente con **Collection Runner**:

### Paso 1: Abrir Collection Runner

1. Click derecho en la colección **"WOM Auth Service API"**
2. Seleccionar **"Run collection"**

### Paso 2: Configurar Ejecución

1. Seleccionar el environment: **"WOM Auth Service - Local"**
2. Orden de ejecución:
   - ✅ Mantener orden por defecto (Login primero)
   - ⚠️ O desmarcar "Error Cases" para evitar fallos esperados
3. Configurar iteraciones: **1** (recomendado)
4. Delay entre requests: **0ms** (opcional: 100ms para ver logs)

### Paso 3: Ejecutar

1. Click en **"Run WOM Auth Service API"**
2. Ver resultados en tiempo real:
   - ✅ Tests pasados (verde)
   - ❌ Tests fallidos (rojo)
   - 📊 Tiempos de respuesta
3. Ver logs detallados en cada request

### Resultados Esperados

**Happy Path (Authentication):**
- ✅ 1. Login: 5/5 tests passed
- ✅ 2. Refresh Token: 5/5 tests passed
- ✅ 3. Get Current User: 5/5 tests passed
- ✅ 4. Logout: 3/3 tests passed
- ✅ 5. Logout All Devices: 3/3 tests passed

**Health & Monitoring:**
- ✅ Health Check: 4/4 tests passed
- ✅ Prometheus Metrics: 3/3 tests passed

**Error Cases:**
- ✅ Invalid Credentials: 3/3 tests passed (error esperado)
- ✅ No Token: 1/1 tests passed (error esperado)
- ✅ Invalid Token: 2/2 tests passed (error esperado)

---

## 💡 Tips y Trucos

### Ver Logs Detallados

1. Abrir **Postman Console**:
   - Mac: `Cmd + Alt + C`
   - Windows/Linux: `Ctrl + Alt + C`
   - O: View → Show Postman Console

2. Ejecutar cualquier request

3. Ver logs en consola:
   ```
   🔐 Iniciando login...
   ✅ Access Token guardado: eyJhbGciOiJSUzI1NiJ9...
   ✅ Refresh Token guardado: eyJhbGciOiJSUzI1NiJ9...
   ⏱️ Response Time: 245ms
   📊 Status Code: 200
   ```

### Ejecutar Requests Rápido

- **Send**: `Cmd/Ctrl + Enter`
- **New Request**: `Cmd/Ctrl + N`
- **Save Request**: `Cmd/Ctrl + S`
- **Open Console**: `Cmd/Ctrl + Alt + C`

### Usar Variables en Body

Las variables de entorno se pueden usar en cualquier parte:

```json
{
  "identifier": "{{user_email}}",
  "password": "{{user_password}}"
}
```

### Cambiar a Otro Servidor

Si tienes la API en otro servidor (staging, producción):

1. Duplicar environment: Click derecho → Duplicate
2. Renombrar a "WOM Auth Service - Staging"
3. Cambiar `base_url` a `https://staging.example.com`
4. Activar este environment

---

## 🔍 Troubleshooting

### "Error: No access_token"

**Causa:** No has ejecutado "1. Login" primero  
**Solución:** Ejecutar "1. Login" para obtener tokens

### "401 Unauthorized: Token has been revoked"

**Causa:** Token invalidado por logout  
**Solución:** Ejecutar "1. Login" nuevamente

### "401 Unauthorized: Invalid credentials"

**Causa:** Credenciales incorrectas o usuario no existe  
**Solución:** Verificar que la API esté corriendo con usuarios de prueba

### "Connection refused"

**Causa:** API no está corriendo  
**Solución:** 
```bash
docker-compose up -d
curl http://localhost:8080/actuator/health
```

### "Token Rotation test fails"

**Causa:** Refresh token ya fue usado (solo se puede usar 1 vez)  
**Solución:** Ejecutar "1. Login" nuevamente para obtener nuevo refresh token

---

## 📊 Ejemplo de Flujo Completo

```
1. Login
   → Guarda access_token: "eyJ..."
   → Guarda refresh_token: "eyJ..."

2. Get Current User
   → Usa access_token automáticamente
   → Responde: { id: 1, email: "admin@test.com", ... }

3. Refresh Token
   → Usa refresh_token automáticamente
   → Genera nuevos tokens:
     • Nuevo access_token: "abc..."
     • Nuevo refresh_token: "def..." (diferente al anterior)
   → Actualiza variables automáticamente

4. Get Current User (nuevamente)
   → Usa el nuevo access_token automáticamente
   → Responde: { id: 1, email: "admin@test.com", ... }

5. Logout
   → Invalida access_token y refresh_token
   → Variables siguen con valores, pero tokens están en blacklist

6. Get Current User (después de logout)
   → Error: 401 Unauthorized: Token has been revoked

7. Login (nuevamente)
   → Genera nuevos tokens
   → Ciclo se reinicia
```

---

## 🎓 Casos de Uso

### Desarrollo

Usa la colección durante desarrollo para:
- ✅ Probar nuevos endpoints
- ✅ Validar cambios de seguridad
- ✅ Verificar Token Rotation
- ✅ Debuggear errores de autenticación

### Testing Manual

Ejecuta Collection Runner para:
- ✅ Smoke tests después de deploy
- ✅ Validar que todos los endpoints funcionen
- ✅ Verificar health checks

### Documentación

Comparte la colección con:
- ✅ Frontend developers
- ✅ QA testers
- ✅ Nuevos miembros del equipo

---

## 📚 Recursos

- **README Principal**: [README.md](README.md)
- **Documentación Swagger**: http://localhost:8080/swagger-ui/index.html
- **Postman Learning**: https://learning.postman.com/
- **Collection Format**: https://schema.postman.com/

---

## 🤝 Contribuir

Si encuentras algún problema o quieres agregar más tests:

1. Editar la colección en Postman
2. Exportar la colección (Collection → Export → Collection v2.1)
3. Reemplazar `WOM_Auth_Service_API.postman_collection.json`
4. Crear Pull Request

---

**Desarrollado por:** Kevin Bayter  
**Fecha:** Octubre 2025  
**Versión:** 1.0.0
