package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.campus.common.BusinessException;
import com.campus.common.ResourcePreviewKind;
import com.campus.entity.JobApplication;

class ApplicationSnapshotPreviewServiceTests {

    @Test
    void previewKindOfReturnsFileForPdfAndDocxButNotDoc() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());

        assertThat(service.previewKindOf("pdf", "application/pdf")).isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf("docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf("doc", "application/msword")).isEqualTo(ResourcePreviewKind.NONE);
    }

    @Test
    void docxArtifactKeyUsesSnapshotFieldsAndIgnoresUpdatedAt() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication first = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 22, 11, 0), LocalDateTime.of(2026, 4, 22, 12, 0));
        JobApplication second = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 22, 11, 0), LocalDateTime.of(2026, 5, 1, 8, 0));
        JobApplication changedSubmittedAt = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 23, 11, 0), LocalDateTime.of(2026, 5, 1, 8, 0));

        assertThat(service.docxArtifactKeyOf(first)).isEqualTo(service.docxArtifactKeyOf(second));
        assertThat(service.docxArtifactKeyOf(first)).isNotEqualTo(service.docxArtifactKeyOf(changedSubmittedAt));
    }

    @Test
    void previewPdfReturnsRawSnapshotStream() throws IOException {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication application = application(41L, "resume.pdf", "pdf", "seed/snapshots/a.pdf", 256L,
                LocalDateTime.now(), LocalDateTime.now());

        ApplicationSnapshotPreviewService.PreviewFile preview = service.preview(application,
                () -> new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(preview.fileName()).isEqualTo("resume.pdf");
        assertThat(preview.contentType()).isEqualTo("application/pdf");
        assertThat(new String(preview.inputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("%PDF");
    }

    @Test
    void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(storage, generator);
        JobApplication application = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.now(), LocalDateTime.now());

        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));
        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewCacheMissWritesArtifact() throws IOException {
        MissingOnOpenStorage storage = new MissingOnOpenStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(storage, generator);
        JobApplication application = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.now(), LocalDateTime.now());

        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.writtenKeys()).containsExactly(service.docxArtifactKeyOf(application));
    }

    @Test
    void unsupportedPreviewTypeBecomesBusinessException() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication application = application(41L, "resume.doc", "doc", "seed/snapshots/a.doc", 256L,
                LocalDateTime.now(), LocalDateTime.now());

        assertThatThrownBy(() -> service.preview(application,
                () -> new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("application resume preview only supports pdf or docx");
    }

    @Test
    void docxPreviewFailureBecomesBusinessException() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(new NoopStorage(),
                inputStream -> {
                    throw new IOException("boom");
                });
        JobApplication application = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 256L,
                LocalDateTime.now(), LocalDateTime.now());

        assertThatThrownBy(() -> service.preview(application,
                () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("application resume preview unavailable");
    }

    private static class NoopStorage implements ResourcePreviewArtifactStorage {

        @Override
        public boolean exists(String artifactKey) throws IOException {
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

        @Override
        public void delete(String artifactKey) throws IOException {
            throw new IOException("not implemented");
        }
    }

    private static class InMemoryStorage implements ResourcePreviewArtifactStorage {

        private final Map<String, byte[]> artifacts = new LinkedHashMap<>();

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

        @Override
        public void delete(String artifactKey) {
            artifacts.remove(artifactKey);
        }
    }

    private static class MissingOnOpenStorage implements ResourcePreviewArtifactStorage {

        private final List<String> writtenKeys = new ArrayList<>();

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
            writtenKeys.add(artifactKey);
            inputStream.readAllBytes();
        }

        @Override
        public void delete(String artifactKey) {
        }

        private List<String> writtenKeys() {
            return writtenKeys;
        }
    }

    private static class CountingDocxPreviewGenerator implements DocxPreviewGenerator {

        private int invocationCount;

        @Override
        public byte[] generate(InputStream docxInputStream) throws IOException {
            invocationCount++;
            return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        }

        int invocationCount() {
            return invocationCount;
        }
    }

    private JobApplication application(Long id, String fileName, String fileExt, String storageKey, Long fileSize,
            LocalDateTime submittedAt, LocalDateTime updatedAt) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setResumeFileNameSnapshot(fileName);
        application.setResumeFileExtSnapshot(fileExt);
        application.setResumeStorageKeySnapshot(storageKey);
        application.setResumeFileSizeSnapshot(fileSize);
        application.setSubmittedAt(submittedAt);
        application.setUpdatedAt(updatedAt);
        if ("pdf".equalsIgnoreCase(fileExt)) {
            application.setResumeContentTypeSnapshot("application/pdf");
        } else if ("docx".equalsIgnoreCase(fileExt)) {
            application.setResumeContentTypeSnapshot(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if ("doc".equalsIgnoreCase(fileExt)) {
            application.setResumeContentTypeSnapshot("application/msword");
        }
        return application;
    }
}
