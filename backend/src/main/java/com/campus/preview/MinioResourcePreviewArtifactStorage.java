package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.campus.storage.MinioObjectOperations;

public class MinioResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final String bucketName;
    private final String minioPrefix;
    private final MinioObjectOperations operations;

    public MinioResourcePreviewArtifactStorage(String bucketName, String minioPrefix, MinioObjectOperations operations)
            throws IOException {
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName");
        this.minioPrefix = normalizePrefix(minioPrefix);
        this.operations = Objects.requireNonNull(operations, "operations");
        ensureBucket();
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        return operations.objectExists(bucketName, objectKeyOf(artifactKey));
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        return operations.getObject(bucketName, objectKeyOf(artifactKey));
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        operations.putObject(bucketName, objectKeyOf(artifactKey), inputStream);
    }

    @Override
    public void delete(String artifactKey) throws IOException {
        operations.removeObject(bucketName, objectKeyOf(artifactKey));
    }

    private void ensureBucket() throws IOException {
        if (!operations.bucketExists(bucketName)) {
            operations.createBucket(bucketName);
        }
    }

    private String objectKeyOf(String artifactKey) {
        return minioPrefix + "/" + normalizeArtifactKey(artifactKey);
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("preview minio prefix is blank");
        }
        String normalized = value.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("preview minio prefix is blank");
        }
        return normalized;
    }

    private String normalizeArtifactKey(String artifactKey) {
        if (artifactKey == null || artifactKey.isBlank()) {
            throw new IllegalArgumentException("artifact key is blank");
        }
        String normalized = artifactKey.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("artifact key is blank");
        }
        if (normalized.equals("..") || normalized.contains("../")) {
            throw new IllegalArgumentException("artifact key escapes preview minio prefix");
        }
        return normalized;
    }
}
