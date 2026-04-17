# Study-Career Platform Phase H Resource Preview Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the current resource preview slice so published and visible `PPTX` resources render inline through cached `PDF` artifacts, visible `ZIP` resources render as cached directory-tree JSON, and `DOCX` remains download-only.

**Architecture:** Keep the existing Spring Boot monolith and Vue SPA intact, but carve preview generation into a small preview subsystem with its own cache storage root and type-specific generators. Preserve the existing `ResourceService` visibility rules as the only authorization gate, then extend DTOs with a `previewKind` contract so the frontend can choose between binary inline preview and ZIP directory preview without guessing from filenames.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, Jackson, Apache POI, PDFBox, local filesystem storage, Vue 3, Vue Router, Pinia, Axios, Vite, Vitest

**Implementation Outcome:** Completed on `2026-04-17`. Commits: `31fe89e` backend preview expansion, `5f82028` frontend type-aware preview UI, `e94866e` README updates, `c17f2eb` local artifact ignore rules. Local smoke evidence captured in `.local-smoke/phase-h-acceptance-backend.json` and `.local-smoke/phase-h-acceptance-frontend.json`.

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-17-study-career-platform-phase-h-resource-preview-expansion-design.md`
- Current implementation baseline:
  - `docs/superpowers/specs/2026-04-16-study-career-platform-phase-g-resource-lifecycle-completion-design.md`
  - `docs/superpowers/plans/2026-04-17-study-career-platform-phase-g-resource-lifecycle-completion-implementation.md`
- Current repo already has:
  - published-resource guest detail access
  - owner/admin unpublished-resource detail visibility
  - authenticated blob-based PDF preview helper in `frontend/src/api/resources.js`
  - local filesystem raw resource storage through `ResourceFileStorage`
  - owner resource board at `/profile/resources`
  - admin review board at `/admin/resources`
- Important seeded data already available in `backend/src/main/resources/data.sql`:
  - resource `1` is published `PDF`
  - resource `2` is published `ZIP`
  - resource `3` is pending `DOCX`
- This phase should stay local-first:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

## Scope Lock

This plan covers only the approved Phase H slice:

- extend resource preview beyond Phase G PDF-only behavior
- add cached `ZIP` directory-tree preview
- add cached `PPTX -> PDF` preview
- keep `DOCX` download-only
- expose preview intent in backend DTOs so frontend actions are type-aware
- preserve Phase G owner/admin/public visibility behavior
- invalidate preview cache when the underlying resource file changes

This plan explicitly does not implement:

- `DOCX` online preview
- archive entry download from ZIP preview
- upload-time preview pre-generation
- MinIO-backed preview storage
- conversion workers or queue-based processing
- annotations, comments, or page-level markup
- a preview-artifact database table

## Concrete Technical Decisions

### Preview Contract

- Keep `previewAvailable` for low-risk compatibility with the current frontend.
- Add `previewKind` to detail, owner-list, and admin-list DTOs with enum values:
  - `NONE`
  - `FILE`
  - `ZIP_TREE`
- `FILE` means the frontend should call `previewResource(id)` and open a new tab.
- `ZIP_TREE` means the frontend should call `previewZipResource(id)` and render the returned JSON inline.

### Cache Storage

- Add a dedicated preview cache root at `.local-storage/previews`.
- Store artifacts under:
  - `pptx/<resource-id>/<fingerprint>.pdf`
  - `zip/<resource-id>/<fingerprint>.json`
- Build the fingerprint from `storageKey + updatedAt + fileSize`.

### PPTX Conversion Strategy

- Use pure Java rendering instead of shelling out to LibreOffice.
- Add `Apache POI` to read slides and `PDFBox` to assemble rendered slide images into a PDF.
- This keeps the local closed loop self-contained and avoids requiring `soffice` in PATH.

## Frontend Skill Rules

Every UI task in this plan must use:

- `@frontend-design`
  Use before editing resource-detail actions, profile resource cards, admin review actions, and any new ZIP preview panel.
- `@ui-ux-pro-max`
  Use before closing each UI task to review responsive behavior, empty/error states, and action labeling.

Preserve the current visual system:

- keep the existing warm-paper and editorial archive styling
- avoid introducing modal-heavy UI when inline panels fit the current screens better
- use one consistent label map for `Preview` versus `Preview Contents`

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/ResourcePreviewKind.java`
- Create: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Create: `backend/src/main/java/com/campus/dto/ResourceZipPreviewResponse.java`
- Create: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
- Create: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Create: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Create: `backend/src/main/java/com/campus/preview/PptxPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/ApachePoiPptxPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/ZipPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/DefaultZipPreviewGenerator.java`
- Create: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Create: `backend/src/test/java/com/campus/preview/ApachePoiPptxPreviewGeneratorTests.java`

