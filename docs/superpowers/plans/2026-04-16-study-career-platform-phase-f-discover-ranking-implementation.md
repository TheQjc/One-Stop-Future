# Study-Career Platform Phase F Discover Ranking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Phase F first-slice discover ranking flow: a public backend `/api/discover` API plus homepage discover preview, navbar entry, and a dedicated `/discover` page that can browse weekly and all-time hot content across published community posts, jobs, and resources.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Add a narrow discover boundary on the backend with `DiscoverController` and `DiscoverService`, and expose read-only discover candidate methods from the existing community, jobs, and resources services instead of moving domain visibility logic into the controller. On the frontend, add a dedicated `/discover` route backed by URL query params so discover state is shareable, refresh-safe, and aligned with the existing `/search` page contract, while extending `HomeService` and `HomeView` with a small `discoverPreview` block that reuses the same item structure.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 local/test profile, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-f-discover-ranking-design.md`
- Requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Existing implementation baseline:
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-b-community-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-c-job-aggregation-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-d-resource-library-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-e-unified-search-implementation.md`
- Current repo already has:
  - public published-content list/detail flows for community, jobs, and resources
  - a public home summary endpoint at `GET /api/home/summary`
  - a consistent `Result.success(...)` / `Result.error(...)` JSON response envelope
  - `BusinessException` plus `GlobalExceptionHandler` returning business failures inside HTTP `200` for most validation errors
  - a navbar and homepage already acting as cross-module entry points
  - an existing URL-query-driven `/search` page that is the correct interaction reference for `/discover`
- Database/schema changes are not required for this slice.
- The weekly discover window is defined as:
  - content published in the last 7 rolling days
  - sorted by current cumulative heat
  - not by the last-7-day delta of likes, favorites, or downloads
- Discover ranking must remain non-personalized:
  - guests, users, verified users, and admins all see the same public ranking
  - admins do not get hidden/admin-only discover content in this public flow
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

This plan covers only the Phase F discover-ranking first slice:

- public `GET /api/discover`
- discover ranking enums and DTO contract
- cross-domain discover aggregation across:
  - community posts
  - jobs
  - resources
- discover filters:
  - `tab=ALL|POST|JOB|RESOURCE`
  - `period=WEEK|ALL`
- shared discover item structure for backend and frontend
- homepage `discoverPreview` extension inside `/api/home/summary`
- navbar `发现` entry
- dedicated `/discover` route and page
- homepage lightweight discover preview block
- empty/error/loading states for discover UI

This plan explicitly does not implement:

- personalized ranking
- day ranking
- Redis ranking cache or snapshot tables
- admin/manual curated recommendation slots
- detail-page related recommendations
- analytics dashboards
- recommendation explanation system beyond a simple `hotLabel`

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing page layout, navigation placement, discover cards, and homepage visual hierarchy.
- `@ui-ux-pro-max`
  Use before closing each UI task to review mobile behavior, loading/empty/error states, CTA clarity, and tab/toggle usability.

Use the current visual system as the base:

- theme: `editorial student decision desk`
- preserve the current warm-paper, deep-navy, and muted-accent visual language
- keep navbar and homepage patterns recognizable instead of introducing a second design system
- discover tabs and period toggles must work on mobile without hidden sidebar-only controls

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/DiscoverContentType.java`
- Create: `backend/src/main/java/com/campus/common/DiscoverPeriodType.java`
- Create: `backend/src/main/java/com/campus/dto/DiscoverItemView.java`
- Create: `backend/src/main/java/com/campus/dto/DiscoverResponse.java`
- Create: `backend/src/main/java/com/campus/controller/DiscoverController.java`
- Create: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Create: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`
- Create: `backend/src/test/java/com/campus/service/HomeServiceTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/components/NavBar.spec.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`

### Frontend: Create

- Create: `frontend/src/api/discover.js`
- Create: `frontend/src/components/DiscoverItemCard.vue`
- Create: `frontend/src/views/DiscoverView.vue`
- Create: `frontend/src/views/DiscoverView.spec.js`

### Repo Docs

- Modify: `README.md`

### Responsibility Notes

- `DiscoverController` owns request parsing and the public HTTP contract only.
- `DiscoverService` owns discover defaults, validation helpers, time-window filtering, heat scoring, sorting, and homepage preview generation.
- `CommunityService`, `JobService`, and `ResourceService` each stay responsible for deciding what counts as published/visible in their own domain and for exposing read-only discover candidates without leaking unpublished content.
- `DiscoverItemView` is the stable cross-layer contract shared by `/api/discover` and `HomeSummaryResponse.discoverPreview`.
- `HomeService` must consume `DiscoverService` for preview data instead of duplicating ranking logic.
- `DiscoverView.vue` owns URL-state synchronization and page states.
- `DiscoverItemCard.vue` keeps the discover item presentation reusable between homepage preview and the full `/discover` page.
- Do not reuse `SearchResultCard.vue` directly; discover cards need heat labels and ranking context, while search cards are keyword-result cards.

## Task 1: Add the Public Discover API Contract and a Minimal Resource-Only Slice

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Create: `backend/src/main/java/com/campus/common/DiscoverContentType.java`
- Create: `backend/src/main/java/com/campus/common/DiscoverPeriodType.java`
- Create: `backend/src/main/java/com/campus/dto/DiscoverItemView.java`
- Create: `backend/src/main/java/com/campus/dto/DiscoverResponse.java`
- Create: `backend/src/main/java/com/campus/controller/DiscoverController.java`
- Create: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Create: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`

