package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DecisionSchoolControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestCanListSchoolsForSupportedTrack() throws Exception {
        mockMvc.perform(get("/api/decision/schools").param("track", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.track").value("EXAM"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.schools[0].schoolId").value(5001));
    }

    @Test
    void invalidTrackReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/decision/schools").param("track", "CAREER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid track"));
    }

    @Test
    void guestCanCompareSchoolsAndPreserveRequestOrder() throws Exception {
        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5002,5001]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.schools[0].schoolId").value(5002))
                .andExpect(jsonPath("$.data.schools[1].schoolId").value(5001))
                .andExpect(jsonPath("$.data.metricDefinitions[0].metricOrder").value(1))
                .andExpect(jsonPath("$.data.tableRows[0].metricCode").value("COST_MONTHLY"))
                .andExpect(jsonPath("$.data.highlightSummary").isNotEmpty());
    }

    @Test
    void compareValidatesCountAndDuplicatesAndMixedDomain() throws Exception {
        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001,5002,5003,6001,6002]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001,5001]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001,6001]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001,9999]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void chartSeriesOnlyIncludesChartableMetrics() throws Exception {
        mockMvc.perform(post("/api/decision/schools/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolIds":[5001,5003]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chartSeries.length()").value(3))
                .andExpect(jsonPath("$.data.chartSeries[0].metricCode").value("COST_MONTHLY"))
                .andExpect(jsonPath("$.data.tableRows[2].metricCode").value("ADMISSION_RATE"))
                .andExpect(jsonPath("$.data.tableRows[2].cells[1].isMissing").value(true))
                .andExpect(jsonPath("$.data.tableRows[2].cells[1].displayValue").value("N/A"))
                .andExpect(jsonPath("$.data.chartSeries[2].metricCode").value("ADMISSION_RATE"))
                .andExpect(jsonPath("$.data.chartSeries[2].points[1].isMissing").value(true));
    }
}
