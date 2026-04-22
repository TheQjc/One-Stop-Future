# Study-Career Platform Phase J Historical Local Resource MinIO Migration Design

> **Validation note:** This design was implemented and validated on 2026-04-17. Execution record: `docs/superpowers/plans/2026-04-17-study-career-platform-phase-j-historical-local-resource-minio-migration-implementation.md`. Documented verification covered `ResourceStorageConfigurationTests`, `LocalStoragePathResolverTests`, `HistoricalLocalResourceReaderTests`, `AdminResourceMigrationServiceTests`, and `AdminResourceControllerTests`, with optional live MinIO smoke recorded only when a reachable runtime was available.

## 1. Goal

Phase J adds an admin-managed migration path that copies historical raw resource files from the existing local filesystem store into MinIO while preserving the current Spring Boot monolith, Vue SPA, resource HTTP contracts, database schema, and Phase H preview behavior.

This phase delivers a narrow operational slice:

- admins can dry-run a historical local-resource migration from the backend
- admins can execute the migration in bounded batches through an existing admin API surface
- migrated objects keep the existing `storageKey` value and are written to MinIO under the same key
- local source files remain on disk after successful migration for rollback and manual verification

The design intentionally avoids turning this work into a runtime dual-read system, an automatic background migration framework, or a frontend migration console.

## 2. User-Validated Scope

The following decisions were explicitly chosen for this phase:

- migration is triggered manually from an admin backend endpoint
- the endpoint supports `dryRun` preview and formal execution
- migration supports condition-based filtering and a bounded `limit`
- each resource is handled independently; partial success is allowed
- successful migration keeps the local source file
- `storageKey` remains unchanged during migration
- the migration targets only raw resource files, not preview artifacts

## 3. Non-Goals

This phase does not implement:

- automatic migration during normal download, preview, or upload flows
- runtime dual-read fallback between local disk and MinIO
- deletion of local files after migration
- database schema changes for migration markers, object URLs, or bucket metadata
- frontend admin pages for migration management
- background jobs, schedulers, or startup-time migration tasks
- MinIO-backed preview artifact migration
- migration of unrelated local files outside the resource storage root
- bulk rollback logic

## 4. Chosen Approach

### 4.1 Recommendation

Add a dedicated admin migration endpoint under the existing admin resource controller and implement the migration logic in a focused backend service that reads historical local files from the configured local resource root and uploads them to MinIO with the existing `storageKey`.

### 4.2 Why This Approach

This approach fits the current codebase best:

- it reuses the current `/api/admin/resources` management surface
- it keeps migration concerns out of the user-facing resource service flow
- it avoids changing resource rows, controller contracts, and preview behavior
- it lets operators inspect likely outcomes with `dryRun` before writing objects
- it keeps rollback simple because the local files are preserved

### 4.3 Rejected Alternatives

#### Automatic On-Read Or On-Preview Migration

Rejected for this phase because it would:

- couple admin migration work to user traffic
- blur infrastructure failures with user-facing resource access
- quietly introduce a dual-read migration path that was explicitly rejected in Phase I

#### Offline Script Or Startup Task

Rejected for this phase because it would:

- move the workflow away from the current admin backend surface
- provide weaker access control and result visibility
- make partial retries and operator review harder

#### Regenerate `storageKey` During Migration

Rejected for this phase because it would:

- require database writes for every migrated resource
- complicate rollback and auditability
- create unnecessary divergence between local and MinIO object identity

## 5. Functional Scope

### 5.1 Migration Entry Point

Admins can call a new backend endpoint:

- `POST /api/admin/resources/migrate-to-minio`

The endpoint is admin-only and uses the existing authentication and authorization model.

### 5.2 Request Shape

The request supports the following fields:

- `dryRun`
- `statuses`
- `resourceIds`
- `keyword`
- `onlyMissingInMinio`
- `limit`

Recommended defaults:

- `dryRun=true`
- `onlyMissingInMinio=true`
- `limit=100`

Validation rules:

- `limit` must be positive
- `limit` must not exceed `200`
- unknown resource statuses should fail with `400`

Filter combination rules:

