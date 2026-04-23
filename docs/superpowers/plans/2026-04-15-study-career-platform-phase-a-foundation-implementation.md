# Study-Career Platform Phase A Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Completion status:** Completed on 2026-04-16.
> Delivered by commits `b7af75a`, `792001b`, `5001697`, `b2972aa`, `390a1c0`, `7e804da`, `a02f3bc`, and `1d64c76`.
> This slice remains the active foundation baseline referenced by Phases B-H and the current README; later full backend/frontend regression runs through Phase H cover it as part of the integrated application.

**Goal:** Replace the current legacy notice prototype with the approved Phase A foundation for phone-code auth, independent home aggregation, personal center, student verification review, notifications, and the minimal admin backbone.

**Architecture:** The repository already contains a Spring Boot 3 + Vue 3 prototype, but its business model is still the old `username/password + teacher/admin notice review` flow. Phase A must first realign the domain model to the new spec, then build a thin but complete vertical slice: auth, home, profile, verification, notifications, and admin review. Backend remains a modular Spring Boot monolith with MyBatis-Plus and JWT; frontend remains a Vue SPA with Pinia and Vue Router; Redis / Elasticsearch / MinIO stay as reserved future extension points, not active Phase A dependencies.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MySQL 8, H2 test DB, JWT, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest, Docker Compose

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-15-study-career-platform-regeneration-design.md`
- Global requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Current codebase mismatch:
  - backend still models `teacher` and `notice`
  - frontend still routes around `notice` browse/manage pages
  - auth is still `username/password`
  - response body currently uses `msg`, while the new requirement baseline expects `message`

Do not extend the old `notice` domain as if it were Phase A. Retire it cleanly and replace it with the new foundation domain.

## Scope Lock

This plan covers only Phase A:

- phone-code register / login / logout
- JWT auth and route protection
- independent home aggregation page
- profile page and verification submission
- site notifications
- admin verification review dashboard

This plan explicitly does not implement:

- posts, comments, likes, rankings
- jobs, resource upload/download, global search
- recommendation quiz, school comparison, analytics dashboards beyond the admin minimum

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing layout, styles, or page composition. Keep one deliberate visual direction across guest home, auth, profile, notifications, and admin.
- `@ui-ux-pro-max`
  Use before closing any UI task to review navigation clarity, empty states, mobile behavior, validation feedback, and accessible touch target sizing.

Use this visual direction unless the spec changes:

- theme: `editorial student decision desk`
- tone: trustworthy, clean, information-dense, not like a generic admin template
- colors: warm paper background, deep navy text, muted teal support, rust/orange accents
- typography: strong display face for home hero, readable Chinese sans for content
- motion: subtle stagger / reveal only on home modules and admin cards
- mobile rule: verify `375px`, `768px`, and desktop widths before closing each UI task

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application.yml`
- Modify: `backend/src/main/java/com/campus/common/Result.java`
- Modify: `backend/src/main/java/com/campus/common/JwtUtil.java`
- Modify: `backend/src/main/java/com/campus/common/UserRole.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/campus/controller/AuthController.java`
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/AuthService.java`
- Modify: `backend/src/main/java/com/campus/service/UserService.java`
- Modify: `backend/src/main/java/com/campus/mapper/UserMapper.java`
- Modify: `backend/src/main/java/com/campus/entity/User.java`
- Modify: `backend/src/main/java/com/campus/dto/AuthResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/LoginRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/RegisterRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/UserProfile.java`
- Modify: `backend/src/test/java/com/campus/common/JwtUtilTests.java`
- Modify: `backend/src/test/java/com/campus/controller/AuthControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/NotificationType.java`
- Create: `backend/src/main/java/com/campus/common/UserStatus.java`
- Create: `backend/src/main/java/com/campus/common/VerificationStatus.java`
- Create: `backend/src/main/java/com/campus/controller/HomeController.java`
- Create: `backend/src/main/java/com/campus/controller/NotificationController.java`
- Create: `backend/src/main/java/com/campus/controller/VerificationController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminVerificationController.java`
- Create: `backend/src/main/java/com/campus/dto/SendCodeRequest.java`
- Create: `backend/src/main/java/com/campus/dto/SendCodeResponse.java`
- Create: `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Create: `backend/src/main/java/com/campus/dto/NotificationListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/NotificationReadRequest.java`
- Create: `backend/src/main/java/com/campus/dto/VerificationApplyRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminVerificationReviewRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminVerificationDashboardResponse.java`
- Create: `backend/src/main/java/com/campus/entity/Notification.java`
- Create: `backend/src/main/java/com/campus/entity/VerificationApplication.java`
- Create: `backend/src/main/java/com/campus/entity/VerificationCode.java`
- Create: `backend/src/main/java/com/campus/mapper/NotificationMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/VerificationApplicationMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/VerificationCodeMapper.java`
- Create: `backend/src/main/java/com/campus/service/HomeService.java`
- Create: `backend/src/main/java/com/campus/service/NotificationService.java`
- Create: `backend/src/main/java/com/campus/service/VerificationService.java`
- Create: `backend/src/main/java/com/campus/service/AdminVerificationService.java`
- Create: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/NotificationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/VerificationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminVerificationControllerTests.java`

### Backend: Delete Legacy Notice Domain

- Delete: `backend/src/main/java/com/campus/common/NoticeStatus.java`
- Delete: `backend/src/main/java/com/campus/controller/NoticeController.java`
- Delete: `backend/src/main/java/com/campus/service/NoticeService.java`
- Delete: `backend/src/main/java/com/campus/mapper/NoticeMapper.java`
- Delete: `backend/src/main/java/com/campus/entity/Notice.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeCreateRequest.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeReviewRequest.java`
- Delete: `backend/src/test/java/com/campus/controller/NoticeControllerTests.java`
- Delete: `backend/src/main/java/com/campus/dto/ChangePasswordRequest.java`

### Frontend: Modify Existing

- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/stores/user.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/api/auth.js`
- Modify: `frontend/src/api/user.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/styles/base.css`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/RegisterView.vue`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/App.spec.js`
- Modify: `frontend/src/views/LoginView.spec.js`
- Modify: `frontend/src/views/ProfileView.spec.js`

