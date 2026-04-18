# Study-Career Platform Phase M Admin Dashboard Design

## 1. Goal

Phase M adds a unified admin overview dashboard that helps administrators understand current operational load and quickly jump into the existing specialized workbenches.

This phase delivers one bounded slice:

- a new admin-only route at `/admin/dashboard`
- one aggregated backend summary endpoint at `GET /api/admin/dashboard/summary`
- four overview sections for:
  - verification
  - community
  - jobs
  - resources
- overview counts, a recent-item list, and a jump link for each section
- admin navigation and home-entry activation for the new dashboard

The dashboard is intentionally read-only. It is an overview and routing surface, not a merged all-in-one moderation console.

## 2. User-Validated Scope

The following decisions were chosen for this phase:

- this phase is `Phase M: Admin Operations Dashboard`
- the dashboard is a new route:
  - `/admin/dashboard`
- the dashboard is admin-only
- the dashboard is overview-only:
  - counts
  - recent list
  - jump link
- the dashboard does not execute approve, reject, publish, offline, delete, or hide actions directly
- existing admin workbenches remain the operational destinations:
  - `/admin/verifications`
  - `/admin/community`
  - `/admin/jobs`
  - `/admin/resources`
- admin home gets a separate `admin-dashboard` entry
- the top navigation also gets an admin-only dashboard link
- the community section is an operations board over current post statuses; it does not introduce a new moderation queue

## 3. Non-Goals

This phase does not implement:

- direct moderation or publishing actions on the dashboard page
- a new workflow engine, moderation queue, or bulk-operations surface
- charts, trend lines, or historical BI reporting
- filtering, searching, pagination, or drill-down tables inside the dashboard
- role expansion beyond the current admin boundary
- changes to the existing admin workbench behavior
- public exposure of any admin metrics

## 4. Chosen Approach

### 4.1 Recommendation

Implement one backend-owned aggregated summary contract:

- `GET /api/admin/dashboard/summary`

and one frontend dashboard route:

- `/admin/dashboard`

The backend owns count aggregation, recent-item selection, and dashboard-focused DTO shaping. The frontend owns section rendering, admin-only access flow, empty states, retry behavior, and navigation to the existing workbenches.

### 4.2 Why This Approach

This approach fits the current project structure best:

- it preserves the current split admin workbenches instead of collapsing them into one oversized page
- it gives admins a single situational-awareness surface without duplicating domain logic across multiple list endpoints
- it avoids deriving dashboard counts from paged list APIs that were not designed for summary accuracy
- it keeps implementation risk low by staying read-only and navigation-oriented
- it creates a natural admin home destination without disturbing existing workflows

### 4.3 Rejected Alternatives

#### Combined Mega-Workbench

Rejected because it would:

- mix overview and action concerns into one crowded page
- duplicate existing workbench interactions
- increase regression risk across four already-working admin surfaces

#### Reusing Existing List Endpoints For Counts

Rejected because it would:

- make totals depend on page-oriented payloads
- push summary math into the frontend
- create fragile coupling between dashboard needs and workbench response shapes

#### Adding Direct Dashboard Actions

Rejected for this phase because it would:

- turn a lightweight overview into a second operations surface
- require per-section action states, confirmations, and partial mutation recovery
- make the first slice larger than necessary

## 5. Functional Scope

### 5.1 Route And Access

This phase introduces:

- `GET /admin/dashboard`
- `GET /api/admin/dashboard/summary`

Access rules:

- only admins can open the page
- only admins can access the summary endpoint
- non-admin users should be blocked by the existing admin route and backend security rules

### 5.2 Verification Section

The verification block should show:

- `pendingCount`
- `reviewedToday`
- `latestPendingApplications`
- jump link to `/admin/verifications`

Recent-list semantics:

- use up to `5` rows
- include only `PENDING` applications
- order by `createdAt desc`

### 5.3 Community Section

The community block should show:

