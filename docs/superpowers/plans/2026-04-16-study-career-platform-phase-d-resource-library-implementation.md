# Study-Career Platform Phase D Resource Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Completion status:** Completed on 2026-04-16.
> Delivered by commits `d364a59`, `45b8153`, `56d85cb`, `5a0f634`, and `e0cf53c`, with follow-up doc/test alignment in `ae78b90`.
> This slice became the baseline extended by Phases E-H and remains covered by later full backend/frontend regression runs through Phase H.

**Goal:** Build the first Phase D resource-library vertical slice: real file upload/storage, public resource browse/detail/download, resource favorites, read-only "my resources", homepage activation, and a minimal admin review workspace.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Add a focused `resources` domain that mirrors the working community and jobs layering: SQL schema, MyBatis-Plus entity/mapper, dedicated service/controller pairs, a narrow file-storage abstraction with a local-filesystem implementation, and view-level frontend API modules. Reuse the existing `t_user_favorite` table and `/api/users/me/favorites` entrypoint instead of introducing a second favorites system.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MySQL 8, H2 local/test profile, JWT, multipart upload, local filesystem storage, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-d-resource-library-design.md`
- Requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Existing implementation baseline:
  - `docs/superpowers/plans/2026-04-15-study-career-platform-phase-a-foundation-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-b-community-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-c-job-aggregation-implementation.md`
- Current repo already has:
  - JWT login and role gating
  - unified home aggregation page with a reserved `resources` entry
  - reusable `t_user_favorite` table and `FavoriteTargetType`
  - public and admin flows for community and jobs that should be treated as the reference implementation pattern
- Storage choice for this slice is fixed:
  - use local filesystem storage first
  - keep a storage abstraction so MinIO can replace the implementation later
  - do not introduce Docker into this slice
- Safe local backend run must continue using:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

- Safe local frontend run must continue using:

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

## Scope Lock

This plan covers only the Phase D resource-library first slice:

- public `/resources` list with keyword and category filters
- public `/resources/:id` detail page for published resources
- authenticated `/resources/upload`
- authenticated download for published resources
- authenticated favorite / unfavorite
- authenticated read-only `/profile/resources`
- reuse of `/api/users/me/favorites?type=RESOURCE`
- admin `/admin/resources` review workspace
- resource homepage entry activation

This plan explicitly does not implement:

- MinIO integration
- unified search aggregation
- chunked upload or resume upload
- online preview
- edit or re-submit after rejection
- batch upload or batch review
- ranking, hotness, analytics, or reporting boards
- comments, likes, or reporting for resources

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing layout, composition, spacing, typography, and visual hierarchy.
- `@ui-ux-pro-max`
  Use before closing each UI task to review clarity, mobile behavior, empty states, and action affordances.

Use the current visual system as the base:

- theme: `editorial student decision desk`
- keep the existing warm-paper, deep-navy, muted accent language
- preserve the current homepage, profile, and admin shell patterns
- do not introduce a second design system for resources

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/ResourceCategory.java`
- Create: `backend/src/main/java/com/campus/common/ResourceStatus.java`
- Create: `backend/src/main/java/com/campus/config/ResourceStorageProperties.java`
- Create: `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
- Create: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
- Create: `backend/src/main/java/com/campus/entity/ResourceItem.java`
- Create: `backend/src/main/java/com/campus/mapper/ResourceItemMapper.java`
- Create: `backend/src/main/java/com/campus/dto/ResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Create: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourceReviewRequest.java`
- Create: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Create: `backend/src/main/java/com/campus/service/ResourceService.java`
- Create: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Create: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/views/ProfileView.spec.js`
- Modify: `frontend/src/views/ProfileFavoritesView.vue`
- Modify: `frontend/src/views/ProfileFavoritesView.spec.js`

### Frontend: Create

- Create: `frontend/src/api/resources.js`
- Create: `frontend/src/components/ResourceCard.vue`
- Create: `frontend/src/components/ResourceFilterBar.vue`
- Create: `frontend/src/views/ResourcesListView.vue`
- Create: `frontend/src/views/ResourceDetailView.vue`
- Create: `frontend/src/views/ResourceUploadView.vue`
- Create: `frontend/src/views/ProfileResourcesView.vue`
- Create: `frontend/src/views/admin/AdminResourceManageView.vue`
- Create: `frontend/src/views/ResourcesListView.spec.js`
- Create: `frontend/src/views/ResourceDetailView.spec.js`
- Create: `frontend/src/views/ResourceUploadView.spec.js`
- Create: `frontend/src/views/ProfileResourcesView.spec.js`
- Create: `frontend/src/views/admin/AdminResourceManageView.spec.js`

