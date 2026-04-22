# Study Career Platform Phase AA Application Snapshot Online Preview Design

## 1. Goal

Phase AA adds a narrow online-preview slice for immutable job-application resume snapshots while preserving the current Spring Boot monolith, Vue SPA, Phase N application workflow, Phase Z live-resume preview boundary, existing raw-file storage boundary, and the Phase T / X / Y preview-artifact runtime model.

The goal is to let applicants and admins preview already-submitted application resume snapshots inline from the two existing application boards without turning application management into a broader workflow rewrite, changing snapshot immutability semantics, or introducing a second preview-storage architecture.

This phase delivers one bounded runtime behavior:

- applicants can preview their own submitted application resume snapshots from `/profile/applications`
- admins can preview application resume snapshots from `/admin/applications`
- `PDF` snapshots preview inline directly from raw storage
- `DOCX` snapshots preview as generated cached PDF output
- `DOC` snapshots remain download-only in this phase
- snapshot preview reuses the existing preview-artifact storage boundary and caching model

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- this slice is `Phase AA application snapshot online preview first slice`
- preview scope covers only immutable application resume snapshots
- preview appears on:
  - `/profile/applications`
  - `/admin/applications`
- `/profile/resumes` keeps its existing Phase Z behavior and semantics
- `PDF` snapshots should preview inline directly from raw storage
- `DOCX` snapshots should preview as generated PDF output
- `DOC` snapshots remain download-only in this phase
- user and admin lists should both expose preview metadata through the backend contract
- the applicant-facing application page should also gain snapshot download so `DOC` remains usable
- generated snapshot previews should reuse the existing preview-artifact storage infrastructure rather than introducing a separate cache backend

## 3. Non-Goals

This phase does not implement:

- changes to live resume-library preview behavior on `/profile/resumes`
- public or shareable snapshot URLs
- application review actions, comments, notes, or status progression
- snapshot replacement, rename, version history, or deletion
- preview for legacy `DOC` snapshots
- a generic cross-domain preview framework for all private documents
- scheduled cleanup jobs, bucket scans, or recursive preview-root cleanup
- application-row-driven preview cleanup on status updates
- a rewrite of current preview infrastructure away from `DocxPreviewGenerator` plus `ResourcePreviewArtifactStorage`

## 4. Chosen Approach

### 4.1 Recommendation

Add one snapshot-specific preview service and reuse it from both the applicant and admin application services, while keeping permission checks and list shaping in the existing domain services.

Recommended high-level shape:

- keep `JobApplicationService` responsible for applicant ownership and applicant-facing list shaping
- keep `AdminJobApplicationService` responsible for admin visibility and admin-facing list shaping
- add `ApplicationSnapshotPreviewService` as the snapshot-domain source of truth for preview support, preview targets, and generated preview reads
- reuse `ResourcePreviewArtifactStorage` for generated `DOCX -> PDF` artifacts
- keep `PDF` snapshot preview as direct raw-file inline streaming with no derived artifact
- do not introduce delete-time cleanup, because application snapshots are immutable and this phase adds no snapshot-deletion flow

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the clear boundary between live resumes and immutable application snapshots
- it keeps applicant and admin permission rules in their existing services
- it gives both surfaces one shared preview rule set so behavior cannot drift
- it reuses already-proven preview generation and artifact storage infrastructure instead of duplicating it
- it keeps the phase small enough to land safely after Phase Z

### 4.3 Rejected Alternatives

#### Extend `ResumePreviewService` To Also Handle Snapshots

Rejected for this phase because it would:

- mix live-resume semantics with immutable application-snapshot semantics
- make the resume service name and responsibilities misleading
- increase future maintenance friction when live resumes and snapshots evolve differently

#### Put Preview Logic Directly Into `JobApplicationService` And `AdminJobApplicationService`

Rejected because it would:

- duplicate preview-kind mapping, artifact-key derivation, and conversion-failure handling
- make applicant and admin behavior more likely to diverge over time
- weaken the current service boundaries

#### Add `DOC` Preview In The Same Slice

Rejected because it would:

- broaden the conversion surface beyond the already-supported `DOCX` path
- increase runtime and verification risk without strong infrastructure reuse
- weaken the predictability of a narrow first slice

## 5. Functional Scope

### 5.1 Surfaces In Scope

Phase AA adds snapshot preview only to:

- `/profile/applications`
- `/admin/applications`

Applicant access remains ownership-scoped:

- an authenticated user may preview or download only snapshots belonging to their own applications

Admin access remains admin-scoped:

- an admin may preview or download snapshots for any application row visible in the current admin workbench

### 5.2 Supported Snapshot Types

Phase AA preview support is:

- `PDF -> inline raw-file preview`
- `DOCX -> generated PDF preview`

Preview is not supported in this phase for:

- `DOC`

`DOC` snapshots remain usable through download actions on both applicant and admin surfaces.

### 5.3 Snapshot Lifecycle Boundary

Application snapshots remain immutable in this phase:

- application submission still creates a raw-file snapshot once
- later application-row changes do not replace the snapshot file
- this phase does not add snapshot deletion
- this phase therefore does not introduce preview-artifact cleanup hooks

## 6. Preview Semantics

### 6.1 PDF Snapshot Preview

When the application snapshot is a `PDF`:

- no derived preview artifact is generated
- the backend opens the raw snapshot file directly
- the response is streamed inline with the stored snapshot content type

### 6.2 DOCX Snapshot Preview

When the application snapshot is a `DOCX`:

- the backend derives a logical preview-artifact key
- if a cached artifact exists, it is returned directly as `application/pdf`
- otherwise the backend opens the raw snapshot `DOCX`, generates a PDF through `DocxPreviewGenerator`, writes the artifact through `ResourcePreviewArtifactStorage`, and returns the generated PDF

### 6.3 Unsupported Snapshot Preview Types

When the application snapshot is a `DOC` or any unsupported type:

- the backend should reject preview with `400 application resume preview only supports pdf or docx`
- the frontend should not render a preview button for that row
- the frontend should still render `Download`

## 7. Preview Key Semantics

### 7.1 Preview Key Rule

For `DOCX` application snapshots, the derived preview key should be:

- `application/snapshot/docx/{applicationId}/{fingerprint}.pdf`

### 7.2 Fingerprint Rule

The fingerprint should be derived from snapshot-specific immutable fields rather than the mutable application row timestamp:

- `resumeStorageKeySnapshot`
- `submittedAt`
- `resumeFileSizeSnapshot`

This is recommended because it:

- keeps the cache key aligned with the immutable snapshot itself
- avoids rotating preview keys when unrelated application fields change later
- stays close to the existing preview fingerprint philosophy without depending on mutable row updates

### 7.3 Cleanup Rule

Phase AA does not add preview-artifact cleanup behavior.

Rationale:

- application snapshots are immutable
- this phase adds no snapshot delete endpoint
- application status changes should not invalidate a stable snapshot preview key

This means:

- no delete-time preview cleanup hook
- no cleanup on preview-read paths
- no scheduled preview cleanup job
- no bucket scan or recursive cleanup

## 8. Architecture

### 8.1 Existing Components Reused

- `JobApplicationService`
- `AdminJobApplicationService`
- `JobApplication`
- `JobApplicationMapper`
- `ResourceFileStorage`
- `ResourcePreviewArtifactStorage`
- `DocxPreviewGenerator`
- existing preview-storage configuration in `ResourcePreviewProperties`

### 8.2 New Or Changed Backend Units

- `ApplicationSnapshotPreviewService`
  - new snapshot-domain preview service for preview support, target derivation, cache reads, and generated `DOCX` preview writes
- `JobApplicationController`
  - gains applicant-facing snapshot preview and snapshot download endpoints
