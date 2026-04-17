# Study-Career Platform Phase H Resource Preview Expansion Design

> **Validation note:** This design was implemented and validated on 2026-04-17. Execution record: `docs/superpowers/plans/2026-04-17-study-career-platform-phase-h-resource-preview-expansion-implementation.md`. Local evidence: `.local-smoke/phase-h-acceptance-backend.json` and `.local-smoke/phase-h-acceptance-frontend.json`.

## 1. Goal

Phase H extends the resource preview capability beyond the Phase G PDF-only flow while keeping the current Spring Boot monolith, Vue SPA, and local filesystem development model intact.

This phase delivers a minimal but complete preview-expansion slice:

- `PPTX` can be previewed online by converting to `PDF`
- `ZIP` can be previewed as a directory tree
- `DOCX` remains download-only and does not enter the preview pipeline

The design intentionally avoids introducing MinIO, message queues, distributed workers, or a production-grade document conversion platform in this iteration.

## 2. User-Validated Scope

The following decisions were explicitly chosen for this phase:

- `DOCX` stays download-only
- `ZIP` preview only shows the archive directory structure
- `PPTX` preview uses the default approach: convert to `PDF` and preview inline
- implementation should prioritize a `local` profile closed loop first

## 3. Non-Goals

This phase does not implement:

- `DOCX` online preview
- `ZIP` file extraction, inline reading, or per-entry downloads from the preview UI
- annotation, highlight, page comments, or review markup
- upload-time preview pre-generation
- preview analytics
- MinIO-backed preview storage
- a standalone conversion microservice
- a full version-history model for preview artifacts

## 4. Chosen Approach

### 4.1 Recommendation

Use on-demand preview generation with local cached preview artifacts.

This means:

- `PDF` keeps the current direct inline-preview path
- `PPTX` preview generates a cached `PDF` on first request, then reuses it
- `ZIP` preview generates a cached directory index on first request, then reuses it

### 4.2 Why This Approach

This approach fits the current codebase best:

- it preserves the existing `ResourceFileStorage` abstraction for original files
- it avoids making upload or resubmission slower by generating previews upfront
- it keeps the new complexity local to preview-specific code paths
- it creates a clean stepping stone toward future pre-generation or object-storage migration

### 4.3 Rejected Alternatives

#### Upload-Time Pre-Generation

Rejected for this phase because it would:

- slow down upload and resubmission
- expand failure-handling requirements around preview generation
- require more cleanup logic when a rejected resource is edited and resubmitted

#### Separate Preview Service

Rejected for this phase because it would:

- introduce a deployment and ops burden that does not match the current repository stage
- force early decisions on queues, object storage, or service-to-service auth
- make the first useful preview expansion materially larger than necessary

## 5. Functional Scope

### 5.1 Resource Types

#### PDF

- published `PDF` resources remain previewable for guests
- unpublished `PDF` resources remain previewable for the owner and admins according to Phase G rules
- preview continues to return inline file content

#### PPTX

- published `PPTX` resources become previewable for guests
- unpublished `PPTX` resources become previewable for the owner and admins according to resource visibility rules
- preview is served as converted `PDF` output

#### ZIP

- published `ZIP` resources become previewable as a directory tree for guests
- unpublished `ZIP` resources become previewable as a directory tree for the owner and admins according to resource visibility rules
- preview does not open archive contents directly

#### DOCX

- remains download-only
- no preview action should be exposed in the frontend

### 5.2 Visibility Rules

Preview authorization must continue to follow resource visibility rather than introducing a second permission model.

That means:

- if a viewer can access the resource detail, the viewer can access any preview mode supported by that resource type
- if a viewer cannot access the resource detail, preview must return `404`

## 6. Architecture

### 6.1 Existing Components Reused

- `ResourceController`
- `ResourceService`
- `AdminResourceService`
- `ResourceFileStorage`
- local filesystem resource storage under `.local-storage/resources`
- existing JWT-based frontend preview helper for protected file preview

### 6.2 New Backend Responsibilities

Introduce a preview-artifact layer with two responsibilities:

- store generated `PPTX -> PDF` preview files
- store generated `ZIP` directory index JSON artifacts

The preview-artifact layer should stay local to the backend monolith and should not alter the existing raw resource storage abstraction.

### 6.3 Suggested New Backend Units

- `ResourcePreviewService`
  - orchestrates type-based preview behavior
  - owns preview cache lookup and generation
- `ResourcePreviewArtifactStorage`
  - stores and reads preview artifacts from local disk
- `PptxPreviewGenerator`
  - converts `PPTX` input into cached `PDF`
- `ZipPreviewGenerator`
  - extracts archive directory metadata into cached JSON

These names are suggestions, not mandatory exact class names, but the responsibilities should stay separated.

## 7. Data Model and Caching

### 7.1 Raw Resource Model

No database schema change is required for this phase.

Preview cache invalidation should be derived from the current resource row and storage metadata instead of adding new preview tables.

### 7.2 Preview Artifact Fingerprint

The preview cache key should be based on a stable fingerprint composed from:

- `storageKey`
- `updatedAt`
- `fileSize`

This is enough for the current model because rejected-resource resubmission already updates either the storage key or the update timestamp.

### 7.3 Preview Cache Layout

Recommended local cache layout:

- `preview/pptx/<resource-id>/<fingerprint>.pdf`
- `preview/zip/<resource-id>/<fingerprint>.json`

This gives a predictable structure, keeps raw files separate from derived artifacts, and avoids immediate need for a preview-artifact table.

## 8. API Design

### 8.1 Existing File Preview Endpoint

Keep:

- `GET /api/resources/{id}/preview`

