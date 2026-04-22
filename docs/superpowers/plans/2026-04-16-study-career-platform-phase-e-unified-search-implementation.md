# Study-Career Platform Phase E Unified Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Completion status:** Completed on 2026-04-16.
> Delivered by commit `e23d998`.
> This slice became the baseline extended by Phase F and remains covered by later full backend/frontend regression runs through Phase H.

**Goal:** Build the Phase E first-slice unified search: a public backend aggregate search API plus homepage/nav entry points and a dedicated `/search` page that can search published community posts, jobs, and resources.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Add a narrow search boundary on the backend with `SearchController` and `SearchService`, and expose read-only published-search methods from the existing community, jobs, and resources services instead of querying those tables directly from the controller. On the frontend, add a dedicated `/search` route backed by URL query params so search state is shareable, refresh-safe, and ready for later Elasticsearch replacement without reworking page contracts.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 local/test profile, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-e-unified-search-design.md`
- Requirements baseline: `docs/superpowers/requirements/2026-04-15-study-career-platform-formal-requirements.md`
- Existing implementation baseline:
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-b-community-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-c-job-aggregation-implementation.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-d-resource-library-implementation.md`
- Current repo already has:
  - public published-content list/detail flows for community, jobs, and resources
  - a consistent `Result.success(...)` / `Result.error(...)` JSON response envelope
  - `BusinessException` plus `GlobalExceptionHandler` returning business failures inside HTTP `200` for most validation errors
  - a homepage and navbar that already act as cross-module entry points
- Database/schema changes are not required for this slice.
- Search test coverage should reuse current seeded data where possible and insert deterministic fixture rows inside search tests when cross-domain ordering must be proved.
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

This plan covers only the Phase E unified-search first slice:

- homepage top search box
- navbar search entry
- dedicated `/search` route
- public `GET /api/search`
- published-content search across:
  - community posts
  - jobs
  - resources
- type switch:
  - `ALL`
  - `POST`
  - `JOB`
  - `RESOURCE`
- sort switch:
  - `RELEVANCE`
  - `LATEST`
- shareable URL query state using `q`, `type`, and `sort`

This plan explicitly does not implement:

- Elasticsearch integration
- search suggestions, history, or hot terms
- advanced filters beyond type and sort
- highlight snippets
- private/admin-only hidden results in public search
- personalized ranking
- homepage inline result stream

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing layout, interaction flow, spacing, and visual hierarchy.
- `@ui-ux-pro-max`
  Use before closing each UI task to review mobile behavior, empty/loading/error states, and clarity of actions.

Use the current visual system as the base:

- theme: `editorial student decision desk`
- preserve the current warm-paper, deep-navy, and muted-accent visual language
- keep homepage and navbar patterns recognizable instead of introducing a second design system
- search controls must still work on mobile without sidebar-only interactions

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/SearchContentType.java`
- Create: `backend/src/main/java/com/campus/common/SearchSortType.java`
- Create: `backend/src/main/java/com/campus/dto/SearchResponse.java`
- Create: `backend/src/main/java/com/campus/controller/SearchController.java`
- Create: `backend/src/main/java/com/campus/service/SearchService.java`
- Create: `backend/src/test/java/com/campus/controller/SearchControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/components/NavBar.vue`

### Frontend: Create

- Create: `frontend/src/api/search.js`
- Create: `frontend/src/components/SearchResultCard.vue`
- Create: `frontend/src/components/NavBar.spec.js`
- Create: `frontend/src/views/SearchView.vue`
- Create: `frontend/src/views/SearchView.spec.js`

### Repo Docs

- Modify: `README.md`

### Responsibility Notes

- `SearchController` owns request parsing and the public HTTP contract only.
- `SearchService` owns normalization, aggregation, totals, and unified sorting.
- `CommunityService`, `JobService`, and `ResourceService` each stay responsible for deciding what counts as published/visible in their own domain.
- `SearchResponse` is the stable cross-layer contract for the unified results page.
- `SearchView.vue` owns URL-state synchronization and page states.
- `SearchResultCard.vue` keeps the unified result card layout small enough to test and reuse.

## Task 1: Add the Public Search Contract and a Working Resource Search Slice

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Create: `backend/src/main/java/com/campus/common/SearchContentType.java`
- Create: `backend/src/main/java/com/campus/common/SearchSortType.java`
- Create: `backend/src/main/java/com/campus/dto/SearchResponse.java`
- Create: `backend/src/main/java/com/campus/controller/SearchController.java`
- Create: `backend/src/main/java/com/campus/service/SearchService.java`
- Create: `backend/src/test/java/com/campus/controller/SearchControllerTests.java`

- [x] **Step 1: Write the failing search contract tests**

```java
@Test
void guestCanUsePublicSearchEndpointForResources() throws Exception {
    mockMvc.perform(get("/api/search")
                    .param("q", "resume")
                    .param("type", "RESOURCE")
                    .param("sort", "RELEVANCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.query").value("resume"))
            .andExpect(jsonPath("$.data.type").value("RESOURCE"))
            .andExpect(jsonPath("$.data.sort").value("RELEVANCE"))
            .andExpect(jsonPath("$.data.totals.resource").value(1))
            .andExpect(jsonPath("$.data.results[0].type").value("RESOURCE"))
            .andExpect(jsonPath("$.data.results[0].path").value("/resources/1"));
}

