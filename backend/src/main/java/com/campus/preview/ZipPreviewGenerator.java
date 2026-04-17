package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;

import com.campus.dto.ResourceZipPreviewResponse;

public interface ZipPreviewGenerator {

    ResourceZipPreviewResponse generate(Long resourceId, String fileName, InputStream zipInputStream) throws IOException;
}
