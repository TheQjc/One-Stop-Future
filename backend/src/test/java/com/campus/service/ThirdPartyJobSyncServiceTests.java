package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.campus.common.BusinessException;
import com.campus.config.JobSyncProperties;
import com.campus.dto.AdminJobSyncResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ThirdPartyJobSyncServiceTests {

    @Autowired
    private ThirdPartyJobSyncService service;

    @Autowired
    private JobSyncProperties jobSyncProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private HttpServer server;

    @BeforeEach
    void setUp() {
        jobSyncProperties.setEnabled(true);
        jobSyncProperties.setFeedUrl("");
        jobSyncProperties.setSourceName("Partner Feed");
        jobSyncProperties.setBearerToken("");
        jobSyncProperties.setConnectTimeoutMs(5000);
        jobSyncProperties.setReadTimeoutMs(10000);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void syncCreatesUpdatesSkipsDeletedAndReportsInvalidItems() {
        jdbcTemplate.update("""
                INSERT INTO t_job_posting (
                  id, title, company_name, city, job_type, education_requirement, source_platform, source_url,
                  summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at
                ) VALUES (
                  99, 'Old Deleted Job', 'Legacy Partner', 'Hangzhou', 'FULL_TIME', 'BACHELOR', 'Partner Feed',
                  'https://partner.example/jobs/deleted-role', 'deleted', 'deleted', NULL, NULL, 'DELETED', 1, 1,
                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);

        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "Java Backend Intern Updated",
                      "companyName": "Future Campus Tech",
                      "city": "Shanghai",
                      "jobType": "INTERNSHIP",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://jobs.example.com/future-campus-tech/backend-intern",
                      "summary": "Updated summary",
                      "content": "Updated content",
                      "deadlineAt": "2026-06-20 18:00:00"
                    },
                    {
                      "title": "Partner Data Analyst",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/data-analyst",
                      "summary": "New draft job",
                      "content": "Partner content",
                      "deadlineAt": "2026-06-30 18:00:00"
                    },
                    {
                      "title": "Deleted Match",
                      "companyName": "Legacy Partner",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/deleted-role",
                      "summary": "Should skip"
                    },
                    {
                      "title": "Broken Item",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "NOT_A_TYPE",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/broken",
                      "summary": "Broken"
                    }
                  ]
                }
                """);

        AdminJobSyncResponse response = service.syncJobs("1");

        assertThat(response.sourceName()).isEqualTo("Partner Feed");
        assertThat(response.fetchedCount()).isEqualTo(4);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.defaultCreatedStatus()).isEqualTo("DRAFT");
        assertThat(response.issues()).extracting(AdminJobSyncResponse.Issue::type)
                .containsExactly("SKIPPED", "INVALID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_job_posting WHERE source_url = 'https://partner.example/jobs/data-analyst'",
                String.class)).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT city FROM t_job_posting WHERE source_url = 'https://jobs.example.com/future-campus-tech/backend-intern'",
                String.class)).isEqualTo("Shanghai");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_job_posting WHERE id = 99",
                String.class)).isEqualTo("DELETED");
    }

    @Test
    void duplicateSourceUrlInsideFeedFailsWholeSyncWithoutWrites() {
        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "One",
                      "companyName": "Partner",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/dup",
                      "summary": "first"
                    },
                    {
                      "title": "Two",
                      "companyName": "Partner",
                      "city": "Shanghai",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/dup",
                      "summary": "second"
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("invalid job sync feed");
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_posting WHERE source_url = 'https://partner.example/jobs/dup'",
                Integer.class)).isEqualTo(0);
    }

    @Test
    void malformedJsonFailsWholeSyncWithoutWrites() {
        serveRaw(200, "{ not-json");

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("invalid job sync feed");
                });
    }

    @Test
    void disabledIntegrationFailsBeforeCallingTheFeed() {
        jobSyncProperties.setEnabled(false);

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("job sync unavailable");
                });
    }

    private void serveJson(String body) {
        serveRaw(200, body);
    }

    private void serveRaw(int status, String body) {
        stopExistingServer();
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        server.createContext("/feed", new StaticResponseHandler(status, body));
        server.start();
        jobSyncProperties.setEnabled(true);
        jobSyncProperties.setFeedUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/feed");
    }

    private void stopExistingServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private record StaticResponseHandler(int status, String body) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, responseBytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBytes);
            }
        }
    }
}
