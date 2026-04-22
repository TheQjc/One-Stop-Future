# Study Career Platform Phase AA Application Snapshot Online Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add applicant-facing and admin-facing online preview for immutable application resume snapshots, while keeping `DOC` snapshots download-only and preserving the existing raw-file plus preview-artifact storage boundaries.

**Architecture:** Add a focused `ApplicationSnapshotPreviewService` beside the existing preview stack so applicant and admin flows share one snapshot-preview rule set. Reuse `DocxPreviewGenerator` and `ResourcePreviewArtifactStorage` for generated `DOCX -> PDF` artifacts, keep `PDF` preview as direct raw-file inline streaming, extend both application-list contracts with preview metadata, and add an applicant snapshot download endpoint so `DOC` remains usable on `/profile/applications`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, AssertJ, Spring Boot Test, MockMvc, Vue 3, Vite, Vitest, Axios

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-design.md`
- Adjacent completed slices that must remain compatible:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-n-job-application-resume-workflow-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-z-resume-online-preview-design.md`
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`
- Existing applicant-facing backend files:
  - `backend/src/main/java/com/campus/controller/JobApplicationController.java`
  - `backend/src/main/java/com/campus/service/JobApplicationService.java`
  - `backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java`
  - `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
- Existing admin backend files:
  - `backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java`
  - `backend/src/main/java/com/campus/service/AdminJobApplicationService.java`
  - `backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java`
- Existing preview runtime files to reuse:
  - `backend/src/main/java/com/campus/preview/ResumePreviewService.java`
  - `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/DocxPreviewGenerator.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Existing frontend application files:
  - `frontend/src/api/applications.js`
  - `frontend/src/api/admin.js`
  - `frontend/src/views/ProfileApplicationsView.vue`
  - `frontend/src/views/admin/AdminApplicationsView.vue`
- Existing tests to extend rather than duplicate:
  - `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`
  - `backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java`
  - `frontend/src/views/ProfileApplicationsView.spec.js`
  - `frontend/src/views/admin/AdminApplicationsView.spec.js`
- Docs that must be updated when the phase lands:
  - `README.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-design.md`

## Scope Lock

This plan covers only the approved Phase AA slice:

- add snapshot preview to `/profile/applications`
- add snapshot preview to `/admin/applications`
- support inline preview for `PDF` snapshots
- support generated cached PDF preview for `DOCX` snapshots
- keep `DOC` snapshots download-only
- add applicant snapshot download on `/profile/applications`
- reuse the existing preview-artifact storage boundary for derived `DOCX` snapshot preview artifacts
- add preview metadata to both applicant and admin application-list contracts for frontend rendering

This plan explicitly does not implement:

- changes to `/profile/resumes` preview behavior
- preview for live resumes through application endpoints
- preview for legacy `DOC` snapshots
- application status progression, notes, moderation, or deletion
- snapshot replacement or version history
- preview-artifact cleanup jobs or delete-time cleanup hooks for application snapshots
- a new preview-storage backend or migration flow just for application snapshots

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/ApplicationSnapshotPreviewService.java`
  - Snapshot-domain preview support, preview-kind mapping, artifact-key derivation, and `PDF`/`DOCX` preview reads.
- Create: `backend/src/test/java/com/campus/preview/ApplicationSnapshotPreviewServiceTests.java`
  - Covers preview kinds, snapshot-specific fingerprint rules, cache reuse, cache writes, and preview failures.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/controller/JobApplicationController.java`
  - Add owner-scoped `GET /api/applications/{id}/resume/download` and `GET /api/applications/{id}/resume/preview`.
- Modify: `backend/src/main/java/com/campus/service/JobApplicationService.java`
  - Inject snapshot preview service, extend applicant list shaping with preview metadata, and add owner-scoped preview/download orchestration.
- Modify: `backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java`
  - Add `previewAvailable` and `previewKind`.
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java`
  - Add admin-scoped `GET /api/admin/applications/{id}/resume/preview`.
- Modify: `backend/src/main/java/com/campus/service/AdminJobApplicationService.java`
  - Inject snapshot preview service, extend admin list shaping with preview metadata, and add admin-scoped preview orchestration.
- Modify: `backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java`
  - Add `previewAvailable` and `previewKind`.
- Modify: `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
  - Extend applicant/admin list projections with snapshot file-extension and content-type fields needed for preview metadata.

### Backend Tests: Modify Existing

- Modify: `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`
  - Cover applicant list preview metadata, applicant snapshot preview, applicant snapshot download, and ownership rejection.
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`
  - Cover admin list preview metadata, admin snapshot preview, and unsupported-type rejection.

### Frontend: Modify Existing

- Modify: `frontend/src/api/applications.js`
  - Add applicant snapshot preview/download helpers.
- Modify: `frontend/src/views/ProfileApplicationsView.vue`
  - Render applicant preview/download buttons and handle their loading/error states.
- Modify: `frontend/src/views/ProfileApplicationsView.spec.js`
  - Cover applicant preview/download button rendering and action dispatch.
- Modify: `frontend/src/api/admin.js`
  - Add admin snapshot preview helper while keeping admin download unchanged.
