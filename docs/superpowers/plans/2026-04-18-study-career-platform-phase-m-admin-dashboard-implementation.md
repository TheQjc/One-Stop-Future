# Study-Career Platform Phase M Admin Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a read-only admin operations dashboard with one aggregated backend summary endpoint, one admin-only `/admin/dashboard` route, and home/nav activation for admins.

**Architecture:** Build Phase M contract-first. Add one admin-only backend summary endpoint that aggregates verification, community, jobs, and resources directly from current tables, then wire admin home entry activation on the backend, then ship a Vue admin dashboard page that renders the unified overview from one payload, and finally expose the dashboard through admin home and nav before closing with docs and full verification. Keep all moderation and publishing actions in the existing specialized admin workbenches.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, MockMvc, JdbcTemplate, Vue 3, Vue Router, Pinia, Axios, Vitest

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-m-admin-dashboard-design.md`
- Existing admin backend workbenches:
  - `backend/src/main/java/com/campus/controller/admin/AdminVerificationController.java`
  - `backend/src/main/java/com/campus/controller/admin/AdminCommunityController.java`
  - `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
  - `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
  - `backend/src/main/java/com/campus/service/AdminVerificationService.java`
  - `backend/src/main/java/com/campus/service/AdminCommunityService.java`
  - `backend/src/main/java/com/campus/service/AdminJobService.java`
  - `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Existing admin DTO shapes to mirror:
  - `backend/src/main/java/com/campus/dto/AdminVerificationDashboardResponse.java`
  - `backend/src/main/java/com/campus/dto/AdminCommunityPostListResponse.java`
  - `backend/src/main/java/com/campus/dto/AdminJobListResponse.java`
  - `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Existing security and result wrapper behavior:
  - `backend/src/main/java/com/campus/config/SecurityConfig.java`
  - `backend/src/main/java/com/campus/common/Result.java`
- Existing home aggregation backend:
  - `backend/src/main/java/com/campus/service/HomeService.java`
  - `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  - `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Existing admin frontend surfaces:
  - `frontend/src/api/admin.js`
  - `frontend/src/router/index.js`
  - `frontend/src/views/admin/AdminVerificationReviewView.vue`
  - `frontend/src/views/admin/AdminCommunityManageView.vue`
  - `frontend/src/views/admin/AdminJobManageView.vue`
  - `frontend/src/views/admin/AdminResourceManageView.vue`
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/components/NavBar.vue`
- Existing frontend test patterns:
  - `frontend/src/views/admin/AdminCommunityManageView.spec.js`
  - `frontend/src/views/admin/AdminJobManageView.spec.js`
  - `frontend/src/views/admin/AdminResourceManageView.spec.js`
  - `frontend/src/views/HomeView.spec.js`
  - `frontend/src/components/NavBar.spec.js`

## Scope Lock

This plan covers only the approved Phase M slice:

- admin-only `GET /api/admin/dashboard/summary`
- admin-only `/admin/dashboard` route
- four read-only dashboard sections:
  - verification
  - community
  - jobs
  - resources
- summary counts, recent lists, and CTA links in each section
- new admin home entry:
  - `admin-dashboard`
- new admin nav link to `/admin/dashboard`

This plan explicitly does not implement:

- direct approve, reject, publish, offline, delete, or hide actions on the dashboard
- a moderation queue, workflow engine, or bulk operation surface
- analytics charts, trend history, or BI reporting
- search, filter, pagination, or drill-down inside the dashboard
- public access to any admin metrics

## Frontend Design Baseline

All UI tasks in this plan must explicitly use these skills before shipping:

- `@frontend-design`
  - preserve the current editorial desk direction
  - keep the dashboard as a calm overview board, not a dense control panel
