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
class DiscoverControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestCanUsePublicDiscoverEndpointForResources() throws Exception {
        mockMvc.perform(get("/api/discover")
                        .param("tab", "RESOURCE")
                        .param("period", "WEEK")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tab").value("RESOURCE"))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].type").value("RESOURCE"))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].path").value("/resources/1"))
                .andExpect(jsonPath("$.data.items[0].hotLabel").value("本周高频下载"));
    }

    @Test
    void defaultsAndValidationErrorsMatchTheContract() throws Exception {
        mockMvc.perform(get("/api/discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tab").value("ALL"))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/discover").param("tab", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover tab"));

        mockMvc.perform(get("/api/discover").param("period", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover period"));

        mockMvc.perform(get("/api/discover").param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover limit"));
    }
}
