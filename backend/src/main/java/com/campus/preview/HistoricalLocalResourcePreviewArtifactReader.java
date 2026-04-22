package com.campus.preview;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class HistoricalLocalResourcePreviewArtifactReader {

    private final LocalPreviewArtifactPathResolver pathResolver;

    public HistoricalLocalResourcePreviewArtifactReader(String localRoot) {
        this.pathResolver = new LocalPreviewArtifactPathResolver(Path.of(localRoot));
    }

    public boolean exists(String artifactKey) {
        return Files.exists(pathResolver.resolve(artifactKey));
    }

    public InputStream open(String artifactKey) throws IOException {
        try {
            return Files.newInputStream(pathResolver.resolve(artifactKey));
        } catch (NoSuchFileException exception) {
            FileNotFoundException notFound = new FileNotFoundException("local preview artifact not found");
            notFound.initCause(exception);
            throw notFound;
        }
    }
}
