package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class HistoricalLocalResourceReader {

    private final LocalStoragePathResolver pathResolver;

    public HistoricalLocalResourceReader(String localRoot) {
        this.pathResolver = new LocalStoragePathResolver(Path.of(localRoot));
    }

    public boolean exists(String storageKey) {
        return Files.exists(pathResolver.resolve(storageKey));
    }

    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(pathResolver.resolve(storageKey));
    }
}
