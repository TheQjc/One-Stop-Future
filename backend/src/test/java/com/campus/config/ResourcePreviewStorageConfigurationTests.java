package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.campus.preview.LocalResourcePreviewArtifactStorage;
import com.campus.preview.MinioResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewArtifactStorage;
import com.campus.storage.MinioObjectOperations;

class ResourcePreviewStorageConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(
                    ResourcePreviewProperties.class,
                    MinioIntegrationProperties.class,
                    ResourcePreviewStorageConfiguration.class);

    @Test
    void localPreviewTypeCreatesLocalPreviewArtifactStorage() {
        contextRunner
                .withPropertyValues("app.resource-preview.type=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                    assertThat(context).hasSingleBean(LocalResourcePreviewArtifactStorage.class);
                    assertThat(context).doesNotHaveBean(MinioResourcePreviewArtifactStorage.class);
                });
    }

    @Test
    void localPreviewTypeCanCoexistWithEnabledMinioIntegration() {
        contextRunner
                .withBean(MinioObjectOperations.class, FakeMinioObjectOperations::new)
                .withPropertyValues(
                        "app.resource-preview.type=local",
                        "platform.integrations.minio.enabled=true",
                        "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
                        "platform.integrations.minio.access-key=minioadmin",
                        "platform.integrations.minio.secret-key=minioadmin",
                        "platform.integrations.minio.bucket=campus-platform")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                    assertThat(context).hasSingleBean(LocalResourcePreviewArtifactStorage.class);
                    assertThat(context).doesNotHaveBean(MinioResourcePreviewArtifactStorage.class);
                });
    }

    @Test
    void minioPreviewTypeCreatesMinioPreviewArtifactStorageWhenMinioIsEnabled() {
        contextRunner
                .withBean(MinioObjectOperations.class, FakeMinioObjectOperations::new)
                .withPropertyValues(
                        "app.resource-preview.type=minio",
                        "app.resource-preview.minio-prefix=preview-artifacts",
                        "platform.integrations.minio.enabled=true",
                        "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
                        "platform.integrations.minio.access-key=minioadmin",
                        "platform.integrations.minio.secret-key=minioadmin",
                        "platform.integrations.minio.bucket=campus-platform")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                    assertThat(context).hasSingleBean(MinioResourcePreviewArtifactStorage.class);
                    assertThat(context).doesNotHaveBean(LocalResourcePreviewArtifactStorage.class);
                });
    }

    @Test
    void minioPreviewTypeFailsFastWhenMinioIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-preview.type=minio",
                        "platform.integrations.minio.enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("minio preview storage is selected but disabled");
                });
    }

    private static final class FakeMinioObjectOperations implements MinioObjectOperations {

        private final Set<String> buckets = new HashSet<>();
        private final Map<String, byte[]> objects = new HashMap<>();

        @Override
        public boolean bucketExists(String bucketName) {
            return buckets.contains(bucketName);
        }

        @Override
        public void createBucket(String bucketName) {
            buckets.add(bucketName);
        }

        @Override
        public void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException {
            objects.put(bucketName + ":" + objectKey, inputStream.readAllBytes());
        }

        @Override
        public InputStream getObject(String bucketName, String objectKey) throws IOException {
            throw new IOException("not implemented");
        }

        @Override
        public boolean objectExists(String bucketName, String objectKey) {
            return objects.containsKey(bucketName + ":" + objectKey);
        }

        @Override
        public void removeObject(String bucketName, String objectKey) throws IOException {
            objects.remove(bucketName + ":" + objectKey);
        }
    }
}
