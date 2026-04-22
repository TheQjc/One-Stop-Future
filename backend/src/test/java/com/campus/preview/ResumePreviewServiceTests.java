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
import com.campus.entity.Resume;

class ResumePreviewServiceTests {

    @Test
    void previewKindOfReturnsFileForPdfAndDocxButNotDoc() {
        ResumePreviewService service = new ResumePreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());

        assertThat(service.previewKindOf(resume(1L, "resume.pdf", "pdf", "seed/a.pdf", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf(resume(2L, "resume.docx", "docx", "seed/a.docx", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf(resume(3L, "resume.doc", "doc", "seed/a.doc", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.NONE);
    }

    @Test
    void previewArtifactTargetOfReturnsDocxLogicalArtifactKeyOnly() {
        ResumePreviewService service = new ResumePreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        Resume docx = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());
        Resume pdf = resume(10L, "intern.pdf", "pdf", "seed/intern.pdf", 1024L, LocalDateTime.now());

        assertThat(service.previewArtifactTargetOf(docx))
                .contains(new ResumePreviewService.PreviewArtifactTarget(
                        "DOCX",
                        service.docxArtifactKeyOf(docx)));
        assertThat(service.previewArtifactTargetOf(pdf)).isEmpty();
    }

    @Test
    void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ResumePreviewService service = new ResumePreviewService(storage, generator);
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));
        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewCacheMissWritesArtifact() throws IOException {
        MissingOnOpenStorage storage = new MissingOnOpenStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ResumePreviewService service = new ResumePreviewService(storage, generator);
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.writtenKeys()).containsExactly(service.docxArtifactKeyOf(resume));
    }

    @Test
    void docxPreviewFailureBecomesBusinessException() {
        ResumePreviewService service = new ResumePreviewService(new NoopStorage(), inputStream -> {
            throw new IOException("boom");
        });
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        assertThatThrownBy(() -> service.previewDocx(resume,
                () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("resume preview unavailable");
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

    private Resume resume(Long id, String fileName, String fileExt, String storageKey, Long fileSize,
            LocalDateTime updatedAt) {
        Resume resume = new Resume();
        resume.setId(id);
        resume.setFileName(fileName);
        resume.setFileExt(fileExt);
        resume.setStorageKey(storageKey);
        resume.setFileSize(fileSize);
        resume.setUpdatedAt(updatedAt);
        if ("pdf".equalsIgnoreCase(fileExt)) {
            resume.setContentType("application/pdf");
        } else if ("docx".equalsIgnoreCase(fileExt)) {
            resume.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if ("doc".equalsIgnoreCase(fileExt)) {
            resume.setContentType("application/msword");
        }
        return resume;
    }
}