- `totalCount`
- `publishedCount`
- `hiddenCount`
- `deletedCount`
- `latestPosts`
- jump link to `/admin/community`

Community semantics in this phase:

- this is a status-board summary over current posts
- there is no synthetic `pending` or moderation-review concept
- `latestPosts` should show up to `5` most recently created posts across current statuses relevant to admins
- order by `createdAt desc`

### 5.4 Jobs Section

The jobs block should show:

- `totalCount`
- `draftCount`
- `publishedCount`
- `offlineCount`
- `latestActionableJobs`
- jump link to `/admin/jobs`

Actionable-job semantics:

- actionable means status is `DRAFT` or `OFFLINE`
- use up to `5` rows
- order by `updatedAt desc`

Deleted jobs are out of the dashboard summary and should not inflate the operational counts.

### 5.5 Resources Section

The resources block should show:

- `totalCount`
- `pendingCount`
- `publishedCount`
- `closedCount`
- `latestPendingResources`
- jump link to `/admin/resources`

Resource semantics:

- `closedCount` means `REJECTED + OFFLINE`
- use up to `5` pending rows for `latestPendingResources`
- order by `createdAt desc`

### 5.6 Home And Navigation Changes

Admin navigation changes in this phase:

- admin home gets a new `admin-dashboard` live entry
- the entry routes to `/admin/dashboard`
- existing admin entries stay visible and unchanged
- `NavBar` adds an admin-only link to `/admin/dashboard`

## 6. Architecture

### 6.1 Existing Components Reused

- existing Spring Boot monolith and Vue SPA shell
- current admin authentication and role checks
- current admin workbench routes and views
- existing admin domain services and tables
- project-standard result wrapper and error structure

### 6.2 New Or Changed Backend Units

- `AdminDashboardController`
  - exposes the summary endpoint
- `AdminDashboardService`
  - aggregates dashboard counts and recent rows
- `AdminDashboardSummaryResponse`
  - backend-owned response contract for the whole page
- `AdminDashboardReadMapper`
  - focused read queries for dashboard counts and recent lists

Boundary rules:

- dashboard aggregation stays in the dedicated dashboard service
- existing admin list services do not absorb dashboard summary responsibilities
- the dashboard service reads current tables directly instead of chaining through list endpoints

### 6.3 New Or Changed Frontend Units

- router entry for `/admin/dashboard`
- `AdminDashboardView.vue`
- admin API adapter function for dashboard summary loading
- `HomeView` entry activation for `admin-dashboard`
- `NavBar` admin link update

### 6.4 Responsibility Boundaries

- the backend returns a page-ready summary contract
- the frontend does not recompute summary counts from recent-item lists
- the dashboard page owns read-only presentation only
- existing workbench pages remain the only surfaces for domain actions

## 7. Data Design

### 7.1 Source Tables And Statuses

This phase reuses the current persisted tables and status models:

- verification applications with `PENDING`, `APPROVED`, `REJECTED`
- community posts with `PUBLISHED`, `HIDDEN`, `DELETED`
- job postings with `DRAFT`, `PUBLISHED`, `OFFLINE`, `DELETED`
- resource items with `PENDING`, `PUBLISHED`, `REJECTED`, `OFFLINE`

No new tables are required.

### 7.2 Verification Contract

Recommended verification fields:

- `pendingCount`
- `reviewedToday`
- `latestPendingApplications`

Recommended application item fields:

- `id`
- `userId`
- `applicantNickname`
- `realName`
- `studentId`
- `status`
- `createdAt`

`reviewedToday` should count verification applications whose `reviewedAt` falls on the current server-local date.

### 7.3 Community Contract

Recommended community fields:

- `totalCount`
- `publishedCount`
- `hiddenCount`
- `deletedCount`
- `latestPosts`

Recommended post item fields:

- `id`
- `tag`
- `title`
- `status`
- `authorId`
- `authorNickname`
- `likeCount`
- `commentCount`
- `favoriteCount`
- `createdAt`

