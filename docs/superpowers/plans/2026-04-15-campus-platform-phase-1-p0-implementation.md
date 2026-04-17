# Campus Platform Phase 1 (P0) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working slice of the campus platform using the approved stack, covering project scaffolding, authentication and user center, the independent home page, and the notice module.

**Architecture:** This plan implements the `P0` scope only: shared platform infrastructure, user center, home aggregation, and notices. The codebase remains a Spring Boot 3 monolith plus a Vue 3 SPA, with MySQL-backed persistence and JWT-based auth; full `校园活动` implementation is intentionally deferred to later plans, while the home page keeps only current Phase A entries and directional aggregation content. All frontend-facing tasks must use `@frontend-design` to lock a deliberate visual direction before coding and `@ui-ux-pro-max` to validate UX, accessibility, and responsive behavior before closing the task.

**Tech Stack:** Vue 3, Vue Router, Pinia, Element Plus, Axios, Vite, Vitest, Spring Boot 3, Spring Security, MyBatis-Plus, MySQL 8, JWT, Lombok, JUnit 5, MockMvc

---

## Scope Split

This platform is too broad for one safe implementation plan. This plan covers only the `P0` slice:

- shared backend/frontend scaffolding
- JWT auth and user center
- independent home page
- notice browse/manage/review

Follow-up plans should cover:

- `校园活动` (`P2`)

## Frontend Design Skill Baseline

All UI tasks in this plan must explicitly use these skills:

- `@frontend-design`
  Use before writing UI code to choose a concrete visual direction, typography pairing, color tokens, motion style, and page composition. Do not ship raw Element Plus defaults.
- `@ui-ux-pro-max`
  Use before finalizing each UI task to review navigation clarity, form feedback, touch target size, spacing rhythm, contrast, responsive layout, and reduced-motion behavior.

Use this Phase 1 design direction unless a later spec overrides it:

- visual theme: `editorial campus bulletin`
- mood: trustworthy, bright, information-dense, not like a generic admin template
- palette: ink navy primary, warm paper background, vermilion/orange accent, muted slate text
- structure: the independent home page is the strongest visual page, with a welcome hero, latest notices, and quick entry cards; admin/teacher pages keep the same tokens but use denser layouts
- implementation rule: centralize shared visual tokens in `frontend/src/styles/tokens.css` and cross-page layout/base rules in `frontend/src/styles/base.css`
- quality gate: verify every frontend task at `375px`, `768px`, and desktop widths, and keep interactive controls at or above `44px` touch height where practical

## Planned File Structure

### Backend

- Create: `backend/pom.xml`
  Spring Boot/Maven dependencies, Java version, test dependencies.
- Create: `backend/src/main/java/com/campus/CampusApplication.java`
  Backend entrypoint.
- Create: `backend/src/main/java/com/campus/common/Result.java`
  Unified API response wrapper.
- Create: `backend/src/main/java/com/campus/common/JwtUtil.java`
  JWT creation and parsing helpers.
- Create: `backend/src/main/java/com/campus/config/SecurityConfig.java`
  Spring Security rules and public/private route definitions.
- Create: `backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java`
  Bearer token parsing and security context population.
- Create: `backend/src/main/java/com/campus/controller/AuthController.java`
  Register/login/logout endpoints.
- Create: `backend/src/main/java/com/campus/controller/UserController.java`
  Profile read/update/password change endpoints.
- Create: `backend/src/main/java/com/campus/controller/NoticeController.java`
  Notice list/detail/create/update/delete/review endpoints.
- Create: `backend/src/main/java/com/campus/service/AuthService.java`
  Auth workflow orchestration.
- Create: `backend/src/main/java/com/campus/service/UserService.java`
  User profile and password logic.
- Create: `backend/src/main/java/com/campus/service/NoticeService.java`
  Notice query and moderation logic.
- Create: `backend/src/main/java/com/campus/mapper/UserMapper.java`
  User persistence.
- Create: `backend/src/main/java/com/campus/mapper/NoticeMapper.java`
  Notice persistence.
- Create: `backend/src/main/java/com/campus/entity/User.java`
  User entity mapping.
- Create: `backend/src/main/java/com/campus/entity/Notice.java`
  Notice entity mapping, including review status fields needed for the confirmed review requirement.
- Create: `backend/src/main/java/com/campus/dto/RegisterRequest.java`
  Register request payload.