- `@ui-ux-pro-max`
  - review hierarchy, card density, CTA clarity, empty states, and mobile behavior before closing UI tasks

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/dto/AdminDashboardSummaryResponse.java`
  Main dashboard response contract with nested verification, community, jobs, and resources sections.
- Create: `backend/src/main/java/com/campus/mapper/AdminDashboardReadMapper.java`
  Focused annotated SQL for counts and recent rows used only by the dashboard.
- Create: `backend/src/main/java/com/campus/service/AdminDashboardService.java`
  Read-only aggregation and response shaping for the unified admin dashboard.
- Create: `backend/src/main/java/com/campus/controller/admin/AdminDashboardController.java`
  Admin-only endpoint for the dashboard summary.
- Create: `backend/src/test/java/com/campus/controller/admin/AdminDashboardControllerTests.java`

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
  Add the `admin-dashboard` home entry for admins while keeping existing entries.
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  Cover `admin-dashboard` entry activation.
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
  Cover the new admin entry in the home summary response.

### Frontend: Create

- Create: `frontend/src/views/admin/AdminDashboardView.vue`
  Unified read-only admin overview page.
- Create: `frontend/src/views/admin/AdminDashboardView.spec.js`

### Frontend: Modify Existing

- Modify: `frontend/src/api/admin.js`
  Add the dashboard summary adapter.
- Modify: `frontend/src/router/index.js`
  Add the admin-only `/admin/dashboard` route.
- Modify: `frontend/src/views/HomeView.vue`
  Add the admin dashboard home card while keeping existing admin cards.
- Modify: `frontend/src/views/HomeView.spec.js`
  Cover the new admin dashboard entry in the home UI.
- Modify: `frontend/src/components/NavBar.vue`
  Add an admin-only dashboard link.
- Modify: `frontend/src/components/NavBar.spec.js`
  Cover admin-only dashboard navigation behavior.

### Docs: Modify Existing

- Modify: `README.md`
  Document the new admin dashboard route, endpoint, and verification path.

## Responsibility Notes

- `AdminDashboardReadMapper` is the only place in this phase allowed to contain dashboard-specific aggregation SQL.
- `AdminDashboardService` owns all section assembly and must not call existing paged admin list endpoints.
- `AdminDashboardController` must return `Result.success(...)` with the dashboard payload nested under `data`.
- This repo's success wrapper is `Result.success(...)` with `code=200` and `message="success"`. Do not implement the spec example as `code=0`.
- `SecurityConfig.java` should stay unchanged unless a test proves the new controller is unreachable for authenticated admins. Default authenticated access plus `@PreAuthorize("hasRole('ADMIN')")` already matches the approved scope.
- Verification semantics must stay pinned to the spec:
  - `pendingCount` counts only `PENDING`
  - `reviewedToday` uses the Spring Boot runtime timezone boundary
  - `latestPendingApplications` orders by `createdAt desc, id desc`
- Community semantics must stay pinned to the spec:
  - `totalCount` counts the full admin-visible universe
  - `latestPosts` includes `PUBLISHED`, `HIDDEN`, and `DELETED`
  - `latestPosts` orders by `createdAt desc, id desc`
- Jobs semantics must stay pinned to the spec:
  - `totalCount` excludes `DELETED`
  - `latestActionableJobs` includes only `DRAFT` and `OFFLINE`
  - `latestActionableJobs` orders by `updatedAt desc, id desc`
- Resource semantics must stay pinned to the spec:
  - `closedCount = REJECTED + OFFLINE`
  - `latestPendingResources` includes only `PENDING`
  - `latestPendingResources` orders by `createdAt desc, id desc`
- The frontend dashboard page fetches one payload, retries at page level on total failure, and never exposes inline mutation buttons.
- `HomeService` only adds summary-entry metadata. `HomeView.vue` still owns which cards actually render on the page.

## Task 1: Add The Backend Admin Dashboard Summary Endpoint

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AdminDashboardSummaryResponse.java`
- Create: `backend/src/main/java/com/campus/mapper/AdminDashboardReadMapper.java`
- Create: `backend/src/main/java/com/campus/service/AdminDashboardService.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminDashboardController.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminDashboardControllerTests.java`

