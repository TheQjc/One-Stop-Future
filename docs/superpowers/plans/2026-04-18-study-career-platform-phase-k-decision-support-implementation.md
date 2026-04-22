# Study-Career Platform Phase K Decision Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first real decision-support user flow with backend-owned assessment data, direction timelines, school comparison, and a live home-page `assessment` entry.

**Architecture:** Build Phase K contract-first. Add decision-support schema and seed data, then implement assessment question read + scoring/session persistence, then timeline projection with stable anchor semantics, then public school comparison, followed by Vue routes/views and home-entry activation. Keep `analytics` as a placeholder and keep admin maintenance out of scope in this phase.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2, JUnit 5, Mockito, MockMvc, Vue 3, Vue Router, Axios, Vitest

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-k-decision-support-design.md`
- Existing home aggregation backend:
  - `backend/src/main/java/com/campus/controller/HomeController.java`
  - `backend/src/main/java/com/campus/service/HomeService.java`
  - `backend/src/main/java/com/campus/dto/HomeSummaryResponse.java`
- Existing frontend home entry and route shell:
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/HomeView.spec.js`
  - `frontend/src/router/index.js`
  - `frontend/src/api/home.js`
- Existing backend SQL baseline:
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/resources/data.sql`
- Existing test style references:
  - `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
  - `backend/src/test/java/com/campus/controller/SearchControllerTests.java`
  - `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  - `frontend/src/views/SearchView.spec.js`
  - `frontend/src/views/DiscoverView.spec.js`
  - `frontend/src/views/HomeView.spec.js`

## Scope Lock

This plan covers only the approved Phase K slice:

- authenticated assessment question read
- authenticated assessment submission and latest-result retrieval
- authenticated timeline retrieval with stable anchor semantics and assessment-required empty state
- public school candidate listing and school comparison for `EXAM` and `ABROAD`
- frontend routes and views for `/assessment`, `/timeline`, and `/schools/compare`
- home entry activation for `assessment`

This plan explicitly does not implement:

- admin CRUD for decision-support data
- `analytics` page delivery
- school comparison for the `CAREER` track
- recommendation ML, personalization, or trend analytics
- external school-data sync

## Frontend Design Baseline

All UI tasks in this plan must explicitly use these skills before shipping:

- `@frontend-design`
  - keep the existing "editorial decision desk" direction rather than adding a brand-new visual language
  - decision pages should feel like guided planning surfaces, not generic dashboards
- `@ui-ux-pro-max`
  - review route clarity, form feedback, result readability, chart legibility, and mobile layout before closing UI tasks

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentQuestion.java`
  Question entity for `t_decision_assessment_question`.
- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentOption.java`
  Option entity for deterministic track scores.
- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentSession.java`
  Latest-result persistence entity.
- Create: `backend/src/main/java/com/campus/entity/DecisionTimelineMilestone.java`
  Direction timeline milestone entity.
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolProfile.java`
  School candidate entity.
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolMetricDefinition.java`
  Ordered metric catalog entity.
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolMetric.java`
  Per-school metric value entity.
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentQuestionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentOptionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentSessionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionTimelineMilestoneMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolProfileMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolMetricDefinitionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolMetricMapper.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentQuestionResponse.java`
  Assessment question contract with nested question/option items.
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentSubmissionRequest.java`
  Validated submit payload using `questionId + optionId`.
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentResultResponse.java`
  Submit/latest result contract.
- Create: `backend/src/main/java/com/campus/dto/DecisionTimelineResponse.java`
  Timeline contract including `assessmentRequired`, `anchorDate`, and projected milestones.
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolListResponse.java`
  Candidate list contract.
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolCompareRequest.java`
  Compare request DTO with `2-4` school IDs.
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolCompareResponse.java`
  Compare response with `schools`, `metricDefinitions`, `tableRows`, `chartSeries`, `highlightSummary`.
- Create: `backend/src/main/java/com/campus/service/DecisionAssessmentService.java`
- Create: `backend/src/main/java/com/campus/service/DecisionTimelineService.java`
- Create: `backend/src/main/java/com/campus/service/DecisionSchoolService.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionAssessmentController.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionTimelineController.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionSchoolController.java`
- Create: `backend/src/test/java/com/campus/service/DecisionAssessmentServiceTests.java`
- Create: `backend/src/test/java/com/campus/service/DecisionTimelineServiceTests.java`
- Create: `backend/src/test/java/com/campus/service/DecisionSchoolServiceTests.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionAssessmentControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionTimelineControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionSchoolControllerTests.java`

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
  Add Phase K decision-support tables.
- Modify: `backend/src/main/resources/data.sql`
  Add deterministic seed questions, options, milestones, schools, and metric definitions/values.
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
  Activate `assessment` for authenticated users and keep `analytics` as `COMING_SOON`.
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
  Cover home-entry activation behavior.
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`
  Cover updated home-summary entry behavior.

