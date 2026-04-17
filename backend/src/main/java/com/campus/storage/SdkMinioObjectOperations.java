package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;

public class SdkMinioObjectOperations implements MinioObjectOperations {

    private static final long PART_SIZE = 10L * 1024L * 1024L;

    private final MinioClient client;

    public SdkMinioObjectOperations(MinioClient client) {
        this.client = client;
    }

    @Override
    public boolean bucketExists(String bucketName) throws IOException {
        try {
            return client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception exception) {
            throw asIoException("failed to check minio bucket existence", exception);
        }
    }

    @Override
    public void createBucket(String bucketName) throws IOException {
        try {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception exception) {
            throw asIoException("failed to create minio bucket", exception);
        }
    }

    @Override
    public void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, -1, PART_SIZE)
                    .contentType("application/octet-stream")
                    .build());
        } catch (Exception exception) {
            throw asIoException("failed to put minio object", exception);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectKey) throws IOException {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw asIoException("failed to get minio object", exception);
        }
    }

    @Override
    public boolean objectExists(String bucketName, String objectKey) throws IOException {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException exception) {
            String code = exception.errorResponse() == null ? null : exception.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return false;
            }
            throw asIoException("failed to stat minio object", exception);
        } catch (Exception exception) {
            throw asIoException("failed to stat minio object", exception);
        }
    }

    @Override
    public void removeObject(String bucketName, String objectKey) throws IOException {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw asIoException("failed to remove minio object", exception);
        }
    }

    private IOException asIoException(String message, Exception exception) {
        if (exception instanceof IOException ioException) {
            return ioException;
        }
        return new IOException(message, exception);
    }
}