### Repo Docs

- Modify: `README.md`

## Task 1: Freeze the Resource Schema, Enum Set, and Storage Skeleton

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
- Create: `backend/src/main/java/com/campus/common/ResourceCategory.java`
- Create: `backend/src/main/java/com/campus/common/ResourceStatus.java`
- Create: `backend/src/main/java/com/campus/config/ResourceStorageProperties.java`
- Create: `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
- Create: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
- Create: `backend/src/main/java/com/campus/entity/ResourceItem.java`
- Create: `backend/src/main/java/com/campus/mapper/ResourceItemMapper.java`

- [ ] **Step 1: Write the failing schema and config safety tests**

```java
@Test
void phaseDResourceTableExists() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
        DatabaseMetaData metaData = connection.getMetaData();
        assertThat(tableExists(metaData, "t_resource_item")).isTrue();
    }
}
```

```java
@Test
void localProfileConfigPinsResourceStorageToWorkspaceFolder() {
    Properties properties = loadYaml("application-local.yml");
    assertThat(properties.getProperty("app.resource-storage.type")).isEqualTo("local");
    assertThat(properties.getProperty("app.resource-storage.local-root"))
            .isEqualTo(".local-storage/resources");
}
```

- [ ] **Step 2: Run the failing schema/config tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=CampusApplicationTests,ApplicationConfigSafetyTests" test
```

Expected: FAIL because the resource table, enum wiring, and storage properties do not exist yet.

- [ ] **Step 3: Add the Phase D persistence model and local-storage skeleton**

Add this minimum table:

```sql
CREATE TABLE t_resource_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(32) NOT NULL,
  summary VARCHAR(300) NOT NULL,
  description TEXT NULL,
  status VARCHAR(20) NOT NULL,
  uploader_id BIGINT NOT NULL,
  reviewed_by BIGINT NULL,
  reject_reason VARCHAR(300) NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(20) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  download_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  published_at DATETIME NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Model rules:

- `ResourceCategory`: `EXAM_PAPER`, `LANGUAGE_TEST`, `RESUME_TEMPLATE`, `INTERVIEW_EXPERIENCE`, `OTHER`
- `ResourceStatus`: `PENDING`, `PUBLISHED`, `REJECTED`, `OFFLINE`
- `FavoriteTargetType` gains `RESOURCE`
- `app.resource-storage.type=local`
- `app.resource-storage.local-root=.local-storage/resources`
- `LocalResourceFileStorage` owns root-path creation and key-to-file resolution

Seed `data.sql` with at least:

- 2 published resources visible to the public
- 1 pending or rejected resource visible only to uploader/admin tests
- realistic `storage_key`, `file_name`, `file_size`, `content_type`, and `published_at` values

- [ ] **Step 4: Re-run the schema/config tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=CampusApplicationTests,ApplicationConfigSafetyTests" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml backend/src/main/java/com/campus/common/FavoriteTargetType.java backend/src/test/java/com/campus/CampusApplicationTests.java backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java backend/src/main/java/com/campus/common/ResourceCategory.java backend/src/main/java/com/campus/common/ResourceStatus.java backend/src/main/java/com/campus/config/ResourceStorageProperties.java backend/src/main/java/com/campus/storage/ResourceFileStorage.java backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java backend/src/main/java/com/campus/entity/ResourceItem.java backend/src/main/java/com/campus/mapper/ResourceItemMapper.java
git commit -m "feat: add phase d resource persistence model"
```

## Task 2: Implement Resource Upload, Public Browse, Detail, and Download APIs

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/campus/dto/ResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Create: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Create: `backend/src/main/java/com/campus/service/ResourceService.java`
- Create: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`

- [ ] **Step 1: Write failing resource controller tests**

```java
@Test
void guestCanReadPublishedResources() throws Exception {
    mockMvc.perform(get("/api/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.resources").isArray());
}