### Backend: Modify Existing

- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

### Frontend: Create

- Create: `frontend/src/components/ResourceZipPreviewPanel.vue`
- Create: `frontend/src/components/ResourceZipPreviewPanel.spec.js`

### Frontend: Modify Existing

- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceDetailView.vue`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.vue`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.vue`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

### Repo Docs

- Modify: `README.md`

## Responsibility Notes

- `ResourceService` remains the owner of resource visibility and raw-file validation; it should not learn PPTX conversion internals or ZIP parsing details.
- `ResourcePreviewService` owns preview-kind routing, fingerprinting, artifact-cache lookup, and delegation to type-specific generators.
- `ResourcePreviewArtifactStorage` keeps derived artifacts physically separate from raw resource uploads.
- `ApachePoiPptxPreviewGenerator` should know how to transform a `PPTX` stream into PDF bytes, but nothing about controller responses or viewer permissions.
- `DefaultZipPreviewGenerator` should normalize archive entries into a stable, frontend-friendly tree payload.
- `ResourceZipPreviewPanel.vue` is a presentational component; API calls stay in the views so each screen can control loading and placement.

## Task 1: Add the Shared Preview Contract and Cache Foundation

**Files:**
- Create: `backend/src/main/java/com/campus/common/ResourcePreviewKind.java`
- Create: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Create: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
- Create: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Create: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Create: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/main/java/com/campus/dto/ResourceDetailResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/MyResourceListResponse.java`
- Modify: `backend/src/main/java/com/campus/dto/AdminResourceListResponse.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [x] **Step 1: Write the failing preview-contract and cache-behavior tests**

Add controller assertions so the contract is explicit:

```java
@Test
void publishedZipDetailExposesZipPreviewKind() throws Exception {
    mockMvc.perform(get("/api/resources/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.previewAvailable").value(true))
            .andExpect(jsonPath("$.data.previewKind").value("ZIP_TREE"));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void myResourcesExposePreviewKindPerFileType() throws Exception {
    mockMvc.perform(get("/api/resources/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resources[0].previewKind").isNotEmpty());
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminResourcesExposePreviewKindPerFileType() throws Exception {
    mockMvc.perform(get("/api/admin/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resources[0].previewKind").isNotEmpty());
}
```

Add a unit test for cache-key behavior:

```java
@Test
void fingerprintChangesWhenStorageKeyOrUpdatedAtChanges() {
    ResourceItem first = resource(9L, "deck-a.pptx", "pptx", "seed/a.pptx", 1024L, now);
    ResourceItem second = resource(9L, "deck-a.pptx", "pptx", "seed/b.pptx", 1024L, now.plusMinutes(1));

    assertThat(service.fingerprintOf(first)).isNotEqualTo(service.fingerprintOf(second));
}
```

- [x] **Step 2: Run the failing backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests" test
```

Expected: FAIL because `previewKind`, preview-cache configuration, and `ResourcePreviewService` do not exist yet.

- [x] **Step 3: Implement the preview-kind and cache foundation**

Add the enum:

```java
public enum ResourcePreviewKind {
    NONE,
    FILE,
    ZIP_TREE
}
```

Add preview-cache properties:

```java
@Data
@Component
@ConfigurationProperties(prefix = "app.resource-preview")
public class ResourcePreviewProperties {
    private String localRoot = ".local-storage/previews";
}
```

Add DTO fields without breaking the existing boolean flag:

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
        boolean previewAvailable,
        ResourcePreviewKind previewKind) {
}
```

Implement the preview-kind helper once in `ResourceService`:

```java
ResourcePreviewKind previewKindOf(ResourceItem resource) {
    if (isZip(resource)) {
        return ResourcePreviewKind.ZIP_TREE;
    }
    if (isPdf(resource) || isPptx(resource)) {
        return ResourcePreviewKind.FILE;
    }
    return ResourcePreviewKind.NONE;
}
```

Implementation notes:

- keep visibility checks centralized in `ResourceService`
- `previewAvailable` should now mean `previewKind != NONE && viewer can see resource`
- keep `ResourcePreviewService` free of authentication logic
- expose a package-visible `fingerprintOf(ResourceItem resource)` helper so unit tests can pin cache invalidation behavior

- [x] **Step 4: Run the backend tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests" test
```

Expected: PASS with stable `previewKind` serialization and fingerprint behavior.

- [x] **Step 5: Commit the preview-contract foundation**

```bash
git add backend/src/main/java/com/campus/common/ResourcePreviewKind.java backend/src/main/java/com/campus/config/ResourcePreviewProperties.java backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/service/AdminResourceService.java backend/src/main/java/com/campus/dto/ResourceDetailResponse.java backend/src/main/java/com/campus/dto/MyResourceListResponse.java backend/src/main/java/com/campus/dto/AdminResourceListResponse.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: add resource preview contract foundation"
```

## Task 2: Implement ZIP Directory Preview Backend

**Files:**
- Create: `backend/src/main/java/com/campus/dto/ResourceZipPreviewResponse.java`
- Create: `backend/src/main/java/com/campus/preview/ZipPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/DefaultZipPreviewGenerator.java`
- Modify: `backend/src/main/java/com/campus/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/campus/controller/ResourceController.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [x] **Step 1: Write the failing ZIP preview tests**

Add controller coverage:

```java
@Test
void guestCanPreviewPublishedZipDirectory() throws Exception {
    writeStoredBinaryFile("seed/2026/04/interview-experience-notes.zip", sampleZipBytes());

    mockMvc.perform(get("/api/resources/2/preview-zip"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.fileName").value("interview-experience-notes.zip"))
            .andExpect(jsonPath("$.data.entries[0].path").isNotEmpty());
}

@Test
void previewZipRejectsNonZipResources() throws Exception {
    mockMvc.perform(get("/api/resources/1/preview-zip"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("zip preview only supports zip resources"));
}
```

Add cache-reuse coverage:

```java
@Test
void zipPreviewReusesCachedArtifactUntilFingerprintChanges() {
    when(zipPreviewGenerator.generate(any(), any())).thenAnswer(invocation -> payload("resume/", "resume/a.md"));

    service.previewZip(resource);
    service.previewZip(resource);

    verify(zipPreviewGenerator, times(1)).generate(any(), any());
}
```

- [x] **Step 2: Run the failing ZIP backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,ResourcePreviewServiceTests" test
```

Expected: FAIL because `/preview-zip` and ZIP artifact generation are not implemented.

- [x] **Step 3: Implement ZIP preview generation and caching**

Add the response DTO:

```java
public record ResourceZipPreviewResponse(
        Long resourceId,
        String fileName,
        int entryCount,
        List<Entry> entries) {

    public record Entry(
            String path,
            String name,
            boolean directory,
            Long size) {
    }
}
```

Add the endpoint:

```java
@GetMapping("/{id}/preview-zip")
public Result<ResourceZipPreviewResponse> previewZip(@PathVariable Long id, Authentication authentication) {
    return Result.success(resourceService.previewZipResource(id, identityOf(authentication)));
}
```

Implement ZIP parsing with stable ordering:

```java
public ResourceZipPreviewResponse generate(Long resourceId, String fileName, InputStream zipInputStream) {
    Map<String, ResourceZipPreviewResponse.Entry> entries = new TreeMap<>();
    // normalize slashes, create implicit parent directories, keep size only for files
    return new ResourceZipPreviewResponse(resourceId, fileName, entries.size(), new ArrayList<>(entries.values()));
}
```

Implementation notes:

- explicitly permit `GET /api/resources/*/preview-zip` in `SecurityConfig`
- reuse `requireVisibleResourceForViewer(...)` before ZIP preview generation
- cache the serialized JSON artifact, not a Java object dump
- make directory ordering deterministic so Vitest and MockMvc assertions stay stable

- [x] **Step 4: Run the ZIP backend tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,ResourcePreviewServiceTests" test
```

Expected: PASS with published/visible ZIP preview, non-ZIP rejection, and cache reuse covered.

- [x] **Step 5: Commit the ZIP backend slice**

```bash
git add backend/src/main/java/com/campus/dto/ResourceZipPreviewResponse.java backend/src/main/java/com/campus/preview/ZipPreviewGenerator.java backend/src/main/java/com/campus/preview/DefaultZipPreviewGenerator.java backend/src/main/java/com/campus/config/SecurityConfig.java backend/src/main/java/com/campus/controller/ResourceController.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "feat: add zip directory preview backend"
```

## Task 3: Implement PPTX-to-PDF Preview Generation Backend

**Files:**
- Create: `backend/src/main/java/com/campus/preview/PptxPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/ApachePoiPptxPreviewGenerator.java`
- Create: `backend/src/test/java/com/campus/preview/ApachePoiPptxPreviewGeneratorTests.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [x] **Step 1: Write the failing PPTX preview tests**

Add controller coverage:

```java
@Test
void guestCanPreviewPublishedPptxAsInlinePdf() throws Exception {
    insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "seed/2026/04/career-deck.pptx");
    writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));

    mockMvc.perform(get("/api/resources/4/preview"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanPreviewPendingPptxAsInlinePdf() throws Exception {
    // insert pending PPTX owned by a normal user and assert inline PDF response
}
```

Add generator coverage:

```java
@Test
void generatorTurnsSimplePptxIntoPdfBytes() {
    byte[] pdf = generator.generate(simplePptxBytes("Slide Title"));
    assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.UTF_8));
}
```

Add cache-reuse coverage:

```java
@Test
void filePreviewReusesCachedPdfUntilFingerprintChanges() {
    service.previewFile(resource);
    service.previewFile(resource);
    verify(pptxPreviewGenerator, times(1)).generate(any());
}
```

- [x] **Step 2: Run the failing PPTX backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests" test
```

Expected: FAIL because `PPTX` is still rejected by `/preview`, no conversion dependencies exist, and no cached artifact path is implemented.

- [x] **Step 3: Implement PPTX conversion and cached file preview**

Add the dependencies:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

Update `/preview` behavior:

```java
public ResourceFileStream previewResource(Long resourceId, String identity) {
    User viewer = findViewer(identity);
    ResourceItem resource = requireVisibleResourceForViewer(resourceId, viewer);

    return switch (previewKindOf(resource)) {
        case FILE -> resourcePreviewService.previewFile(resource);
        case ZIP_TREE, NONE -> throw new BusinessException(400, "resource preview only supports pdf or pptx");
    };
}
```

Use Apache POI plus PDFBox for conversion:

```java
try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(pptxBytes));
     PDDocument pdf = new PDDocument()) {
    Dimension size = slideShow.getPageSize();
    for (XSLFSlide slide : slideShow.getSlides()) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, size.width, size.height);
        slide.draw(graphics);
        PDPage page = new PDPage(new PDRectangle(size.width, size.height));
        pdf.addPage(page);
        // draw image onto page
    }
    pdf.save(outputStream);
}
```

Implementation notes:

- keep direct `PDF` preview on the raw file path; only `PPTX` goes through preview-artifact cache
- store generated artifacts with `.pdf` suffix and return `application/pdf`
- controller responses for converted files should still use `ContentDisposition.inline(...)`
- on conversion failure, return `BusinessException(500, "pptx preview unavailable")`

- [x] **Step 4: Run the PPTX backend tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests" test
```

