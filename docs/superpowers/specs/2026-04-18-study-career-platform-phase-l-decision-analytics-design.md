# Study-Career Platform Phase L Decision Analytics Design

> **Validation note:** This design was implemented and validated on 2026-04-18. Execution record: `docs/superpowers/plans/2026-04-18-study-career-platform-phase-l-decision-analytics-implementation.md`. Documented verification covered `AnalyticsServiceTests`, `AnalyticsControllerTests`, `HomeServiceTests`, `HomeControllerTests`, `AnalyticsView.spec.js`, and `HomeView.spec.js`.

## 1. Goal

Phase L turns the existing home-page `analytics` placeholder into a real first-slice analytics surface while preserving the current Spring Boot monolith, Vue SPA, home aggregation shell, and the Phase K decision-support flow that already ships:

- assessment
- timeline
- school comparison

This phase delivers one bounded analytics slice:

- guests and logged-in users can open a real `/analytics` page
- guests can view public trend and distribution analytics
- logged-in users can view the same public analytics plus a personal decision snapshot
- the home-page `analytics` entry becomes a live route instead of a `COMING_SOON` placeholder

The design intentionally avoids turning this phase into a full admin BI system, an event-tracking platform, or a complex personalized recommendation engine.

## 2. User-Validated Scope

The following decisions were chosen for this phase:

- this phase is `Phase L: Decision Analytics First Slice`
- `analytics` becomes a real route in this phase
- the route is public so it satisfies the "public board" requirement boundary
- the page uses a mixed model:
  - public analytics for everyone
  - authenticated personal decision analytics for logged-in users
- the first slice reuses existing real tables and runtime data; it does not introduce analytics-only seed dashboards
- admin operations dashboards stay out of scope in this phase

## 3. Non-Goals

This phase does not implement:

- admin-only operations dashboards for registrations, moderation throughput, or reviewer workloads
- DAU, retention, funnel conversion, or other metrics that require new tracking instrumentation
- school-comparison history analytics based on persisted compare sessions
- exportable reports, CSV download, or scheduled report delivery
- external analytics vendors, event pipelines, or batch ETL jobs
- a large charting framework migration or a heavy dashboard-builder UI
- behavioral personalization, recommendation scoring changes, or profile-driven ranking

## 4. Chosen Approach

### 4.1 Recommendation

Implement analytics as one public page at:

- `/analytics`

backed by one backend-owned summary endpoint:

- `GET /api/analytics/summary?period=7D|30D`

The backend owns public aggregation, decision-distribution math, authenticated personal snapshot shaping, and period validation. The frontend owns route rendering, public versus personal presentation, empty states, and period switching.

### 4.2 Why This Approach

This approach fits the current codebase and product state best:

- it directly resolves the current home-page `analytics` placeholder
- it respects the formal requirement that guests can access public boards
- it extends the Phase K decision flow without forcing a second analytics-only navigation model
- it keeps the first slice lightweight by reusing existing tables rather than introducing tracking infrastructure
- it gives logged-in users a meaningful "why this path" surface instead of a public-only report page

### 4.3 Rejected Alternatives

#### Public Board Only

Rejected for this phase because it would:

- satisfy the public-board requirement but under-deliver on the current home `analytics` meaning
- give logged-in users little value beyond what guests already see
- break the natural continuation from assessment into analytics

#### Admin Operations Dashboard First

Rejected for this phase because it would:

- leave the current home `analytics` route unresolved
- optimize for admin workflows instead of the current user-facing navigation
- create a bigger scope jump than the existing product shell needs right now

#### Split Public And Personal Analytics Into Two Pages

Rejected for this phase because it would:

- create extra routing and discovery complexity
- fragment a small first slice across multiple pages
- force users to understand a distinction the product does not need yet

## 5. Functional Scope

### 5.1 Analytics Route And Access

This phase introduces a real analytics page at:

- `GET /analytics`

Access rules:

- guests can open the route
- logged-in users can open the same route
- guests see the public analytics layer only
- logged-in users see public analytics plus a personal decision layer

### 5.2 Public Overview And Trends

The public analytics layer should provide:

- high-level public overview cards
- recent trend data for `7D` and `30D`
- direction distribution across `CAREER`, `EXAM`, and `ABROAD`

The first slice should only expose metrics that can be derived cleanly from current tables:

- published community post count
- public job count under the current public-list visibility rules
- published resource count
- total assessment session count

Trend data should cover the same domains:

- posts
- jobs
- resources
- assessments

### 5.3 Decision Distribution

The page should expose a public direction-mix view across:

- `CAREER`
- `EXAM`
- `ABROAD`

To avoid retake-heavy skew, the direction distribution in this phase should use the latest saved assessment session per user as the public "current mix" input, while the overview card may still expose the raw total session count.

This gives the page two clean signals:

- platform assessment activity volume
- current path recommendation mix

### 5.4 Personal Snapshot And History

Logged-in users should see a personal decision layer that includes:

