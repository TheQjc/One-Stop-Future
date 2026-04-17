package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

class ApplicationConfigSafetyTests {

    @Test
    void defaultApplicationConfigDoesNotAutoResetDatabase() {
        Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void localProfileUsesEmbeddedDatabaseInitializationAndLocalResourceStorage() {
        Properties properties = loadYaml(Path.of("src", "main", "resources", "application-local.yml"));

        assertThat(properties.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:campus-local");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("always");
        assertThat(properties.getProperty("app.resource-storage.type")).isEqualTo("local");
        assertThat(properties.getProperty("app.resource-storage.local-root"))
                .isEqualTo(".local-storage/resources");
    }

    @Test
    void defaultApplicationConfigKeepsMinioDisabledUntilExplicitlyEnabled() {
        Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("app.resource-storage.type")).isEqualTo("${RESOURCE_STORAGE_TYPE:local}");
        assertThat(properties.getProperty("platform.integrations.minio.enabled")).isEqualTo("${MINIO_ENABLED:false}");
        assertThat(properties.getProperty("platform.integrations.minio.bucket")).isEqualTo("${MINIO_BUCKET:campus-platform}");
    }

    @Test
    void testConfigKeepsMinioDisabledByDefault() {
        Properties properties = loadYaml(Path.of("src", "test", "resources", "application.yml"));

        assertThat(properties.getProperty("platform.integrations.minio.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("platform.integrations.minio.bucket")).isEqualTo("campus-platform-test");
    }

    private Properties loadYaml(Path resourcePath) {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new FileSystemResource(resourcePath));
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
