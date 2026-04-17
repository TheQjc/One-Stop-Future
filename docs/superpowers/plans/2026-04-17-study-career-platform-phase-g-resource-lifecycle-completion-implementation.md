# Study-Career Platform Phase G Resource Lifecycle Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Completion status:** Completed on 2026-04-17.
> Delivered by commits `b078a70`, `43d6956`, `d16c0eb`, `6f97c9a`, `34d4d30`, `e1c18c4`, and follow-up fix `e35938b`.
> Verification completed with targeted and full backend/frontend test runs plus local live smoke for upload, reject, preview, resubmit, and publish flows.`r`n> Evidence artifacts: `.local-smoke/phase-g-acceptance-backend.json` and `.local-smoke/phase-g-acceptance-frontend.json`.

**Goal:** Complete the first resource-lifecycle follow-up slice by adding rejected-resource edit/resubmit, PDF preview, and the related owner/admin/public UI entry points without introducing version history or document conversion infrastructure.

**Architecture:** Keep the current Spring Boot monolith and Vue SPA structure intact. Extend the existing `ResourceController` and `ResourceService` so detail visibility, resubmission, and preview all share one resource-visibility model, then expose small lifecycle flags in the existing DTOs so the frontend can render actions without guessing from raw status strings. On the frontend, split the current upload page into a shared resource editor form plus a dedicated edit view, and implement preview through authenticated Axios blob fetches instead of raw `window.open("/api/...")` so unpublished owner/admin PDF preview works with JWT stored in `localStorage`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis-Plus, local filesystem resource storage, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-16-study-career-platform-phase-g-resource-lifecycle-completion-design.md`
- Existing implementation baseline:
  - `docs/superpowers/specs/2026-04-16-study-career-platform-phase-d-resource-library-design.md`
  - `docs/superpowers/plans/2026-04-16-study-career-platform-phase-d-resource-library-implementation.md`
  - `docs/superpowers/specs/2026-04-16-study-career-platform-phase-f-discover-ranking-design.md`
- Current repo already has:
  - public resource list/detail/download/favorite flows
  - local filesystem resource storage through `ResourceFileStorage`
  - owner-facing `/profile/resources` record list
  - admin resource review workspace at `/admin/resources`
  - JWT stored in `localStorage` and attached only by Axios interceptors
- Important implementation constraint:
  - raw browser navigation to `/api/resources/{id}/preview` will not carry the Bearer token from `localStorage`
  - protected preview must therefore be implemented on the frontend with `Axios -> blob -> objectURL -> window.open(...)`
  - public published PDF preview can still reuse the same helper path
- Database/schema changes are not required for this slice.
- Safe local backend run must continue using:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

- Safe local frontend run must continue using:

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

## Scope Lock

This plan covers only the Phase G resource-lifecycle completion first slice:

- resource detail visibility expanded so owners can read their own unpublished resources
- `PUT /api/resources/{id}` for rejected-resource edit and resubmit
- optional file replacement during resubmission
- `GET /api/resources/{id}/preview` for inline PDF preview
- lifecycle flags on detail, owner-list, and admin-list DTOs
- dedicated `/resources/:id/edit` route
- shared create/edit resource editor form
- preview buttons on resource detail, my resources, and admin resources
- README updates and verification for the new flow

This plan explicitly does not implement:

- version history
- published/offline resource editing
- DOCX/PPTX/ZIP online preview
- Office-to-PDF conversion
- MinIO
- chunk upload, batch upload, or preview analytics

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing resource editor layout, detail action hierarchy, profile resource cards, and admin preview actions.
- `@ui-ux-pro-max`
  Use before closing each UI task to review mobile behavior, action clarity, error states, and the create/edit form split.

Use the current visual system as the base:

- theme: `editorial archive desk`
- preserve the current warm-paper, deep-navy, and muted-accent language
- keep resource upload/detail/profile/admin screens visually related instead of creating a second subsystem aesthetic

## Planned File Structure

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

### Frontend: Modify Existing

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceUploadView.vue`
- Modify: `frontend/src/views/ResourceUploadView.spec.js`
- Modify: `frontend/src/views/ResourceDetailView.vue`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.vue`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.vue`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

