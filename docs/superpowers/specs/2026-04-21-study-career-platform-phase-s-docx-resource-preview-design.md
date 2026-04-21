# Study Career Platform Phase S DOCX Resource Preview Design

## 1. Goal

Phase S completes the smallest remaining resource-preview gap from Phase H by adding online preview support for `DOCX` files in the resource library.

This phase keeps the delivery intentionally narrow:

- resource-library `DOCX` files become previewable online
- preview continues to use the existing `GET /api/resources/{id}/preview` route
- preview output is normalized to `PDF` so the frontend preview UX stays unchanged

This phase does not introduce a general office-document platform, resume-library document preview, MinIO preview migration, or a new standalone preview API.

## 2. Current Conclusion

### 2.1 Selected Subproject

Phase S implements `resource-library DOCX online preview through server-side DOCX -> PDF conversion`.

Why this is the next slice:

1. Phase H intentionally left `DOCX` as download-only, so the most visible resource-preview gap is still open.
2. The backend already has a working preview-artifact cache, a stable fingerprint strategy, and a single preview endpoint that can be extended without changing access-control rules.
3. The frontend already treats binary file preview as one shared flow, so returning `PDF` for `DOCX` lets the platform add value without designing a second viewer experience.

### 2.2 Chosen Approach

This phase uses:

- on-demand `DOCX -> PDF` conversion on the backend
- a new `DocxPreviewGenerator` boundary
- a `soffice`-based implementation for the actual conversion
- the existing preview-artifact cache location and fingerprinting model
- the existing `ResourcePreviewKind.FILE` semantics in the frontend

This is preferred over browser-side conversion, a new preview microservice, or a dedicated `DOCX` viewer because it is the smallest extension that fits the current monolith and existing preview contract.

## 3. User-Validated Scope

### 3.1 In Scope

This phase includes:

- resource-library `DOCX` preview only
- previewing `DOCX` by converting it to cached `PDF`
- reusing `GET /api/resources/{id}/preview`
- reusing the current preview authorization rules based on resource visibility
- exposing `DOCX` as `ResourcePreviewKind.FILE`
- keeping preview artifacts in the existing local preview storage
- controlled backend failure behavior when the converter is missing or conversion fails

### 3.2 Explicitly Out Of Scope

This phase does not include:

- resume-library `DOC` or `DOCX` preview
- legacy `DOC` conversion support
- MinIO preview-artifact migration
- upload-time preview pre-generation
- a new preview database table
- document annotation, comments, search-in-document, or outline navigation
- a separate preview controller or new public preview endpoint
- changing the frontend viewer layout for `DOCX`

## 4. Approach Options And Recommendation

### 4.1 Recommended Option

Use server-side `soffice` conversion to generate `PDF` on first preview request, cache the generated artifact, and return the cached `PDF` through the existing preview route.

Why this option is recommended:

- it matches the current `PPTX -> PDF` preview model
- it minimizes frontend impact because the browser continues to display `PDF`
- it keeps conversion concerns behind a small backend boundary that can be replaced later
- it preserves the current local-storage preview artifact design

### 4.2 Rejected Alternatives

#### Browser-Side DOCX Rendering

Rejected for this phase because it would:

- create a second preview UX beside the current PDF-oriented flow
- require new frontend libraries and formatting compromises
- increase inconsistency across file-type preview behavior

#### Dedicated Preview Service

Rejected for this phase because it would:

- add deployment and operations complexity that the repository does not currently need
- force early decisions on queueing, object storage, and service-to-service contracts
- make a single missing file-type capability much larger than necessary

#### Upload-Time Pre-Generation

Rejected for this phase because it would:

- slow upload and rejected-resource resubmission paths
- increase coupling between resource lifecycle and preview generation
- require more cleanup logic for replaced files

## 5. Functional Design

### 5.1 Visibility And Access Rules

Preview authorization remains identical to the current resource-detail visibility rules:

- guests can preview published `DOCX` resources
- owners can preview their own unpublished visible `DOCX` resources
- admins can preview visible unpublished `DOCX` resources through existing admin visibility rules
- viewers who cannot access the resource detail must still receive `404`