@Test
void blankQueryReturnsBusinessError() throws Exception {
    mockMvc.perform(get("/api/search").param("q", "   "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("search query is required"));
}
```

```java
@Test
void invalidTypeAndSortReturnBusinessErrors() throws Exception {
    mockMvc.perform(get("/api/search").param("q", "resume").param("type", "ARTICLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("invalid search type"));

    mockMvc.perform(get("/api/search").param("q", "resume").param("sort", "HOT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("invalid search sort"));
}
```

- [x] **Step 2: Run the failing search contract tests**

Run:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
```

Expected: FAIL because the search enums, DTO, controller, service, security rule, and resource-search branch do not exist yet.

- [x] **Step 3: Implement the minimal public search contract**

Create these enums:

```java
public enum SearchContentType {
    ALL,
    POST,
    JOB,
    RESOURCE
}
```

```java
public enum SearchSortType {
    RELEVANCE,
    LATEST
}
```

Use this response shape:

```java
public record SearchResponse(
        String query,
        String type,
        String sort,
        SearchTotals totals,
        List<SearchResultItem> results) {

    public record SearchTotals(int all, int post, int job, int resource) {
    }

    public record SearchResultItem(
            Long id,
            String type,
            String title,
            String summary,
            String metaPrimary,
            String metaSecondary,
            String path,
            LocalDateTime publishedAt) {
    }
}
```

Implement these minimum backend rules:

- `GET /api/search` must be `permitAll`
- `q` is required after trimming
- default `type=ALL`
- default `sort=RELEVANCE`
- `SearchController` should follow the existing controller style:

```java
@GetMapping
public Result<SearchResponse> search(
        @RequestParam String q,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String sort,
        Authentication authentication) {
    return Result.success(searchService.search(q, type, sort, identityOf(authentication)));
}
```

- `ResourceService` must expose:

```java
public List<SearchResponse.SearchResultItem> searchPublishedResources(String keyword)
```

- resource search must:
  - search `title`, `summary`, and `description`
  - include only `PUBLISHED`
  - map results to:
    - `type = "RESOURCE"`
    - `metaPrimary = uploader nickname`
    - `metaSecondary = resource category`
    - `path = "/resources/{id}"`
    - `publishedAt = publishedAt`
- `SearchService` may start with the resource branch only in this task, but it must already normalize query/type/sort and produce stable totals plus sorting helpers that Task 2 can extend.

- [x] **Step 4: Re-run the search contract tests**

Run:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/common/SearchContentType.java backend/src/main/java/com/campus/common/SearchSortType.java backend/src/main/java/com/campus/dto/SearchResponse.java backend/src/main/java/com/campus/controller/SearchController.java backend/src/main/java/com/campus/service/SearchService.java backend/src/test/java/com/campus/controller/SearchControllerTests.java
git commit -m "feat: add unified search api contract"
```

## Task 2: Expand Backend Aggregation to Community, Jobs, Totals, and Final Sorting Rules

**Files:**
- Modify: `backend/src/main/java/com/campus/service/CommunityService.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/SearchService.java`
- Modify: `backend/src/test/java/com/campus/controller/SearchControllerTests.java`

- [x] **Step 1: Write the failing aggregation and sort tests**

Use deterministic test-local rows instead of guessing against shared seed data:

```java
@Test
void allSearchAggregatesPublishedPostJobAndResourceHits() throws Exception {
    insertUnifiedSearchFixtures();

    mockMvc.perform(get("/api/search").param("q", "unified"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totals.all").value(3))
            .andExpect(jsonPath("$.data.totals.post").value(1))
            .andExpect(jsonPath("$.data.totals.job").value(1))
            .andExpect(jsonPath("$.data.totals.resource").value(1))
            .andExpect(jsonPath("$.data.results[0].type").value("RESOURCE"));
}

@Test
void latestSortOrdersCrossDomainResultsByPublishedTimeDescending() throws Exception {
    insertUnifiedSearchFixtures();

    mockMvc.perform(get("/api/search").param("q", "unified").param("sort", "LATEST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.results[0].path").value("/resources/101"))
            .andExpect(jsonPath("$.data.results[1].path").value("/jobs/101"))
            .andExpect(jsonPath("$.data.results[2].path").value("/community/101"));
}
```

```java
private void insertUnifiedSearchFixtures() {
    jdbcTemplate.update("""
            INSERT INTO t_community_post (id, author_id, tag, title, content, status, like_count, comment_count, favorite_count, created_at, updated_at)
            VALUES (101, 2, 'CAREER', 'Unified planning note', 'Cross-domain unified content body', 'PUBLISHED', 0, 0, 0, TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP))
            """);
    jdbcTemplate.update("""
            INSERT INTO t_job_posting (id, title, company_name, city, job_type, education_requirement, source_platform, source_url, summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at)
            VALUES (101, 'Unified campus job', 'Search Labs', 'Shenzhen', 'INTERNSHIP', 'BACHELOR', 'Official Site', 'https://jobs.example.com/unified-job', 'Unified search summary', 'Backend job body', TIMESTAMPADD(DAY, 7, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
    jdbcTemplate.update("""
            INSERT INTO t_resource_item (id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason, file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count, published_at, reviewed_at, created_at, updated_at)
            VALUES (101, 'Unified resume pack', 'RESUME_TEMPLATE', 'Unified search summary', 'Resource body', 'PUBLISHED', 3, 1, NULL, 'unified-pack.pdf', 'pdf', 'application/pdf', 1234, 'seed/unified-pack.pdf', 0, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """);
}
```

- [x] **Step 2: Run the failing aggregation tests**

Run:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
```

Expected: FAIL because community/job branches, totals, and cross-domain ordering are not implemented yet.

- [x] **Step 3: Implement the final backend aggregation behavior**

Expose these domain methods:

```java
public List<SearchResponse.SearchResultItem> searchPublishedPosts(String keyword)
public List<SearchResponse.SearchResultItem> searchPublishedJobs(String keyword)
public List<SearchResponse.SearchResultItem> searchPublishedResources(String keyword)
```

Domain mapping rules:

- `CommunityService.searchPublishedPosts(...)`
  - search `title` and `content`
  - only `PUBLISHED`
  - map:
    - `type = "POST"`
    - `metaPrimary = author nickname`
    - `metaSecondary = tag`
    - `path = "/community/{id}"`
    - `publishedAt = createdAt`
- `JobService.searchPublishedJobs(...)`
  - search `title`, `companyName`, `summary`, and `content`
  - only `PUBLISHED`
  - map:
    - `type = "JOB"`
    - `metaPrimary = company name`
    - `metaSecondary = city + " / " + jobType`
    - `path = "/jobs/{id}"`
    - `publishedAt = publishedAt`
- `ResourceService.searchPublishedResources(...)`
  - keep the Task 1 behavior and do not widen visibility beyond `PUBLISHED`

Finalize `SearchService` rules:

- always gather all three published lists first
- compute totals before applying the `type` filter to the returned list
- `ALL` returns all types
- `POST`, `JOB`, and `RESOURCE` return only that branch in `results`
- `RELEVANCE` should use a stable comparator:

```java
private Comparator<SearchResponse.SearchResultItem> relevanceComparator(String normalizedQuery) {
    return Comparator
            .comparingInt((SearchResponse.SearchResultItem item) -> relevanceBucket(item, normalizedQuery))
            .thenComparing(SearchResponse.SearchResultItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SearchResponse.SearchResultItem::id, Comparator.reverseOrder());
}
```

Scoring rule:

- bucket `0`: title contains query
- bucket `1`: summary, `metaPrimary`, or `metaSecondary` contains query
- bucket `2`: everything else that still matched at the DB level
- tie-break with newest first

`LATEST` rule:

- sort by `publishedAt DESC`, then `id DESC`

- [x] **Step 4: Re-run the aggregation tests**

Run:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/service/CommunityService.java backend/src/main/java/com/campus/service/JobService.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/service/SearchService.java backend/src/test/java/com/campus/controller/SearchControllerTests.java
git commit -m "feat: aggregate community jobs and resources in search"
```

## Task 3: Build the `/search` Page, API Client, and URL-Synced Search Flow

**Files:**
- Modify: `frontend/src/router/index.js`
- Create: `frontend/src/api/search.js`
- Create: `frontend/src/components/SearchResultCard.vue`
- Create: `frontend/src/views/SearchView.vue`
- Create: `frontend/src/views/SearchView.spec.js`

- [x] **Step 1: Write the failing search view tests**

```js
import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import SearchView from "./SearchView.vue";
import { getSearchResults } from "../api/search.js";

vi.mock("../api/search.js", () => ({
  getSearchResults: vi.fn(),
}));

async function mountAt(path) {
  setActivePinia(createPinia());
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/search", component: SearchView }],
  });
  await router.push(path);
  await router.isReady();
  const wrapper = mount(SearchView, {
    global: {
      plugins: [router],
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();
  return { wrapper, router };
}

test("route query drives api fetch and result rendering", async () => {
  getSearchResults.mockResolvedValue({
    query: "resume",
    type: "ALL",
    sort: "RELEVANCE",
    totals: { all: 1, post: 0, job: 0, resource: 1 },
    results: [{ id: 1, type: "RESOURCE", title: "2026 Resume Template Pack", path: "/resources/1" }],
  });

  const { wrapper } = await mountAt("/search?q=resume&type=ALL&sort=RELEVANCE");

  expect(getSearchResults).toHaveBeenCalledWith({ q: "resume", type: "ALL", sort: "RELEVANCE" });
  expect(wrapper.text()).toContain("2026 Resume Template Pack");
});
```

```js
test("blank query stays in guided empty state and does not hit the api", async () => {
  const { wrapper } = await mountAt("/search");

  expect(getSearchResults).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain("Start with a keyword");
});
```

- [x] **Step 2: Run the failing search view tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/SearchView.spec.js
```

Expected: FAIL because the route, API client, search view, and result card do not exist yet.

- [x] **Step 3: Build the search page flow**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Required route:

```js
{
  path: "/search",
  name: "search",
  component: () => import("../views/SearchView.vue"),
}
```

Required API client:

```js
export async function getSearchResults(params = {}) {
  const { data } = await http.get("/search", { params });
  return data.data;
}
```

Required page behavior:

- read `q`, `type`, and `sort` from `route.query`
- if `q` is blank after trim:
  - show guided empty state
  - do not call the API
- when `q`, `type`, or `sort` changes:
  - update the URL
  - refetch results
- render:
  - top search input
  - type switch chips
  - sort switch chips
  - totals summary
  - loading state
  - error state with retry
  - no-results state
  - result cards linking to `item.path`

`SearchResultCard.vue` minimum props contract:

```js
defineProps({
  item: {
    type: Object,
    required: true,
  },
});
```

Card content must show:

- type label
- title
- summary
- `metaPrimary`
- `metaSecondary`
- formatted `publishedAt`

- [x] **Step 4: Re-run the search view tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/SearchView.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/router/index.js frontend/src/api/search.js frontend/src/components/SearchResultCard.vue frontend/src/views/SearchView.vue frontend/src/views/SearchView.spec.js
git commit -m "feat: add unified search page"
```

## Task 4: Wire the Homepage Search Box and Navbar Search Entry

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `frontend/src/components/NavBar.vue`
- Create: `frontend/src/components/NavBar.spec.js`

- [x] **Step 1: Write the failing entry-point tests**

Add a focused homepage submit test:

```js
import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, expect, test, vi } from "vitest";
import HomeView from "./HomeView.vue";
import { getHomeSummary } from "../api/home.js";

const push = vi.fn();

vi.mock("vue-router", async () => {
  const actual = await vi.importActual("vue-router");
  return {
    ...actual,
    useRouter: () => ({ push }),
  };
});

test("home search submits into the unified search page", async () => {
  getHomeSummary.mockResolvedValue(guestSummary);
  setActivePinia(createPinia());

  const wrapper = mount(HomeView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });

  await flushPromises();
  await wrapper.find('input[name="home-search"]').setValue("resume");
  await wrapper.find('[data-test="home-search-form"]').trigger("submit.prevent");

  expect(push).toHaveBeenCalledWith({
    name: "search",
    query: { q: "resume", type: "ALL", sort: "RELEVANCE" },
  });
});
```

Add a navbar search-entry test:

```js
test("navbar exposes a search entry", () => {
  setActivePinia(createPinia());
  const wrapper = mount(NavBar, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });

  expect(wrapper.html()).toContain('data-to="/search"');
});
```

- [x] **Step 2: Run the failing entry-point tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/HomeView.spec.js src/components/NavBar.spec.js
```

Expected: FAIL because the homepage search form and navbar search entry do not exist yet.

- [x] **Step 3: Implement the two public entry points**

Before writing UI code:

- use `@frontend-design`
- then use `@ui-ux-pro-max`

Homepage rules:

- add a top search form in the hero section
- support Enter and click submit
- trim the keyword before navigation
- blank input should not navigate
- submit target:

```js
router.push({
  name: "search",
  query: { q: keyword.trim(), type: "ALL", sort: "RELEVANCE" },
});
```

Navbar rules:

- add a visible search link for guests and authenticated users
- keep current nav items and role-based admin items intact
- do not turn the navbar into a second full search form in this slice

- [x] **Step 4: Re-run the entry-point tests**

Run:

```powershell
cd frontend
npm run test -- --run src/views/HomeView.spec.js src/components/NavBar.spec.js
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js
git commit -m "feat: add unified search entry points"
```

## Task 5: Update Docs and Run the Full Verification Set

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README for Phase E unified search**

Document:

- current repo status now includes unified search first slice
- new public route:
  - `/search`
- new public backend endpoint:
  - `GET /api/search`
- supported query params:
  - `q`
  - `type`
  - `sort`
- supported search types:
  - `ALL`
  - `POST`
  - `JOB`
  - `RESOURCE`
- supported sort types:
  - `RELEVANCE`
  - `LATEST`
- targeted backend test command:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
```

- targeted frontend test command:

```powershell
cd frontend
npm run test -- --run src/views/SearchView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
```

- [x] **Step 2: Run the backend verification set**

Run:

```powershell
cd backend
mvn -q -Dtest=SearchControllerTests test
mvn -q test
```

Expected:

- targeted search controller tests PASS
- full backend suite PASS

- [x] **Step 3: Run the frontend verification set**

Run:

```powershell
cd frontend
npm run test -- --run src/views/SearchView.spec.js src/views/HomeView.spec.js src/components/NavBar.spec.js
npm run test -- --run
npm run build
```

Expected:

- targeted search-related frontend tests PASS
- full frontend suite PASS
- frontend build PASS

- [x] **Step 4: Run the local smoke pass**

Validate in this order:

1. Start backend locally with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
2. Start frontend locally with `npm run dev -- --host 127.0.0.1`.
3. As a guest, use the homepage top search box to search `resume`.
4. Confirm the app navigates to `/search?q=resume&type=ALL&sort=RELEVANCE`.
5. On `/search`, switch between `ALL`, `POST`, `JOB`, and `RESOURCE`.
6. Switch between `RELEVANCE` and `LATEST`.
7. Click at least one result from each populated content type and confirm it opens the original detail page.
8. Refresh the `/search` page and confirm the result state is preserved from the URL.
9. Use browser back/forward and confirm search state follows history.
10. Open the navbar search entry directly and confirm the guided empty state appears when there is no `q`.
11. Confirm guests never see hidden, draft, pending, rejected, or offline content in unified results.

- [x] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add unified search run and verification notes"
```

## Final Verification Set

After Task 5, run the full suite in this order:

```powershell
cd backend
mvn -q test
cd ../frontend
npm run test -- --run
npm run build
```

If any command fails, fix that failure before moving to the next subproject after Phase E.
