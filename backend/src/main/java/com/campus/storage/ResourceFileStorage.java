package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceFileStorage {

    String store(String originalFilename, InputStream inputStream) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey) throws IOException;
}
