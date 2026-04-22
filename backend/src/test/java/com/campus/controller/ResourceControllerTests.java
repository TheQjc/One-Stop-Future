package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.entity.ResourceItem;
import com.campus.mapper.ResourceItemMapper;
import com.campus.preview.DocxPreviewGenerator;
import com.campus.preview.ResourcePreviewService;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResourceControllerTests {

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

    @MockBean
    private DocxPreviewGenerator docxPreviewGenerator;

    @BeforeEach
    void stubDocxPreviewGenerator() throws IOException {
        when(docxPreviewGenerator.generate(any())).thenReturn(samplePdfBytes());
    }

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
        deleteTreeIfExists(PREVIEW_ROOT);
    }

    @Test
    void guestCanReadPublishedResourcesAndFilterByCategory() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.resources[0].title").isNotEmpty());

        mockMvc.perform(get("/api/resources").param("category", "RESUME_TEMPLATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.category").value("RESUME_TEMPLATE"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.resources[0].category").value("RESUME_TEMPLATE"));
    }

    @Test
    void guestCanReadPublishedDetailOnly() throws Exception {
        mockMvc.perform(get("/api/resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("resume-template-pack.pdf"))
                .andExpect(jsonPath("$.data.previewAvailable").value(true))
                .andExpect(jsonPath("$.data.previewKind").value("FILE"));

        mockMvc.perform(get("/api/resources/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resource not found"));
    }

    @Test
    void publishedZipDetailExposesZipPreviewKind() throws Exception {
        mockMvc.perform(get("/api/resources/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.previewAvailable").value(true))
                .andExpect(jsonPath("$.data.previewKind").value("ZIP_TREE"));
    }

    @Test
    void publishedDocxDetailExposesFilePreviewKind() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-guide.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "seed/2026/04/career-guide.docx");

        mockMvc.perform(get("/api/resources/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.previewAvailable").value(true))
                .andExpect(jsonPath("$.data.previewKind").value("FILE"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanReadRejectedResourceDetailAndSeeLifecycleFlags() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");

        mockMvc.perform(get("/api/resources/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("Please simplify the intro section"))
                .andExpect(jsonPath("$.data.editableByMe").value(true))
                .andExpect(jsonPath("$.data.previewAvailable").value(true))
                .andExpect(jsonPath("$.data.previewKind").value("FILE"));
    }

    @Test
    void guestCannotFavoriteOrDownloadResource() throws Exception {
        mockMvc.perform(post("/api/resources/1/favorite"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/resources/1/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanFavoriteAndUnfavoriteResourceIdempotently() throws Exception {
        mockMvc.perform(post("/api/resources/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true));

        mockMvc.perform(post("/api/resources/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true));

        Integer favoriteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_favorite WHERE user_id = 2 AND target_type = 'RESOURCE' AND target_id = 1",
                Integer.class);
        assertThat(favoriteCount).isEqualTo(1);

        mockMvc.perform(delete("/api/resources/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanDownloadPublishedResource() throws Exception {
        writeStoredFile("seed/2026/04/resume-template-pack.pdf", "resource-file");

        mockMvc.perform(get("/api/resources/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("resume-template-pack.pdf")));

        Integer downloadCount = jdbcTemplate.queryForObject(
                "SELECT download_count FROM t_resource_item WHERE id = 1", Integer.class);
        assertThat(downloadCount).isEqualTo(13);
    }

    @Test
    void guestCanPreviewPublishedPdfInline() throws Exception {
        writeStoredFile("seed/2026/04/resume-template-pack.pdf", "pdf");

        mockMvc.perform(get("/api/resources/1/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedPptxAsInlinePdf() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");
        writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedDocxAsInlinePdf() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-guide.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "seed/2026/04/career-guide.docx");
        writeStoredBinaryFile("seed/2026/04/career-guide.docx", "docx".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedZipDirectory() throws Exception {
        writeStoredBinaryFile("seed/2026/04/interview-experience-notes.zip", sampleZipBytes());

        mockMvc.perform(get("/api/resources/2/preview-zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("interview-experience-notes.zip"))
                .andExpect(jsonPath("$.data.entries[0].path").isNotEmpty());
    }

    @Test
    void previewZipRejectsNonZipResources() throws Exception {
        mockMvc.perform(get("/api/resources/1/preview-zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("zip preview only supports zip resources"));
    }

    @Test
    void guestCannotPreviewRejectedPdf() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resource not found"));
    }

    @Test
    void guestCannotPreviewPendingDocx() throws Exception {
        mockMvc.perform(get("/api/resources/3/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resource not found"));
    }

    @Test
    void previewRejectsZipResourcesWithUpdatedSupportMessage() throws Exception {
        mockMvc.perform(get("/api/resources/2/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("resource preview only supports pdf, pptx or docx"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanPreviewOwnRejectedPdfInline() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");
        writeStoredFile("seed/2026/04/resume-template-revision.pdf", "pdf");

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanPreviewOwnPendingPptxAsInlinePdf() throws Exception {
        insertResource(5L, 2L, "PENDING", null, "owner-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/owner-deck.pptx");
        writeStoredBinaryFile("seed/2026/04/owner-deck.pptx", simplePptxBytes("Owner Deck"));

        mockMvc.perform(get("/api/resources/5/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanPreviewOwnPendingDocxAsInlinePdf() throws Exception {
        writeStoredBinaryFile("seed/2026/04/ielts-writing-drill.docx", "docx".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/resources/3/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void missingVisibleFileStillReturnsInfrastructureFailure() throws Exception {
        insertResource(6L, 2L, "PENDING", null, "owner-only.pdf", "pdf", "application/pdf",
                "seed/2026/04/owner-only.pdf");

        mockMvc.perform(get("/api/resources/6/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("resource file unavailable"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void loggedInUserCanUploadResource() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume-template.pdf", "application/pdf", "demo".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resources")
                        .file(file)
                        .param("title", "2026 Resume Template")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Minimal resume template")
                        .param("description", "One-page starter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("2026 Resume Template"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Integer createdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_resource_item WHERE title = '2026 Resume Template' AND status = 'PENDING'",
                Integer.class);
        assertThat(createdCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanResubmitRejectedResourceWithoutReplacingFile() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");

        mockMvc.perform(multipart("/api/resources/4")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Revised Resume Pack")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Revised summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.rejectReason").isEmpty());

        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM t_resource_item WHERE id = 4", String.class);
        String rejectReason = jdbcTemplate.queryForObject(
                "SELECT reject_reason FROM t_resource_item WHERE id = 4", String.class);
        Long reviewedBy = jdbcTemplate.query(
                "SELECT reviewed_by FROM t_resource_item WHERE id = 4",
                resultSet -> resultSet.next() ? resultSet.getObject(1, Long.class) : null);
        LocalDateTime reviewedAt = jdbcTemplate.query(
                "SELECT reviewed_at FROM t_resource_item WHERE id = 4",
                resultSet -> resultSet.next() ? resultSet.getObject(1, LocalDateTime.class) : null);

        assertThat(description).isNull();
        assertThat(rejectReason).isNull();
        assertThat(reviewedBy).isNull();
        assertThat(reviewedAt).isNull();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void ownerCanReplaceFileWhileResubmittingRejectedResource() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");

        MockMultipartFile file = new MockMultipartFile(
                "file", "revised-pack.pdf", "application/pdf", "new-pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resources/4")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Revised Resume Pack")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Revised summary")
                        .param("description", "Revised description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("revised-pack.pdf"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void resubmittingRejectedPptxWithoutFileReplacementDeletesOldPreviewArtifact() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");
        writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));
        ResourceItem original = resourceItemMapper.selectById(4L);
        String oldArtifactKey = resourcePreviewService.pptxArtifactKeyOf(original);
        Path oldPreviewPath = PREVIEW_ROOT.resolve(oldArtifactKey);
        Files.createDirectories(oldPreviewPath.getParent());
        Files.writeString(oldPreviewPath, "%PDF-old");

        mockMvc.perform(multipart("/api/resources/4")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Revised Career Deck")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Revised summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(Files.exists(oldPreviewPath)).isFalse();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void replacingRejectedPptxWithPdfDeletesOldDerivedPreviewArtifact() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");
        writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));
        ResourceItem original = resourceItemMapper.selectById(4L);
        String oldArtifactKey = resourcePreviewService.pptxArtifactKeyOf(original);
        Path oldPreviewPath = PREVIEW_ROOT.resolve(oldArtifactKey);
        Files.createDirectories(oldPreviewPath.getParent());
        Files.writeString(oldPreviewPath, "%PDF-old");
        MockMultipartFile file = new MockMultipartFile(
                "file", "revised-pack.pdf", "application/pdf", "new-pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resources/4")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Revised Resume Pack")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Revised summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("revised-pack.pdf"));

        assertThat(Files.exists(oldPreviewPath)).isFalse();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void publishedResourceCannotBeResubmitted() throws Exception {
        mockMvc.perform(multipart("/api/resources/2")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("title", "Nope")
                        .param("category", "RESUME_TEMPLATE")
                        .param("summary", "Nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("only rejected resource can be resubmitted"));
    }

    @Test
    void invalidEnumFilterReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/resources").param("category", "BOOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid resource category"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanReadMyResourcesAcrossStatuses() throws Exception {
        mockMvc.perform(get("/api/resources/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.resources[0].status").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].previewKind").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void myResourcesExposeEditableAndPreviewFlags() throws Exception {
        insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
                "resume-template-revision.pdf", "pdf", "application/pdf",
                "seed/2026/04/resume-template-revision.pdf");

        mockMvc.perform(get("/api/resources/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.resources[0].id").value(4))
                .andExpect(jsonPath("$.data.resources[0].editable").value(true))
                .andExpect(jsonPath("$.data.resources[0].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources[0].previewKind").value("FILE"))
                .andExpect(jsonPath("$.data.resources[1].id").value(3))
                .andExpect(jsonPath("$.data.resources[1].editable").value(false))
                .andExpect(jsonPath("$.data.resources[1].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources[1].previewKind").value("FILE"));
    }

    private void writeStoredFile(String storageKey, String content) throws IOException {
        Path filePath = STORAGE_ROOT.resolve(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    private void writeStoredBinaryFile(String storageKey, byte[] content) throws IOException {
        Path filePath = STORAGE_ROOT.resolve(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    private byte[] sampleZipBytes() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry("backend/"));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("backend/questions.md"));
            zipOutputStream.write("q".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }

    private byte[] simplePptxBytes(String title) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(48, 48, 600, 100));
            XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
            XSLFTextRun textRun = paragraph.addNewTextRun();
            textRun.setText(title);
            slideShow.write(outputStream);
            return outputStream.toByteArray();
        }
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
