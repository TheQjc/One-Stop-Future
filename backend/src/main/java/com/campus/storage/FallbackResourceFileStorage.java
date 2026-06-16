package com.campus.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class FallbackResourceFileStorage implements ResourceFileStorage {

    private final ResourceFileStorage primaryStorage;
    private final HistoricalLocalResourceReader localFallbackReader;

    public FallbackResourceFileStorage(
            ResourceFileStorage primaryStorage,
            HistoricalLocalResourceReader localFallbackReader) {
        this.primaryStorage = Objects.requireNonNull(primaryStorage, "primaryStorage");
        this.localFallbackReader = Objects.requireNonNull(localFallbackReader, "localFallbackReader");
    }

    @Override
    public String store(String originalFilename, InputStream inputStream) throws IOException {
        return primaryStorage.store(originalFilename, inputStream);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            return primaryStorage.open(storageKey);
        } catch (FileNotFoundException exception) {
            return localFallbackReader.open(storageKey);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        IOException failure = null;
        try {
            primaryStorage.delete(storageKey);
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            localFallbackReader.delete(storageKey);
        } catch (IOException exception) {
            if (failure != null) {
                failure.addSuppressed(exception);
            } else {
                failure = exception;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public boolean exists(String storageKey) throws IOException {
        if (primaryStorage.exists(storageKey)) {
            return true;
        }
        return localFallbackReader.exists(storageKey);
    }
}
