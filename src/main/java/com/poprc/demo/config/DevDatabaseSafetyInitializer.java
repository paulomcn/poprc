package com.poprc.demo.config;

import java.util.Arrays;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

public class DevDatabaseSafetyInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch("dev"::equals)) {
            return;
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String databaseName = extractDatabaseName(datasourceUrl);
        if (!databaseName.endsWith("_dev")) {
            throw new IllegalStateException(
                    "Perfil dev recusado: o banco configurado precisa terminar em '_dev'. URL recebida: "
                            + datasourceUrl);
        }
    }

    private String extractDatabaseName(String datasourceUrl) {
        int slash = datasourceUrl.lastIndexOf('/');
        if (slash < 0 || slash == datasourceUrl.length() - 1) {
            return "";
        }
        String databaseName = datasourceUrl.substring(slash + 1);
        int query = databaseName.indexOf('?');
        return query >= 0 ? databaseName.substring(0, query) : databaseName;
    }
}
