package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

public interface MinioObjectOperations {

    boolean bucketExists(String bucketName) throws IOException;

    void createBucket(String bucketName) throws IOException;

    void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException;

    InputStream getObject(String bucketName, String objectKey) throws IOException;

    boolean objectExists(String bucketName, String objectKey) throws IOException;

    void removeObject(String bucketName, String objectKey) throws IOException;
}
