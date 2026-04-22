# Study-Career Platform Phase K Decision Support Design

> **Validation note:** This design was implemented and validated on 2026-04-18. Execution record: `docs/superpowers/plans/2026-04-18-study-career-platform-phase-k-decision-support-implementation.md`. Documented verification covered `DecisionAssessmentServiceTests`, `DecisionAssessmentControllerTests`, `DecisionTimelineServiceTests`, `DecisionTimelineControllerTests`, `DecisionSchoolServiceTests`, `DecisionSchoolControllerTests`, `HomeServiceTests`, `HomeControllerTests`, `AssessmentView.spec.js`, `TimelineView.spec.js`, `SchoolCompareView.spec.js`, and `HomeView.spec.js`.

## 1. Goal

Phase K adds the first real decision-support user flow to the study-career platform while preserving the current Spring Boot monolith, Vue SPA, existing home aggregation surface, and the already delivered community, jobs, resources, search, discover, preview, MinIO, and migration slices.

This phase delivers a focused product slice:

- logged-in users can complete a short path assessment and receive a ranked recommendation across `CAREER`, `EXAM`, and `ABROAD`
- logged-in users can open a direction timeline that defaults to their latest recommended track
- guests and logged-in users can compare `2-4` schools for `EXAM` or `ABROAD`
- the home-page `assessment` entry becomes a real route instead of a `COMING_SOON` placeholder

The design intentionally avoids turning this phase into a recommendation-engine project, an admin content-management system, or a full analytics dashboard.

## 2. User-Validated Scope

The following decisions were explicitly chosen for this phase:

- this phase is `Phase K: Decision Support`
- the phase uses a progressive user flow instead of a single all-in-one workbench page
- the flow covers all three formal decision-support requirements together:
  - path assessment
  - direction timeline
  - school comparison
- data comes from backend-owned seed or built-in data in this phase
- this phase delivers user-facing capability only; it does not add admin maintenance screens
- `assessment` becomes a live home entry in this phase
- `analytics` remains a placeholder and stays out of scope

## 3. Non-Goals

This phase does not implement:

- admin CRUD pages for assessment questions, timeline templates, or school data
- external school-data synchronization or crawler-based ingestion
- a complex recommendation engine, ML scoring, or behavioral personalization
- historical trend analysis across multiple assessment sessions
- dashboard-style analytics pages for users or admins
- new community, jobs, or resource ranking logic based on assessment outcomes
- a unified single-page "decision desk" that merges all flows into one heavy screen
- direct frontend-owned mock data as the primary runtime source of truth

## 4. Chosen Approach

### 4.1 Recommendation

Implement decision support as three bounded user-facing flows backed by backend-owned read and submit APIs:

- `/assessment`
- `/timeline`
- `/schools/compare`

The backend owns the question set, option scoring, timeline templates, school seed data, and result calculation. The frontend owns route flow, rendering, empty states, and navigation between the three decision surfaces.

### 4.2 Why This Approach

This approach fits the current codebase best:

- it matches the existing route-based SPA structure
- it directly covers `FR-DECISION-001` and `FR-DECISION-002`, and delivers the Phase-K school-comparison slice of `FR-DECISION-003`
- it keeps backend and frontend responsibilities clear
- it lets the timeline depend on the latest saved assessment result without coupling the assessment API to page state
- it keeps later admin-maintenance work additive rather than forcing a rewrite

### 4.3 Rejected Alternatives

#### Single Decision Workbench Page

Rejected for this phase because it would:

- create a heavier state model than the current frontend structure needs
- blur the boundaries between assessment, timeline, and comparison
- make testing and later admin data maintenance harder

#### Frontend-Static First

Rejected for this phase because it would:

- diverge from the current backend-owned domain pattern
- create avoidable migration work when seeded data later needs admin maintenance
- weaken contract testing and data consistency

#### Assessment + Timeline Now, School Compare Later

Rejected for this phase because it would:

- leave the formal decision-support requirement set incomplete
- make the phase feel artificially split even though school comparison is a clean read-only slice
- reduce the value of the assessment result by removing a natural follow-up action

## 5. Functional Scope

### 5.1 Assessment Flow

This phase introduces a logged-in assessment flow at:

- `GET /assessment`

The flow is intentionally short:

- `6-8` questions
- single-choice answers only in this phase
- immediate submission result
- deterministic score accumulation across `CAREER`, `EXAM`, and `ABROAD`

The result must return:

- three direction scores
- ranked order
- a single `recommendedTrack`
- a short summary
- explicit next-step links to timeline and school comparison

If the user already has a previous result, the page may show the latest saved result before the user starts a new submission.

### 5.2 Timeline Flow

This phase introduces a logged-in direction timeline at:

- `GET /timeline`

Timeline behavior rules:

