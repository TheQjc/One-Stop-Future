# Study Career Platform Phase V Third-Party Job Sync Design

## 1. Goal

Phase V adds the first third-party job synchronization flow while preserving the current Spring Boot monolith, Vue SPA, existing public jobs board, existing admin jobs desk, and the current draft / publish / offline / delete lifecycle.

This phase delivers one narrow admin productivity slice:

- admins can manually trigger one third-party job sync from the existing `/admin/jobs` page
- the backend fetches one fixed external `HTTP JSON feed`
- new remote jobs become local `DRAFT` rows
- existing non-`DELETED` jobs are updated in place by matching `sourceUrl`
- local `DELETED` jobs are not revived automatically
- the admin receives one immediate sync summary with counts plus per-item issues

This phase intentionally does not turn job sync into a generic ETL platform, scheduler framework, or multi-connector marketplace.

## 2. User-Validated Scope

The following decisions were chosen for this phase during design discussion:

- the next slice after Phase U is `Phase V: Third-party job sync first slice`
- the first sync source is one fixed `HTTP JSON feed`
- sync is triggered manually by an admin
- sync entry lives inside the existing `/admin/jobs` workbench
- sync is synchronous
- when a remote item matches an existing non-`DELETED` job by `sourceUrl`, the existing job is updated
- when a remote item matches an existing `DELETED` job by `sourceUrl`, it is skipped and reported
- feed-level failures fail the sync
- item-level validation issues do not abort the whole sync; valid items still continue

## 3. Non-Goals

This phase does not implement:

- multiple third-party feeds
- admin-entered arbitrary feed URLs
- scheduler-driven automatic sync
- sync history tables or progress polling
- retry queues, dead-letter handling, or webhook callbacks
- external authentication flows beyond an optional static bearer token
- remote-driven publish or offline actions
- automatic deletion or offlining when remote jobs disappear from the feed
- public jobs-page redesign
- generalized partner configuration UI

## 4. Chosen Approach

### 4.1 Recommendation

Add one admin-only sync endpoint plus one focused sync service that pulls a fixed JSON feed, validates each remote item, creates missing jobs as `DRAFT`, updates existing non-`DELETED` jobs in place, skips `DELETED` matches, and returns a compact summary with per-item issues.

Recommended runtime shape:

- frontend keeps using `/admin/jobs`
- backend adds `POST /api/admin/jobs/sync`
- request body is omitted in the first slice because the source is fixed in server-side config
- backend-owned config provides:
  - `enabled`
  - `feed-url`
  - `source-name`
  - optional static bearer token
  - timeouts
- success response returns counts plus `issues[]`

### 4.2 Why This Approach

This approach fits the current codebase best:

- it matches the existing admin-triggered workflow pattern already used by resource migration
- it avoids adding a scheduler before the sync semantics are proven
- it keeps partner feed configuration on the server instead of exposing arbitrary external fetches from the admin UI
- it lets the system distinguish feed-wide failures from item-level data issues
- it keeps future expansion possible:
  - scheduled sync
  - multiple feeds
  - sync history
  - connector-specific adapters

### 4.3 Rejected Alternatives

#### Let Admins Paste Any Feed URL

Rejected because it would:

- expand the security boundary
- require input validation for external URLs before the first connector contract is proven
- make reproducible testing and rollout harder

#### Build Scheduled Sync First

Rejected because it would:

- introduce scheduler semantics, overlap handling, and observability before the basic sync rules are stable
- require hidden operational decisions that the product has not validated yet
- make debugging first-release feed issues slower and less controllable

#### Treat Third-Party Sync As Another CSV Import Mode

Rejected because it would:

- mix transport concerns with file-ingestion concerns
- blur the difference between admin-supplied batch files and backend-owned external feeds
- make future connector-specific logic awkward

## 5. Functional Scope

### 5.1 Admin Sync Entry

Admins can open the existing route:

- `/admin/jobs`

The page gains one additional sync surface:

