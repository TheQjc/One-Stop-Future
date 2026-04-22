# Study Career Platform Phase Z Resume Online Preview Design

## 1. Goal

Phase Z adds a narrow online-preview slice for user-owned resumes while preserving the current Spring Boot monolith, Vue SPA, Phase N resume-library workflow, existing raw-file storage boundary, and the Phase T / X / Y preview-artifact runtime model.

The goal is to let authenticated users preview their own uploaded resumes inline from `/profile/resumes` without turning the resume library into a public resource system, an admin-review workflow, or a broad document-conversion rewrite.

This phase delivers one bounded runtime behavior:

- authenticated users can preview their own `PDF` resumes inline
- authenticated users can preview their own `DOCX` resumes as generated PDF output
- generated `DOCX` resume previews reuse the existing preview-artifact storage boundary and caching model
- preview remains private to the owner; application snapshots and admin surfaces are unchanged
- resume deletion best-effort cleans up exact stale derived preview artifacts when the old logical key is known

## 2. User-Validated Scope

The following scope decisions were explicitly chosen for this phase:

- this slice is `Phase Z resume online preview first slice`
- preview scope covers only the current user's live resume-library records on `/profile/resumes`
- preview does not expand to application snapshot downloads, `/profile/applications`, or `/admin/applications`
- `PDF` resumes should preview inline directly from raw storage
- `DOCX` resumes should preview as generated PDF output
- `DOC` resumes remain download-only in this phase
- generated resume previews should reuse the existing preview-artifact storage infrastructure rather than introducing a separate cache backend
- stale `DOCX` resume preview artifacts should be deleted best-effort when the owning resume is deleted
- preview cleanup stays exact-key-only and does not introduce scanning or scheduled garbage collection

## 3. Non-Goals

This phase does not implement:

- preview for job-application resume snapshots
- preview for admin application review surfaces
- preview for legacy `DOC` resumes
- resume rename, file replacement, or version history
- a public or shared resume URL
- a generic cross-domain document-preview framework for all private file types
- recursive preview-root cleanup or scheduled cleanup jobs
- preview migration workflows separate from the existing Phase W / X / Y model
- a rewrite of the current preview fingerprint semantics away from `storageKey + updatedAt + fileSize`

## 4. Chosen Approach

### 4.1 Recommendation

Add one resume-specific preview service and one owner-scoped preview endpoint, while reusing the existing raw-file storage boundary, `DocxPreviewGenerator`, and preview-artifact storage abstraction already proven by resource previews.

Recommended high-level shape:

- keep `ResumeService` responsible for resume ownership and business visibility
- add `ResumePreviewService` as the resume-domain source of truth for preview support, preview targets, and generated preview reads
- reuse `ResourcePreviewArtifactStorage` for generated `DOCX -> PDF` artifacts
- keep `PDF` resume preview as direct raw-file inline streaming with no derived artifact
- clean stale `DOCX` preview artifacts only when the exact old key is known during resume deletion

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the clear domain boundary between public resources and private resumes
- it reuses the existing file-storage and preview-artifact infrastructure instead of duplicating storage logic
- it avoids forcing `ResourcePreviewService` to absorb `Resume`-specific semantics and ownership rules
- it keeps the phase small enough to land safely after Phases T, X, and Y
- it leaves future snapshot-preview or broader private-document preview work as separate slices

### 4.3 Rejected Alternatives

#### Reuse `ResourcePreviewService` Directly As The Resume Preview Service

Rejected for this phase because it would:

- couple the resume domain to `ResourceItem`-specific preview semantics
- mix private resume ownership rules with public-resource visibility logic
- make future resume-specific changes harder to reason about

#### Add `DOC` Preview In The Same Slice

Rejected because it would:

- broaden the conversion surface beyond the already-supported `DOCX` generator path
- increase environment and verification risk without strong reuse from the current preview stack
- make a narrow first slice less predictable to land

#### Build A Generic Private-Document Preview Framework First

Rejected because it would:

- expand a focused resume-preview request into a larger architectural refactor
- touch already-stable resource preview flows unnecessarily
- delay user-visible value for a problem that can be solved cleanly with a thin resume-domain service

## 5. Functional Scope

### 5.1 User Surface In Scope