This phase does not create a second permission model for preview.

### 5.2 Preview Kind Semantics

`DOCX` should move from `ResourcePreviewKind.NONE` to `ResourcePreviewKind.FILE`.

That means:

- `ResourceDetailView.vue` can show the existing preview action
- `ProfileResourcesView.vue` can show the existing preview action
- `AdminResourceManageView.vue` can show the existing preview action
- the browser continues to open a file preview that is actually backed by converted `PDF`

### 5.3 Supported File Types After Phase S

- `PDF`: direct inline preview
- `PPTX`: converted cached `PDF` preview
- `DOCX`: converted cached `PDF` preview
- `ZIP`: directory-tree preview on the separate ZIP route from Phase H

## 6. Backend Architecture

### 6.1 Existing Components Reused

Reuse:

- `ResourceService`
- `ResourcePreviewService`
- `ResourcePreviewArtifactStorage`
- `ResourceFileStorage`
- the current preview artifact fingerprint strategy
- the existing `/api/resources/{id}/preview` controller flow

### 6.2 New Backend Responsibility Boundary

Add a dedicated `DocxPreviewGenerator` abstraction with one responsibility:

- convert a source `DOCX` stream into `PDF` bytes for preview generation

Recommended implementation:

- `SofficeDocxPreviewGenerator`

This keeps the conversion mechanism isolated so a future phase can replace `soffice` without rewriting controller, service, or frontend behavior.

### 6.3 ResourcePreviewService Extension

Extend `ResourcePreviewService` so it can:

- derive a `docx` preview artifact key
- look up cached generated `PDF` artifacts for `DOCX`
- call the `DocxPreviewGenerator` when the cache is missing
- write the generated `PDF` artifact to preview storage
- reopen or stream the generated `PDF` as a standard preview file response

Recommended artifact path pattern:

- `docx/<resource-id>/<fingerprint>.pdf`

With the current local profile defaults, this resolves under `backend/.local-storage/previews/...`.

## 7. Data Model And Caching

### 7.1 Database Impact

No database schema change is required for this phase.

The current `ResourceItem` metadata already contains enough information to support preview cache invalidation.

### 7.2 Cache Fingerprint Strategy

Reuse the existing resource fingerprint composition:

- `storageKey`
- `updatedAt`
- `fileSize`

This keeps `DOCX` aligned with the current `PPTX` and `ZIP` preview cache behavior and ensures a replaced resource file gets a new artifact path automatically.

### 7.3 Artifact Lifecycle

On preview:

1. compute the fingerprinted artifact key
2. if the cached `PDF` exists, reuse it
3. otherwise generate and store a new artifact

As in earlier preview phases, old fingerprinted artifacts may remain on disk until manual cleanup. This phase does not add preview-artifact garbage collection.

## 8. API Design

### 8.1 Existing Preview Endpoint

Keep:

- `GET /api/resources/{id}/preview`

Behavior after Phase S:

- `PDF` returns the original file inline
- `PPTX` returns generated or cached `PDF` inline
- `DOCX` returns generated or cached `PDF` inline
- unsupported types still return a business error

Suggested unsupported-type message after this phase:

- `resource preview only supports pdf, pptx or docx`

### 8.2 Response Characteristics

For successful `DOCX` preview:

- response content type should be `application/pdf`
- response file name should end in `.pdf`
- frontend consumers should not need a new route or a new response shape

This keeps the preview UX identical to the current binary file preview flow.

## 9. Preview Generation Flow

### 9.1 DOCX Flow

1. Validate viewer visibility using the existing resource-detail rules.
2. Validate that the target resource is `DOCX`.
3. Compute the preview fingerprint from current resource metadata.
4. Check preview-artifact storage for `docx/<resource-id>/<fingerprint>.pdf`.
5. If the artifact exists, open and stream the cached `PDF`.
6. If the artifact does not exist:
   - open the raw `DOCX` file from `ResourceFileStorage`
   - pass the source into `DocxPreviewGenerator`
   - receive generated `PDF` bytes
   - write the artifact to preview storage
   - stream the generated `PDF`

### 9.2 Cache Invalidation

No extra invalidation mechanism is needed in this phase.

