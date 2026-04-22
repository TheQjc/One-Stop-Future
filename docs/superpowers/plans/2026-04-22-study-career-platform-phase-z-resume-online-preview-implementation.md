# Study Career Platform Phase Z Resume Online Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add owner-scoped online preview for `PDF` and `DOCX` resumes on `/profile/resumes` while preserving Phase N resume-library behavior, the existing raw-file storage boundary, and the current preview-artifact runtime model.

**Architecture:** Add a thin `ResumePreviewService` beside the existing resource-preview stack, reuse `DocxPreviewGenerator` plus `ResourcePreviewArtifactStorage` for generated `DOCX -> PDF` artifacts, and keep `PDF` preview as direct inline raw-file streaming. Extend the resume list contract with preview metadata, wire a new `/api/resumes/{id}/preview` endpoint through `ResumeService`, and best-effort delete exact stale `DOCX` preview artifacts when the owning resume is deleted.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, AssertJ, Spring Boot Test, MockMvc, Vue 3, Vite, Vitest, Axios

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md`
- Adjacent completed slices that must remain compatible:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-n-job-application-resume-workflow-design.md`
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`
- Existing resume-domain backend files:
  - `backend/src/main/java/com/campus/controller/ResumeController.java`
  - `backend/src/main/java/com/campus/service/ResumeService.java`
  - `backend/src/main/java/com/campus/entity/Resume.java`
  - `backend/src/main/java/com/campus/mapper/ResumeMapper.java`
  - `backend/src/main/java/com/campus/dto/ResumeRecordResponse.java`
  - `backend/src/main/java/com/campus/dto/ResumeListResponse.java`
- Existing preview runtime files to reuse:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  - `backend/src/main/java/com/campus/preview/DocxPreviewGenerator.java`
  - `backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Existing frontend resume files:
  - `frontend/src/api/resumes.js`
  - `frontend/src/views/ProfileResumesView.vue`
- Existing tests to extend rather than duplicate:
  - `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`
  - `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
  - `frontend/src/views/ProfileResumesView.spec.js`
- Docs that must be updated when the phase lands:
  - `README.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md`

## Scope Lock

This plan covers only the approved Phase Z slice:

- add owner-scoped resume preview on `/profile/resumes`
- support inline preview for `PDF`
- support generated cached PDF preview for `DOCX`
- keep `DOC` resumes download-only
- reuse the existing preview-artifact storage boundary for derived `DOCX` preview artifacts
- add preview metadata to the resume list contract for frontend rendering
- best-effort delete exact stale `DOCX` preview artifacts when the owning resume is deleted

This plan explicitly does not implement:

- preview for application snapshot files
- preview on `/profile/applications` or `/admin/applications`
- preview for `DOC`
- resume rename, replace, or version history
- a new preview-storage backend or migration flow just for resumes
- recursive preview-root cleanup, bucket scanning, or scheduled cleanup jobs
- a rewrite of the `storageKey + updatedAt + fileSize` fingerprint rule

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/ResumePreviewService.java`
  - Resume-domain preview support, preview-target derivation, preview-kind mapping, and generated `DOCX` preview reads/writes.
- Create: `backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java`
  - Covers preview kinds, preview-target derivation, cache reuse, cache writes, and conversion failures.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/controller/ResumeController.java`
  - Add owner-scoped `GET /api/resumes/{id}/preview`.
- Modify: `backend/src/main/java/com/campus/service/ResumeService.java`
  - Inject `ResumePreviewService`, extend list shaping with preview metadata, wire preview orchestration, and delete stale exact preview artifacts on resume delete.
- Modify: `backend/src/main/java/com/campus/dto/ResumeRecordResponse.java`
  - Add `previewAvailable` and `previewKind`.

### Backend Tests: Modify Existing

- Modify: `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`
  - Cover `PDF` preview, `DOCX` preview, unsupported `DOC`, ownership rejection, list preview metadata, and delete-time preview cleanup.

### Frontend: Modify Existing

- Modify: `frontend/src/api/resumes.js`
  - Add `previewResume(id)` that mirrors the current resource-preview blob-open behavior.
- Modify: `frontend/src/views/ProfileResumesView.vue`
  - Render `Preview` for previewable resume rows and handle preview action states.
- Modify: `frontend/src/views/ProfileResumesView.spec.js`
  - Cover preview-button rendering and preview action dispatch while keeping upload/download/delete regressions green.

### Docs: Modify Existing

- Modify: `README.md`
  - Mark Phase Z implemented, remove `online resume preview` from the “not implemented yet” list, and document `PDF / DOCX` preview plus `DOC` download-only semantics.
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md`
  - Add the post-implementation validation note.

