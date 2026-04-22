# Study Career Platform Phase X Preview Artifact Runtime Dual-Read Fallback Design

## 1. Goal

Phase X adds a narrow runtime compatibility slice for preview-artifact reads after a preview-storage cutover to MinIO.

The goal is to preserve cache hits for historical local preview artifacts during normal preview requests while keeping the current Spring Boot monolith, Vue SPA, preview HTTP contracts, database schema, preview-artifact key rules, and Phase T / Phase W MinIO storage model intact.

This phase delivers a constrained runtime behavior:

- when preview storage type is `minio`, runtime preview reads may first check MinIO and then fall back to the historical local preview root
- the fallback applies only to existing derived preview artifacts for `PPTX`, `DOCX`, and `ZIP`
- newly generated preview artifacts still write only to MinIO
- local development keeps the current local-only preview loop by default

This phase intentionally avoids turning normal preview reads into an automatic migration, cleanup, or storage-healing workflow.

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- the dual-read behavior is runtime-only and applies only when preview storage type is `minio`
- read order is `MinIO first -> historical local fallback second`
- `local` preview storage mode does not reverse-read MinIO
- fallback hits on local artifacts do not automatically write back to MinIO
- the existing local preview root remains the only fallback directory
- MinIO infrastructure failures must not be hidden behind local fallback
- missing artifacts may still be regenerated through the existing preview-generation flow

## 3. Non-Goals

This phase does not implement:

- automatic migration of local preview artifacts into MinIO during runtime reads
- automatic write-back to MinIO after a successful local fallback hit
- automatic deletion or cleanup of local preview artifacts
- a second local fallback root or second preview-history directory setting
- background synchronization, scheduled cache warming, or startup-time repair jobs
- frontend contract changes or new preview endpoints
- database schema changes for preview lifecycle markers or migration state
- high-availability failover for MinIO outages
- changes to raw resource storage behavior

## 4. Chosen Approach

### 4.1 Recommendation

Keep `ResourcePreviewArtifactStorage` as the only preview-artifact storage boundary and add a composed storage implementation that:

- uses `MinioResourcePreviewArtifactStorage` as the primary active backend
- uses `HistoricalLocalResourcePreviewArtifactReader` as a read-only fallback source
- exposes the same storage contract to `ResourcePreviewService`

Recommended runtime selection rules:

- `app.resource-preview.type=local` -> `LocalResourcePreviewArtifactStorage`
- `app.resource-preview.type=minio` and fallback disabled -> `MinioResourcePreviewArtifactStorage`
- `app.resource-preview.type=minio` and fallback enabled -> composed dual-read storage

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the storage abstraction introduced in Phases H and T
- it keeps `ResourcePreviewService` focused on artifact-key derivation, cache lookup, and generation
- it reuses the local preview-path safety work added in Phase W
- it adds the smallest possible behavior change to the runtime preview path
- it keeps rollout explicit and reversible through configuration

### 4.3 Rejected Alternatives

#### Put Dual-Read Logic Directly In `ResourcePreviewService`

Rejected because it would:

- make the service aware of storage-backend composition details
- mix preview-generation flow with storage-fallback policy
- weaken the clean storage boundary already present in the codebase

#### Read Local First And Then MinIO

Rejected because it would:

- keep the new MinIO backend from being authoritative after cutover
- make old local artifacts shadow newer MinIO objects
- blur the operational meaning of a completed preview-storage migration

#### Auto-Write Back To MinIO On Local Fallback Hit

Rejected for this phase because it would:

- make a normal read request mutate storage state
- add latency and partial-failure complexity to the user-facing preview path
- turn a runtime compatibility slice into a background migration surrogate

## 5. Functional Scope

### 5.1 Activation Rules

Phase X introduces one narrow configuration switch:

- `app.resource-preview.read-fallback-local-enabled`

Recommended default:

- `false`

Behavior rules:

- fallback may be active only when `app.resource-preview.type=minio`
- when preview storage type is `local`, the new flag is ignored
- when preview storage type is `minio` and the flag is `false`, runtime behavior remains the current Phase T behavior
- when preview storage type is `minio` and the flag is `true`, runtime reads may use local fallback on MinIO cache miss

### 5.2 Supported Preview Types

Dual-read fallback applies only to derived preview artifacts that already participate in preview-artifact storage:

- `PPTX -> PDF`
- `DOCX -> PDF`
- `ZIP -> JSON`

It does not apply to direct raw-file previews such as:

- `PDF`

### 5.3 Read Behavior

When dual-read fallback is active:

1. derive the logical artifact key through `ResourcePreviewService`
2. attempt to read the artifact from MinIO-backed preview storage
3. if the artifact is clearly missing in MinIO, attempt to read it from the historical local preview root
4. if both backends miss, continue with the current preview-generation path
5. write any newly generated artifact only to MinIO

The fallback is read-only:

- it does not copy the local artifact into MinIO
- it does not delete the local artifact
- it does not mutate resource metadata

### 5.4 Local-Mode Behavior

