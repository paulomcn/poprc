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
        boolean devProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equals);
        boolean testProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("test"::equals);
        if (!devProfile && !testProfile) {
            return;
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String databaseName = extractDatabaseName(datasourceUrl);
        String requiredSuffix = testProfile ? "_test" : "_dev";
        if (!databaseName.endsWith(requiredSuffix)) {
            throw new IllegalStateException(
                    "Perfil " + (testProfile ? "test" : "dev")
                            + " recusado: o banco configurado precisa terminar em '"
                            + requiredSuffix + "'. URL recebida: " + datasourceUrl);
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
