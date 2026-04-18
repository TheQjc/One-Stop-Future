# Study-Career Platform Phase L Decision Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first real analytics slice with a live public `/analytics` page, authenticated personal decision analytics, and home-page `analytics` entry activation.

**Architecture:** Build Phase L contract-first. Add one public analytics summary endpoint backed by real current domain tables, then extend it with authenticated personal decision analytics, then ship a Vue analytics route that renders public and personal regions on the same page, and finally activate the home entry and close with docs plus regression. Keep admin operations dashboards, tracking instrumentation, and heavy BI features out of scope.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, Mockito, MockMvc, Vue 3, Vue Router, Axios, Vitest

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-l-decision-analytics-design.md`
- Existing home aggregation backend:
  - `backend/src/main/java/com/campus/controller/HomeController.java`
  - `backend/src/main/java/com/campus/service/HomeService.java`
  - `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Existing decision-session source of truth:
  - `backend/src/main/java/com/campus/entity/DecisionAssessmentSession.java`
  - `backend/src/main/java/com/campus/mapper/DecisionAssessmentSessionMapper.java`
  - `backend/src/main/java/com/campus/service/DecisionAssessmentService.java`
- Existing public-content source tables and entities:
  - `backend/src/main/java/com/campus/entity/CommunityPost.java`
  - `backend/src/main/java/com/campus/entity/JobPosting.java`
  - `backend/src/main/java/com/campus/entity/ResourceItem.java`
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/resources/data.sql`
- Existing security and public-endpoint patterns:
  - `backend/src/main/java/com/campus/config/SecurityConfig.java`
  - `backend/src/main/java/com/campus/mapper/NotificationMapper.java`
- Existing frontend home and decision surfaces:
  - `frontend/src/router/index.js`
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/HomeView.spec.js`
  - `frontend/src/views/AssessmentView.vue`
  - `frontend/src/views/TimelineView.vue`
  - `frontend/src/views/SchoolCompareView.vue`
- Existing API adapter patterns:
  - `frontend/src/api/home.js`
  - `frontend/src/api/decision.js`

## Scope Lock

This plan covers only the approved Phase L slice:

- public `GET /api/analytics/summary?period=7D|30D`
- public `/analytics` route
- public overview cards, trends, and decision-distribution rendering
- authenticated personal decision snapshot, history, and next actions on the same page
- home `analytics` entry activation

This plan explicitly does not implement:

- admin-only operations dashboards
- DAU, retention, funnel, or tracking-driven metrics
- compare-history persistence or school-compare history analytics
- chart-library migration or dashboard-builder infrastructure
- exports, saved reports, or scheduled delivery

## Frontend Design Baseline

All UI tasks in this plan must explicitly use these skills before shipping:

- `@frontend-design`
  - preserve the current `editorial student decision desk` direction
  - avoid turning analytics into a generic admin dashboard
