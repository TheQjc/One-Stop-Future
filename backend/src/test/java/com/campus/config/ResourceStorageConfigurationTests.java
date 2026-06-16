package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.campus.storage.LocalResourceFileStorage;
import com.campus.storage.FallbackResourceFileStorage;
import com.campus.storage.MinioObjectOperations;
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
    void localStorageTypeCanStillExposeMinioOperationsWhenMinioIntegrationIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-storage.type=local",
                        "platform.integrations.minio.enabled=true",
                        "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
                        "platform.integrations.minio.access-key=minioadmin",
                        "platform.integrations.minio.secret-key=minioadmin",
                        "platform.integrations.minio.bucket=campus-platform")
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalResourceFileStorage.class);
                    assertThat(context).hasSingleBean(MinioObjectOperations.class);
                });
    }

    @Test
    void localStorageTypeDoesNotCreateMinioOperationsWhenMinioIntegrationIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-storage.type=local",
                        "platform.integrations.minio.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MinioObjectOperations.class));
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

    @Test
    void minioStorageTypeUsesLocalReadFallbackWhenEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        ResourceStorageConfiguration.class,
                        StorageKeyFactory.class)
                .withBean(ResourceStorageProperties.class, () -> {
                    ResourceStorageProperties properties = new ResourceStorageProperties();
                    properties.setType("minio");
                    properties.setReadFallbackLocalEnabled(true);
                    return properties;
                })
                .withBean(MinioObjectOperations.class, FakeMinioObjectOperations::new)
                .withBean(MinioIntegrationProperties.class, () -> {
                    MinioIntegrationProperties properties = new MinioIntegrationProperties();
                    properties.setEnabled(true);
                    properties.setBucket("campus-platform");
                    return properties;
                })
                .withPropertyValues("app.resource-storage.type=minio")
                .run(context -> {
                    assertThat(context).hasSingleBean(FallbackResourceFileStorage.class);
                    assertThat(context).doesNotHaveBean(LocalResourceFileStorage.class);
                });
    }

    private static final class FakeMinioObjectOperations implements MinioObjectOperations {

        @Override
        public boolean bucketExists(String bucketName) {
            return true;
        }

        @Override
        public void createBucket(String bucketName) {
        }

        @Override
        public void putObject(String bucketName, String objectKey, java.io.InputStream inputStream) {
        }

        @Override
        public java.io.InputStream getObject(String bucketName, String objectKey) throws java.io.IOException {
            throw new java.io.FileNotFoundException(objectKey);
        }

        @Override
        public boolean objectExists(String bucketName, String objectKey) {
            return false;
        }

        @Override
        public void removeObject(String bucketName, String objectKey) {
        }
    }
}
