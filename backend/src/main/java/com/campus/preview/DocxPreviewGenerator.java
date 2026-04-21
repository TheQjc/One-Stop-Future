package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;

public interface DocxPreviewGenerator {

    byte[] generate(InputStream docxInputStream) throws IOException;
}