### Frontend: Create

- Create: `frontend/src/api/decision.js`
  Assessment, timeline, school list, and compare adapters.
- Create: `frontend/src/views/AssessmentView.vue`
  Question flow, submit, result summary, and next-step actions.
- Create: `frontend/src/views/TimelineView.vue`
  Track tabs, milestone cards, assessment-required empty state.
- Create: `frontend/src/views/SchoolCompareView.vue`
  Track switcher, candidate search, selected schools, comparison table, and chart area.
- Create: `frontend/src/views/AssessmentView.spec.js`
- Create: `frontend/src/views/TimelineView.spec.js`
- Create: `frontend/src/views/SchoolCompareView.spec.js`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
  Add Phase K routes and auth requirements.
- Modify: `frontend/src/views/HomeView.vue`
  Update decision-entry copy and live assessment navigation.
- Modify: `frontend/src/views/HomeView.spec.js`
  Update home summary expectations for live `assessment`.

### Docs: Modify Existing

- Modify: `README.md`
  Document the new decision-support endpoints, routes, and local verification path.

## Responsibility Notes

- `DecisionAssessmentService` is the single source of truth for answer validation, score accumulation, session persistence, and latest-result lookup.
- `DecisionTimelineService` owns anchor-date resolution and milestone projection. It must not silently choose an active track for the frontend.
- `DecisionSchoolService` owns candidate search and the compare payload shape. `DecisionSchoolMetricDefinition.metric_order` is the only metric ordering source.
- `DecisionSchoolController` remains public read-only in this phase.
- `HomeService` only decides whether the home entry is live; it must not absorb assessment domain logic.
- The frontend timeline page should not call the timeline API until it has either a latest-result anchor or an explicit anchor date.
- Missing chartable metrics still require an explicit empty-chart state; the page must not silently drop the chart region.

## Task 1: Add Assessment Schema And Question Read Contract

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentQuestion.java`
- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentOption.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentQuestionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentOptionMapper.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentQuestionResponse.java`
- Create: `backend/src/main/java/com/campus/service/DecisionAssessmentService.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionAssessmentController.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionAssessmentControllerTests.java`

- [x] **Step 1: Write the failing question-read controller tests**

Create `DecisionAssessmentControllerTests` with `@SpringBootTest`, `@AutoConfigureMockMvc`, and `@Sql("/schema.sql", "/data.sql")` coverage:

```java
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
            .andExpect(jsonPath("$.data.questions[0].options[0].code").value("Q1_A"));
}
```

- [x] **Step 2: Run the targeted controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionAssessmentControllerTests" test
```

Expected: FAIL because the tables, seed rows, DTO, service, and controller do not exist yet.

- [x] **Step 3: Add the Phase K assessment schema, seed data, and read-only endpoint**

Update `schema.sql` to add:

- `t_decision_assessment_question`
- `t_decision_assessment_option`

Update `data.sql` to add:

- `6-8` active seeded questions
- ordered options with deterministic `career_score`, `exam_score`, `abroad_score`

Implement the minimal backend read slice:

- `DecisionAssessmentQuestion` / `DecisionAssessmentOption`
- MyBatis mappers
- `DecisionAssessmentQuestionResponse`
- `DecisionAssessmentService.listQuestions()`
- `DecisionAssessmentController.questions()`

Use nested DTO items so the response shape is already frontend-ready:

```java
public record DecisionAssessmentQuestionResponse(List<QuestionItem> questions) {
    public record QuestionItem(Long id, String code, String prompt, String description, int displayOrder,
            List<OptionItem> options) {}
    public record OptionItem(Long id, String code, String label, String description, int displayOrder) {}
}
```

- [x] **Step 4: Re-run the targeted question tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionAssessmentControllerTests" test
```

