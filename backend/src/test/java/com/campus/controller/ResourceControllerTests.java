package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResourceControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanLocalStorage() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
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
                .andExpect(jsonPath("$.data.fileName").value("resume-template-pack.pdf"));

        mockMvc.perform(get("/api/resources/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resource not found"));
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
                .andExpect(jsonPath("$.data.resources[0].status").isNotEmpty());
    }

    private void writeStoredFile(String storageKey, String content) throws IOException {
        Path filePath = STORAGE_ROOT.resolve(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }
}
