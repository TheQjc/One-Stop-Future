package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;

public interface PptxPreviewGenerator {

    byte[] generate(InputStream pptxInputStream) throws IOException;
}
