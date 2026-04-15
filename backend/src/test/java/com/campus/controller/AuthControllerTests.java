package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendRegisterCodeReturnsDebugCodeInMockMode() throws Exception {
        mockMvc.perform(post("/api/auth/codes/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001","purpose":"REGISTER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.purpose").value("REGISTER"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.data.debugCode").isNotEmpty());
    }

    @Test
    void registerWithCodeCreatesUserAndWelcomeNotificationAndReturnsToken() throws Exception {
        insertCode("13800000011", "REGISTER", "123456", 5);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000011","verificationCode":"123456","nickname":"NewUser"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.phone").value("13800000011"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long userId = json.path("data").path("userId").asLong();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = ? AND type = 'WELCOME'", Integer.class, userId);
        org.junit.jupiter.api.Assertions.assertNotNull(count);
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void loginReturnsTokenWithStableIdentityFields() throws Exception {
        insertCode("13800000001", "LOGIN", "654321", 5);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001","verificationCode":"654321"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.phone").value("13800000001"));
    }

    @Test
    void loginRejectsBannedUsers() throws Exception {
        jdbcTemplate.update("UPDATE t_user SET status = 'BANNED' WHERE phone = '13800000001'");
        insertCode("13800000001", "LOGIN", "777777", 5);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000001","verificationCode":"777777"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("account is banned"));
    }

    @Test
    void logoutReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private void insertCode(String phone, String purpose, String code, int expireMinutes) {
        jdbcTemplate.update(
                "INSERT INTO t_verification_code (phone, purpose, code, expires_at, consumed_at, created_at) VALUES (?, ?, ?, ?, NULL, ?)",
                phone, purpose, code, LocalDateTime.now().plusMinutes(expireMinutes), LocalDateTime.now());
    }
}
