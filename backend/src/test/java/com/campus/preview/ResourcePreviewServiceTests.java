package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.campus.common.BusinessException;
import com.campus.dto.ResourceZipPreviewResponse;
import com.campus.entity.ResourceItem;
import com.fasterxml.jackson.databind.ObjectMapper;

class ResourcePreviewServiceTests {

    @Test
    void fingerprintChangesWhenStorageKeyOrUpdatedAtChanges() {
        LocalDateTime now = LocalDateTime.now();
        ResourceItem first = resource(9L, "deck-a.pptx", "pptx", "seed/a.pptx", 1024L, now);
        ResourceItem second = resource(9L, "deck-a.pptx", "pptx", "seed/b.pptx", 1024L, now.plusMinutes(1));
        ResourcePreviewService service = new ResourcePreviewService(new NoopStorage(), new ObjectMapper(),
                new NoopPptxPreviewGenerator(), new NoopDocxPreviewGenerator(),
                new CountingZipPreviewGenerator(payload("seed/", "seed/a.txt")));

        assertThat(service.fingerprintOf(first)).isNotEqualTo(service.fingerprintOf(second));
    }

    @Test
    void zipPreviewReusesCachedArtifactUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingZipPreviewGenerator generator = new CountingZipPreviewGenerator(payload("resume/", "resume/a.md"));
        ResourcePreviewService service = new ResourcePreviewService(storage, new ObjectMapper(),
                new NoopPptxPreviewGenerator(), new NoopDocxPreviewGenerator(), generator);
        ResourceItem resource = resource(9L, "resume.zip", "zip", "seed/resume.zip", 1024L, LocalDateTime.now());

        service.previewZip(resource, this::sampleZipStream);
        service.previewZip(resource, this::sampleZipStream);

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void pptxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingPptxPreviewGenerator generator = new CountingPptxPreviewGenerator();
        ResourcePreviewService service = new ResourcePreviewService(storage, new ObjectMapper(), generator,
                new NoopDocxPreviewGenerator(), new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
        ResourceItem resource = resource(9L, "career-deck.pptx", "pptx", "seed/career-deck.pptx", 1024L,
                LocalDateTime.now());

        service.previewFile(resource, this::samplePptxStream);
        service.previewFile(resource, this::samplePptxStream);

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ResourcePreviewService service = new ResourcePreviewService(storage, new ObjectMapper(),
                new NoopPptxPreviewGenerator(), generator,
                new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
        ResourceItem resource = resource(9L, "writing-workbook.docx", "docx", "seed/workbook.docx", 1024L,
                LocalDateTime.now());

        service.previewDocx(resource, this::sampleDocxStream);
        service.previewDocx(resource, this::sampleDocxStream);

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewFailureBecomesBusinessException() {
        ResourcePreviewService service = new ResourcePreviewService(new NoopStorage(), new ObjectMapper(),
                new NoopPptxPreviewGenerator(), inputStream -> {
                    throw new IOException("boom");
                }, new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
        ResourceItem resource = resource(9L, "writing-workbook.docx", "docx", "seed/workbook.docx", 1024L,
                LocalDateTime.now());

        assertThatThrownBy(() -> service.previewDocx(resource, this::sampleDocxStream))
                .isInstanceOf(BusinessException.class)
                .hasMessage("docx preview unavailable");
    }

    private static class NoopStorage implements ResourcePreviewArtifactStorage {

        @Override
        public boolean exists(String artifactKey) {
            return false;
        }

        @Override
        public InputStream open(String artifactKey) throws IOException {
            throw new IOException("not implemented");
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) throws IOException {
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
                throw new IOException("artifact not found");
            }
            return new ByteArrayInputStream(artifact);
        }

        @Override
        public void write(String artifactKey, InputStream inputStream) throws IOException {
            artifacts.put(artifactKey, inputStream.readAllBytes());
        }
    }

    private static class CountingZipPreviewGenerator implements ZipPreviewGenerator {

        private final ResourceZipPreviewResponse response;
        private int invocationCount;

        private CountingZipPreviewGenerator(ResourceZipPreviewResponse response) {
            this.response = response;
        }

        @Override
        public ResourceZipPreviewResponse generate(Long resourceId, String fileName, InputStream zipInputStream)
                throws IOException {
            invocationCount++;
            return new ResourceZipPreviewResponse(resourceId, fileName, response.entryCount(), response.entries());
        }

        int invocationCount() {
            return invocationCount;
        }
    }

    private static class NoopPptxPreviewGenerator implements PptxPreviewGenerator {

        @Override
        public byte[] generate(InputStream pptxInputStream) throws IOException {
            return new byte[0];
        }
    }

    private static class NoopDocxPreviewGenerator implements DocxPreviewGenerator {

        @Override
        public byte[] generate(InputStream docxInputStream) throws IOException {
            return new byte[0];
        }
    }

    private static class CountingPptxPreviewGenerator implements PptxPreviewGenerator {

        private int invocationCount;

        @Override
        public byte[] generate(InputStream pptxInputStream) throws IOException {
            invocationCount++;
            return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        }

        int invocationCount() {
            return invocationCount;
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

    private ResourceItem resource(Long id, String fileName, String fileExt, String storageKey, Long fileSize,
            LocalDateTime updatedAt) {
        ResourceItem resource = new ResourceItem();
        resource.setId(id);
        resource.setFileName(fileName);
        resource.setFileExt(fileExt);
        resource.setStorageKey(storageKey);
        resource.setFileSize(fileSize);
        resource.setUpdatedAt(updatedAt);
        return resource;
    }

    private ResourceZipPreviewResponse payload(String firstPath, String secondPath) {
        List<ResourceZipPreviewResponse.Entry> entries = List.of(
                new ResourceZipPreviewResponse.Entry(firstPath, "resume", true, null),
                new ResourceZipPreviewResponse.Entry(secondPath, "a.md", false, 12L));
        return new ResourceZipPreviewResponse(9L, "resume.zip", entries.size(), entries);
    }

    private InputStream sampleZipStream() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("resume/"));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("resume/a.md"));
            zipOutputStream.write("resume".getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private InputStream samplePptxStream() {
        return new ByteArrayInputStream("pptx".getBytes(StandardCharsets.UTF_8));
    }

    private InputStream sampleDocxStream() {
        return new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8));
    }
}