- [ ] **Step 1: Write the failing discover contract tests**

```java
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
            .andExpect(jsonPath("$.data.items[0].type").value("RESOURCE"));
}

@Test
void defaultsAndValidationErrorsMatchTheContract() throws Exception {
    mockMvc.perform(get("/api/discover"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tab").value("ALL"))
            .andExpect(jsonPath("$.data.period").value("WEEK"));

    mockMvc.perform(get("/api/discover").param("tab", "ARTICLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("invalid discover tab"));

    mockMvc.perform(get("/api/discover").param("period", "MONTH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("invalid discover period"));
}
```

- [ ] **Step 2: Run the failing backend discover tests**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests test
```

Expected: FAIL because the discover enums, DTOs, controller, service, and security rule do not exist yet.

- [ ] **Step 3: Implement the minimal public discover contract**

Create these enums:

```java
public enum DiscoverContentType {
    ALL,
    POST,
    JOB,
    RESOURCE
}
```

```java
public enum DiscoverPeriodType {
    WEEK,
    ALL
}
```

Use this shared item shape:

```java
public record DiscoverItemView(
        Long id,
        String type,
        String title,
        String summary,
        String primaryMeta,
        String secondaryMeta,
        String path,
        LocalDateTime publishedAt,
        double hotScore,
        String hotLabel) {
}
```

Use this response shape:

```java
public record DiscoverResponse(
        String tab,
        String period,
        int total,
        List<DiscoverItemView> items) {
}
```

Implement these minimum backend rules:

- `GET /api/discover` must be `permitAll`
- default `tab=ALL`
- default `period=WEEK`
- default `limit=20`
- reject unsupported `tab`, `period`, and out-of-range `limit` with `BusinessException`
- `DiscoverController` must match the existing controller style:

```java
@GetMapping
public Result<DiscoverResponse> discover(
        @RequestParam(required = false) String tab,
        @RequestParam(required = false) String period,
        @RequestParam(required = false) Integer limit) {
    return Result.success(discoverService.discover(tab, period, limit));
}
```

- `DiscoverService` should first support a narrow resource-only branch so the public contract is real before cross-domain aggregation lands
- `ResourceService` should expose a read-only discover-candidate method for published resources only
- the initial resource heat formula should already match spec:

```java
double rawHeat = resource.getDownloadCount() * 2.0 + resource.getFavoriteCount() * 4.0;
double hotScore = rawHeat + Math.max(0, 14 - ageDays);
```

- [ ] **Step 4: Run the discover tests again and make them pass**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests test
```