@Test
@WithMockUser(username = "2", roles = "USER")
void loggedInUserCanUploadResource() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "file", "resume-template.pdf", "application/pdf", "demo".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/resources")
                    .file(file)
                    .param("title", "2026 Resume Template")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Minimal resume template")
                    .param("description", "One-page starter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
}
```

- [ ] **Step 2: Run the failing resource controller tests**

Run:

```powershell
cd backend
mvn -q -Dtest=ResourceControllerTests test
```

Expected: FAIL because the controller, DTOs, service, and upload flow do not exist yet.

- [ ] **Step 3: Implement the public resource API slice**

Required endpoints:

```java
GET    /api/resources
GET    /api/resources/{id}
GET    /api/resources/{id}/download
POST   /api/resources
POST   /api/resources/{id}/favorite
DELETE /api/resources/{id}/favorite
```

Implementation rules:

- `GET /api/resources` only returns `PUBLISHED`
- supported filters:
  - `keyword`
  - `category`
- keyword should match `title`, `summary`, and `description`
- default sort should be `publishedAt DESC, id DESC`
- detail rejects `PENDING`, `REJECTED`, and `OFFLINE` for non-admin viewers
- download requires authentication and only works for `PUBLISHED`
- successful download increments `downloadCount`
- upload accepts `multipart/form-data`
- allowed file types: `pdf`, `docx`, `pptx`, `zip`
- maximum file size: `100MB`
- upload always creates `PENDING`
- favorite and unfavorite must be idempotent
- detail and list items should expose `favoritedByMe` when the viewer is authenticated
- invalid enum filters must return `400`

Minimum response shape:

```java
public record ResourceListResponse(
        String keyword,
        String category,
        int total,
        List<ResourceSummary> resources) {}
```

- [ ] **Step 4: Re-run the resource controller tests**

Run:

```powershell
cd backend
mvn -q -Dtest=ResourceControllerTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/dto/ResourceListResponse.java backend/src/main/java/com/campus/dto/ResourceDetailResponse.java backend/src/main/java/com/campus/controller/ResourceController.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java
git commit -m "feat: add public resource browse and upload apis"
```

## Task 3: Add Admin Review, My Resources, Favorites Branching, and Home Activation

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Create: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourceReviewRequest.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Create: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write failing favorites/home/admin tests**

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void resourceFavoritesReturnPublishedResources() throws Exception {
    mockMvc.perform(get("/api/users/me/favorites").param("type", "RESOURCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resources").isArray());
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanReviewPendingResources() throws Exception {
    mockMvc.perform(post("/api/admin/resources/3/publish"))
            .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run the failing favorites/home/admin tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=UserControllerTests,HomeControllerTests,AdminResourceControllerTests" test
```

Expected: FAIL because the `RESOURCE` favorites branch, home-entry activation, my-resources flow, and admin review endpoints do not exist yet.

- [ ] **Step 3: Implement admin review and resource branching**

Required endpoints:

```java
GET  /api/resources/mine
GET  /api/admin/resources
POST /api/admin/resources/{id}/publish
POST /api/admin/resources/{id}/reject
POST /api/admin/resources/{id}/offline
GET  /api/users/me/favorites?type=RESOURCE
```

Required behavior:

- `/api/resources/mine` is authenticated and returns uploader-owned rows across all statuses
- admin list includes pending, published, rejected, and offline rows
- publish moves `PENDING` or `OFFLINE` to `PUBLISHED`
- publish sets `publishedAt` and `reviewedAt`
- reject requires a non-empty reason and moves `PENDING` to `REJECTED`
- offline moves `PUBLISHED` to `OFFLINE`
- uploader and admin can inspect non-public rows through owned/admin endpoints only
- add `type=RESOURCE` to `/api/users/me/favorites` without regressing `POST` and `JOB`
- update home summary so the `resources` entry is live instead of `COMING_SOON`

Admin review request can stay intentionally small:

```java
public record AdminResourceReviewRequest(String reason) {}
```

