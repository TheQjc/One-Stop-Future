# Study Career Platform Phase W Historical Preview Artifact MinIO Migration Design

> **Validation note:** This design was implemented and validated on 2026-04-22 using the approved execution record at `docs/superpowers/plans/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-implementation.md`. Local verification suites now present for this slice are `LocalPreviewArtifactPathResolverTests`, `HistoricalLocalResourcePreviewArtifactReaderTests`, `ResourcePreviewServiceTests`, `AdminResourcePreviewMigrationServiceTests`, `AdminResourceControllerTests`, `ResourceControllerTests`, `MinioResourcePreviewArtifactStorageTests`, and `ResourcePreviewStorageConfigurationTests`.

## 1. Goal

Phase W adds an admin-managed migration path that copies historical local preview artifacts into MinIO while preserving the current Spring Boot monolith, Vue SPA, preview HTTP contracts, database schema, and the Phase T preview-storage selection model.

This phase delivers a narrow operational slice:

- admins can dry-run a historical preview-artifact migration from the backend
- admins can execute the migration in bounded batches through an existing admin API surface
- migration targets the current derived preview artifact for each eligible `PPTX`, `DOCX`, or `ZIP` resource row
- migrated preview artifacts are written to the existing MinIO preview namespace using the same logical artifact keys
- local preview source files remain on disk after successful migration for rollback and manual verification

The design intentionally avoids turning this work into a runtime dual-read system, an automatic preview regeneration framework, or a preview-artifact cleanup project.

## 2. User-Validated Scope

The following decisions were explicitly chosen for this phase:

- migration is triggered manually from an admin backend endpoint
- the endpoint supports `dryRun` preview and formal execution
- migration supports condition-based filtering and a bounded `limit`
- each resource is handled independently, and partial success is allowed
- candidate selection is resource-row driven rather than recursive directory scanning
- only the current fingerprinted preview artifact derived from the current resource metadata is eligible for migration
- successful migration keeps the local preview source file
- this phase does not add dual-read fallback, automatic regeneration of missing preview artifacts, or garbage collection

## 3. Non-Goals

This phase does not implement:

- runtime local-plus-MinIO dual-read fallback during normal preview requests
- recursive migration of stale historical fingerprint artifacts that no longer match current resource metadata
- automatic generation of missing preview artifacts during migration
- deletion of local preview artifacts after migration
- database schema changes for migration markers, object URLs, or preview lifecycle metadata
- frontend admin pages for migration management
- background jobs, schedulers, or startup-time migration tasks
- raw resource file migration changes
- a second MinIO bucket or a second MinIO integration configuration block
- bulk rollback logic

## 4. Chosen Approach

### 4.1 Recommendation

Add a dedicated admin migration endpoint under the existing admin resource controller and implement the migration logic in a focused backend service that:

- selects candidate `ResourceItem` rows through bounded filters
- determines whether the row currently has a derived preview artifact type
- derives the current logical artifact key from existing preview-key rules
- reads historical local preview artifacts from `app.resource-preview.local-root`
- uploads matching artifacts to MinIO under the configured preview prefix

### 4.2 Why This Approach

This approach fits the current codebase best:

- it reuses the proven admin-triggered migration workflow shape from Phase J
- it keeps migration concerns out of the user-facing preview read path
- it preserves Phase T artifact-key semantics and cache invalidation rules
- it allows operators to migrate before or after switching `app.resource-preview.type` to `minio`
- it keeps rollback simple because local preview artifacts are preserved

### 4.3 Rejected Alternatives

#### Runtime Dual-Read Local Then MinIO

Rejected for this phase because it would:

- move migration complexity into the normal preview path
- require runtimes to keep access to old local preview directories indefinitely
- blur cache misses, legacy fallback behavior, and storage failures

#### Recursive Filesystem Scan Of The Preview Root

Rejected for this phase because it would:

- migrate stale fingerprint artifacts with no current business value
- weaken the linkage between resource rows and migrated artifacts
- complicate filtering, response reporting, and operator intent

#### Automatic Regeneration-As-Migration

Rejected for this phase because it would:

- turn migration into a preview-generation workflow
- introduce `soffice`, conversion, and archive parsing failures into an ops slice
- make runtime cost and side effects less predictable than a pure copy-based migration

## 5. Functional Scope

### 5.1 Migration Entry Point

Admins can call a new backend endpoint:

- `POST /api/admin/resources/migrate-preview-artifacts-to-minio`

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