When `app.resource-preview.type=local`:

- runtime preview reads continue using local preview storage only
- the new fallback flag must not cause a reverse lookup into MinIO
- current local development behavior remains unchanged

### 5.5 Regeneration Boundary

This phase preserves the current preview-generation behavior:

- if no preview artifact exists in either backend, runtime generation may still happen
- successful generation stores the new artifact in the active MinIO backend
- old local artifacts remain untouched

This allows the system to recover cache misses naturally without introducing a write-back side effect on fallback hits.

## 6. Architecture

### 6.1 Existing Components Reused

- `ResourcePreviewArtifactStorage`
- `ResourcePreviewService`
- `ResourceService`
- `ResourceController`
- `LocalPreviewArtifactPathResolver`
- `HistoricalLocalResourcePreviewArtifactReader`
- `MinioResourcePreviewArtifactStorage`
- `MinioObjectOperations`
- existing preview key derivation in `ResourcePreviewService`

### 6.2 New Or Changed Backend Units

- `ResourcePreviewProperties`
  - gains `readFallbackLocalEnabled`
- composed preview storage implementation
  - owns `MinIO first -> local fallback` behavior behind the existing storage contract
- `SdkMinioObjectOperations`
  - should distinguish object-not-found from general MinIO I/O failure for safe fallback decisions
- `ResourcePreviewService`
  - should use a single open-first cache-read flow that can distinguish `not found` from infrastructure failure

### 6.3 Responsibility Boundaries

- `ResourcePreviewService` remains the owner of preview-artifact keys, preview generation, and preview result shaping
- the composed preview storage owns fallback order and read semantics
- `HistoricalLocalResourcePreviewArtifactReader` remains a read-only local artifact source
- `MinioResourcePreviewArtifactStorage` remains the authoritative write target when preview storage type is `minio`
- controllers and HTTP response contracts remain unchanged

## 7. Configuration Design

### 7.1 Preview Settings

Phase X keeps the current preview settings:

- `app.resource-preview.type`
- `app.resource-preview.local-root`
- `app.resource-preview.minio-prefix`
- `app.resource-preview.docx.soffice-command`

And adds:

- `app.resource-preview.read-fallback-local-enabled`

Recommended defaults:

- `app.resource-preview.read-fallback-local-enabled=${RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED:false}`

### 7.2 Fallback Source

The historical local fallback source remains:

- `app.resource-preview.local-root`

This phase should not add a separate `historical-local-root` setting because:

- it would increase rollout complexity
- the current operator workflow already assumes the old preview root remains available during cutover
- the Phase W migration flow already uses the same root safely

### 7.3 Configuration Validation

Selection rules should remain explicit:

- local preview type should continue to instantiate only `LocalResourcePreviewArtifactStorage`
- MinIO preview type with fallback disabled should continue to instantiate only `MinioResourcePreviewArtifactStorage`
- MinIO preview type with fallback enabled should instantiate the composed storage and require MinIO integration to be enabled

This phase should not silently degrade from MinIO mode to local-only mode.

## 8. Artifact And Storage Semantics

### 8.1 Logical Artifact Keys

Phase X must preserve the existing logical artifact key rules from `ResourcePreviewService`:

- `pptx/<resource-id>/<fingerprint>.pdf`
- `docx/<resource-id>/<fingerprint>.pdf`
- `zip/<resource-id>/<fingerprint>.json`

The fallback flow must not invent secondary key shapes, directory scans, or stale-fingerprint recovery.

### 8.2 Active Object Mapping

When preview storage type is `minio`, the active MinIO object key remains:

- `<preview-minio-prefix>/<logical-artifact-key>`

The local fallback continues resolving the same logical key under:

- `app.resource-preview.local-root/<logical-artifact-key>`

That means both backends refer to the same logical artifact identity, only with different physical storage backends.

### 8.3 Authoritative Write Rules

When preview storage type is `minio`:

- all newly generated preview artifacts write only to MinIO
- runtime fallback hits on local artifacts do not write to MinIO
- runtime fallback hits do not update timestamps or business data

## 9. Runtime Data Flow

### 9.1 Preview Request Flow

For `PPTX`, `DOCX`, and `ZIP` preview requests in MinIO mode with fallback enabled:

1. authorize the resource through the existing `ResourceService` rules
2. derive the logical preview artifact key
3. attempt to open the MinIO artifact
4. if MinIO reports object-not-found, attempt to open the local historical artifact
5. if both backends miss, open the raw resource file and generate the preview artifact
6. write the generated artifact to MinIO
7. return the preview content through the existing controller contract

### 9.2 Cache-Read Strategy

`ResourcePreviewService` should prefer an open-first flow over `exists`-then-`open`:

- try to open the cached artifact directly
- on explicit `not found`, treat it as a cache miss
- on any other `IOException`, treat it as an infrastructure failure

This reduces duplicate backend round-trips and avoids race conditions where `exists` succeeds but `open` fails because the object disappears between calls.

### 9.3 Fallback Conditions

Local fallback should happen only when the primary MinIO read reports a clear cache miss.