- [ ] **Step 1: Write the failing admin dashboard controller tests**

Create `AdminDashboardControllerTests` using the same integration style as the existing admin controller tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminDashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotAccessDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminDashboardSummaryReturnsCountsAndRecentRows() throws Exception {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        insertPendingApplication(301L, 2L, startOfToday.plusHours(8));
        insertReviewedApplication(302L, 3L, startOfToday.plusHours(7), startOfToday.plusHours(9));
        insertCommunityPost(401L, 2L, "PUBLISHED", startOfToday.plusHours(6));
        insertCommunityPost(402L, 2L, "DELETED", startOfToday.plusHours(10));
        insertJob(501L, "DRAFT", startOfToday.plusHours(5));
        insertJob(502L, "OFFLINE", startOfToday.plusHours(11));
        insertResource(601L, 2L, "PENDING", startOfToday.plusHours(4));
        insertResource(602L, 2L, "REJECTED", startOfToday.plusHours(12));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verification.pendingCount").value(1))
                .andExpect(jsonPath("$.data.verification.reviewedToday").value(1))
                .andExpect(jsonPath("$.data.community.deletedCount").value(1))
                .andExpect(jsonPath("$.data.community.latestPosts[0].id").value(402))
                .andExpect(jsonPath("$.data.community.latestPosts[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.jobs.draftCount").value(1))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[0].id").value(502))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[0].status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.resources.pendingCount").value(1))
                .andExpect(jsonPath("$.data.resources.closedCount").value(1))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].id").value(601))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].status").value("PENDING"));
    }
}
```

Add one more failing test that clears actionable rows and verifies empty lists stay present:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminDashboardSummaryKeepsEmptyRecentLists() throws Exception {
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

    jdbcTemplate.update(
            "UPDATE t_verification_application SET status = 'APPROVED', reviewed_at = ?",
            startOfToday.plusHours(1));
    jdbcTemplate.update("UPDATE t_job_posting SET status = 'PUBLISHED' WHERE status IN ('DRAFT', 'OFFLINE')");
    jdbcTemplate.update("UPDATE t_resource_item SET status = 'PUBLISHED' WHERE status = 'PENDING'");

    mockMvc.perform(get("/api/admin/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verification.latestPendingApplications.length()").value(0))
            .andExpect(jsonPath("$.data.jobs.latestActionableJobs.length()").value(0))
            .andExpect(jsonPath("$.data.resources.latestPendingResources.length()").value(0));
}
```

Add one more failing test that pins the spec-required `up to 5` behavior:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminDashboardSummaryCapsEachRecentListAtFiveItems() throws Exception {
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

    for (long index = 0; index < 6; index++) {
        insertPendingApplication(500L + index, 2L, startOfToday.plusMinutes(index));
        insertCommunityPost(600L + index, 2L, "PUBLISHED", startOfToday.plusMinutes(index));
        insertJob(700L + index, "DRAFT", startOfToday.plusMinutes(index));
        insertResource(800L + index, 2L, "PENDING", startOfToday.plusMinutes(index));
    }

    mockMvc.perform(get("/api/admin/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verification.latestPendingApplications.length()").value(5))
            .andExpect(jsonPath("$.data.community.latestPosts.length()").value(5))
            .andExpect(jsonPath("$.data.jobs.latestActionableJobs.length()").value(5))
            .andExpect(jsonPath("$.data.resources.latestPendingResources.length()").value(5));
}
```

- [ ] **Step 2: Run the targeted backend dashboard tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminDashboardControllerTests" test
```

Expected: FAIL because the dashboard DTO, mapper, service, and controller do not exist yet.

- [ ] **Step 3: Implement the minimal backend dashboard slice**

Create `AdminDashboardSummaryResponse` with one section per domain. Reuse the existing admin item field sets exactly so the dashboard contract stays aligned with the current admin workbenches:

- verification latest item fields must match `AdminVerificationDashboardResponse.VerificationApplicationSummary`
- community latest item fields must match `AdminCommunityPostListResponse.PostItem`
- jobs latest item fields must match `AdminJobListResponse.JobItem`
- resources latest item fields must match `AdminResourceListResponse.ResourceItem`

```java
public record AdminDashboardSummaryResponse(
        VerificationSection verification,
        CommunitySection community,
        JobsSection jobs,
        ResourcesSection resources) {

    public record VerificationSection(
            int pendingCount,
            int reviewedToday,
            List<AdminVerificationDashboardResponse.VerificationApplicationSummary> latestPendingApplications) {}

    public record CommunitySection(
            int totalCount,
            int publishedCount,
            int hiddenCount,
            int deletedCount,
            List<AdminCommunityPostListResponse.PostItem> latestPosts) {}

    public record JobsSection(
            int totalCount,
            int draftCount,
            int publishedCount,
            int offlineCount,
            List<AdminJobListResponse.JobItem> latestActionableJobs) {}

    public record ResourcesSection(
            int totalCount,
            int pendingCount,
            int publishedCount,
            int closedCount,
            List<AdminResourceListResponse.ResourceItem> latestPendingResources) {}
}
```

Create `AdminDashboardReadMapper` with focused queries for counts and recent rows. Keep the queries direct and deterministic:

```java
int countPendingVerificationApplications();
int countReviewedVerificationApplicationsBetween(LocalDateTime start, LocalDateTime end);
List<AdminVerificationDashboardResponse.VerificationApplicationSummary> selectLatestPendingApplications(@Param("limit") int limit);

int countCommunityPosts();
int countCommunityPostsByStatus(@Param("status") String status);
List<AdminCommunityPostListResponse.PostItem> selectLatestCommunityPosts(@Param("limit") int limit);

int countNonDeletedJobs();
int countJobsByStatus(@Param("status") String status);
List<AdminJobListResponse.JobItem> selectLatestActionableJobs(@Param("limit") int limit);

int countResources();
int countResourcesByStatus(@Param("status") String status);
int countClosedResources();
List<ResourceItem> selectLatestPendingResourceRows(@Param("limit") int limit);

@Select("""
        SELECT p.id,
               p.user_id AS userId,
               COALESCE(u.nickname, 'Unknown') AS applicantNickname,
               p.real_name AS realName,
               p.student_id AS studentId,
               p.status,
               p.created_at AS createdAt
        FROM t_verification_application p
        LEFT JOIN t_user u ON u.id = p.user_id
        WHERE p.status = 'PENDING'
        ORDER BY p.created_at DESC, p.id DESC
        LIMIT #{limit}
        """)
List<AdminVerificationDashboardResponse.VerificationApplicationSummary> selectLatestPendingApplications(@Param("limit") int limit);
```

Use the same pattern for the other domains:

- community latest rows join `t_user` for `authorNickname`
- jobs latest actionable rows filter `status IN ('DRAFT', 'OFFLINE')`
- resources latest pending rows join `t_user` for `uploaderNickname`
- count methods return simple `int`

Implement `AdminDashboardService` so it:

- defines one shared dashboard recent-list limit:
  - `private static final int RECENT_LIMIT = 5;`
- builds `verification`, `community`, `jobs`, and `resources` from the mapper
- computes `reviewedToday` from the current runtime date boundary
- maps `closedCount` as `REJECTED + OFFLINE`
- enforces `RECENT_LIMIT` for every `latest...` list
- never derives counts from existing admin list endpoints
- never performs mutation logic

For resources specifically, do not try to reconstruct preview metadata in SQL. Let the mapper return the latest pending resource rows or ids needed for ordering, then have `AdminDashboardService` map them into `AdminResourceListResponse.ResourceItem` using the existing `ResourceService` preview helpers:

