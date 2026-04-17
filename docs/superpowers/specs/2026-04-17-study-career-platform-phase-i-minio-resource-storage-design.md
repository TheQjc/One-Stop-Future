# Study-Career Platform Phase I MinIO Resource Storage Design

## 1. Goal

Phase I introduces MinIO-backed raw resource file storage for non-`local` environments while preserving the current Spring Boot monolith, Vue SPA, resource HTTP contracts, database schema, and Phase H preview behavior.

This phase delivers a narrow infrastructure slice:

- uploaded and replaced raw resource files can be stored in MinIO
- download and preview flows continue to read raw files through the existing backend permission model
- `local` development keeps using local filesystem storage
- preview artifacts for `PPTX -> PDF` and `ZIP -> JSON` remain on local disk in this phase

The design intentionally avoids turning this work into a broader storage migration or preview-system rewrite.

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- only raw resource files move to MinIO
- preview cache remains on local disk
- backend remains the only entry point for resource download and preview
- `storageKey` remains a logical object key stored in the database
- `local` profile must remain fully local and keep the current closed loop
- historical local-file migration is out of scope

## 3. Non-Goals

This phase does not implement:

- MinIO-backed preview artifact storage
- direct browser upload to MinIO
- direct browser download from MinIO
- presigned upload or download URLs
- CDN integration
- automatic migration of existing local resource files into MinIO
- dual-read fallback that tries local disk and MinIO in the same runtime path
- database schema changes for bucket, endpoint, or object URL fields
- object versioning, retention, or lifecycle policies
- chunk upload, batch upload, or resume upload

## 4. Chosen Approach

### 4.1 Recommendation

Keep the existing `ResourceFileStorage` boundary and add a second implementation, `MinioResourceFileStorage`, selected by configuration.

The current database `storageKey` field remains the only persisted locator. Its meaning becomes "logical object key" rather than "local path", which works for both storage backends.

### 4.2 Why This Approach

This approach fits the current codebase best:

- it reuses the storage abstraction introduced in Phase D
- it keeps `ResourceService` and controller contracts stable
- it does not require frontend changes
- it isolates MinIO risk to one backend infrastructure boundary
- it keeps Phase H preview generation untouched except for reading raw files through the same storage abstraction

### 4.3 Rejected Alternatives

#### Move Raw Files and Preview Artifacts to MinIO Together

Rejected for this phase because it would:

- reopen the recently completed Phase H preview subsystem
- expand risk into preview-cache layout, invalidation, and artifact reads
- make storage cutover and preview regression debugging harder to separate

#### Add Dual-Read Local + MinIO Fallback

Rejected for this phase because it would:

- turn a clean storage switch into a migration system
- blur the meaning of `storageKey`
- make operational failures harder to reason about
- increase code complexity beyond the value of this slice

#### Use Presigned URLs and Let the Frontend Access MinIO Directly

Rejected for this phase because it would:

- bypass the current backend permission model
- require new signing, expiration, and object exposure rules
- force frontend and API contract changes that are unnecessary for the current product stage

## 5. Functional Scope

### 5.1 Local Development

When the backend runs with the `local` profile:

- raw resource files continue to use local filesystem storage
- preview artifacts continue to use local filesystem storage
- current README local run instructions remain valid
- current backend and frontend tests remain Docker-free by default

### 5.2 MinIO-Backed Runtime

When `app.resource-storage.type=minio`:

- newly uploaded resources store raw files in MinIO
- rejected-resource replacement uploads store the new raw file in MinIO
- download reads raw files from MinIO through the backend
- `PDF` preview reads raw files from MinIO through the backend
- `PPTX` preview reads the raw file from MinIO, then keeps generated preview artifacts on local disk
- `ZIP` preview reads the raw file from MinIO, then keeps generated preview artifacts on local disk

### 5.3 Historical Data Boundary

This phase does not migrate existing local files.

That means:

- a database row whose `storageKey` points to a file that only exists on local disk will not become readable simply by switching the runtime to MinIO
- clean MinIO environments work for new uploads and replacements created under MinIO
- environments reusing an old database need a separate offline migration or manual re-upload path before enabling MinIO storage

## 6. Architecture

### 6.1 Existing Components Reused

- `ResourceFileStorage`
- `ResourceService`
- `ResourceController`
- `ResourcePreviewService`
- `LocalResourcePreviewArtifactStorage`
- existing resource database model and `storageKey` field
- existing frontend resource upload, detail, preview, and admin flows

### 6.2 New or Changed Backend Units

- `MinioIntegrationProperties`
  - binds `platform.integrations.minio`
  - validates MinIO enablement and connection settings when MinIO storage is selected
- `MinioResourceFileStorage`
  - stores, opens, deletes, and checks raw resource objects in MinIO
- shared storage-key generator or helper
  - generates the existing `yyyy/MM/dd/<uuid>.<ext>` format for both local and MinIO storage implementations
- conditional storage bean selection
  - activates exactly one raw resource storage implementation based on `app.resource-storage.type`

### 6.3 Storage Boundary Contract

The storage abstraction should remain responsible only for raw object operations:

- `store`
- `open`
- `delete`
- `exists`

No HTTP permission logic, preview-artifact logic, or controller formatting should move into the storage layer.

## 7. Configuration Design

### 7.1 Raw Resource Storage Switch

Continue using:

- `app.resource-storage.type`
- `app.resource-storage.local-root`

Supported values for `type` in this phase:

- `local`
- `minio`

### 7.2 MinIO Integration Settings

Reuse the existing `platform.integrations.minio` block from `application.yml`:

- `enabled`
- `endpoint`
- `access-key`
- `secret-key`
- `bucket`

