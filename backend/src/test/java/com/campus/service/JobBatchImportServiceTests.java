package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;

import com.campus.common.JobImportValidationException;
import com.campus.dto.AdminJobImportResponse;
import com.campus.dto.AdminJobImportValidationError;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JobBatchImportServiceTests {

    @Autowired
    private JobBatchImportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void importsMultipleValidRowsAsDrafts() {
        MockMultipartFile file = csvFile("""
                title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                Data Intern,Campus Future,Hangzhou,internship,bachelor,Official Site,https://jobs.example.com/import/data-intern,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                Growth PM,Campus Future,Shanghai,CAMPUS,ANY,Official Site,https://jobs.example.com/import/growth-pm,Support campus growth,,
                """);

        AdminJobImportResponse response = service.importJobs("1", file);

        assertThat(response.importedCount()).isEqualTo(2);
        assertThat(response.defaultStatus()).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
                String.class)).containsOnly("DRAFT");
    }

    @Test
    void duplicateSourceUrlInsideFileFailsWholeImport() {
        MockMultipartFile file = csvFile("""
                title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/dup,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                Growth PM,Campus Future,Shanghai,CAMPUS,ANY,Official Site,https://jobs.example.com/import/dup,Support campus growth,,
                """);

        assertThatThrownBy(() -> service.importJobs("1", file))
                .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                    assertThat(exception.response().importedCount()).isEqualTo(0);
                    assertThat(exception.response().errors())
                            .extracting(AdminJobImportValidationError::message)
                            .contains("duplicate source url in file");
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_posting WHERE source_url = 'https://jobs.example.com/import/dup'",
                Integer.class)).isEqualTo(0);
    }

    @Test
    void duplicateSourceUrlAgainstExistingNonDeletedJobFailsWholeImport() {
        MockMultipartFile file = csvFile("""
                title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/future-campus-tech/backend-intern,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                """);

        assertThatThrownBy(() -> service.importJobs("1", file))
                .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                    assertThat(exception.response().errors())
                            .extracting(AdminJobImportValidationError::message)
                            .contains("duplicate source url already exists");
                });
    }

    @Test
    void oneInvalidRowRollsBackTheWholeBatch() {
        MockMultipartFile file = csvFile("""
                title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                Valid Row,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/valid-row,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                Broken Row,Campus Future,Hangzhou,NOT_A_TYPE,BACHELOR,Official Site,https://jobs.example.com/import/broken-row,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                """);

        assertThatThrownBy(() -> service.importJobs("1", file))
                .isInstanceOf(JobImportValidationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
                Integer.class)).isEqualTo(0);
    }

    private MockMultipartFile csvFile(String body) {
        return new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                body.getBytes(StandardCharsets.UTF_8));
    }
}