```java
new AdminResourceListResponse.ResourceItem(
        resource.getId(),
        resource.getTitle(),
        resource.getCategory(),
        uploaderNickname,
        resource.getFileName(),
        resource.getFileSize(),
        resource.getDownloadCount(),
        resource.getStatus(),
        resource.getRejectReason(),
        resource.getCreatedAt(),
        resource.getReviewedAt(),
        resource.getPublishedAt(),
        resourceService.isPreviewAvailableForAdmin(resource),
        resourceService.previewKindForAdmin(resource))
```

Create `AdminDashboardController`:

```java
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryResponse> summary() {
        return Result.success(adminDashboardService.getSummary());
    }
}
```

- [ ] **Step 4: Re-run the targeted backend dashboard tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminDashboardControllerTests" test
```

Expected: PASS with admin-only access, correct counts, deterministic ordering, and empty recent-list structure.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/dto/AdminDashboardSummaryResponse.java backend/src/main/java/com/campus/mapper/AdminDashboardReadMapper.java backend/src/main/java/com/campus/service/AdminDashboardService.java backend/src/main/java/com/campus/controller/admin/AdminDashboardController.java backend/src/test/java/com/campus/controller/admin/AdminDashboardControllerTests.java
git commit -m "feat: add admin dashboard summary endpoint"
```

## Task 2: Activate The Backend Admin Home Entry

**Files:**
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`

- [ ] **Step 1: Write the failing admin home-entry tests**

Extend `HomeServiceTests` with an admin-entry assertion:

```java
@Test
void adminSummaryIncludesDashboardAndKeepsVerificationEntry() {
    when(discoverService.previewForHome(4)).thenReturn(List.of());

    User admin = new User();
    admin.setId(1L);
    admin.setPhone("13800000000");
    admin.setNickname("PlatformAdmin");
    admin.setRole("ADMIN");
    admin.setVerificationStatus("VERIFIED");

    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("1");
    when(userService.requireByIdentity("1")).thenReturn(admin);
    when(notificationService.countUnreadByUserId(1L)).thenReturn(0);
    when(notificationService.listLatestSnippets(1L, 5)).thenReturn(List.of());

    HomeSummaryResponse response = homeService.getSummary(authentication);

    HomeSummaryResponse.HomeEntryCard dashboard = response.entries().stream()
            .filter(entry -> "admin-dashboard".equals(entry.code()))
            .findFirst()
            .orElseThrow();

    assertThat(dashboard.path()).isEqualTo("/admin/dashboard");
    assertThat(dashboard.enabled()).isTrue();
    assertThat(response.entries()).anyMatch(entry -> "admin-verifications".equals(entry.code()));
}
```

Extend `HomeControllerTests`:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminSummaryIncludesAdminDashboardEntry() throws Exception {
    mockMvc.perform(get("/api/home/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.viewerType").value("ADMIN"))
            .andExpect(jsonPath("$.data.entries[?(@.code=='admin-dashboard')].length()").value(1))
            .andExpect(jsonPath("$.data.entries[?(@.code=='admin-dashboard')][0].path").value("/admin/dashboard"))
            .andExpect(jsonPath("$.data.entries[?(@.code=='admin-verifications')].length()").value(1));
}
```

- [ ] **Step 2: Run the targeted backend home tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=HomeServiceTests,HomeControllerTests" test
```

Expected: FAIL because the `admin-dashboard` home entry does not exist yet.

- [ ] **Step 3: Implement backend home-entry activation**

Update `HomeService.buildEntries(User user)` so admins receive the new live dashboard entry before the existing admin verification entry:

```java
if ("ADMIN".equals(user.getRole())) {
    entries.add(new HomeSummaryResponse.HomeEntryCard(
            "admin-dashboard",
            "Admin Dashboard",
            "/admin/dashboard",
            true,
            null));
    entries.add(new HomeSummaryResponse.HomeEntryCard(
            "admin-verifications",
            "Admin Verification Review",
            "/admin/verifications",
            true,
            null));
}
```

Keep guest and normal-user entry behavior unchanged.

- [ ] **Step 4: Re-run the targeted backend home tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=HomeServiceTests,HomeControllerTests" test
```