This phase should not introduce a second MinIO configuration prefix.

### 7.3 Local Profile Rules

`application-local.yml` remains authoritative for the local closed loop:

- `app.resource-storage.type=local`
- `platform.integrations.minio.enabled=false`
- preview cache remains under `.local-storage/previews`

### 7.4 Compose Runtime Rules

`docker-compose.yml` should be extended with:

- a `minio` service
- persistent volume for MinIO data
- backend environment variables that explicitly switch raw resource storage to MinIO

The backend container should still keep a writable local volume for preview artifacts because preview cache remains local in this phase.

## 8. Object Key Design

### 8.1 `storageKey` Semantics

`storageKey` should be treated as a logical object key, not an absolute path or full URL.

The database should continue to store values shaped like:

- `2026/04/17/550e8400-e29b-41d4-a716-446655440000.pdf`

The application must not persist:

- MinIO endpoint
- bucket-qualified URLs
- signed URLs
- local absolute filesystem paths

### 8.2 Key Generation

The key format should stay aligned with the current local implementation:

- date prefix: `yyyy/MM/dd`
- random UUID body
- original extension preserved

Both local and MinIO storage implementations should share the same key-generation rule so storage backend changes do not change key shape.

## 9. Data Flow

### 9.1 Upload

For new resource uploads:

1. `ResourceService` validates request metadata and file type.
2. `ResourceService` calls `resourceFileStorage.store(...)`.
3. The selected storage backend writes the raw file and returns a `storageKey`.
4. The resource row stores that `storageKey`.

### 9.2 Rejected Resource Replacement

For rejected-resource resubmission with file replacement:

1. validate owner and `REJECTED` state
2. store the new raw object first
3. update the database row to the new `storageKey`
4. attempt to delete the old object on a best-effort basis

This preserves the current safety property: a replacement upload should not delete the old file before the new one is durable.

### 9.3 Download and Preview

For download and raw-file preview access:

1. controller and service permission checks stay unchanged
2. `ResourceService` resolves the resource row and `storageKey`
3. `resourceFileStorage.open(storageKey)` reads the raw file from the selected backend
4. the existing download or preview response pipeline continues unchanged

Phase H preview generation remains layered on top of this raw-file read.

## 10. Error Handling

### 10.1 Startup Failures

When `app.resource-storage.type=minio`, the backend should fail fast at startup if:

- `platform.integrations.minio.enabled=false`
- endpoint, access key, secret key, or bucket is blank
- MinIO client initialization fails
- bucket existence cannot be checked
- bucket creation is required but fails

The runtime should not silently fall back to local storage when MinIO is explicitly selected.

### 10.2 Runtime Failures

Runtime MinIO errors should preserve current business semantics as closely as possible:

- upload failure -> `500 failed to store resource file`
- open/read failure -> `500 resource file unavailable`
- existence check I/O failure -> treated as infrastructure failure, not as "object missing"
- best-effort delete failure after replacement -> warn in logs, do not fail the already successful resubmission

### 10.3 `exists(...)` Semantics

`exists(...)` should be treated as a real I/O operation, not as a guaranteed local boolean check.

The contract should distinguish:

- object does not exist
- storage backend could not be reached or queried

The exact Java signature can be decided in implementation, but the service layer must not conflate remote failure with normal absence.

## 11. Testing Strategy

### 11.1 Configuration Safety

Extend configuration safety coverage so it remains explicit that:

- `local` profile still uses local raw storage
- test configuration still keeps MinIO disabled by default
- MinIO-enabled runtime paths require explicit configuration

### 11.2 Storage-Level Verification

Add focused backend tests for the MinIO storage implementation covering:

- object store
- object open
- object exists
- object delete
- bucket initialization behavior

Automated test coverage for this repository should remain practical for non-Docker environments; if real MinIO integration cannot be made mandatory in `mvn test`, use repository-safe tests for the storage boundary and reserve live MinIO wiring for compose smoke verification.

### 11.3 Service Regression

Existing resource behavior must remain green:

- upload
- rejected edit and resubmit
- download
- `PDF` preview
- `PPTX` preview
- `ZIP` preview

No frontend contract change is expected in this phase.

## 12. Deployment And Ops

### 12.1 Compose Shape

The compose stack for this phase should become:

- `mysql`
- `minio`
- `backend`
- `frontend`

`backend` should use MinIO for raw resource files in this stack.

### 12.2 Persistence Layout

Persistence should stay split by responsibility:

- MySQL data in a database volume
- MinIO raw resource objects in a MinIO data volume
- backend local preview artifacts in the existing backend local-storage volume

This keeps original files and derived preview artifacts physically separate.

### 12.3 Operational Notes

README and deployment notes should explicitly document:

- local development remains filesystem-based
- Compose or production-style environments can enable MinIO-backed raw storage
- this phase does not migrate historical local resource files
- common failure points are bucket setup, endpoint reachability, access key / secret key mismatch, and backend preview-cache disk path

## 13. Acceptance Criteria

This design is complete when the implementation can demonstrate:

1. `local` profile still behaves exactly as the current local closed loop.
2. A MinIO-backed runtime can upload a new resource and persist its raw file in MinIO.
3. A MinIO-backed runtime can download that resource through the existing backend endpoint.
4. A MinIO-backed runtime can preview `PDF`, `PPTX`, and `ZIP` resources through the existing Phase H flow, with raw files read from MinIO and preview artifacts still written locally.
5. Rejected-resource replacement still updates `storageKey` safely and only logs old-object delete failures.
6. Switching an old local-file database directly to MinIO is documented as unsupported without separate migration work.
