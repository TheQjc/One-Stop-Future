# Study-Career Platform Phase C Job Aggregation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Completion status:** Completed on 2026-04-16.
> Delivered together with the Phase B community slice by commit `af7c1b7`, with plan/design and delivery-note documentation added in `0ab0d3a`.
> This slice remains part of the active baseline referenced by Phases D-H and the current README; later full backend/frontend regression runs through Phase H cover it as part of the integrated application.

**Goal:** Build the first Phase C jobs vertical slice: admin-maintained job cards, public browse/filter/detail pages, job favorites, home entry activation, and a minimal admin jobs workspace.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Add a focused `jobs` domain that mirrors the successful Phase B community layering: SQL seed data, MyBatis-Plus entity/mapper, service/controller pairs, focused view-level frontend APIs, and route-level admin gating. Reuse the existing `UserFavorite` table and `/api/users/me/favorites` entrypoint instead of introducing a second favorites system.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MySQL 8, H2 local/test profile, JWT, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-c-job-aggregation-design.md`
- Requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Existing implementation baseline:
  - `docs/superpowers/plans/2026-04-15-study-career-platform-phase-a-foundation-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-b-community-implementation.md`
- Current repo already has:
  - JWT login and profile center
  - unified home aggregation page with reserved `jobs` entry
  - reusable `t_user_favorite` table and `FavoriteTargetType`
  - community public flow and admin governance flow that should be treated as the reference pattern
- Safe local backend run must continue using:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

- Do not use Docker for this slice.

## Scope Lock

This plan covers only the Phase C jobs first slice:

- admin manual create/edit/publish/offline/delete job cards
- public `/jobs` list with keyword and structured filters
- public `/jobs/:id` detail page
- external source-link jump
- authenticated favorite / unfavorite
- reuse of `/api/users/me/favorites?type=JOB`
- activation of the homepage jobs entry
- admin `/admin/jobs` management page

This plan explicitly does not implement:

- CSV or Excel batch import
- third-party platform sync
- in-site application or resume upload
- recommendation, ranking, or hotness scoring
- unified search aggregation behavior
- resource library linkage
- notifications, subscriptions, or reminder workflows

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing layout, composition, spacing, typography, and visual hierarchy.
- `@ui-ux-pro-max`
  Use before closing each UI task to review clarity, mobile behavior, empty states, and action affordances.

Use the current visual system as the base:

- theme: `editorial student decision desk`
- keep the existing warm-paper, deep-navy, muted accent language
- preserve the current homepage/profile/admin shell patterns
- do not introduce a second design system for jobs

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/JobType.java`
- Create: `backend/src/main/java/com/campus/common/JobEducationRequirement.java`
- Create: `backend/src/main/java/com/campus/common/JobPostingStatus.java`
- Create: `backend/src/main/java/com/campus/entity/JobPosting.java`
- Create: `backend/src/main/java/com/campus/mapper/JobPostingMapper.java`
- Create: `backend/src/main/java/com/campus/dto/CreateJobRequest.java`
- Create: `backend/src/main/java/com/campus/dto/UpdateJobRequest.java`
- Create: `backend/src/main/java/com/campus/dto/JobListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/JobDetailResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminJobListResponse.java`
- Create: `backend/src/main/java/com/campus/controller/JobController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
- Create: `backend/src/main/java/com/campus/service/JobService.java`
- Create: `backend/src/main/java/com/campus/service/AdminJobService.java`
- Create: `backend/src/test/java/com/campus/controller/JobControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/views/ProfileFavoritesView.vue`
- Modify: `frontend/src/views/ProfileFavoritesView.spec.js`

### Frontend: Create

- Create: `frontend/src/api/jobs.js`
- Create: `frontend/src/components/JobPostingCard.vue`
- Create: `frontend/src/components/JobFilterBar.vue`
- Create: `frontend/src/views/JobsListView.vue`
- Create: `frontend/src/views/JobDetailView.vue`
- Create: `frontend/src/views/admin/AdminJobManageView.vue`
- Create: `frontend/src/views/JobsListView.spec.js`
- Create: `frontend/src/views/JobDetailView.spec.js`
- Create: `frontend/src/views/admin/AdminJobManageView.spec.js`

### Repo Docs

- Modify: `README.md`

## Task 1: Freeze the Job Schema and Persistence Model

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Modify: `backend/src/main/java/com/campus/common/FavoriteTargetType.java`
- Modify: `backend/src/test/java/com/campus/CampusApplicationTests.java`
- Create: `backend/src/main/java/com/campus/common/JobType.java`
- Create: `backend/src/main/java/com/campus/common/JobEducationRequirement.java`
- Create: `backend/src/main/java/com/campus/common/JobPostingStatus.java`
- Create: `backend/src/main/java/com/campus/entity/JobPosting.java`
- Create: `backend/src/main/java/com/campus/mapper/JobPostingMapper.java`

- [x] **Step 1: Write the failing schema smoke test**

```java
@SpringBootTest
class CampusApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void phaseCJobTableExists() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertThat(tableExists(metaData, "t_job_posting")).isTrue();
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