- `@ui-ux-pro-max`
  - review hierarchy, period switching clarity, card readability, empty states, and mobile layout before closing UI tasks

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/dto/AnalyticsSummaryResponse.java`
  Main analytics contract with nested public and personal sections.
- Create: `backend/src/main/java/com/campus/dto/AnalyticsTrendRow.java`
  Mapper projection for per-day trend counts.
- Create: `backend/src/main/java/com/campus/dto/AnalyticsDistributionRow.java`
  Mapper projection for latest-session-per-user track mix.
- Create: `backend/src/main/java/com/campus/mapper/AnalyticsReadMapper.java`
  Focused annotated SQL queries for analytics aggregation.
- Create: `backend/src/main/java/com/campus/service/AnalyticsService.java`
  Public summary aggregation, personal snapshot/history shaping, and period validation.
- Create: `backend/src/main/java/com/campus/controller/AnalyticsController.java`
  Public endpoint for analytics summary.
- Create: `backend/src/test/java/com/campus/service/AnalyticsServiceTests.java`
- Create: `backend/src/test/java/com/campus/controller/AnalyticsControllerTests.java`

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
  Permit `GET /api/analytics/summary`.
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
  Activate the home `analytics` entry for guests and authenticated users.
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  Cover live analytics entry behavior.
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
  Cover analytics entry visibility and badge removal.

### Frontend: Create

- Create: `frontend/src/api/analytics.js`
  Analytics summary adapter.
- Create: `frontend/src/views/AnalyticsView.vue`
  Public overview, trends, decision mix, and personal analytics desk.
- Create: `frontend/src/views/AnalyticsView.spec.js`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
  Add the public `/analytics` route.
- Modify: `frontend/src/views/HomeView.vue`
  Update analytics card copy and live navigation state.
- Modify: `frontend/src/views/HomeView.spec.js`
  Reflect the live analytics entry for guests and authenticated users.

### Docs: Modify Existing

- Modify: `README.md`
  Document the new endpoint, route, and verification path.

## Responsibility Notes

- `AnalyticsReadMapper` is the only place in this phase allowed to contain analytics-specific aggregation SQL.
- `AnalyticsService` owns period validation, zero-filled trend shaping, latest-session-per-user distribution math, and personal analytics assembly.
- `HomeService` only changes entry availability; it must not duplicate analytics aggregation.
- `AnalyticsController` stays public and viewer-aware; it should accept optional authentication context instead of requiring auth.
- The frontend analytics page owns guest versus authenticated presentation, not the backend security layer.
- The first slice should reuse `section-card`, `panel-card`, `dashboard-grid`, and `empty-state` patterns that already exist in `frontend/src/styles/base.css`.
- Public overview semantics must stay pinned to the spec:
  - `publishedPostCount` counts published public posts
  - `activeJobCount` counts the public job rows exposed by current product rules
  - `publishedResourceCount` counts published resources
  - `assessmentSessionCount` is the raw saved session volume and is not period-filtered
- Trend time sources must stay pinned to the spec:
  - posts use `t_community_post.created_at`
  - jobs use `t_job_posting.published_at`
  - resources use `t_resource_item.published_at`
  - assessments use `t_decision_assessment_session.session_date`

## Task 1: Add Public Analytics Summary Backend Contract

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AnalyticsSummaryResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AnalyticsTrendRow.java`
- Create: `backend/src/main/java/com/campus/dto/AnalyticsDistributionRow.java`
- Create: `backend/src/main/java/com/campus/mapper/AnalyticsReadMapper.java`
- Create: `backend/src/main/java/com/campus/service/AnalyticsService.java`
- Create: `backend/src/main/java/com/campus/controller/AnalyticsController.java`
- Create: `backend/src/test/java/com/campus/service/AnalyticsServiceTests.java`
- Create: `backend/src/test/java/com/campus/controller/AnalyticsControllerTests.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`

- [ ] **Step 1: Write the failing public-summary tests**

Create `AnalyticsControllerTests` with `@SpringBootTest`, `@AutoConfigureMockMvc`, and `@Sql("/schema.sql", "/data.sql")` coverage:

```java
@Test
void guestCanReadAnalyticsSummary() throws Exception {
    mockMvc.perform(get("/api/analytics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.publicOverview.publishedPostCount").isNumber())
            .andExpect(jsonPath("$.data.publicTrends.length()").value(30))
            .andExpect(jsonPath("$.data.decisionDistribution.tracks.length()").value(3))
            .andExpect(jsonPath("$.data.personalSnapshot").value(org.hamcrest.Matchers.nullValue()));
}

@Test
void invalidPeriodReturnsBusinessError() throws Exception {
    mockMvc.perform(get("/api/analytics/summary").param("period", "90D"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("invalid period"));
}
```

Create `AnalyticsServiceTests` coverage for deterministic public shaping:

```java
@Test
void summaryReturnsZeroFilledTrendSeriesAndExplicitTrackMix() {
    AnalyticsSummaryResponse response = service.summary(null, "7D");

    assertThat(response.publicTrends()).hasSize(7);
    assertThat(response.decisionDistribution().tracks()).extracting(TrackMixItem::track)
            .containsExactly("CAREER", "EXAM", "ABROAD");
    assertThat(response.personalSnapshot()).isNull();
}
```