Expected: PASS with guest, owner, and admin visible-PPTX preview behavior covered.

- [x] **Step 5: Commit the PPTX backend slice**

```bash
git add backend/pom.xml backend/src/main/java/com/campus/preview/PptxPreviewGenerator.java backend/src/main/java/com/campus/preview/ApachePoiPptxPreviewGenerator.java backend/src/test/java/com/campus/preview/ApachePoiPptxPreviewGeneratorTests.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "feat: add pptx preview generation"
```

## Task 4: Expand the Frontend Preview Actions and ZIP Preview UI

**Files:**
- Create: `frontend/src/components/ResourceZipPreviewPanel.vue`
- Create: `frontend/src/components/ResourceZipPreviewPanel.spec.js`
- Modify: `frontend/src/api/resources.js`
- Modify: `frontend/src/views/ResourceDetailView.vue`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.vue`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.vue`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

- [x] **Step 1: Write the failing frontend preview-kind and ZIP-panel tests**

Use `@frontend-design` before touching the screens.

Add or extend tests:

```js
test("resource detail shows Preview Contents for ZIP resources and loads the tree inline", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    fileName: "interview-experience-notes.zip",
    fileExt: "zip",
    previewAvailable: true,
    previewKind: "ZIP_TREE",
  });
  previewZipResource.mockResolvedValue({
    resourceId: 11,
    fileName: "interview-experience-notes.zip",
    entryCount: 2,
    entries: [
      { path: "backend/", name: "backend", directory: true, size: null },
      { path: "backend/questions.md", name: "questions.md", directory: false, size: 1834 },
    ],
  });
  // assert button label and inline tree render
});

test("profile resources map preview labels from previewKind", async () => {
  getMyResources.mockResolvedValue({
    total: 2,
    resources: [
      { id: 3, title: "Deck", status: "PENDING", previewAvailable: true, previewKind: "FILE" },
      { id: 4, title: "Archive", status: "PUBLISHED", previewAvailable: true, previewKind: "ZIP_TREE" },
    ],
  });
  // assert Preview and Preview Contents both appear
});

test("docx resources still do not expose preview actions", async () => {
  getAdminResources.mockResolvedValue({
    total: 1,
    resources: [
      { id: 5, title: "Workbook", status: "PENDING", previewAvailable: false, previewKind: "NONE" },
    ],
  });
  // assert no preview button
});
```

