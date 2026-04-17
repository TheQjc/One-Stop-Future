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
    private final LocalStoragePathResolver pathResolver;
    private final StorageKeyFactory storageKeyFactory;

    public LocalResourceFileStorage(ResourceStorageProperties properties, StorageKeyFactory storageKeyFactory) {
        this.rootPath = Path.of(properties.getLocalRoot()).toAbsolutePath().normalize();
        this.pathResolver = new LocalStoragePathResolver(rootPath);
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
        Path filePath = pathResolver.resolve(storageKey);

        Path parentPath = filePath.getParent();
        if (parentPath != null) {
            Files.createDirectories(parentPath);
        }
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return rootPath.relativize(filePath).toString().replace("\\", "/");
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(pathResolver.resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(pathResolver.resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey) throws IOException {
        return Files.exists(pathResolver.resolve(storageKey));
    }
}