- [ ] **Step 4: Re-run the favorites/home/admin tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=UserControllerTests,HomeControllerTests,AdminResourceControllerTests" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/controller/UserController.java backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/controller/UserControllerTests.java backend/src/test/java/com/campus/controller/HomeControllerTests.java backend/src/main/java/com/campus/dto/MyResourceListResponse.java backend/src/main/java/com/campus/dto/AdminResourceListResponse.java backend/src/main/java/com/campus/dto/AdminResourceReviewRequest.java backend/src/main/java/com/campus/controller/admin/AdminResourceController.java backend/src/main/java/com/campus/service/AdminResourceService.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: add admin review and resource favorites flows"
```

## Task 4: Build the Public Resource Frontend Experience

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Create: `frontend/src/api/resources.js`
- Create: `frontend/src/components/ResourceCard.vue`
- Create: `frontend/src/components/ResourceFilterBar.vue`
- Create: `frontend/src/views/ResourcesListView.vue`
- Create: `frontend/src/views/ResourceDetailView.vue`
- Create: `frontend/src/views/ResourceUploadView.vue`
- Create: `frontend/src/views/ResourcesListView.spec.js`
- Create: `frontend/src/views/ResourceDetailView.spec.js`
- Create: `frontend/src/views/ResourceUploadView.spec.js`

- [ ] **Step 1: Write failing resource view tests**

```js
import { mount } from "@vue/test-utils";
import ResourcesListView from "./ResourcesListView.vue";

