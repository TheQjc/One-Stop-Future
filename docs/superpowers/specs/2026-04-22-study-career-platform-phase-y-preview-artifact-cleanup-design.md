# Study Career Platform Phase Y Preview Artifact Cleanup Design

## 1. Goal

Phase Y adds a narrow automatic cleanup slice for stale preview artifacts while preserving the current Spring Boot monolith, Vue SPA, preview HTTP contracts, database schema, and the Phase T / W / X preview-storage rollout model.

The goal is to stop avoidable preview-artifact buildup when a resource's derived preview is no longer valid, without turning preview cleanup into a background scanning system, a user-facing workflow, or a risky rewrite of the existing preview key model.

This phase delivers a constrained runtime behavior:

- cleanup is backend-internal and automatic
- cleanup targets only preview artifacts whose keys can be derived exactly from known resource state
- cleanup runs after resource mutations that invalidate an old preview artifact
- cleanup stays best-effort and does not decide whether the main business mutation succeeds
- Phase T MinIO preview storage, Phase W historical migration, and Phase X runtime fallback remain compatible

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- automatic cleanup should cover stale preview artifacts created by rejected-resource resubmission flows
- cleanup should remove stale active artifacts and stale historical local fallback copies when they can be identified exactly
- cleanup should not require any new frontend control, admin cleanup API, or scheduled job
- cleanup should remain precise rather than recursive; no directory scans and no guessed historical fingerprints
- cleanup failures should be logged and ignored rather than surfaced as business failures
- non-preview business actions should stop creating unnecessary preview-key churn through unrelated `updatedAt` writes
- the current preview fingerprint formula should remain unchanged in this phase to avoid invalidating existing migrated or fallback-readable artifacts all at once

## 3. Non-Goals

This phase does not implement:

- recursive filesystem cleanup of preview roots
- MinIO bucket scans or object lifecycle policies
- background jobs, schedulers, startup repair tasks, or periodic sweepers
- fallback-hit write-back into MinIO
- preview cleanup endpoints for admins or end users
- database fields for preview lifecycle state, cleanup markers, or tombstones
- changes to preview URLs, preview payloads, or preview permissions
- deletion of historical fingerprint artifacts that cannot be derived from current resource state
- a direct rewrite of preview fingerprint rules away from `storageKey + updatedAt + fileSize`

## 4. Chosen Approach

### 4.1 Recommendation

Keep the existing preview-artifact key model in place, but narrow the set of mutations that change `ResourceItem.updatedAt`, then add a focused cleanup service that deletes only the exact stale preview keys derived from known before-and-after resource states.

Recommended high-level shape:

- keep `ResourcePreviewService` as the source of truth for preview-artifact targets
- extend `ResourcePreviewArtifactStorage` with exact-key deletion
- add one read-only local cleaner for historical fallback artifacts
- add one orchestration service that compares old and new preview targets and deletes only stale keys
- invoke that orchestration only from existing resource lifecycle paths that truly invalidate preview output

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the storage abstraction already used by Phases T and X
- it reuses the existing preview-artifact key derivation in `ResourcePreviewService`
- it avoids a high-risk cold-cache event that would happen if Phase Y rewrote fingerprint semantics immediately
- it removes a major source of avoidable preview churn by reducing unrelated `updatedAt` writes
- it keeps cleanup logic in the backend service layer where resource lifecycle mutations already live

### 4.3 Rejected Alternatives

#### Rewrite Preview Fingerprints Immediately

Rejected for this phase because it would:

- invalidate already migrated MinIO preview artifacts from Phase W
- invalidate historical local artifacts currently readable through Phase X fallback
- create a broad cold-cache event during a cleanup-focused slice
- mix lifecycle cleanup with a larger storage-key migration problem

#### Periodic Scanner Or Sweeper Job

Rejected because it would:

- require directory or bucket scans that go beyond exact-key cleanup
- increase operational complexity and mis-deletion risk
- blur a narrow lifecycle cleanup slice into an infrastructure-management project

#### Cleanup Inside Preview Read Requests

Rejected because it would:

- add mutation side effects to normal preview reads
- make request latency and failure semantics harder to reason about
- conflict with the current read-only compatibility intent of Phase X fallback

## 5. Functional Scope

### 5.1 Mutation Paths In Scope

Phase Y should clean stale preview artifacts when an existing resource mutation makes the previous derived preview invalid.

In the current codebase, the primary in-scope mutation path is:

- `ResourceService.updateRejectedResource(...)`

That path may:

- replace the raw file with a new storage key
- change the resource file type from one previewable derived type to another
- change a previously previewable derived type into a non-derived type
- keep the same file but still create a new preview key because `updatedAt` changes on edit

The cleanup design should be reusable for future delete or replacement paths, but those paths are not introduced in this phase.

### 5.2 Mutation Paths Explicitly Out Of Scope

