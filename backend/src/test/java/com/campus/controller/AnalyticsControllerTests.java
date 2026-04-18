package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestCanReadAnalyticsSummary() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publicOverview.publishedPostCount").isNumber())
                .andExpect(jsonPath("$.data.publicTrends.length()").value(30))
                .andExpect(jsonPath("$.data.decisionDistribution.tracks.length()").value(3))
                .andExpect(jsonPath("$.data.personalSnapshot").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void invalidPeriodReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").param("period", "90D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid period"));
    }
}

