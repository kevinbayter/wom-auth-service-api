package com.wom.auth.config;

import com.wom.auth.filter.JwtAuthenticationFilter;
import com.wom.auth.filter.RateLimitingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SecurityConfig
 * Valida la configuración de seguridad y los beans creados
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthFilter;

    @Mock
    private RateLimitingFilter rateLimitFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("SecurityConfig debe instanciarse correctamente")
    void securityConfig_ShouldBeInstantiated() {
        assertNotNull(securityConfig);
    }

    @Test
    @DisplayName("passwordEncoder debe retornar BCryptPasswordEncoder")
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // When
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        // Then
        assertNotNull(encoder);
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("passwordEncoder debe crear nueva instancia cada vez")
    void passwordEncoder_ShouldCreateNewInstance() {
        // When
        PasswordEncoder encoder1 = securityConfig.passwordEncoder();
        PasswordEncoder encoder2 = securityConfig.passwordEncoder();

        // Then
        assertNotNull(encoder1);
        assertNotNull(encoder2);
        assertNotSame(encoder1, encoder2, "Cada llamada debe crear una nueva instancia");
    }

    @Test
    @DisplayName("passwordEncoder debe poder encodear passwords")
    void passwordEncoder_ShouldEncodePasswords() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecretPassword123";

        // When
        String encodedPassword = encoder.encode(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$"), "BCrypt passwords should start with $2a$");
    }

    @Test
    @DisplayName("passwordEncoder debe validar passwords correctamente")
    void passwordEncoder_ShouldValidatePasswords() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "myPassword123";
        String encodedPassword = encoder.encode(rawPassword);

        // When & Then
        assertTrue(encoder.matches(rawPassword, encodedPassword));
        assertFalse(encoder.matches("wrongPassword", encodedPassword));
    }

    @Test
    @DisplayName("SecurityConfig debe tener JwtAuthenticationFilter inyectado")
    void securityConfig_ShouldHaveJwtAuthFilterInjected() {
        // When
        JwtAuthenticationFilter filter = (JwtAuthenticationFilter) ReflectionTestUtils.getField(securityConfig, "jwtAuthFilter");

        // Then
        assertNotNull(filter);
        assertSame(jwtAuthFilter, filter);
    }

    @Test
    @DisplayName("SecurityConfig debe tener RateLimitingFilter inyectado")
    void securityConfig_ShouldHaveRateLimitFilterInjected() {
        // When
        RateLimitingFilter filter = (RateLimitingFilter) ReflectionTestUtils.getField(securityConfig, "rateLimitFilter");

        // Then
        assertNotNull(filter);
        assertSame(rateLimitFilter, filter);
    }

    @Test
    @DisplayName("SecurityConfig debe tener anotación @Configuration")
    void securityConfig_ShouldHaveConfigurationAnnotation() {
        // When
        boolean hasConfigAnnotation = SecurityConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class);

        // Then
        assertTrue(hasConfigAnnotation, "SecurityConfig debe tener @Configuration");
    }

    @Test
    @DisplayName("SecurityConfig debe tener anotación @EnableWebSecurity")
    void securityConfig_ShouldHaveEnableWebSecurityAnnotation() {
        // When
        boolean hasEnableWebSecurity = SecurityConfig.class.isAnnotationPresent(org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class);

        // Then
        assertTrue(hasEnableWebSecurity, "SecurityConfig debe tener @EnableWebSecurity");
    }

    @Test
    @DisplayName("SecurityConfig debe tener constructor con filtros inyectados")
    void securityConfig_ShouldHaveConstructorWithFilters() {
        // When
        java.lang.reflect.Constructor<?>[] constructors = SecurityConfig.class.getDeclaredConstructors();
        
        // Then
        assertTrue(constructors.length > 0, "SecurityConfig debe tener al menos un constructor");
        
        // Buscar constructor que acepte JwtAuthenticationFilter y RateLimitingFilter
        boolean hasRequiredConstructor = java.util.Arrays.stream(constructors)
            .anyMatch(constructor -> {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                return paramTypes.length == 2 &&
                       (paramTypes[0].equals(JwtAuthenticationFilter.class) && paramTypes[1].equals(RateLimitingFilter.class) ||
                        paramTypes[1].equals(JwtAuthenticationFilter.class) && paramTypes[0].equals(RateLimitingFilter.class));
            });
        
        assertTrue(hasRequiredConstructor, "SecurityConfig debe tener constructor con JwtAuthenticationFilter y RateLimitingFilter");
    }

    @Test
    @DisplayName("passwordEncoder debe ser un método @Bean")
    void passwordEncoder_ShouldBeBeanMethod() throws NoSuchMethodException {
        // When
        boolean hasBean = SecurityConfig.class
                .getMethod("passwordEncoder")
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class);

        // Then
        assertTrue(hasBean, "passwordEncoder debe tener @Bean");
    }

    @Test
    @DisplayName("securityFilterChain debe ser un método @Bean")
    void securityFilterChain_ShouldBeBeanMethod() throws NoSuchMethodException {
        // When
        boolean hasBean = SecurityConfig.class
                .getMethod("securityFilterChain", org.springframework.security.config.annotation.web.builders.HttpSecurity.class)
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class);

        // Then
        assertTrue(hasBean, "securityFilterChain debe tener @Bean");
    }

    @Test
    @DisplayName("BCryptPasswordEncoder debe generar hashes diferentes para la misma contraseña")
    void bCryptPasswordEncoder_ShouldGenerateDifferentHashesForSamePassword() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "testPassword123";

        // When
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        // Then
        assertNotEquals(hash1, hash2, "BCrypt debe generar diferentes hashes para la misma contraseña (salt aleatorio)");
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }

    @Test
    @DisplayName("SecurityConfig clase debe ser pública")
    void securityConfig_ClassShouldBePublic() {
        // When
        int modifiers = SecurityConfig.class.getModifiers();

        // Then
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), "SecurityConfig debe ser pública");
    }

    @Test
    @DisplayName("SecurityConfig no debe ser abstracta")
    void securityConfig_ShouldNotBeAbstract() {
        // When
        int modifiers = SecurityConfig.class.getModifiers();

        // Then
        assertFalse(java.lang.reflect.Modifier.isAbstract(modifiers), "SecurityConfig no debe ser abstracta");
    }

    @Test
    @DisplayName("SecurityConfig no debe ser interface")
    void securityConfig_ShouldNotBeInterface() {
        // Then
        assertFalse(SecurityConfig.class.isInterface(), "SecurityConfig no debe ser una interface");
    }
}