Expected: FAIL because `t_job_posting` does not exist yet.

- [x] **Step 3: Add the Phase C job schema, enum set, and seed data**

Add this minimum table:

```sql
CREATE TABLE t_job_posting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  company_name VARCHAR(80) NOT NULL,
  city VARCHAR(80) NOT NULL,
  job_type VARCHAR(20) NOT NULL,
  education_requirement VARCHAR(20) NOT NULL,
  source_platform VARCHAR(50) NOT NULL,
  source_url VARCHAR(500) NOT NULL,
  summary VARCHAR(300) NOT NULL,
  content TEXT NULL,
  deadline_at DATETIME NULL,
  published_at DATETIME NULL,
  status VARCHAR(20) NOT NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Model rules:

- `JobType`: `INTERNSHIP`, `FULL_TIME`, `CAMPUS`
- `JobEducationRequirement`: `ANY`, `BACHELOR`, `MASTER`, `DOCTOR`
- `JobPostingStatus`: `DRAFT`, `PUBLISHED`, `OFFLINE`, `DELETED`
- `FavoriteTargetType` gains `JOB`

Seed `data.sql` with at least:

- 2 published jobs visible to public readers
- 1 draft or offline job visible only to admins/tests
- realistic `source_url`, `source_platform`, `deadline_at`, and `published_at` values

- [x] **Step 4: Re-run the schema smoke test**

Run:

```powershell
cd backend
mvn -q -Dtest=CampusApplicationTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/common/FavoriteTargetType.java backend/src/test/java/com/campus/CampusApplicationTests.java backend/src/main/java/com/campus/common/JobType.java backend/src/main/java/com/campus/common/JobEducationRequirement.java backend/src/main/java/com/campus/common/JobPostingStatus.java backend/src/main/java/com/campus/entity/JobPosting.java backend/src/main/java/com/campus/mapper/JobPostingMapper.java
git commit -m "feat: add phase c job persistence model"
```

## Task 2: Implement the Public Jobs Backend APIs

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/campus/dto/JobListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/JobDetailResponse.java`
- Create: `backend/src/main/java/com/campus/controller/JobController.java`
- Create: `backend/src/main/java/com/campus/service/JobService.java`
- Create: `backend/src/test/java/com/campus/controller/JobControllerTests.java`