Phase Z adds preview only to:

- `/profile/resumes`

Authenticated users may preview only resume records they own.

There is no new route for:

- `/profile/applications`
- `/admin/applications`
- job-application snapshot download flows

### 5.2 Supported Preview Types

Phase Z preview support is:

- `PDF -> inline raw-file preview`
- `DOCX -> generated PDF preview`

Preview is not supported in this phase for:

- `DOC`

If a user or caller attempts to preview an unsupported resume type, the backend should reject the request with a clear business error.

### 5.3 Resume Delete Behavior

Deleting a resume should continue to:

- remove the resume row
- best-effort delete the original raw resume file

Phase Z additionally allows delete to:

- best-effort delete the exact stale derived preview artifact for a `DOCX` resume when the old logical preview key is known

Delete remains successful even if preview-artifact cleanup fails.

## 6. Preview Semantics

### 6.1 PDF Resume Preview

When the resume is a `PDF`:

- no derived preview artifact is generated
- the backend opens the raw stored file directly
- the response is streamed inline with the stored content type

### 6.2 DOCX Resume Preview

When the resume is a `DOCX`:

- the backend derives a logical preview-artifact key
- if a cached artifact exists, it is returned directly as `application/pdf`
- otherwise the backend opens the raw `DOCX`, generates a PDF through `DocxPreviewGenerator`, writes the artifact through `ResourcePreviewArtifactStorage`, and returns the generated PDF

### 6.3 Unsupported Resume Preview Types

When the resume is a `DOC` or any unsupported type:

- the backend should reject preview with `400 resume preview only supports pdf or docx`
- the frontend should not render a preview button for unsupported rows

## 7. Preview Key And Cleanup Semantics

### 7.1 Preview Key Rule

For `DOCX` resumes, the derived preview key should be:

- `resume/docx/{resumeId}/{fingerprint}.pdf`

### 7.2 Fingerprint Rule

The fingerprint should reuse the current preview-key model already established by resource previews:

- `storageKey`
- `updatedAt`
- `fileSize`

Using the same fingerprint ingredients is recommended because it:

- keeps cache invalidation semantics aligned with the existing preview system
- avoids introducing a second invalidation model for private documents
- allows future replace-style resume mutations to rotate preview keys naturally when preview-relevant file state changes

### 7.3 Cleanup Rule

Preview cleanup in this phase applies only when:

- a `DOCX` resume is deleted
- the old preview target can be derived exactly from the current resume record before deletion

Cleanup remains:

- exact-key-only
- best-effort
- non-blocking

This means:

- no recursive directory walk
- no bucket scan
- no scheduled preview cleanup job
- no cleanup on preview-read paths

## 8. Architecture

### 8.1 Existing Components Reused

- `ResumeController`
- `ResumeService`
- `Resume`
- `ResumeMapper`
- `ResourceFileStorage`
- `ResourcePreviewArtifactStorage`
- `DocxPreviewGenerator`
- existing preview-storage configuration in `ResourcePreviewProperties`

### 8.2 New Or Changed Backend Units

- `ResumeController`
  - gains `GET /api/resumes/{id}/preview`
- `ResumeService`
  - gains owner-scoped preview orchestration and delete-time preview cleanup invocation
- `ResumePreviewService`
  - new resume-domain preview service for preview support, target derivation, cache reads, and generated `DOCX` preview writes
- `ResumeRecordResponse`
  - gains preview metadata for frontend rendering

### 8.3 Responsibility Boundaries

- `ResumeService` remains responsible for ownership, resume lookup, and high-level business orchestration
- `ResumePreviewService` owns preview-support rules, preview-target derivation, cache interaction, and generated preview reads
- `ResourceFileStorage` remains responsible only for raw resume files
- `ResourcePreviewArtifactStorage` remains responsible only for derived preview artifacts
- the resource domain and `ResourcePreviewService` should not become the main owner of resume-preview behavior in this phase

## 9. API Design

### 9.1 New Endpoint

Recommended endpoint:

```http
GET /api/resumes/{id}/preview
```

Behavior:

- authentication required
- owner-scoped to the authenticated user
- returns `inline` file content
- `PDF` keeps original content type
- `DOCX` returns `application/pdf`

