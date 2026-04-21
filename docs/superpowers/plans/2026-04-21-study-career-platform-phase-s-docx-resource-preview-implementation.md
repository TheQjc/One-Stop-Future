# Phase S DOCX Resource Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add resource-library `DOCX` online preview by converting visible `DOCX` files to cached `PDF` artifacts behind the existing `/api/resources/{id}/preview` route.

**Architecture:** Keep the current preview pipeline intact: `ResourceService` still owns access control, `ResourcePreviewService` still owns fingerprinted preview artifacts, and the frontend still treats binary file preview as one generic `FILE` flow. Add a dedicated `DocxPreviewGenerator` boundary plus a `soffice`-based implementation so `DOCX` can reuse the same cached-PDF pattern already used for `PPTX`.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, H2 test profile, Vue 3, Vue Router, Axios, Vite, Vitest

---

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/DocxPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java`
- Create: `backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java`

### Backend: Modify

- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

### Frontend: Modify

- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

### Frontend: Verify Only Unless Red Tests Prove Otherwise

- Verify: `frontend/src/views/ResourceDetailView.vue`
- Verify: `frontend/src/views/ProfileResourcesView.vue`
- Verify: `frontend/src/views/admin/AdminResourceManageView.vue`

## Task 1: Add A Testable DOCX Conversion Boundary

**Files:**

- Create: `backend/src/main/java/com/campus/preview/DocxPreviewGenerator.java`
- Create: `backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java`
- Create: `backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java`
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`

- [ ] **Step 1: Write failing unit tests for `soffice`-backed DOCX conversion**

Create focused tests that do not require LibreOffice to be installed by injecting a fake command runner into the generator:

```java
@Test
void generatorReturnsPdfBytesWhenCommandProducesPdf() throws Exception {
    SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
            "fake-soffice",
            (command, workingDirectory) -> {
                Files.writeString(workingDirectory.resolve("preview.pdf"), "%PDF-1.7\n");
                return 0;
            });

    byte[] pdf = generator.generate(new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
}

@Test
void generatorFailsWhenCommandExitsWithoutPdfOutput() {
    SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
            "fake-soffice",
            (command, workingDirectory) -> 0);

    assertThatThrownBy(() -> generator.generate(new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("docx preview unavailable");
}
```

- [ ] **Step 2: Run the new DOCX generator tests and confirm failure**

Run:

```powershell
cd backend
mvn -q -Dtest=SofficeDocxPreviewGeneratorTests test
```

Expected: FAIL because `DocxPreviewGenerator` and `SofficeDocxPreviewGenerator` do not exist yet.

- [ ] **Step 3: Implement the minimal conversion boundary**

Add the interface and implementation with a secondary constructor for tests:

```java
public interface DocxPreviewGenerator {
    byte[] generate(InputStream docxInputStream) throws IOException;
}

@Component
public class SofficeDocxPreviewGenerator implements DocxPreviewGenerator {

    @FunctionalInterface
    interface CommandRunner {
        int run(List<String> command, Path workingDirectory) throws IOException, InterruptedException;
    }

    @Override
    public byte[] generate(InputStream docxInputStream) throws IOException {
        Path tempDirectory = Files.createTempDirectory("docx-preview-");
        try {
            Path inputFile = tempDirectory.resolve("preview.docx");
            Path outputFile = tempDirectory.resolve("preview.pdf");
            Files.copy(docxInputStream, inputFile, StandardCopyOption.REPLACE_EXISTING);

            int exitCode = commandRunner.run(buildCommand(inputFile), tempDirectory);
            if (exitCode != 0 || !Files.exists(outputFile)) {
                throw new IOException("docx preview unavailable");
            }
            return Files.readAllBytes(outputFile);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("docx preview unavailable", exception);
        } finally {
            deleteTempDirectory(tempDirectory);
        }
    }
}
```

Also extend preview configuration so `app.resource-preview.docx.soffice-command` binds cleanly:

```java
@Data
public class ResourcePreviewProperties {
    private String localRoot = ".local-storage/previews";
    private Docx docx = new Docx();

    @Data
    public static class Docx {
        private String sofficeCommand = "soffice";
    }
}
```

Add matching config entries:

```yaml
app:
  resource-preview:
    local-root: .local-storage/previews
    docx:
      soffice-command: ${RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND:soffice}
```

- [ ] **Step 4: Re-run the DOCX generator tests**

Run:

```powershell
cd backend
mvn -q -Dtest=SofficeDocxPreviewGeneratorTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/preview/DocxPreviewGenerator.java backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java backend/src/main/java/com/campus/config/ResourcePreviewProperties.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml
git commit -m "feat: add docx preview generator boundary"
```

## Task 2: Extend Preview Caching And Resource Preview Semantics