- `JobApplicationService`
  - gains owner-scoped snapshot preview orchestration, owner-scoped snapshot download orchestration, and applicant-list preview metadata shaping
- `MyJobApplicationListResponse.ApplicationItem`
  - gains preview metadata for frontend rendering
- `AdminJobApplicationController`
  - gains admin snapshot preview endpoint
- `AdminJobApplicationService`
  - gains admin-scoped snapshot preview orchestration and admin-list preview metadata shaping
- `AdminJobApplicationListResponse.ApplicationItem`
  - gains preview metadata for frontend rendering

### 8.3 Responsibility Boundaries

- `JobApplicationService` remains responsible for applicant identity, ownership checks, and applicant-facing list shaping
- `AdminJobApplicationService` remains responsible for admin visibility and admin-facing list shaping
- `ApplicationSnapshotPreviewService` owns preview-support rules, artifact-key derivation, cache interaction, and generated preview reads
- `ResourceFileStorage` remains responsible only for raw snapshot files
- `ResourcePreviewArtifactStorage` remains responsible only for derived preview artifacts
- live resume preview behavior stays owned by `ResumePreviewService`, not by the snapshot-preview service

## 9. API Design

### 9.1 Applicant-Facing Endpoints

Recommended new applicant-facing endpoints:

```http
GET /api/applications/{id}/resume/preview
GET /api/applications/{id}/resume/download
```

Behavior:

- authentication required
- owner-scoped to the authenticated applicant
- preview returns `inline` content
- download returns `attachment`
- `PDF` preview keeps original content type
- `DOCX` preview returns `application/pdf`

### 9.2 Admin Endpoint

Recommended new admin endpoint:

```http
GET /api/admin/applications/{id}/resume/preview
```

Behavior:

- admin-only
- returns `inline` content
- `PDF` preview keeps original content type
- `DOCX` preview returns `application/pdf`

The existing admin endpoint remains unchanged:

```http
GET /api/admin/applications/{id}/resume/download
```

### 9.3 Applicant List Contract Change

`GET /api/applications/mine` should extend each application item with:

- `previewAvailable`
- `previewKind`

Recommended values:

- `previewAvailable=true`, `previewKind=FILE` for `PDF` and `DOCX`
- `previewAvailable=false`, `previewKind=NONE` for `DOC`

### 9.4 Admin List Contract Change

`GET /api/admin/applications` should extend each application item with:

- `previewAvailable`
- `previewKind`

Recommended values:

- `previewAvailable=true`, `previewKind=FILE` for `PDF` and `DOCX`
- `previewAvailable=false`, `previewKind=NONE` for `DOC`

The enum should reuse the existing `ResourcePreviewKind` values rather than introducing a snapshot-only contract type.

## 10. Frontend Flow Design

### 10.1 Applicant Applications Page

`/profile/applications` should remain a read-only history board, but each application card should now expose snapshot file actions:

- `PDF` and `DOCX` show `Preview`
- all supported snapshot types show `Download`
- `DOC` does not show `Preview`
- existing `Open Job Detail` remains unchanged

The page copy should be updated from snapshot-information-only language to clearly state that applicants can preview or download their stored snapshot files from the board.

### 10.2 Admin Applications Page

`/admin/applications` should remain a read-only admin workbench.

Action behavior per row:

- keep `Download Resume`
- add `Preview` for `PDF` and `DOCX`
- do not show `Preview` for `DOC`
- keep `Open Job`

Both the desktop table and the mobile card layout should follow the same preview-display rule.

### 10.3 Preview Interaction

The frontend preview interaction should match the current resource and resume preview behavior:

- request the preview endpoint as a blob
- open the blob in a new browser tab or window
- if popup open fails, fall back to `window.location.assign(...)`
- revoke the temporary object URL after a timeout

This keeps snapshot preview consistent with already-shipped preview behaviors.

## 11. Error Handling