Expected: PASS with the new `admin-dashboard` entry and no regression to existing admin verification entry behavior.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/service/HomeServiceTests.java backend/src/test/java/com/campus/controller/HomeControllerTests.java
git commit -m "feat: add admin dashboard home entry"
```

## Task 3: Add The Frontend Admin Dashboard Route And Success Surface

**Files:**
- Create: `frontend/src/views/admin/AdminDashboardView.vue`
- Create: `frontend/src/views/admin/AdminDashboardView.spec.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: Write the failing admin dashboard view tests**

Create `AdminDashboardView.spec.js` with mocked admin API behavior:

```javascript
import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import AdminDashboardView from "./AdminDashboardView.vue";
import { getAdminDashboardSummary } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  getAdminDashboardSummary: vi.fn(),
}));

const summaryFixture = {
  verification: {
    pendingCount: 2,
    reviewedToday: 1,
    latestPendingApplications: [
      { id: 1, applicantNickname: "NormalUser", realName: "Normal User", studentId: "20260009", status: "PENDING", createdAt: "2026-04-18T09:30:00" },
    ],
  },
  community: {
    totalCount: 8,
    publishedCount: 5,
    hiddenCount: 2,
    deletedCount: 1,
    latestPosts: [
      {
        id: 2,
        tag: "CAREER",
        title: "Moderation sample",
        status: "PUBLISHED",
        authorId: 1,
        authorNickname: "CampusAdmin",
        likeCount: 2,
        commentCount: 1,
        favoriteCount: 3,
        createdAt: "2026-04-18T08:00:00",
      },
    ],
  },
  jobs: {
    totalCount: 6,
    draftCount: 2,
    publishedCount: 3,
    offlineCount: 1,
    latestActionableJobs: [
      {
        id: 3,
        title: "Draft job",
        companyName: "Campus Future",
        city: "Shanghai",
        jobType: "INTERNSHIP",
        educationRequirement: "BACHELOR",
        sourcePlatform: "Official Site",
        status: "DRAFT",
        summary: "A draft campus role",
        deadlineAt: "2026-05-01T18:00:00",
        publishedAt: null,
        updatedAt: "2026-04-18T07:00:00",
      },
    ],
  },
  resources: {
    totalCount: 7,
    pendingCount: 1,
    publishedCount: 4,
    closedCount: 2,
    latestPendingResources: [
      {
        id: 4,
        title: "Pending archive",
        category: "NOTES",
        uploaderNickname: "NormalUser",
        fileName: "archive.pdf",
        fileSize: 10240,
        downloadCount: 3,
        status: "PENDING",
        rejectReason: null,
        createdAt: "2026-04-18T06:00:00",
        reviewedAt: null,
        publishedAt: null,
        previewAvailable: true,
        previewKind: "PDF",
      },
    ],
  },
};

test("loads the admin summary and renders the four section CTAs", async () => {
  getAdminDashboardSummary.mockResolvedValue(summaryFixture);

  const wrapper = mount(AdminDashboardView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='typeof to === \"string\" ? to : JSON.stringify(to)'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(getAdminDashboardSummary).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("Verification");
  expect(wrapper.text()).toContain("Community");
  expect(wrapper.text()).toContain("Jobs");
  expect(wrapper.text()).toContain("Resources");
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
  expect(wrapper.html()).toContain('data-to="/admin/community"');
  expect(wrapper.html()).toContain('data-to="/admin/jobs"');
  expect(wrapper.html()).toContain('data-to="/admin/resources"');
});
```

Add a failure-and-retry test:

```javascript
test("shows a page-level retry when summary loading fails", async () => {
  getAdminDashboardSummary
    .mockRejectedValueOnce(new Error("dashboard load failed"))
    .mockResolvedValueOnce(summaryFixture);

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("dashboard load failed");

  await wrapper.find("button.ghost-btn").trigger("click");
  await flushPromises();

  expect(getAdminDashboardSummary).toHaveBeenCalledTimes(2);
});
```