Phase Y does not add cleanup behavior to status-only or interaction-only operations such as:

- `publish`
- `reject`
- `offline`
- `download`
- `favorite`
- `unfavorite`

These operations should also stop updating `ResourceItem.updatedAt` in this phase so they no longer create avoidable preview cache churn.

### 5.3 Cleanup Targets

Cleanup applies only to derived preview artifacts that already participate in preview-artifact storage:

- `PPTX -> PDF`
- `DOCX -> PDF`
- `ZIP -> JSON`

It does not apply to direct raw-file preview types such as:

- `PDF`

## 6. Preview Key Stability Adjustment

### 6.1 Current Constraint

The current preview fingerprint uses:

- `storageKey`
- `updatedAt`
- `fileSize`

Because several non-preview business operations currently write `updatedAt`, they can create new preview artifact keys even when the preview content is unchanged.

### 6.2 Phase Y Decision

Phase Y keeps the fingerprint formula unchanged, but narrows `updatedAt` writes so that non-preview business actions do not create unnecessary preview-key churn.

Recommended `updatedAt` behavior after Phase Y:

- keep `updatedAt` changes on true edit paths such as rejected-resource resubmission
- stop updating `updatedAt` for `download`
- stop updating `updatedAt` for `favorite`
- stop updating `updatedAt` for `unfavorite`
- stop updating `updatedAt` for `publish`
- stop updating `updatedAt` for `reject`
- stop updating `updatedAt` for `offline`

### 6.3 Accepted Residual Behavior

If a rejected resource is edited without replacing the file, the preview key may still change because the edit path updates `updatedAt`.

This residual behavior is accepted in Phase Y because:

- it affects a true edit path rather than passive interactions
- the new cleanup flow removes the now-stale old artifact instead of letting stale artifacts accumulate
- it preserves compatibility with previously generated Phase T / W / X artifacts

## 7. Architecture

### 7.1 Existing Components Reused

- `ResourcePreviewArtifactStorage`
- `ResourcePreviewService`
- `ResourceService`
- `LocalPreviewArtifactPathResolver`
- `HistoricalLocalResourcePreviewArtifactReader`
- `FallbackResourcePreviewArtifactStorage`
- `LocalResourcePreviewArtifactStorage`
- `MinioResourcePreviewArtifactStorage`
- existing preview configuration in `ResourcePreviewProperties`

### 7.2 New Or Changed Backend Units

- `ResourcePreviewArtifactStorage`
  - gains `delete(String artifactKey)`
- `LocalResourcePreviewArtifactStorage`
  - supports exact-key delete
- `MinioResourcePreviewArtifactStorage`
  - supports exact-key delete
- `FallbackResourcePreviewArtifactStorage`
  - delegates delete only to the primary active storage
- `HistoricalLocalResourcePreviewArtifactCleaner`
  - deletes a historical local artifact by exact logical key using the existing safe path resolver
- `PreviewArtifactCleanupService`
  - compares old and new preview targets and runs best-effort cleanup against the appropriate physical backends

### 7.3 Responsibility Boundaries

- `ResourcePreviewService` remains the source of truth for preview-artifact target derivation
- active preview storage remains responsible only for active-backend object operations
- historical local cleanup remains outside the active storage abstraction
- `PreviewArtifactCleanupService` owns stale-key comparison and orchestration
- `ResourceService` owns deciding when a resource lifecycle mutation should invoke cleanup

## 8. Storage And Cleanup Semantics

### 8.1 Exact-Key-Only Rule

Phase Y cleanup must delete only artifact keys that can be derived exactly from known resource state.

This means:

- no recursive directory walk
- no bucket scan
- no guessing of old fingerprints beyond the exact old resource snapshot already in memory
- no recovery attempt for unknown historical artifacts

### 8.2 Old Versus New Target Rules

The cleanup service should derive:

- `oldTarget` from the resource state before mutation
- `newTarget` from the resource state after mutation

Decision rules:

- if `oldTarget` is absent, skip cleanup
- if `oldTarget` and `newTarget` are equal, skip cleanup
- if `oldTarget` exists and `newTarget` is absent, delete the old artifact
- if `oldTarget` exists and `newTarget` differs, delete the old artifact

### 8.3 Active Backend Rules

When `app.resource-preview.type=local`:

- delete the old artifact from active local preview storage only

When `app.resource-preview.type=minio`:

- delete the old artifact from active MinIO preview storage
- also best-effort delete the same logical key under the historical local preview root

The local historical cleanup in MinIO mode should not depend on the Phase X fallback flag being enabled. A stale local fallback copy is still stale even when runtime fallback is currently disabled.

### 8.4 Missing Artifact Semantics

Delete operations should be idempotent:

- deleting a missing active artifact is treated as success
- deleting a missing historical local artifact is treated as success
- cleanup should not convert a not-found delete into a business failure

