package com.campus.service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.campus.config.ResourcePreviewProperties;
import com.campus.preview.HistoricalLocalResourcePreviewArtifactCleaner;
import com.campus.preview.ResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewService;

@Service
public class PreviewArtifactCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PreviewArtifactCleanupService.class);

    private final ResourcePreviewArtifactStorage activeStorage;
    private final HistoricalLocalResourcePreviewArtifactCleaner historicalLocalCleaner;
    private final ResourcePreviewProperties previewProperties;

    public PreviewArtifactCleanupService(
            ResourcePreviewArtifactStorage activeStorage,
            HistoricalLocalResourcePreviewArtifactCleaner historicalLocalCleaner,
            ResourcePreviewProperties previewProperties) {
        this.activeStorage = Objects.requireNonNull(activeStorage, "activeStorage");
        this.historicalLocalCleaner = Objects.requireNonNull(historicalLocalCleaner, "historicalLocalCleaner");
        this.previewProperties = Objects.requireNonNull(previewProperties, "previewProperties");
    }

    public void cleanupAfterResourceMutation(
            Optional<ResourcePreviewService.PreviewArtifactTarget> oldTarget,
            Optional<ResourcePreviewService.PreviewArtifactTarget> newTarget) {
        if (oldTarget.isEmpty()) {
            return;
        }
        if (newTarget.isPresent() && oldTarget.get().equals(newTarget.get())) {
            return;
        }
        String artifactKey = oldTarget.get().artifactKey();
        deleteQuietly(() -> activeStorage.delete(artifactKey), "active preview artifact", artifactKey);
        if ("minio".equalsIgnoreCase(previewProperties.getType())) {
            deleteQuietly(() -> historicalLocalCleaner.delete(artifactKey), "historical local preview artifact",
                    artifactKey);
        }
    }

    private void deleteQuietly(IoAction action, String target, String artifactKey) {
        try {
            action.run();
        } catch (IOException | RuntimeException exception) {
            log.warn("Failed to delete {}: {}", target, artifactKey, exception.toString());
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