- [x] **Step 2: Run the failing frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/components/ResourceZipPreviewPanel.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: FAIL because there is no ZIP preview helper, no ZIP panel component, and the views still hardcode `Preview PDF`.

- [x] **Step 3: Implement the type-aware preview helpers and inline ZIP panel**

Add the API helper:

```js
export async function previewZipResource(id) {
  const { data } = await http.get(`/resources/${id}/preview-zip`);
  return data.data;
}
```

Create a focused presentational component:

```vue
<script setup>
const props = defineProps({
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: "" },
  preview: { type: Object, default: null },
});
</script>
```

Drive labels from `previewKind` instead of filename guessing:

```js
function previewLabel(kind) {
  return kind === "ZIP_TREE" ? "Preview Contents" : "Preview";
}
```

Implementation notes:

- `FILE` preview keeps using the existing blob-to-objectURL flow
- `ZIP_TREE` preview should load JSON once per open action and render inline on the current page
- detail page can show the ZIP panel below the action card
- profile resource cards can expand the ZIP panel within the active card only
- admin board should render ZIP preview in the selected-resource column rather than inside every table row
- `DOCX` must continue to show no preview button anywhere

- [x] **Step 4: Run the frontend tests again and make them pass**

Run:

```powershell
cd frontend
npx vitest run src/components/ResourceZipPreviewPanel.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: PASS with correct labels, ZIP tree rendering, and no preview action for `DOCX`.

- [x] **Step 5: Commit the frontend preview expansion**

Use `@ui-ux-pro-max` before closing the task, then commit:

```bash
git add frontend/src/components/ResourceZipPreviewPanel.vue frontend/src/components/ResourceZipPreviewPanel.spec.js frontend/src/api/resources.js frontend/src/views/ResourceDetailView.vue frontend/src/views/ResourceDetailView.spec.js frontend/src/views/ProfileResourcesView.vue frontend/src/views/ProfileResourcesView.spec.js frontend/src/views/admin/AdminResourceManageView.vue frontend/src/views/admin/AdminResourceManageView.spec.js
git commit -m "feat: add expanded resource preview frontend"
```

## Task 5: Update Docs and Run Full Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README for the completed Phase H preview behavior**

Document:

- `PPTX` inline preview through cached PDF conversion
- `ZIP` directory-tree preview
- `DOCX` remains download-only
- local preview cache root under `.local-storage/previews`
- no external LibreOffice requirement because conversion is pure Java

- [x] **Step 2: Run targeted backend verification**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests" test
```

