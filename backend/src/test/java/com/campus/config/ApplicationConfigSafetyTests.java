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
    void defaultApplicationConfigKeepsJobSyncDisabledUntilExplicitlyEnabled() {
        Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("platform.integrations.job-sync.enabled"))
                .isEqualTo("${JOB_SYNC_ENABLED:false}");
        assertThat(properties.getProperty("platform.integrations.job-sync.source-name"))
                .isEqualTo("${JOB_SYNC_SOURCE_NAME:Partner Feed}");
    }

    @Test
    void defaultApplicationConfigKeepsRedisDisabledButProvidesConnectionTemplate() {
        Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

        assertThat(properties.getProperty("platform.integrations.redis.enabled"))
                .isEqualTo("${REDIS_ENABLED:false}");
        assertThat(properties.getProperty("platform.integrations.redis.ttl-seconds"))
                .isEqualTo("${REDIS_CACHE_TTL_SECONDS:60}");
        assertThat(properties.getProperty("spring.data.redis.host"))
                .isEqualTo("${REDIS_HOST:114.132.220.42}");
        assertThat(properties.getProperty("spring.data.redis.port"))
                .isEqualTo("${REDIS_PORT:16379}");
    }

    @Test
    void localAndTestConfigsKeepJobSyncDisabledWithoutPinnedFeedUrl() {
        Properties localProperties = loadYaml(Path.of("src", "main", "resources", "application-local.yml"));
        Properties testProperties = loadYaml(Path.of("src", "test", "resources", "application.yml"));

        assertThat(localProperties.getProperty("platform.integrations.job-sync.enabled")).isEqualTo("false");
        assertThat(localProperties.getProperty("platform.integrations.job-sync.feed-url")).isNull();
        assertThat(testProperties.getProperty("platform.integrations.job-sync.enabled")).isEqualTo("false");
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