### Frontend: Create

- Create: `frontend/src/components/ResourceEditorForm.vue`
- Create: `frontend/src/views/ResourceEditView.vue`
- Create: `frontend/src/views/ResourceEditView.spec.js`

### Repo Docs

- Modify: `README.md`

### Responsibility Notes

- `ResourceController` continues to own the public/user resource HTTP contract.
- `ResourceService` owns resource visibility rules, rejected-resource edit/resubmit, preview file streaming, and lifecycle-flag mapping.
- `AdminResourceService` remains responsible for admin review list shaping; it should only add list-level preview flags rather than duplicating preview authorization logic.
- `ResourceDetailResponse` is the single source of truth for `rejectReason`, `editableByMe`, and `previewAvailable` on the detail screen.
- `ResourceEditorForm.vue` owns the shared create/edit form UI and validation behavior.
- `ResourceUploadView.vue` becomes a thin create-mode wrapper.
- `ResourceEditView.vue` owns rejected-resource preload, editability gating, and resubmit submission flow.
- `api/resources.js` must own the blob-preview helper so components do not each reinvent object URL behavior.

## Task 1: Extend Resource Detail Visibility and Add the PDF Preview Contract

**Files:**
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`

- [ ] **Step 1: Write the failing detail-visibility and preview tests**

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void ownerCanReadRejectedResourceDetailAndSeeLifecycleFlags() throws Exception {
    mockMvc.perform(get("/api/resources/3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectReason").isNotEmpty())
            .andExpect(jsonPath("$.data.editableByMe").value(true))
            .andExpect(jsonPath("$.data.previewAvailable").value(true));
}

@Test
void guestCanPreviewPublishedPdfInline() throws Exception {
    writeStoredFile("seed/2026/04/resume-template-pack.pdf", "pdf");

    mockMvc.perform(get("/api/resources/1/preview"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
void guestCannotPreviewRejectedPdf() throws Exception {
    mockMvc.perform(get("/api/resources/3/preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
}

@Test
void previewRejectsNonPdfResources() throws Exception {
    mockMvc.perform(get("/api/resources/2/preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("resource preview only supports pdf"));
}
```

- [ ] **Step 2: Run the failing backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: FAIL because owners still cannot read unpublished detail, the preview endpoint does not exist, and the lifecycle flags are not returned.

- [ ] **Step 3: Implement the detail-visibility and preview contract**

Extend the detail DTO:

```java
public record ResourceDetailResponse(
        Long id,
        String title,
        String category,
        String summary,
        String description,
        String status,
        Long uploaderId,
        String uploaderNickname,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        Integer downloadCount,
        Integer favoriteCount,
        LocalDateTime publishedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String rejectReason,
        boolean favoritedByMe,
        boolean editableByMe,
        boolean previewAvailable) {
}
```

Add a preview endpoint beside download:

```java
@GetMapping("/{id}/preview")
public ResponseEntity<InputStreamResource> preview(@PathVariable Long id, Authentication authentication) {
    ResourceFileStream preview = resourceService.previewResource(id, identityOf(authentication));
    return ResponseEntity.ok()
            .contentType(resolveMediaType(preview.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename(preview.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString())
            .body(new InputStreamResource(preview.inputStream()));
}
```

Concentrate visibility logic in `ResourceService`:

```java
private ResourceItem requireVisibleResourceForViewer(Long resourceId, User viewer) {
    ResourceItem resource = requireExistingResource(resourceId);
    if (viewer != null && "ADMIN".equals(viewer.getRole())) {
        return resource;
    }
    if (ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
        return resource;
    }
    if (viewer != null && resource.getUploaderId().equals(viewer.getId())) {
        return resource;
    }
    throw new BusinessException(404, "resource not found");
}

public ResourceFileStream previewResource(Long resourceId, String identity) {
    User viewer = findViewer(identity);
    ResourceItem resource = requireVisibleResourceForViewer(resourceId, viewer);
    if (!isPdf(resource)) {
        throw new BusinessException(400, "resource preview only supports pdf");
    }
    if (!resourceFileStorage.exists(resource.getStorageKey())) {
        throw new BusinessException(500, "resource file unavailable");
    }
    return new ResourceFileStream(resource.getFileName(), resource.getContentType(),
            resourceFileStorage.open(resource.getStorageKey()));
}
```

Implementation notes:

- keep `/api/resources/{id}/download` login-only
- explicitly permit `GET /api/resources/*/preview` in `SecurityConfig`
- compute `editableByMe` and `previewAvailable` inside `toResourceDetail(...)`
- `previewAvailable` should mean 鈥渃urrent viewer can preview this resource now,鈥?not only 鈥渇ileExt == pdf鈥?
- [ ] **Step 4: Run the backend tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: PASS with owner detail visibility, guest published-PDF preview, rejected-resource preview blocking, and non-PDF preview validation all covered.

- [ ] **Step 5: Commit the detail-visibility and preview backend slice**

```bash
git add backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/controller/ResourceController.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/dto/ResourceDetailResponse.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java
git commit -m "feat: add resource detail visibility and pdf preview"
```

## Task 2: Add Rejected-Resource Edit and Resubmission

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`

- [ ] **Step 1: Write the failing resubmission tests**

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void ownerCanResubmitRejectedResourceWithoutReplacingFile() throws Exception {
    mockMvc.perform(multipart("/api/resources/3")
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .param("title", "Revised Resume Pack")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Revised summary")
                    .param("description", "Revised description"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.rejectReason").doesNotExist());
}

@Test
@WithMockUser(username = "2", roles = "USER")
void ownerCanReplaceFileWhileResubmittingRejectedResource() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "file", "revised-pack.pdf", "application/pdf", "new-pdf".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/resources/3")
                    .file(file)
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .param("title", "Revised Resume Pack")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Revised summary")
                    .param("description", "Revised description"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fileName").value("revised-pack.pdf"))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void publishedResourceCannotBeResubmitted() throws Exception {
    mockMvc.perform(multipart("/api/resources/1")
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .param("title", "Nope")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Nope"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("only rejected resource can be resubmitted"));
}
```

- [ ] **Step 2: Run the failing backend resubmission tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: FAIL because the `PUT /api/resources/{id}` endpoint and rejected-resource resubmission behavior do not exist yet.

- [ ] **Step 3: Implement the resubmission flow**

Add the HTTP contract:

```java
@PutMapping("/{id}")
public Result<ResourceDetailResponse> update(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam String category,
        @RequestParam String summary,
        @RequestParam(required = false) String description,
        @RequestParam(name = "file", required = false) MultipartFile file,
        Authentication authentication) {
    return Result.success(resourceService.updateRejectedResource(authentication.getName(), id, title, category, summary,
            description, file));
}
```

Implement the service behavior:

```java
public ResourceDetailResponse updateRejectedResource(String identity, Long resourceId, String title, String category,
        String summary, String description, MultipartFile file) {
    User viewer = userService.requireByIdentity(identity);
    ResourceItem resource = requireEditableRejectedResource(resourceId, viewer);

    resource.setTitle(requireText(title, "title"));
    resource.setCategory(normalizeRequiredCategory(category));
    resource.setSummary(requireText(summary, "summary"));
    resource.setDescription(normalizeOptional(description));

    String previousStorageKey = resource.getStorageKey();
    if (file != null && !file.isEmpty()) {
        ValidatedFile validatedFile = validateFile(file);
        String replacementKey = storeValidatedFile(validatedFile, file);
        resource.setFileName(validatedFile.originalFilename());
        resource.setFileExt(validatedFile.extension());
        resource.setContentType(validatedFile.contentType());
        resource.setFileSize(validatedFile.size());
        resource.setStorageKey(replacementKey);
    }

    resource.setStatus(ResourceStatus.PENDING.name());
    resource.setRejectReason(null);
    resource.setReviewedAt(null);
    resource.setReviewedBy(null);
    resource.setPublishedAt(null);
    resource.setUpdatedAt(LocalDateTime.now());
    resourceItemMapper.updateById(resource);

    tryDeleteReplacedFile(previousStorageKey, resource.getStorageKey());
    return toResourceDetail(resource, viewer);
}
```