- Modify: `frontend/src/views/admin/AdminApplicationsView.vue`
  - Render admin preview buttons in table and mobile card layouts.
- Modify: `frontend/src/views/admin/AdminApplicationsView.spec.js`
  - Cover admin preview button rendering and action dispatch.

### Docs: Modify Existing

- Modify: `README.md`
  - Mark Phase AA implemented, add new application snapshot preview/download endpoints, and document `PDF/DOCX` preview plus `DOC` download-only semantics.
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-design.md`
  - Add the post-implementation validation note.

## Responsibility Notes

- `JobApplicationService` owns applicant identity, ownership checks, applicant download, applicant preview orchestration, and applicant list shaping.
- `AdminJobApplicationService` owns admin preview authorization, admin preview orchestration, and admin list shaping.
- `ApplicationSnapshotPreviewService` is the source of truth for snapshot preview support, preview kinds, preview-artifact keys, and generated `DOCX` preview reads/writes.
- `PDF` snapshot preview must bypass preview-artifact storage and stream directly from raw snapshot storage.
- Only `DOCX` snapshots generate derived preview artifacts in this phase.
- Because application snapshots are immutable and there is no snapshot delete flow in this phase, no preview-artifact cleanup hook should be added.
- `ResourcePreviewKind` should be reused instead of creating a new snapshot-only preview enum.

## Task 1: Add Application Snapshot Preview Service Foundation

**Files:**
- Create: `backend/src/main/java/com/campus/preview/ApplicationSnapshotPreviewService.java`
- Create: `backend/src/test/java/com/campus/preview/ApplicationSnapshotPreviewServiceTests.java`

- [ ] **Step 1: Write the failing snapshot-preview-service tests**

Create `ApplicationSnapshotPreviewServiceTests` with focused coverage:

```java
class ApplicationSnapshotPreviewServiceTests {