Fallback should not happen when:

- MinIO is unreachable
- bucket access fails
- credentials are invalid
- the SDK returns a non-not-found object access error

## 10. Error Handling

### 10.1 Not-Found Versus Failure

This phase requires an explicit distinction between:

- artifact not found
- artifact read failed

Recommended implementation rule:

- map MinIO `NoSuchKey` / `NoSuchObject` responses to `FileNotFoundException`
- keep other MinIO problems as general `IOException`

This allows the storage layer and preview service to decide safely when fallback or regeneration is appropriate.

### 10.2 Runtime Read Semantics

Recommended storage semantics:

- `open(key)`
  - return MinIO stream when present
  - on MinIO `FileNotFoundException`, try local fallback
  - on local `FileNotFoundException`, surface a cache miss
  - on other I/O failures, surface an error
- `exists(key)`
  - may remain for compatibility, but should obey the same miss-versus-failure distinction
- `write(key, stream)`
  - writes only to MinIO

### 10.3 User-Facing Behavior

This phase should preserve current user-facing error behavior:

- missing cache artifacts should still lead to generation when generation is possible
- infrastructure problems should still surface as the existing preview-unavailable business failures
- dual-read fallback must not cause preview requests to succeed when the active MinIO backend is actually broken

## 11. Testing Strategy

### 11.1 Storage-Level Tests

Add focused tests covering:

- MinIO hit returns the MinIO artifact without consulting local fallback
- MinIO object-not-found falls back to local and returns the local artifact
- both backends missing surface a cache miss rather than an infrastructure failure
- MinIO infrastructure failure does not fall back to local
- write operations always target MinIO only
- local fallback continues to normalize backslash-separated keys and reject root escape attempts

### 11.2 Service-Level Tests

Add focused `ResourcePreviewService` tests covering:

- `PPTX` preview opens an artifact through the combined storage before generating
- `DOCX` preview opens an artifact through the combined storage before generating
- `ZIP` preview opens cached JSON through the combined storage before generating
- explicit cache miss still regenerates and writes to MinIO
- non-not-found storage failure still becomes the existing preview-unavailable `BusinessException`

### 11.3 Configuration Tests

Extend `ResourcePreviewStorageConfigurationTests` to cover:

- local preview type ignores the fallback flag and still creates local storage only
- MinIO preview type without fallback creates MinIO storage only
- MinIO preview type with fallback creates the composed dual-read storage
- MinIO preview type with fallback still fails fast when MinIO integration is disabled

### 11.4 Controller Regression Tests

Keep controller-level coverage narrow but meaningful:

- preview endpoints still return `200` for visible resources whose artifacts exist only in local historical storage while MinIO mode with fallback is enabled
- preview endpoints still return the existing `500`-style business failure when the active MinIO backend is broken
- public and owner visibility rules remain unchanged

## 12. Deployment And Ops

### 12.1 Rollout Requirements

Runtime dual-read fallback requires:

- `app.resource-preview.type=minio`
- MinIO integration enabled and reachable
- the historical local preview root still mounted or otherwise accessible to the backend
- `app.resource-preview.read-fallback-local-enabled=true`

### 12.2 Recommended Operator Flow

Recommended operator flow:

1. migrate or preserve the historical local preview directory so the backend can still read it
2. switch preview storage type to `minio`
3. enable `read-fallback-local-enabled`
4. allow runtime reads to hit MinIO first and local history second
5. optionally run the Phase W admin migration endpoint to copy historical artifacts into MinIO
6. once local fallback is no longer needed, disable the fallback flag and retire the old local preview directory

### 12.3 Rollback Guidance

Rollback remains straightforward:

- disabling the fallback flag restores the current Phase T MinIO-only runtime behavior
- switching preview storage type back to `local` restores the local-only runtime path
- no database cleanup is required because this phase does not persist fallback state

## 13. Documentation Follow-Up

Implementation should update the README to document:

- the new runtime fallback flag
- the fact that fallback works only in MinIO preview mode
- the read order of `MinIO first -> local historical fallback`
- the fact that fallback hits do not automatically write back to MinIO
- the fact that MinIO failures are not masked by local fallback
- the continued availability of the Phase W admin migration endpoint as the formal copy path

## 14. Acceptance Criteria

1. When preview storage type is `minio` and local fallback is disabled, runtime preview behavior remains unchanged from Phase T.
2. When preview storage type is `minio` and local fallback is enabled, runtime preview reads can serve historical `PPTX`, `DOCX`, and `ZIP` preview artifacts from the existing local preview root when the MinIO object is missing.
3. Fallback uses the same logical artifact key shape already produced by `ResourcePreviewService` and does not scan or guess stale artifact keys.
4. Local preview mode does not reverse-read MinIO.
5. Newly generated preview artifacts continue to write only to MinIO when preview storage type is `minio`.
6. Successful local fallback hits do not automatically copy or delete artifacts.
7. MinIO infrastructure failures continue to surface as preview failures rather than being hidden behind local fallback.
