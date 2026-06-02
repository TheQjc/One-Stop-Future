package com.campus.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

/**
 * Validates file types by checking file header magic bytes, not just extensions.
 */
public final class FileMagicBytesValidator {

    // PDF files start with %PDF
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
    // ZIP-based formats (DOCX, PPTX, XLSX, ZIP) start with PK (0x50 0x4B)
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B};

    private static final Map<String, Set<byte[]>> ALLOWED_MAGIC_BYTES = Map.of(
            "pdf", Set.of(PDF_MAGIC),
            "docx", Set.of(ZIP_MAGIC),
            "pptx", Set.of(ZIP_MAGIC),
            "doc", Set.of(ZIP_MAGIC), // DOC can also be OLE2 CF, but we accept ZIP-based too
            "zip", Set.of(ZIP_MAGIC));

    private FileMagicBytesValidator() {
    }

    /**
     * Validates that the file content matches its claimed extension.
     *
     * @param file      the uploaded file
     * @param extension lowercase file extension (without dot)
     * @throws IOException if the file cannot be read
     */
    public static void validateMultipartFile(MultipartFile file, String extension) throws IOException {
        Set<byte[]> expectedMagic = ALLOWED_MAGIC_BYTES.get(extension);
        if (expectedMagic == null) {
            return; // unknown extension, skip magic byte check
        }

        byte[] header = readHeader(file);
        for (byte[] magic : expectedMagic) {
            if (startsWith(header, magic)) {
                return; // valid
            }
        }

        throw new IllegalArgumentException(
                "file content does not match extension ." + extension);
    }

    /**
     * Validates that the input stream content matches the claimed extension.
     * Does NOT close the stream.
     *
     * @param inputStream the file input stream (must support mark/reset)
     * @param extension   lowercase file extension (without dot)
     * @throws IOException if the file cannot be read
     */
    public static void validateStream(InputStream inputStream, String extension) throws IOException {
        Set<byte[]> expectedMagic = ALLOWED_MAGIC_BYTES.get(extension);
        if (expectedMagic == null) {
            return;
        }

        if (!inputStream.markSupported()) {
            return; // cannot validate without mark/reset support
        }

        byte[] header = readHeaderFromStream(inputStream);
        for (byte[] magic : expectedMagic) {
            if (startsWith(header, magic)) {
                return;
            }
        }

        throw new IllegalArgumentException(
                "file content does not match extension ." + extension);
    }

    private static byte[] readHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            if (read < 4) {
                return new byte[0];
            }
        }
        return header;
    }

    private static byte[] readHeaderFromStream(InputStream inputStream) throws IOException {
        byte[] header = new byte[8];
        inputStream.mark(8);
        int read = inputStream.read(header);
        inputStream.reset();
        if (read < 4) {
            return new byte[0];
        }
        return header;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
