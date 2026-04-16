package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.campus.config.ResourceStorageProperties;

@Component
public class LocalResourceFileStorage implements ResourceFileStorage {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path rootPath;

    public LocalResourceFileStorage(ResourceStorageProperties properties) {
        this.rootPath = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize local resource storage", exception);
        }
    }

    @Override
    public String store(String originalFilename, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");

        String extension = extractExtension(originalFilename);
        String storageKey = DATE_PATH_FORMATTER.format(LocalDate.now()) + "/" + UUID.randomUUID() + extension;
        Path filePath = resolve(storageKey);

        Files.createDirectories(filePath.getParent());
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return normalizeStorageKey(storageKey);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolve(storageKey));
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDot);
    }

    private Path resolve(String storageKey) {
        Path resolvedPath = rootPath.resolve(normalizeStorageKey(storageKey)).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }
        return resolvedPath;
    }

    private String normalizeStorageKey(String storageKey) {
        return storageKey.replace("\\", "/");
    }
}
