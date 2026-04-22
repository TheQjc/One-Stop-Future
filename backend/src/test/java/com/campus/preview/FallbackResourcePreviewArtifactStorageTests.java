package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FallbackResourcePreviewArtifactStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void openReturnsPrimaryArtifactWhenPrimaryHitExists() throws Exception {
        RecordingPrimaryStorage primary = new RecordingPrimaryStorage();
        primary.put("pptx/9/fingerprint.pdf", "%PDF-primary");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                primary,
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThat(new String(
                storage.open("pptx/9/fingerprint.pdf").readAllBytes(),
                StandardCharsets.UTF_8)).isEqualTo("%PDF-primary");
    }

    @Test
    void openFallsBackToHistoricalLocalWhenPrimaryReportsFileNotFound() throws Exception {
        Path localArtifact = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(localArtifact.getParent());
        Files.writeString(localArtifact, "%PDF-local");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                new MissingPrimaryStorage(),
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThat(new String(
                storage.open("pptx/9/fingerprint.pdf").readAllBytes(),
                StandardCharsets.UTF_8)).isEqualTo("%PDF-local");
    }

    @Test
    void openDoesNotFallbackWhenPrimaryFailsWithIoError() throws Exception {
        Path localArtifact = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(localArtifact.getParent());
        Files.writeString(localArtifact, "%PDF-local");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                new BrokenPrimaryStorage(new IOException("boom")),
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThatThrownBy(() -> storage.open("pptx/9/fingerprint.pdf"))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void openPropagatesFileNotFoundWhenBothBackendsMiss() {
        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                new MissingPrimaryStorage(),
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThatThrownBy(() -> storage.open("pptx/9/fingerprint.pdf"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void writeDelegatesToPrimaryOnly() throws Exception {
        RecordingPrimaryStorage primary = new RecordingPrimaryStorage();
        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                primary,
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        storage.write("pptx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(primary.stored("pptx/9/fingerprint.pdf")).isEqualTo("%PDF");
        assertThat(Files.exists(tempDir.resolve("pptx/9/fingerprint.pdf"))).isFalse();
    }

    private static class RecordingPrimaryStorage implements ResourcePreviewArtifactStorage {

        private final Map<String, byte[]> artifacts = new LinkedHashMap<>();

        void put(String artifactKey, String value) {
            artifacts.put(artifactKey, value.getBytes(StandardCharsets.UTF_8));
        }

        String stored(String artifactKey) {
            byte[] artifact = artifacts.get(artifactKey);
            return artifact == null ? null : new String(artifact, StandardCharsets.UTF_8);
        }

        @Override
        public boolean exists(String artifactKey) {
            return artifacts.containsKey(artifactKey);
        }

        @Override
        public InputStream open(String artifactKey) throws IOException {
            byte[] artifact = artifacts.get(artifactKey);
            if (artifact == null) {
                throw new FileNotFoundException(artifactKey);
            }
            return new ByteArrayInputStream(artifact);
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) throws IOException {
            artifacts.put(artifactKey, inputStream.readAllBytes());
        }
    }

    private static class MissingPrimaryStorage implements ResourcePreviewArtifactStorage {

        @Override
        public boolean exists(String artifactKey) {
            return false;
        }

        @Override
        public InputStream open(String artifactKey) throws IOException {
            throw new FileNotFoundException(artifactKey);
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) throws IOException {
            throw new IOException("not implemented");
        }
    }

    private static class BrokenPrimaryStorage implements ResourcePreviewArtifactStorage {

        private final IOException exception;

        private BrokenPrimaryStorage(IOException exception) {
            this.exception = exception;
        }

        @Override
        public boolean exists(String artifactKey) throws IOException {
            throw exception;
        }

        @Override
        public InputStream open(String artifactKey) throws IOException {
            throw exception;
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) throws IOException {
            throw exception;
        }
    }
}