**Files:**

- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [ ] **Step 1: Write failing service tests for DOCX cache reuse and failure mapping**

Extend `ResourcePreviewServiceTests` with a counting DOCX stub:

```java
@Test
void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
    InMemoryStorage storage = new InMemoryStorage();
    CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
    ResourcePreviewService service = new ResourcePreviewService(
            storage, new ObjectMapper(), new NoopPptxPreviewGenerator(), generator,
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
    ResourceItem resource = resource(9L, "writing-workbook.docx", "docx", "seed/workbook.docx", 1024L, LocalDateTime.now());

    service.previewDocx(resource, this::sampleDocxStream);
    service.previewDocx(resource, this::sampleDocxStream);

    assertThat(generator.invocationCount()).isEqualTo(1);
}

@Test
void docxPreviewFailureBecomesBusinessException() {
    ResourcePreviewService service = new ResourcePreviewService(
            new NoopStorage(), new ObjectMapper(), new NoopPptxPreviewGenerator(),
            input -> { throw new IOException("boom"); },
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));

    assertThatThrownBy(() -> service.previewDocx(resource(9L, "writing-workbook.docx", "docx", "seed/workbook.docx", 1024L, LocalDateTime.now()), this::sampleDocxStream))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("docx preview unavailable");
}
```

- [ ] **Step 2: Run the preview-service test suite and confirm failure**

Run:

```powershell
cd backend
mvn -q -Dtest=ResourcePreviewServiceTests test
```

Expected: FAIL because `ResourcePreviewService` has no DOCX path yet.

- [ ] **Step 3: Implement DOCX preview caching and expose DOCX as `FILE`**

Keep the implementation DRY by sharing the generated-PDF code path:

```java
public PreviewFile previewDocx(ResourceItem resource, DocxSourceSupplier docxSourceSupplier) {
    return previewGeneratedPdf(
            "docx/" + resource.getId() + "/" + fingerprintOf(resource) + ".pdf",
            resource,
            docxSourceSupplier::open,
            docxPreviewGenerator::generate,
            "docx preview unavailable");
}
```

Update `ResourceService` so the new type participates everywhere the current `PPTX` path already does:

```java
public ResourceFileStream previewResource(Long resourceId, String identity) {
    ResourceItem resource = requireVisibleResourceForViewer(resourceId, findViewer(identity));
    if (isPdf(resource)) {
        return openResourceFile(resource);
    }
    if (isPptx(resource)) {
        PreviewFile previewFile = resourcePreviewService.previewFile(resource, () -> openResourceFile(resource).inputStream());
        return new ResourceFileStream(previewFile.fileName(), previewFile.contentType(), previewFile.inputStream());
    }
    if (isDocx(resource)) {
        PreviewFile previewFile = resourcePreviewService.previewDocx(resource, () -> openResourceFile(resource).inputStream());
        return new ResourceFileStream(previewFile.fileName(), previewFile.contentType(), previewFile.inputStream());
    }
    throw new BusinessException(400, "resource preview only supports pdf, pptx or docx");
}

ResourcePreviewKind previewKindOf(ResourceItem resource) {
    if (isZip(resource)) {
        return ResourcePreviewKind.ZIP_TREE;
    }
    if (isPdf(resource) || isPptx(resource) || isDocx(resource)) {
        return ResourcePreviewKind.FILE;
    }
    return ResourcePreviewKind.NONE;
}
```

- [ ] **Step 4: Re-run the preview-service tests**

Run:

```powershell
cd backend
mvn -q -Dtest=ResourcePreviewServiceTests test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "feat: add docx preview caching flow"
```

## Task 3: Lock Down Controller And Admin Preview Behavior

**Files:**

- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write failing integration coverage for DOCX detail, list, and preview**

Add or update controller tests for these scenarios:

- published `DOCX` detail returns `previewAvailable = true` and `previewKind = FILE`
- `/api/resources/mine` now reports the seeded pending `DOCX` as previewable to its owner
- `/api/admin/resources` now reports the seeded pending `DOCX` as previewable to admins
- guest can preview a published `DOCX` resource and receives `application/pdf`
- owner can preview a visible pending `DOCX` resource and receives `application/pdf`
- admin can preview a visible pending `DOCX` resource and receives `application/pdf`
- `ZIP` still fails on `/preview` with the updated unsupported-type message

Use `@MockBean DocxPreviewGenerator` in both integration test classes so these tests do not depend on a local LibreOffice install:

```java
@MockBean
private DocxPreviewGenerator docxPreviewGenerator;

@BeforeEach
void stubDocxPreview() throws IOException {
    when(docxPreviewGenerator.generate(any())).thenReturn("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
}
```

- [ ] **Step 2: Run the backend controller suites and confirm failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: FAIL because the current controllers still treat `DOCX` as non-previewable.