- short explanation of the fixed feed source
- one `Sync Feed` action button
- loading state during the request
- sync summary after success
- issue list for skipped or invalid remote items

No new admin route is added.

### 5.2 Sync Outcomes

For each remote job in the feed:

- if no local job has the same `sourceUrl`
  - create a new `JobPosting`
  - set `status = DRAFT`
  - set `publishedAt = null`
  - set `createdBy` and `updatedBy` to the triggering admin
- if one local job has the same `sourceUrl` and `status != DELETED`
  - update the existing row in place
  - preserve the existing `status`
  - preserve the existing `publishedAt`
  - update `updatedBy` and `updatedAt`
- if one local job has the same `sourceUrl` and `status = DELETED`
  - skip the remote item
  - record a skip issue

### 5.3 Missing-From-Feed Behavior

If a local job previously created or updated by this sync source is absent from the latest feed:

- do nothing in Phase V
- do not delete it
- do not offline it
- do not mark it stale automatically

This keeps the first slice strictly additive / update-only for feed-present items.

### 5.4 Public Visibility

Phase V does not auto-publish newly synced jobs.

Important consequence:

- new synced jobs are not visible on public `/jobs` until an admin publishes them
- existing published jobs that are updated by sync keep their current `PUBLISHED` status, so their public content updates immediately after sync

This keeps lifecycle behavior simple in the first slice while preserving existing publish semantics.

## 6. External Feed Contract

### 6.1 Transport

The first connector contract is:

- method: `GET`
- response type: `application/json`
- payload encoding: `UTF-8`

The backend owns the feed URL and any static bearer token through config.

### 6.2 Top-Level JSON Shape

The approved first-shape response is:

```json
{
  "jobs": [
    {
      "title": "Data Analyst Trainee",
      "companyName": "South Coast Studio",
      "city": "Hangzhou",
      "jobType": "FULL_TIME",
      "educationRequirement": "BACHELOR",
      "sourceUrl": "https://partner.example/jobs/data-analyst-trainee",
      "summary": "Support dashboard delivery, reporting, and operational analysis.",
      "content": "Assist with dashboard updates, reporting QA, and weekly hiring analytics.",
      "deadlineAt": "2026-06-20 18:00:00"
    }
  ]
}
```

### 6.3 Required And Optional Fields

Required per remote item:

- `title`
- `companyName`
- `city`
- `jobType`
- `educationRequirement`
- `sourceUrl`
- `summary`

Optional per remote item:

- `content`
- `deadlineAt`

Server-side supplied fields:

- `sourcePlatform`
  - derived from configured `sourceName`
  - configured `sourceName` must be non-blank and fit the existing local `source_platform` limit of `50`
- lifecycle and audit fields

### 6.4 Field Rules

The remote item rules align with the current local job constraints:

- `title`
  - required
  - max `120`
- `companyName`
  - required
  - max `80`
- `city`
  - required
  - max `80`
- `jobType`
  - required
  - accepted values:
    - `INTERNSHIP`
    - `FULL_TIME`
    - `CAMPUS`
- `educationRequirement`
  - required
  - accepted values:
    - `ANY`
    - `BACHELOR`
    - `MASTER`
    - `DOCTOR`
- `sourceUrl`
  - required
  - max `500`
  - must be `http` or `https`
- `summary`
  - required
  - max `300`
- `content`
  - optional
  - max `10000`
- `deadlineAt`
  - optional
  - accepted format:
    - `yyyy-MM-dd HH:mm:ss`

Normalization rules:

- trim leading and trailing whitespace
- normalize `jobType` and `educationRequirement` case-insensitively

### 6.5 Feed-Level Invalid Shapes

The whole sync fails when:

- the sync integration is disabled or misconfigured
- the HTTP request fails
- the response is not valid JSON
- the top-level `jobs` array is missing
- the feed contains duplicate `sourceUrl` values

These are treated as feed-wide contract failures rather than item-level issues.

## 7. Backend Design

### 7.1 New Units

