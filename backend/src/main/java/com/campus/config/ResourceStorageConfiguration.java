package com.campus.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.campus.storage.LocalResourceFileStorage;
import com.campus.storage.MinioObjectOperations;
import com.campus.storage.MinioResourceFileStorage;
import com.campus.storage.ResourceFileStorage;
import com.campus.storage.SdkMinioObjectOperations;
import com.campus.storage.StorageKeyFactory;

import io.minio.MinioClient;

@Configuration
public class ResourceStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "local", matchIfMissing = true)
    ResourceFileStorage localResourceFileStorage(ResourceStorageProperties properties, StorageKeyFactory keyFactory) {
        return new LocalResourceFileStorage(properties, keyFactory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.integrations.minio", name = "enabled", havingValue = "true")
    MinioClient minioClient(MinioIntegrationProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.integrations.minio", name = "enabled", havingValue = "true")
    MinioObjectOperations minioObjectOperations(MinioClient client) {
        return new SdkMinioObjectOperations(client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
    Object minioStorageSelectionGuard(MinioIntegrationProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("minio storage is selected but disabled");
        }
        return new Object();
    }

    @Bean
    @DependsOn("minioStorageSelectionGuard")
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
    ResourceFileStorage minioResourceFileStorage(
            MinioIntegrationProperties properties,
            MinioObjectOperations operations,
            StorageKeyFactory keyFactory) throws IOException {
        return new MinioResourceFileStorage(properties.getBucket(), operations, keyFactory);
    }
}