- [ ] **Step 2: Run the targeted analytics backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests" test
```

Expected: FAIL because the analytics DTO, mapper, service, controller, and public security rule do not exist yet.

- [ ] **Step 3: Implement the minimal public analytics slice**

Create `AnalyticsSummaryResponse` with nested public-only sections already shaped for the frontend:

```java
public record AnalyticsSummaryResponse(
        PublicOverview publicOverview,
        List<TrendPoint> publicTrends,
        DecisionDistribution decisionDistribution,
        String personalStatus,
        String personalMessage,
        PersonalSnapshot personalSnapshot,
        List<PersonalHistoryItem> personalHistory,
        List<NextActionItem> nextActions) {
    public record PublicOverview(int publishedPostCount, int activeJobCount, int publishedResourceCount,
            int assessmentSessionCount) {}
    public record TrendPoint(LocalDate date, int posts, int jobs, int resources, int assessments) {}
    public record DecisionDistribution(int participantCount, List<TrackMixItem> tracks) {}
    public record TrackMixItem(String track, int count, double percent) {}
}
```

Create `AnalyticsReadMapper` with annotated queries, following the existing `NotificationMapper` style:

```java
@Select("SELECT DATE(created_at) AS bucket_date, COUNT(*) AS total FROM t_community_post " +
        "WHERE status = 'PUBLISHED' AND created_at >= #{start} GROUP BY DATE(created_at)")
List<AnalyticsTrendRow> summarizePublishedPostTrend(@Param("start") LocalDateTime start);
```

Use the correct source column per domain in the actual mapper methods:

```java
@Select("SELECT DATE(published_at) AS bucket_date, COUNT(*) AS total FROM t_job_posting " +
        "WHERE status = 'PUBLISHED' AND published_at IS NOT NULL AND published_at >= #{start} GROUP BY DATE(published_at)")
List<AnalyticsTrendRow> summarizePublishedJobTrend(@Param("start") LocalDateTime start);
```

Implement the minimal backend contract:

- permit `GET /api/analytics/summary` in `SecurityConfig`
- `AnalyticsService.summary(null, period)` validates `7D|30D`
- use current tables only:
  - `t_community_post`
  - `t_job_posting`
  - `t_resource_item`
  - `t_decision_assessment_session`
- pin public overview semantics to the spec:
  - posts = published public posts
  - jobs = current public job rows under existing visibility rules
  - resources = published resources
  - assessments = raw saved session volume
- zero-fill daily trend buckets in the service
- use the correct trend time sources:
  - posts -> `created_at`
  - jobs -> `published_at`
  - resources -> `published_at`
  - assessments -> `session_date`
- compute public decision mix from the latest saved session per user
- return guest-compatible:
  - `personalStatus="ANONYMOUS"`
  - `personalMessage=null`
  - `personalSnapshot=null`
  - `personalHistory=[]`
  - `nextActions=[]`

- [ ] **Step 4: Re-run the targeted analytics backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests" test
```

Expected: PASS with public access, deterministic trend length, explicit track mix, and business-error period validation.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/dto/AnalyticsSummaryResponse.java backend/src/main/java/com/campus/dto/AnalyticsTrendRow.java backend/src/main/java/com/campus/dto/AnalyticsDistributionRow.java backend/src/main/java/com/campus/mapper/AnalyticsReadMapper.java backend/src/main/java/com/campus/service/AnalyticsService.java backend/src/main/java/com/campus/controller/AnalyticsController.java backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/test/java/com/campus/service/AnalyticsServiceTests.java backend/src/test/java/com/campus/controller/AnalyticsControllerTests.java
git commit -m "feat: add public analytics summary endpoint"
```

## Task 2: Add Personal Analytics And Activate The Backend Home Entry

**Files:**
- Modify: `backend/src/main/java/com/campus/dto/AnalyticsSummaryResponse.java`
- Modify: `backend/src/main/java/com/campus/service/AnalyticsService.java`
- Modify: `backend/src/main/java/com/campus/controller/AnalyticsController.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/service/AnalyticsServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/AnalyticsControllerTests.java`
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`

- [ ] **Step 1: Write the failing authenticated analytics and home-entry tests**