Expected: PASS with guest access, default values, and validation behavior working.

- [ ] **Step 5: Commit the first discover backend slice**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/common/DiscoverContentType.java backend/src/main/java/com/campus/common/DiscoverPeriodType.java backend/src/main/java/com/campus/dto/DiscoverItemView.java backend/src/main/java/com/campus/dto/DiscoverResponse.java backend/src/main/java/com/campus/controller/DiscoverController.java backend/src/main/java/com/campus/service/DiscoverService.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/controller/DiscoverControllerTests.java
git commit -m "feat: add discover api contract"
```

## Task 2: Expand Discover Aggregation to Community and Jobs with Real Heat Scoring

**Files:**
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/DiscoverService.java`
- Modify: `backend/src/test/java/com/campus/controller/DiscoverControllerTests.java`

- [ ] **Step 1: Write the failing aggregation and ranking tests**

```java
@Test
void allTabAggregatesPostJobAndResourceItems() throws Exception {
    insertDiscoverFixtures();

    mockMvc.perform(get("/api/discover")
                    .param("tab", "ALL")
                    .param("period", "ALL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(3))
            .andExpect(jsonPath("$.data.items[0].type").exists())
            .andExpect(jsonPath("$.data.items[1].type").exists())
            .andExpect(jsonPath("$.data.items[2].type").exists());
}

@Test
void weekPeriodOnlyIncludesItemsPublishedInTheLastSevenDays() throws Exception {
    insertDiscoverFixtures();

    mockMvc.perform(get("/api/discover")
                    .param("tab", "ALL")
                    .param("period", "WEEK"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2));
}

@Test
void tabFilterReturnsOnlyMatchingTypeButKeepsSharedItemShape() throws Exception {
    insertDiscoverFixtures();

    mockMvc.perform(get("/api/discover")
                    .param("tab", "JOB")
                    .param("period", "ALL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].type").value("JOB"))
            .andExpect(jsonPath("$.data.items[0].hotLabel").value("持续关注"));
}
```

- [ ] **Step 2: Run the expanded discover tests to verify they fail**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests test
```

Expected: FAIL because only the minimal resource-only discover slice exists.

- [ ] **Step 3: Implement the cross-domain discover aggregation**

Expose read-only candidate methods from existing domain services and keep each service responsible for published-content visibility:

```java
public List<CommunityPost> listPublishedDiscoverPosts() { ... }
public List<JobPosting> listPublishedDiscoverJobs() { ... }
public List<ResourceItem> listPublishedDiscoverResources() { ... }
```

Implement heat scoring rules exactly as specified:

```java
double communityRawHeat = likeCount * 3.0 + commentCount * 4.0 + favoriteCount * 5.0 + verifiedAuthorBonus;
double jobRawHeat = favoriteCount * 5.0;
double resourceRawHeat = downloadCount * 2.0 + favoriteCount * 4.0;
double hotScore = rawHeat + Math.max(0, 14 - ageDays);
```

Implement these guardrails:

- `WEEK` means published in the last 7 rolling days only
- `ALL` means all published history
- `ALL` tab merges all branches and sorts by:

```java
Comparator.comparingDouble(DiscoverItemView::hotScore).reversed()
          .thenComparing(DiscoverItemView::publishedAt, Comparator.reverseOrder())
          .thenComparing(DiscoverItemView::id, Comparator.reverseOrder());
