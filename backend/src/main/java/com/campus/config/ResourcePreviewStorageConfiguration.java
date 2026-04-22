package com.campus.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.campus.preview.FallbackResourcePreviewArtifactStorage;
import com.campus.preview.HistoricalLocalResourcePreviewArtifactReader;
import com.campus.preview.LocalResourcePreviewArtifactStorage;
import com.campus.preview.MinioResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewArtifactStorage;
import com.campus.storage.MinioObjectOperations;

@Configuration
public class ResourcePreviewStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "local",
            matchIfMissing = true)
    ResourcePreviewArtifactStorage localResourcePreviewArtifactStorage(ResourcePreviewProperties properties) {
        return new LocalResourcePreviewArtifactStorage(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "minio")
    Object minioPreviewStorageSelectionGuard(MinioIntegrationProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("minio preview storage is selected but disabled");
        }
        return new Object();
    }

    @Bean
    @DependsOn("minioPreviewStorageSelectionGuard")
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "minio")
    ResourcePreviewArtifactStorage minioResourcePreviewArtifactStorage(
            ResourcePreviewProperties previewProperties,
            MinioIntegrationProperties minioProperties,
            MinioObjectOperations operations) throws IOException {
        MinioResourcePreviewArtifactStorage primaryStorage = new MinioResourcePreviewArtifactStorage(
                minioProperties.getBucket(),
                previewProperties.getMinioPrefix(),
                operations);
        if (!previewProperties.isReadFallbackLocalEnabled()) {
            return primaryStorage;
        }
        return new FallbackResourcePreviewArtifactStorage(
                primaryStorage,
                new HistoricalLocalResourcePreviewArtifactReader(previewProperties.getLocalRoot()));
    }
}