Expected: PASS with the ordered authenticated question contract.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/entity/DecisionAssessmentQuestion.java backend/src/main/java/com/campus/entity/DecisionAssessmentOption.java backend/src/main/java/com/campus/mapper/DecisionAssessmentQuestionMapper.java backend/src/main/java/com/campus/mapper/DecisionAssessmentOptionMapper.java backend/src/main/java/com/campus/dto/DecisionAssessmentQuestionResponse.java backend/src/main/java/com/campus/service/DecisionAssessmentService.java backend/src/main/java/com/campus/controller/DecisionAssessmentController.java backend/src/test/java/com/campus/controller/DecisionAssessmentControllerTests.java
git commit -m "feat: add decision assessment question contract"
```

## Task 2: Add Assessment Submission And Latest Result Flow

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/java/com/campus/entity/DecisionAssessmentSession.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionAssessmentSessionMapper.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentSubmissionRequest.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionAssessmentResultResponse.java`
- Modify: `backend/src/main/java/com/campus/service/DecisionAssessmentService.java`
- Modify: `backend/src/main/java/com/campus/controller/DecisionAssessmentController.java`
- Create: `backend/src/test/java/com/campus/service/DecisionAssessmentServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/DecisionAssessmentControllerTests.java`

- [x] **Step 1: Write the failing scoring and latest-result tests**

Add service tests for deterministic scoring:

```java
@Test
void submitAnswersPersistsLatestSessionAndReturnsRecommendedTrack() {
    DecisionAssessmentResultResponse response = service.submit("2", new DecisionAssessmentSubmissionRequest(List.of(
            new AnswerItem(1L, 11L),
            new AnswerItem(2L, 22L)
    )));

    assertThat(response.recommendedTrack()).isEqualTo("EXAM");
    assertThat(response.ranking().get(0).track()).isEqualTo("EXAM");
}
```

Extend controller tests with:

```java
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
            .andExpect(jsonPath("$.data.recommendedTrack").isNotEmpty());

    mockMvc.perform(get("/api/decision/assessment/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.hasResult").value(true));
}
```

Also cover:

- unauthenticated submit -> `401`
- incomplete answer set -> body `code=400`
- duplicate question answers -> body `code=400`
- option-question mismatch -> body `code=400`
- latest without session -> `hasResult=false`

- [x] **Step 2: Run the targeted assessment tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionAssessmentServiceTests,DecisionAssessmentControllerTests" test
```

Expected: FAIL because session persistence and submit/latest contracts are not implemented.

- [x] **Step 3: Implement assessment session persistence and scoring**

Add `t_decision_assessment_session` to `schema.sql`.

Implement:

- `DecisionAssessmentSession`
- `DecisionAssessmentSessionMapper`
- `DecisionAssessmentSubmissionRequest`
- `DecisionAssessmentResultResponse`
- `DecisionAssessmentService.submit(...)`
- `DecisionAssessmentService.latestFor(...)`
- `POST /api/decision/assessment/submissions`
- `GET /api/decision/assessment/latest`

Lock the submit DTO shape to one form only:

```java
public record DecisionAssessmentSubmissionRequest(List<AnswerItem> answers) {
    public record AnswerItem(
            @NotNull Long questionId,
            @NotNull Long optionId) {
    }
}
```

The result DTO should include at minimum:

- `hasResult`
- `recommendedTrack`
- `summaryText`
- `scores`
- `ranking`
- `sessionDate`
- `nextActions`

Validation rules to enforce in this task:

- every active question is answered exactly once
- duplicate `questionId` values are rejected
- every `optionId` must belong to the specified `questionId`
- unknown question or option IDs are rejected

- [x] **Step 4: Re-run the targeted assessment tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionAssessmentServiceTests,DecisionAssessmentControllerTests" test
```

