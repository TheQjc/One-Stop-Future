package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MinioResourceFileStorage implements ResourceFileStorage {

    private final String bucketName;
    private final MinioObjectOperations operations;
    private final StorageKeyFactory keyFactory;

    public MinioResourceFileStorage(String bucketName, MinioObjectOperations operations, StorageKeyFactory keyFactory)
            throws IOException {
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName");
        this.operations = Objects.requireNonNull(operations, "operations");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        ensureBucket();
    }

    @Override
    public String store(String originalFilename, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");

        String storageKey = keyFactory.newStorageKey(originalFilename);
        operations.putObject(bucketName, storageKey, inputStream);
        return storageKey;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return operations.getObject(bucketName, normalizeKey(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        operations.removeObject(bucketName, normalizeKey(storageKey));
    }

    @Override
    public boolean exists(String storageKey) throws IOException {
        return operations.objectExists(bucketName, normalizeKey(storageKey));
    }

    private void ensureBucket() throws IOException {
        if (!operations.bucketExists(bucketName)) {
            operations.createBucket(bucketName);
        }
    }

    private String normalizeKey(String storageKey) {
        return storageKey.replace("\\", "/");
    }
}