### Frontend: Create

- Create: `frontend/src/api/admin.js`
- Create: `frontend/src/api/home.js`
- Create: `frontend/src/api/notification.js`
- Create: `frontend/src/api/verification.js`
- Create: `frontend/src/components/HomeEntryCard.vue`
- Create: `frontend/src/components/NotificationBell.vue`
- Create: `frontend/src/components/VerificationStatusBadge.vue`
- Create: `frontend/src/views/NotificationCenterView.vue`
- Create: `frontend/src/views/admin/AdminVerificationReviewView.vue`
- Create: `frontend/src/views/HomeView.spec.js`
- Create: `frontend/src/views/NotificationCenterView.spec.js`
- Create: `frontend/src/views/admin/AdminVerificationReviewView.spec.js`

### Frontend: Delete Legacy Notice UI

- Delete: `frontend/src/api/notice.js`
- Delete: `frontend/src/components/NoticeCard.vue`
- Delete: `frontend/src/views/NoticeView.vue`
- Delete: `frontend/src/views/NoticeDetailView.vue`
- Delete: `frontend/src/views/admin/NoticeManageView.vue`
- Delete: `frontend/src/views/NoticeView.spec.js`
- Delete: `frontend/src/views/admin/NoticeManageView.spec.js`

### Repo / Deployment

- Modify: `README.md`
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `docker-compose.yml`
- Create: `deploy/nginx/default.conf`