    @Test
    void previewKindOfReturnsFileForPdfAndDocxButNotDoc() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());

        assertThat(service.previewKindOf("pdf", "application/pdf")).isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf("docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .isEqualTo(ResourcePreviewKind.FILE);
        assertThat(service.previewKindOf("doc", "application/msword")).isEqualTo(ResourcePreviewKind.NONE);
    }

    @Test
    void docxArtifactKeyUsesSnapshotFieldsAndIgnoresUpdatedAt() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication first = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 22, 11, 0), LocalDateTime.of(2026, 4, 22, 12, 0));
        JobApplication second = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 22, 11, 0), LocalDateTime.of(2026, 5, 1, 8, 0));
        JobApplication changedSubmittedAt = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.of(2026, 4, 23, 11, 0), LocalDateTime.of(2026, 5, 1, 8, 0));

        assertThat(service.docxArtifactKeyOf(first)).isEqualTo(service.docxArtifactKeyOf(second));
        assertThat(service.docxArtifactKeyOf(first)).isNotEqualTo(service.docxArtifactKeyOf(changedSubmittedAt));
    }

    @Test
    void previewPdfReturnsRawSnapshotStream() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication application = application(41L, "resume.pdf", "pdf", "seed/snapshots/a.pdf", 256L,
                LocalDateTime.now(), LocalDateTime.now());

        ApplicationSnapshotPreviewService.PreviewFile preview = service.preview(application,
                () -> new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(preview.fileName()).isEqualTo("resume.pdf");
        assertThat(preview.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void docxPreviewReusesCachedPdfUntilFingerprintChanges() throws IOException {
        InMemoryStorage storage = new InMemoryStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(storage, generator);
        JobApplication application = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.now(), LocalDateTime.now());

        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));
        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(generator.invocationCount()).isEqualTo(1);
    }

    @Test
    void docxPreviewCacheMissWritesArtifact() throws IOException {
        MissingOnOpenStorage storage = new MissingOnOpenStorage();
        CountingDocxPreviewGenerator generator = new CountingDocxPreviewGenerator();
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(storage, generator);
        JobApplication application = application(41L, "resume.docx", "docx", "seed/snapshots/a.docx", 1024L,
                LocalDateTime.now(), LocalDateTime.now());

        service.preview(application, () -> new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.writtenKeys()).containsExactly(service.docxArtifactKeyOf(application));
    }

    @Test
    void unsupportedPreviewTypeBecomesBusinessException() {
        ApplicationSnapshotPreviewService service = new ApplicationSnapshotPreviewService(
                new NoopStorage(),
                new CountingDocxPreviewGenerator());
        JobApplication application = application(41L, "resume.doc", "doc", "seed/snapshots/a.doc", 256L,
                LocalDateTime.now(), LocalDateTime.now());

        assertThatThrownBy(() -> service.preview(application,
                () -> new ByteArrayInputStream("doc".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("application resume preview only supports pdf or docx");
    }
}
```

- [ ] **Step 2: Run the targeted snapshot-preview-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ApplicationSnapshotPreviewServiceTests" test
```

Expected: FAIL because `ApplicationSnapshotPreviewService` does not exist yet.

- [ ] **Step 3: Implement the snapshot preview service**

Create `ApplicationSnapshotPreviewService` in the preview package so it stays close to the existing preview stack:

```java
@Service
public class ApplicationSnapshotPreviewService {

    private final ResourcePreviewArtifactStorage artifactStorage;
    private final DocxPreviewGenerator docxPreviewGenerator;

    public ApplicationSnapshotPreviewService(
            ResourcePreviewArtifactStorage artifactStorage,
            DocxPreviewGenerator docxPreviewGenerator) {
        this.artifactStorage = Objects.requireNonNull(artifactStorage, "artifactStorage");
        this.docxPreviewGenerator = Objects.requireNonNull(docxPreviewGenerator, "docxPreviewGenerator");
    }

    public ResourcePreviewKind previewKindOf(String fileExt, String contentType) {
        if (isPdf(fileExt, contentType) || isDocx(fileExt, contentType)) {
            return ResourcePreviewKind.FILE;
        }
        return ResourcePreviewKind.NONE;
    }

    public ResourcePreviewKind previewKindOf(JobApplication application) {
        return previewKindOf(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot());
    }

    public boolean isPreviewAvailable(String fileExt, String contentType) {
        return previewKindOf(fileExt, contentType) != ResourcePreviewKind.NONE;
    }

    public String docxArtifactKeyOf(JobApplication application) {
        return "application/snapshot/docx/" + application.getId() + "/" + fingerprintOf(application) + ".pdf";
    }

    public PreviewFile preview(JobApplication application, SnapshotSourceSupplier sourceSupplier) {
        if (isPdf(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot())) {
            try {
                return new PreviewFile(application.getResumeFileNameSnapshot(),
                        application.getResumeContentTypeSnapshot(),
                        sourceSupplier.open());
            } catch (IOException | RuntimeException exception) {
                throw new BusinessException(500, "application resume preview unavailable");
            }
        }

        if (!isDocx(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot())) {
            throw new BusinessException(400, "application resume preview only supports pdf or docx");
        }

        String artifactKey = docxArtifactKeyOf(application);
        Optional<InputStream> cachedArtifact = openArtifactIfPresent(artifactKey);
        if (cachedArtifact.isPresent()) {
            return new PreviewFile(previewFileName(application.getResumeFileNameSnapshot()), "application/pdf",
                    cachedArtifact.get());
        }

        try (InputStream sourceInputStream = sourceSupplier.open()) {
            byte[] pdfBytes = docxPreviewGenerator.generate(sourceInputStream);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
            return new PreviewFile(previewFileName(application.getResumeFileNameSnapshot()), "application/pdf",
                    new ByteArrayInputStream(pdfBytes));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(500, "application resume preview unavailable");
        }
    }
}
```

Keep the helper methods small and parallel to `ResumePreviewService`:

- `fingerprintOf(JobApplication application)` uses `resumeStorageKeySnapshot + submittedAt + resumeFileSizeSnapshot`
- `openArtifactIfPresent(...)` converts `FileNotFoundException` to cache miss
- `previewFileName(...)` converts `*.docx` to `*.pdf`
- `isPdf(...)` and `isDocx(...)` accept both file extension and content type
- do not use `updatedAt` in the fingerprint

- [ ] **Step 4: Re-run the targeted snapshot-preview-service tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ApplicationSnapshotPreviewServiceTests" test
```

Expected: PASS with:

- `PDF / DOCX / DOC` preview-kind mapping correct
- `DOCX` artifact keys derived from snapshot-specific fingerprint fields
- cache reuse on repeated `DOCX` preview
- cache write on miss
- unsupported types and conversion failures translated to clear business errors

- [ ] **Step 5: Commit the snapshot-preview-service foundation**

```bash
git add backend/src/main/java/com/campus/preview/ApplicationSnapshotPreviewService.java backend/src/test/java/com/campus/preview/ApplicationSnapshotPreviewServiceTests.java
git commit -m "feat: add application snapshot preview service"
```

## Task 2: Wire Applicant-Facing Snapshot Preview, Download, And Preview Metadata

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/JobApplicationController.java`
- Modify: `backend/src/main/java/com/campus/service/JobApplicationService.java`
- Modify: `backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java`
- Modify: `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
- Modify: `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`

- [ ] **Step 1: Write the failing applicant-facing controller regressions**

Extend `JobApplicationControllerTests`:

1. Extend the existing applicant list assertion to prove preview metadata is present:

```java
mockMvc.perform(get("/api/applications/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.applications[0].previewAvailable").value(true))
        .andExpect(jsonPath("$.data.applications[0].previewKind").value("FILE"));
```

2. Add applicant snapshot preview/download regressions:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void applicantCanPreviewOwnPdfSnapshotInline() throws Exception {
    long applicationId = insertApplication(1L, 2L, 9001L, "Intern Resume", "intern-resume.pdf", "application/pdf",
            "seed/applications/intern-resume.pdf");
    writeStoredFile("seed/applications/intern-resume.pdf", "%PDF-snapshot");

    mockMvc.perform(get("/api/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void applicantCanPreviewOwnDocxSnapshotAsPdf() throws Exception {
    long applicationId = insertApplication(1L, 2L, 9001L, "Intern Resume", "intern-resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "seed/applications/intern-resume.docx");
    writeStoredFile("seed/applications/intern-resume.docx", "docx");

    mockMvc.perform(get("/api/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void applicantCanDownloadOwnDocSnapshot() throws Exception {
    long applicationId = insertApplication(1L, 2L, 9001L, "Intern Resume", "intern-resume.doc", "application/msword",
            "seed/applications/intern-resume.doc");
    writeStoredFile("seed/applications/intern-resume.doc", "doc");

    mockMvc.perform(get("/api/applications/{id}/resume/download", applicationId))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/msword")));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void docSnapshotPreviewIsRejected() throws Exception {
    long applicationId = insertApplication(1L, 2L, 9001L, "Intern Resume", "intern-resume.doc", "application/msword",
            "seed/applications/intern-resume.doc");

    mockMvc.perform(get("/api/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("application resume preview only supports pdf or docx"));
}

@Test
@WithMockUser(username = "3", roles = "USER")
void applicantCannotPreviewOrDownloadAnotherUsersSnapshot() throws Exception {
    long applicationId = insertApplication(1L, 2L, 9001L, "Intern Resume", "intern-resume.pdf", "application/pdf",
            "seed/applications/intern-resume.pdf");

    mockMvc.perform(get("/api/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("application not found"));

    mockMvc.perform(get("/api/applications/{id}/resume/download", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("application not found"));
}
```

- [ ] **Step 2: Run the targeted applicant controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobApplicationControllerTests" test
```

Expected: FAIL because the applicant snapshot preview/download endpoints and list preview metadata do not exist yet.

- [ ] **Step 3: Implement the applicant-facing snapshot contract**

Update `MyJobApplicationListResponse`:

```java
public record MyJobApplicationListResponse(
        int total,
        List<ApplicationItem> applications) {

    public record ApplicationItem(
            Long id,
            Long jobId,
            String jobTitle,
            String companyName,
            String city,
            String status,
            String resumeTitleSnapshot,
            String resumeFileNameSnapshot,
            boolean previewAvailable,
            ResourcePreviewKind previewKind,
            LocalDateTime submittedAt) {
    }
}
```

Extend the applicant list query in `JobApplicationMapper` so row mapping has the snapshot metadata needed for preview decisions:

```java
SELECT a.id,
       a.job_id AS jobId,
       j.title AS jobTitle,
       j.company_name AS companyName,
       j.city AS city,
       a.status,
       a.resume_title_snapshot AS resumeTitleSnapshot,
       a.resume_file_name_snapshot AS resumeFileNameSnapshot,
       a.resume_file_ext_snapshot AS resumeFileExtSnapshot,
       a.resume_content_type_snapshot AS resumeContentTypeSnapshot,
       a.submitted_at AS submittedAt
```

Update `JobApplicationService`:

- inject `ApplicationSnapshotPreviewService`
- add `requireOwnedApplication(...)`
- add applicant download method
- add applicant preview method
- make `toMyApplicationItem(...)` include preview metadata

Recommended shape:

```java
public DownloadedApplicationResume downloadSnapshot(String identity, Long applicationId) {
    User applicant = userService.requireByIdentity(identity);
    JobApplication application = requireOwnedApplication(applicant.getId(), applicationId);
    return openSnapshot(application, "application resume snapshot unavailable");
}

public ApplicationSnapshotPreviewService.PreviewFile previewSnapshot(String identity, Long applicationId) {
    User applicant = userService.requireByIdentity(identity);
    JobApplication application = requireOwnedApplication(applicant.getId(), applicationId);
    return applicationSnapshotPreviewService.preview(application,
            () -> openSnapshotInputStream(application, "application resume preview unavailable"));
}

private MyJobApplicationListResponse.ApplicationItem toMyApplicationItem(JobApplicationMapper.MyApplicationRow row) {
    ResourcePreviewKind previewKind = applicationSnapshotPreviewService.previewKindOf(
            row.getResumeFileExtSnapshot(),
            row.getResumeContentTypeSnapshot());
    return new MyJobApplicationListResponse.ApplicationItem(
            row.getId(),
            row.getJobId(),
            row.getJobTitle(),
            row.getCompanyName(),
            row.getCity(),
            row.getStatus(),
            row.getResumeTitleSnapshot(),
            row.getResumeFileNameSnapshot(),
            previewKind != ResourcePreviewKind.NONE,
            previewKind,
            row.getSubmittedAt());
}
```

Update `JobApplicationController` with the new endpoints using the same response style as `ResumeController`:

```java
@GetMapping("/{id}/resume/download")
public ResponseEntity<InputStreamResource> downloadResume(@PathVariable Long id, Authentication authentication) {
    JobApplicationService.DownloadedApplicationResume download =
            jobApplicationService.downloadSnapshot(authentication.getName(), id);
    return ResponseEntity.ok()
            .contentType(resolveMediaType(download.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename(download.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString())
            .body(new InputStreamResource(download.inputStream()));
}

@GetMapping("/{id}/resume/preview")
public ResponseEntity<InputStreamResource> previewResume(@PathVariable Long id, Authentication authentication) {
    ApplicationSnapshotPreviewService.PreviewFile preview =
            jobApplicationService.previewSnapshot(authentication.getName(), id);
    return ResponseEntity.ok()
            .contentType(resolveMediaType(preview.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename(preview.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString())
            .body(new InputStreamResource(preview.inputStream()));
}
```

- [ ] **Step 4: Re-run the targeted applicant controller tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobApplicationControllerTests" test
```

Expected: PASS with:

- preview metadata present in `/api/applications/mine`
- applicant `PDF` snapshots preview inline
- applicant `DOCX` snapshots preview as `application/pdf`
- applicant `DOC` snapshots stay downloadable
- applicant preview rejects unsupported `DOC`
- ownership boundary enforced for both preview and download

- [ ] **Step 5: Commit the applicant-facing backend contract**

```bash
git add backend/src/main/java/com/campus/controller/JobApplicationController.java backend/src/main/java/com/campus/service/JobApplicationService.java backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java backend/src/main/java/com/campus/mapper/JobApplicationMapper.java backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java
git commit -m "feat: add applicant snapshot preview endpoints"
```

## Task 3: Wire Admin Snapshot Preview And Admin Preview Metadata

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java`
- Modify: `backend/src/main/java/com/campus/service/AdminJobApplicationService.java`
- Modify: `backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java`
- Modify: `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`

- [ ] **Step 1: Write the failing admin controller regressions**

Extend `AdminJobApplicationControllerTests`:

1. Add non-admin preview rejection:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void nonAdminCannotPreviewApplicationResumeSnapshot() throws Exception {
    mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", 1L))
            .andExpect(status().isForbidden());
}
```

2. Extend the admin list assertion to verify preview metadata:

```java
mockMvc.perform(get("/api/admin/applications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.applications[0].previewAvailable").value(true))
        .andExpect(jsonPath("$.data.applications[0].previewKind").value("FILE"));
```

3. Add admin preview regressions:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanPreviewPdfSnapshotInline() throws Exception {
    long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
            "seed/resumes/intern-resume.pdf");
    long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf",
            "application/pdf", "seed/applications/intern-resume.pdf");
    writeStoredFile("seed/applications/intern-resume.pdf", "%PDF-snapshot");

    mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanPreviewDocxSnapshotAsPdf() throws Exception {
    long resumeId = insertResume(2L, "Intern Resume", "intern-resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "seed/resumes/intern-resume.docx");
    long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "seed/applications/intern-resume.docx");
    writeStoredFile("seed/applications/intern-resume.docx", "docx");

    mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminDocSnapshotPreviewIsRejected() throws Exception {
    long resumeId = insertResume(2L, "Intern Resume", "intern-resume.doc", "application/msword",
            "seed/resumes/intern-resume.doc");
    long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.doc",
            "application/msword", "seed/applications/intern-resume.doc");

    mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("application resume preview only supports pdf or docx"));
}
```

- [ ] **Step 2: Run the targeted admin controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobApplicationControllerTests" test
```

Expected: FAIL because the admin snapshot preview endpoint and admin preview metadata do not exist yet.

- [ ] **Step 3: Implement the admin snapshot preview contract**

Update `AdminJobApplicationListResponse`:

```java
public record AdminJobApplicationListResponse(
        int total,
        int submittedToday,
        int uniqueApplicants,
        int uniqueJobs,
        List<ApplicationItem> applications) {

    public record ApplicationItem(
            Long id,
            Long jobId,
            String jobTitle,
            String companyName,
            Long applicantUserId,
            String applicantNickname,
            String resumeFileNameSnapshot,
            boolean previewAvailable,
            ResourcePreviewKind previewKind,
            String status,
            LocalDateTime submittedAt) {
    }
}
```

Extend the admin list query in `JobApplicationMapper`:

```java
SELECT a.id,
       a.job_id AS jobId,
       j.title AS jobTitle,
       j.company_name AS companyName,
       a.applicant_user_id AS applicantUserId,
       u.nickname AS applicantNickname,
       a.resume_file_name_snapshot AS resumeFileNameSnapshot,
       a.resume_file_ext_snapshot AS resumeFileExtSnapshot,
       a.resume_content_type_snapshot AS resumeContentTypeSnapshot,
       a.status,
       a.submitted_at AS submittedAt
```

Update `AdminJobApplicationService`:

- inject `ApplicationSnapshotPreviewService`
- add admin preview method
- make `toAdminApplicationItem(...)` include preview metadata

Recommended shape:

```java
public ApplicationSnapshotPreviewService.PreviewFile previewResumeSnapshot(Long applicationId) {
    JobApplication application = requireApplication(applicationId);
    return applicationSnapshotPreviewService.preview(application,
            () -> openSnapshotInputStream(application, "application resume preview unavailable"));
}

private AdminJobApplicationListResponse.ApplicationItem toAdminApplicationItem(
        JobApplicationMapper.AdminApplicationRow row) {
    ResourcePreviewKind previewKind = applicationSnapshotPreviewService.previewKindOf(
            row.getResumeFileExtSnapshot(),
            row.getResumeContentTypeSnapshot());
    return new AdminJobApplicationListResponse.ApplicationItem(
            row.getId(),
            row.getJobId(),
            row.getJobTitle(),
            row.getCompanyName(),
            row.getApplicantUserId(),
            row.getApplicantNickname(),
            row.getResumeFileNameSnapshot(),
            previewKind != ResourcePreviewKind.NONE,
            previewKind,
            row.getStatus(),
            row.getSubmittedAt());
}
```

Update `AdminJobApplicationController`:

```java
@GetMapping("/{id}/resume/preview")
public ResponseEntity<InputStreamResource> previewResume(@PathVariable Long id) {
    ApplicationSnapshotPreviewService.PreviewFile preview =
            adminJobApplicationService.previewResumeSnapshot(id);
    return ResponseEntity.ok()
            .contentType(resolveMediaType(preview.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                    .filename(preview.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString())
            .body(new InputStreamResource(preview.inputStream()));
}
```

- [ ] **Step 4: Re-run the targeted admin controller tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobApplicationControllerTests" test
```

Expected: PASS with:

- non-admin preview blocked by the admin boundary
- preview metadata present in `/api/admin/applications`
- admin `PDF` snapshots preview inline
- admin `DOCX` snapshots preview as `application/pdf`
- unsupported `DOC` preview rejected cleanly
- existing admin download behavior still green

- [ ] **Step 5: Commit the admin backend preview contract**

```bash
git add backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java backend/src/main/java/com/campus/service/AdminJobApplicationService.java backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java backend/src/main/java/com/campus/mapper/JobApplicationMapper.java backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java
git commit -m "feat: add admin snapshot preview endpoint"
```

## Task 4: Add Applicant Snapshot Preview And Download Actions To The Profile Applications Page

**Files:**
- Modify: `frontend/src/api/applications.js`
- Modify: `frontend/src/views/ProfileApplicationsView.vue`
- Modify: `frontend/src/views/ProfileApplicationsView.spec.js`

- [ ] **Step 1: Write the failing applicant frontend regressions**

Extend `ProfileApplicationsView.spec.js`:

1. Mock the new applicant snapshot helpers:

```javascript
import {
  getMyApplications,
  downloadMyApplicationResume,
  previewMyApplicationResume,
} from "../api/applications.js";

vi.mock("../api/applications.js", () => ({
  getMyApplications: vi.fn(),
  downloadMyApplicationResume: vi.fn(),
  previewMyApplicationResume: vi.fn(),
}));
```

2. Add action rendering coverage:

```javascript
test("pdf and docx snapshots show preview and download while doc stays download-only", async () => {
  getMyApplications.mockResolvedValue({
    total: 3,
    applications: [
      {
        id: 11,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "PDF Snapshot",
        resumeFileNameSnapshot: "intern.pdf",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:30:00",
      },
      {
        id: 12,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "DOCX Snapshot",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:35:00",
      },
      {
        id: 13,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "DOC Snapshot",
        resumeFileNameSnapshot: "intern.doc",
        previewAvailable: false,
        previewKind: "NONE",
        submittedAt: "2026-04-22T10:40:00",
      },
    ],
  });

  const wrapper = mount(ProfileApplicationsView, { /* existing RouterLink stub */ });
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-application-resume-11"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-12"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-13"]').exists()).toBe(false);
  expect(wrapper.find('[data-testid="download-application-resume-13"]').exists()).toBe(true);
});
```

3. Add action dispatch coverage:

```javascript
test("preview and download actions call the applicant snapshot helpers", async () => {
  getMyApplications.mockResolvedValue({
    total: 1,
    applications: [
      {
        id: 11,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        city: "Shenzhen",
        status: "SUBMITTED",
        resumeTitleSnapshot: "DOCX Snapshot",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        submittedAt: "2026-04-22T10:35:00",
      },
    ],
  });
  previewMyApplicationResume.mockResolvedValue("blob:application-preview");
  downloadMyApplicationResume.mockResolvedValue("intern.docx");

  const wrapper = mount(ProfileApplicationsView, { /* existing RouterLink stub */ });
  await flushPromises();

  await wrapper.find('[data-testid="preview-application-resume-11"]').trigger("click");
  await wrapper.find('[data-testid="download-application-resume-11"]').trigger("click");

  expect(previewMyApplicationResume).toHaveBeenCalledWith(11);
  expect(downloadMyApplicationResume).toHaveBeenCalledWith(11);
});
```

- [ ] **Step 2: Run the targeted applicant frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileApplicationsView.spec.js
```

Expected: FAIL because the new applicant snapshot helpers and UI actions do not exist yet.

- [ ] **Step 3: Implement the applicant frontend snapshot actions**

Update `frontend/src/api/applications.js`:

```javascript
export async function downloadMyApplicationResume(id) {
  const response = await http.get(`/applications/${id}/resume/download`, {
    responseType: "blob",
  });

  const filename = extractFilename(response.headers["content-disposition"]) || `application-resume-${id}`;
  const objectUrl = window.URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(objectUrl);
  return filename;
}

export async function previewMyApplicationResume(id) {
  const response = await http.get(`/applications/${id}/resume/preview`, {
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

Update `ProfileApplicationsView.vue`:

- import the new helpers
- add `actionMessage`, `actionError`, and `actionLoadingId`
- add `handlePreview(application)` and `handleDownload(application)`
- render buttons using backend-provided preview metadata
- update the page copy to mention snapshot preview/download

Recommended button block:

```vue
<div class="inline-form-actions">
  <button
    v-if="application.previewAvailable && application.previewKind === 'FILE'"
    :data-testid="`preview-application-resume-${application.id}`"
    type="button"
    class="ghost-btn"
    :disabled="actionLoadingId === `preview-${application.id}`"
    @click="handlePreview(application)"
  >
    {{ actionLoadingId === `preview-${application.id}` ? "Opening Preview..." : "Preview" }}
  </button>
  <button
    :data-testid="`download-application-resume-${application.id}`"
    type="button"
    class="ghost-btn"
    :disabled="actionLoadingId === `download-${application.id}`"
    @click="handleDownload(application)"
  >
    {{ actionLoadingId === `download-${application.id}` ? "Preparing Download..." : "Download" }}
  </button>
  <RouterLink :to="`/jobs/${application.jobId}`" class="app-link">
    Open Job Detail
  </RouterLink>
</div>
```

- [ ] **Step 4: Re-run the targeted applicant frontend tests and verify success**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileApplicationsView.spec.js
```

Expected: PASS with:

- preview buttons only on `PDF / DOCX` snapshots
- `DOC` snapshots staying download-only
- preview action calling `previewMyApplicationResume(id)`
- download action calling `downloadMyApplicationResume(id)`
- existing history rendering still green

- [ ] **Step 5: Commit the applicant frontend snapshot actions**

```bash
git add frontend/src/api/applications.js frontend/src/views/ProfileApplicationsView.vue frontend/src/views/ProfileApplicationsView.spec.js
git commit -m "feat: add snapshot actions to profile applications"
```

## Task 5: Add Snapshot Preview Action To The Admin Applications Workbench

**Files:**
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/admin/AdminApplicationsView.vue`
- Modify: `frontend/src/views/admin/AdminApplicationsView.spec.js`

- [ ] **Step 1: Write the failing admin frontend regressions**

Extend `AdminApplicationsView.spec.js`:

1. Mock the new preview helper:

```javascript
import {
  downloadAdminApplicationResume,
  getAdminApplications,
  previewAdminApplicationResume,
} from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  downloadAdminApplicationResume: vi.fn(),
  getAdminApplications: vi.fn(),
  previewAdminApplicationResume: vi.fn(),
}));
```

2. Add preview rendering coverage for desktop/mobile contracts:

```javascript
test("admin shows preview for previewable snapshots and hides it for doc", async () => {
  getAdminApplications.mockResolvedValue({
    total: 2,
    submittedToday: 2,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:30:00",
      },
      {
        id: 1002,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.doc",
        previewAvailable: false,
        previewKind: "NONE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:35:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, { /* existing RouterLink stub */ });
  await flushPromises();

  expect(wrapper.find('[data-testid="preview-application-resume-1001"]').exists()).toBe(true);
  expect(wrapper.find('[data-testid="preview-application-resume-1002"]').exists()).toBe(false);
});
```

3. Add preview dispatch coverage:

```javascript
test("preview action calls the admin snapshot preview helper", async () => {
  getAdminApplications.mockResolvedValue({
    total: 1,
    submittedToday: 1,
    uniqueApplicants: 1,
    uniqueJobs: 1,
    applications: [
      {
        id: 1001,
        jobId: 1,
        jobTitle: "Java Backend Intern",
        companyName: "Future Campus Tech",
        applicantUserId: 2,
        applicantNickname: "NormalUser",
        resumeFileNameSnapshot: "intern.docx",
        previewAvailable: true,
        previewKind: "FILE",
        status: "SUBMITTED",
        submittedAt: "2026-04-22T10:30:00",
      },
    ],
  });
  previewAdminApplicationResume.mockResolvedValue("blob:admin-application-preview");

  const wrapper = mount(AdminApplicationsView, { /* existing RouterLink stub */ });
  await flushPromises();

  await wrapper.find('[data-testid="preview-application-resume-1001"]').trigger("click");

  expect(previewAdminApplicationResume).toHaveBeenCalledWith(1001);
});
```

- [ ] **Step 2: Run the targeted admin frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminApplicationsView.spec.js
```

Expected: FAIL because the admin snapshot preview helper and preview button are not implemented yet.

- [ ] **Step 3: Implement the admin frontend preview action**

Update `frontend/src/api/admin.js`:

```javascript
export async function previewAdminApplicationResume(id) {
  const response = await http.get(`/admin/applications/${id}/resume/preview`, {
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

Update `AdminApplicationsView.vue`:

- import `previewAdminApplicationResume`
- add `handlePreview(application)`
- reserve `actionLoadingId = "preview-{id}"` while preview is opening
- render `Preview` only when `application.previewAvailable && application.previewKind === "FILE"`
- keep `Download Resume` and `Open Job` unchanged
- update the page copy from download-only language to preview/download language while keeping the board read-only

Recommended button block:

```vue
<button
  v-if="application.previewAvailable && application.previewKind === 'FILE'"
  :data-testid="`preview-application-resume-${application.id}`"
  type="button"
  class="ghost-btn"
  :disabled="actionLoadingId === `preview-${application.id}`"
  @click="handlePreview(application)"
>
  {{ actionLoadingId === `preview-${application.id}` ? "Opening Preview..." : "Preview" }}
</button>
```

Render the same preview button in both the desktop table and the mobile card layout.

- [ ] **Step 4: Re-run the targeted admin frontend tests and verify success**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminApplicationsView.spec.js
```

Expected: PASS with:

- preview buttons only on `PDF / DOCX` snapshots
- `DOC` snapshots staying download-only
- preview action calling `previewAdminApplicationResume(id)`
- existing admin download flow remaining green

- [ ] **Step 5: Commit the admin frontend preview action**

```bash
git add frontend/src/api/admin.js frontend/src/views/admin/AdminApplicationsView.vue frontend/src/views/admin/AdminApplicationsView.spec.js
git commit -m "feat: add snapshot preview to admin applications"
```

## Task 6: Update Docs And Record Validation

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-design.md`

- [ ] **Step 1: Update the README**

Apply the following documentation changes:

- add `Phase AA application snapshot online preview first slice` to the repository status line
- in `Implemented now`, update the application-related bullets so applicant and admin application boards are documented as supporting snapshot preview/download
- in the `Job Application And Resume Workflow` section:
  - add `GET /api/applications/{id}/resume/preview`
  - add `GET /api/applications/{id}/resume/download`
  - add `GET /api/admin/applications/{id}/resume/preview`
  - keep `GET /api/admin/applications/{id}/resume/download`
  - document `PDF` and `DOCX` snapshot preview on `/profile/applications` and `/admin/applications`
  - document `DOC` snapshots as download-only in this phase
- remove or revise the note that application snapshot preview remains out of scope

Suggested wording:

```markdown
- my applications history with snapshot preview / download
- admin applications read-only workbench with snapshot preview / download
```

```markdown
- authenticated users can preview their own `PDF` and `DOCX` application snapshots from `/profile/applications`
- authenticated users can also download their own stored snapshot files from `/profile/applications`
- admins can preview and download `PDF` and `DOCX` application snapshots from `/admin/applications`
- `DOC` snapshots remain download-only on both surfaces in this phase
```

- [ ] **Step 2: Add the Phase AA validation note to the spec**

Add a validation note near the top of the Phase AA spec:

```markdown
> **Validation note:** This design was implemented and validated on 2026-04-22 using the approved execution record at `docs/superpowers/plans/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-implementation.md`. Local verification suites now present for this slice are `ApplicationSnapshotPreviewServiceTests`, `JobApplicationControllerTests`, `AdminJobApplicationControllerTests`, `ProfileApplicationsView.spec.js`, and `AdminApplicationsView.spec.js`.
```

- [ ] **Step 3: Run the targeted verification suites**

Backend:

```powershell
cd backend
mvn -q "-Dtest=ApplicationSnapshotPreviewServiceTests,JobApplicationControllerTests,AdminJobApplicationControllerTests,ResumePreviewServiceTests,ResourcePreviewStorageConfigurationTests" test
```

Expected: PASS with:

- snapshot preview service green
- applicant snapshot preview/download green
- admin snapshot preview green
- existing resume preview service still green
- shared preview-storage configuration still green

Frontend:

```powershell
cd frontend
npx vitest run src/views/ProfileApplicationsView.spec.js src/views/admin/AdminApplicationsView.spec.js
```

Expected: PASS with the new applicant/admin preview button behavior and no regressions to download interactions.

- [ ] **Step 4: Commit the rollout notes**

```bash
git add README.md docs/superpowers/specs/2026-04-22-study-career-platform-phase-aa-application-snapshot-online-preview-design.md
git commit -m "docs: add phase aa snapshot preview rollout notes"
```

## Final Verification Checklist

- [ ] `backend/src/test/java/com/campus/preview/ApplicationSnapshotPreviewServiceTests.java`
- [ ] `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`
- [ ] `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`
- [ ] `backend/src/test/java/com/campus/preview/ResumePreviewServiceTests.java`
- [ ] `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- [ ] `frontend/src/views/ProfileApplicationsView.spec.js`
- [ ] `frontend/src/views/admin/AdminApplicationsView.spec.js`

## Execution Notes

- Keep Phase AA narrow: no live-resume behavior changes, no application mutation flow, and no generic private-document preview framework.
- Keep `DOC` snapshot preview out of scope even if `soffice` could theoretically support it.
- Reuse `ResourcePreviewKind` and do not introduce a snapshot-only preview enum.
- Do not use mutable `updatedAt` as part of the snapshot preview fingerprint.
- Because application snapshots are immutable and undeletable in this phase, do not add preview-artifact cleanup hooks.
- Use `apply_patch` for manual edits.
- Do not add new environment variables in this phase.
