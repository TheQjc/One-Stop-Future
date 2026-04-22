package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;

public interface ResourcePreviewArtifactStorage {

    boolean exists(String artifactKey) throws IOException;

    InputStream open(String artifactKey) throws IOException;

    void write(String artifactKey, InputStream inputStream) throws IOException;

    void delete(String artifactKey) throws IOException;
}
