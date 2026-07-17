package com.poprc.demo.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class DevDatabaseSafetyInitializerTest {

    private final DevDatabaseSafetyInitializer initializer = new DevDatabaseSafetyInitializer();

    @Test
    void aceitaPerfilDevSomenteComBancoDev() {
        GenericApplicationContext context = context("dev", "jdbc:postgresql://localhost:5432/poprc_dev");

        assertDoesNotThrow(() -> initializer.initialize(context));
    }

    @Test
    void bloqueiaPerfilDevApontandoParaBancoPrincipal() {
        GenericApplicationContext context = context("dev", "jdbc:postgresql://localhost:5432/poprc");

        assertThrows(IllegalStateException.class, () -> initializer.initialize(context));
    }

    @Test
    void naoInterfereEmOutrosPerfis() {
        GenericApplicationContext context = context("prod", "jdbc:postgresql://localhost:5432/poprc");

        assertDoesNotThrow(() -> initializer.initialize(context));
    }

    private GenericApplicationContext context(String profile, String datasourceUrl) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        environment.setProperty("spring.datasource.url", datasourceUrl);
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        return context;
    }
}