- the page supports manual switching between `CAREER`, `EXAM`, and `ABROAD`
- when the user has a latest assessment result, the frontend should default to that `recommendedTrack`
- when the user has no saved assessment result, the page should show a guided empty state and send the user to `/assessment`

Each milestone must provide:

- phase label
- milestone title
- short summary
- target date
- remaining days
- recommended actions
- optional resource or follow-up hints

This phase still avoids institution-specific or user-editable schedule planning. Instead, the timeline remains template-based and direction-based, while the backend derives a lightweight `targetDate` from a stable planning anchor plus the configured offsets, then derives `remainingDays` relative to the current date.

### 5.3 School Comparison Flow

This phase introduces a school comparison page at:

- `GET /schools/compare`

Access rules:

- guests can browse and compare schools
- logged-in users can also reach this page from assessment results and timelines

Comparison rules:

- the comparison domain in this phase is limited to `EXAM` and `ABROAD`
- this is an explicit Phase K interpretation of `FR-DECISION-003`; `CAREER` does not use school-entity comparison in this phase
- the user must choose at least `2` schools
- the user must choose at most `4` schools
- the comparison result should include:
  - a comparison table
  - lightweight chart-ready data
  - a short highlight summary

This phase does not force the `CAREER` track into the school-comparison model.

### 5.4 Home Entry Changes

The existing home summary entry cards should change as follows:

- guest users still see `assessment` as `LOGIN_REQUIRED`
- logged-in users see `assessment` as enabled and no longer `COMING_SOON`
- `analytics` remains enabled only as a future placeholder with `COMING_SOON`

The home page copy should also become more explicit that:

- `assessment` is a real decision-support entry
- `analytics` is not yet delivered

## 6. Architecture

### 6.1 Existing Components Reused

- `HomeService`
- home summary DTOs and home page entry-card rendering
- existing frontend router and auth-guard structure
- existing backend result wrapper and error format
- current local-profile and seed-data development model

### 6.2 New Or Changed Backend Units

- `DecisionAssessmentController`
  - serves assessment questions, latest result, and submission
- `DecisionTimelineController`
  - serves timeline milestones by track
- `DecisionSchoolController`
  - serves school list and school comparison results
- `DecisionAssessmentService`
  - question loading, answer validation, score calculation, and latest-session persistence
- `DecisionTimelineService`
  - track validation and timeline loading
- `DecisionSchoolService`
  - school search, compare validation, and comparison shaping
- assessment DTOs
- timeline DTOs
- school search and compare DTOs
- seed-data additions and schema additions for decision-support tables

### 6.3 New Or Changed Frontend Units

- router entries for:
  - `/assessment`
  - `/timeline`
  - `/schools/compare`
- `AssessmentView.vue`
- `TimelineView.vue`
- `SchoolCompareView.vue`
- decision-support API adapters
- home-entry and home-copy updates

### 6.4 Responsibility Boundaries

- assessment service owns question retrieval, answer validation, scoring, and session persistence
- timeline service owns direction-based milestone retrieval only
- school service owns candidate search and compare shaping only
- home service owns entry visibility, not decision domain logic
- frontend views own page flow and empty-state presentation, not score calculation

## 7. Data Design

### 7.1 Assessment Questions

Recommended table:

- `decision_assessment_question`

Key fields:

- `id`
- `code`
- `prompt`
- `description`
- `dimension_code`
- `display_order`
- `active`

This table stores `6-8` active questions in this phase.

### 7.2 Assessment Options

Recommended table:

- `decision_assessment_option`

Key fields:

- `id`
- `question_id`
- `code`
- `label`
- `description`
- `career_score`
- `exam_score`
- `abroad_score`
- `display_order`

Each option contributes deterministic weights to the three supported tracks.

### 7.3 Assessment Sessions

Recommended table:

- `decision_assessment_session`

Key fields:

- `id`
- `user_id`
- `answers_json`
- `career_score`
- `exam_score`
- `abroad_score`
- `recommended_track`
- `summary_text`
- `created_at`

This phase only requires storing enough history to fetch the latest result cleanly. No trend aggregation is required.

### 7.4 Timeline Milestones

Recommended table:

- `decision_timeline_milestone`

Key fields:

- `id`
- `track`
- `phase_code`
- `phase_label`
- `title`
- `summary`
- `offset_months`
- `offset_days`
- `action_checklist`
- `resource_hint`
- `display_order`

The milestones are direction templates rather than user-authored schedules. The runtime timeline response derives `targetDate` from `offset_months`, optional `offset_days`, and a stable `anchorDate`, then derives `remainingDays` relative to the current date.

### 7.5 School Profiles

Recommended table:

- `decision_school_profile`

Key fields:

- `id`
- `track`
- `name`
- `region`
- `country`
- `type`
- `tier_label`
- `summary`
- `active`