Expected: PASS with deterministic scoring, persisted latest result, and stable validation behavior.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/java/com/campus/entity/DecisionAssessmentSession.java backend/src/main/java/com/campus/mapper/DecisionAssessmentSessionMapper.java backend/src/main/java/com/campus/dto/DecisionAssessmentSubmissionRequest.java backend/src/main/java/com/campus/dto/DecisionAssessmentResultResponse.java backend/src/main/java/com/campus/service/DecisionAssessmentService.java backend/src/main/java/com/campus/controller/DecisionAssessmentController.java backend/src/test/java/com/campus/service/DecisionAssessmentServiceTests.java backend/src/test/java/com/campus/controller/DecisionAssessmentControllerTests.java
git commit -m "feat: add decision assessment scoring flow"
```

## Task 3: Add Timeline Projection With Stable Anchor Semantics

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Create: `backend/src/main/java/com/campus/entity/DecisionTimelineMilestone.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionTimelineMilestoneMapper.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionTimelineResponse.java`
- Create: `backend/src/main/java/com/campus/service/DecisionTimelineService.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionTimelineController.java`
- Create: `backend/src/test/java/com/campus/service/DecisionTimelineServiceTests.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionTimelineControllerTests.java`

- [x] **Step 1: Write the failing timeline tests**

Create service coverage for milestone projection:

```java
@Test
void timelineUsesAnchorDateToProjectTargetDateAndRemainingDays() {
    DecisionTimelineResponse response = service.timelineFor("2", "EXAM", LocalDate.parse("2026-05-01"));

    assertThat(response.assessmentRequired()).isFalse();
    assertThat(response.anchorDate()).isEqualTo(LocalDate.parse("2026-05-01"));
    assertThat(response.items().get(0).targetDate()).isNotNull();
    assertThat(response.items().get(0).remainingDays()).isNotNull();
}
```

Create controller coverage:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void timelineReturnsAssessmentRequiredWhenNoLatestResultOrAnchorExists() throws Exception {
    mockMvc.perform(get("/api/decision/timeline").param("track", "EXAM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.assessmentRequired").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(0));
}
```

Also cover:

- unauthenticated access -> `401`
- invalid `track` -> body `code=400`
- explicit `anchorDate` returns ordered milestones

- [x] **Step 2: Run the targeted timeline tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionTimelineServiceTests,DecisionTimelineControllerTests" test
```

Expected: FAIL because no milestone table, DTO, or projection service exists.

- [x] **Step 3: Implement the timeline milestone slice**

Add `t_decision_timeline_milestone` to `schema.sql` and seed ordered `CAREER`, `EXAM`, and `ABROAD` rows in `data.sql`.

Implement:

- `DecisionTimelineMilestone`
- `DecisionTimelineMilestoneMapper`
- `DecisionTimelineResponse`
- `DecisionTimelineService`
- `DecisionTimelineController`

Recommended response shape:

```java
public record DecisionTimelineResponse(
        String track,
        LocalDate anchorDate,
        boolean assessmentRequired,
        List<TimelineItem> items) {
    public record TimelineItem(
            String phaseCode,
            String phaseLabel,
            String title,
            String summary,
            LocalDate targetDate,
            long remainingDays,
            List<String> actionChecklist,
            String resourceHint) {
    }
}
```

Anchor rules:

- use explicit `anchorDate` when provided
- otherwise use latest assessment session date for the current user
- if neither exists, return `assessmentRequired=true` with empty items

- [x] **Step 4: Re-run the targeted timeline tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionTimelineServiceTests,DecisionTimelineControllerTests" test
```

Expected: PASS with stable anchor semantics and the assessment-required empty state.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/entity/DecisionTimelineMilestone.java backend/src/main/java/com/campus/mapper/DecisionTimelineMilestoneMapper.java backend/src/main/java/com/campus/dto/DecisionTimelineResponse.java backend/src/main/java/com/campus/service/DecisionTimelineService.java backend/src/main/java/com/campus/controller/DecisionTimelineController.java backend/src/test/java/com/campus/service/DecisionTimelineServiceTests.java backend/src/test/java/com/campus/controller/DecisionTimelineControllerTests.java
git commit -m "feat: add decision timeline endpoint"
```

## Task 4: Add Public School Search And Compare Backend

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/resources/data.sql`
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolProfile.java`
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolMetricDefinition.java`
- Create: `backend/src/main/java/com/campus/entity/DecisionSchoolMetric.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolProfileMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolMetricDefinitionMapper.java`
- Create: `backend/src/main/java/com/campus/mapper/DecisionSchoolMetricMapper.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolCompareRequest.java`
- Create: `backend/src/main/java/com/campus/dto/DecisionSchoolCompareResponse.java`
- Create: `backend/src/main/java/com/campus/service/DecisionSchoolService.java`
- Create: `backend/src/main/java/com/campus/controller/DecisionSchoolController.java`
- Create: `backend/src/test/java/com/campus/service/DecisionSchoolServiceTests.java`
- Create: `backend/src/test/java/com/campus/controller/DecisionSchoolControllerTests.java`