`totalCount` should represent the same admin-visible universe as the existing admin community manage page, excluding no status except rows already physically absent from the table.

### 7.4 Jobs Contract

Recommended jobs fields:

- `totalCount`
- `draftCount`
- `publishedCount`
- `offlineCount`
- `latestActionableJobs`

Recommended job item fields:

- `id`
- `title`
- `companyName`
- `city`
- `jobType`
- `educationRequirement`
- `sourcePlatform`
- `status`
- `summary`
- `deadlineAt`
- `publishedAt`
- `updatedAt`

Count semantics:

- `totalCount` should exclude `DELETED`
- `draftCount`, `publishedCount`, and `offlineCount` are status-specific counts within the same non-deleted universe

### 7.5 Resources Contract

Recommended resources fields:

- `totalCount`
- `pendingCount`
- `publishedCount`
- `closedCount`
- `latestPendingResources`

Recommended resource item fields:

- `id`
- `title`
- `category`
- `uploaderNickname`
- `fileName`
- `fileSize`
- `downloadCount`
- `status`
- `rejectReason`
- `createdAt`
- `reviewedAt`
- `publishedAt`
- `previewAvailable`
- `previewKind`

Count semantics:

- `totalCount` should include all admin-visible resources in current statuses
- `closedCount` should equal `REJECTED + OFFLINE`

## 8. API Design

### 8.1 Summary Endpoint

```http
GET /api/admin/dashboard/summary
```

Behavior:

- admin-only endpoint
- returns the full dashboard payload in one response
- does not accept page, search, or status filter parameters in this phase

### 8.2 Response Shape

Recommended structure:

```json
{
  "verification": {
    "pendingCount": 3,
    "reviewedToday": 2,
    "latestPendingApplications": [
      {
        "id": 1001,
        "userId": 7,
        "applicantNickname": "alice",
        "realName": "Alice Chen",
        "studentId": "20260001",
        "status": "PENDING",
        "createdAt": "2026-04-18T09:30:00"
      }
    ]
  },
  "community": {
    "totalCount": 18,
    "publishedCount": 12,
    "hiddenCount": 4,
    "deletedCount": 2,
    "latestPosts": [
      {
        "id": 2001,
        "tag": "EXAM",
        "title": "Study Plan Notes",
        "status": "PUBLISHED",
        "authorId": 8,
        "authorNickname": "study-user",
        "likeCount": 15,
        "commentCount": 3,
        "favoriteCount": 6,
        "createdAt": "2026-04-18T10:00:00"
      }
    ]
  },
  "jobs": {
    "totalCount": 11,
    "draftCount": 3,
    "publishedCount": 6,
    "offlineCount": 2,
    "latestActionableJobs": [
      {
        "id": 3001,
        "title": "Backend Intern",
        "companyName": "Campus Tech",
        "city": "Shanghai",
        "jobType": "INTERNSHIP",
        "educationRequirement": "BACHELOR",
        "sourcePlatform": "Official Site",
        "status": "DRAFT",
        "summary": "Java backend internship",
        "deadlineAt": "2026-04-30T18:00:00",
        "publishedAt": null,
        "updatedAt": "2026-04-18T08:00:00"
      }
    ]
  },
  "resources": {
    "totalCount": 14,
    "pendingCount": 4,
    "publishedCount": 7,
    "closedCount": 3,
    "latestPendingResources": [
      {
        "id": 4001,
        "title": "Algorithm Notes",
        "category": "NOTES",
        "uploaderNickname": "verified-user",
        "fileName": "algo.pdf",
        "fileSize": 123456,
        "downloadCount": 9,
        "status": "PENDING",
        "rejectReason": null,
        "createdAt": "2026-04-18T11:00:00",
        "reviewedAt": null,
        "publishedAt": null,
        "previewAvailable": true,
        "previewKind": "PDF"
      }
    ]
  }
}
```