Guardrails:

- only the owner can resubmit
- only `REJECTED` resources can resubmit
- `file` remains optional
- if replacement storage succeeds and old-file deletion fails, log a warning and keep the request successful

- [ ] **Step 4: Run the backend tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: PASS with metadata-only resubmission, file-replacement resubmission, and state-guard failures covered.

- [ ] **Step 5: Commit the resubmission backend slice**

```bash
git add backend/src/main/java/com/campus/controller/ResourceController.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java
git commit -m "feat: add resource resubmission flow"
```

## Task 3: Expose Lifecycle Action Flags in Owner and Admin Lists

**Files:**
- Modify: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write the failing lifecycle-flag list tests**

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void myResourcesExposeEditableAndPreviewFlags() throws Exception {
    mockMvc.perform(get("/api/resources/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resources[0].editable").exists())
            .andExpect(jsonPath("$.data.resources[0].previewAvailable").exists());
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminResourcesExposePreviewAvailability() throws Exception {
    mockMvc.perform(get("/api/admin/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resources[0].previewAvailable").exists());
}
```

- [ ] **Step 2: Run the failing backend list tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: FAIL because the owner/admin list DTOs do not expose lifecycle action flags yet.

- [ ] **Step 3: Implement list-level lifecycle flags**

Extend the owner list DTO:

```java
public record ResourceItem(
        Long id,
        String title,
        String category,
        String summary,
        String status,
        String fileName,
        Long fileSize,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        boolean editable,
        boolean previewAvailable) {
}
```

Extend the admin list DTO:

```java
public record ResourceItem(
        Long id,
        String title,
        String category,
        String uploaderNickname,
        String fileName,
        Long fileSize,
        Integer downloadCount,
        String status,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime publishedAt,
        boolean previewAvailable) {
}
```

Use explicit helpers instead of inline boolean soup:

```java
private boolean isPdf(ResourceItem resource) {
    return "pdf".equalsIgnoreCase(resource.getFileExt())
            || "application/pdf".equalsIgnoreCase(resource.getContentType());
}

private boolean isEditableByOwner(ResourceItem resource) {
    return ResourceStatus.REJECTED.name().equals(resource.getStatus());
}
```

Implementation notes:

- owner-list `editable` means rejected only
- owner/admin `previewAvailable` means PDF only
- do not leak per-row 鈥渁dmin can edit鈥?or 鈥減ublishable鈥?flags into this slice

- [ ] **Step 4: Run the backend list tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS with stable list-level lifecycle flags for both owner and admin views.

- [ ] **Step 5: Commit the lifecycle-flag DTO slice**

```bash
git add backend/src/main/java/com/campus/dto/MyResourceListResponse.java backend/src/main/java/com/campus/dto/AdminResourceListResponse.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/service/AdminResourceService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: expose resource lifecycle list flags"
```

## Task 4: Build the Shared Resource Editor Form and Edit Route

**Files:**
- Create: `frontend/src/components/ResourceEditorForm.vue`
- Create: `frontend/src/views/ResourceEditView.vue`
- Create: `frontend/src/views/ResourceEditView.spec.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceUploadView.vue`
- Modify: `frontend/src/views/ResourceUploadView.spec.js`

- [ ] **Step 1: Write the failing create/edit resource editor tests**

Use `@frontend-design` before touching the create/edit form split.

Add or extend tests:

```js
test("upload view still submits a new resource through the shared editor form", async () => {
  // keep the current create flow green after the refactor
});

test("edit view preloads a rejected resource and resubmits without replacing the file", async () => {
  getResourceDetail.mockResolvedValue({
    id: 3,
    title: "Rejected pack",
    category: "RESUME_TEMPLATE",
    summary: "Needs revision",
    description: "Old copy",
    fileName: "rejected-pack.pdf",
    rejectReason: "Clarify the summary",
    editableByMe: true,
    previewAvailable: true,
    status: "REJECTED",
  });
  updateResource.mockResolvedValue({ id: 3, status: "PENDING" });
  // assert prefill + submit
});

test("edit view blocks when the resource is not editable by the current user", async () => {
  getResourceDetail.mockResolvedValue({
    id: 3,
    editableByMe: false,
    previewAvailable: true,
    status: "REJECTED",
  });
  // assert error state instead of editable form
});
```

- [ ] **Step 2: Run the failing frontend editor tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js
```

Expected: FAIL because there is no shared form, no edit route, and no resource update API helper.

- [ ] **Step 3: Implement the shared editor form and edit route**

Add a shared form component:

```vue
<script setup>
const props = defineProps({
  mode: { type: String, required: true },
  initialValue: { type: Object, required: true },
  reviewNote: { type: String, default: "" },
  currentFileLabel: { type: String, default: "" },
  submitting: { type: Boolean, default: false },
});

const emit = defineEmits(["submit"]);
</script>
```

Add the update API helper:

```js
export async function updateResource(id, formData) {
  const { data } = await http.put(`/resources/${id}`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}
```

Add the new route:

```js
{
  path: "/resources/:id/edit",
  name: "resource-edit",
  component: () => import("../views/ResourceEditView.vue"),
  meta: { requiresAuth: true },
}
```

Implementation notes:

- `ResourceUploadView.vue` should become a thin create-mode wrapper around `ResourceEditorForm.vue`
- `ResourceEditView.vue` should own the rejected-resource preload and `editableByMe` gate
- the edit form must keep file replacement optional
- the edit success path should redirect to `/profile/resources`

- [ ] **Step 4: Run the frontend editor tests again and make them pass**

Run:

```powershell
cd frontend
npm run test -- src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js
```

Expected: PASS with the shared create/edit form, edit prefill, and editability blocking behavior working.

- [ ] **Step 5: Commit the editor form and edit route**

```bash
git add frontend/src/components/ResourceEditorForm.vue frontend/src/views/ResourceEditView.vue frontend/src/views/ResourceEditView.spec.js frontend/src/router/index.js frontend/src/api/resources.js frontend/src/views/ResourceUploadView.vue frontend/src/views/ResourceUploadView.spec.js
git commit -m "feat: add resource edit and resubmit page"
```

## Task 5: Add PDF Preview Actions Across Detail, Profile, and Admin Screens

**Files:**
- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceDetailView.vue`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.vue`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.vue`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

- [ ] **Step 1: Write the failing preview-action frontend tests**

Use `@frontend-design` before editing the action layouts, then `@ui-ux-pro-max` before closing the task.

Add or extend tests:

```js
test("resource detail shows a preview action when preview is available", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    previewAvailable: true,
  });
  previewResource.mockResolvedValue("blob:resource-preview");
  // assert button visibility and handler call
});