- [x] **Step 1: Write the failing school search and compare tests**

Controller coverage:

```java
@Test
void guestCanListExamSchoolCandidates() throws Exception {
    mockMvc.perform(get("/api/decision/schools").param("track", "EXAM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.schools.length()").value(3));
}

@Test
void guestCanCompareTwoSchools() throws Exception {
    mockMvc.perform(post("/api/decision/schools/compare")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"schoolIds":[1,2]}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.metricDefinitions.length()").isNotEmpty())
            .andExpect(jsonPath("$.data.tableRows.length()").isNotEmpty())
            .andExpect(jsonPath("$.data.chartSeries.length()").isNotEmpty())
            .andExpect(jsonPath("$.data.highlightSummary").isNotEmpty());
}
```

Service coverage should also verify:

- request-order preservation in `schools`
- metric ordering comes only from `metricDefinitions.metricOrder`
- `1` or `5` school IDs returns body `code=400`
- duplicate school IDs returns body `code=400`
- invalid mixed-domain compare returns body `code=400`
- `chartSeries` includes only `chartable=true` metrics

- [x] **Step 2: Run the targeted school tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionSchoolServiceTests,DecisionSchoolControllerTests" test
```

Expected: FAIL because the school tables, DTOs, service, and controller do not exist.

- [x] **Step 3: Implement the public school slice**

Add to `schema.sql`:

- `t_decision_school_profile`
- `t_decision_school_metric_definition`
- `t_decision_school_metric`

Seed to `data.sql`:

- deterministic `EXAM` and `ABROAD` schools
- ordered metric definitions
- per-school metric values

Implement:

- entities and mappers
- list and compare DTOs
- `DecisionSchoolService`
- `DecisionSchoolController`

The compare response must explicitly construct and return:

- `schools`
- `metricDefinitions`
- `tableRows`
- `chartSeries`
- `highlightSummary`

Lock the compare DTO contract:

```java
public record DecisionSchoolCompareRequest(
        @Size(min = 2, max = 4) List<Long> schoolIds) {
}
```

The response should make ordering explicit:

- `schools` ordered by accepted request order
- `metricDefinitions` ordered by `metricOrder`
- `tableRows` ordered by `metricOrder`
- `chartSeries` ordered by `metricOrder` and filtered to `chartable=true` metrics only
- each table/chart cell includes explicit missing-value markers

- [x] **Step 4: Re-run the targeted school tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionSchoolServiceTests,DecisionSchoolControllerTests" test
```