### 5.3 Supported Resource Types And States

Migration is storage-oriented rather than workflow-oriented, so all current resource states remain eligible:

- `PENDING`
- `PUBLISHED`
- `REJECTED`
- `OFFLINE`

Preview-artifact migration only applies to rows whose current file type produces a derived preview artifact:

- `PPTX`
- `DOCX`
- `ZIP`

Rows whose current file type does not have a derived preview artifact, such as `PDF`, remain in scope for filtering but should be reported as skipped rather than migrated.

### 5.4 MinIO Availability Rules

Migration should not require the active preview storage backend to already be set to `minio`.

However, the migration endpoint does require MinIO integration to be explicitly enabled and reachable. In practice, this means:

- `platform.integrations.minio.enabled=true`
- MinIO endpoint, credentials, and bucket configuration are present and valid
- the backend can create or use the configured bucket before migration runs

This allows operators to migrate historical preview artifacts before switching the active runtime preview backend from local disk to MinIO.

### 5.5 Local Preview Source Rules

Historical local preview artifacts are resolved from:

- `app.resource-preview.local-root`

This lookup is independent from the currently selected active `ResourcePreviewArtifactStorage` implementation.

Artifact-key resolution should follow the same safety rules used by local preview storage:

- normalize backslashes to forward slashes before path handling
- treat the artifact key as a relative path only
- reject keys that are blank after trimming
- reject keys that attempt to escape the preview root
- resolve the normalized key under `app.resource-preview.local-root`
- normalize the resolved path and verify it still stays within the configured preview root

The migration path should read only the exact derived local preview artifact addressed by the current logical key. It must not:

- scan unrelated files under the preview root
- migrate arbitrary files discovered without a corresponding resource row
- infer or recover old fingerprint values from directory contents

## 6. Architecture

### 6.1 Existing Components Reused

- `AdminResourceController`
- `ResourceItemMapper`
- `ResourcePreviewService`
- `ResourcePreviewProperties`
- `MinioIntegrationProperties`
- `MinioObjectOperations`
- existing preview artifact key generation in `ResourcePreviewService`

### 6.2 New Or Changed Backend Units

- `AdminResourcePreviewMigrationRequest`
  - request DTO for migration filters and execution mode
- `AdminResourcePreviewMigrationResponse`
  - summary and per-item response DTO
- `AdminResourcePreviewMigrationService`
  - orchestrates filtering, preview-type resolution, dry-run evaluation, execution, and result aggregation
- `LocalPreviewArtifactPathResolver`
  - resolves logical preview artifact keys under `app.resource-preview.local-root` with root-escape protection
- `HistoricalLocalResourcePreviewArtifactReader`
  - checks and opens local preview artifacts using the shared resolver
- `LocalResourcePreviewArtifactStorage`
  - should reuse the same resolver so active local storage and migration share artifact-key normalization rules

### 6.3 Responsibility Boundaries

- `AdminResourceController` owns HTTP input and admin endpoint exposure
- `AdminResourcePreviewMigrationService` owns batch orchestration and per-resource migration decisions
- `ResourcePreviewService` remains the owner of artifact-key derivation and fingerprint semantics
- `LocalPreviewArtifactPathResolver` and `HistoricalLocalResourcePreviewArtifactReader` own local preview path safety and stream opening
- `MinioObjectOperations` owns bucket and object operations against MinIO
- user-facing preview endpoints and preview-generation flows remain unchanged

## 7. API Design

### 7.1 Endpoint