Expected: PASS

- [x] **Step 3: Run targeted frontend verification**

Run:

```powershell
cd frontend
npx vitest run src/components/ResourceZipPreviewPanel.spec.js src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: PASS

- [x] **Step 4: Run full regression and build checks**

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

- [x] **Step 5: Manual smoke, evidence capture, and final commit**

Manual smoke checklist:

1. Start backend with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
2. Start frontend with `npm run dev -- --host 127.0.0.1`.
3. As guest, open a published `PDF` resource and confirm inline preview still opens in a new tab.
4. As guest, open the published `ZIP` resource and confirm `Preview Contents` renders a directory tree inline.
5. As guest, open a published or pending `DOCX` detail and confirm no preview action appears.
6. Upload or seed a `PPTX` resource, publish it, and confirm guest inline preview returns a PDF.
7. As owner, preview an unpublished visible `PPTX`.
8. As admin, preview a pending `PPTX` and a visible `ZIP`.
9. Resubmit a rejected `PPTX` or `ZIP` with a file replacement and confirm preview cache refreshes on the next preview.
10. Record the smoke results in:
    - `.local-smoke/phase-h-acceptance-backend.json`
    - `.local-smoke/phase-h-acceptance-frontend.json`

Final docs commit:

```bash
git add README.md
git commit -m "docs: update phase h preview documentation"
```

## Execution Notes

- Follow TDD in order. Do not skip the failing-test step even when the implementation feels obvious.
- Keep `previewKind` as the single frontend contract for deciding which preview helper to call.
- Do not infer preview support from `fileName` in Vue code.
- Keep raw resource storage and derived preview storage separate.
- Make ZIP preview ordering deterministic so UI snapshots and JSON assertions stay stable.
- Keep `PPTX` conversion isolated behind `PptxPreviewGenerator` so a later phase can replace the implementation without reworking controllers or frontend code.
- Do not expand this slice into `DOCX` preview, archive entry download, or object storage migration.
