package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import com.campus.config.ResourcePreviewProperties;

public class LocalResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final LocalPreviewArtifactPathResolver pathResolver;

    public LocalResourcePreviewArtifactStorage(ResourcePreviewProperties properties) {
        Path rootPath = Path.of(properties.getLocalRoot());
        this.pathResolver = new LocalPreviewArtifactPathResolver(rootPath);
        try {
            Files.createDirectories(rootPath.toAbsolutePath().normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize local resource preview storage", exception);
        }
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        return Files.exists(pathResolver.resolve(artifactKey));
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        return Files.newInputStream(pathResolver.resolve(artifactKey));
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        Path artifactPath = pathResolver.resolve(artifactKey);
        Files.createDirectories(artifactPath.getParent());
        Files.copy(inputStream, artifactPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