Behavior:

- `PDF` returns the original file inline
- `PPTX` returns the converted cached `PDF` inline
- other file types return a business error

For this phase, the endpoint should support:

- `pdf`
- `pptx`

It should reject:

- `docx`
- `zip`

Suggested business message:

- `resource preview only supports pdf or pptx`

### 8.2 New ZIP Preview Endpoint

Add:

- `GET /api/resources/{id}/preview-zip`

Response shape:

```json
{
  "resourceId": 12,
  "fileName": "interview-notes.zip",
  "entryCount": 5,
  "entries": [
    {
      "path": "backend/",
      "name": "backend",
      "directory": true,
      "size": null
    },
    {
      "path": "backend/questions.md",
      "name": "questions.md",
      "directory": false,
      "size": 1834
    }
  ]
}
```

Behavior:

- only valid for `ZIP`
- returns structured archive directory data
- no binary file streaming

Suggested business message for non-`ZIP` use:

- `zip preview only supports zip resources`

## 9. Preview Generation Flow

### 9.1 PDF

1. Validate viewer visibility
2. Validate `PDF` type
3. Stream original file from resource storage

### 9.2 PPTX

1. Validate viewer visibility
2. Validate `PPTX` type
3. Compute preview fingerprint
4. Check preview artifact storage for cached `PDF`
5. If cache exists, stream cached `PDF`
6. If cache does not exist:
   - open original `PPTX`
   - generate `PDF`
   - store artifact
   - stream stored artifact

### 9.3 ZIP

1. Validate viewer visibility
2. Validate `ZIP` type
3. Compute preview fingerprint
4. Check preview artifact storage for cached directory JSON
5. If cache exists, return cached JSON
6. If cache does not exist:
   - open original `ZIP`
   - parse archive entries
   - build normalized directory tree payload
   - store artifact
   - return artifact payload

## 10. Frontend Design

### 10.1 Detail Page

`ResourceDetailView.vue` should expose preview actions by file type:

- `PDF` and `PPTX`: `Preview`
- `ZIP`: `Preview Contents`
- `DOCX`: no preview action

### 10.2 Profile Resources

`ProfileResourcesView.vue` should keep the current lifecycle actions and extend preview labeling:

- `PDF` and `PPTX`: `Preview`
- `ZIP`: `Preview Contents`
- `DOCX`: no preview action

### 10.3 Admin Resource Review

`AdminResourceManageView.vue` should follow the same file-type behavior:

- `PDF` and `PPTX`: preview action opens converted or direct inline view
- `ZIP`: preview action opens directory-structure preview
- `DOCX`: no preview action

### 10.4 Frontend API Layer

Keep the current file-preview helper for binary preview and add a second helper for ZIP preview JSON.

Recommended frontend API shape:

- `previewResource(id)` for `PDF` and `PPTX`
- `previewZipResource(id)` for `ZIP`

## 11. Error Handling

### 11.1 Visibility Errors

- unauthorized viewers receive `404`
- behavior must stay aligned with existing detail visibility rules

### 11.2 Unsupported Types

- `DOCX -> /preview` returns `400`
- non-`ZIP -> /preview-zip` returns `400`

### 11.3 Missing Files

- if the raw resource file is missing, return the existing unified `resource file unavailable` style error

### 11.4 PPTX Conversion Failure

- return `500`
- use a dedicated error message such as `pptx preview unavailable`
- do not block resource download, upload, review, or resubmission

### 11.5 ZIP Parse Failure

- return `500`
- use a dedicated error message such as `zip preview unavailable`
- do not affect the base resource lifecycle

## 12. Testing Strategy

### 12.1 Backend Service Tests

Cover:

- `PDF` preview still works
- first `PPTX` preview generates cached `PDF`
- second `PPTX` preview reuses cache
- first `ZIP` preview generates cached index
- second `ZIP` preview reuses cache
- `DOCX` remains non-previewable

### 12.2 Backend Controller Tests

Cover:

- guest can preview published `PPTX`
- guest cannot preview unpublished `PPTX`
- owner can preview own unpublished `PPTX`
- admin can preview pending `PPTX`
- guest can preview published `ZIP` directory
- guest cannot preview unpublished `ZIP`
- non-`ZIP` calls to `/preview-zip` fail with `400`

### 12.3 Frontend Tests

Cover:

- `PPTX` rows and details show preview action
- `ZIP` rows and details show directory-preview action
- `DOCX` rows and details do not show preview action
- ZIP preview panel handles loading, success, empty, and error states

### 12.4 Local Smoke

Cover:

- guest preview of published `PPTX`
- owner preview of unpublished `PPTX`
- admin preview of pending `PPTX`
- guest preview of published `ZIP` directory
- owner/admin preview of unpublished `ZIP` directory
- `DOCX` remains download-only

## 13. Rollout Notes

This phase should continue to use the local filesystem in the `local` profile.

Any conversion implementation chosen for `PPTX` must be wrapped behind a small boundary so a later phase can replace the mechanism without rewriting controller or frontend behavior.

The same applies to ZIP indexing: artifact generation should be treated as an internal implementation detail, not a public contract.

## 14. Acceptance Criteria

Phase H enters an acceptable implementation state when all of the following are true:

- `PDF` preview behavior from Phase G remains intact
- published `PPTX` resources can be previewed inline by guests
- unpublished visible `PPTX` resources can be previewed by owners and admins
- published `ZIP` resources can show a directory preview
- unpublished visible `ZIP` resources can show a directory preview to owners and admins
- `DOCX` still does not expose preview actions
- rejected-resource resubmission continues to work without preview-cache corruption
- preview cache invalidates correctly after resource file replacement