- latest recommendation snapshot
- latest score split across the three tracks
- short summary text
- recent assessment history for up to `5` sessions
- explicit next-step links into:
  - `/assessment`
  - `/timeline`
  - `/schools/compare`

If the user has no assessment history:

- the route still loads successfully
- the personal area shows a guided empty state
- the page links the user to `/assessment`

### 5.5 Home Entry Changes

The existing home summary entry cards should change as follows:

- `assessment` stays as delivered in Phase K
- `analytics` becomes a real live route in this phase
- `analytics` should no longer render with `COMING_SOON`
- guests and logged-in users should both be able to navigate into `/analytics`

The home-page copy should also make the entry explicit:

- analytics is now a live trend and path-analysis surface
- assessment remains the action surface for generating or refreshing personal path data

## 6. Architecture

### 6.1 Existing Components Reused

- `HomeService`
- existing home summary DTOs and home entry rendering
- existing decision assessment session data
- frontend router, auth store, and route shell
- existing backend result wrapper and project-standard error structure
- current local profile and seeded development model

### 6.2 New Or Changed Backend Units

- `AnalyticsController`
  - serves the viewer-aware analytics summary endpoint
- `AnalyticsService`
  - owns public aggregation, period validation, distribution shaping, and personal snapshot assembly
- analytics summary DTOs
- focused analytics read queries against the existing domain tables

This phase does not require analytics-specific persistence tables.

### 6.3 New Or Changed Frontend Units

- router entry for:
  - `/analytics`
- `AnalyticsView.vue`
- `analytics` API adapter
- home-entry and home-copy updates

### 6.4 Responsibility Boundaries

- `AnalyticsService` owns read-time analytics aggregation only
- `HomeService` owns entry availability only; it must not absorb analytics math
- decision services remain the source of decision-domain contracts; analytics only reads and reshapes their stored outputs
- the frontend page owns the separation between public and personal regions, not the backend services
- guest requests must never fail with `401`; the route is public by design

## 7. Data Design

### 7.1 Reused Source Tables

This phase reuses existing persisted data from:

- `t_community_post`
- `t_job_posting`
- `t_resource_item`
- `t_decision_assessment_session`

No new analytics tables are required in this first slice.

### 7.2 Public Overview Metrics

Recommended public overview fields:

- `publishedPostCount`
- `activeJobCount`
- `publishedResourceCount`
- `assessmentSessionCount`

Semantics in this phase:

- posts count only public published posts
- jobs count only the public job rows exposed by the current product rules
- resources count only published resources
- assessment session count uses raw saved session volume

### 7.3 Public Trend Points

Recommended trend point fields:

- `date`
- `posts`
- `jobs`
- `resources`
- `assessments`

Time source rules in this phase:

- posts trend uses published public post creation dates
- jobs trend uses public job publish dates when available
- resources trend uses resource publish dates
- assessments trend uses assessment session dates

The backend should return one deterministic item per date bucket inside the requested period, including zeros where needed.

### 7.4 Decision Distribution

Recommended decision-distribution fields:

- `participantCount`
- `tracks`
  - `track`
  - `count`
  - `percent`

Calculation rule:

- use the latest assessment session per user as the distribution source
- include only the three supported track codes
- return zero-count entries explicitly so frontend rendering stays deterministic

### 7.5 Personal Snapshot And History

Recommended personal snapshot fields:

- `hasAssessment`
- `recommendedTrack`
- `summaryText`
- `sessionDate`
- `scores`

Recommended personal history item fields:

- `sessionDate`
- `recommendedTrack`
- `careerScore`
- `examScore`
- `abroadScore`

This phase stores no separate analytics history rows. Personal analytics is derived at read time from the existing assessment sessions.

## 8. API Design

### 8.1 Analytics Summary

```http
GET /api/analytics/summary?period=7D|30D
```

Behavior:

- public read-only endpoint
- accepts optional authenticated context
- defaults `period` to `30D`
- validates `period`
- returns public analytics for all callers
- returns personal analytics only when an authenticated user is present

### 8.2 Response Shape

The response should include:

- `publicOverview`
- `publicTrends`
- `decisionDistribution`
- `personalSnapshot`
- `personalHistory`
- `nextActions`

Recommended structure:

```json
{
  "publicOverview": {
    "publishedPostCount": 12,
    "activeJobCount": 8,
    "publishedResourceCount": 15,
    "assessmentSessionCount": 9
  },
  "publicTrends": [
    {
      "date": "2026-04-18",
      "posts": 1,
      "jobs": 0,
      "resources": 2,
      "assessments": 1
    }
  ],
  "decisionDistribution": {
    "participantCount": 4,
    "tracks": [
      { "track": "CAREER", "count": 1, "percent": 25.0 },
      { "track": "EXAM", "count": 2, "percent": 50.0 },
      { "track": "ABROAD", "count": 1, "percent": 25.0 }
    ]
  },
  "personalSnapshot": null,
  "personalHistory": [],
  "nextActions": []
}
```

Authenticated-user contract rules:

- when the user has history, `personalSnapshot` is populated and `personalHistory` contains up to `5` items
- when the user has no history, `personalSnapshot.hasAssessment=false` should be returned instead of a hard error
- `nextActions` should always be explicit, not implied by frontend-only rules

Guest contract rules:

- `personalSnapshot` is `null`
- `personalHistory` is an empty list
- `nextActions` is an empty list

### 8.3 Next-Action Contract

Each `nextActions` item should include:

- `code`
- `label`
- `path`
- `description`

Expected actions in this phase:

- `START_ASSESSMENT`
- `OPEN_TIMELINE`
- `COMPARE_SCHOOLS`

`COMPARE_SCHOOLS` should remain conditional:

- it is a strong fit for `EXAM` and `ABROAD`
- it should not be pushed as the primary next step for `CAREER`

### 8.4 Validation And Errors

Validation failures in this phase:

- invalid `period`

These should return the existing project-standard error structure and clear messages.

The endpoint should not turn missing personal assessment history into an error response.

## 9. Frontend Flow Design

### 9.1 Analytics Page

Route:

- `/analytics`

Page regions:

- overview cards
- period switcher
- public trends
- decision distribution
- personal analytics desk

The page should feel like an editorial decision desk, not a heavy operations dashboard.

### 9.2 Period Switching

The page supports:

- `7D`
- `30D`

Flow:

1. load summary with the default period
2. render overview, trends, and decision mix
3. when the user switches period, refetch the summary from the backend
4. do not fake period filtering on the frontend

### 9.3 Guest Experience

Guests should see:

- the public analytics layer
- a clear note that personal path analytics unlocks after login and assessment
- a direct route to login when appropriate

Guests should not see:

- a blank or broken personal panel
- an auth error modal for the route itself

### 9.4 Authenticated Experience

Logged-in users should see:

- the public analytics layer
- a personal decision snapshot panel
- recent assessment history when available
- next-step actions into the existing decision flow

If no assessment history exists:

- show a guided personal empty state
- explain that assessment unlocks personal analytics
- link directly to `/assessment`

## 10. Empty States And Error Rules

### 10.1 No Trend Data

If the selected period has no matching public rows:

- return an explicit zero-filled trend series
- render a clear empty-trend explanation on the frontend
- do not hide the trends region entirely

### 10.2 No Personal Assessment History

If an authenticated user has never submitted an assessment:

- return `hasAssessment=false`
- show a guided empty state
- link to `/assessment`

### 10.3 Partial Analytics Failure

If personal analytics cannot be loaded cleanly but public analytics can:

- public analytics should still render
- the personal region should show an explicit error state
- the endpoint or page should not collapse the whole route when only the personal slice fails

### 10.4 Invalid Period

If `period` is not one of the supported values:

- return the project-standard error structure
- do not silently coerce unsupported values

## 11. Testing Strategy

### 11.1 Backend Tests

Analytics tests should verify:

- guests can access `/api/analytics/summary`
- invalid `period` is rejected with a business error
- public overview and trend sections return deterministic structure
- decision distribution returns all three tracks explicitly
- authenticated users with assessment history receive personal snapshot and history
- authenticated users without assessment history receive a clean empty personal state

### 11.2 Frontend Tests

`AnalyticsView` tests should verify:

- the route is accessible without auth
- guests see the public analytics sections plus a personal-login prompt
- logged-in users with assessment history see a personal snapshot
- logged-in users without assessment history see the assessment CTA empty state
- period switching triggers a refetch and re-renders the selected data
- overview, distribution, and personal sections handle empty states explicitly

`HomeView` tests should verify:

- the analytics entry is live
- the analytics entry no longer renders `COMING_SOON`
- the analytics card routes to `/analytics`

### 11.3 Manual Smoke

A local smoke pass should verify:

1. open `/analytics` as a guest
2. confirm the public overview, trends, and decision mix render
3. log in as seeded user `13800000001`
4. open `/analytics` again
5. if no assessment exists, confirm the personal panel sends the user to `/assessment`
6. if an assessment exists, confirm the latest snapshot and history render
7. confirm the home-page `analytics` entry is live and no longer reads as coming soon

## 12. Acceptance Criteria

Phase L can be considered complete when all of the following are true:

- `/analytics` is a real route
- guests can open `/analytics` and see public analytics
- logged-in users can see public analytics plus a personal decision layer
- the analytics summary is backed by real current domain data rather than fabricated placeholder payloads
- authenticated users without assessment history receive a guided personal empty state instead of an error
- the home `analytics` entry is real and no longer `COMING_SOON`
- backend and frontend verification for this slice pass
- one local smoke path passes

## 13. Implementation Handoff

The implementation plan for this phase should be written next at:

- `docs/superpowers/plans/2026-04-18-study-career-platform-phase-l-decision-analytics-implementation.md`

Recommended implementation order:

1. analytics backend summary contract
2. home-entry activation for analytics
3. frontend analytics route and view
4. regression, smoke, and docs