## Responsibility Notes

- `ResumeService` owns resume lookup, ownership checks, list shaping, and delete orchestration.
- `ResumePreviewService` is the source of truth for resume preview support, preview kinds, preview-artifact keys, and generated `DOCX` preview reads/writes.
- `PDF` resume preview must bypass preview-artifact storage and stream directly from raw file storage.
- Only `DOCX` generates derived preview artifacts in this phase.
- Delete-time preview cleanup is active-storage-only and best-effort; this phase does not introduce historical-local cleanup for resumes.
- `ResourcePreviewKind` should be reused instead of creating a new parallel enum for resumes.

## Task 1: Add Resume Preview Service Foundation

**Files:**
- Create: `backend/src/main/java/com/campus/preview/ResumePreviewService.java`
- Create: `backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java`

- [x] **Step 1: Write the failing resume-preview-service tests**

Create `ResumePreviewServiceTests` with focused coverage:

```java
class ResumePreviewServiceTests {

    @Test
    void previewKindOfReturnsFileForPdfAndDocxButNotDoc() {
        ResumePreviewService service = new ResumePreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());

        assertThat(service.previewKindOf(resume(1L, "resume.pdf", "pdf", "seed/a.pdf", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf(resume(2L, "resume.docx", "docx", "seed/a.docx", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf(resume(3L, "resume.doc", "doc", "seed/a.doc", 10L, LocalDateTime.now())))
                .isEqualTo(ResourcePreviewKind.NONE);
    }

    @Test
    void previewArtifactTargetOfReturnsDocxLogicalArtifactKeyOnly() {
        ResumePreviewService service = new ResumePreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        Resume docx = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());
        Resume pdf = resume(10L, "intern.pdf", "pdf", "seed/intern.pdf", 1024L, LocalDateTime.now());

        assertThat(service.previewArtifactTargetOf(docx))
                .contains(new ResumePreviewService.PreviewArtifactTarget(
                        "DOCX",
                        service.docxArtifactKeyOf(docx)));
        assertThat(service.previewArtifactTargetOf(pdf)).isEmpty();
    }

    @Test
    void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ResumePreviewService service = new ResumePreviewService(storage, generator);
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));
        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewCacheMissWritesArtifact() throws IOException {
        MissingOnOpenStorage storage = new MissingOnOpenStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ResumePreviewService service = new ResumePreviewService(storage, generator);
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        service.previewDocx(resume, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.writtenKeys()).containsExactly(service.docxArtifactKeyOf(resume));
    }

    @Test
    void docxPreviewFailureBecomesBusinessException() {
        ResumePreviewService service = new ResumePreviewService(new NoopStorage(), inputStream -> {
            throw new IOException("boom");
        });
        Resume resume = resume(9L, "intern.docx", "docx", "seed/intern.docx", 1024L, LocalDateTime.now());

        assertThatThrownBy(() -> service.previewDocx(resume,
                () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("resume preview unavailable");
    }
}
```

- [x] **Step 2: Run the targeted preview-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumePreviewServiceTests" test
```

Expected: FAIL because `ResumePreviewService` does not exist yet.

- [x] **Step 3: Implement the resume preview service**

Create `ResumePreviewService` in the preview package so it stays close to the existing preview stack:

```java
@Service
public class ResumePreviewService {

    private final ResourcePreviewArtifactStorage artifactStorage;
    private final DocxPreviewGenerator docxPreviewGenerator;