### 11.1 Applicant Ownership And Existence

If the application does not exist or is not owned by the current user:

- return `404 application not found`

### 11.2 Admin Existence

If the application does not exist for an admin preview or download request:

- return `404 application not found`

### 11.3 Unsupported Preview Type

If preview is requested for an unsupported snapshot type:

- return `400 application resume preview only supports pdf or docx`

### 11.4 Preview Availability Failures

If preview generation or preview-artifact access fails:

- return `500 application resume preview unavailable`

This includes:

- raw snapshot file missing
- raw snapshot file open failure
- preview-artifact read failure
- `DOCX` conversion failure
- preview-artifact write failure

### 11.5 Download Availability Failures

If snapshot download cannot open the stored file:

- return `500 application resume snapshot unavailable`

## 12. Testing Strategy

### 12.1 Backend Tests

Backend coverage should verify:

- preview-kind mapping for `PDF`, `DOCX`, and `DOC` snapshots
- `DOCX` snapshot preview key derivation uses the snapshot-specific fingerprint fields
- repeated `DOCX` snapshot preview requests reuse cached artifacts
- applicant can preview their own `PDF` snapshot inline
- applicant can preview their own `DOCX` snapshot as PDF
- applicant can download their own snapshot
- applicant cannot preview or download another user's snapshot
- admin can preview any `PDF` or `DOCX` snapshot
- unsupported `DOC` preview is rejected cleanly on both surfaces
- applicant and admin list contracts return preview metadata

### 12.2 Frontend Tests

Frontend coverage should verify:

- `/profile/applications` renders `Preview` plus `Download` for `PDF` and `DOCX`
- `/profile/applications` renders only `Download` for `DOC`
- applicant preview calls the applicant preview helper
- applicant download calls the applicant download helper
- `/admin/applications` renders `Preview` plus `Download Resume` for `PDF` and `DOCX`
- `/admin/applications` omits `Preview` for `DOC`
- admin preview calls the admin preview helper
- existing read-only admin behavior remains unchanged

### 12.3 Manual Smoke

A local smoke pass should verify:

1. log in as `13800000001`
2. submit one application using a `PDF` resume and one using a `DOCX` resume
3. confirm both rows on `/profile/applications` show `Preview` and `Download`
4. submit one application using a `DOC` resume and confirm its row shows `Download` but not `Preview`
5. preview the `PDF` snapshot and confirm it opens inline
6. preview the `DOCX` snapshot and confirm it opens as PDF
7. download the `DOC` snapshot and confirm it still works
8. log in as admin `13800000000`
9. open `/admin/applications`
10. confirm preview works for the `PDF` and `DOCX` snapshots and remains hidden for the `DOC` snapshot

## 13. Documentation Follow-Up

Implementation should update the README to document:

- Phase AA application snapshot online preview as implemented
- that `/profile/applications` supports preview for `PDF` and `DOCX` snapshots
- that `/profile/applications` also supports snapshot download
- that `/admin/applications` supports preview for `PDF` and `DOCX` snapshots
- that `DOC` snapshots remain download-only
- that live resume preview and application snapshot preview remain separate slices

## 14. Acceptance Criteria

1. Applicants can preview their own `PDF` and `DOCX` application snapshots from `/profile/applications`.
2. Admins can preview `PDF` and `DOCX` application snapshots from `/admin/applications`.
3. `DOC` snapshots remain download-only on both applicant and admin surfaces.
4. Applicant and admin application list contracts expose preview metadata through `previewAvailable` and `previewKind`.
5. `DOCX` snapshot preview reuses preview-artifact caching and does not depend on mutable application-row timestamps.
6. Existing download behavior remains available, and applicants gain a snapshot download path for their own applications.
7. No new snapshot cleanup flow, application mutation flow, or generic private-document preview framework is introduced in this phase.
8. Backend and frontend verification for the new snapshot-preview slice pass.