```http
POST /api/admin/resources/migrate-preview-artifacts-to-minio
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
- `previewType`
- `artifactKey`
- `outcome`
- `message`

Recommended `previewType` values:

- `PPTX`
- `DOCX`
- `ZIP`
- `NONE`

Recommended `outcome` values:

- `SUCCESS`
- `SKIPPED`
- `FAILED`

`artifactKey` may be `null` only when `previewType=NONE`.

### 7.4 Response Semantics

The response remains resource-oriented:

- one matched resource row produces at most one response item in this phase
- `matchedCount` reflects the number of resource rows selected by the filters
- `processedCount` reflects the number of returned items

Because each supported resource currently maps to exactly one derived preview artifact, resource-oriented reporting is sufficient for this slice.

## 8. Artifact And Storage Semantics

### 8.1 Current Artifact-Key Derivation

The migration must target only the current logical artifact key derived from current resource metadata.

`ResourcePreviewService` should remain the source of truth for the logical key shapes:

- `pptx/<resource-id>/<fingerprint>.pdf`
- `docx/<resource-id>/<fingerprint>.pdf`
- `zip/<resource-id>/<fingerprint>.json`

`PDF` and other non-derived preview formats do not produce a migration target in this phase.

### 8.2 Current-Fingerprint Boundary

This phase intentionally migrates only the current fingerprinted artifact for the current resource row.

That means:

- if the local preview root still contains older fingerprint artifacts for previous versions of the same resource, they are ignored
- if the current artifact key does not exist locally, the migration reports a skip rather than searching for stale fallback candidates
- this phase does not attempt to reconstruct or guess missing preview artifacts

### 8.3 MinIO Object Mapping And Overwrite Rules

When migration uploads an artifact to MinIO, it should reuse the Phase T preview namespace:

- `<preview-minio-prefix>/<logical-artifact-key>`

Where `<preview-minio-prefix>` comes from:

- `app.resource-preview.minio-prefix`

Existence rules:

- when `onlyMissingInMinio=true`, an already-existing MinIO object should be reported as skipped
- when `onlyMissingInMinio=false`, execution mode may overwrite the existing object using the same MinIO key
- dry-run mode should report the same decision outcome without performing the write

### 8.4 Local Artifact Retention And Database Mutation Rules

Successful migration must not delete the local source artifact.

This phase should not update:

- `t_resource_item`
- preview storage configuration
- preview fingerprint fields
- any other business data

## 9. Data Flow

### 9.1 Candidate Selection

The service should load candidate resources from `t_resource_item` using the requested filters:

- `statuses`
- `resourceIds`
- `keyword`
- deterministic `id ASC` ordering
- bounded `LIMIT`

This phase should not walk the local preview directory as the primary candidate source.

### 9.2 Per-Resource Dry-Run Evaluation

For each matched resource:

1. determine whether the current file type has a derived preview artifact
2. if not, return `SKIPPED` with `previewType=NONE`
3. derive the current logical artifact key from `ResourcePreviewService`
4. check whether the corresponding local preview artifact exists
5. if missing, return `SKIPPED`
6. if `onlyMissingInMinio=true`, check whether the target MinIO object already exists
7. if it exists, return `SKIPPED`
8. if `dryRun=true`, return `SUCCESS` with `ready to migrate`

### 9.3 Per-Resource Execution

When execution mode is enabled:

1. open the local preview artifact through the historical local reader
2. upload the artifact bytes to the shared MinIO bucket under the preview prefix plus the logical key
3. return `SUCCESS` when upload completes
4. if stream open fails, return `FAILED`
5. if upload fails, return `FAILED`

### 9.4 Processing Model

Processing should remain:

- serial
- bounded by `limit`
- non-transactional at the batch level
- tolerant of partial success

An item-level failure must not abort the rest of the batch.

## 10. Outcome Rules

### 10.1 `SUCCESS`

Use `SUCCESS` when:

- `dryRun=true` and the artifact is ready to migrate
- execution succeeds and the artifact is uploaded to MinIO

Recommended messages:

- `ready to migrate`
- `uploaded to minio`

### 10.2 `SKIPPED`

Use `SKIPPED` when:

- the resource does not currently have a derived preview artifact
- the local preview artifact does not exist for the current logical key
- the target MinIO object already exists and `onlyMissingInMinio=true`

Recommended messages:

- `preview not supported`
- `local preview artifact not found`
- `preview artifact already exists in minio`

### 10.3 `FAILED`

Use `FAILED` when:

- the local preview artifact should be readable but cannot be opened
- the upload to MinIO fails

Recommended messages:

- `failed to open local preview artifact`
- `failed to upload preview artifact`

## 11. Error Handling

### 11.1 Request-Level Failures

Return request-level failures when:

- `limit` is invalid
- any requested resource status is invalid
- the caller is not authorized as an admin

Return an infrastructure-style failure when migration cannot start because:

- MinIO integration is disabled
- MinIO bucket verification or creation fails
- required MinIO configuration is missing

Recommended infrastructure failure message:

- `minio preview migration unavailable`

### 11.2 Batch-Level Behavior

Once preflight succeeds, the batch should continue processing even if individual items fail.

The endpoint should return a normal batch response with mixed `SUCCESS`, `SKIPPED`, and `FAILED` items rather than aborting on the first item-level issue.

### 11.3 Transaction Boundaries

The migration should not wrap the entire batch in a single database transaction.

This phase is object-copy oriented and should not introduce transactional coupling between:

- resource-row lookup
- local artifact reads
- MinIO object writes

## 12. Testing Strategy

### 12.1 Controller Coverage

Add focused controller tests covering:

- admin-only access to the new endpoint
- empty request body normalization to defaults
- valid request success serialization
- invalid request validation such as an out-of-range `limit`

### 12.2 Service-Level Verification

Add focused service tests covering:

- dry-run success when the current local preview artifact exists and MinIO is missing the object
- skip behavior for unsupported preview types
- skip behavior when the local preview artifact is missing
- skip behavior when the MinIO object already exists and `onlyMissingInMinio=true`
- overwrite-path execution when `onlyMissingInMinio=false`
- item-level open and upload failures with partial batch continuation
- deterministic filtering and bounded-batch query behavior
- fail-fast behavior when MinIO integration is unavailable

### 12.3 Regression Safety

Regression coverage should also confirm:

- current preview runtime behavior does not gain dual-read fallback in this phase
- preview-artifact key derivation remains aligned with `ResourcePreviewService`
- local preview path normalization is shared between active local preview storage and historical migration logic

## 13. Deployment And Ops

### 13.1 Runtime Requirements

Historical preview-artifact migration requires:

- access to the configured local preview root
- MinIO integration enabled and reachable
- the shared MinIO bucket configured for preview artifacts

The migration may run whether `app.resource-preview.type` is currently `local` or `minio`.

### 13.2 Operational Guidance

Recommended operator flow:

1. ensure the backend still has access to the historical local preview root
2. enable MinIO integration and verify the shared bucket configuration
3. call the migration endpoint with `dryRun=true`
4. inspect skipped and failure reasons
5. execute bounded migration batches
6. optionally switch or keep `app.resource-preview.type=minio` based on rollout timing

If the runtime has already switched to MinIO preview storage, this migration can still recover cache hits for historical local artifacts as long as the old local preview directory remains accessible.

### 13.3 Example Request And Response

Example request:

```json
{
  "dryRun": true,
  "statuses": ["PUBLISHED", "OFFLINE"],
  "resourceIds": [21, 22, 23],
  "keyword": "career",
  "onlyMissingInMinio": true,
  "limit": 50
}
```

Example response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dryRun": true,
    "requestedLimit": 50,
    "matchedCount": 3,
    "processedCount": 3,
    "successCount": 1,
    "skippedCount": 2,
    "failureCount": 0,
    "items": [
      {
        "resourceId": 21,
        "title": "Career Deck",
        "status": "PUBLISHED",
        "previewType": "PPTX",
        "artifactKey": "pptx/21/3f6c9f.pdf",
        "outcome": "SUCCESS",
        "message": "ready to migrate"
      },
      {
        "resourceId": 22,
        "title": "Resume Guide",
        "status": "PUBLISHED",
        "previewType": "DOCX",
        "artifactKey": "docx/22/9a4d20.pdf",
        "outcome": "SKIPPED",
        "message": "preview artifact already exists in minio"
      },
      {
        "resourceId": 23,
        "title": "Scholarship Handbook",
        "status": "OFFLINE",
        "previewType": "NONE",
        "artifactKey": null,
        "outcome": "SKIPPED",
        "message": "preview not supported"
      }
    ]
  }
}
```

### 13.4 Documentation Follow-Up

Implementation should update the README to document:

- the new admin preview-artifact migration endpoint
- the fact that migration is admin-triggered and backend-only
- the fact that migration only covers the current logical preview artifact for each eligible resource
- the fact that local source artifacts are preserved after migration
- the fact that dual-read fallback and automatic cleanup are still out of scope after this phase

## 14. Acceptance Criteria

1. Admins can call a dry-run endpoint and receive a bounded per-resource summary for historical preview-artifact migration.
2. Admins can execute a formal migration batch that uploads current `PPTX`, `DOCX`, and `ZIP` preview artifacts from the local preview root to MinIO under the Phase T preview namespace.
3. Migration can run while the active preview storage backend is still `local`, as long as MinIO integration is enabled and the local preview root is accessible.
4. Resources without a derived preview artifact are reported cleanly as skipped rather than failing the batch.
5. Missing or already-migrated artifacts are reported cleanly without aborting the rest of the batch.
6. Successful migration leaves local source preview artifacts untouched.
7. Existing preview runtime behavior remains unchanged: no runtime dual-read fallback, no automatic regeneration during migration, and no automatic preview-artifact cleanup are added in this phase.