This phase supports `EXAM` and `ABROAD` profile rows only.

### 7.6 School Metric Definitions

Recommended table:

- `decision_school_metric_definition`

Key fields:

- `id`
- `metric_code`
- `metric_label`
- `metric_unit`
- `value_type`
- `chartable`
- `metric_order`

Allowed `value_type` values in this phase:

- `NUMBER`
- `PERCENT`
- `TEXT`

`chartable=true` is only valid for quantitative metrics, which means `NUMBER` or `PERCENT` in this phase.

This table defines the ordered metric catalog used consistently across table and chart rendering.

### 7.7 School Metrics

Recommended table:

- `decision_school_metric`

Key fields:

- `id`
- `school_id`
- `metric_code`
- `metric_value`

The frontend uses metric definitions plus per-school metric values to render the comparison table and lightweight charts deterministically.

### 7.8 Seed-Data Update Rules

Because this phase intentionally excludes admin maintenance tools, decision-support data updates must ship through backend-owned migrations or seed patches.

Operational rules for this phase:

- question, timeline, school, and metric-definition changes are versioned in code
- local and test environments rely on deterministic seed data
- production-like data corrections are applied through reviewed database patches rather than ad hoc manual edits
- rollback follows normal schema or data patch rollback procedures, not runtime admin actions

## 8. API Design

### 8.1 Assessment Questions

```http
GET /api/decision/assessment/questions
```

Behavior:

- requires authentication
- returns active questions in deterministic order
- returns each question with ordered options
- does not return prior user answers

### 8.2 Assessment Submission

```http
POST /api/decision/assessment/submissions
```

Request shape:

- `answers`
  - ordered or unordered answer items shaped as:
    - `questionId`
    - `optionId`

Behavior:

- requires authentication
- validates that all required active questions are answered exactly once
- validates that each selected option belongs to the specified question
- calculates `CAREER`, `EXAM`, and `ABROAD` scores
- persists a session snapshot
- returns the computed result

Response data should include:

- `recommendedTrack`
- `scores`
- `ranking`
- `summaryText`
- `nextActions`

### 8.3 Latest Assessment Result

```http
GET /api/decision/assessment/latest
```

Behavior:

- requires authentication
- returns the latest saved session for the current user
- returns a clean empty result when no session exists

This endpoint exists so the timeline page and assessment page can load default user context without duplicating submit logic.

### 8.4 Timeline By Track

```http
GET /api/decision/timeline?track=CAREER|EXAM|ABROAD&anchorDate=YYYY-MM-DD
```

Behavior:

- requires authentication
- validates `track`
- accepts optional `anchorDate`
- returns milestones in deterministic display order
- derives `targetDate` from the supplied `anchorDate` when present
- otherwise derives `targetDate` from the latest assessment session date for the current user when available
- derives `remainingDays` relative to the current date
- returns an explicit assessment-required empty result when neither `anchorDate` nor a latest assessment session exists
- returns an empty list instead of `500` when the track has no configured milestones

The backend should not implicitly derive a track from the latest assessment result. That remains frontend flow logic. The anchor-date fallback is allowed because it stabilizes milestone math without choosing the active track.

### 8.5 School Candidates

```http
GET /api/decision/schools?track=EXAM|ABROAD&keyword=...
```

Behavior:

- public read-only endpoint
- validates `track`
- supports optional keyword filtering
- returns active school candidates in deterministic order

### 8.6 School Comparison

```http
POST /api/decision/schools/compare
```

Request shape:

- `schoolIds`

Validation rules:

- public read-only endpoint
- at least `2` IDs
- at most `4` IDs
- no duplicates
- all IDs must exist and belong to the same supported comparison domain

Response data should include:

- `schools`
- `metricDefinitions`
- `tableRows`
- `chartSeries`
- `highlightSummary`

Contract rules:

- `schools` is ordered by the accepted `schoolIds` request order and each item includes:
  - `schoolId`
  - `name`
  - `track`
  - `region`
  - `tierLabel`
- `metricDefinitions` is the ordered descriptor list for all rendered metrics and each item includes:
  - `metricCode`
  - `metricLabel`
  - `metricUnit`
  - `valueType`
  - `chartable`
  - `metricOrder`
- `tableRows` is ordered by `metricOrder`; each row includes:
  - `metricCode`
  - `metricLabel`
  - `metricUnit`
  - `valueType`
  - `cells`
    - `schoolId`
    - `displayValue`
    - `rawValue`
    - `isMissing`
- `chartSeries` is ordered by `metricOrder` and only includes `chartable=true` metrics; each series includes:
  - `metricCode`
  - `metricLabel`
  - `metricUnit`
  - `valueType`
  - `points`
    - `schoolId`
    - `schoolName`
    - `numericValue`
    - `displayValue`
    - `isMissing`