- [ ] **Step 3: Make the controller suites green without broadening scope**

The production code changes from Task 2 should satisfy most of this work. Keep this task focused on test wiring and assertions:

- add the DOCX generator mock beans
- write dummy `DOCX` bytes to local test storage where preview requests need a visible source file
- update old `NONE` expectations for seeded `DOCX` resource `id = 3` to `FILE`
- keep all ZIP assertions and guest-visibility assertions intact so the Phase H behavior stays covered

Example assertion updates:

```java
mockMvc.perform(get("/api/resources/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.resources[1].previewAvailable").value(true))
        .andExpect(jsonPath("$.data.resources[1].previewKind").value("FILE"));

mockMvc.perform(get("/api/resources/2/preview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("resource preview only supports pdf, pptx or docx"));
```

- [ ] **Step 4: Re-run the backend controller suites**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "test: cover docx resource preview endpoints"
```

## Task 4: Update Frontend Regressions And Verify The Generic FILE UI Path

**Files:**

- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`
- Verify: `frontend/src/views/ResourceDetailView.vue`
- Verify: `frontend/src/views/ProfileResourcesView.vue`
- Verify: `frontend/src/views/admin/AdminResourceManageView.vue`

- [ ] **Step 1: Rewrite the stale DOCX tests so they describe the new behavior**

Replace the old "DOCX does not expose preview" assertions with tests that prove `DOCX` now rides the existing `FILE` flow:

```javascript
test("resource detail treats docx resources as Preview-able FILE items", async () => {
  getResourceDetail.mockResolvedValue({
    ...baseDetail,
    fileName: "interview-notes.docx",
    fileExt: "docx",
    contentType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    previewAvailable: true,
    previewKind: "FILE",
  });
  previewResource.mockResolvedValue("blob:docx-preview");

  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-testid="preview-action"]').trigger("click");
  expect(previewResource).toHaveBeenCalledWith(11);
});
```

Mirror the same update in:

- `ProfileResourcesView.spec.js`
- `AdminResourceManageView.spec.js`

- [ ] **Step 2: Run the targeted frontend tests and confirm failure**

Run:

```powershell
cd frontend
npx vitest run src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: FAIL until the stale non-preview assertions are replaced.

- [ ] **Step 3: Keep runtime Vue changes minimal**

If the red tests show the existing generic `FILE` handling already works, change only the tests.

If a hidden fallback bug appears, patch only the affected helper in the matching view file:

```javascript
function canPreview(resource) {
  const kind = previewKindOf(resource);
  return kind === "FILE" || kind === "ZIP_TREE";
}
```

No new `DOCX`-specific branch or label should be introduced in the UI.

- [ ] **Step 4: Re-run frontend tests and build verification**

Run:

```powershell
cd frontend
npx vitest run src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
npm run build
```

Expected: PASS for all three specs and a successful production build.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ResourceDetailView.spec.js frontend/src/views/ProfileResourcesView.spec.js frontend/src/views/admin/AdminResourceManageView.spec.js frontend/src/views/ResourceDetailView.vue frontend/src/views/ProfileResourcesView.vue frontend/src/views/admin/AdminResourceManageView.vue
git commit -m "test: update docx resource preview ui coverage"
```

## Task 5: Final Cross-Surface Verification

**Files:**

- Modify: `backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Modify: `frontend/src/views/ResourceDetailView.spec.js`
- Modify: `frontend/src/views/ProfileResourcesView.spec.js`
- Modify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

- [ ] **Step 1: Run the full targeted backend verification set**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests,SofficeDocxPreviewGeneratorTests,ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS.

- [ ] **Step 2: Re-run the full targeted frontend verification set**

Run:

```powershell
cd frontend
npx vitest run src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
```

Expected: PASS.

- [ ] **Step 3: Review the final diff for scope discipline**

Run:

```powershell
git diff --stat
git status --short
```

Confirm:

- only Phase S `DOCX` resource-preview files changed
- no resume-library preview work slipped in
- no new public preview route was introduced
- no MinIO preview migration work was mixed into this slice

- [ ] **Step 4: Commit final polish if verification required code adjustments**

```bash
git add backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java frontend/src/views/ResourceDetailView.spec.js frontend/src/views/ProfileResourcesView.spec.js frontend/src/views/admin/AdminResourceManageView.spec.js
git commit -m "chore: finalize docx resource preview verification"
```

- [ ] **Step 5: Record rollout dependency**

Before shipping beyond local verification, note in the task handoff or release notes:

- runtime environments need LibreOffice or compatible `soffice` available on `PATH`, or `RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND` must point to the installed binary
- when that dependency is absent, `DOCX` preview should fail with `docx preview unavailable` while download remains functional