## Task 1: Freeze the Phase A Backend Data Model

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application.yml`
- Modify: `backend/src/main/java/com/campus/common/Result.java`
- Modify: `backend/src/main/java/com/campus/common/UserRole.java`
- Modify: `backend/src/main/java/com/campus/entity/User.java`
- Modify: `backend/src/main/java/com/campus/dto/UserProfile.java`
- Create: `backend/src/main/java/com/campus/common/UserStatus.java`
- Create: `backend/src/main/java/com/campus/common/VerificationStatus.java`
- Create: `backend/src/main/java/com/campus/common/NotificationType.java`
- Create: `backend/src/main/java/com/campus/entity/VerificationApplication.java`
- Create: `backend/src/main/java/com/campus/entity/VerificationCode.java`
- Create: `backend/src/main/java/com/campus/entity/Notification.java`
- Test: `backend/src/test/java/com/campus/CampusApplicationTests.java`

- [x] **Step 1: Write a failing schema smoke test**

```java
package com.campus;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void phaseATablesExist() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(metaData.getTables(null, null, "t_verification_application", null).next()).isTrue();
            assertThat(metaData.getTables(null, null, "t_notification", null).next()).isTrue();
            assertThat(metaData.getTables(null, null, "t_verification_code", null).next()).isTrue();
        }
    }
}
```

- [x] **Step 2: Run the schema smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: FAIL because the legacy schema still only provides `t_user` and `t_notice`.

- [x] **Step 3: Replace the legacy schema with the Phase A model**

Implement this minimum schema shape:

```sql
CREATE TABLE t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL UNIQUE,
  nickname VARCHAR(50) NOT NULL,
  real_name VARCHAR(50) NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  verification_status VARCHAR(20) NOT NULL,
  student_id VARCHAR(50) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_verification_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  purpose VARCHAR(20) NOT NULL,
  code VARCHAR(6) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_verification_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  student_id VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reject_reason VARCHAR(255) NULL,
  reviewer_id BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(255) NOT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  source_type VARCHAR(30) NULL,
  source_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at DATETIME NULL
);
```

Also make these model decisions explicit in code:

- `Result` becomes `Result<T>(int code, String message, T data)`
- persisted account roles become `USER` and `ADMIN`
- guest remains unauthenticated, not a persisted role
- verification state is tracked separately as `UNVERIFIED`, `PENDING`, `VERIFIED`
- user status is tracked separately as `ACTIVE`, `BANNED`
- `application.yml` keeps MySQL runtime config and H2 test config, but reserves Redis / ES / MinIO placeholders for later phases

Seed `data.sql` with:

- one admin user
- one normal user
- one verified user
- zero legacy notice data

- [x] **Step 4: Re-run the schema smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/resources/application.yml backend/src/test/resources/application.yml backend/src/main/java/com/campus/common/Result.java backend/src/main/java/com/campus/common/UserRole.java backend/src/main/java/com/campus/common/UserStatus.java backend/src/main/java/com/campus/common/VerificationStatus.java backend/src/main/java/com/campus/common/NotificationType.java backend/src/main/java/com/campus/entity/User.java backend/src/main/java/com/campus/entity/VerificationApplication.java backend/src/main/java/com/campus/entity/VerificationCode.java backend/src/main/java/com/campus/entity/Notification.java backend/src/main/java/com/campus/dto/UserProfile.java backend/src/test/java/com/campus/CampusApplicationTests.java
git commit -m "refactor: align backend schema with phase a model"
```

## Task 2: Implement Phone-Code Authentication Backend

**Files:**
- Modify: `backend/src/main/java/com/campus/common/JwtUtil.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/campus/controller/AuthController.java`
- Modify: `backend/src/main/java/com/campus/service/AuthService.java`
- Modify: `backend/src/main/java/com/campus/mapper/UserMapper.java`
- Modify: `backend/src/main/java/com/campus/dto/LoginRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/RegisterRequest.java`
- Modify: `backend/src/main/java/com/campus/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/campus/dto/SendCodeRequest.java`
- Create: `backend/src/main/java/com/campus/dto/SendCodeResponse.java`
- Create: `backend/src/main/java/com/campus/mapper/VerificationCodeMapper.java`
- Modify: `backend/src/test/java/com/campus/common/JwtUtilTests.java`
- Modify: `backend/src/test/java/com/campus/controller/AuthControllerTests.java`

- [x] **Step 1: Write failing auth tests for phone-code flow**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sendRegisterCodeReturnsDebugCodeInMockMode() throws Exception {
        mockMvc.perform(post("/api/auth/codes/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"13800000001","purpose":"REGISTER"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.debugCode").isNotEmpty());
    }

    @Test
    void registerWithCodeReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"phone":"13800000001","verificationCode":"123456","nickname":"鏂扮敤鎴?}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
```

- [x] **Step 2: Run the auth tests**

Run:

```powershell
cd backend
mvn -q -Dtest=AuthControllerTests,JwtUtilTests test
```

Expected: FAIL because auth still uses `username/password`.

- [x] **Step 3: Implement phone-code auth**

API contract:

```java
POST /api/auth/codes/send
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
```

DTO shape:

```java
public record SendCodeRequest(
        @Pattern(regexp = "^\\d{11}$") String phone,
        @NotBlank String purpose) {
}

public record RegisterRequest(
        @Pattern(regexp = "^\\d{11}$") String phone,
        @Pattern(regexp = "^\\d{6}$") String verificationCode,
        @NotBlank String nickname) {
}

