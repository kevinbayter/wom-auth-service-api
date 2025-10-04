package com.wom.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthServiceApplication.
 */
class AuthServiceApplicationTest {

    @Test
    void mainMethodShouldExist() {
        // This test verifies that main method exists with correct signature
        assertDoesNotThrow(() -> {
            Method mainMethod = AuthServiceApplication.class.getDeclaredMethod("main", String[].class);
            assertNotNull(mainMethod);
            assertTrue(Modifier.isPublic(mainMethod.getModifiers()));
            assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
        });
    }

    @Test
    void applicationHasSpringBootApplicationAnnotation() {
        // Verify that the class has @SpringBootApplication annotation
        assertTrue(AuthServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

    @Test
    void applicationClassIsPublic() {
        // Verify that the application class is public
        assertTrue(Modifier.isPublic(AuthServiceApplication.class.getModifiers()));
    }

    @Test
    void applicationMainClassHasCorrectPackage() {
        // Verify that the application is in the correct package
        assertEquals("com.wom.auth", AuthServiceApplication.class.getPackageName());
    }

    @Test
    void applicationCanBeInstantiated() {
        // Verify that the application class can be instantiated
        assertDoesNotThrow(() -> new AuthServiceApplication());
    }

    @Test
    void applicationClassHasCorrectName() {
        // Verify the class name
        assertEquals("AuthServiceApplication", AuthServiceApplication.class.getSimpleName());
    }

    @Test
    void springBootApplicationAnnotationIsPresent() {
        // Double check SpringBootApplication annotation
        SpringBootApplication annotation = AuthServiceApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation);
    }

    @Test
    void applicationIsNotAbstract() {
        // Verify that the class is not abstract
        assertFalse(Modifier.isAbstract(AuthServiceApplication.class.getModifiers()));
    }

    @Test
    void applicationIsNotInterface() {
        // Verify that it's not an interface
        assertFalse(AuthServiceApplication.class.isInterface());
    }

    @Test
    void mainMethodHasVoidReturnType() {
        // Verify main method return type
        assertDoesNotThrow(() -> {
            Method mainMethod = AuthServiceApplication.class.getDeclaredMethod("main", String[].class);
            assertEquals(void.class, mainMethod.getReturnType());
        });
    }
}
