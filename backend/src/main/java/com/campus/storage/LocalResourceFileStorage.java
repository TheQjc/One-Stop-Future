package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import com.campus.config.ResourceStorageProperties;

public class LocalResourceFileStorage implements ResourceFileStorage {

    private final Path rootPath;
    private final StorageKeyFactory storageKeyFactory;

    public LocalResourceFileStorage(ResourceStorageProperties properties, StorageKeyFactory storageKeyFactory) {
        this.rootPath = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        this.storageKeyFactory = storageKeyFactory;
        try {
            Files.createDirectories(rootPath);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize local resource storage", exception);
        }
    }

    @Override
    public String store(String originalFilename, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");

        String storageKey = storageKeyFactory.newStorageKey(originalFilename);
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
    public boolean exists(String storageKey) throws IOException {
        return Files.exists(resolve(storageKey));
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