Extend `AnalyticsControllerTests`:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void authenticatedUserReceivesPersonalSnapshotHistoryAndNextActions() throws Exception {
    mockMvc.perform(get("/api/analytics/summary").param("period", "7D"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.personalSnapshot.hasAssessment").value(true))
            .andExpect(jsonPath("$.data.personalSnapshot.recommendedTrack").isNotEmpty())
            .andExpect(jsonPath("$.data.personalHistory.length()").isNumber())
            .andExpect(jsonPath("$.data.nextActions.length()").isNumber());
}
```

Add no-history service coverage:

```java
@Test
void authenticatedUserWithoutAssessmentsGetsGuidedEmptyPersonalState() {
    AnalyticsSummaryResponse response = service.summary("9", "30D");

    assertThat(response.personalStatus()).isEqualTo("EMPTY");
    assertThat(response.personalSnapshot().hasAssessment()).isFalse();
    assertThat(response.personalHistory()).isEmpty();
    assertThat(response.nextActions()).extracting(NextActionItem::code)
            .containsExactly("START_ASSESSMENT");
}
```

Add partial-failure unit coverage so public analytics survives a personal-slice failure:

```java
@Test
void personalAnalyticsFailureFallsBackToPublicSummaryAndErrorState() {
    when(decisionAssessmentSessionMapper.selectList(any())).thenThrow(new RuntimeException("boom"));

    AnalyticsSummaryResponse response = service.summary("2", "30D");

    assertThat(response.publicOverview()).isNotNull();
    assertThat(response.personalStatus()).isEqualTo("ERROR");
    assertThat(response.personalMessage()).isEqualTo("Personal analytics temporarily unavailable.");
}
```

Update home tests to assert analytics is now live:

```java
assertThat(analytics.enabled()).isTrue();
assertThat(analytics.badge()).isNull();
```

and guest home now includes an enabled analytics entry:

```java
HomeEntryCard analytics = response.entries().stream()
        .filter(entry -> "analytics".equals(entry.code()))
        .findFirst()
        .orElseThrow();
assertThat(analytics.enabled()).isTrue();
assertThat(analytics.path()).isEqualTo("/analytics");
```

- [ ] **Step 2: Run the targeted authenticated analytics and home tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests,HomeServiceTests,HomeControllerTests" test
```

Expected: FAIL because personal analytics shaping and live home analytics entry behavior are not implemented yet.

- [ ] **Step 3: Implement personal analytics and home-entry activation**

Extend `AnalyticsSummaryResponse` with personal sections:

```java
public record PersonalSnapshot(
        boolean hasAssessment,
        String recommendedTrack,
        String summaryText,
        LocalDate sessionDate,
        ScoreBundle scores) {}

public record PersonalHistoryItem(
        LocalDate sessionDate,
        String recommendedTrack,
        int careerScore,
        int examScore,
        int abroadScore) {}

public record NextActionItem(String code, String label, String path, String description) {}
```

Implement in `AnalyticsService`:

- when `viewerIdentity` is absent:
  - return `personalStatus="ANONYMOUS"`
  - keep personal sections empty
- when the user has saved assessments:
  - return `personalStatus="READY"`
  - return `personalMessage=null`
  - load latest snapshot plus up to `5` recent sessions
  - shape next actions from the latest recommended track
- when the user has no saved assessments:
  - return `personalStatus="EMPTY"`
  - return `personalMessage=null`
  - return `hasAssessment=false`
  - return a guided `START_ASSESSMENT` action
- when personal analytics fails unexpectedly for an authenticated request:
  - keep public sections intact
  - return `personalStatus="ERROR"`
  - return `personalMessage="Personal analytics temporarily unavailable."`
  - return empty personal collections instead of failing the whole endpoint

Update `HomeService`:

- guest summary now includes analytics
- authenticated summary keeps analytics enabled
- analytics no longer carries `COMING_SOON`

- [ ] **Step 4: Re-run the targeted authenticated analytics and home tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests,HomeServiceTests,HomeControllerTests" test
```

Expected: PASS with authenticated personal analytics, clean no-history behavior, and live home analytics entry state.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/dto/AnalyticsSummaryResponse.java backend/src/main/java/com/campus/service/AnalyticsService.java backend/src/main/java/com/campus/controller/AnalyticsController.java backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/service/AnalyticsServiceTests.java backend/src/test/java/com/campus/controller/AnalyticsControllerTests.java backend/src/test/java/com/campus/service/HomeServiceTests.java backend/src/test/java/com/campus/controller/HomeControllerTests.java
git commit -m "feat: add personal analytics summary and activate home entry"
```

## Task 3: Add The Public Analytics Frontend Route And Guest Surface

**Files:**
- Create: `frontend/src/api/analytics.js`
- Create: `frontend/src/views/AnalyticsView.vue`
- Create: `frontend/src/views/AnalyticsView.spec.js`
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: Write the failing public analytics view tests**

Create `AnalyticsView.spec.js` to cover route and guest rendering:

```javascript
test("router exposes analytics as a public route", () => {
  expect(router.resolve("/analytics").meta.requiresAuth).toBeUndefined();
});

test("guest sees public analytics sections and a login prompt for personal insights", async () => {
  getAnalyticsSummary.mockResolvedValue({
    publicOverview: {
      publishedPostCount: 3,
      activeJobCount: 2,
      publishedResourceCount: 4,
      assessmentSessionCount: 5,
    },
    publicTrends: Array.from({ length: 30 }, (_, index) => ({
      date: `2026-04-${String(index + 1).padStart(2, "0")}`,
      posts: 0,
      jobs: 0,
      resources: 0,
      assessments: 0,
    })),
    decisionDistribution: {
      participantCount: 2,
      tracks: [
        { track: "CAREER", count: 1, percent: 50 },
        { track: "EXAM", count: 1, percent: 50 },
        { track: "ABROAD", count: 0, percent: 0 },
      ],
    },
    personalStatus: "ANONYMOUS",
    personalMessage: null,
    personalSnapshot: null,
    personalHistory: [],
    nextActions: [],
  });

  const wrapper = mount(AnalyticsView);
  await flushPromises();

  expect(wrapper.text()).toContain("Decision Mix");
  expect(wrapper.text()).toContain("Log in to unlock your personal path analysis");
});
```

Also cover period switching:

```javascript
await wrapper.find('[data-test="period-7D"]').trigger("click");
expect(getAnalyticsSummary).toHaveBeenLastCalledWith({ period: "7D" });
```

- [ ] **Step 2: Run the targeted analytics frontend tests and verify failure**

Run:

```powershell
cd frontend
npm run test -- src/views/AnalyticsView.spec.js
```

Expected: FAIL because the analytics API adapter, route, and view do not exist yet.

- [ ] **Step 3: Implement the public analytics route and guest view**

Before writing UI code, use `@frontend-design` to preserve the current editorial desk tone, then use `@ui-ux-pro-max` to review hierarchy and mobile layout.

Create `frontend/src/api/analytics.js`:

```javascript
import http from "./http.js";

export async function getAnalyticsSummary(params = {}) {
  const { data } = await http.get("/analytics/summary", { params });
  return data.data;
}
```

Add the public route:

```javascript
{
  path: "/analytics",
  name: "analytics",
  component: () => import("../views/AnalyticsView.vue"),
}
```

Implement `AnalyticsView.vue` so the public slice:

- loads `period=30D` by default
- renders overview cards, trends, and decision mix
- refetches from the backend when the user switches `7D` or `30D`
- renders a guest-safe personal panel from `personalStatus="ANONYMOUS"`
- uses existing `section-card`, `panel-card`, `dashboard-grid`, and `empty-state` patterns

- [ ] **Step 4: Re-run the targeted analytics frontend tests**

Run:

```powershell
cd frontend
npm run test -- src/views/AnalyticsView.spec.js
```

Expected: PASS with a public route, public analytics rendering, and period-switch refetch behavior.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/analytics.js frontend/src/views/AnalyticsView.vue frontend/src/views/AnalyticsView.spec.js frontend/src/router/index.js
git commit -m "feat: add public analytics view"
```

## Task 4: Add Personal Analytics Panels And Activate The Frontend Home Entry

**Files:**
- Modify: `frontend/src/views/AnalyticsView.vue`
- Modify: `frontend/src/views/AnalyticsView.spec.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`

- [ ] **Step 1: Write the failing personal analytics and home-entry frontend tests**

Extend `AnalyticsView.spec.js` with authenticated cases:

```javascript
test("logged-in user sees a personal snapshot and next actions", async () => {
  userStore.token = "demo-token";
  getAnalyticsSummary.mockResolvedValue({
    ...summaryFixture,
    personalStatus: "READY",
    personalMessage: null,
    personalSnapshot: {
      hasAssessment: true,
      recommendedTrack: "EXAM",
      summaryText: "Exam path currently leads.",
      sessionDate: "2026-04-18",
      scores: { career: 3, exam: 8, abroad: 4 },
    },
    personalHistory: [
      { sessionDate: "2026-04-18", recommendedTrack: "EXAM", careerScore: 3, examScore: 8, abroadScore: 4 },
    ],
    nextActions: [
      { code: "OPEN_TIMELINE", label: "Open Timeline", path: "/timeline", description: "Review milestones." },
    ],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Your Desk");
  expect(wrapper.text()).toContain("Open Timeline");
});

test("logged-in user without assessment history sees the assessment CTA", async () => {
  userStore.token = "demo-token";
  getAnalyticsSummary.mockResolvedValue({
    ...summaryFixture,
    personalStatus: "EMPTY",
    personalMessage: null,
    personalSnapshot: { hasAssessment: false, recommendedTrack: null, summaryText: null, sessionDate: null, scores: null },
    personalHistory: [],
    nextActions: [
      { code: "START_ASSESSMENT", label: "Start Assessment", path: "/assessment", description: "Unlock your personal path analytics." },
    ],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Start Assessment");
});
```

Also cover the personal error state:

```javascript
test("shows a personal analytics error card without hiding public sections", async () => {
  userStore.token = "demo-token";
  getAnalyticsSummary.mockResolvedValue({
    ...summaryFixture,
    personalStatus: "ERROR",
    personalMessage: "Personal analytics temporarily unavailable.",
    personalSnapshot: null,
    personalHistory: [],
    nextActions: [],
  });

  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Decision Mix");
  expect(wrapper.text()).toContain("Personal analytics temporarily unavailable.");
});
```

Update `HomeView.spec.js` so both guest and authenticated fixtures carry a live analytics entry:

```javascript
{ code: "analytics", title: "Analytics", path: "/analytics", enabled: true, badge: null }
```

and assert the link is rendered:

```javascript
expect(wrapper.html()).toContain('data-to="/analytics"');
```

- [ ] **Step 2: Run the targeted personal analytics and home frontend tests and verify failure**

Run:

```powershell
cd frontend
npm run test -- src/views/AnalyticsView.spec.js src/views/HomeView.spec.js
```

Expected: FAIL because the personal analytics panel and live home analytics copy are not implemented yet.

- [ ] **Step 3: Implement personal analytics rendering and home activation**

Update `AnalyticsView.vue` so it:

- renders guest messaging when `personalStatus="ANONYMOUS"`
- renders a personal snapshot when `personalStatus="READY"` and `personalSnapshot.hasAssessment=true`
- renders recent history for up to `5` sessions
- renders backend-provided next-action links
- shows a guided assessment CTA when `personalStatus="EMPTY"`
- shows an explicit personal error card when `personalStatus="ERROR"`
- keeps public analytics visible for all of those states

Update `HomeView.vue` so:

- analytics copy no longer reads as future-only
- analytics card routes to `/analytics`
- analytics meta label no longer defaults to `COMING_SOON`
- the surrounding copy positions analytics as the trend-and-path overview desk

- [ ] **Step 4: Re-run the targeted personal analytics and home frontend tests**

Run:

```powershell
cd frontend
npm run test -- src/views/AnalyticsView.spec.js src/views/HomeView.spec.js
```

Expected: PASS with guest/public analytics, authenticated personal desk behavior, and a live home analytics entry.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/AnalyticsView.vue frontend/src/views/AnalyticsView.spec.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js
git commit -m "feat: activate analytics entry and personal analytics panel"
```

## Task 5: Update Docs And Run Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for Phase L analytics**

Document:

- new backend endpoint:
  - `GET /api/analytics/summary?period=7D|30D`
- new frontend route:
  - `/analytics`
- public plus authenticated behavior split
- explicit note that admin operations dashboards remain out of scope in this phase
- local verification path:
  - open analytics as guest
  - log in
  - confirm personal analytics or assessment CTA

- [ ] **Step 2: Run the targeted backend verification set**

Run:

```powershell
cd backend
mvn -q "-Dtest=AnalyticsServiceTests,AnalyticsControllerTests,HomeServiceTests,HomeControllerTests" test
```

Expected: PASS.

- [ ] **Step 3: Run the targeted frontend verification set**

Run:

```powershell
cd frontend
npm run test -- src/views/AnalyticsView.spec.js src/views/HomeView.spec.js
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
2. open `/analytics` as a guest
3. confirm public overview, trend, and decision-mix sections render
4. log in as seeded user `13800000001`
5. open `/analytics`
6. if no assessment exists, confirm the personal desk points to `/assessment`
7. if an assessment exists, confirm the latest snapshot, history, and next actions render
8. return to `/` and confirm the `analytics` entry is live and no longer reads as coming soon

Then commit:

```bash
git add README.md
git commit -m "docs: add phase l analytics verification notes"
```