test("profile resources show edit and preview actions from lifecycle flags", async () => {
  getMyResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 3,
        title: "Rejected pack",
        status: "REJECTED",
        editable: true,
        previewAvailable: true,
      },
    ],
  });
  // assert edit route link and preview button
});

test("admin resource board exposes preview actions for previewable resources", async () => {
  getAdminResources.mockResolvedValue({
    total: 1,
    resources: [
      {
        id: 41,
        title: "Pending archive",
        uploaderNickname: "NormalUser",
        status: "PENDING",
        previewAvailable: true,
      },
    ],
  });
  // assert preview action appears
});
```

- [ ] **Step 2: Run the failing preview-action tests**

Run:

```powershell
cd frontend
npm run test -- src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: FAIL because preview helper behavior and lifecycle-driven preview buttons are not implemented yet.

- [ ] **Step 3: Implement the preview helper and preview actions**

Implement preview through Axios blob fetch instead of raw route opening:

```js
export async function previewResource(id) {
  const response = await http.get(`/resources/${id}/preview`, {
    responseType: "blob",
  });

  const objectUrl = window.URL.createObjectURL(response.data);
  const previewWindow = window.open(objectUrl, "_blank", "noopener");

  if (!previewWindow) {
    window.location.assign(objectUrl);
  }

  window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
  return objectUrl;
}
```