- Create: `backend/src/main/java/com/campus/dto/LoginRequest.java`
  Login request payload.
- Create: `backend/src/main/java/com/campus/dto/AuthResponse.java`
  Auth response payload.
- Create: `backend/src/main/java/com/campus/dto/UpdateProfileRequest.java`
  Profile update payload.
- Create: `backend/src/main/java/com/campus/dto/ChangePasswordRequest.java`
  Password change payload.
- Create: `backend/src/main/java/com/campus/dto/NoticeCreateRequest.java`
  Notice creation payload.
- Create: `backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java`
  Notice update payload.
- Create: `backend/src/main/java/com/campus/dto/NoticeReviewRequest.java`
  Notice review payload.
- Create: `backend/src/main/resources/application.yml`
  Environment configuration.
- Create: `backend/src/test/resources/application.yml`
  Test-only H2 datasource and SQL init configuration so backend tests never depend on a local MySQL instance.
- Create: `backend/src/main/resources/schema.sql`
  Phase 1 schema.
- Create: `backend/src/main/resources/data.sql`
  Minimal demo data for manual verification.
- Create: `backend/src/test/java/com/campus/CampusApplicationTests.java`
  Context smoke test.
- Create: `backend/src/test/java/com/campus/common/JwtUtilTests.java`
  Token round-trip tests.
- Create: `backend/src/test/java/com/campus/controller/AuthControllerTests.java`
  Register/login/logout tests.
- Create: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
  Profile and password tests.
- Create: `backend/src/test/java/com/campus/controller/NoticeControllerTests.java`
  Notice browse/manage/review tests.

### Frontend

- Create: `frontend/package.json`
  Frontend dependencies and scripts.
- Create: `frontend/vite.config.js`
  Vite config.
- Create: `frontend/vitest.config.js`
  Vitest config.
- Create: `frontend/index.html`
  SPA host document.
- Create: `frontend/src/main.js`
  App bootstrap.
- Create: `frontend/src/App.vue`
  Root shell.
- Create: `frontend/src/styles/tokens.css`
  Shared color, radius, shadow, spacing, and typography tokens for the Phase 1 UI.
- Create: `frontend/src/styles/base.css`
  Global reset, layout shell, responsive rules, and shared utility classes.
- Create: `frontend/src/router/index.js`
  Route map and auth guard.
- Create: `frontend/src/stores/user.js`
  Auth/user state.
- Create: `frontend/src/api/http.js`
  Axios instance and auth header injection.
- Create: `frontend/src/api/auth.js`
  Auth API adapter.
- Create: `frontend/src/api/user.js`
  User API adapter.
- Create: `frontend/src/api/notice.js`
  Notice API adapter.
- Create: `frontend/src/components/NavBar.vue`
  Main navigation.
- Create: `frontend/src/components/PageFooter.vue`
  Footer.
- Create: `frontend/src/components/NoticeCard.vue`
  Reusable notice summary card.
- Create: `frontend/src/views/LoginView.vue`
  Login form.
- Create: `frontend/src/views/RegisterView.vue`
  Register form.
- Create: `frontend/src/views/ProfileView.vue`
  User center/profile form.
- Create: `frontend/src/views/HomeView.vue`
  Independent home page with latest notices and quick links.
- Create: `frontend/src/views/NoticeView.vue`
  Notice list page with pagination/filter UI.
- Create: `frontend/src/views/NoticeDetailView.vue`
  Notice detail page.
- Create: `frontend/src/views/admin/NoticeManageView.vue`
  Teacher/admin notice management and review UI.
- Create: `frontend/src/test/setup.js`
  Frontend test bootstrap.
- Create: `frontend/src/App.spec.js`
  Root shell smoke test.
- Create: `frontend/src/views/LoginView.spec.js`
  Login view behavior test.
- Create: `frontend/src/views/ProfileView.spec.js`
  Profile view behavior test.
- Create: `frontend/src/views/NoticeView.spec.js`
  Notice list behavior test.
- Create: `frontend/src/views/admin/NoticeManageView.spec.js`
  Notice management behavior test.

### Repo

- Modify: `.gitignore`
  Add `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, local env files, and IDE noise beyond the existing `.idea/`.
- Create: `README.md`
  Phase 1 local setup and verification steps.

## Task 1: Scaffold the Backend Project

**Files:**
- Modify: `.gitignore`
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/campus/CampusApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/campus/CampusApplicationTests.java`

