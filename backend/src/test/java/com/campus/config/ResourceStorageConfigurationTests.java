package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.campus.storage.LocalResourceFileStorage;
import com.campus.storage.StorageKeyFactory;

class ResourceStorageConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ResourceStorageProperties.class,
                    MinioIntegrationProperties.class,
                    ResourceStorageConfiguration.class,
                    StorageKeyFactory.class);

    @Test
    void localStorageTypeCreatesLocalResourceFileStorage() {
        contextRunner
                .withPropertyValues("app.resource-storage.type=local")
                .run(context -> assertThat(context).hasSingleBean(LocalResourceFileStorage.class));
    }

    @Test
    void minioStorageTypeFailsFastWhenMinioIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-storage.type=minio",
                        "platform.integrations.minio.enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure()).hasMessageContaining("minio storage is selected but disabled");
                });
    }
}