```

- community verified-author bonus must be small and explicit:

```java
double verifiedAuthorBonus = isVerifiedAuthor ? 2.0 : 0.0;
```

- `hotLabel` mapping must be deterministic by type plus period:
  - `POST + WEEK -> 本周热议`
  - `POST + ALL -> 长期热议`
  - `JOB + WEEK -> 本周关注`
  - `JOB + ALL -> 持续关注`
  - `RESOURCE + WEEK -> 本周高频下载`
  - `RESOURCE + ALL -> 长期热门资料`

Implementation note: batch favorite-count lookup for jobs inside `JobService` instead of calling `selectCount` once per job.

- [ ] **Step 4: Run the discover controller tests again**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests test
```

Expected: PASS with all-tab aggregation, period filtering, type filtering, and label mapping working.

- [ ] **Step 5: Commit the complete ranking backend**

```bash
git add backend/src/main/java/com/campus/service/CommunityService.java backend/src/main/java/com/campus/service/JobService.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/service/DiscoverService.java backend/src/test/java/com/campus/controller/DiscoverControllerTests.java
git commit -m "feat: add discover ranking aggregation"
```

## Task 3: Extend Home Summary with Discover Preview and a Safe Degradation Path

**Files:**
- Modify: `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
- Create: `backend/src/test/java/com/campus/service/HomeServiceTests.java`

- [ ] **Step 1: Write the failing home-summary preview tests**

Add one integration test to the existing controller suite:

```java
@Test
void guestSummaryIncludesDiscoverPreview() throws Exception {
    mockMvc.perform(get("/api/home/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.discoverPreview.period").value("WEEK"))
            .andExpect(jsonPath("$.data.discoverPreview.items").isArray());
}
```

Add one focused service degradation test:

```java
@Test
void discoverPreviewFallsBackToEmptyListWhenDiscoverServiceFails() {
    when(discoverService.previewForHome(4)).thenThrow(new RuntimeException("boom"));

    HomeSummaryResponse response = homeService.getSummary(null);

    assertThat(response.discoverPreview().items()).isEmpty();
    assertThat(response.viewerType()).isEqualTo("GUEST");
}
```

- [ ] **Step 2: Run the home controller and home service tests to verify failure**

Run:

```powershell
cd backend
mvn -q -Dtest=HomeControllerTests,HomeServiceTests test
```

Expected: FAIL because `HomeSummaryResponse` and `HomeService` do not expose discover preview yet.

- [ ] **Step 3: Implement `discoverPreview` on the home summary contract**

Add shared preview records inside `HomeSummaryResponse` and reuse `DiscoverItemView`:

```java
public record HomeSummaryResponse(
        String viewerType,
        IdentitySnapshot identity,
        String roleLabel,
        String verificationStatus,
        int unreadNotificationCount,
        List<String> todos,
        List<HomeEntryCard> entries,
        List<NotificationSnippet> latestNotifications,
        DiscoverPreview discoverPreview) {

    public record DiscoverPreview(
            String period,
            List<DiscoverItemView> items) {
    }
}
```

Update `HomeService` to:

- inject `DiscoverService`
- call `previewForHome(4)` for both guest and authenticated summaries
- wrap preview loading in a narrow `try/catch`
- log and degrade to `new DiscoverPreview("WEEK", List.of())` on failure

Keep all existing home fields untouched and append the new preview field at the end of the contract.

- [ ] **Step 4: Run the home tests again and make them pass**

Run:

```powershell
cd backend
mvn -q -Dtest=HomeControllerTests,HomeServiceTests test
```

Expected: PASS with `discoverPreview` included and the degradation path covered.

- [ ] **Step 5: Commit the home-summary extension**

```bash
git add backend/src/main/java/com/campus/dto/HomeSummaryResponse.java backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/controller/HomeControllerTests.java backend/src/test/java/com/campus/service/HomeServiceTests.java
git commit -m "feat: add discover preview to home summary"
```

## Task 4: Add the Discover Route, API Client, Shared Card, and Navbar Entry

**Files:**
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/components/NavBar.spec.js`
- Create: `frontend/src/api/discover.js`
- Create: `frontend/src/components/DiscoverItemCard.vue`
- Create: `frontend/src/views/DiscoverView.vue`
- Create: `frontend/src/views/DiscoverView.spec.js`