- [ ] **Step 1: Write the failing backend smoke test**

```java
package com.campus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run the test to verify the scaffold does not exist yet**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: FAIL because `pom.xml` and/or `CampusApplication` do not exist yet.

- [ ] **Step 3: Write the minimal backend scaffold**

Create:

```xml
<!-- backend/pom.xml -->
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
  </parent>
  <groupId>com.campus</groupId>
  <artifactId>backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>17</java.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

```java
// backend/src/main/java/com/campus/CampusApplication.java
package com.campus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusApplication.class, args);
    }
}
```

```yaml
# backend/src/main/resources/application.yml
server:
  port: 8080
spring:
  application:
    name: campus-platform
```

Also extend `.gitignore` with:

```gitignore
backend/target/
frontend/node_modules/
frontend/dist/
.env
.env.*
```

- [ ] **Step 4: Re-run the backend smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add .gitignore backend/pom.xml backend/src/main/java/com/campus/CampusApplication.java backend/src/main/resources/application.yml backend/src/test/java/com/campus/CampusApplicationTests.java
git commit -m "chore: scaffold backend application"
```

## Task 2: Scaffold the Frontend App Shell

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/vitest.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/styles/tokens.css`
- Create: `frontend/src/styles/base.css`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/user.js`
- Create: `frontend/src/components/NavBar.vue`
- Create: `frontend/src/components/PageFooter.vue`
- Create: `frontend/src/views/LoginView.vue`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/test/setup.js`
- Create: `frontend/src/App.spec.js`

- [ ] **Step 1: Write the failing shell test**

```js
import { mount } from "@vue/test-utils";
import App from "./App.vue";

test("renders the app shell", () => {
  const wrapper = mount(App, {
    global: {
      stubs: ["router-view"],
    },
  });

  expect(wrapper.text()).toContain("校园一站式信息平台");
});
```

- [ ] **Step 2: Run the shell test before the app exists**

Run:

```powershell
cd frontend
npm run test -- --run src/App.spec.js
```

Expected: FAIL because the frontend package and components do not exist yet.

- [ ] **Step 3: Create the minimal app shell**

Before writing the files, use `@frontend-design` to commit to the `editorial campus bulletin` direction defined above, then use `@ui-ux-pro-max` as a checklist for readable navigation, clear hierarchy, and mobile-safe spacing.

Create:

```json
// frontend/package.json
{
  "name": "campus-platform-frontend",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "test": "vitest"
  },
  "dependencies": {
    "axios": "^1.7.7",
    "element-plus": "^2.8.4",
    "pinia": "^2.2.4",
    "vue": "^3.5.12",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "@vue/test-utils": "^2.4.6",
    "jsdom": "^25.0.1",
    "vite": "^5.4.8",
    "vitest": "^2.1.2"
  }
}
```

```vue
<!-- frontend/src/App.vue -->
<script setup>
import NavBar from "./components/NavBar.vue";
import PageFooter from "./components/PageFooter.vue";
</script>

<template>
  <div class="app-shell">
    <NavBar />
    <main>
      <router-view />
    </main>
    <PageFooter />
  </div>
</template>
```

```vue
<!-- frontend/src/components/NavBar.vue -->
<template>
  <header class="nav-bar">校园一站式信息平台</header>
</template>
```

```vue
<!-- frontend/src/components/PageFooter.vue -->
<template>
  <footer class="page-footer">Phase 1 / P0</footer>
</template>
```

```html
<!-- frontend/index.html -->
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>校园一站式信息平台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

```js
// frontend/src/main.js
import { createApp } from "vue";
import { createPinia } from "pinia";
import router from "./router/index.js";
import App from "./App.vue";
import "./styles/tokens.css";
import "./styles/base.css";

createApp(App).use(createPinia()).use(router).mount("#app");
```

```css
/* frontend/src/styles/tokens.css */
:root {
  --cp-bg: #f5efe2;
  --cp-surface: #fffaf0;
  --cp-surface-strong: #ffffff;
  --cp-ink: #1f2a44;
  --cp-ink-soft: #4b5563;
  --cp-accent: #c75b39;
  --cp-accent-strong: #9f3f24;
  --cp-border: rgba(31, 42, 68, 0.12);
  --cp-radius-md: 16px;
  --cp-radius-lg: 24px;
  --cp-shadow-card: 0 18px 40px rgba(31, 42, 68, 0.12);
  --cp-max-width: 1200px;
}
```

```css
/* frontend/src/styles/base.css */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: radial-gradient(circle at top, #fff7e8, var(--cp-bg) 55%);
  color: var(--cp-ink);
  font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
}

