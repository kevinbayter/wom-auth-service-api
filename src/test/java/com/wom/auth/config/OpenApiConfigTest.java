package com.wom.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenApiConfig.
 * 
 * @author Kevin Bayter
 * @see <a href="https://github.com/kevinbayter">GitHub Profile</a>
 */
class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
    }

    @Test
    void customOpenAPI_ShouldReturnConfiguredOpenAPI() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertNotNull(openAPI);
    }

    @Test
    void customOpenAPI_ShouldHaveCorrectInfo() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Info info = openAPI.getInfo();

        // Then
        assertNotNull(info);
        assertEquals("WOM Auth Service API", info.getTitle());
        assertEquals("Enterprise-grade authentication service with JWT RS256, refresh token rotation, and rate limiting", info.getDescription());
        assertEquals("1.0.0", info.getVersion());
    }

    @Test
    void customOpenAPI_ShouldHaveContact() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Contact contact = openAPI.getInfo().getContact();

        // Then
        assertNotNull(contact);
        assertEquals("WOM Development Team", contact.getName());
        assertEquals("dev@wom.com", contact.getEmail());
    }

    @Test
    void customOpenAPI_ShouldHaveLicense() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        License license = openAPI.getInfo().getLicense();

        // Then
        assertNotNull(license);
        assertEquals("MIT License", license.getName());
        assertEquals("https://opensource.org/licenses/MIT", license.getUrl());
    }

    @Test
    void customOpenAPI_ShouldHaveServers() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertNotNull(openAPI.getServers());
        assertEquals(2, openAPI.getServers().size());
    }

    @Test
    void customOpenAPI_ShouldHaveDevelopmentServer() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Server devServer = openAPI.getServers().get(0);

        // Then
        assertNotNull(devServer);
        assertEquals("http://localhost:8080", devServer.getUrl());
        assertEquals("Development server", devServer.getDescription());
    }

    @Test
    void customOpenAPI_ShouldHaveProductionServer() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Server prodServer = openAPI.getServers().get(1);

        // Then
        assertNotNull(prodServer);
        assertEquals("https://api.wom.com", prodServer.getUrl());
        assertEquals("Production server", prodServer.getDescription());
    }

    @Test
    void customOpenAPI_ShouldHaveSecurityScheme() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("bearerAuth"));
    }

    @Test
    void customOpenAPI_SecuritySchemeShouldBeBearer() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        // Then
        assertNotNull(securityScheme);
        assertEquals("bearerAuth", securityScheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
    }

    @Test
    void customOpenAPI_SecuritySchemeShouldHaveDescription() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        // Then
        assertNotNull(securityScheme);
        assertEquals("JWT access token obtained from /auth/login or /auth/refresh", securityScheme.getDescription());
    }

    @Test
    void customOpenAPI_InfoShouldBeComplete() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Info info = openAPI.getInfo();

        // Then
        assertNotNull(info.getTitle());
        assertNotNull(info.getDescription());
        assertNotNull(info.getVersion());
        assertNotNull(info.getContact());
        assertNotNull(info.getLicense());
    }

    @Test
    void customOpenAPI_ServersShouldBeOrdered() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertEquals("http://localhost:8080", openAPI.getServers().get(0).getUrl());
        assertEquals("https://api.wom.com", openAPI.getServers().get(1).getUrl());
    }
}