public record LoginRequest(
        @Pattern(regexp = "^\\d{11}$") String phone,
        @Pattern(regexp = "^\\d{6}$") String verificationCode) {
}
```

Implementation rules:

- store verification codes in `t_verification_code`
- enforce 6-digit code and 5-minute expiry
- in mock-SMS mode, return `debugCode` in the send-code response and log it
- register creates a `USER` with `ACTIVE + UNVERIFIED`
- register sends a welcome notification
- login rejects `BANNED` users
- JWT subject should be stable user identity, not the removed legacy username
- `SecurityConfig` must permit `POST /api/auth/**` and public `GET /api/home/summary`

- [x] **Step 4: Re-run auth tests**

Run:

```powershell
cd backend
mvn -q -Dtest=AuthControllerTests,JwtUtilTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/common/JwtUtil.java backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java backend/src/main/java/com/campus/controller/AuthController.java backend/src/main/java/com/campus/service/AuthService.java backend/src/main/java/com/campus/mapper/UserMapper.java backend/src/main/java/com/campus/mapper/VerificationCodeMapper.java backend/src/main/java/com/campus/dto/SendCodeRequest.java backend/src/main/java/com/campus/dto/SendCodeResponse.java backend/src/main/java/com/campus/dto/LoginRequest.java backend/src/main/java/com/campus/dto/RegisterRequest.java backend/src/main/java/com/campus/dto/AuthResponse.java backend/src/test/java/com/campus/common/JwtUtilTests.java backend/src/test/java/com/campus/controller/AuthControllerTests.java
git commit -m "feat: implement phone code authentication"
```

## Task 3: Implement Profile, Home, Notifications, and Verification Backend

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/UserService.java`
- Modify: `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/campus/controller/HomeController.java`
- Create: `backend/src/main/java/com/campus/controller/NotificationController.java`
- Create: `backend/src/main/java/com/campus/controller/VerificationController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminVerificationController.java`
- Create: `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Create: `backend/src/main/java/com/campus/dto/NotificationListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/NotificationReadRequest.java`
- Create: `backend/src/main/java/com/campus/dto/VerificationApplyRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminVerificationReviewRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminVerificationDashboardResponse.java`
- Create: `backend/src/main/java/com/campus/mapper/NotificationMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/VerificationApplicationMapper.java`
- Create: `backend/src/main/java/com/campus/service/HomeService.java`
- Create: `backend/src/main/java/com/campus/service/NotificationService.java`
- Create: `backend/src/main/java/com/campus/service/VerificationService.java`
- Create: `backend/src/main/java/com/campus/service/AdminVerificationService.java`
- Create: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/NotificationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/VerificationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminVerificationControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`

- [x] **Step 1: Write failing integration tests for the Phase A backend slice**

```java
@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTests {
    @Test
    void guestCanReadHomeSummary() throws Exception {
        mockMvc.perform(get("/api/home/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.viewerType").value("GUEST"));
    }
}
```

```java
@SpringBootTest
@AutoConfigureMockMvc
class VerificationControllerTests {
    @Test
    @WithMockUser(username = "user-1", roles = "USER")
    void submitVerificationCreatesPendingApplication() throws Exception {
        mockMvc.perform(post("/api/verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"realName":"寮犱笁","studentId":"20260001"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

```java
@SpringBootTest
@AutoConfigureMockMvc
class AdminVerificationControllerTests {
    @Test
    @WithMockUser(username = "admin-1", roles = "ADMIN")
    void adminCanApproveVerification() throws Exception {
        mockMvc.perform(post("/api/admin/verifications/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"action":"APPROVE","reason":""}
                """))
            .andExpect(status().isOk());
    }
}
```

- [x] **Step 2: Run the new integration tests**

Run:

```powershell
cd backend
mvn -q -Dtest=HomeControllerTests,VerificationControllerTests,NotificationControllerTests,AdminVerificationControllerTests,UserControllerTests test
```

Expected: FAIL because these endpoints do not exist yet.

- [x] **Step 3: Implement the Phase A backend APIs**

Required endpoints:

```java
GET  /api/home/summary
GET  /api/users/me
PUT  /api/users/me
POST /api/verifications
GET  /api/notifications
POST /api/notifications/{id}/read
POST /api/notifications/read-all
GET  /api/admin/verifications/dashboard
GET  /api/admin/verifications
POST /api/admin/verifications/{id}/review
```

Required behavior:

- `GET /api/home/summary` works for guests and logged-in users
- home response includes identity snapshot, unread count, entry cards, todo items, and latest notifications
- verification submit rejects duplicate pending applications
- admin reject requires a non-empty reason
- admin approval updates `t_user.verification_status` to `VERIFIED`
- approval / rejection both create notifications
- `Result` error bodies always use `message`

Home response minimum shape:

```java
public record HomeSummaryResponse(
        String viewerType,
        String roleLabel,
        String verificationStatus,
        int unreadNotificationCount,
        List<String> todos,
        List<HomeEntryCard> entries,
        List<NotificationSnippet> latestNotifications) {
}
```

- [x] **Step 4: Re-run the Phase A backend test set**

Run:

```powershell
cd backend
mvn -q -Dtest=HomeControllerTests,VerificationControllerTests,NotificationControllerTests,AdminVerificationControllerTests,UserControllerTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/controller/UserController.java backend/src/main/java/com/campus/service/UserService.java backend/src/main/java/com/campus/config/GlobalExceptionHandler.java backend/src/main/java/com/campus/controller/HomeController.java backend/src/main/java/com/campus/controller/NotificationController.java backend/src/main/java/com/campus/controller/VerificationController.java backend/src/main/java/com/campus/controller/admin/AdminVerificationController.java backend/src/main/java/com/campus/dto/HomeSummaryResponse.java backend/src/main/java/com/campus/dto/NotificationListResponse.java backend/src/main/java/com/campus/dto/NotificationReadRequest.java backend/src/main/java/com/campus/dto/VerificationApplyRequest.java backend/src/main/java/com/campus/dto/AdminVerificationReviewRequest.java backend/src/main/java/com/campus/dto/AdminVerificationDashboardResponse.java backend/src/main/java/com/campus/mapper/NotificationMapper.java backend/src/main/java/com/campus/mapper/VerificationApplicationMapper.java backend/src/main/java/com/campus/service/HomeService.java backend/src/main/java/com/campus/service/NotificationService.java backend/src/main/java/com/campus/service/VerificationService.java backend/src/main/java/com/campus/service/AdminVerificationService.java backend/src/test/java/com/campus/controller/HomeControllerTests.java backend/src/test/java/com/campus/controller/NotificationControllerTests.java backend/src/test/java/com/campus/controller/VerificationControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminVerificationControllerTests.java backend/src/test/java/com/campus/controller/UserControllerTests.java
git commit -m "feat: add phase a backend application slice"
```

## Task 4: Rebuild the Frontend Shell Around the Phase A Information Architecture

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/stores/user.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/styles/base.css`
- Create: `frontend/src/api/home.js`
- Create: `frontend/src/api/notification.js`
- Create: `frontend/src/api/verification.js`
- Create: `frontend/src/api/admin.js`
- Create: `frontend/src/components/HomeEntryCard.vue`
- Create: `frontend/src/components/NotificationBell.vue`
- Create: `frontend/src/components/VerificationStatusBadge.vue`
- Delete: `frontend/src/api/notice.js`
- Delete: `frontend/src/components/NoticeCard.vue`
- Delete: `frontend/src/views/NoticeView.vue`
- Delete: `frontend/src/views/NoticeDetailView.vue`
- Delete: `frontend/src/views/admin/NoticeManageView.vue`
- Delete: `frontend/src/views/NoticeView.spec.js`
- Delete: `frontend/src/views/admin/NoticeManageView.spec.js`
- Modify: `frontend/src/App.spec.js`

- [x] **Step 1: Write a failing shell test that reflects the new IA**

```js
import { mount } from "@vue/test-utils";
import App from "./App.vue";

test("renders the phase a shell", () => {
  const wrapper = mount(App, {
    global: {
      stubs: {
        RouterView: { template: "<div>view</div>" },
      },
    },
  });

  expect(wrapper.text()).toContain("One-Stop Future");
});
```

- [x] **Step 2: Run the shell test**

Run:

```powershell
cd frontend
npm run test -- src/App.spec.js
```

Expected: FAIL or become irrelevant because the shell still assumes notice-first navigation.

- [x] **Step 3: Replace the shell, routing, and store model**

Before writing UI code, use `@frontend-design` to restate the `editorial student decision desk` direction, then use `@ui-ux-pro-max` to verify hierarchy, nav clarity, unread affordances, and mobile spacing.

Routing target:

```js
[
  { path: "/", name: "home", component: HomeView },
  { path: "/login", name: "login", component: LoginView, meta: { guestOnly: true } },
  { path: "/register", name: "register", component: RegisterView, meta: { guestOnly: true } },
  { path: "/profile", name: "profile", component: ProfileView, meta: { requiresAuth: true } },
  { path: "/notifications", name: "notifications", component: NotificationCenterView, meta: { requiresAuth: true } },
  { path: "/admin/verifications", name: "admin-verifications", component: AdminVerificationReviewView, meta: { requiresAuth: true, roles: ["ADMIN"] } },
]
```

Store target:

```js
export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("one-stop-future-token") || "",
    profile: readJson("one-stop-future-profile", null),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.profile?.role === "ADMIN",
    isVerified: (state) => state.profile?.verificationStatus === "VERIFIED",
    unreadCount: (state) => state.profile?.unreadNotificationCount || 0,
  },
});
```

Also update `http.js` so it:

- attaches JWT
- normalizes backend `{code,message,data}`
- clears auth and redirects on `401`

- [x] **Step 4: Re-run the shell test**

Run:

```powershell
cd frontend
npm run test -- src/App.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/App.vue frontend/src/main.js frontend/src/router/index.js frontend/src/stores/user.js frontend/src/api/http.js frontend/src/api/home.js frontend/src/api/notification.js frontend/src/api/verification.js frontend/src/api/admin.js frontend/src/components/NavBar.vue frontend/src/components/HomeEntryCard.vue frontend/src/components/NotificationBell.vue frontend/src/components/VerificationStatusBadge.vue frontend/src/styles/tokens.css frontend/src/styles/base.css frontend/src/App.spec.js
git commit -m "refactor: rebuild frontend shell for phase a"
```

## Task 5: Implement Guest Auth Pages and the Independent Home Page

**Files:**
- Modify: `frontend/src/api/auth.js`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/RegisterView.vue`
- Modify: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/views/LoginView.spec.js`

- [x] **Step 1: Write failing view tests**

```js
import { mount } from "@vue/test-utils";
import LoginView from "./LoginView.vue";

test("login view renders phone and verification code fields", () => {
  const wrapper = mount(LoginView);
  expect(wrapper.find('input[name="phone"]').exists()).toBe(true);
  expect(wrapper.find('input[name="verificationCode"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("鑾峰彇楠岃瘉鐮?);
});
```

```js
import { mount } from "@vue/test-utils";
import HomeView from "./HomeView.vue";

test("guest home renders module entries and auth guidance", () => {
  const wrapper = mount(HomeView);
  expect(wrapper.text()).toContain("灏变笟");
  expect(wrapper.text()).toContain("鑰冪爺");
  expect(wrapper.text()).toContain("鐣欏");
});
```

- [x] **Step 2: Run the guest/auth view tests**

Run:

```powershell
cd frontend
npm run test -- src/views/LoginView.spec.js src/views/HomeView.spec.js
```

Expected: FAIL because views still use the old username/password and notice-first language.

- [x] **Step 3: Implement the guest auth and home experience**

Before writing code, use `@frontend-design` for layout and `@ui-ux-pro-max` for validation / readability review.

`auth.js` target:

```js
export async function sendCode(payload) {
  const { data } = await http.post("/auth/codes/send", payload);
  return data.data;
}

export async function login(payload) {
  const { data } = await http.post("/auth/login", payload);
  return data.data;
}

export async function register(payload) {
  const { data } = await http.post("/auth/register", payload);
  return data.data;
}
```

Home page requirements:

- guest mode: strong hero, three path themes, login/register call to action, module cards, 鈥滃嵆灏嗗紑鏀锯€?placeholders for non-Phase-A modules
- logged-in mode: identity banner, unread count, todo card, module entry grid, latest notifications panel
- admin mode: same home language, but with extra admin entry card

Auth page requirements:

- `phone + code + nickname`
- send-code button with cooldown state
- registration and login separated as two pages
- mock-SMS debug code shown only when backend returns it

- [x] **Step 4: Re-run the guest/auth view tests**

Run:

```powershell
cd frontend
npm run test -- src/views/LoginView.spec.js src/views/HomeView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/api/auth.js frontend/src/views/LoginView.vue frontend/src/views/RegisterView.vue frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/views/LoginView.spec.js
git commit -m "feat: implement guest auth and home pages"
```

## Task 6: Implement Profile, Verification Submission, and Notification Center UI

**Files:**
- Modify: `frontend/src/api/user.js`
- Modify: `frontend/src/views/ProfileView.vue`
- Create: `frontend/src/views/NotificationCenterView.vue`
- Create: `frontend/src/views/NotificationCenterView.spec.js`
- Modify: `frontend/src/views/ProfileView.spec.js`
- Modify: `frontend/src/stores/user.js`

- [x] **Step 1: Write failing user-side Phase A tests**

```js
import { mount } from "@vue/test-utils";
import ProfileView from "./ProfileView.vue";

test("profile view renders verification application section", () => {
  const wrapper = mount(ProfileView);
  expect(wrapper.text()).toContain("瀛﹀彿璁よ瘉");
});
```

```js
import { mount } from "@vue/test-utils";
import NotificationCenterView from "./NotificationCenterView.vue";

test("notification center renders unread and read controls", () => {
  const wrapper = mount(NotificationCenterView);
  expect(wrapper.text()).toContain("鍏ㄩ儴鏍囪涓哄凡璇?);
});
```

- [x] **Step 2: Run the user-side tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js
```

Expected: FAIL because profile still reflects the legacy account model and there is no notification center.

- [x] **Step 3: Implement profile, verification, and notification UI**

Use `@frontend-design` first, then `@ui-ux-pro-max`.

Profile page must show:

- phone
- nickname
- real-name status
- verification badge
- verification application form when status is `UNVERIFIED`
- pending state message when status is `PENDING`
- verified state summary when status is `VERIFIED`

Notification center must support:

- unread badge summary
- list render by notification type
- mark single read
- mark all read
- empty state

`user.js` target shape:

```js
export async function getProfile() {
  const { data } = await http.get("/users/me");
  return data.data;
}

export async function updateProfile(payload) {
  const { data } = await http.put("/users/me", payload);
  return data.data;
}
```

- [x] **Step 4: Re-run the user-side tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ProfileView.spec.js src/views/NotificationCenterView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/api/user.js frontend/src/views/ProfileView.vue frontend/src/views/ProfileView.spec.js frontend/src/views/NotificationCenterView.vue frontend/src/views/NotificationCenterView.spec.js frontend/src/stores/user.js
git commit -m "feat: implement profile verification and notifications ui"
```

## Task 7: Implement the Admin Verification Review UI

**Files:**
- Modify: `frontend/src/router/index.js`
- Create: `frontend/src/views/admin/AdminVerificationReviewView.vue`
- Create: `frontend/src/views/admin/AdminVerificationReviewView.spec.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/components/NavBar.vue`

- [x] **Step 1: Write a failing admin review test**

```js
import { mount } from "@vue/test-utils";
import AdminVerificationReviewView from "./AdminVerificationReviewView.vue";

test("admin review page shows approve and reject actions", () => {
  const wrapper = mount(AdminVerificationReviewView);
  expect(wrapper.text()).toContain("閫氳繃");
  expect(wrapper.text()).toContain("椹冲洖");
});
```

- [x] **Step 2: Run the admin review test**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminVerificationReviewView.spec.js
```

Expected: FAIL because the page and route do not exist.

- [x] **Step 3: Build the minimal admin backbone**

Use `@frontend-design` and `@ui-ux-pro-max`.

Admin page requirements:

- top summary cards: pending count, reviewed today
- review list with applicant, student ID, submit time, current status
- approve action
- reject action with mandatory reason input
- loading, empty, and error states

`admin.js` target:

```js
export async function getVerificationDashboard() {
  const { data } = await http.get("/admin/verifications/dashboard");
  return data.data;
}

export async function reviewVerification(id, payload) {
  const { data } = await http.post(`/admin/verifications/${id}/review`, payload);
  return data.data;
}
```

- [x] **Step 4: Re-run the admin review test**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminVerificationReviewView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/api/admin.js frontend/src/components/NavBar.vue frontend/src/views/admin/AdminVerificationReviewView.vue frontend/src/views/admin/AdminVerificationReviewView.spec.js
git commit -m "feat: implement admin verification review ui"
```

## Task 8: Add Deployment Skeleton, Remove Legacy Files, and Run Full Verification

**Files:**
- Delete: `backend/src/main/java/com/campus/common/NoticeStatus.java`
- Delete: `backend/src/main/java/com/campus/controller/NoticeController.java`
- Delete: `backend/src/main/java/com/campus/service/NoticeService.java`
- Delete: `backend/src/main/java/com/campus/mapper/NoticeMapper.java`
- Delete: `backend/src/main/java/com/campus/entity/Notice.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeCreateRequest.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java`
- Delete: `backend/src/main/java/com/campus/dto/NoticeReviewRequest.java`
- Delete: `backend/src/test/java/com/campus/controller/NoticeControllerTests.java`
- Delete: `backend/src/main/java/com/campus/dto/ChangePasswordRequest.java`
- Delete: `frontend/src/api/notice.js`
- Delete: `frontend/src/components/NoticeCard.vue`
- Delete: `frontend/src/views/NoticeView.vue`
- Delete: `frontend/src/views/NoticeDetailView.vue`
- Delete: `frontend/src/views/admin/NoticeManageView.vue`
- Delete: `frontend/src/views/NoticeView.spec.js`
- Delete: `frontend/src/views/admin/NoticeManageView.spec.js`
- Modify: `README.md`
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `docker-compose.yml`
- Create: `deploy/nginx/default.conf`

- [x] **Step 1: Write the failing deployment sanity check**

Add a short README checklist section that assumes these commands exist:

```text
docker compose up -d --build
docker compose ps
```

Run:

```powershell
docker compose config
```

Expected: FAIL because no Compose stack exists yet.

- [x] **Step 2: Add deployment files and remove dead legacy code**

Create minimum containerization:

```dockerfile
# backend/Dockerfile
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY backend /app/backend
RUN mvn -f /app/backend/pom.xml clean package -DskipTests
```

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend .
RUN npm run build
```

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
  backend:
    build:
      context: .
      dockerfile: backend/Dockerfile
  frontend:
    build:
      context: .
      dockerfile: frontend/Dockerfile
```

README must include:

- local backend startup
- local frontend startup
- test commands
- mock-SMS debug behavior
- admin / normal / verified demo accounts
- Docker Compose startup

Then delete all dead notice-domain files listed above.

- [x] **Step 3: Run full verification**

Run:

```powershell
cd backend
mvn test
cd ../frontend
npm run test
npm run build
cd ..
docker compose config
```

Expected:

- backend tests PASS
- frontend tests PASS
- frontend build PASS
- `docker compose config` PASS

- [x] **Step 4: Manual smoke pass**

Validate:

- guest can open home page and see auth guidance
- send-code works in mock mode and exposes debug code only in mock mode
- register / login work with phone + code
- logged-in user sees personal center and unread count
- normal user can submit verification
- admin can review verification with approve / reject
- reject requires reason
- approved user sees verified status on next profile / home fetch
- notification center supports single read and read-all
- admin entry is hidden from non-admin users

- [x] **Step 5: Commit**

```bash
git add README.md backend/Dockerfile frontend/Dockerfile docker-compose.yml deploy/nginx/default.conf
git add -u backend/src/main/java/com/campus/common/NoticeStatus.java backend/src/main/java/com/campus/controller/NoticeController.java backend/src/main/java/com/campus/service/NoticeService.java backend/src/main/java/com/campus/mapper/NoticeMapper.java backend/src/main/java/com/campus/entity/Notice.java backend/src/main/java/com/campus/dto/NoticeCreateRequest.java backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java backend/src/main/java/com/campus/dto/NoticeReviewRequest.java backend/src/test/java/com/campus/controller/NoticeControllerTests.java backend/src/main/java/com/campus/dto/ChangePasswordRequest.java frontend/src/api/notice.js frontend/src/components/NoticeCard.vue frontend/src/views/NoticeView.vue frontend/src/views/NoticeDetailView.vue frontend/src/views/admin/NoticeManageView.vue frontend/src/views/NoticeView.spec.js frontend/src/views/admin/NoticeManageView.spec.js
git commit -m "chore: finalize phase a deployment and cleanup"
```

## Final Verification Set

After Task 8, run the full suite in this exact order:

```powershell
cd backend
mvn test
cd ../frontend
npm run test
npm run build
cd ..
docker compose config
```

If any command fails, fix that failure before moving to the next phase plan.