Phase V introduces these focused backend units:

- `JobSyncProperties`
  - binds fixed third-party feed settings
- `ThirdPartyJobFeedClient`
  - calls the external feed
  - parses JSON into internal feed item DTOs
- `ThirdPartyJobFeedItem`
  - backend-owned remote item shape
- `ThirdPartyJobSyncService`
  - coordinates fetch, validation, duplicate detection, upsert, skip rules, and summary building
- `AdminJobController` sync action
  - admin-only request boundary for `POST /api/admin/jobs/sync`

### 7.2 Targeted Reuse And Improvement

Phase V should reuse current job normalization rules instead of introducing a third separate ruleset.

Recommended targeted improvement:

- extract a small shared normalizer for core job fields
- reuse it from:
  - `AdminJobService`
  - `JobPostingDraftFactory`
  - `ThirdPartyJobSyncService`

This keeps:

- enum normalization
- URL validation
- text trimming
- deadline parsing

consistent across manual create, CSV import, and feed sync.

### 7.3 Existing Units Reused

Phase V should reuse:

- `JobPosting`
- `JobPostingMapper`
- current admin security boundary on `/api/admin/**`
- current `/admin/jobs` page and jobs list refresh flow
- current `JobType`, `JobEducationRequirement`, and `JobPostingStatus` enums

### 7.4 HTTP Client Choice

The backend currently has no existing external HTTP abstraction for third-party APIs.

Recommended first-slice implementation:

- use JDK `java.net.http.HttpClient`

Why:

- no new runtime dependency is required
- the connector is only one fixed JSON `GET`
- the code remains easy to test with a lightweight local test server

## 8. Sync Semantics

### 8.1 Create Rule

Create a new local job when:

- remote `sourceUrl` does not match any local row

Create behavior:

- write validated content fields
- set `sourcePlatform` from config
- set `status = DRAFT`
- set `publishedAt = null`
- set `createdBy` and `updatedBy` to the triggering admin

### 8.2 Update Rule

Update an existing local job when:

- remote `sourceUrl` matches exactly one local row
- local `status != DELETED`

Update behavior:

- overwrite synced content fields:
  - `title`
  - `companyName`
  - `city`
  - `jobType`
  - `educationRequirement`
  - `sourcePlatform`
  - `sourceUrl`
  - `summary`
  - `content`
  - `deadlineAt`
- preserve:
  - `status`
  - `publishedAt`
  - `createdBy`
  - `createdAt`
- set:
  - `updatedBy`
  - `updatedAt`

### 8.3 Deleted Rule

Skip a remote item when:

- remote `sourceUrl` matches a local row with `status = DELETED`

Skip behavior:

- do not recreate the job
- do not update the deleted row
- record an issue with a readable skip reason

### 8.4 Invalid Item Rule

When one remote item is invalid:

- record an item-level issue
- skip only that item
- continue processing the rest of the feed

### 8.5 Transaction Boundary

Phase V does not need all-or-nothing semantics across the entire feed.

Recommended behavior:

1. fetch and parse the feed
2. perform feed-level validation
3. for each remote item:
   - validate item
   - create, update, or skip
4. commit valid writes from this request
5. return a summary with counts and issues

This keeps sync resilient to isolated partner data problems.

## 9. Runtime Configuration

### 9.1 Properties

Recommended config prefix:

- `platform.integrations.job-sync`

Recommended properties:

- `enabled`
- `feed-url`
- `source-name`
- `bearer-token`
- `connect-timeout-ms`
- `read-timeout-ms`

Suggested default posture:

- disabled by default
- no feed URL in `application-local.yml`
- when enabled, `source-name` is required and must not exceed `50`

### 9.2 Example Shape

```yaml
platform:
  integrations:
    job-sync:
      enabled: false
      feed-url: ""
      source-name: "Partner Feed"
      bearer-token: ""
      connect-timeout-ms: 5000
      read-timeout-ms: 10000
```

## 10. API Contract

### 10.1 Endpoint