### 8.3 Error Rules

This endpoint should use the existing project-standard error wrapper for:

- unauthenticated access
- authenticated but non-admin access
- unexpected backend failures

The dashboard does not require section-level partial-success contracts in this phase. If the summary endpoint fails, the page should treat it as a whole-page load failure and offer retry.

## 9. Frontend Flow Design

### 9.1 Page Structure

Route:

- `/admin/dashboard`

The page should render:

- a page header with overall admin-operations positioning
- four section cards:
  - verification
  - community
  - jobs
  - resources
- each card should contain:
  - compact metrics
  - recent list
  - jump CTA to the full workbench

The visual direction should feel like a calm overview board, not a dense table console.

### 9.2 Loading And Retry

Page behavior:

1. load the full summary on page enter
2. show a unified loading state while the summary is pending
3. if the request fails, show one page-level error state with retry
4. if the request succeeds, render all four sections from the single payload

### 9.3 Section Empty States

Per-section behavior:

- if a recent list is empty, keep the metrics area visible
- show a concise empty-state message inside that section
- keep the CTA link visible so admins can still open the destination workbench

Examples:

- verification with no pending rows still shows `pendingCount=0`
- jobs with no actionable rows still shows counts and the link to `/admin/jobs`
- resources with no pending rows still shows the resource counts and the link to `/admin/resources`

### 9.4 Navigation Integration

Frontend navigation updates should ensure:

- admins can reach `/admin/dashboard` from home
- admins can reach `/admin/dashboard` from `NavBar`
- non-admin navigation remains unchanged

## 10. Testing Strategy

### 10.1 Backend Tests

Backend tests should verify:

- admin can access `GET /api/admin/dashboard/summary`
- non-admin access is rejected
- verification counts and recent pending rows are returned deterministically
- community status counts match current post statuses
- jobs actionable list only contains `DRAFT` and `OFFLINE` rows ordered by `updatedAt desc`
- resource `closedCount` equals `REJECTED + OFFLINE`
- empty recent lists still return valid section structures

### 10.2 Frontend Tests

`AdminDashboardView` tests should verify:

- the page loads and renders four sections on success
- page-level retry appears on summary failure
- each section keeps its CTA when the recent list is empty
- admin-only navigation link renders for admins

`HomeView` tests should verify:

- the `admin-dashboard` entry is live for admins
- the entry routes to `/admin/dashboard`

`NavBar` tests should verify:

- admins see the dashboard link
- non-admin users do not see the dashboard link

### 10.3 Manual Smoke

A local smoke pass should verify:

1. log in as an admin user
2. open `/admin/dashboard`
3. confirm the four overview sections render
4. confirm each section shows counts and a recent list or empty state
5. confirm each CTA reaches the correct existing admin page
6. confirm the admin home entry reaches `/admin/dashboard`
7. confirm the admin nav link is visible and works

## 11. Acceptance Criteria

Phase M can be considered complete when all of the following are true:

- `/admin/dashboard` exists as a real admin-only route
- `GET /api/admin/dashboard/summary` exists as a real admin-only endpoint
- the dashboard shows verification, community, jobs, and resources in one overview page
- each section shows summary counts, a recent list, and a jump CTA
- the dashboard itself exposes no direct moderation or publishing actions
- existing admin workbenches remain the operational destinations
- admin home exposes a live `admin-dashboard` entry
- `NavBar` exposes an admin-only dashboard link
- backend and frontend verification for this slice pass
- one local admin smoke path passes

## 12. Implementation Handoff

The implementation plan for this phase should be written next at:

- `docs/superpowers/plans/2026-04-18-study-career-platform-phase-m-admin-dashboard-implementation.md`

Recommended implementation order:

1. backend summary DTO, mapper, service, and controller
2. frontend API adapter and route wiring
3. `AdminDashboardView` page implementation
4. home and nav activation
5. automated verification and local smoke