    public ResumePreviewService(
            ResourcePreviewArtifactStorage artifactStorage,
            DocxPreviewGenerator docxPreviewGenerator) {
        this.artifactStorage = Objects.requireNonNull(artifactStorage, "artifactStorage");
        this.docxPreviewGenerator = Objects.requireNonNull(docxPreviewGenerator, "docxPreviewGenerator");
    }

    public ResourcePreviewKind previewKindOf(Resume resume) {
        if (isPdf(resume) || isDocx(resume)) {
            return ResourcePreviewKind.FILE;
        }
        return ResourcePreviewKind.NONE;
    }

    public boolean isPreviewAvailable(Resume resume) {
        return previewKindOf(resume) != ResourcePreviewKind.NONE;
    }

    public String docxArtifactKeyOf(Resume resume) {
        return "resume/docx/" + resume.getId() + "/" + fingerprintOf(resume) + ".pdf";
    }

    public Optional<PreviewArtifactTarget> previewArtifactTargetOf(Resume resume) {
        if (resume == null || !isDocx(resume)) {
            return Optional.empty();
        }
        return Optional.of(new PreviewArtifactTarget("DOCX", docxArtifactKeyOf(resume)));
    }

    public PreviewFile previewDocx(Resume resume, DocxSourceSupplier sourceSupplier) {
        String artifactKey = docxArtifactKeyOf(resume);
        Optional<InputStream> cachedArtifact = openArtifactIfPresent(artifactKey);
        if (cachedArtifact.isPresent()) {
            return new PreviewFile(previewFileName(resume.getFileName()), "application/pdf", cachedArtifact.get());
        }

        try (InputStream sourceInputStream = sourceSupplier.open()) {
            byte[] pdfBytes = docxPreviewGenerator.generate(sourceInputStream);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
            return new PreviewFile(previewFileName(resume.getFileName()), "application/pdf",
                    new ByteArrayInputStream(pdfBytes));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(500, "resume preview unavailable");
        }
    }
}
```

Keep the helper methods small and parallel to `ResourcePreviewService`:

- `fingerprintOf(Resume resume)` uses `storageKey + updatedAt + fileSize`
- `openArtifactIfPresent(...)` converts `FileNotFoundException` to cache miss
- `previewFileName(...)` converts `*.docx` to `*.pdf`
- `isPdf(...)` and `isDocx(...)` accept both extension and content type

- [x] **Step 4: Re-run the targeted preview-service tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumePreviewServiceTests" test
```

Expected: PASS with:

- `PDF / DOCX / DOC` preview-kind mapping correct
- exact logical `DOCX` artifact keys derived
- cache reuse on repeated preview
- cache write on miss
- conversion failures translated to `resume preview unavailable`

- [x] **Step 5: Commit the preview-service foundation**

```bash
git add backend/src/main/java/com/campus/preview/ResumePreviewService.java backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java
git commit -m "feat: add resume preview service"
```

## Task 2: Wire Backend Resume Preview Endpoint And Delete Cleanup

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/ResumeController.java`
- Modify: `backend/src/main/java/com/campus/service/ResumeService.java`
- Modify: `backend/src/main/java/com/campus/dto/ResumeRecordResponse.java`
- Modify: `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`

- [x] **Step 1: Write the failing controller regressions**

Extend `ResumeControllerTests`:

1. Add a preview root constant and cleanup:

```java
private static final Path PREVIEW_ROOT = Path.of(".local-storage", "previews");

@AfterEach
void cleanLocalStorage() throws IOException {
    deleteTreeIfExists(STORAGE_ROOT);
    deleteTreeIfExists(PREVIEW_ROOT);
}
```

2. Extend the existing list assertion to prove preview metadata is present:

```java
mockMvc.perform(get("/api/resumes/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.resumes[0].previewAvailable").value(true))
        .andExpect(jsonPath("$.data.resumes[0].previewKind").value("FILE"));
