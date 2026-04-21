package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.campus.config.ResourcePreviewProperties;

@Component
public class LocalResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final Path rootPath;

    public LocalResourcePreviewArtifactStorage(ResourcePreviewProperties properties) {
        this.rootPath = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize local resource preview storage", exception);
        }
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        return Files.exists(resolve(artifactKey));
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        return Files.newInputStream(resolve(artifactKey));
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        Path artifactPath = resolve(artifactKey);
        Files.createDirectories(artifactPath.getParent());
        Files.copy(inputStream, artifactPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path resolve(String artifactKey) {
        Path resolvedPath = rootPath.resolve(normalizeKey(artifactKey)).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("artifact key escapes local preview root");
        }
        return resolvedPath;
    }

    private String normalizeKey(String artifactKey) {
        return artifactKey.replace("\\", "/");
    }
}
