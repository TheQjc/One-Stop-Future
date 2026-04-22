package com.campus.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.dto.ResourceZipPreviewResponse;
import com.campus.entity.ResourceItem;
import com.campus.mapper.ResourceItemMapper;
import com.campus.preview.DocxPreviewGenerator;
import com.campus.preview.ResourcePreviewService;
import com.campus.storage.MinioObjectOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "app.resource-preview.type=minio",
        "app.resource-preview.read-fallback-local-enabled=true",
        "platform.integrations.minio.enabled=true",
        "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
        "platform.integrations.minio.access-key=minioadmin",
        "platform.integrations.minio.secret-key=minioadmin",
        "platform.integrations.minio.bucket=campus-platform"
})
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResourceControllerPreviewFallbackTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");
    private static final Path PREVIEW_ROOT = Path.of(".local-storage", "previews");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceItemMapper resourceItemMapper;

    @Autowired
    private ResourcePreviewService resourcePreviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MinioObjectOperations minioObjectOperations;

    @MockBean
    private DocxPreviewGenerator docxPreviewGenerator;

    @BeforeEach
    void setUp() throws IOException {
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(docxPreviewGenerator.generate(any())).thenReturn(samplePdfBytes());
    }

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
        deleteTreeIfExists(PREVIEW_ROOT);
    }

    @Test
    void guestCanPreviewPublishedPptxFromHistoricalLocalFallbackWhenMinioObjectIsMissing() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");

        ResourceItem resource = resourceItemMapper.selectById(4L);
        String artifactKey = resourcePreviewService.pptxArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, samplePdfBytes());

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new FileNotFoundException("missing"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedDocxFromHistoricalLocalFallbackWhenMinioObjectIsMissing() throws Exception {
        insertResource(5L, 2L, "PUBLISHED", null, "career-guide.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "seed/2026/04/career-guide.docx");

        ResourceItem resource = resourceItemMapper.selectById(5L);
        String artifactKey = resourcePreviewService.docxArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, samplePdfBytes());

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new FileNotFoundException("missing"));

        mockMvc.perform(get("/api/resources/5/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedZipTreeFromHistoricalLocalFallbackWhenMinioObjectIsMissing() throws Exception {
        ResourceItem resource = resourceItemMapper.selectById(2L);
        String artifactKey = resourcePreviewService.zipArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, objectMapper.writeValueAsBytes(
                new ResourceZipPreviewResponse(
                        2L,
                        "interview-experience-notes.zip",
                        1,
                        List.of(new ResourceZipPreviewResponse.Entry("notes/", "notes", true, null)))));

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new FileNotFoundException("missing"));

        mockMvc.perform(get("/api/resources/2/preview-zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("interview-experience-notes.zip"))
                .andExpect(jsonPath("$.data.entryCount").value(1));
    }

    @Test
    void minioFailureIsNotMaskedByHistoricalLocalFallback() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");

        ResourceItem resource = resourceItemMapper.selectById(4L);
        String artifactKey = resourcePreviewService.pptxArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, samplePdfBytes());

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new IOException("boom"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("pptx preview unavailable"));
    }

    private void writePreviewArtifact(String artifactKey, byte[] content) throws IOException {
        Path artifactPath = PREVIEW_ROOT.resolve(artifactKey);
        Files.createDirectories(artifactPath.getParent());
        Files.write(artifactPath, content);
    }

    private byte[] samplePdfBytes() {
        return """
                %PDF-1.4
                1 0 obj
                <<>>
                endobj
                trailer
                <<>>
                %%EOF
                """.getBytes(StandardCharsets.UTF_8);
    }

    private void deleteTreeIfExists(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }

    private void insertResource(Long id, Long uploaderId, String status, String rejectReason, String fileName,
            String fileExt, String contentType, String storageKey) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO t_resource_item (
                          id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                          file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                          published_at, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                "Inserted Resource " + id,
                "RESUME_TEMPLATE",
                "Inserted resource summary",
                "Inserted resource description",
                status,
                uploaderId,
                "REJECTED".equals(status) ? 1L : null,
                rejectReason,
                fileName,
                fileExt,
                contentType,
                2048L,
                storageKey,
                0,
                0,
                "PUBLISHED".equals(status) ? now.minusHours(2) : null,
                "PUBLISHED".equals(status) || "REJECTED".equals(status) || "OFFLINE".equals(status) ? now.minusHours(2)
                        : null,
                now.plusSeconds(id),
                now.plusSeconds(id + 1));
    }
}
