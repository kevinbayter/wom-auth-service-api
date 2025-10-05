# 📊 Sistema de Auditoría - Guía de Integración para Frontend

## 🎯 Descripción General

El backend ahora implementa un **sistema de auditoría completo** que registra automáticamente todos los eventos de autenticación. Esta guía explica cómo el frontend debe interactuar con el sistema y qué información es capturada.

---

## 🔍 ¿Qué se Audita Automáticamente?

Todos los eventos de autenticación son registrados **automáticamente** en el backend sin necesidad de enviar información adicional desde el frontend:

| Evento | Cuándo se Registra | Información Capturada |
|--------|-------------------|----------------------|
| `LOGIN_SUCCESS` | Login exitoso | ✅ User ID, email/username, IP, user-agent |
| `LOGIN_FAILURE` | Credenciales incorrectas | ❌ Email/username intentado, IP, razón del fallo |
| `REFRESH_TOKEN` | Al renovar tokens | 🔄 User ID, IP |
| `LOGOUT` | Cierre de sesión | 🚪 User ID, IP |
| `LOGOUT_ALL_DEVICES` | Cierre global | 🚪 User ID, IP |
| `ACCOUNT_LOCKED` | 5+ intentos fallidos | 🔒 User ID, IP, email |

---

## ✅ No Requiere Cambios en el Frontend

### El frontend NO necesita:

- ❌ Enviar información adicional en los requests
- ❌ Implementar lógica de auditoría
- ❌ Capturar IPs o user-agents
- ❌ Modificar los DTOs existentes

### Todo funciona automáticamente porque:

1. **IP Real**: El backend captura automáticamente desde headers `X-Forwarded-For` o `X-Real-IP`
2. **User-Agent**: El backend extrae el navegador/dispositivo del request HTTP
3. **Metadata**: El backend relaciona la acción con el usuario autenticado

---

## 📡 Endpoints Sin Cambios

Los endpoints de autenticación **NO cambian** desde la perspectiva del frontend:

### 1. Login - `POST /auth/login`

**Request** (sin cambios):
```json
{
  "identifier": "admin@test.com",
  "password": "password"
}
```

**Response** (sin cambios):
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Auditoría automática**:
- ✅ Si es exitoso → `LOGIN_SUCCESS`
- ❌ Si falla → `LOGIN_FAILURE` con razón (Invalid password, User not found, Account locked)

---

### 2. Refresh Token - `POST /auth/refresh`

**Request** (sin cambios):
```json
{
  "refreshToken": "eyJhbGci..."
}
```

**Response** (sin cambios):
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Auditoría automática**:
- 🔄 `REFRESH_TOKEN` registrado con user_id e IP

---

### 3. Logout - `POST /auth/logout`

**Request** (sin cambios):
```http
POST /auth/logout
Authorization: Bearer eyJhbGci...
```

**Response** (sin cambios):
```json
{
  "message": "Logged out successfully"
}
```

**Auditoría automática**:
- 🚪 `LOGOUT` registrado con user_id e IP

---

### 4. Logout All Devices - `POST /auth/logout-all`

**Request** (sin cambios):
```http
POST /auth/logout-all
Authorization: Bearer eyJhbGci...
```

**Response** (sin cambios):
```json
{
  "message": "Logged out from all devices"
}
```

**Auditoría automática**:
- 🚪 `LOGOUT_ALL_DEVICES` registrado

---

## 🔒 Rate Limiting (Ya Implementado)

El sistema de **Rate Limiting** ya estaba implementado desde antes. Aquí están los detalles:

### Configuración Actual

- **Límite**: 100 requests por minuto por IP
- **Scope**: Por dirección IP (no por usuario)
- **Tecnología**: Bucket4j 7.6.0 con Redis
- **Endpoints protegidos**: Todos los endpoints `/auth/**`

### Comportamiento

