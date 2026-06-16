package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FallbackResourceFileStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void opensHistoricalLocalFileWhenPrimaryObjectIsMissing() throws Exception {
        writeLocalFile("2026/04/17/resume.pdf", "legacy-pdf");
        FallbackResourceFileStorage storage = new FallbackResourceFileStorage(
                new MissingPrimaryStorage(),
                new HistoricalLocalResourceReader(tempDir.toString()));

        assertThat(storage.exists("2026/04/17/resume.pdf")).isTrue();
        assertThat(new String(storage.open("2026/04/17/resume.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("legacy-pdf");
    }

    @Test
    void writesNewFilesToPrimaryStorageOnly() throws Exception {
        RecordingPrimaryStorage primaryStorage = new RecordingPrimaryStorage();
        FallbackResourceFileStorage storage = new FallbackResourceFileStorage(
                primaryStorage,
                new HistoricalLocalResourceReader(tempDir.toString()));

        String key = storage.store("new.pdf", new ByteArrayInputStream("new-pdf".getBytes(StandardCharsets.UTF_8)));

        assertThat(key).isEqualTo("stored/new.pdf");
        assertThat(primaryStorage.storedBody).isEqualTo("new-pdf");
    }

    @Test
    void deleteRemovesPrimaryAndHistoricalLocalFile() throws Exception {
        writeLocalFile("2026/04/17/old.pdf", "legacy-pdf");
        RecordingPrimaryStorage primaryStorage = new RecordingPrimaryStorage();
        FallbackResourceFileStorage storage = new FallbackResourceFileStorage(
                primaryStorage,
                new HistoricalLocalResourceReader(tempDir.toString()));

        storage.delete("2026/04/17/old.pdf");

        assertThat(primaryStorage.deletedKey).isEqualTo("2026/04/17/old.pdf");
        assertThat(storage.exists("2026/04/17/old.pdf")).isFalse();
    }

    private void writeLocalFile(String storageKey, String body) throws IOException {
        Path path = tempDir.resolve(storageKey);
        Files.createDirectories(path.getParent());
        Files.writeString(path, body, StandardCharsets.UTF_8);
    }

    private static final class MissingPrimaryStorage implements ResourceFileStorage {

        @Override
        public String store(String originalFilename, InputStream inputStream) throws IOException {
            throw new IOException("unused");
        }

        @Override
        public InputStream open(String storageKey) throws IOException {
            throw new FileNotFoundException(storageKey);
        }

        @Override
        public void delete(String storageKey) {
        }

        @Override
        public boolean exists(String storageKey) {
            return false;
        }
    }

    private static final class RecordingPrimaryStorage implements ResourceFileStorage {

        private String storedBody;
        private String deletedKey;

        @Override
        public String store(String originalFilename, InputStream inputStream) throws IOException {
            storedBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return "stored/" + originalFilename;
        }

        @Override
        public InputStream open(String storageKey) throws IOException {
            throw new FileNotFoundException(storageKey);
        }

        @Override
        public void delete(String storageKey) {
            deletedKey = storageKey;
        }

        @Override
        public boolean exists(String storageKey) {
            return false;
        }
    }
}