```

3. Add preview endpoint regressions:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void authenticatedUserCanPreviewOwnPdfResumeInline() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            9001L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");
    Files.createDirectories(STORAGE_ROOT.resolve("seed"));
    Files.writeString(STORAGE_ROOT.resolve("seed/resume.pdf"), "%PDF-resume");

    mockMvc.perform(get("/api/resumes/9001/preview"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void authenticatedUserCanPreviewOwnDocxResumeAsPdf() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            9002L, 2L, "Docx Resume", "resume.docx", "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100L, "seed/resume.docx");
    Files.createDirectories(STORAGE_ROOT.resolve("seed"));
    Files.write(STORAGE_ROOT.resolve("seed/resume.docx"), "docx".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(get("/api/resumes/9002/preview"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void docResumePreviewIsRejected() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            9003L, 2L, "Doc Resume", "resume.doc", "doc", "application/msword", 100L, "seed/resume.doc");

    mockMvc.perform(get("/api/resumes/9003/preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("resume preview only supports pdf or docx"));
}

@Test
@WithMockUser(username = "3", roles = "USER")
void userCannotPreviewAnotherUsersResume() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            9004L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");

    mockMvc.perform(get("/api/resumes/9004/preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("resume not found"));
}
```

4. Add delete-time stale-preview cleanup regression:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void deletingDocxResumeDeletesGeneratedPreviewArtifact() throws Exception {
    LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 22, 11, 0);
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
            9005L, 2L, "Docx Resume", "resume.docx", "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100L,
            "seed/resume.docx", updatedAt, updatedAt);
    Path artifactPath = PREVIEW_ROOT.resolve("resume/docx/9005/<fingerprint>.pdf");
    Files.createDirectories(artifactPath.getParent());
    Files.writeString(artifactPath, "%PDF-preview");

    mockMvc.perform(delete("/api/resumes/9005"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    assertThat(Files.exists(artifactPath)).isFalse();
}
```

Use the same fingerprint helper formula inline in the test or compute it from a small local helper so the expected artifact path stays deterministic.

- [x] **Step 2: Run the targeted resume controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumeControllerTests" test
```

Expected: FAIL because the preview endpoint, preview metadata, and delete-time preview cleanup do not exist yet.

- [x] **Step 3: Implement the backend resume preview contract**

Update `ResumeRecordResponse`:

```java
public record ResumeRecordResponse(
        Long id,
        String title,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean previewAvailable,
        ResourcePreviewKind previewKind) {
}
```

Update `ResumeService`:

- inject `ResumePreviewService`
- make `toRecord(...)` include preview metadata
- add owner-scoped preview method
- snapshot old preview target before delete and best-effort delete the exact artifact key after deleting the row

Recommended shape:

```java
public ResumeFileStream preview(String identity, Long resumeId) {
    User viewer = userService.requireByIdentity(identity);
    Resume resume = requireOwnedResume(viewer.getId(), resumeId);
    if (isPdf(resume)) {
        return openResumeFile(resume, "resume preview unavailable");
    }
    if (isDocx(resume)) {
        ResumePreviewService.PreviewFile previewFile = resumePreviewService.previewDocx(
                resume,
                () -> openResumeFile(resume, "resume preview unavailable").inputStream());
        return new ResumeFileStream(previewFile.fileName(), previewFile.contentType(), previewFile.inputStream());
    }
    throw new BusinessException(400, "resume preview only supports pdf or docx");
}

@Transactional
public void delete(String identity, Long resumeId) {
    User viewer = userService.requireByIdentity(identity);
    Resume resume = requireOwnedResume(viewer.getId(), resumeId);
    Optional<ResumePreviewService.PreviewArtifactTarget> oldTarget =
            resumePreviewService.previewArtifactTargetOf(resume);
    String storageKey = resume.getStorageKey();
    resumeMapper.deleteById(resumeId);
    tryDeleteStoredFile(storageKey);
    tryDeletePreviewArtifact(oldTarget);
}
```

Keep preview-artifact delete best-effort:

```java
private void tryDeletePreviewArtifact(Optional<ResumePreviewService.PreviewArtifactTarget> oldTarget) {
    if (oldTarget.isEmpty()) {
        return;
    }
    try {
        resumePreviewService.delete(oldTarget.get().artifactKey());
    } catch (IOException | RuntimeException exception) {
        log.warn("Failed to delete resume preview artifact: {}", oldTarget.get().artifactKey(), exception);
    }
}
```

Expose a small delete method from `ResumePreviewService`:

```java
public void delete(String artifactKey) throws IOException {
    artifactStorage.delete(artifactKey);
}
```

Update `ResumeController`:

```java
@GetMapping("/{id}/preview")
public ResponseEntity<InputStreamResource> preview(@PathVariable Long id, Authentication authentication) {
    ResumeService.ResumeFileStream preview = resumeService.preview(authentication.getName(), id);
    return ResponseEntity.ok()
            .contentType(resolveMediaType(preview.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename(preview.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString())
            .body(new InputStreamResource(preview.inputStream()));
}
```

- [x] **Step 4: Re-run the targeted resume controller tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumeControllerTests" test
```

Expected: PASS with:

- preview metadata present in `/api/resumes/mine`
- `PDF` resumes preview inline
- `DOCX` resumes preview as `application/pdf`
- `DOC` preview rejected clearly
- owner-only access enforced
- delete-time stale preview cleanup green

- [x] **Step 5: Commit the backend preview contract**

```bash
git add backend/src/main/java/com/campus/controller/ResumeController.java backend/src/main/java/com/campus/service/ResumeService.java backend/src/main/java/com/campus/dto/ResumeRecordResponse.java backend/src/test/java/com/campus/controller/ResumeControllerTests.java
git commit -m "feat: add owned resume preview endpoint"
```

## Task 3: Add Resume Preview Action To The Profile Resume Library

**Files:**
- Modify: `frontend/src/api/resumes.js`
- Modify: `frontend/src/views/ProfileResumesView.vue`
- Modify: `frontend/src/views/ProfileResumesView.spec.js`

- [x] **Step 1: Write the failing frontend preview regressions**

Extend `ProfileResumesView.spec.js`:

1. Mock the new API helper:

```javascript
import { createResume, deleteResume, downloadResume, getMyResumes, previewResume } from "../api/resumes.js";

vi.mock("../api/resumes.js", () => ({
  createResume: vi.fn(),
  deleteResume: vi.fn(),
  downloadResume: vi.fn(),
  getMyResumes: vi.fn(),
  previewResume: vi.fn(),
}));
```

2. Add preview-button rendering coverage:

```javascript
test("pdf and docx resumes show preview while doc stays download-only", async () => {
  getMyResumes.mockResolvedValue({
    total: 3,
    resumes: [
      {
        id: 1,
        title: "PDF Resume",
        fileName: "resume.pdf",
        previewAvailable: true,
        previewKind: "FILE",
      },
      {
        id: 2,
        title: "DOCX Resume",
        fileName: "resume.docx",
        previewAvailable: true,
        previewKind: "FILE",
      },
      {
        id: 3,
        title: "DOC Resume",
        fileName: "resume.doc",
        previewAvailable: false,
        previewKind: "NONE",
      },
    ],
  });
  previewResume.mockResolvedValue("blob:resume-preview");

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-resume-1"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-resume-2"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-resume-3"]').exists()).toBe(false);

  await wrapper.find('[data-testid="preview-resume-2"]').trigger("click");

  expect(previewResume).toHaveBeenCalledWith(2);
});
```

3. Keep the current upload/download/delete regression and extend the mocked data with preview metadata so the component contract matches the new backend shape.

- [x] **Step 2: Run the targeted profile-resume frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js
```

Expected: FAIL because `previewResume(...)` and the preview button are not implemented yet.

- [x] **Step 3: Implement the frontend preview action**

Update `frontend/src/api/resumes.js` with the same blob-open pattern used by `previewResource(...)`:

```javascript
export async function previewResume(id) {
  const response = await http.get(`/resumes/${id}/preview`, {
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

Update `ProfileResumesView.vue`:

- import `previewResume`
- add `handlePreview(resume)`
- reserve `actionLoadingId = "preview-${id}"` while preview is opening
- render `Preview` only when `resume.previewAvailable && resume.previewKind === "FILE"`
- keep `Download` and `Delete` unchanged
- update the page copy from “DOCX files download only and do not open in preview” to “PDF and DOCX files support online preview in this phase; DOC remains download-only”

Recommended button block:

```vue
<button
  v-if="resume.previewAvailable && resume.previewKind === 'FILE'"
  :data-testid="`preview-resume-${resume.id}`"
  type="button"
  class="ghost-btn"
  :disabled="actionLoadingId === `preview-${resume.id}`"
  @click="handlePreview(resume)"
>
  {{ actionLoadingId === `preview-${resume.id}` ? "Opening Preview..." : "Preview" }}
</button>
```

- [x] **Step 4: Re-run the targeted profile-resume frontend tests and verify success**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js
```

Expected: PASS with:

- preview buttons only on `PDF / DOCX`
- `DOC` staying download-only
- preview action calling `previewResume(id)`
- upload/download/delete behavior remaining green

- [x] **Step 5: Commit the frontend preview action**

```bash
git add frontend/src/api/resumes.js frontend/src/views/ProfileResumesView.vue frontend/src/views/ProfileResumesView.spec.js
git commit -m "feat: add resume preview action to profile library"
```

## Task 4: Update Docs And Record Validation

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md`

- [x] **Step 1: Update the README**

Apply the following documentation changes:

- add `Phase Z resume online preview first slice` to the repository status line
- in `Implemented now`, update the resume-library bullet from `upload / list / download / delete` to `upload / list / preview / download / delete`
- remove `online resume preview` from the `Explicitly not implemented yet` list
- in the `Job Application And Resume Workflow` section:
  - add `GET /api/resumes/{id}/preview`
  - document `PDF` and `DOCX` preview support on `/profile/resumes`
  - document `DOC` as download-only in this phase

Suggested wording:

```markdown
- resume library upload / list / preview / download / delete
```

```markdown
- authenticated users can preview their own `PDF` and `DOCX` resumes from `/profile/resumes`
- `DOC` resumes remain download-only in this phase
- application snapshot preview and admin-side resume preview are still out of scope
```

- [x] **Step 2: Add the Phase Z validation note to the spec**

Add a validation note near the top of the Phase Z spec:

```markdown
> **Validation note:** This design was implemented and validated on 2026-04-22 using the approved execution record at `docs/superpowers/plans/2026-04-22-study-career-platform-phase-z-resume-online-preview-implementation.md`. Local verification suites now present for this slice are `ResumePreviewServiceTests`, `ResumeControllerTests`, `ResourcePreviewStorageConfigurationTests`, and `ProfileResumesView.spec.js`.
```

- [x] **Step 3: Run the targeted verification suites**

Backend:

```powershell
cd backend
mvn -q "-Dtest=ResumePreviewServiceTests,ResumeControllerTests,ResourcePreviewServiceTests,ResourcePreviewStorageConfigurationTests" test
```

Expected: PASS with:

- resume preview service green
- owner-scoped resume preview endpoint green
- shared preview-artifact storage configuration still green
- existing resource-preview suite still green

Frontend:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js
```

Expected: PASS with the new preview button behavior and no regressions to upload/download/delete interactions.

- [x] **Step 4: Commit the rollout notes**

```bash
git add README.md docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md
git commit -m "docs: add phase z resume preview rollout notes"
```

## Final Verification Checklist

- [x] `backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java`
- [x] `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`
- [x] `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- [x] `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- [x] `frontend/src/views/ProfileResumesView.spec.js`

## Execution Notes

- Keep Phase Z narrow: no snapshot preview and no admin-application preview.
- Keep `DOC` resume preview out of scope even if `soffice` could theoretically support it.
- Reuse `ResourcePreviewKind` and do not introduce a resume-only preview enum.
- Keep `DOCX` preview-artifact cleanup exact-key-only and best-effort.
- Use `apply_patch` for manual edits.
- Do not add new environment variables in this phase.