Add one new admin endpoint:

- `POST /api/admin/jobs/sync`

### 10.2 Request Shape

Phase V request body:

- none

The endpoint always uses the fixed server-side feed configuration.

### 10.3 Success Response

Success keeps the existing result envelope:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sourceName": "Partner Feed",
    "fetchedCount": 12,
    "createdCount": 4,
    "updatedCount": 6,
    "skippedCount": 1,
    "invalidCount": 1,
    "defaultCreatedStatus": "DRAFT",
    "issues": [
      {
        "itemIndex": 7,
        "sourceUrl": "https://partner.example/jobs/old-role",
        "type": "SKIPPED",
        "message": "job is deleted locally"
      },
      {
        "itemIndex": 9,
        "sourceUrl": "https://partner.example/jobs/broken-role",
        "type": "INVALID",
        "message": "invalid job type"
      }
    ]
  }
}
```

Rules:

- `itemIndex` is `1`-based inside the remote `jobs` array
- `issues` are sorted by `itemIndex`
- empty `issues` means the sync completed without skipped or invalid items

### 10.4 Feed Failure Response

When the fixed feed cannot be fetched or parsed:

- return the existing business-error envelope style
- do not write partial changes

Example categories:

- sync integration unavailable
- feed request failed
- invalid feed response

## 11. Frontend Design

### 11.1 Admin Jobs Desk Integration

The existing `/admin/jobs` page gains one additional sync card or panel:

- source label
- short explanation of what sync does
- one `Sync Feed` button
- current sync summary block
- issue list block

No new route is added.

### 11.2 Success Interaction

On successful sync:

- show the summary counts
- show skipped / invalid issues when present
- refresh the admin jobs list
- keep the admin on `/admin/jobs`

### 11.3 Failure Interaction

On feed failure:

- show one clear error message
- keep the current jobs list unchanged
- keep the admin on the same page so they can retry later

## 12. Error Handling

### 12.1 Authorization

- guest access to the sync endpoint returns `401`
- authenticated non-admin access returns `403`

### 12.2 Feed-Level Failures

Feed-level failures return a business error and abort the sync when:

- integration is disabled
- required config is missing
- remote request fails
- remote JSON is malformed
- duplicate `sourceUrl` values exist in the feed

### 12.3 Item-Level Issues

Item-level issues do not abort the sync when:

- a required field is missing
- a field exceeds max length
- an enum value is invalid
- `sourceUrl` is invalid
- `deadlineAt` is invalid
- the local matched row is `DELETED`

These are returned in `data.issues`.

## 13. Testing Strategy

### 13.1 Backend

Add focused backend coverage for:

- admin can trigger sync successfully
- fixed feed creates new jobs as `DRAFT`
- existing non-`DELETED` job is updated by matching `sourceUrl`
- existing `DELETED` job is skipped and reported
- invalid remote item is reported without aborting valid items
- duplicate `sourceUrl` in the feed fails the sync
- malformed JSON fails the sync
- non-admin access rejection

Recommended test style:

- service tests use a lightweight local HTTP server such as JDK `HttpServer`
- controller tests keep using `MockMvc`

### 13.2 Frontend

Add focused frontend coverage for:

- clicking `Sync Feed` from `/admin/jobs`
- success summary rendering
- issue list rendering
- jobs list refresh after success
- feed failure error rendering

## 14. Acceptance Criteria

This design is complete when the implementation can demonstrate:

1. admins can manually trigger one fixed third-party job sync from `/admin/jobs`.
2. new remote jobs are created locally as `DRAFT`.
3. existing non-`DELETED` jobs are updated in place by `sourceUrl`.
4. local `DELETED` jobs are not recreated automatically.
5. feed-level failures abort the sync with no writes.
6. item-level validation issues are reported without blocking valid items.
7. the admin jobs list refreshes after a successful sync.
8. existing public jobs browsing, favorites, applications, CSV import, and manual admin job lifecycle actions remain unchanged.