- [ ] **Step 2: Run the targeted frontend dashboard tests and verify failure**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminDashboardView.spec.js
```

Expected: FAIL because the dashboard view, route, and API adapter do not exist yet.

- [ ] **Step 3: Implement the frontend dashboard route and success state**

Before writing UI code, use `@frontend-design`, then review the finished surface with `@ui-ux-pro-max`.

Update `frontend/src/api/admin.js`:

```javascript
export async function getAdminDashboardSummary() {
  const { data } = await http.get("/admin/dashboard/summary");
  return data.data;
}
```

Add the route in `frontend/src/router/index.js`:

```javascript
{
  path: "/admin/dashboard",
  name: "admin-dashboard",
  component: () => import("../views/admin/AdminDashboardView.vue"),
  meta: { requiresAuth: true, roles: ["ADMIN"] },
}
```

Implement `AdminDashboardView.vue` so it:

- loads once on page entry
- relies on the existing router `meta.roles: ["ADMIN"]` guard exactly like the other admin routes
- renders one page-level loading state
- renders one page-level error state with retry on total failure
- shows four section cards from the single summary payload
- exposes only CTA links to:
  - `/admin/verifications`
  - `/admin/community`
  - `/admin/jobs`
  - `/admin/resources`
- uses existing layout primitives such as `section-card`, `panel-card`, `dashboard-grid`, `empty-state`, and `app-link`

- [ ] **Step 4: Re-run the targeted frontend dashboard tests**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminDashboardView.spec.js
```

