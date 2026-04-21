package com.campus.controller.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminJobImportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanImportValidCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                """
                        title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                        Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/controller-success,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                        """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("jobs.csv"))
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.defaultStatus").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void validationFailureReturnsStructuredErrorPayload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                """
                        title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                        Broken Row,Campus Future,Hangzhou,NOPE,BACHELOR,Official Site,https://jobs.example.com/import/controller-broken,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                        """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("job import validation failed"))
                .andExpect(jsonPath("$.data.importedCount").value(0))
                .andExpect(jsonPath("$.data.errors[0].column").value("jobType"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotImportJobs() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                "x".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
                .andExpect(status().isForbidden());
    }
}
