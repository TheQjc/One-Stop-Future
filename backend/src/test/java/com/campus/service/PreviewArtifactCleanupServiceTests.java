package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.campus.config.ResourcePreviewProperties;
import com.campus.preview.HistoricalLocalResourcePreviewArtifactCleaner;
import com.campus.preview.ResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewService;

class PreviewArtifactCleanupServiceTests {

    @Test
    void skipsWhenOldTargetIsMissing() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("local", storage, cleaner);

        service.cleanupAfterResourceMutation(Optional.empty(), Optional.empty());

        assertThat(storage.deletedKeys()).isEmpty();
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void skipsWhenOldAndNewTargetsMatch() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("minio", storage, cleaner);
        ResourcePreviewService.PreviewArtifactTarget target =
                new ResourcePreviewService.PreviewArtifactTarget("PPTX", "pptx/9/fingerprint.pdf");

        service.cleanupAfterResourceMutation(Optional.of(target), Optional.of(target));

        assertThat(storage.deletedKeys()).isEmpty();
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void minioModeDeletesActiveAndHistoricalCopiesWhenTargetChanges() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("minio", storage, cleaner);

        service.cleanupAfterResourceMutation(
                Optional.of(new ResourcePreviewService.PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.of(new ResourcePreviewService.PreviewArtifactTarget("PPTX", "pptx/9/new.pdf")));

        assertThat(storage.deletedKeys()).containsExactly("pptx/9/old.pdf");
        assertThat(cleaner.deletedKeys()).containsExactly("pptx/9/old.pdf");
    }

    @Test
    void localModeDeletesOnlyActiveCopyWhenPreviewSupportDisappears() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("local", storage, cleaner);

        service.cleanupAfterResourceMutation(
                Optional.of(new ResourcePreviewService.PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.empty());

        assertThat(storage.deletedKeys()).containsExactly("pptx/9/old.pdf");
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void deleteFailuresAreSwallowed() {
        PreviewArtifactCleanupService service = service(
                "minio",
                new BrokenStorage(new IOException("boom")),
                new BrokenHistoricalCleaner(new IOException("boom")));

        assertThatCode(() -> service.cleanupAfterResourceMutation(
                Optional.of(new ResourcePreviewService.PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.empty())).doesNotThrowAnyException();
    }

    private PreviewArtifactCleanupService service(String previewType, ResourcePreviewArtifactStorage storage,
            HistoricalLocalResourcePreviewArtifactCleaner cleaner) {
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setType(previewType);
        return new PreviewArtifactCleanupService(storage, cleaner, properties);
    }

    private static class RecordingStorage implements ResourcePreviewArtifactStorage {

        private final List<String> deletedKeys = new ArrayList<>();

        List<String> deletedKeys() {
            return deletedKeys;
        }

        @Override
        public boolean exists(String artifactKey) {
            return false;
        }

        @Override
        public InputStream open(String artifactKey) throws IOException {
            throw new IOException("unused");
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) {
        }

        @Override
        public void delete(String artifactKey) {
            deletedKeys.add(artifactKey);
        }
    }

    private static class BrokenStorage implements ResourcePreviewArtifactStorage {

        private final IOException exception;

        private BrokenStorage(IOException exception) {
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

        @Override
        public void delete(String artifactKey) throws IOException {
            throw exception;
        }
    }

    private static class RecordingHistoricalCleaner extends HistoricalLocalResourcePreviewArtifactCleaner {

        private final List<String> deletedKeys = new ArrayList<>();

        private RecordingHistoricalCleaner() {
            super(properties());
        }

        List<String> deletedKeys() {
            return deletedKeys;
        }

        @Override
        public void delete(String artifactKey) {
            deletedKeys.add(artifactKey);
        }
    }

    private static class BrokenHistoricalCleaner extends HistoricalLocalResourcePreviewArtifactCleaner {

        private final IOException exception;

        private BrokenHistoricalCleaner(IOException exception) {
            super(properties());
            this.exception = exception;
        }

        @Override
        public void delete(String artifactKey) throws IOException {
            throw exception;
        }
    }

    private static ResourcePreviewProperties properties() {
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setLocalRoot(System.getProperty("java.io.tmpdir"));
        return properties;
    }
}