Expected: PASS with public list/compare contracts and deterministic output ordering.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/resources/data.sql backend/src/main/java/com/campus/entity/DecisionSchoolProfile.java backend/src/main/java/com/campus/entity/DecisionSchoolMetricDefinition.java backend/src/main/java/com/campus/entity/DecisionSchoolMetric.java backend/src/main/java/com/campus/mapper/DecisionSchoolProfileMapper.java backend/src/main/java/com/campus/mapper/DecisionSchoolMetricDefinitionMapper.java backend/src/main/java/com/campus/mapper/DecisionSchoolMetricMapper.java backend/src/main/java/com/campus/dto/DecisionSchoolListResponse.java backend/src/main/java/com/campus/dto/DecisionSchoolCompareRequest.java backend/src/main/java/com/campus/dto/DecisionSchoolCompareResponse.java backend/src/main/java/com/campus/service/DecisionSchoolService.java backend/src/main/java/com/campus/controller/DecisionSchoolController.java backend/src/test/java/com/campus/service/DecisionSchoolServiceTests.java backend/src/test/java/com/campus/controller/DecisionSchoolControllerTests.java
git commit -m "feat: add decision school compare backend"
```

## Task 5: Add Assessment And Timeline Frontend Slice

**Files:**
- Create: `frontend/src/api/decision.js`
- Create: `frontend/src/views/AssessmentView.vue`
- Create: `frontend/src/views/TimelineView.vue`
- Create: `frontend/src/views/AssessmentView.spec.js`
- Create: `frontend/src/views/TimelineView.spec.js`
- Modify: `frontend/src/router/index.js`

- [x] **Step 1: Write the failing assessment and timeline view tests**

`AssessmentView.spec.js` should cover:

```javascript
test("loads decision questions and blocks submit until all answers are selected", async () => {
  listDecisionQuestions.mockResolvedValue({ questions: [/* seeded questions */] });
  submitDecisionAnswers.mockResolvedValue({ hasResult: true, recommendedTrack: "EXAM" });

  const wrapper = mount(AssessmentView);
  await flushPromises();

  expect(wrapper.find('[data-test="assessment-submit"]').attributes("disabled")).toBeDefined();
});
```

`TimelineView.spec.js` should cover:

```javascript
test("shows assessment-required empty state when latest result is absent", async () => {
  getLatestDecisionResult.mockResolvedValue({ hasResult: false });

  const wrapper = mount(TimelineView);
  await flushPromises();

  expect(wrapper.text()).toContain("Complete the assessment first");
});
```

Also cover:

- latest result defaults the first active track to `recommendedTrack`
- clicking a different track tab reloads the timeline for that track
- timeline cards render `targetDate` and `remainingDays`

Also add route assertions:

- `/assessment` requires auth
- `/timeline` requires auth

- [x] **Step 2: Run the targeted frontend tests and verify failure**

Run:

```powershell
cd frontend
npm run test -- src/views/AssessmentView.spec.js src/views/TimelineView.spec.js
```

Expected: FAIL because the views, API adapter, and routes do not exist yet.

- [x] **Step 3: Implement the assessment and timeline views**

Create `frontend/src/api/decision.js` with:

- `listDecisionQuestions()`
- `submitDecisionAnswers(payload)`
- `getLatestDecisionResult()`
- `getDecisionTimeline(params)`

Add routes:

```javascript
{
  path: "/assessment",
  name: "assessment",
  component: () => import("../views/AssessmentView.vue"),
  meta: { requiresAuth: true },
},
{
  path: "/timeline",
  name: "timeline",
  component: () => import("../views/TimelineView.vue"),
  meta: { requiresAuth: true },
}
```

Implement the pages so they:

- load backend data rather than inline mock data
- keep the assessment submit button disabled until the answer set is complete
- use the latest result to choose the first timeline `track`
- allow manual switching among `CAREER`, `EXAM`, and `ABROAD`
- only call the timeline API when a latest result or explicit anchor exists
- show the assessment-required empty state when no anchor exists

- [x] **Step 4: Re-run the targeted frontend tests**

Run:

```powershell
cd frontend
npm run test -- src/views/AssessmentView.spec.js src/views/TimelineView.spec.js
```

Expected: PASS with live routes, question flow, result rendering, and assessment-gated timeline behavior.

- [x] **Step 5: Commit**

```bash
git add frontend/src/api/decision.js frontend/src/views/AssessmentView.vue frontend/src/views/TimelineView.vue frontend/src/views/AssessmentView.spec.js frontend/src/views/TimelineView.spec.js frontend/src/router/index.js
git commit -m "feat: add decision assessment and timeline views"
```

## Task 6: Add School Compare Page And Activate The Home Entry

**Files:**
- Create: `frontend/src/views/SchoolCompareView.vue`
- Create: `frontend/src/views/SchoolCompareView.spec.js`
- Modify: `frontend/src/api/decision.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/views/HomeView.spec.js`
- Modify: `backend/src/main/java/com/campus/service/HomeService.java`
- Modify: `backend/src/test/java/com/campus/service/HomeServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/HomeControllerTests.java`

- [x] **Step 1: Write the failing school-compare and home-entry tests**

`SchoolCompareView.spec.js` should cover:

```javascript
test("enforces 2-4 school selection and renders table + chart results", async () => {
  listDecisionSchools.mockResolvedValue({ schools: [/* seeded schools */] });
  compareDecisionSchools.mockResolvedValue({ metricDefinitions: [/* ... */], tableRows: [/* ... */], chartSeries: [/* ... */], highlightSummary: "School A is stronger on cost while School B leads on reputation." });

  const wrapper = mount(SchoolCompareView);
  await flushPromises();

  expect(wrapper.text()).toContain("Select at least 2 schools");
});
```

Also cover:

- successful compare renders the highlight summary card
- empty `chartSeries` still renders an explicit empty-chart state instead of hiding the region

Extend backend home tests to assert:

- guest summary still exposes `assessment` with `LOGIN_REQUIRED`
- authenticated user summary exposes `assessment` as enabled with no `COMING_SOON`
- `analytics` remains `COMING_SOON`

Extend `HomeView.spec.js` to assert the live assessment link is rendered for authenticated summaries.

- [x] **Step 2: Run the targeted backend and frontend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=HomeServiceTests,HomeControllerTests" test
```