### 9.2 Resume List Contract Change

`GET /api/resumes/mine` should extend each resume record with:

- `previewAvailable`
- `previewKind`

Recommended values:

- `previewAvailable=true`, `previewKind=FILE` for `PDF` and `DOCX`
- `previewAvailable=false`, `previewKind=NONE` for `DOC`

The enum should reuse the existing `ResourcePreviewKind` values rather than introducing a new parallel frontend-only contract.

## 10. Frontend Flow Design

### 10.1 Resume Library Page

`/profile/resumes` should remain the only user-visible entry point for this slice.

For each resume card:

- `PDF` and `DOCX` show `Preview`
- all existing supported types continue to show `Download`
- all rows continue to show `Delete`
- `DOC` does not show `Preview`

### 10.2 Preview Interaction

The frontend preview interaction should match the current resource-preview behavior:

- request the preview endpoint as a blob
- open the blob in a new browser tab or window
- if popup open fails, fall back to `window.location.assign(...)`

This keeps resume preview consistent with the current user expectation already established by resource preview actions.

### 10.3 Copy Changes

The resume-library description should be updated from a download-only preview statement to:

- `PDF` and `DOCX` support online preview in this phase
- `DOC` remains download-only

No navigation, route, or admin copy changes are required beyond documenting the new capability.

## 11. Error Handling

### 11.1 Ownership And Existence

If the resume does not exist or is not owned by the current user:

- return `404 resume not found`

### 11.2 Unsupported Preview Type

If preview is requested for an unsupported resume type:

- return `400 resume preview only supports pdf or docx`

### 11.3 Preview Availability Failures

If preview generation or preview-artifact access fails:

- return `500 resume preview unavailable`

This includes:

- raw resume file missing
- raw resume file open failure
- preview-artifact read failure
- `DOCX` conversion failure
- preview-artifact write failure

### 11.4 Delete-Time Cleanup Failures

If delete-time preview cleanup fails:

- log a `warn`
- continue the main delete flow
- do not surface a new business error to the user

## 12. Testing Strategy

### 12.1 Backend Tests

Backend coverage should verify:

- owner can preview a `PDF` resume inline
- owner can preview a `DOCX` resume as PDF
- unsupported `DOC` preview is rejected
- another user cannot preview someone else's resume
- `DOCX` preview generation writes the expected artifact key
- repeated `DOCX` preview requests reuse cached artifacts when present
- deleting a `DOCX` resume best-effort deletes the exact stale preview artifact

### 12.2 Frontend Tests

Frontend coverage should verify:

- `/profile/resumes` renders `Preview` only for preview-supported rows
- `DOC` rows remain download-only
- clicking preview calls the new resume-preview API helper
- upload, download, and delete behaviors continue to work unchanged

### 12.3 Manual Smoke

A local smoke pass should verify:

1. log in as `13800000001`
2. upload one `PDF` resume and one `DOCX` resume
3. confirm both rows show `Preview`
4. upload one `DOC` resume and confirm it does not show `Preview`
5. preview the `PDF` and confirm it opens inline
6. preview the `DOCX` and confirm it opens as PDF
7. delete the previewed `DOCX` resume and confirm the record disappears without a user-visible error

## 13. Documentation Follow-Up

Implementation should update the README to document:

- Phase Z resume online preview as implemented
- that `/profile/resumes` supports online preview for `PDF` and `DOCX`
- that `DOC` remains download-only
- that job-application snapshots and admin application views are unchanged in this phase

The README should also remove `online resume preview` from the `Explicitly not implemented yet` list while leaving `resume rename / replace`, `version history`, and `chunk upload` out of scope.

## 14. Acceptance Criteria

1. Authenticated users can preview their own `PDF` and `DOCX` resumes from `/profile/resumes`.
2. `DOCX` resume preview is returned as PDF and reuses preview-artifact caching.
3. `DOC` resumes remain download-only in this phase.
4. Resume preview is not added to application snapshots or admin application surfaces.
5. Delete-time cleanup removes only exact stale `DOCX` preview artifacts when the old key is known.
6. Cleanup failures do not change the user-visible success semantics of resume deletion.
7. Backend and frontend verification for the new preview slice pass.