- [ ] **Step 1: Write the failing frontend discover navigation and page tests**

Add navbar coverage:

```js
test("navbar exposes a discover entry for guests and authenticated users", () => {
  const wrapper = mountNavBar();
  expect(wrapper.html()).toContain('data-to="/discover"');
});
```

Create discover page coverage:

```js
test("discover view defaults to all plus week and hits the api", async () => {
  getDiscoverResults.mockResolvedValue({
    tab: "ALL",
    period: "WEEK",
    total: 1,
    items: [{ id: 1, type: "RESOURCE", title: "Hot resource", path: "/resources/1", hotLabel: "本周高频下载" }],
  });

  const { wrapper } = await mountAt("/discover");

  expect(getDiscoverResults).toHaveBeenCalledWith({ tab: "ALL", period: "WEEK", limit: 20 });
  expect(wrapper.text()).toContain("Hot resource");
});

test("tab and period toggles sync the url-backed discover state", async () => {
  // route-backed push assertions here
});
```

- [ ] **Step 2: Run the targeted frontend tests and confirm failure**

Run:

```powershell
cd frontend
npm run test -- src/components/NavBar.spec.js src/views/DiscoverView.spec.js
```

Expected: FAIL because the discover route, API client, card, page, and navbar entry do not exist yet.

- [ ] **Step 3: Implement the discover frontend slice**

Create the API layer:

```js
export async function getDiscoverResults(params) {
  const { data } = await http.get("/discover", { params });
  return data.data;
}
```

Add a public route:

```js
{
  path: "/discover",
  name: "discover",
  component: () => import("../views/DiscoverView.vue"),
}
```

Update navbar items:

```js
const items = [
  { to: "/", label: "首页" },
  { to: "/community", label: "社区" },
  { to: "/discover", label: "发现" },
  { to: "/search", label: "搜索" },
];
```

Implement `DiscoverView.vue` using `/search` as the interaction reference:

- read `tab` and `period` from `useRoute()`
- default missing values to `ALL` and `WEEK`
- sync UI toggles back through `router.push(...)`
- call `getDiscoverResults({ tab, period, limit: 20 })`
- render loading, empty, error, and populated states

Implement `DiscoverItemCard.vue` with:

- type badge
- title
- summary
- primary and secondary metadata
- published time
- `hotLabel`

- [ ] **Step 4: Run the discover navbar and page tests again**

Run:

```powershell
cd frontend
npm run test -- src/components/NavBar.spec.js src/views/DiscoverView.spec.js
```

Expected: PASS with the discover route, URL-backed toggles, navbar entry, and discover card rendering working.

- [ ] **Step 5: Commit the discover frontend slice**

```bash
git add frontend/src/router/index.js frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js frontend/src/api/discover.js frontend/src/components/DiscoverItemCard.vue frontend/src/views/DiscoverView.vue frontend/src/views/DiscoverView.spec.js
git commit -m "feat: add discover page and navigation"
```

## Task 5: Add the Homepage Discover Preview Block

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`

- [ ] **Step 1: Write the failing homepage discover preview tests**

Extend the mocked home summary shape:

```js
const guestSummary = {
  // existing fields...
  discoverPreview: {
    period: "WEEK",
    items: [
      {
        id: 11,
        type: "RESOURCE",
        title: "Resume Pack",
        summary: "A practical starter pack.",
        primaryMeta: "Career Desk",
        secondaryMeta: "RESUME_TEMPLATE",
        path: "/resources/11",
        publishedAt: "2026-04-16T08:00:00",
        hotScore: 12,
        hotLabel: "本周高频下载",
      },
    ],
  },
};
```

Add assertions:

```js
test("home renders discover preview items and a discover cta", async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain("Resume Pack");
  expect(wrapper.html()).toContain('data-to="/resources/11"');
  expect(wrapper.html()).toContain('data-to="{\"name\":\"discover\",\"query\":{\"tab\":\"ALL\",\"period\":\"WEEK\"}}"');
});