#app {
  min-height: 100vh;
}

.app-shell main {
  width: min(100% - 32px, var(--cp-max-width));
  margin: 0 auto;
  padding: 24px 0 40px;
}
```

```js
// frontend/vite.config.js
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
});
```

```js
// frontend/vitest.config.js
import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.js",
  },
});
```

```js
// frontend/src/test/setup.js
import { config } from "@vue/test-utils";

config.global.stubs = {
  "router-link": true,
  "router-view": true,
};
```

```vue
<!-- frontend/src/views/LoginView.vue -->
<template>
  <form>
    <input name="username" />
    <input name="password" type="password" />
    <button type="submit">登录</button>
  </form>
</template>
```

```vue
<!-- frontend/src/views/HomeView.vue -->
<template>
  <section>
    <h1>校园一站式信息平台</h1>
  </section>
</template>
```

```js
// frontend/src/stores/user.js
import { defineStore } from "pinia";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: "",
    profile: null,
  }),
});
```

```js
// frontend/src/router/index.js
import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import HomeView from "../views/HomeView.vue";

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: LoginView },
    { path: "/", component: HomeView },
  ],
});
```

- [ ] **Step 4: Re-run the shell test**

Run:

```powershell
cd frontend
npm install
npm run test -- --run src/App.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/vite.config.js frontend/vitest.config.js frontend/index.html frontend/src/main.js frontend/src/App.vue frontend/src/styles/tokens.css frontend/src/styles/base.css frontend/src/router/index.js frontend/src/stores/user.js frontend/src/components/NavBar.vue frontend/src/components/PageFooter.vue frontend/src/views/LoginView.vue frontend/src/views/HomeView.vue frontend/src/test/setup.js frontend/src/App.spec.js
git commit -m "chore: scaffold frontend application shell"
```

## Task 3: Add the Shared Backend Infrastructure and Phase 1 Schema

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/resources/application.yml`
- Create: `backend/src/main/java/com/campus/common/Result.java`
- Create: `backend/src/main/java/com/campus/common/JwtUtil.java`
- Create: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java`
- Create: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/resources/data.sql`
- Create: `backend/src/test/java/com/campus/common/JwtUtilTests.java`

- [ ] **Step 1: Write the failing JWT round-trip test**

```java
package com.campus.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JwtUtilTests {
    @Test
    void tokenRoundTripPreservesUsername() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-key-test-secret-key", 3600);
        String token = jwtUtil.generateToken("student01", "STUDENT");

        assertEquals("student01", jwtUtil.extractUsername(token));
    }
}
```

- [ ] **Step 2: Run the JWT test**

Run:

```powershell
cd backend
mvn -q -Dtest=JwtUtilTests test
```

Expected: FAIL because `JwtUtil` does not exist yet.

- [ ] **Step 3: Implement the shared backend plumbing**

Create:

```java
// backend/src/main/java/com/campus/common/Result.java
package com.campus.common;

public record Result<T>(int code, String msg, T data) {
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
}
```

```sql
-- backend/src/main/resources/schema.sql
CREATE TABLE t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50),
  role TINYINT NOT NULL DEFAULT 0,
  email VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE t_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  category TINYINT NOT NULL DEFAULT 0,
  author_id BIGINT NOT NULL,
  is_top TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

```sql
-- backend/src/main/resources/data.sql
INSERT INTO t_user (username, password, real_name, role, email)
VALUES
  ('admin01', '{noop}secret123', '管理员账号', 2, 'admin@example.com'),
  ('teacher01', '{noop}secret123', '教师账号', 1, 'teacher@example.com'),
  ('student01', '{noop}secret123', '学生账号', 0, 'student@example.com');
