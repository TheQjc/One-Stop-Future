package com.campus.preview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.campus.config.ResourcePreviewProperties;

@Component
public class HistoricalLocalResourcePreviewArtifactCleaner {

    private final LocalPreviewArtifactPathResolver pathResolver;

    public HistoricalLocalResourcePreviewArtifactCleaner(ResourcePreviewProperties properties) {
        this.pathResolver = new LocalPreviewArtifactPathResolver(Path.of(properties.getLocalRoot()));
    }

    public void delete(String artifactKey) throws IOException {
        Files.deleteIfExists(pathResolver.resolve(artifactKey));
    }
}
