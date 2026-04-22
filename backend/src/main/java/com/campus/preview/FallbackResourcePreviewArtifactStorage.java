package com.campus.preview;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class FallbackResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final ResourcePreviewArtifactStorage primaryStorage;
    private final HistoricalLocalResourcePreviewArtifactReader localFallbackReader;

    public FallbackResourcePreviewArtifactStorage(
            ResourcePreviewArtifactStorage primaryStorage,
            HistoricalLocalResourcePreviewArtifactReader localFallbackReader) {
        this.primaryStorage = Objects.requireNonNull(primaryStorage, "primaryStorage");
        this.localFallbackReader = Objects.requireNonNull(localFallbackReader, "localFallbackReader");
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        if (primaryStorage.exists(artifactKey)) {
            return true;
        }
        return localFallbackReader.exists(artifactKey);
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        try {
            return primaryStorage.open(artifactKey);
        } catch (FileNotFoundException exception) {
            return localFallbackReader.open(artifactKey);
        }
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        primaryStorage.write(artifactKey, inputStream);
    }
}