```powershell
cd frontend
npm run test -- src/views/SchoolCompareView.spec.js src/views/HomeView.spec.js
```

Expected: FAIL because the home entry is still a placeholder and the school compare view does not exist.

- [x] **Step 3: Implement school compare UI and home-entry activation**

Add the public route:

```javascript
{
  path: "/schools/compare",
  name: "schools-compare",
  component: () => import("../views/SchoolCompareView.vue"),
}
```

Update `frontend/src/api/decision.js` with:

- `listDecisionSchools(params)`
- `compareDecisionSchools(payload)`

Implement `SchoolCompareView.vue` so it:

- supports `EXAM` / `ABROAD` switching
- limits selected schools to `2-4`
- renders comparison table and chart regions
- renders the backend-provided `highlightSummary` card
- shows an explicit empty-chart state when `chartSeries` is empty

Update `HomeService` so:

- guest `assessment` entry stays disabled with `LOGIN_REQUIRED`
- authenticated `assessment` entry is enabled and has no `COMING_SOON`
- `analytics` remains `COMING_SOON`

Update `HomeView.vue` copy so the decision-support entry reads as live while `analytics` stays clearly future-facing.

- [x] **Step 4: Re-run the targeted backend and frontend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=HomeServiceTests,HomeControllerTests" test
```

```powershell
cd frontend
npm run test -- src/views/SchoolCompareView.spec.js src/views/HomeView.spec.js
```

Expected: PASS with public school compare flow and live home entry activation.

- [x] **Step 5: Commit**

```bash
git add frontend/src/views/SchoolCompareView.vue frontend/src/views/SchoolCompareView.spec.js frontend/src/api/decision.js frontend/src/router/index.js frontend/src/views/HomeView.vue frontend/src/views/HomeView.spec.js backend/src/main/java/com/campus/service/HomeService.java backend/src/test/java/com/campus/service/HomeServiceTests.java backend/src/test/java/com/campus/controller/HomeControllerTests.java
git commit -m "feat: add school compare page and activate decision entry"
```

## Task 7: Update Docs And Run Full Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README for Phase K decision support**

Document:

- new backend endpoints
- new frontend routes
- local verification path:
  - login
  - submit assessment
  - open timeline
  - compare schools
- explicit note that `analytics` remains out of scope in this phase

- [x] **Step 2: Run the targeted backend verification set**

Run:

```powershell
cd backend
mvn -q "-Dtest=DecisionAssessmentServiceTests,DecisionAssessmentControllerTests,DecisionTimelineServiceTests,DecisionTimelineControllerTests,DecisionSchoolServiceTests,DecisionSchoolControllerTests,HomeServiceTests,HomeControllerTests" test
```

Expected: PASS.

- [x] **Step 3: Run the targeted frontend verification set**

Run:

```powershell
cd frontend
npm run test -- src/views/AssessmentView.spec.js src/views/TimelineView.spec.js src/views/SchoolCompareView.spec.js src/views/HomeView.spec.js
```

Expected: PASS.

- [x] **Step 4: Run full regression and build**

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

- [x] **Step 5: Manual smoke and commit**

Manual smoke:

1. start backend and frontend in local profile
2. log in as seeded user `13800000001`
3. open `/assessment` and complete one submission
4. confirm the result page links into `/timeline`
5. confirm `/timeline` defaults to the recommended track
6. confirm `/schools/compare` allows public compare with `2-4` schools
7. confirm the home-page assessment entry is live and `analytics` still reads as not yet delivered

Then commit:

```bash
git add README.md
git commit -m "docs: add phase k verification notes"
```