- `statuses`, `resourceIds`, and `keyword` combine with logical `AND`
- when a filter field is omitted or empty, that filter does not restrict the query
- when all three filters are omitted or empty, the endpoint evaluates all resource rows subject only to deterministic ordering and `limit`

`keyword` matching rules:

- `keyword` is trimmed before use
- blank `keyword` behaves the same as an omitted `keyword`
- matching uses a case-insensitive contains-style search
- the search fields are `title`, `summary`, and `file_name`
- this phase does not introduce full-text search, stemming, or fuzzy matching

### 5.3 Supported Resource States

Migration is storage-oriented rather than workflow-oriented, so all current resource states are eligible:

- `PENDING`
- `PUBLISHED`
- `REJECTED`
- `OFFLINE`

The endpoint may filter by status, but it should not hard-code a publish-only restriction.

### 5.4 MinIO Availability Rules

Migration should not require the active raw resource storage backend to already be set to `minio`.

However, the migration endpoint does require MinIO integration to be explicitly enabled and reachable. In practice, this means:

- `platform.integrations.minio.enabled=true`
- MinIO endpoint, credentials, and bucket configuration are present and valid
- the backend can create or use the configured bucket before migration runs

This allows operators to migrate historical objects before switching the active runtime storage backend from local disk to MinIO.

### 5.5 Local Source Rules

Historical local files are resolved from the configured raw-resource local root:

- `app.resource-storage.local-root`

This lookup is independent from the currently selected active `ResourceFileStorage` implementation.

`storageKey` must be resolved using explicit safe rules:

- normalize backslashes to forward slashes before path handling
- treat the key as a relative path only
- reject keys that are blank after trimming
- reject keys that start with `/` or contain a `..` path segment
- resolve the normalized key under `app.resource-storage.local-root`
- normalize the resolved path and verify it still stays within the configured local root

These checks are required so migration does not escape the resource storage root because of malformed historical keys.

The migration path should read only the raw local resource store. It must not scan or migrate:

- preview artifacts
- unrelated files outside the storage root
- files discovered by recursive directory walking without a corresponding resource row

## 6. Architecture

### 6.1 Existing Components Reused

- `AdminResourceController`
- `AdminResourceService`
- `ResourceItemMapper`
- `MinioIntegrationProperties`
- `MinioObjectOperations`
- existing resource database model and `storageKey` field

### 6.2 New Or Changed Backend Units

- `AdminResourceMigrationRequest`
  - request DTO for migration filters and execution mode
- `AdminResourceMigrationResponse`
  - summary and per-item response DTO
- `AdminResourceMigrationService`
  - orchestrates filtering, dry-run evaluation, execution, and result aggregation
- `HistoricalLocalResourceReader`
  - resolves and opens local files from `app.resource-storage.local-root` using existing `storageKey` semantics
- MinIO integration configuration adjustment
  - exposes MinIO client and object operations whenever MinIO integration is enabled, not only when `app.resource-storage.type=minio`

### 6.3 Responsibility Boundaries

- `AdminResourceController` owns HTTP input and admin endpoint exposure
- `AdminResourceMigrationService` owns batch orchestration and per-resource migration decisions
- `HistoricalLocalResourceReader` owns local file resolution and stream opening for historical files
- `MinioObjectOperations` owns bucket and object operations against MinIO
- `ResourceService` and user-facing resource controllers remain unchanged

## 7. API Design

### 7.1 Endpoint

```http
POST /api/admin/resources/migrate-to-minio
```

### 7.2 Request DTO

Recommended DTO fields:

- `Boolean dryRun`
- `List<String> statuses`
- `List<Long> resourceIds`
- `String keyword`
- `Boolean onlyMissingInMinio`
- `Integer limit`

The service layer should apply defaults rather than relying on transport-layer field initialization.

### 7.3 Response DTO

The response should remain wrapped in the existing `Result<T>` structure.

`data` should include:

- `dryRun`
- `requestedLimit`
- `matchedCount`
- `processedCount`
- `successCount`
- `skippedCount`
- `failureCount`
- `items`

Each item should include:

- `resourceId`
- `title`
- `status`
- `storageKey`
- `outcome`
- `message`

Recommended `outcome` values:

- `SUCCESS`
- `SKIPPED`
- `FAILED`

