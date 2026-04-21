package com.campus.preview;

import java.nio.file.Path;
import java.util.Objects;

public class LocalPreviewArtifactPathResolver {

    private final Path rootPath;

    public LocalPreviewArtifactPathResolver(Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
    }

    public Path resolve(String artifactKey) {
        String normalizedKey = normalizeArtifactKey(artifactKey);
        Path resolvedPath = rootPath.resolve(normalizedKey).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("artifact key escapes local preview root");
        }
        return resolvedPath;
    }

    private String normalizeArtifactKey(String artifactKey) {
        if (artifactKey == null || artifactKey.isBlank()) {
            throw new IllegalArgumentException("artifact key is blank");
        }

        String normalized = artifactKey.trim().replace("\\", "/");
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("artifact key escapes local preview root");
        }

        for (String segment : normalized.split("/+")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("artifact key escapes local preview root");
            }
        }

        Path normalizedPath = Path.of(normalized).normalize();
        if (normalizedPath.isAbsolute()) {
            throw new IllegalArgumentException("artifact key escapes local preview root");
        }

        String normalizedString = normalizedPath.toString().replace("\\", "/");
        if (normalizedString.isBlank() || ".".equals(normalizedString)) {
            throw new IllegalArgumentException("artifact key is blank");
        }
        return normalizedString;
    }
}