test("resource list renders filters and resource cards", () => {
  const wrapper = mount(ResourcesListView, {
    global: { stubs: ["RouterLink"] },
  });

  expect(wrapper.find('input[name="keyword"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Resources");
});
```

```js
import { mount } from "@vue/test-utils";
import ResourceUploadView from "./ResourceUploadView.vue";

test("resource upload view renders the file input and submit action", () => {
  const wrapper = mount(ResourceUploadView, {
    global: { stubs: ["RouterLink"] },
  });

  expect(wrapper.find('input[type="file"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Upload");
});
```

- [ ] **Step 2: Run the failing resource frontend tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ResourcesListView.spec.js src/views/ResourceDetailView.spec.js src/views/ResourceUploadView.spec.js
```

Expected: FAIL because the routes, views, API client, and components do not exist yet.

- [ ] **Step 3: Build the public resource list/detail/upload flow**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required routes:

```js
{ path: "/resources", name: "resources", component: () => import("../views/ResourcesListView.vue") }
{ path: "/resources/:id", name: "resource-detail", component: () => import("../views/ResourceDetailView.vue") }
{ path: "/resources/upload", name: "resource-upload", component: () => import("../views/ResourceUploadView.vue"), meta: { requiresAuth: true } }
```

Required behavior:

- homepage resources entry must navigate to `/resources`
- resource list shows:
  - keyword search
  - category filter
  - empty, loading, and retry states
- resource cards show:
  - title
  - category
  - summary
  - uploader nickname
  - published time
  - download count
- detail page shows:
  - structured metadata
  - summary and description
  - download action
  - favorite / unfavorite action
- guests can read, but favorite and download redirect to login with `redirect=/resources/:id`
- upload page validates:
  - required title, category, summary, and file
  - extension whitelist
  - max-size hint and client-side guard
- successful upload redirects to `/profile/resources`

- [ ] **Step 4: Re-run the resource frontend tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ResourcesListView.spec.js src/views/ResourceDetailView.spec.js src/views/ResourceUploadView.spec.js src/views/HomeView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/api/resources.js frontend/src/components/ResourceCard.vue frontend/src/components/ResourceFilterBar.vue frontend/src/views/ResourcesListView.vue frontend/src/views/ResourceDetailView.vue frontend/src/views/ResourceUploadView.vue frontend/src/views/ResourcesListView.spec.js frontend/src/views/ResourceDetailView.spec.js frontend/src/views/ResourceUploadView.spec.js
git commit -m "feat: add public resource frontend flow"
```

## Task 5: Build My Resources, Resource Favorites Switching, and the Admin Review UI

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/views/ProfileView.spec.js`
- Modify: `frontend/src/views/ProfileFavoritesView.vue`
- Modify: `frontend/src/views/ProfileFavoritesView.spec.js`
- Create: `frontend/src/views/ProfileResourcesView.vue`
- Create: `frontend/src/views/ProfileResourcesView.spec.js`
- Create: `frontend/src/views/admin/AdminResourceManageView.vue`
- Create: `frontend/src/views/admin/AdminResourceManageView.spec.js`

- [ ] **Step 1: Write failing profile/admin resource UI tests**

```js
import { mount } from "@vue/test-utils";
import ProfileFavoritesView from "./ProfileFavoritesView.vue";

test("favorites view can switch to the resources collection", () => {
  const wrapper = mount(ProfileFavoritesView, {
    global: { stubs: ["RouterLink", "CommunityPostCard", "JobPostingCard", "ResourceCard"] },
  });

  expect(wrapper.text()).toContain("My Favorites");
});
```

```js
import { mount } from "@vue/test-utils";
import ProfileResourcesView from "./ProfileResourcesView.vue";

test("my resources view shows status labels and upload records", () => {
  const wrapper = mount(ProfileResourcesView, {
    global: { stubs: ["RouterLink"] },
  });

  expect(wrapper.text()).toContain("My Resources");
});
```

- [ ] **Step 2: Run the failing profile/admin resource tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ProfileFavoritesView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js src/views/ProfileView.spec.js
```

Expected: FAIL because the resource tab, my-resources page, profile shortcut, and admin review page do not exist yet.

- [ ] **Step 3: Implement profile/admin resource workflows**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required behavior:

- add `/profile/resources` route with `meta: { requiresAuth: true }`
- add `/admin/resources` route with `meta: { requiresAuth: true, roles: ["ADMIN"] }`
- add a visible profile quick entry to "My Resources"
- keep `/profile/favorites` as the single shell, but add a visible type switch:
  - `POST`
  - `JOB`
  - `RESOURCE`
- `RESOURCE` uses `/api/users/me/favorites?type=RESOURCE` through `frontend/src/api/resources.js`
- favorites render resource cards in a compact mode
- my-resources page is read-only and shows:
  - title
  - category
  - status
  - created time
  - reject reason when present
- admin resources page supports:
  - list all resources
  - publish pending resources
  - reject pending resources with reason
  - offline published resources
- admin page should work on desktop and mobile without a second route

- [ ] **Step 4: Re-run the profile/admin resource tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/ProfileFavoritesView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js src/views/ProfileView.spec.js
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/api/admin.js frontend/src/views/ProfileView.vue frontend/src/views/ProfileView.spec.js frontend/src/views/ProfileFavoritesView.vue frontend/src/views/ProfileFavoritesView.spec.js frontend/src/views/ProfileResourcesView.vue frontend/src/views/ProfileResourcesView.spec.js frontend/src/views/admin/AdminResourceManageView.vue frontend/src/views/admin/AdminResourceManageView.spec.js
git commit -m "feat: add resource profile and admin review ui"
```

## Task 6: Local Verification and Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for local Phase D resource development**

Document:

- backend local profile startup
- frontend local startup
- backend targeted test commands
- frontend targeted test commands
- local filesystem storage location
- allowed upload types and size limit
- public resource routes
- profile resource routes
- admin resource route
- guest, user, and admin permissions

- [ ] **Step 2: Run the backend verification set**

Run:

```powershell
cd backend
mvn -q test
```

Expected: PASS.

- [ ] **Step 3: Run the frontend verification set**

Run:

```powershell
cd frontend
npm run test -- --run
npm run build
```

Expected:

- frontend tests PASS
- frontend build PASS

- [ ] **Step 4: Run the local smoke pass**

Validate in this order:

1. Start backend locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`
2. Start frontend locally with `npm run dev -- --host 127.0.0.1`
3. As guest:
   - open `/resources`
   - filter by keyword or category
   - open `/resources/:id`
4. As a normal user:
   - upload one supported file
   - open `/profile/resources`
   - verify the new row appears as `PENDING`
5. As an admin:
   - open `/admin/resources`
   - publish or reject a pending row
   - if published, confirm it appears in `/resources`
   - offline it and confirm the public list/detail no longer expose it
6. As a normal user:
   - open a published resource detail page
   - download it
   - favorite it
   - open `/profile/favorites`
   - switch to `RESOURCE`
   - verify the favorited resource appears
7. Open `/` and verify the resources card now routes into the live resources module

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add phase d resource local run notes"
```

## Final Verification Set

After Task 6, run the full suite in this order:

```powershell
cd backend
mvn -q test
cd ../frontend
npm run test -- --run
npm run build
```

If any command fails, fix that failure before moving on to the next Phase D subproject.