Implementation notes:

- do not use `window.open("/api/resources/.../preview")` because unpublished owner/admin preview would lose the JWT header
- `ResourceDetailView.vue` should conditionally show `Preview PDF` from `detail.previewAvailable`
- `ProfileResourcesView.vue` should show:
  - `Edit And Resubmit` when `editable`
  - `Preview PDF` when `previewAvailable`
- `AdminResourceManageView.vue` should show `Preview` only when `previewAvailable`

- [ ] **Step 4: Run the preview-action tests again and make them pass**

Run:

```powershell
cd frontend
npm run test -- src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: PASS with authenticated/public preview behavior routed through the shared helper and all three screens showing the correct actions.

- [ ] **Step 5: Commit the preview frontend slice**

```bash
git add frontend/src/api/resources.js frontend/src/views/ResourceDetailView.vue frontend/src/views/ResourceDetailView.spec.js frontend/src/views/ProfileResourcesView.vue frontend/src/views/ProfileResourcesView.spec.js frontend/src/views/admin/AdminResourceManageView.vue frontend/src/views/admin/AdminResourceManageView.spec.js
git commit -m "feat: add resource preview actions"
```

## Task 6: Update Docs and Run Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for the completed resource lifecycle flow**

Document:

- rejected resource edit and resubmit
- PDF-only online preview
- `/resources/:id/edit`
- owners can view their own unpublished resources
- preview remains PDF-only; DOCX/PPTX/ZIP still download-only

- [ ] **Step 2: Run targeted backend verification**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS

- [ ] **Step 3: Run targeted frontend verification**

Run:

```powershell
cd frontend
npm run test -- src/views/ResourceUploadView.spec.js src/views/ResourceEditView.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: PASS

- [ ] **Step 4: Run full regression and build checks**

Run:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run test
npm run build
```

Expected: PASS across the full backend suite, full frontend suite, and production build.

- [ ] **Step 5: Manual smoke and final commit**

Manual smoke checklist:

1. Start backend with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
2. Start frontend with `npm run dev -- --host 127.0.0.1`.
3. As guest, open a published PDF resource detail and confirm `Preview PDF` works.
4. As guest, open a published ZIP resource detail and confirm no preview action is shown.
5. As a normal user, upload a PDF resource, get it rejected from admin, then open `/profile/resources`.
6. Click `Edit And Resubmit`, change metadata without replacing the file, and confirm the record returns to `PENDING`.
7. Repeat with a rejected PDF resource but replace the file before resubmitting.
8. As the resource owner, preview the rejected or pending PDF through the app and confirm the tab opens correctly.
9. As admin, open `/admin/resources` and confirm preview is available for PDF records only.

Final commit:

```bash
git add README.md
git commit -m "docs: update resource lifecycle documentation"
```

## Execution Notes

- Follow TDD in order. Do not skip the failing-test step even if the implementation seems obvious.
- Keep rejected-resource editability logic concentrated in `ResourceService`; do not duplicate the same authorization checks in multiple controllers or Vue components.
- Keep the preview helper centralized in `frontend/src/api/resources.js`; components should consume it, not recreate blob/object URL logic ad hoc.
- Preserve existing resource download behavior.
- Preserve current public resource list and detail behavior for published resources.
- Do not add a version table, document converter, or extra preview backend in this slice.