- missing values must be represented explicitly so table and chart rendering stay deterministic

## 9. Frontend Flow Design

### 9.1 Assessment Page

Route:

- `/assessment`

Page regions:

- question form
- result summary
- next-step actions

Flow:

1. load questions
2. prevent submit until all required answers are present
3. submit answers
4. render scores, recommendation, and summary
5. expose:
   - `Go to Timeline`
   - `Compare Schools`

When a latest result exists, the page may also show a "latest recommendation" block before a new submission starts.

### 9.2 Timeline Page

Route:

- `/timeline`

Page regions:

- track tabs
- milestone list
- supporting links

Flow:

1. resolve a default track from the latest assessment result when available
2. otherwise show a guided empty state
3. only call the timeline API after a latest assessment result or explicit `anchorDate` exists
4. when a latest assessment result exists, pass its session date as the first `anchorDate`
5. load milestones for the active track
6. allow manual switching among all three tracks

Each milestone card should render:

- phase label
- target date
- remaining days
- action checklist
- follow-up hint

Supporting links may route into:

- community discussions
- jobs
- resources

These are guidance links only and do not change the timeline domain model.

### 9.3 School Compare Page

Route:

- `/schools/compare`

Page regions:

- track switcher
- school candidate search
- selected school chips
- comparison table
- lightweight comparison chart area
- highlight summary card

Flow:

1. choose `EXAM` or `ABROAD`
2. search and select `2-4` schools
3. submit compare request
4. render structured comparison output in both table and chart form

## 10. Empty States And Error Rules

### 10.1 Assessment Empty State

If no active questions exist:

- show a clear "assessment temporarily unavailable" state
- do not render a fake result
- do not allow submission

### 10.2 Timeline Without Assessment Context

If the user opens `/timeline` with no saved assessment result:

- show a guided empty state
- explain that completing one assessment unlocks the default recommendation
- provide a direct link to `/assessment`

### 10.3 School Compare Selection Rules

- fewer than `2` selected schools should be blocked on the frontend
- more than `4` selected schools should be blocked on the frontend
- the backend must still enforce both rules

### 10.4 Missing Seed Data

If seed data is missing for timelines or school comparison:

- return explicit empty results rather than server errors where possible
- show empty states on the frontend
- do not fabricate placeholder comparison data at runtime
- if chartable school metrics are missing, the page must still render an explicit empty-chart state instead of silently omitting the chart area

### 10.5 Validation Failures

Examples:

- invalid track
- incomplete assessment answers
- duplicate answer items
- duplicate school IDs
- unsupported school count

These should return the existing project-standard error structure and clear messages.

## 11. Testing Strategy

### 11.1 Backend Tests

Assessment tests should verify:

- deterministic question ordering
- valid scoring
- latest-session retrieval
- invalid-answer rejection
- auth protection

Timeline tests should verify:

- valid track loading
- invalid track rejection
- empty milestone handling
- auth protection

School tests should verify:

- track-filtered candidate search
- keyword filtering
- valid `2-4` school comparison
- invalid selection-size rejection
- invalid ID rejection

### 11.2 Frontend Tests

`AssessmentView` tests should verify:

- question rendering
- submit disabled until complete
- result rendering after submit
- next-step CTA rendering

`TimelineView` tests should verify:

- recommended-track defaulting when latest result exists
- guided empty state when no latest result exists
- correct milestone switching across tracks

`SchoolCompareView` tests should verify:

- track switching
- candidate selection limits
- comparison rendering
- API error state rendering

### 11.3 Manual Smoke

A local smoke pass should verify:

1. login
2. open `/assessment`
3. submit one assessment
4. follow the result link into `/timeline`
5. switch tracks manually
6. open `/schools/compare`
7. compare `2-4` schools successfully

## 12. Acceptance Criteria

Phase K can be considered complete when all of the following are true:

- a logged-in user can complete one assessment and immediately receive a ranked track result
- the latest result can drive the default timeline track
- the timeline page supports manual switching among `CAREER`, `EXAM`, and `ABROAD`
- guests and logged-in users can compare `2-4` schools for supported domains
- the home `assessment` entry is real and no longer `COMING_SOON` for logged-in users
- the home `analytics` entry remains clearly marked as not yet delivered
- backend and frontend verification for this slice pass
- one end-to-end local smoke path passes

## 13. Implementation Handoff

The implementation plan for this phase should be written next at:

- `docs/superpowers/plans/2026-04-18-study-career-platform-phase-k-decision-support-implementation.md`

Recommended implementation order:

1. schema and seed data
2. assessment backend contracts
3. timeline backend contracts
4. school search and compare backend contracts
5. frontend routes and views
6. home-entry integration
7. regression, smoke, and docs