```http
# Primeros 100 requests en 1 minuto
HTTP 200 OK (o según el endpoint)

# Request 101 en el mismo minuto
HTTP 429 Too Many Requests
{
  "path": "/auth/login",
  "error": "Too Many Requests",
  "message": "Too many requests. Please try again later.",
  "timestamp": "2025-10-05T10:30:00",
  "status": 429
}
```

### Headers de Rate Limit

El backend ya incluye estos headers en las respuestas:

```http
X-Rate-Limit-Remaining: 95
X-Rate-Limit-Retry-After-Seconds: 30
```

### Manejo en Frontend

**Recomendación**: El frontend debe manejar el error 429:

```typescript
// Ejemplo en Angular/TypeScript
handleRateLimit(error: HttpErrorResponse) {
  if (error.status === 429) {
    const retryAfter = error.headers.get('X-Rate-Limit-Retry-After-Seconds') || '60';
    
    this.toastService.error(
      `Demasiados intentos. Intenta nuevamente en ${retryAfter} segundos.`
    );
    
    // Opcional: Deshabilitar botón de login temporalmente
    setTimeout(() => {
      this.enableLoginButton();
    }, parseInt(retryAfter) * 1000);
  }
}
```

---

## 🚨 Manejo de Account Locked

Cuando una cuenta se bloquea (5 intentos fallidos en 15 minutos), el backend responde:

### Response de Account Locked

```http
HTTP 403 Forbidden
```

```json
{
  "path": "/auth/login",
  "error": "Forbidden",
  "message": "Account is locked due to multiple failed login attempts. Try again in 30 minutes.",
  "timestamp": "2025-10-05T10:30:00",
  "status": 403,
  "lockedUntil": "2025-10-05T11:00:00"
}
```

### Manejo Recomendado en Frontend

```typescript
handleAccountLocked(error: HttpErrorResponse) {
  if (error.status === 403 && error.error.message.includes('locked')) {
    const lockedUntil = new Date(error.error.lockedUntil);
    const now = new Date();
    const minutesRemaining = Math.ceil((lockedUntil.getTime() - now.getTime()) / 60000);
    
    this.dialogService.showError({
      title: 'Cuenta Bloqueada',
      message: `Tu cuenta ha sido bloqueada por seguridad debido a múltiples intentos fallidos.\n\nPodrás intentar nuevamente en ${minutesRemaining} minutos.`,
      icon: 'lock'
    });
    
    // Deshabilitar formulario de login
    this.loginForm.disable();
    
    // Opcional: Redirigir a página de ayuda
    this.router.navigate(['/help/account-locked']);
  }
}
```

---

## 📊 Dashboard de Auditoría (Futuro)

Aunque no es necesario para el funcionamiento básico, el backend está preparado para que el frontend pueda consultar los logs de auditoría en el futuro.

### Posible Endpoint Futuro (No implementado aún)

```http
GET /api/audit/me
Authorization: Bearer eyJhbGci...
```

**Respuesta esperada**:
```json
{
  "events": [
    {
      "id": 1,
      "action": "LOGIN_SUCCESS",
      "result": "SUCCESS",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
      "createdAt": "2025-10-05T10:30:00"
    },
    {
      "id": 2,
      "action": "LOGOUT",
      "result": "SUCCESS",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
      "createdAt": "2025-10-05T11:00:00"
    }
  ],
  "totalEvents": 25,
  "suspiciousActivity": false
}
```

**Vista en el frontend**: Tabla de "Actividad Reciente" en el perfil del usuario.

---

## 🔐 Mejores Prácticas de Seguridad

### 1. Mostrar Feedback Claro

```typescript
// ❌ NO hacer esto (información sensible)
showError("Usuario admin@test.com no existe");

// ✅ Hacer esto (mensaje genérico)
showError("Credenciales inválidas");
```

### 2. Implementar Throttling en el Cliente