### 7.4 Response Semantics

For `dryRun=true`:

- resources that are ready to migrate should be reported as `SUCCESS` with a message such as `ready to migrate`
- no MinIO object writes occur
- no local files are modified
- no database rows are modified

For `dryRun=false`:

- resources that are uploaded successfully should be reported as `SUCCESS`
- resources that do not require action or cannot be migrated for expected reasons should be reported as `SKIPPED`
- resources that hit infrastructure or unexpected failures should be reported as `FAILED`

Top-level response rules:

- request validation failures return the existing error wrapper with `code=400`
- preflight infrastructure failures that prevent the batch from starting return the existing error wrapper with `code=500`
- a batch that starts successfully returns `code=200` even when individual items finish as `FAILED`

## 8. Data And Storage Semantics

### 8.1 `storageKey` Preservation

Migration must preserve the existing `storageKey`.

If a resource row stores:

- `2026/04/17/550e8400-e29b-41d4-a716-446655440000.pdf`

then the MinIO object must be written under exactly the same logical key.

The migration must not:

- generate a new key
- rewrite the database row
- persist endpoint-qualified or bucket-qualified URLs

### 8.2 Local File Retention

After successful upload to MinIO:

- the local source file stays on disk

This is an intentional operational safety choice for this phase.

### 8.3 MinIO Existence Semantics

`onlyMissingInMinio` controls whether existing MinIO objects are treated as already satisfied:

- when `true`, the migration checks whether the object already exists in MinIO before upload
- when `false`, the migration overwrites the existing object using the same key
- overwrite uses MinIO's normal single-object replacement behavior for the same bucket and object key
- this phase does not add versioning, compare-and-swap checks, or special conflict handling

The default should remain `true` for safety.

### 8.4 Database Mutation Rules

This migration should not update resource business fields or storage fields.

Specifically, it must not change:

- `status`
- `fileName`
- `fileSize`
- `storageKey`
- review fields
- timestamps on the resource row

## 9. Data Flow

### 9.1 Candidate Selection

1. Validate the request payload and apply defaults.
2. Query candidate resources from `t_resource_item` using the provided filters.
3. Apply filter semantics as an `AND` combination across `statuses`, `resourceIds`, and `keyword`.
4. Match `keyword` against `title`, `summary`, and `file_name` using case-insensitive contains semantics.
5. Order candidates by `id ASC` for repeatable execution.
6. Bound the evaluation to the requested `limit`.

### 9.2 Per-Resource Dry-Run Evaluation

For each candidate resource:

1. Validate that `storageKey` is present and non-blank.
2. Normalize and validate the key as a safe relative path.
3. Resolve the historical local file from `app.resource-storage.local-root`.
4. If `onlyMissingInMinio=true`, check whether the object already exists in MinIO.
5. Report one of the terminal outcomes:
   - `SUCCESS` when the object is ready to migrate
   - `SKIPPED` when migration is unnecessary or not applicable
   - `FAILED` when infrastructure checks fail

### 9.3 Per-Resource Execution

For formal execution:

1. Re-run the same readiness checks used by `dryRun`.
2. Open the local file as a stream.
3. Upload the object to MinIO using the existing `storageKey`.
4. When `onlyMissingInMinio=false`, upload proceeds even if the object already exists and replaces the current object body for that key.
5. Close the stream.
6. Record the final per-resource outcome in the response summary.

### 9.4 Processing Model

The batch should run serially in this phase.

This keeps behavior easier to reason about:

- fewer concurrent file handles
- simpler logs and failure analysis
- lower risk of overwhelming object storage or local disk I/O

## 10. Outcome Rules

### 10.1 `SUCCESS`

Use `SUCCESS` when:

- `dryRun=true` and the resource is ready to migrate
- `dryRun=false` and the MinIO upload completes successfully

### 10.2 `SKIPPED`

Use `SKIPPED` when:

- `storageKey` is missing or blank
- the local source file does not exist
- `onlyMissingInMinio=true` and the object already exists in MinIO
- the resource does not require upload for an expected, non-infrastructure reason

### 10.3 `FAILED`

Use `FAILED` when:

- the local file cannot be opened because of an I/O failure
- MinIO existence checks fail because of infrastructure errors
- MinIO upload fails
- any unexpected exception escapes the normal migration path for that resource

## 11. Error Handling

### 11.1 Request-Level Failures

Return `400` for invalid requests such as:

- invalid `limit`
- invalid status names
- malformed resource ID lists

Return an infrastructure-style failure when migration cannot start because:

- MinIO integration is disabled
- required MinIO settings are missing
- bucket initialization or MinIO client setup fails

These preflight startup-style failures should map to the existing business error shape with top-level `code=500`.

### 11.2 Batch-Level Behavior

This phase explicitly allows partial success.

That means:

- one failing resource does not roll back previously successful resource uploads
- one failing resource does not stop the rest of the batch
- the final response summarizes all terminal outcomes seen in the batch

### 11.3 Transaction Boundaries

The migration should not wrap the entire batch in a single database transaction.

Because the phase does not mutate resource rows, a large transaction would add cost without adding meaningful safety.

## 12. Testing Strategy

### 12.1 Controller Coverage

Extend admin controller tests to verify:

- non-admin users cannot call the migration endpoint
- admins can call `dryRun`
- invalid `limit` values return `400`
- the response includes summary counts and item outcomes

### 12.2 Service-Level Verification

Add focused service tests covering:

- missing `storageKey`
- missing local files
- already-existing MinIO objects when `onlyMissingInMinio=true`
- `dryRun` success reporting for ready-to-migrate resources
- successful execution upload
- partial success behavior across multiple resources in one batch

Repository-safe tests should use fakes or temporary directories rather than requiring live MinIO during normal unit-test runs.

### 12.3 Regression Safety

Existing resource behavior must remain green:

- upload
- rejected edit and resubmit
- download
- `PDF` preview
- `PPTX` preview
- `ZIP` preview
- admin review and publish/offline flows

No frontend contract change is required in this phase.

## 13. Deployment And Ops

### 13.1 Runtime Requirements

Operators need:

- a backend runtime with access to the historical local resource directory
- MinIO integration enabled and reachable
- valid MinIO bucket configuration

### 13.2 Operational Guidance

Recommended operator flow:

1. run `dryRun` with a bounded `limit`
2. inspect `SUCCESS / SKIPPED / FAILED` counts and sample items
3. run formal execution with the same filters
4. verify MinIO objects out-of-band if needed
5. keep local files until the environment fully cuts over

### 13.3 Example Request And Response

Example request:

```json
{
  "dryRun": true,
  "statuses": ["PUBLISHED", "OFFLINE"],
  "keyword": "resume",
  "onlyMissingInMinio": true,
  "limit": 100
}
```

Example response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dryRun": true,
    "requestedLimit": 100,
    "matchedCount": 2,
    "processedCount": 2,
    "successCount": 1,
    "skippedCount": 1,
    "failureCount": 0,
    "items": [
      {
        "resourceId": 12,
        "title": "Campus Resume Template",
        "status": "PUBLISHED",
        "storageKey": "2026/04/10/a.pdf",
        "outcome": "SUCCESS",
        "message": "ready to migrate"
      },
      {
        "resourceId": 18,
        "title": "Resume Writing Notes",
        "status": "OFFLINE",
        "storageKey": "2026/04/11/b.pdf",
        "outcome": "SKIPPED",
        "message": "object already exists in minio"
      }
    ]
  }
}
```

### 13.4 Documentation Follow-Up

README or operator notes should document:

- the new admin migration endpoint
- the fact that local files are preserved after migration
- the requirement that MinIO integration be enabled even if active raw storage is still local
- the fact that preview artifacts are still outside this migration scope

## 14. Acceptance Criteria

This design is complete when the implementation can demonstrate:

1. Admins can call a `dryRun` migration endpoint and receive a bounded per-item summary.
2. Admins can execute a formal migration batch that uploads historical local files to MinIO under the existing `storageKey`.
3. The migration can run while the active raw resource storage backend is still local, as long as MinIO integration is enabled and reachable.
4. Successful migration leaves the local source file untouched.
5. The migration allows partial success and reports failures without aborting the entire batch.
6. Existing resource upload, download, preview, and admin review flows remain unchanged.