- [x] **Step 1: Write failing public jobs controller tests**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestCanReadPublishedJobsAndFilterByCity() throws Exception {
        mockMvc.perform(get("/api/jobs").param("city", "Shenzhen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobs").isArray());
    }

    @Test
    void guestCannotFavoriteJob() throws Exception {
        mockMvc.perform(post("/api/jobs/1/favorite"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [x] **Step 2: Run the failing public jobs tests**

Run:

```powershell
cd backend
mvn -q -Dtest=JobControllerTests test
```

Expected: FAIL because the controller/service/DTOs do not exist yet.

- [x] **Step 3: Implement the public jobs API slice**

Required endpoints:

```java
GET    /api/jobs
GET    /api/jobs/{id}
POST   /api/jobs/{id}/favorite
DELETE /api/jobs/{id}/favorite
```

Implementation rules:

- `GET /api/jobs` only returns `PUBLISHED`
- supported filters:
  - `keyword`
  - `city`
  - `jobType`
  - `educationRequirement`
  - `sourcePlatform`
- keyword should match `title`, `companyName`, and `summary`
- default sort should be `publishedAt DESC, id DESC`
- detail rejects `DRAFT`, `OFFLINE`, and `DELETED` for non-admin viewers
- summary/detail should expose `favoritedByMe` when the viewer is authenticated
- favorite and unfavorite must be idempotent
- invalid enum filters must return `400`

Minimum response shape:

```java
public record JobListResponse(
        String keyword,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        int total,
        List<JobSummary> jobs) {}
```

- [x] **Step 4: Re-run the public jobs tests**

Run:

```powershell
cd backend
mvn -q -Dtest=JobControllerTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/dto/JobListResponse.java backend/src/main/java/com/campus/dto/JobDetailResponse.java backend/src/main/java/com/campus/controller/JobController.java backend/src/main/java/com/campus/service/JobService.java backend/src/test/java/com/campus/controller/JobControllerTests.java
git commit -m "feat: add public job browse and favorite apis"
```

## Task 3: Add Admin Job Maintenance and Favorites/Home Wiring

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/UserController.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/controller/UserControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Create: `backend/src/main/java/com/campus/dto/CreateJobRequest.java`
- Create: `backend/src/main/java/com/campus/dto/UpdateJobRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminJobListResponse.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
- Create: `backend/src/main/java/com/campus/service/AdminJobService.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`

- [x] **Step 1: Write failing favorites/home/admin tests**

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void jobFavoritesReturnPublishedJobs() throws Exception {
    mockMvc.perform(get("/api/users/me/favorites").param("type", "JOB"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.jobs").isArray());
}

@Test
@WithMockUser(username = "2", roles = "USER")
void normalUserCannotOpenAdminJobsList() throws Exception {
    mockMvc.perform(get("/api/admin/jobs"))
            .andExpect(status().isForbidden());
}
```

- [x] **Step 2: Run the failing tests**

Run:

```powershell
cd backend
mvn -q -Dtest=UserControllerTests,HomeControllerTests,AdminJobControllerTests test
```

Expected: FAIL because the jobs favorites branch, home entry change, and admin jobs endpoints do not exist yet.

- [x] **Step 3: Implement admin job maintenance and jobs favorites branching**

Required endpoints:

```java
GET  /api/admin/jobs
POST /api/admin/jobs
PUT  /api/admin/jobs/{id}
POST /api/admin/jobs/{id}/publish
POST /api/admin/jobs/{id}/offline
POST /api/admin/jobs/{id}/delete
GET  /api/users/me/favorites?type=JOB
```

Required behavior:

- create defaults to `DRAFT`
- publish requires a non-empty `title`, `companyName`, `city`, `jobType`, `educationRequirement`, `sourcePlatform`, `sourceUrl`, and `summary`
- publish sets `publishedAt` when transitioning to `PUBLISHED`
- offline changes `PUBLISHED` to `OFFLINE`
- delete changes any non-deleted row to `DELETED`
- admin list includes draft, published, offline, and deleted rows
- keep `/api/users/me/favorites?type=POST` behavior backward-compatible
- add `type=JOB` branch that returns `JobListResponse`
- update home summary so the `jobs` entry is live instead of `COMING_SOON`

Request shape can stay intentionally simple:

```java
public record CreateJobRequest(
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        String sourceUrl,
        String summary,
        String content,
        LocalDateTime deadlineAt) {}
```

- [x] **Step 4: Re-run the admin/favorites/home tests**

Run:

```powershell
cd backend
mvn -q -Dtest=UserControllerTests,HomeControllerTests,AdminJobControllerTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/controller/UserController.java backend/src/main/java/com/campus/service/JobService.java backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/controller/UserControllerTests.java backend/src/test/java/com/campus/controller/HomeControllerTests.java backend/src/main/java/com/campus/dto/CreateJobRequest.java backend/src/main/java/com/campus/dto/UpdateJobRequest.java backend/src/main/java/com/campus/dto/AdminJobListResponse.java backend/src/main/java/com/campus/controller/admin/AdminJobController.java backend/src/main/java/com/campus/service/AdminJobService.java backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java
git commit -m "feat: add admin job maintenance and job favorites"
```

## Task 4: Build the Public Jobs Frontend Experience

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Create: `frontend/src/api/jobs.js`
- Create: `frontend/src/components/JobPostingCard.vue`
- Create: `frontend/src/components/JobFilterBar.vue`
- Create: `frontend/src/views/JobsListView.vue`
- Create: `frontend/src/views/JobDetailView.vue`
- Create: `frontend/src/views/JobsListView.spec.js`
- Create: `frontend/src/views/JobDetailView.spec.js`

- [x] **Step 1: Write failing jobs view tests**

```js
import { mount } from "@vue/test-utils";
import JobsListView from "./JobsListView.vue";

test("jobs list renders filter controls and result cards", () => {
  const wrapper = mount(JobsListView, {
    global: { stubs: ["RouterLink"] },
  });

  expect(wrapper.find('input[name="keyword"]').exists()).toBe(true);
  expect(wrapper.text()).toContain("Jobs");
});
```

```js
import { mount } from "@vue/test-utils";
import JobDetailView from "./JobDetailView.vue";

test("job detail exposes the source jump action", () => {
  const wrapper = mount(JobDetailView, {
    global: { stubs: ["RouterLink"] },
  });

  expect(wrapper.text()).toContain("Source");
});
```

- [x] **Step 2: Run the failing jobs frontend tests**

Run:

```powershell
cd frontend
npm run test -- src/views/JobsListView.spec.js src/views/JobDetailView.spec.js
```

Expected: FAIL because the jobs pages and API client do not exist yet.

- [x] **Step 3: Build the public jobs list/detail flow**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required routes:

```js
{ path: "/jobs", name: "jobs", component: () => import("../views/JobsListView.vue") }
{ path: "/jobs/:id", name: "job-detail", component: () => import("../views/JobDetailView.vue") }
```

Required behavior:

- homepage jobs entry and quick card must navigate to `/jobs`
- jobs list shows:
  - keyword search
  - city filter
  - job type filter
  - education filter
  - source platform filter
  - empty, loading, and retry states
- job cards show:
  - title
  - company
  - city
  - job type
  - education requirement
  - deadline
  - source platform
  - summary
- detail page shows:
  - structured metadata
  - summary and content
  - favorite / unfavorite
  - source link jump action
- guests can read, but favoriting redirects to login with `redirect=/jobs/:id`
- the jobs badge on home should no longer render as `COMING_SOON`

- [x] **Step 4: Re-run the jobs frontend tests**

Run:

```powershell
cd frontend
npm run test -- src/views/JobsListView.spec.js src/views/JobDetailView.spec.js src/views/HomeView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/api/jobs.js frontend/src/components/JobPostingCard.vue frontend/src/components/JobFilterBar.vue frontend/src/views/JobsListView.vue frontend/src/views/JobDetailView.vue frontend/src/views/JobsListView.spec.js frontend/src/views/JobDetailView.spec.js
git commit -m "feat: add public jobs frontend experience"
```

## Task 5: Extend Profile Favorites and Build the Admin Jobs UI

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/ProfileFavoritesView.vue`
- Modify: `frontend/src/views/ProfileFavoritesView.spec.js`
- Create: `frontend/src/views/admin/AdminJobManageView.vue`
- Create: `frontend/src/views/admin/AdminJobManageView.spec.js`

- [x] **Step 1: Write failing profile/admin jobs UI tests**

```js
import { mount } from "@vue/test-utils";
import ProfileFavoritesView from "./ProfileFavoritesView.vue";

test("favorites view can switch to the jobs collection", async () => {
  const wrapper = mount(ProfileFavoritesView, {
    global: { stubs: ["RouterLink", "CommunityPostCard", "JobPostingCard"] },
  });

  expect(wrapper.text()).toContain("My Favorites");
});
```

```js
import { mount } from "@vue/test-utils";
import AdminJobManageView from "./AdminJobManageView.vue";

test("admin jobs view exposes publish and offline actions", () => {
  const wrapper = mount(AdminJobManageView);
  expect(wrapper.text()).toContain("Publish");
  expect(wrapper.text()).toContain("Offline");
});
```

- [x] **Step 2: Run the failing profile/admin jobs tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ProfileFavoritesView.spec.js src/views/admin/AdminJobManageView.spec.js
```

Expected: FAIL because the jobs tab and admin jobs page do not exist yet.

- [x] **Step 3: Implement favorites switching and the admin jobs workspace**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required behavior:

- add `/admin/jobs` route with `meta: { requiresAuth: true, roles: ["ADMIN"] }`
- keep `/profile/favorites` as the single shell, but add a visible type switch:
  - `POST`
  - `JOB`
- `POST` continues to call the existing community favorites API
- `JOB` calls `/api/users/me/favorites?type=JOB` through `frontend/src/api/jobs.js`
- job favorites render with `JobPostingCard`
- admin jobs page supports:
  - list all jobs
  - create draft
  - edit draft/offline/published rows
  - publish
  - offline
  - delete
- the admin jobs page should be usable on desktop and mobile without a second route

- [x] **Step 4: Re-run the profile/admin jobs tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ProfileFavoritesView.spec.js src/views/admin/AdminJobManageView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/api/admin.js frontend/src/views/ProfileFavoritesView.vue frontend/src/views/ProfileFavoritesView.spec.js frontend/src/views/admin/AdminJobManageView.vue frontend/src/views/admin/AdminJobManageView.spec.js
git commit -m "feat: add job favorites tab and admin jobs ui"
```

## Task 6: Local Verification and Documentation

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README for local Phase C jobs development**

Document:

- backend local profile startup
- frontend local startup
- backend targeted test commands
- frontend targeted test commands
- public jobs routes
- admin jobs route
- guest, user, admin permissions
- profile favorites `POST` vs `JOB` usage

- [x] **Step 2: Run the backend verification set**

Run:

```powershell
cd backend
mvn -q test
```

Expected: PASS.

- [x] **Step 3: Run the frontend verification set**

Run:

```powershell
cd frontend
npm run test
npm run build
```

Expected:

- frontend tests PASS
- frontend build PASS

- [x] **Step 4: Run the local smoke pass**

Validate in this order:

1. Start backend locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`
2. Start frontend locally with `npm run dev -- --host 127.0.0.1`
3. As guest:
   - open `/jobs`
   - filter by city or job type
   - open `/jobs/:id`
   - verify the source link is present
4. As a normal user:
   - favorite a published job
   - open `/profile/favorites`
   - switch to `JOB`
   - verify the favorited job appears
5. As an admin:
   - open `/admin/jobs`
   - create a new draft
   - publish it
   - verify it appears in `/jobs`
   - offline it
   - verify public list/detail no longer expose it
6. Open `/` and verify the jobs card now routes into the live jobs module

- [x] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add phase c jobs local run notes"
```

## Final Verification Set

After Task 6, run the full suite in this order:

```powershell
cd backend
mvn -q test
cd ../frontend
npm run test
npm run build
```

If any command fails, fix that failure before moving on to the next Phase C subproject.
