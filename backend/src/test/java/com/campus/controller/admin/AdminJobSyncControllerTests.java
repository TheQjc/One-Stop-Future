package com.campus.controller.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.config.JobSyncProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminJobSyncControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobSyncProperties jobSyncProperties;

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
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanTriggerJobSync() throws Exception {
        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "Partner Data Analyst",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/data-analyst",
                      "summary": "New draft job"
                    }
                  ]
                }
                """);

        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceName").value("Partner Feed"))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.defaultCreatedStatus").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotTriggerJobSync() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestCannotTriggerJobSync() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/sync").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void feedFailureReturnsBusinessErrorEnvelope() throws Exception {
        serveRaw(200, "{ not-json");

        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("invalid job sync feed"));
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
