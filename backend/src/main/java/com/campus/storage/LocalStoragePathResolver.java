package com.campus.storage;

import java.nio.file.Path;
import java.util.Objects;

public class LocalStoragePathResolver {

    private final Path rootPath;

    public LocalStoragePathResolver(Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
    }

    public Path resolve(String storageKey) {
        String normalizedKey = normalizeStorageKey(storageKey);
        Path resolvedPath = rootPath.resolve(normalizedKey).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }
        return resolvedPath;
    }

    private String normalizeStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storage key is blank");
        }

        String normalized = storageKey.trim().replace("\\", "/");
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }

        for (String segment : normalized.split("/+")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("storage key escapes local resource root");
            }
        }

        Path normalizedPath = Path.of(normalized).normalize();
        if (normalizedPath.isAbsolute()) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }

        String normalizedString = normalizedPath.toString().replace("\\", "/");
        if (normalizedString.isBlank() || ".".equals(normalizedString)) {
            throw new IllegalArgumentException("storage key is blank");
        }
        return normalizedString;
    }
}
