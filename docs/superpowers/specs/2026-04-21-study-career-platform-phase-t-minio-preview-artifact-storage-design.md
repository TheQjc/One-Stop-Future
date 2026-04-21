# Study Career Platform Phase T MinIO Preview Artifact Storage Design

> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-implementation.md`. Local verification covered `ResourcePreviewStorageConfigurationTests`, `MinioResourcePreviewArtifactStorageTests`, `ResourcePreviewServiceTests`, `SofficeDocxPreviewGeneratorTests`, `ResourceControllerTests`, `AdminResourceControllerTests`, the targeted frontend resource-preview Vitest suite, and `frontend` production build. Repository-safe MinIO verification used in-memory test doubles; no live MinIO smoke was executed in this environment.

## 1. Goal

Phase T moves newly generated resource preview artifacts to MinIO-capable storage while preserving the current Spring Boot monolith, Vue SPA, preview HTTP contracts, database schema, and preview-generation behavior introduced through Phases H and S.

This phase delivers a narrow infrastructure slice:

- preview artifacts can be stored in MinIO for non-`local` runtimes
- preview generation and preview authorization stay inside the existing backend flow
- local development keeps using local filesystem preview storage
- raw resource storage and preview-artifact storage become independently selectable

This phase intentionally avoids turning preview storage into a migration system, dual-read cache layer, or artifact lifecycle project.

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- only newly generated preview artifacts move to MinIO
- historical local preview artifacts are not migrated
- preview-artifact garbage collection remains out of scope
- the existing MinIO bucket is reused
- preview artifacts are separated inside that bucket by a dedicated prefix
- local development keeps the current local preview closed loop

## 3. Non-Goals

This phase does not implement:

- historical preview-artifact migration
- local + MinIO dual-read fallback for old preview artifacts
- preview-artifact garbage collection
- a second MinIO client or second MinIO configuration block
- frontend preview contract changes
- new preview endpoints
- raw resource storage changes beyond compatibility with the new preview storage option
- preview pre-generation at upload time
- preview artifact database tables, versioning, or lifecycle policies

## 4. Chosen Approach

### 4.1 Recommendation

Keep the existing `ResourcePreviewArtifactStorage` boundary and add a second implementation, `MinioResourcePreviewArtifactStorage`, selected by configuration through a dedicated preview-storage type switch.

Recommended preview-storage configuration:

- `app.resource-preview.type=local|minio`
- `app.resource-preview.local-root=...`
- `app.resource-preview.minio-prefix=preview-artifacts`

The MinIO bucket continues to come from the existing shared `platform.integrations.minio.bucket` setting.

### 4.2 Why This Approach

This approach fits the current codebase best:

- it mirrors the existing raw resource storage selection pattern from Phase I
- it keeps `ResourcePreviewService` unaware of backend storage details
- it preserves the current preview artifact key model and cache invalidation rules
- it lets raw resource storage and preview storage evolve independently
- it keeps local development behavior unchanged by default

### 4.3 Rejected Alternatives

#### Auto-Follow Raw Resource Storage Type

Rejected for this phase because it would:

- tightly couple preview storage to raw resource storage
- remove flexibility for staged rollout and troubleshooting
- make it harder to preserve the current local preview loop while raw storage evolves

#### Dual-Read Local Then MinIO

Rejected for this phase because it would:

- turn a clean storage cutover into a migration system
- blur whether preview misses are normal cache misses or legacy-storage fallbacks
- increase runtime complexity for limited value in this slice

#### Separate Preview Bucket

Rejected for this phase because it would:

- add extra bucket setup and configuration burden
- create a second operational surface without strong product value
- solve isolation through infrastructure instead of a simpler object-key prefix

## 5. Functional Scope

### 5.1 Local Development

When the backend runs with the `local` profile:

- preview artifacts continue to use local filesystem storage
- raw resource files continue to follow the current local closed loop
- current README local run instructions remain valid
- existing local smoke expectations remain stable

### 5.2 MinIO-Backed Preview Runtime

When `app.resource-preview.type=minio`:

- newly generated `PPTX -> PDF` preview artifacts are written to MinIO
- newly generated `DOCX -> PDF` preview artifacts are written to MinIO
- newly generated `ZIP -> JSON` preview artifacts are written to MinIO
- repeated preview requests still reuse cached artifacts through the existing artifact-key lookup
- old local preview artifacts are not consulted or migrated

### 5.3 Historical Artifact Boundary

This phase does not migrate existing local preview artifacts.

That means:

- switching from local preview storage to MinIO preview storage causes old local preview artifacts to stop participating in cache hits
- the next preview request for an uncached object key simply regenerates the artifact and stores it in MinIO
- this behavior is accepted for the phase because it preserves correctness without introducing migration or dual-read complexity

## 6. Architecture

### 6.1 Existing Components Reused

- `ResourcePreviewArtifactStorage`
- `ResourcePreviewService`
- `ResourceService`
- `ResourceController`
- `AdminResourceController`
- `MinioIntegrationProperties`
- `MinioObjectOperations`
- existing preview artifact key generation in `ResourcePreviewService`

### 6.2 New Or Changed Backend Units

- `ResourcePreviewProperties`
  - gains a dedicated preview storage type
  - gains a MinIO prefix setting
- `ResourcePreviewStorageConfiguration`
  - selects exactly one `ResourcePreviewArtifactStorage` implementation
  - validates MinIO enablement when preview storage type is `minio`
- `MinioResourcePreviewArtifactStorage`
  - stores, opens, and checks preview artifacts in MinIO
  - ensures the shared bucket exists before artifact use
- `LocalResourcePreviewArtifactStorage`
  - remains the default local implementation
  - should activate only when preview storage type is `local`

### 6.3 Storage Boundary Contract

The preview artifact storage abstraction should remain responsible only for artifact object operations:

- `exists`
- `open`
- `write`

Preview-generation logic, fingerprinting, permissions, controller responses, and content-type handling must remain outside the storage layer.

## 7. Configuration Design

### 7.1 Preview Storage Switch

Phase T introduces a dedicated preview-storage selector:

- `app.resource-preview.type`

Supported values in this phase:

- `local`
- `minio`

This setting is independent from `app.resource-storage.type`.

### 7.2 Preview Storage Settings

Phase T keeps and extends the current preview settings:

- `app.resource-preview.local-root`
- `app.resource-preview.minio-prefix`
- `app.resource-preview.docx.soffice-command`

Recommended defaults:

- `app.resource-preview.type=${RESOURCE_PREVIEW_TYPE:local}`
- `app.resource-preview.local-root=${RESOURCE_PREVIEW_LOCAL_ROOT:.local-storage/previews}`
- `app.resource-preview.minio-prefix=${RESOURCE_PREVIEW_MINIO_PREFIX:preview-artifacts}`

### 7.3 Shared MinIO Integration Settings

Phase T reuses the existing `platform.integrations.minio` block:

- `enabled`
- `endpoint`
- `access-key`
- `secret-key`
- `bucket`

This phase should not introduce a second MinIO integration prefix.

### 7.4 Local Profile Rules

`application-local.yml` remains authoritative for local development:

- `app.resource-preview.type=local`
- `app.resource-preview.local-root=.local-storage/previews`
- `platform.integrations.minio.enabled=false`

This keeps the current closed loop intact.

## 8. Object Key Design

### 8.1 Artifact Key Semantics

`ResourcePreviewService` should keep generating the same logical artifact keys it already uses:

- `pptx/<resource-id>/<fingerprint>.pdf`
- `docx/<resource-id>/<fingerprint>.pdf`
- `zip/<resource-id>/<fingerprint>.json`

These keys remain backend-internal logical identifiers, not public URLs.

### 8.2 MinIO Object Key Mapping

When preview storage type is `minio`, the logical artifact key should be stored under a dedicated prefix inside the shared bucket:

- `preview-artifacts/pptx/<resource-id>/<fingerprint>.pdf`
- `preview-artifacts/docx/<resource-id>/<fingerprint>.pdf`
- `preview-artifacts/zip/<resource-id>/<fingerprint>.json`

The storage implementation should normalize separators and guard against malformed keys escaping the configured prefix shape.

### 8.3 Cache Invalidation

Phase T intentionally preserves the existing fingerprint strategy:

- `storageKey`
- `updatedAt`
- `fileSize`

If any of these change, the artifact key changes automatically and the next preview request regenerates a new artifact in the selected backend.

## 9. Data Flow

### 9.1 Preview Read Path

For `PPTX`, `DOCX`, and `ZIP` preview requests:

1. controller and service visibility checks remain unchanged
2. `ResourcePreviewService` computes the fingerprinted logical artifact key
3. `ResourcePreviewArtifactStorage.exists(...)` checks the currently selected backend
4. if the artifact exists, `open(...)` returns the cached artifact
5. if the artifact does not exist, generation proceeds through the existing Phase H / Phase S path
6. the generated artifact is written through `write(...)` to the selected backend

### 9.2 Local-To-MinIO Cutover Behavior

When preview storage type changes from `local` to `minio`:

1. the logical artifact key remains the same
2. the storage backend lookup changes
3. old local artifacts are not read
4. the first preview miss regenerates and writes the artifact into MinIO

This keeps behavior simple and deterministic.

### 9.3 Raw File Dependency

Phase T does not change how raw preview source files are read.

Preview generation still opens the source resource through the selected `ResourceFileStorage` backend, which may itself be local or MinIO depending on Phase I configuration.

## 10. Error Handling

### 10.1 Startup Failures

When `app.resource-preview.type=minio`, the backend should fail fast at startup if:

- `platform.integrations.minio.enabled=false`
- MinIO client wiring is unavailable
- bucket existence cannot be checked
- bucket creation is required but fails

The runtime should not silently fall back to local preview storage when MinIO preview storage is explicitly selected.

### 10.2 Runtime Failures

Runtime preview-artifact storage failures should preserve current preview semantics as closely as possible:

- artifact read/write failure during `PPTX` preview -> existing `pptx preview unavailable`
- artifact read/write failure during `DOCX` preview -> existing `docx preview unavailable`
- artifact read/write failure during `ZIP` preview -> existing `zip preview unavailable`

These failures must not affect:

- resource detail access
- resource download
- preview authorization rules
- successful preview behavior for other already-cached artifacts in healthy environments

### 10.3 Missing Historical Local Artifact

A missing old local artifact after switching to MinIO preview storage is treated as a normal cache miss, not as an operational error.

That miss should trigger regeneration through the current backend preview pipeline.

## 11. Testing Strategy

### 11.1 Configuration Safety

Add focused configuration coverage proving that:

- `app.resource-preview.type=local` creates the local artifact storage
- `app.resource-preview.type=minio` with `MINIO_ENABLED=true` creates the MinIO artifact storage
- `app.resource-preview.type=minio` with `MINIO_ENABLED=false` fails fast
- local preview storage can still coexist with enabled shared MinIO integration when preview type is `local`

### 11.2 Storage-Level Verification

Add focused backend tests for the MinIO preview artifact storage covering:

- bucket initialization behavior
- artifact write
- artifact exists
- artifact open
- key normalization for backslash-separated keys
- object-key prefix mapping under the configured preview prefix

Repository-safe tests should use a fake in-memory `MinioObjectOperations` rather than requiring a live MinIO service in `mvn test`.

### 11.3 Service Regression

Keep `ResourcePreviewService` regression coverage proving that:

- caching semantics stay unchanged across storage backends
- `PPTX`, `DOCX`, and `ZIP` preview generation still reuse cached artifacts
- the service does not need a contract change to support MinIO-backed preview artifacts

### 11.4 Controller Regression

Targeted controller verification should confirm that:

- preview HTTP routes and response shapes remain unchanged
- preview permissions remain unchanged
- switching artifact storage backend does not change `previewKind` or content types

## 12. Deployment And Ops

### 12.1 Runtime Shape

For production-style or Compose-style MinIO runtimes:

- raw resource storage may be `local` or `minio`
- preview artifact storage may independently be `local` or `minio`
- the shared MinIO bucket should contain both raw resource objects and preview artifacts, separated by logical key namespaces

### 12.2 Persistence Layout

When preview artifacts are MinIO-backed:

- MinIO stores raw resource objects and preview artifacts in the same bucket
- preview artifacts remain isolated by the configured `minio-prefix`
- backend local disk no longer needs to be authoritative for preview artifacts in that runtime

When preview artifacts remain local:

- the existing preview-artifact filesystem layout remains valid

### 12.3 Operational Notes

README and deployment notes should explicitly document:

- the new `RESOURCE_PREVIEW_TYPE` switch
- the new `RESOURCE_PREVIEW_MINIO_PREFIX` setting
- the fact that historical local preview artifacts are not migrated
- the fact that old local artifacts are not dual-read after a MinIO cutover
- the fact that preview-artifact garbage collection is still outside this phase

## 13. Acceptance Criteria

This design is complete when the implementation can demonstrate:

1. `local` preview storage still behaves exactly as the current local closed loop.
2. `minio` preview storage writes newly generated `PPTX`, `DOCX`, and `ZIP` preview artifacts into the shared MinIO bucket under the configured preview prefix.
3. Repeated preview requests still reuse cached artifacts through the existing logical artifact keys.
4. Switching preview storage from local to MinIO does not require historical artifact migration for correctness; old local artifacts simply stop participating in cache hits.
5. Preview HTTP contracts, permissions, content types, and frontend preview behavior remain unchanged.
6. Explicitly selecting MinIO preview storage while MinIO integration is disabled fails fast instead of silently falling back to local storage.