```typescript
// Prevenir spam de requests desde el frontend
private loginAttempts = 0;
private lastAttempt = Date.now();

async login() {
  const now = Date.now();
  
  // Reset counter si pasaron más de 1 minuto
  if (now - this.lastAttempt > 60000) {
    this.loginAttempts = 0;
  }
  
  this.loginAttempts++;
  this.lastAttempt = now;
  
  // Mostrar warning después de 3 intentos
  if (this.loginAttempts >= 3) {
    this.toastService.warning(
      'Múltiples intentos detectados. Recuerda que después de 5 intentos fallidos tu cuenta se bloqueará por 30 minutos.'
    );
  }
  
  // Proceder con el login...
}
```

### 3. Limpiar Tokens al Logout

```typescript
logout() {
  // Llamar al backend
  this.authService.logout().subscribe({
    next: () => {
      // Limpiar localStorage/sessionStorage
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      
      // Limpiar estado de la aplicación
      this.store.dispatch(AuthActions.clearUser());
      
      // Redirigir
      this.router.navigate(['/login']);
    },
    error: (err) => {
      // Incluso si falla, limpiar tokens locales
      localStorage.clear();
      this.router.navigate(['/login']);
    }
  });
}
```

---

## 🧪 Testing del Frontend

### Casos de Prueba Recomendados

1. **Login Exitoso**
   - ✅ Tokens guardados en localStorage
   - ✅ Redirección a dashboard
   - ✅ User info cargada

2. **Login Fallido**
   - ❌ Mensaje de error genérico
   - ❌ Contador de intentos incrementado
   - ❌ Formulario no limpiado (para que usuario pueda corregir)

3. **Account Locked**
   - 🔒 Formulario deshabilitado
   - 🔒 Mensaje claro con tiempo de espera
   - 🔒 Link a ayuda/soporte

4. **Rate Limiting**
   - 🚫 Botón deshabilitado por X segundos
   - 🚫 Mensaje con countdown
   - 🚫 Toast notification

5. **Token Expiration**
   - 🔄 Refresh automático antes de expirar
   - 🔄 Fallback a login si refresh falla
   - 🔄 Sin pérdida de estado de la app

---

## 📞 Endpoints de Salud

El frontend puede verificar el estado del backend:

### Health Check
```http
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "postgres": { "status": "UP" }
  }
}
```

**Uso recomendado**: Mostrar banner si el backend está caído.

---

## 🎨 Componentes UI Recomendados

### 1. Login Form
- Input email/username
- Input password (con show/hide)
- Botón "Iniciar Sesión" (con loading state)
- Link "¿Olvidaste tu contraseña?"
- Contador de intentos (después de 3 fallos)

### 2. Account Locked Modal
- Ícono de candado
- Mensaje claro
- Countdown timer
- Botón "Contactar Soporte"
- Link "Recuperar Contraseña"

### 3. Rate Limit Toast
- Mensaje: "Demasiados intentos"
- Countdown: "Intenta en X segundos"
- Auto-dismiss cuando termina el countdown

---

## 📝 Resumen para Implementación

### ✅ Lo que YA está implementado en el backend:
- Sistema de auditoría completo
- Rate limiting (100 req/min)
- Account locking (5 intentos = 30 min bloqueo)
- Captura automática de IP y User-Agent
- Manejo de errores con mensajes apropiados

### 🎯 Lo que el frontend debe implementar:
- Manejo de error 429 (Rate Limit)
- Manejo de error 403 (Account Locked)
- Throttling del lado del cliente (recomendado)
- Feedback visual para intentos fallidos
- Limpiar tokens al logout

### ❌ Lo que NO necesita implementar:
- Enviar información de auditoría
- Capturar IPs o user-agents
- Implementar rate limiting (ya está en backend)
- Modificar DTOs existentes

---

## 🤝 Soporte

Si tienes dudas sobre la integración, contacta al equipo backend:

- **Documentación API**: http://localhost:8080/swagger-ui/index.html
- **Postman Collection**: `WOM_Auth_Service_API.postman_collection.json`
- **Health Check**: http://localhost:8080/actuator/health

---

**Última actualización**: Octubre 5, 2025  
**Versión del Backend**: 1.0.0  
**Autor**: Kevin Bayter