## 9. Runtime Data Flow

### 9.1 Rejected Resource Resubmission Flow

For `updateRejectedResource(...)`:

1. load the existing editable rejected resource
2. derive `oldTarget` from the pre-mutation resource state
3. apply metadata updates and optional raw-file replacement
4. persist the resource row
5. delete the replaced raw resource file if the storage key changed
6. derive `newTarget` from the updated resource state
7. invoke `PreviewArtifactCleanupService.cleanupAfterResourceMutation(oldTarget, newTarget)`
8. return the updated resource response regardless of cleanup outcome

### 9.2 Status And Interaction Flows

For `publish`, `reject`, `offline`, `download`, `favorite`, and `unfavorite`:

- do not invoke preview cleanup
- avoid writing `updatedAt` solely for those operations
- preserve existing business counters, reviewed timestamps, and published timestamps as appropriate

## 10. Error Handling

### 10.1 Cleanup Failure Behavior

Preview cleanup is strictly best-effort.

If deletion fails in either backend:

- log a `warn`
- continue the main business mutation
- do not throw a new `BusinessException`
- do not roll back the resource mutation solely because cleanup failed

### 10.2 Transaction Boundary

Cleanup should happen after the main resource mutation is persisted.

Phase Y should not try to make preview cleanup transactional across:

- database row updates
- raw resource file deletion
- active preview artifact deletion
- historical local preview artifact deletion

### 10.3 Read-Path Safety

Phase Y cleanup must not change read-path error semantics:

- preview reads still use the existing Phase T / X behavior
- preview generation still happens on cache miss
- MinIO failures are still not masked by local fallback

## 11. Testing Strategy

### 11.1 Storage-Level Tests

Extend or add tests covering:

- local preview storage delete of existing artifacts
- local preview storage delete of missing artifacts
- local preview storage delete with normalized backslash-separated keys
- MinIO preview storage delete against the configured object prefix
- fallback preview storage delete affecting only the primary storage
- historical local cleaner delete behavior and root-escape protection

### 11.2 Cleanup-Orchestration Tests

Add focused `PreviewArtifactCleanupService` tests covering:

- no old target -> skip
- same old and new target -> skip
- old target changed -> cleanup happens
- old target removed because preview support disappeared -> cleanup happens
- MinIO mode deletes active MinIO and historical local copies
- local mode deletes only active local storage
- delete failures are swallowed and logged without rethrowing

### 11.3 Service-Level Regressions

Extend service regression coverage to prove:

- rejected-resource resubmission triggers stale preview cleanup when the old key is invalidated
- status-only operations no longer create preview-key churn through unnecessary `updatedAt` changes
- existing preview generation and preview read flows remain unchanged

### 11.4 Controller Regression Safety

Keep controller regressions focused on ensuring:

- rejected-resource edit and resubmit behavior remains intact
- preview endpoints still serve generated or cached previews with the same contracts
- admin review operations continue to behave as before from the API consumer perspective

## 12. Deployment And Ops

### 12.1 Runtime Shape

Phase Y does not add any new environment variable, cron, queue, or operator workflow.

The phase remains fully backend-internal:

- local preview storage continues to work in the local profile
- MinIO preview storage continues to work in MinIO-enabled runtimes
- historical fallback compatibility from Phase X remains available

### 12.2 Operational Outcome

After Phase Y:

- passive interactions stop causing unnecessary preview key rotation
- rejected-resource edits no longer leave old derived preview artifacts behind indefinitely when those keys are known
- operators still use the existing Phase W migration endpoint for historical copy workflows
- operators do not gain a cleanup console or scan job in this phase

## 13. Documentation Follow-Up

Implementation should update the README to document:

- Phase Y preview artifact cleanup as implemented
- that cleanup is automatic and backend-internal
- that cleanup targets exact stale preview keys only
- that no scheduled garbage collector or directory scanner is introduced
- that preview-artifact migration and runtime fallback behavior from earlier phases remain available

## 14. Acceptance Criteria

1. Non-preview business operations such as `download`, `favorite`, `unfavorite`, `publish`, `reject`, and `offline` no longer create unnecessary preview key churn through unrelated `updatedAt` writes.
2. When a rejected resource edit invalidates a previously derived preview artifact, the backend performs best-effort cleanup for the exact stale logical key.
3. In local preview mode, cleanup deletes only the active local preview artifact.
4. In MinIO preview mode, cleanup deletes the active MinIO preview artifact and also best-effort deletes the same logical key from the historical local preview root.
5. Cleanup remains exact-key-only and does not scan directories, scan buckets, or guess unknown historical fingerprints.
6. Cleanup failures do not change existing business API contracts or cause the main resource mutation to fail.
7. Phase T MinIO preview storage, Phase W preview-artifact migration, and Phase X runtime dual-read fallback remain behaviorally compatible after the Phase Y change.