test("home preview shows a graceful empty state when preview items are empty", async () => {
  // assert fallback copy
});
```

- [ ] **Step 2: Run the targeted homepage tests and verify they fail**

Run:

```powershell
cd frontend
npm run test -- src/views/HomeView.spec.js
```

Expected: FAIL because `HomeView.vue` does not render discover preview data yet.

- [ ] **Step 3: Implement the homepage preview UI**

Keep `getHomeSummary()` unchanged at the transport layer and update `HomeView.vue` to consume the new response field.

Add computed preview state:

```js
const discoverPreview = computed(() => summary.value.discoverPreview || { period: "WEEK", items: [] });
```

Render a small homepage block that:

- shows a short section title plus explanation
- shows up to 4 preview items
- reuses `DiscoverItemCard.vue` or a lightweight wrapper around it
- links each item to its original path
- links the section CTA to:

```js
{ name: "discover", query: { tab: "ALL", period: "WEEK" } }
```

- shows a graceful empty-state message instead of hiding the entire section

- [ ] **Step 4: Run the homepage tests again**

Run:

```powershell
cd frontend
npm run test -- src/views/HomeView.spec.js
```

Expected: PASS with discover preview items, CTA, and empty-state rendering covered.

- [ ] **Step 5: Commit the homepage preview**

```bash
git add frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js
git commit -m "feat: add home discover preview"
```

## Task 6: Update Docs and Run Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for the new discover flow**

Document:

- public `/discover` page
- homepage discover preview
- supported discover filters:
  - `ALL / POST / JOB / RESOURCE`
  - `WEEK / ALL`
- current limitations:
  - non-personalized
  - no day ranking
  - no Redis/cache layer yet

- [ ] **Step 2: Run targeted backend verification**

Run:

```powershell
cd backend
mvn -q -Dtest=DiscoverControllerTests,HomeControllerTests,HomeServiceTests test
```

Expected: PASS

- [ ] **Step 3: Run targeted frontend verification**

Run:

```powershell
cd frontend
npm run test -- src/components/NavBar.spec.js src/views/DiscoverView.spec.js src/views/HomeView.spec.js
```

Expected: PASS

- [ ] **Step 4: Run full regression and build checks**

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

Expected: PASS across the full backend suite, full frontend suite, and production build.

- [ ] **Step 5: Manual smoke and final commit**

Manual smoke checklist:

```powershell
Invoke-WebRequest 'http://127.0.0.1:8080/api/discover?tab=ALL&period=WEEK'
Invoke-WebRequest 'http://127.0.0.1:8080/api/discover?tab=JOB&period=ALL'
Invoke-WebRequest 'http://127.0.0.1:8080/api/home/summary'
Invoke-WebRequest 'http://127.0.0.1:5173/discover?tab=ALL&period=WEEK'
Invoke-WebRequest 'http://127.0.0.1:5173/'
```

If Vite selects a different port because `5173` is occupied, use the actual printed port instead.

Final commit:

```bash
git add README.md
git commit -m "docs: update discover feature documentation"
```

## Execution Notes

- Follow TDD in order. Do not skip the failing-test step even if the implementation seems obvious.
- Keep discover ranking logic concentrated in `DiscoverService`; do not scatter heat-score math across controllers or Vue components.
- Do not introduce a new database table or scheduled job in this slice.
- Reuse the `DiscoverItemView` contract everywhere instead of duplicating response shapes.
- Keep homepage degradation behavior explicit and tested.
- Preserve existing public behavior for `/search`, `/community`, `/jobs`, and `/resources`.
