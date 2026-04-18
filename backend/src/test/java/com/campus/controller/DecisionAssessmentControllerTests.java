package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DecisionAssessmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void questionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/decision/assessment/questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void questionsReturnOrderedQuestionSet() throws Exception {
        mockMvc.perform(get("/api/decision/assessment/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.questions.length()").value(6))
                .andExpect(jsonPath("$.data.questions[0].code").value("DECISION_Q1"))
                .andExpect(jsonPath("$.data.questions[0].options[0].code").value("Q1_A"))
                .andExpect(jsonPath("$.data.questions[0].options[0].careerScore").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].examScore").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].options[0].abroadScore").doesNotExist());
    }
}