If a resource file is replaced and any of `storageKey`, `updatedAt`, or `fileSize` changes, the preview artifact path changes automatically and the next preview request generates a fresh `PDF`.

## 10. Operational Dependency Boundary

### 10.1 Converter Dependency

The recommended implementation uses headless LibreOffice via `soffice`.

Recommended configuration key:

- `app.resource-preview.docx.soffice-command`

Recommended default behavior:

- allow the configured value to point to a binary name or full path
- default to `soffice` when no explicit override is provided

### 10.2 Execution Boundary

The `soffice` integration should stay behind the `DocxPreviewGenerator` implementation and should:

- materialize the input `DOCX` into a temporary working directory
- invoke the configured command in headless conversion mode
- read back the generated `PDF`
- clean up temporary files after success or failure

The rest of the application should not need to know how `soffice` is invoked.

### 10.3 Failure Boundary

If the converter is unavailable, misconfigured, times out, or exits without a usable `PDF`, preview should fail with a controlled backend error:

- `docx preview unavailable`

This failure must not affect:

- resource detail access
- resource download
- upload or rejected-resource resubmission
- existing `PDF`, `PPTX`, or `ZIP` preview behavior

## 11. Frontend Impact

### 11.1 UI Behavior

The frontend should not introduce a new `DOCX`-specific viewer.

Instead, once the backend reports `previewKind = FILE` for `DOCX`, the existing preview buttons and file-preview modal flow remain valid.

### 11.2 Expected Surface Changes

Regression-check the current preview-action behavior in:

- `ResourceDetailView.vue`
- `ProfileResourcesView.vue`
- `AdminResourceManageView.vue`

The intended result is simple:

- `DOCX` should now expose the same preview affordance currently used for `PDF` and `PPTX`

## 12. Validation And Error Handling

### 12.1 Access And Type Validation

The backend should continue to reject:

- invisible resources with `404`
- unsupported preview file types with `400`

### 12.2 File And Conversion Failures

Use controlled backend errors for:

- missing raw file: existing `resource file unavailable`
- `DOCX` conversion failure: `docx preview unavailable`

This keeps the preview failure mode explicit without leaking converter-specific process details to the client.

## 13. Testing Strategy

### 13.1 Backend Service Tests

Add or update coverage for:

- `previewKindOf` maps `DOCX` to `FILE`
- first `DOCX` preview generates a cached `PDF`
- second `DOCX` preview reuses the cached artifact
- replacing the source file changes the fingerprint and causes regeneration
- converter failure maps to `docx preview unavailable`

### 13.2 Backend Controller Tests

Add or update coverage for:

- published `DOCX` resource detail returns `previewKind = FILE`
- guest can preview a published `DOCX` resource and receives `application/pdf`
- guest cannot preview an unpublished `DOCX` resource
- owner can preview their own visible unpublished `DOCX` resource
- admin can preview visible unpublished `DOCX` resource
- non-`DOCX` behavior for existing file types is not regressed

### 13.3 Frontend Tests

Update coverage so that:

- resource detail view no longer treats `DOCX` as non-previewable
- profile resource list no longer treats `DOCX` as non-previewable
- admin resource management no longer treats `DOCX` as non-previewable
- existing file-preview invocation still works when backend serves converted `PDF`

## 14. Compatibility And Rollout Notes

- no schema migration is required
- no frontend route contract change is required
- the change is intentionally aligned with the current preview pipeline so rollout risk stays local to the backend preview path
- environments that do not install or configure `soffice` will keep resource download behavior, but `DOCX` preview requests will fail with the controlled availability error

## 15. Acceptance Criteria

Phase S reaches an acceptable implementation state when all of the following are true:

- published resource-library `DOCX` items expose `previewKind = FILE`
- guest preview of a published `DOCX` resource returns inline `PDF`
- owner and admin preview permissions for unpublished `DOCX` resources follow existing resource visibility rules
- repeated preview requests reuse the cached generated artifact until the resource fingerprint changes
- frontend preview actions appear for `DOCX` in the same places that already support `PDF` and `PPTX`
- conversion failure surfaces `docx preview unavailable` without breaking download or other resource flows
