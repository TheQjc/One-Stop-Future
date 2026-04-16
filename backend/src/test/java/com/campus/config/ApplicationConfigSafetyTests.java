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
        Properties properties = loadYaml("application.yml");

        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void localProfileUsesEmbeddedDatabaseInitializationAndLocalResourceStorage() {
        Properties properties = loadYaml("application-local.yml");

        assertThat(properties.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:campus-local");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("always");
        assertThat(properties.getProperty("app.resource-storage.type")).isEqualTo("local");
        assertThat(properties.getProperty("app.resource-storage.local-root"))
                .isEqualTo(".local-storage/resources");
    }

    private Properties loadYaml(String resourcePath) {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new FileSystemResource(Path.of("src", "main", "resources", resourcePath)));
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
