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

    @Test
    void submitRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/decision/assessment/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"optionId":11},{"questionId":2,"optionId":22},{"questionId":3,"optionId":31},{"questionId":4,"optionId":41},{"questionId":5,"optionId":51},{"questionId":6,"optionId":61}]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void latestRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/decision/assessment/latest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void latestReturnsEmptyWhenNoSessionExists() throws Exception {
        mockMvc.perform(get("/api/decision/assessment/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hasResult").value(false));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void submitIncompleteAnswersReturnsBusinessError() throws Exception {
        mockMvc.perform(post("/api/decision/assessment/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"optionId":11},{"questionId":2,"optionId":22},{"questionId":3,"optionId":31},{"questionId":4,"optionId":41},{"questionId":5,"optionId":51}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void submitDuplicateQuestionAnswersReturnsBusinessError() throws Exception {
        mockMvc.perform(post("/api/decision/assessment/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"optionId":11},{"questionId":1,"optionId":12},{"questionId":2,"optionId":22},{"questionId":3,"optionId":31},{"questionId":4,"optionId":41},{"questionId":5,"optionId":51}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void submitOptionQuestionMismatchReturnsBusinessError() throws Exception {
        mockMvc.perform(post("/api/decision/assessment/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"optionId":22},{"questionId":2,"optionId":11},{"questionId":3,"optionId":31},{"questionId":4,"optionId":41},{"questionId":5,"optionId":51},{"questionId":6,"optionId":61}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void submitReturnsResultAndLatestReflectsIt() throws Exception {
        mockMvc.perform(post("/api/decision/assessment/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":1,"optionId":11},{"questionId":2,"optionId":22},{"questionId":3,"optionId":31},{"questionId":4,"optionId":41},{"questionId":5,"optionId":51},{"questionId":6,"optionId":61}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hasResult").value(true))
                .andExpect(jsonPath("$.data.recommendedTrack").value("EXAM"))
                .andExpect(jsonPath("$.data.summaryText").isNotEmpty())
                .andExpect(jsonPath("$.data.scores.exam").isNumber())
                .andExpect(jsonPath("$.data.ranking.length()").value(3))
                .andExpect(jsonPath("$.data.sessionDate").isNotEmpty())
                .andExpect(jsonPath("$.data.nextActions.length()").value(2));

        mockMvc.perform(get("/api/decision/assessment/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.hasResult").value(true))
                .andExpect(jsonPath("$.data.recommendedTrack").value("EXAM"));
    }
}