Expected: PASS with dashboard loading, page-level retry, and four working CTA links.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/admin/AdminDashboardView.vue frontend/src/views/admin/AdminDashboardView.spec.js frontend/src/api/admin.js frontend/src/router/index.js
git commit -m "feat: add admin dashboard view"
```

## Task 4: Activate Home And Nav Entry Points And Finish Empty-State Behavior

**Files:**
- Modify: `frontend/src/views/admin/AdminDashboardView.vue`
- Modify: `frontend/src/views/admin/AdminDashboardView.spec.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/components/NavBar.spec.js`

- [ ] **Step 1: Write the failing home, nav, and empty-state tests**

Extend `AdminDashboardView.spec.js`:

```javascript
test("keeps CTA links visible when recent lists are empty", async () => {
  getAdminDashboardSummary.mockResolvedValue({
    verification: { pendingCount: 0, reviewedToday: 0, latestPendingApplications: [] },
    community: { totalCount: 0, publishedCount: 0, hiddenCount: 0, deletedCount: 0, latestPosts: [] },
    jobs: { totalCount: 0, draftCount: 0, publishedCount: 0, offlineCount: 0, latestActionableJobs: [] },
    resources: { totalCount: 0, pendingCount: 0, publishedCount: 0, closedCount: 0, latestPendingResources: [] },
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("No pending verification applications");
  expect(wrapper.text()).toContain("No actionable jobs right now");
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
  expect(wrapper.html()).toContain('data-to="/admin/jobs"');
});
```

Extend `HomeView.spec.js` with an admin fixture that includes `admin-dashboard`:

```javascript
const adminSummary = {
  ...authenticatedSummary,
  viewerType: "ADMIN",
  identity: {
    userId: 1,
    phone: "13800000000",
    nickname: "PlatformAdmin",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
  },
  entries: [
    { code: "community", title: "Community", path: "/community", enabled: true, badge: null },
    { code: "jobs", title: "Jobs", path: "/jobs", enabled: true, badge: null },
    { code: "resources", title: "Resources", path: "/resources", enabled: true, badge: null },
    { code: "assessment", title: "Assessment", path: "/assessment", enabled: true, badge: null },
    { code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: null },
    { code: "admin-dashboard", title: "Admin Dashboard", path: "/admin/dashboard", enabled: true, badge: null },
    { code: "admin-verifications", title: "Admin Verification Review", path: "/admin/verifications", enabled: true, badge: null },
  ],
};

test("admin home renders the dashboard entry alongside existing admin cards", async () => {
  getHomeSummary.mockResolvedValue(adminSummary);

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.html()).toContain('data-to="/admin/dashboard"');
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
  expect(wrapper.html()).toContain('data-to="/admin/community"');
  expect(wrapper.html()).toContain('data-to="/admin/jobs"');
});
```

Extend `NavBar.spec.js`:

```javascript
test("navbar shows the admin dashboard link for admins", () => {
  const userStore = useUserStore();
  userStore.token = "demo-token";
  userStore.profile = {
    id: 1,
    userId: 1,
    nickname: "Admin",
    phone: "13800000000",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 2,
  };

  const wrapper = mountNavBar();

  expect(wrapper.html()).toContain('data-to="/admin/dashboard"');
  expect(wrapper.html()).toContain('data-to="/admin/verifications"');
});
```

- [ ] **Step 2: Run the targeted home, nav, and empty-state tests and verify failure**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminDashboardView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

Expected: FAIL because the home card, nav link, and section empty-state copy are not implemented yet.

- [ ] **Step 3: Implement admin entry activation and section empty states**

Update `AdminDashboardView.vue` so each section:

- always keeps the metric summary visible
- renders a concise empty-state message when the recent list is empty
- keeps its CTA link visible even when the list is empty
- does not render mutation buttons copied from the workbench pages

Update `HomeView.vue`:

- add `admin-dashboard` to `entryMetaMap`
- add a new admin dashboard card in `serviceCards`
- place the dashboard card before the existing admin verification, community, and jobs cards
- keep the existing admin cards unchanged

Update `NavBar.vue`:

- add `{ to: "/admin/dashboard", label: "Admin Dashboard" }` for admins
- keep existing admin nav items intact

- [ ] **Step 4: Re-run the targeted home, nav, and empty-state tests**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminDashboardView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

Expected: PASS with stable empty states, a live home dashboard entry, and an admin-only nav link.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/admin/AdminDashboardView.vue frontend/src/views/admin/AdminDashboardView.spec.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js
git commit -m "feat: activate admin dashboard entry points"
```

## Task 5: Update Docs And Run Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for Phase M admin dashboard**

Document:

- new backend endpoint:
  - `GET /api/admin/dashboard/summary`
- new frontend route:
  - `/admin/dashboard`
- admin-only access boundary
- read-only overview behavior and CTA handoff to existing workbenches
- local verification path:
  - log in as admin
  - open `/admin/dashboard`
  - verify home and nav entry points

- [ ] **Step 2: Run the targeted backend verification set**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminDashboardControllerTests,HomeServiceTests,HomeControllerTests" test
```

Expected: PASS.

- [ ] **Step 3: Run the targeted frontend verification set**

Run:

```powershell
cd frontend
npm run test -- src/views/admin/AdminDashboardView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

Expected: PASS.

- [ ] **Step 4: Run full regression and build**

Run:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run test
npm run build
```

Expected: PASS across the full backend and frontend suites.

- [ ] **Step 5: Manual smoke and commit**

Manual smoke:

1. start backend and frontend in local profile
2. log in as seeded admin user `13800000000`
3. open `/admin/dashboard`
4. confirm the verification, community, jobs, and resources sections render
5. confirm each section shows counts and either a recent list or an empty-state message
6. confirm each CTA reaches the correct existing workbench
7. open `/`
8. confirm the admin home dashboard entry reaches `/admin/dashboard`
9. confirm the nav bar shows the admin dashboard link and it works
10. confirm the dashboard page exposes no approve, reject, publish, offline, hide, or delete buttons

Then commit:

```bash
git add README.md
git commit -m "docs: add phase m admin dashboard verification notes"
```