```

Implement `JwtUtil`, `SecurityConfig`, and `JwtAuthenticationFilter` so that:

- `/api/auth/**` is public
- everything else requires authentication
- bearer tokens populate the authenticated principal

Update `backend/pom.xml` to add:

- `spring-boot-starter-security`
- `com.baomidou:mybatis-plus-spring-boot3-starter`
- `com.mysql:mysql-connector-j`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- `org.projectlombok:lombok`
- `com.h2database:h2` with test scope

Update `backend/src/main/resources/application.yml` to include environment-driven MySQL datasource and JWT settings.

Create `backend/src/test/resources/application.yml` so every `mvn test` run uses H2 automatically:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:campus;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
jwt:
  secret: test-secret-key-test-secret-key
  expire-seconds: 3600
```

This test config is required so `@SpringBootTest` and `MockMvc` tasks never depend on a local MySQL instance and always load `schema.sql` plus `data.sql`.

- [ ] **Step 4: Re-run the JWT test**

Run:

```powershell
cd backend
mvn -q -Dtest=JwtUtilTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/common/Result.java backend/src/main/java/com/campus/common/JwtUtil.java backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/config/JwtAuthenticationFilter.java backend/src/main/resources/application.yml backend/src/test/resources/application.yml backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/test/java/com/campus/common/JwtUtilTests.java
git commit -m "feat: add shared backend infrastructure"
```

## Task 4: Implement Auth and User Center Backend

**Files:**
- Create: `backend/src/main/java/com/campus/entity/User.java`
- Create: `backend/src/main/java/com/campus/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/campus/service/AuthService.java`
- Create: `backend/src/main/java/com/campus/service/UserService.java`
- Create: `backend/src/main/java/com/campus/controller/AuthController.java`
- Create: `backend/src/main/java/com/campus/controller/UserController.java`
- Create: `backend/src/main/java/com/campus/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/campus/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/campus/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/com/campus/dto/ChangePasswordRequest.java`
- Create: `backend/src/test/java/com/campus/controller/AuthControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/UserControllerTests.java`

- [ ] **Step 1: Write the failing auth tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {
    @Test
    void registerTeacherReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"teacher02","password":"secret123","role":"TEACHER"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {
    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student01", roles = "STUDENT")
    void changePasswordReturnsSuccess() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"oldPassword":"secret123","newPassword":"secret456"}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: Run the auth and user tests**

Run:

```powershell
cd backend
mvn -q -Dtest=AuthControllerTests,UserControllerTests test
```

Expected: FAIL because the domain, controller, and service files do not exist yet.

- [ ] **Step 3: Implement the auth and user backend slice**

Implement:

- user registration for student and teacher roles
- reject duplicate usernames
- login with JWT issuance
- logout endpoint contract
- current-user profile read
- profile update
- password change

Minimal controller shape:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) { ... }

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) { ... }

    @PostMapping("/logout")
    public Result<Void> logout() { ... }
}
```

- [ ] **Step 4: Re-run the auth and user tests**

Run:

```powershell
cd backend
mvn -q -Dtest=AuthControllerTests,UserControllerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/entity/User.java backend/src/main/java/com/campus/mapper/UserMapper.java backend/src/main/java/com/campus/service/AuthService.java backend/src/main/java/com/campus/service/UserService.java backend/src/main/java/com/campus/controller/AuthController.java backend/src/main/java/com/campus/controller/UserController.java backend/src/main/java/com/campus/dto/RegisterRequest.java backend/src/main/java/com/campus/dto/LoginRequest.java backend/src/main/java/com/campus/dto/AuthResponse.java backend/src/main/java/com/campus/dto/UpdateProfileRequest.java backend/src/main/java/com/campus/dto/ChangePasswordRequest.java backend/src/test/java/com/campus/controller/AuthControllerTests.java backend/src/test/java/com/campus/controller/UserControllerTests.java
git commit -m "feat: implement auth and user center backend"
```

## Task 5: Implement the Notice Backend

**Files:**
- Create: `backend/src/main/java/com/campus/entity/Notice.java`
- Create: `backend/src/main/java/com/campus/mapper/NoticeMapper.java`
- Create: `backend/src/main/java/com/campus/service/NoticeService.java`
- Create: `backend/src/main/java/com/campus/controller/NoticeController.java`
- Create: `backend/src/main/java/com/campus/dto/NoticeCreateRequest.java`
- Create: `backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java`
- Create: `backend/src/main/java/com/campus/dto/NoticeReviewRequest.java`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Create: `backend/src/test/java/com/campus/controller/NoticeControllerTests.java`

- [ ] **Step 1: Write the failing notice tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
class NoticeControllerTests {
    @Test
    @WithMockUser(username = "student01", roles = "STUDENT")
    void listReturnsPagedNotices() throws Exception {
        mockMvc.perform(get("/api/notices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "teacher01", roles = "TEACHER")
    void teacherCanReviewNotice() throws Exception {
        mockMvc.perform(post("/api/notices/1/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"APPROVED"}"""))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin01", roles = "ADMIN")
    void adminCanDeleteNotice() throws Exception {
        mockMvc.perform(delete("/api/notices/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: Run the notice tests**

Run:

```powershell
cd backend
mvn -q -Dtest=NoticeControllerTests test
```

Expected: FAIL because the notice stack does not exist yet.

- [ ] **Step 3: Implement the notice backend slice**

Implement:

- notice list
- notice detail
- pagination and category filtering
- create/update/delete
- review action for teacher/admin
- seeded admin account support for verification

Minimal controller shape:

```java
@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    @GetMapping
    public Result<?> list(...) { ... }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) { ... }

    @PostMapping
    public Result<Void> create(@RequestBody NoticeCreateRequest request) { ... }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NoticeUpdateRequest request) { ... }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { ... }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody NoticeReviewRequest request) { ... }
}
```

- [ ] **Step 4: Re-run the notice tests**

Run:

```powershell
cd backend
mvn -q -Dtest=NoticeControllerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/entity/Notice.java backend/src/main/java/com/campus/mapper/NoticeMapper.java backend/src/main/java/com/campus/service/NoticeService.java backend/src/main/java/com/campus/controller/NoticeController.java backend/src/main/java/com/campus/dto/NoticeCreateRequest.java backend/src/main/java/com/campus/dto/NoticeUpdateRequest.java backend/src/main/java/com/campus/dto/NoticeReviewRequest.java backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/test/java/com/campus/controller/NoticeControllerTests.java
git commit -m "feat: implement notice backend"
```

## Task 6: Implement Frontend Auth and User Center

**Files:**
- Create: `frontend/src/api/http.js`
- Create: `frontend/src/api/auth.js`
- Create: `frontend/src/api/user.js`
- Create: `frontend/src/views/RegisterView.vue`
- Create: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/styles/base.css`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/stores/user.js`
- Modify: `frontend/src/views/LoginView.vue`
- Modify: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/LoginView.spec.js`
- Create: `frontend/src/views/ProfileView.spec.js`

- [ ] **Step 1: Write the failing frontend auth tests**

```js
import { mount } from "@vue/test-utils";
import LoginView from "./LoginView.vue";

test("renders login form fields", () => {
  const wrapper = mount(LoginView);
  expect(wrapper.find('input[name="username"]').exists()).toBe(true);
  expect(wrapper.find('input[name="password"]').exists()).toBe(true);
  expect(wrapper.find("button[type='submit']").exists()).toBe(true);
});
```

```js
import { mount } from "@vue/test-utils";
import ProfileView from "./ProfileView.vue";

test("renders profile form", () => {
  const wrapper = mount(ProfileView);
  expect(wrapper.text()).toContain("个人信息");
});
```

- [ ] **Step 2: Run the frontend auth tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/LoginView.spec.js src/views/ProfileView.spec.js
```

Expected: FAIL because the auth/user files do not exist or are incomplete.

- [ ] **Step 3: Implement the auth frontend slice**

Implement:

- first, use `@frontend-design` to keep login/register/profile pages in the shared `editorial campus bulletin` language rather than raw form pages, then use `@ui-ux-pro-max` to check labels, validation placement, button states, and mobile spacing
- login and register forms
- user store with token persistence
- auth route guard
- profile read/update/password flow
- home page shell that shows the current user and quick links
- shared use of `frontend/src/styles/tokens.css` and `frontend/src/styles/base.css` so the auth pages feel consistent with the home page

Visual direction for this task:

- login/register: one strong header area, concise helper copy, and clear role selection for registration
- profile: card-based sections for base info and password update, not one long undifferentiated form
- interaction: loading/disabled/error states must be visually distinct and readable on mobile

Minimal store shape:

```js
export const useUserStore = defineStore("user", {
  state: () => ({ token: "", profile: null }),
  actions: {
    setToken(token) { this.token = token; },
    logout() { this.token = ""; this.profile = null; },
  },
});
```

- [ ] **Step 4: Re-run the frontend auth tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/LoginView.spec.js src/views/ProfileView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/http.js frontend/src/api/auth.js frontend/src/api/user.js frontend/src/views/RegisterView.vue frontend/src/views/ProfileView.vue frontend/src/styles/tokens.css frontend/src/styles/base.css frontend/src/router/index.js frontend/src/stores/user.js frontend/src/views/LoginView.vue frontend/src/views/HomeView.vue frontend/src/views/LoginView.spec.js frontend/src/views/ProfileView.spec.js
git commit -m "feat: implement frontend auth and user center"
```

## Task 7: Implement Notice Pages and Home Aggregation

**Files:**
- Create: `frontend/src/api/notice.js`
- Create: `frontend/src/components/NoticeCard.vue`
- Create: `frontend/src/views/NoticeView.vue`
- Create: `frontend/src/views/NoticeDetailView.vue`
- Create: `frontend/src/views/admin/NoticeManageView.vue`
- Modify: `frontend/src/styles/tokens.css`
- Modify: `frontend/src/styles/base.css`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/router/index.js`
- Create: `frontend/src/views/NoticeView.spec.js`
- Create: `frontend/src/views/admin/NoticeManageView.spec.js`
- Create: `README.md`

- [ ] **Step 1: Write the failing notice UI tests**

```js
import { mount } from "@vue/test-utils";
import NoticeView from "./NoticeView.vue";

test("renders notice filter and list", () => {
  const wrapper = mount(NoticeView);
  expect(wrapper.text()).toContain("通知公告");
  expect(wrapper.text()).toContain("分类");
});
```

```js
import { mount } from "@vue/test-utils";
import NoticeManageView from "./NoticeManageView.vue";

test("shows review actions for teacher/admin", () => {
  const wrapper = mount(NoticeManageView);
  expect(wrapper.text()).toContain("审核");
});
```

- [ ] **Step 2: Run the notice UI tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/NoticeView.spec.js src/views/admin/NoticeManageView.spec.js
```

Expected: FAIL because the notice pages do not exist yet.

- [ ] **Step 3: Implement the notice frontend slice**

Implement:

- first, use `@frontend-design` to make the independent home page the strongest branded screen in `P0`, then use `@ui-ux-pro-max` to review filter clarity, table/list density, review actions, and responsive behavior
- notice list with pagination/filter controls
- notice detail page
- teacher/admin notice management page
- home page latest-notice block plus activity planning copy
- root `README.md` with local startup and smoke-test instructions
- demo account section that includes `admin01`, `teacher01`, and `student01`
- keep notice browse pages visually lighter and more editorial; keep teacher/admin management pages denser but still on the same token system instead of switching to a separate backend theme

Minimal home page requirement:

```vue
<template>
  <section>
    <h1>校园一站式信息平台</h1>
    <div class="quick-links">
      <router-link to="/notices">通知公告</router-link>
      <span>校园活动（Phase 2）</span>
    </div>
  </section>
</template>
```

- [ ] **Step 4: Re-run the notice UI tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/NoticeView.spec.js src/views/admin/NoticeManageView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/notice.js frontend/src/components/NoticeCard.vue frontend/src/views/NoticeView.vue frontend/src/views/NoticeDetailView.vue frontend/src/views/admin/NoticeManageView.vue frontend/src/styles/tokens.css frontend/src/styles/base.css frontend/src/views/HomeView.vue frontend/src/router/index.js frontend/src/views/NoticeView.spec.js frontend/src/views/admin/NoticeManageView.spec.js README.md
git commit -m "feat: implement homepage and notice frontend"
```

## Verification Checklist

After Task 7, run the full Phase 1 verification set:

```powershell
cd backend
mvn test
cd ../frontend
npm run test -- --run
npm run build
```

Manual smoke checklist:

- Register a teacher account.
- Log in as student, teacher, and admin.
- View the independent home page.
- Verify the frontend layout at mobile, tablet, and desktop widths, and confirm buttons/forms remain readable and tappable.
- Open the notice list, filter by category, and paginate.
- Open a notice detail page.
- Create, edit, delete, and review a notice as teacher/admin.
- Verify profile read/update/password change.

If any of those fail, fix before starting the Phase 2 plan.

