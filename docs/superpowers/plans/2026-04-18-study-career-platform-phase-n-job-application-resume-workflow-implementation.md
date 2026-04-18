# Study-Career Platform Phase N Job Application And Resume Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a user-owned resume library, in-platform one-time job application flow, applicant history page, and admin read-only applications workbench with stable resume snapshots.

**Architecture:** Build Phase N contract-first in two backend slices and two frontend slices. First add the resume domain and its storage-backed CRUD endpoints, then add the application domain with immutable resume snapshots and job-detail applied-state enrichment, then ship the applicant profile pages and finally wire the job-detail apply surface plus admin applications workbench. Keep raw file persistence on the existing `ResourceFileStorage` abstraction, but do not reuse resource-library review semantics.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, JUnit 5, MockMvc, JdbcTemplate, Vue 3, Vue Router, Pinia, Axios, Vitest

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-18-study-career-platform-phase-n-job-application-resume-workflow-design.md`
- Existing jobs backend:
  - `backend/src/main/java/com/campus/controller/JobController.java`
  - `backend/src/main/java/com/campus/service/JobService.java`
  - `backend/src/main/java/com/campus/dto/JobDetailResponse.java`
  - `backend/src/test/java/com/campus/controller/JobControllerTests.java`
- Existing file-storage baseline:
  - `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
  - `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
  - `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
  - `backend/src/main/java/com/campus/storage/StorageKeyFactory.java`
- Existing multipart + download controller patterns:
  - `backend/src/main/java/com/campus/controller/ResourceController.java`
  - `backend/src/main/java/com/campus/service/ResourceService.java`
  - `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Existing admin workbench patterns:
  - `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
  - `backend/src/main/java/com/campus/service/AdminJobService.java`
  - `frontend/src/views/admin/AdminJobManageView.vue`
  - `frontend/src/api/admin.js`
- Existing profile workspace patterns:
  - `frontend/src/views/ProfileView.vue`
  - `frontend/src/views/ProfileResourcesView.vue`
  - `frontend/src/views/ProfileResourcesView.spec.js`
- Existing job detail frontend surface:
  - `frontend/src/views/JobDetailView.vue`
  - `frontend/src/views/JobDetailView.spec.js`
  - `frontend/src/api/jobs.js`
- Existing route and nav patterns:
  - `frontend/src/router/index.js`
  - `frontend/src/components/NavBar.vue`
  - `frontend/src/components/NavBar.spec.js`
- Existing schema and seed baseline:
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/resources/data.sql`

## Scope Lock

This plan covers only the approved Phase N slice:

- authenticated users can manage a multi-resume library
- supported resume formats are `PDF / DOC / DOCX`
- published job detail pages expose `Apply In Platform`
- each user can apply to a given job at most once
- applications create immutable resume snapshots
- authenticated users can view `/profile/applications`
- admins can view `/admin/applications`
- admin applications page is read-only

This plan explicitly does not implement:

- recruiter or employer accounts
- application status progression beyond `SUBMITTED`
- resume rename, replace, preview, or version history
- cover letters or free-text application messages
- homepage or admin-dashboard application summaries
- exports, analytics, or notification flows for applications

## Frontend Design Baseline

All UI tasks in this plan must explicitly use these skills before shipping:

- `@frontend-design`
  - preserve the current editorial desk language
  - keep profile/application views as calm boards, not generic SaaS tables
- `@ui-ux-pro-max`
  - review hierarchy, empty states, action clarity, and mobile behavior before closing UI tasks

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/JobApplicationStatus.java`
  Single-status enum for Phase N application lifecycle.
- Create: `backend/src/main/java/com/campus/entity/Resume.java`
  User-owned resume record.
- Create: `backend/src/main/java/com/campus/entity/JobApplication.java`
  Application record with immutable resume snapshot fields.
- Create: `backend/src/main/java/com/campus/mapper/ResumeMapper.java`
  MyBatis-Plus mapper for resumes.
- Create: `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
  MyBatis-Plus mapper for applications.
- Create: `backend/src/main/java/com/campus/dto/ResumeRecordResponse.java`
  Single resume item response.
- Create: `backend/src/main/java/com/campus/dto/ResumeListResponse.java`
  Current-user resume list response.
- Create: `backend/src/main/java/com/campus/dto/ApplyJobRequest.java`
  Request body for selecting one resume during apply.
- Create: `backend/src/main/java/com/campus/dto/JobApplicationRecordResponse.java`
  Apply-success response payload.
- Create: `backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java`
  Applicant history response.
- Create: `backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java`
  Admin applications workbench response with counts and rows.
- Create: `backend/src/main/java/com/campus/service/ResumeService.java`
  Resume upload/list/download/delete behavior.
- Create: `backend/src/main/java/com/campus/service/JobApplicationService.java`
  Apply flow, duplicate guard, snapshot copy, and my-applications list.
- Create: `backend/src/main/java/com/campus/service/AdminJobApplicationService.java`
  Admin read-only applications list and snapshot download.
- Create: `backend/src/main/java/com/campus/controller/ResumeController.java`
  Resume-library endpoints.
- Create: `backend/src/main/java/com/campus/controller/JobApplicationController.java`
  `/api/applications/mine` endpoint.
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java`
  Admin list and snapshot-download endpoints.
- Create: `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`

### Backend: Modify Existing

- Modify: `backend/src/main/resources/schema.sql`
  Add `t_resume` and `t_job_application` tables and drop order.
- Modify: `backend/src/main/java/com/campus/controller/JobController.java`
  Add `POST /api/jobs/{id}/apply`.
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
  Enrich job detail with current-user applied state.
- Modify: `backend/src/main/java/com/campus/dto/JobDetailResponse.java`
  Add `appliedByMe` and optional `applicationId`.
- Modify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`
  Pin `appliedByMe` behavior.

### Frontend: Create

- Create: `frontend/src/api/resumes.js`
  Resume-library API adapter and download helper.
- Create: `frontend/src/api/applications.js`
  Applicant-history API adapter.
- Create: `frontend/src/views/ProfileResumesView.vue`
  Resume-library page with upload/list/download/delete.
- Create: `frontend/src/views/ProfileResumesView.spec.js`
- Create: `frontend/src/views/ProfileApplicationsView.vue`
  Applicant history board.
- Create: `frontend/src/views/ProfileApplicationsView.spec.js`
- Create: `frontend/src/views/admin/AdminApplicationsView.vue`
  Read-only admin applications workbench.
- Create: `frontend/src/views/admin/AdminApplicationsView.spec.js`

### Frontend: Modify Existing

- Modify: `frontend/src/api/jobs.js`
  Add `applyToJob`.
- Modify: `frontend/src/api/admin.js`
  Add admin applications list and snapshot-download helpers.
- Modify: `frontend/src/router/index.js`
  Add `/profile/resumes`, `/profile/applications`, and `/admin/applications`.
- Modify: `frontend/src/views/ProfileView.vue`
  Add workspace links to the new resume and application pages.
- Modify: `frontend/src/views/ProfileView.spec.js`
  Cover the new profile workspace links.
- Modify: `frontend/src/views/JobDetailView.vue`
  Add the inline apply surface while keeping `Source Link`.
- Modify: `frontend/src/views/JobDetailView.spec.js`
  Cover guest redirect, no-resume guidance, apply success, and applied state.
- Modify: `frontend/src/components/NavBar.vue`
  Add admin applications nav link.
- Modify: `frontend/src/components/NavBar.spec.js`
  Cover admin-only applications nav behavior.

### Docs: Modify Existing

- Modify: `README.md`
  Document Phase N routes, endpoints, permissions, and verification path.

## Responsibility Notes

- `ResumeService` owns resume file validation, ownership checks, upload, list shaping, download, and delete semantics.
- `JobApplicationService` owns duplicate-apply rules and immutable snapshot creation.
- `JobService` stays responsible for job visibility and detail shaping; it only reads application state.
- Resume and application files must use `ResourceFileStorage`; do not route them through `ResourceService`.
- Resume deletion must never delete application snapshot files.
- Resume deletion order is explicit in this phase:
  - delete the `t_resume` row first
  - then attempt best-effort deletion of the original resume file
  - if file deletion fails after row removal, log a warning and preserve user-visible success
- `AdminJobApplicationService` is read-only in this phase and must not introduce status mutation behavior.
- `JobDetailView.vue` must keep the existing external `Source Link`.
- The admin applications workbench may expose only the four spec-recommended metrics:
  - all applications
  - submitted today
  - unique applicants
  - unique jobs
  - do not add any extra analytics or trend reporting

## Task 1: Add The Resume Persistence Baseline And Backend Resume Library

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/java/com/campus/entity/Resume.java`
- Create: `backend/src/main/java/com/campus/mapper/ResumeMapper.java`
- Create: `backend/src/main/java/com/campus/dto/ResumeRecordResponse.java`
- Create: `backend/src/main/java/com/campus/dto/ResumeListResponse.java`
- Create: `backend/src/main/java/com/campus/service/ResumeService.java`
- Create: `backend/src/main/java/com/campus/controller/ResumeController.java`
- Create: `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`

- [ ] **Step 1: Write the failing resume controller tests**

Create `ResumeControllerTests.java` using the same integration style as `ResourceControllerTests.java`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResumeControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanUploadListDownloadAndDeleteOwnResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-resume.pdf",
                "application/pdf",
                "resume".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("title", "Intern Resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Intern Resume"))
                .andExpect(jsonPath("$.data.fileName").value("campus-resume.pdf"));

        Long resumeId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_resume WHERE user_id = 2 AND title = 'Intern Resume'",
                Long.class);
        String storageKey = jdbcTemplate.queryForObject(
                "SELECT storage_key FROM t_resume WHERE id = ?",
                String.class,
                resumeId);

        assertThat(Files.exists(STORAGE_ROOT.resolve(storageKey))).isTrue();

        mockMvc.perform(get("/api/resumes/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.resumes[0].title").value("Intern Resume"));

        mockMvc.perform(get("/api/resumes/{id}/download", resumeId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("campus-resume.pdf")));

        mockMvc.perform(delete("/api/resumes/{id}", resumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_resume WHERE id = ?", Integer.class, resumeId);
        assertThat(count).isEqualTo(0);
    }
}
```

Add a second failing test for unsupported file type:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void resumeUploadRejectsUnsupportedFileTypes() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "portfolio.zip",
            "application/zip",
            "zip".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/resumes")
                    .file(file)
                    .param("title", "Zip Resume"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("unsupported resume file type"));
}
```

Add one more failing test for owner boundary:

```java
@Test
@WithMockUser(username = "3", roles = "USER")
void userCannotDeleteAnotherUsersResume() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resume (
                      id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            9001L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");

    mockMvc.perform(delete("/api/resumes/9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("resume not found"));
}
```

Add one more failing test for configured size-limit enforcement:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void resumeUploadRejectsFilesAboveConfiguredMultipartLimit() throws Exception {
    byte[] payload = new byte[6 * 1024 * 1024];
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "oversized-resume.pdf",
            "application/pdf",
            payload);

    mockMvc.perform(multipart("/api/resumes")
                    .file(file)
                    .param("title", "Too Large Resume"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("resume file is too large"));
}
```

- [ ] **Step 2: Run the targeted resume backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumeControllerTests" test
```

Expected: FAIL because the resume table, entity, mapper, service, and controller do not exist yet.

- [ ] **Step 3: Implement the resume table and backend library**

Update `backend/src/main/resources/schema.sql` so it drops and creates `t_resume`:

```sql
DROP TABLE IF EXISTS t_job_application;
DROP TABLE IF EXISTS t_resume;
```

Add the table:

```sql
CREATE TABLE t_resume (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(20) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Create `Resume.java`:

```java
@Data
@TableName("t_resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String fileName;
    private String fileExt;
    private String contentType;
    private Long fileSize;
    private String storageKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Create the DTOs:

```java
public record ResumeRecordResponse(
        Long id,
        String title,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
```

```java
public record ResumeListResponse(
        int total,
        List<ResumeRecordResponse> resumes) {
}
```

Implement `ResumeService`:

```java
@Service
public class ResumeService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final ResumeMapper resumeMapper;
    private final UserService userService;
    private final ResourceFileStorage resourceFileStorage;
    private final MultipartProperties multipartProperties;

    public ResumeRecordResponse upload(String identity, String title, MultipartFile file) {
        User viewer = userService.requireByIdentity(identity);
        String normalizedTitle = requireText(title, "title");
        ValidatedResumeFile validated = validateFile(file);
        String storageKey = storeValidatedFile(validated, file);

        Resume resume = new Resume();
        resume.setUserId(viewer.getId());
        resume.setTitle(normalizedTitle);
        resume.setFileName(validated.originalFilename());
        resume.setFileExt(validated.extension());
        resume.setContentType(validated.contentType());
        resume.setFileSize(validated.size());
        resume.setStorageKey(storageKey);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        return toRecord(resume);
    }

    public ResumeListResponse listMine(String identity) { /* select by user_id order by created_at desc, id desc */ }

    public DownloadedResume download(String identity, Long resumeId) { /* owner-only + attachment */ }

    @Transactional
    public void delete(String identity, Long resumeId) {
        Resume resume = requireOwnedResume(identity, resumeId);
        String storageKey = resume.getStorageKey();
        resumeMapper.deleteById(resumeId);
        tryDeleteStoredFile(storageKey);
    }

    private void validateFileSize(MultipartFile file) {
        DataSize configuredLimit = multipartProperties.getMaxFileSize();
        long maxBytes = configuredLimit == null ? Long.MAX_VALUE : configuredLimit.toBytes();
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, "resume file is too large");
        }
    }
}
```

Validation rules to pin:

- title required
- file required
- only `pdf`, `doc`, `docx`
- file size must respect the configured multipart limit
- blank filename rejected
- error message:
  - `unsupported resume file type`
  - `resume file is too large`

Create `ResumeController`:

```java
@Validated
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/mine")
    public Result<ResumeListResponse> mine(Authentication authentication) {
        return Result.success(resumeService.listMine(authentication.getName()));
    }

    @PostMapping
    public Result<ResumeRecordResponse> upload(
            @RequestParam String title,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return Result.success(resumeService.upload(authentication.getName(), title, file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id, Authentication authentication) {
        DownloadedResume download = resumeService.download(authentication.getName(), id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id, Authentication authentication) {
        resumeService.delete(authentication.getName(), id);
        return Result.success(true);
    }
}
```

- [ ] **Step 4: Re-run the targeted resume backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumeControllerTests" test
```

Expected: PASS with owner-only upload/list/download/delete and file-type validation.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/java/com/campus/entity/Resume.java backend/src/main/java/com/campus/mapper/ResumeMapper.java backend/src/main/java/com/campus/dto/ResumeRecordResponse.java backend/src/main/java/com/campus/dto/ResumeListResponse.java backend/src/main/java/com/campus/service/ResumeService.java backend/src/main/java/com/campus/controller/ResumeController.java backend/src/test/java/com/campus/controller/ResumeControllerTests.java
git commit -m "feat: add user resume library backend"
```

## Task 2: Add Backend Job Application Records, Snapshot Copy, And Job Detail Applied State

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/java/com/campus/common/JobApplicationStatus.java`
- Create: `backend/src/main/java/com/campus/entity/JobApplication.java`
- Create: `backend/src/main/java/com/campus/mapper/JobApplicationMapper.java`
- Create: `backend/src/main/java/com/campus/dto/ApplyJobRequest.java`
- Create: `backend/src/main/java/com/campus/dto/JobApplicationRecordResponse.java`
- Create: `backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java`
- Create: `backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java`
- Create: `backend/src/main/java/com/campus/service/JobApplicationService.java`
- Create: `backend/src/main/java/com/campus/service/AdminJobApplicationService.java`
- Create: `backend/src/main/java/com/campus/controller/JobApplicationController.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java`
- Modify: `backend/src/main/java/com/campus/controller/JobController.java`
- Modify: `backend/src/main/java/com/campus/service/JobService.java`
- Modify: `backend/src/main/java/com/campus/dto/JobDetailResponse.java`
- Create: `backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`

- [ ] **Step 1: Write the failing application backend tests**

Create `JobApplicationControllerTests.java`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JobApplicationControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void userCanApplyOnceAndListOwnApplications() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "resume-pdf");

        mockMvc.perform(post("/api/jobs/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobId").value(1))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.resumeTitleSnapshot").value("Intern Resume"));

        mockMvc.perform(get("/api/applications/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.applications[0].jobId").value(1))
                .andExpect(jsonPath("$.data.applications[0].jobTitle").value("Java Backend Intern"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void duplicateApplyToSameJobIsRejected() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "resume-pdf");
        insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/applications/a.pdf");

        mockMvc.perform(post("/api/jobs/1/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("already applied to this job"));
    }
}
```

Add a second test for resume ownership:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void applyingWithAnotherUsersResumeIsRejected() throws Exception {
    long resumeId = insertResume(3L, "Foreign Resume", "foreign.pdf", "application/pdf", "seed/resumes/foreign.pdf");

    mockMvc.perform(post("/api/jobs/1/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"resumeId": %d}
                            """.formatted(resumeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("resume not found"));
}
```

Add one more test for non-published jobs:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void applyingToNonPublishedJobIsRejected() throws Exception {
    long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/resumes/intern-resume.pdf");

    mockMvc.perform(post("/api/jobs/3/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"resumeId": %d}
                            """.formatted(resumeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("job not found"));
}
```

Create `AdminJobApplicationControllerTests.java`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminJobApplicationControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotReadAdminApplicationsWorkbench() throws Exception {
        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanReadApplicationsWorkbenchAndDownloadSnapshot() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/resumes/intern-resume.pdf");
        insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/applications/intern-resume-snapshot.pdf");
        writeStoredFile("seed/applications/intern-resume-snapshot.pdf", "snapshot");

        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.uniqueApplicants").value(1))
                .andExpect(jsonPath("$.data.applications[0].jobTitle").value("Java Backend Intern"))
                .andExpect(jsonPath("$.data.applications[0].applicantNickname").value("NormalUser"));

        mockMvc.perform(get("/api/admin/applications/1/resume/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("intern-resume.pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void snapshotDownloadStillWorksAfterOriginalResumeDeletion() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf", "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "original");

        mockMvc.perform(post("/api/jobs/1/apply")
                        .with(user("2").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/resumes/{id}", resumeId)
                        .with(user("2").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Long applicationId = jdbcTemplate.queryForObject("SELECT id FROM t_job_application WHERE job_id = 1 AND applicant_user_id = 2", Long.class);

        mockMvc.perform(get("/api/admin/applications/{id}/resume/download", applicationId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("intern-resume.pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminApplicationsWorkbenchExposesNoMutationRoute() throws Exception {
        mockMvc.perform(post("/api/admin/applications/1/review"))
                .andExpect(status().isNotFound());
    }
}
```

Modify `JobControllerTests.java` with one failing applied-state test:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void authenticatedUserSeesAppliedStateInJobDetail() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_job_application (
                      id, job_id, applicant_user_id, resume_id, status,
                      resume_title_snapshot, resume_file_name_snapshot, resume_file_ext_snapshot,
                      resume_content_type_snapshot, resume_file_size_snapshot, resume_storage_key_snapshot,
                      submitted_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """,
            7001L, 1L, 2L, 9001L, "SUBMITTED",
            "Intern Resume", "intern-resume.pdf", "pdf", "application/pdf", 100L, "seed/applications/intern-resume-snapshot.pdf");

    mockMvc.perform(get("/api/jobs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.appliedByMe").value(true))
            .andExpect(jsonPath("$.data.applicationId").value(7001));
}
```

- [ ] **Step 2: Run the targeted application backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobApplicationControllerTests,AdminJobApplicationControllerTests,JobControllerTests" test
```

Expected: FAIL because the application table, DTOs, services, controllers, and job-detail applied-state fields do not exist yet.

- [ ] **Step 3: Implement the application backend and job-detail enrichment**

Update `schema.sql` to add `t_job_application`:

```sql
CREATE TABLE t_job_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  applicant_user_id BIGINT NOT NULL,
  resume_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  resume_title_snapshot VARCHAR(120) NOT NULL,
  resume_file_name_snapshot VARCHAR(255) NOT NULL,
  resume_file_ext_snapshot VARCHAR(20) NOT NULL,
  resume_content_type_snapshot VARCHAR(120) NOT NULL,
  resume_file_size_snapshot BIGINT NOT NULL,
  resume_storage_key_snapshot VARCHAR(500) NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_job_application_job_user (job_id, applicant_user_id),
  KEY idx_job_application_applicant (applicant_user_id),
  KEY idx_job_application_submitted (submitted_at)
);
```

Create the enum and entity:

```java
public enum JobApplicationStatus {
    SUBMITTED
}
```

```java
@Data
@TableName("t_job_application")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long applicantUserId;
    private Long resumeId;
    private String status;
    private String resumeTitleSnapshot;
    private String resumeFileNameSnapshot;
    private String resumeFileExtSnapshot;
    private String resumeContentTypeSnapshot;
    private Long resumeFileSizeSnapshot;
    private String resumeStorageKeySnapshot;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Create request/response DTOs:

```java
public record ApplyJobRequest(Long resumeId) {
}
```

```java
public record JobApplicationRecordResponse(
        Long id,
        Long jobId,
        String status,
        Long resumeId,
        String resumeTitleSnapshot,
        String resumeFileNameSnapshot,
        LocalDateTime submittedAt) {
}
```

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
            LocalDateTime submittedAt) {
    }
}
```

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
            String status,
            LocalDateTime submittedAt) {
    }
}
```

Implement `JobApplicationService.apply` so it:

- requires authenticated user
- requires `resumeId`
- requires published job
- requires owned resume
- rejects duplicate application
- copies a new snapshot file before inserting the row

```java
@Transactional
public JobApplicationRecordResponse apply(String identity, Long jobId, ApplyJobRequest request) {
    User applicant = userService.requireByIdentity(identity);
    Long resumeId = request.resumeId();
    if (resumeId == null) {
        throw new BusinessException(400, "resume is required");
    }

    JobPosting job = requirePublishedJob(jobId);
    Resume resume = resumeService.requireOwnedResume(identity, resumeId);

    if (hasExistingApplication(job.getId(), applicant.getId())) {
        throw new BusinessException(400, "already applied to this job");
    }

    String snapshotStorageKey = copyResumeSnapshot(resume);
    LocalDateTime now = LocalDateTime.now();

    JobApplication application = new JobApplication();
    application.setJobId(job.getId());
    application.setApplicantUserId(applicant.getId());
    application.setResumeId(resume.getId());
    application.setStatus(JobApplicationStatus.SUBMITTED.name());
    application.setResumeTitleSnapshot(resume.getTitle());
    application.setResumeFileNameSnapshot(resume.getFileName());
    application.setResumeFileExtSnapshot(resume.getFileExt());
    application.setResumeContentTypeSnapshot(resume.getContentType());
    application.setResumeFileSizeSnapshot(resume.getFileSize());
    application.setResumeStorageKeySnapshot(snapshotStorageKey);
    application.setSubmittedAt(now);
    application.setCreatedAt(now);
    application.setUpdatedAt(now);
    try {
        jobApplicationMapper.insert(application);
    } catch (DuplicateKeyException exception) {
        tryDeleteSnapshotFile(snapshotStorageKey);
        throw new BusinessException(400, "already applied to this job");
    }
    return toRecord(application);
}
```

Snapshot copy helper to pin:

```java
private String copyResumeSnapshot(Resume resume) {
    try (InputStream inputStream = resourceFileStorage.open(resume.getStorageKey())) {
        return resourceFileStorage.store(resume.getFileName(), inputStream);
    } catch (IOException exception) {
        throw new BusinessException(500, "failed to store application resume snapshot");
    }
}
```

Snapshot-cleanup helper to pin:

```java
private void tryDeleteSnapshotFile(String storageKey) {
    try {
        if (storageKey != null && !storageKey.isBlank() && resourceFileStorage.exists(storageKey)) {
            resourceFileStorage.delete(storageKey);
        }
    } catch (IOException exception) {
        log.warn("Failed to delete duplicate-apply snapshot file: {}", storageKey, exception);
    }
}
```

Make `ResumeService.delete` explicit about row-first semantics:

```java
@Transactional
public void delete(String identity, Long resumeId) {
    Resume resume = requireOwnedResume(identity, resumeId);
    String storageKey = resume.getStorageKey();
    resumeMapper.deleteById(resumeId);
    try {
        if (storageKey != null && !storageKey.isBlank() && resourceFileStorage.exists(storageKey)) {
            resourceFileStorage.delete(storageKey);
        }
    } catch (IOException exception) {
        log.warn("Failed to delete resume file after row removal: {}", storageKey, exception);
    }
}
```

Create `JobApplicationController`:

```java
@Validated
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @GetMapping("/mine")
    public Result<MyJobApplicationListResponse> mine(Authentication authentication) {
        return Result.success(jobApplicationService.listMine(authentication.getName()));
    }
}
```

Modify `JobController.java` to add apply:

```java
private final JobApplicationService jobApplicationService;

@PostMapping("/{id}/apply")
public Result<JobApplicationRecordResponse> apply(
        @PathVariable Long id,
        Authentication authentication,
        @Validated @RequestBody ApplyJobRequest request) {
    return Result.success(jobApplicationService.apply(authentication.getName(), id, request));
}
```

Modify `JobDetailResponse.java`:

```java
public record JobDetailResponse(
        Long id,
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        String sourceUrl,
        String summary,
        String content,
        String status,
        LocalDateTime deadlineAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean favoritedByMe,
        boolean appliedByMe,
        Long applicationId) {
}
```

Modify `JobService.toJobDetail(...)` so it resolves current-user application state through `JobApplicationMapper`.

Create `AdminJobApplicationController`:

```java
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/applications")
public class AdminJobApplicationController {

    private final AdminJobApplicationService adminJobApplicationService;

    @GetMapping
    public Result<AdminJobApplicationListResponse> list() {
        return Result.success(adminJobApplicationService.listApplications());
    }

    @GetMapping("/{id}/resume/download")
    public ResponseEntity<InputStreamResource> downloadResume(@PathVariable Long id) {
        DownloadedApplicationResume download = adminJobApplicationService.downloadResumeSnapshot(id);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(new InputStreamResource(download.inputStream()));
    }
}
```

- [ ] **Step 4: Re-run the targeted application backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobApplicationControllerTests,AdminJobApplicationControllerTests,JobControllerTests" test
```

Expected: PASS with duplicate-apply guard, non-published-job rejection, owner-only resume selection, snapshot download, and job-detail applied-state enrichment.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/schema.sql backend/src/main/java/com/campus/common/JobApplicationStatus.java backend/src/main/java/com/campus/entity/JobApplication.java backend/src/main/java/com/campus/mapper/JobApplicationMapper.java backend/src/main/java/com/campus/dto/ApplyJobRequest.java backend/src/main/java/com/campus/dto/JobApplicationRecordResponse.java backend/src/main/java/com/campus/dto/MyJobApplicationListResponse.java backend/src/main/java/com/campus/dto/AdminJobApplicationListResponse.java backend/src/main/java/com/campus/service/JobApplicationService.java backend/src/main/java/com/campus/service/AdminJobApplicationService.java backend/src/main/java/com/campus/controller/JobApplicationController.java backend/src/main/java/com/campus/controller/admin/AdminJobApplicationController.java backend/src/main/java/com/campus/controller/JobController.java backend/src/main/java/com/campus/service/JobService.java backend/src/main/java/com/campus/dto/JobDetailResponse.java backend/src/test/java/com/campus/controller/JobApplicationControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminJobApplicationControllerTests.java backend/src/test/java/com/campus/controller/JobControllerTests.java
git commit -m "feat: add in-platform job application backend"
```

## Task 3: Add User-Side Frontend Resume Library And My Applications Pages

**Files:**
- Create: `frontend/src/api/resumes.js`
- Create: `frontend/src/api/applications.js`
- Create: `frontend/src/views/ProfileResumesView.vue`
- Create: `frontend/src/views/ProfileResumesView.spec.js`
- Create: `frontend/src/views/ProfileApplicationsView.vue`
- Create: `frontend/src/views/ProfileApplicationsView.spec.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/views/ProfileView.spec.js`

- [ ] **Step 1: Write the failing resume/profile frontend tests**

Create `ProfileResumesView.spec.js`:

```javascript
import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, expect, test, vi } from "vitest";
import ProfileResumesView from "./ProfileResumesView.vue";
import { createResume, deleteResume, downloadResume, getMyResumes } from "../api/resumes.js";

vi.mock("../api/resumes.js", () => ({
  createResume: vi.fn(),
  deleteResume: vi.fn(),
  downloadResume: vi.fn(),
  getMyResumes: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

test("loads resumes and supports upload/delete actions", async () => {
  getMyResumes
    .mockResolvedValueOnce({ total: 0, resumes: [] })
    .mockResolvedValueOnce({
      total: 1,
      resumes: [
        {
          id: 1,
          title: "Intern Resume",
          fileName: "intern-resume.pdf",
          fileSize: 1024,
          createdAt: "2026-04-18T10:00:00",
        },
      ],
    })
    .mockResolvedValueOnce({ total: 0, resumes: [] });

  createResume.mockResolvedValue({
    id: 1,
    title: "Intern Resume",
    fileName: "intern-resume.pdf",
  });
  deleteResume.mockResolvedValue(true);

  const wrapper = mount(ProfileResumesView);
  await flushPromises();

  expect(wrapper.text()).toContain("You have not uploaded any resumes yet.");

  const file = new File(["resume"], "intern-resume.pdf", { type: "application/pdf" });
  await wrapper.find('input[name="title"]').setValue("Intern Resume");
  await wrapper.find('input[name="file"]').trigger("change", { target: { files: [file] } });
  await wrapper.find("form").trigger("submit.prevent");
  await flushPromises();

  expect(createResume).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("Intern Resume");

  await wrapper.find('[data-testid="delete-resume-1"]').trigger("click");
  await flushPromises();

  expect(deleteResume).toHaveBeenCalledWith(1);
});
```

Create `ProfileApplicationsView.spec.js`:

```javascript
import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";
import ProfileApplicationsView from "./ProfileApplicationsView.vue";
import { getMyApplications } from "../api/applications.js";

vi.mock("../api/applications.js", () => ({
  getMyApplications: vi.fn(),
}));

test("renders my application history", async () => {
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
        resumeTitleSnapshot: "Intern Resume",
        resumeFileNameSnapshot: "intern-resume.pdf",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(ProfileApplicationsView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(wrapper.text()).toContain("Java Backend Intern");
  expect(wrapper.text()).toContain("Future Campus Tech");
  expect(wrapper.text()).toContain("Shenzhen");
  expect(wrapper.text()).toContain("SUBMITTED");
  expect(wrapper.text()).toContain("Intern Resume");
  expect(wrapper.text()).toContain("2026-04-18");
  expect(wrapper.html()).toContain('data-to="/jobs/1"');
});
```

Extend `ProfileView.spec.js`:

```javascript
test("profile desk exposes resumes and applications workspace links", async () => {
  const wrapper = mount(ProfileView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(wrapper.html()).toContain('data-to="/profile/resumes"');
  expect(wrapper.html()).toContain('data-to="/profile/applications"');
});
```

- [ ] **Step 2: Run the targeted user-side frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js src/views/ProfileView.spec.js
```

Expected: FAIL because the new API adapters, routes, and views do not exist yet.

- [ ] **Step 3: Implement the resume and applicant profile pages**

Before writing UI code, use `@frontend-design`, then review the finished surfaces with `@ui-ux-pro-max`.

Create `frontend/src/api/resumes.js`:

```javascript
import http from "./http.js";

export async function getMyResumes() {
  const { data } = await http.get("/resumes/mine");
  return data.data;
}

export async function createResume(formData) {
  const { data } = await http.post("/resumes", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}

export async function deleteResume(id) {
  const { data } = await http.delete(`/resumes/${id}`);
  return data.data;
}

export async function downloadResume(id) {
  const response = await http.get(`/resumes/${id}/download`, {
    responseType: "blob",
  });
  /* mirror the resource download helper exactly */
}
```

Create `frontend/src/api/applications.js`:

```javascript
import http from "./http.js";

export async function getMyApplications() {
  const { data } = await http.get("/applications/mine");
  return data.data;
}
```

Add the routes:

```javascript
{
  path: "/profile/resumes",
  name: "profile-resumes",
  component: () => import("../views/ProfileResumesView.vue"),
  meta: { requiresAuth: true },
},
{
  path: "/profile/applications",
  name: "profile-applications",
  component: () => import("../views/ProfileApplicationsView.vue"),
  meta: { requiresAuth: true },
},
```

Implement `ProfileResumesView.vue` so it:

- loads resume list on mount
- shows an inline upload form with `title` and `file`
- reloads after successful upload or delete
- offers `Download` and `Delete` actions per card
- uses the existing editorial card language from profile/resources pages

Implement `ProfileApplicationsView.vue` so it:

- loads `/api/applications/mine`
- shows compact stats and read-only application cards
- renders job title, company name, city, submitted time, status, and resume snapshot title on each card
- links each row back to `/jobs/:id`
- uses a stable empty state when no applications exist

Update `ProfileView.vue` workspace links with:

```vue
<RouterLink to="/profile/resumes" class="panel-card profile-link-card">
  <span class="profile-link-card__eyebrow">My Resumes</span>
  <strong>Resume Library</strong>
  <p class="meta-copy">Keep multiple resume files ready for application.</p>
</RouterLink>

<RouterLink to="/profile/applications" class="panel-card profile-link-card">
  <span class="profile-link-card__eyebrow">My Applications</span>
  <strong>Application Records</strong>
  <p class="meta-copy">Review where you have already applied and which resume was used.</p>
</RouterLink>
```

- [ ] **Step 4: Re-run the targeted user-side frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js src/views/ProfileView.spec.js
```

Expected: PASS with working profile routes, upload/list/delete behavior, and applicant history rendering.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/resumes.js frontend/src/api/applications.js frontend/src/views/ProfileResumesView.vue frontend/src/views/ProfileResumesView.spec.js frontend/src/views/ProfileApplicationsView.vue frontend/src/views/ProfileApplicationsView.spec.js frontend/src/router/index.js frontend/src/views/ProfileView.vue frontend/src/views/ProfileView.spec.js
git commit -m "feat: add resume library and applicant history views"
```

## Task 4: Add Job Detail Apply UI And Admin Applications Workbench

**Files:**
- Create: `frontend/src/views/admin/AdminApplicationsView.vue`
- Create: `frontend/src/views/admin/AdminApplicationsView.spec.js`
- Modify: `frontend/src/api/jobs.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/views/JobDetailView.vue`
- Modify: `frontend/src/views/JobDetailView.spec.js`
- Modify: `frontend/src/components/NavBar.vue`
- Modify: `frontend/src/components/NavBar.spec.js`

- [ ] **Step 1: Write the failing apply/admin frontend tests**

Extend `JobDetailView.spec.js` so it mocks resume loading and apply:

```javascript
import { applyToJob, favoriteJob, getJobDetail, unfavoriteJob } from "../api/jobs.js";
import { getMyResumes } from "../api/resumes.js";

vi.mock("../api/jobs.js", () => ({
  applyToJob: vi.fn(),
  favoriteJob: vi.fn(),
  getJobDetail: vi.fn(),
  unfavoriteJob: vi.fn(),
}));

vi.mock("../api/resumes.js", () => ({
  getMyResumes: vi.fn(),
}));

test("guest apply click redirects to login", async () => {
  const wrapper = mountView();
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");

  expect(routerPush).toHaveBeenCalledWith({
    name: "login",
    query: { redirect: "/jobs/11" },
  });
});

test("authenticated user with no resumes sees upload guidance before applying", async () => {
  getMyResumes.mockResolvedValue({ total: 0, resumes: [] });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");
  await flushPromises();

  expect(wrapper.text()).toContain("Upload a resume first");
  expect(wrapper.html()).toContain('data-to="/profile/resumes"');
});

test("authenticated user can apply with a selected resume and then sees applied state", async () => {
  getMyResumes.mockResolvedValue({
    total: 1,
    resumes: [{ id: 21, title: "Intern Resume", fileName: "intern-resume.pdf" }],
  });
  applyToJob.mockResolvedValue({
    id: 1001,
    jobId: 11,
    status: "SUBMITTED",
    resumeTitleSnapshot: "Intern Resume",
    resumeFileNameSnapshot: "intern-resume.pdf",
    submittedAt: "2026-04-18T10:00:00",
  });

  const wrapper = mountView(true);
  await flushPromises();

  await wrapper.find('[data-testid="apply-toggle"]').trigger("click");
  await flushPromises();
  await wrapper.find('input[type="radio"][value="21"]').setValue();
  await wrapper.find('[data-testid="submit-application"]').trigger("click");
  await flushPromises();

  expect(applyToJob).toHaveBeenCalledWith(11, { resumeId: 21 });
  expect(wrapper.text()).toContain("Applied");
  expect(wrapper.html()).toContain('data-to="/profile/applications"');
});
```

Create `AdminApplicationsView.spec.js`:

```javascript
import { flushPromises, mount } from "@vue/test-utils";
import { expect, test, vi } from "vitest";
import AdminApplicationsView from "./AdminApplicationsView.vue";
import { downloadAdminApplicationResume, getAdminApplications } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  downloadAdminApplicationResume: vi.fn(),
  getAdminApplications: vi.fn(),
}));

test("renders the read-only admin applications workbench", async () => {
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
        resumeFileNameSnapshot: "intern-resume.pdf",
        status: "SUBMITTED",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  expect(wrapper.text()).toContain("Java Backend Intern");
  expect(wrapper.text()).toContain("Future Campus Tech");
  expect(wrapper.text()).toContain("2");
  expect(wrapper.text()).toContain("NormalUser");
  expect(wrapper.text()).toContain("intern-resume.pdf");
  expect(wrapper.text()).not.toContain("Approve");
  expect(wrapper.text()).not.toContain("Reject");
  expect(wrapper.html()).toContain('data-to="/jobs/1"');
});

test("download action calls the admin resume snapshot helper", async () => {
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
        resumeFileNameSnapshot: "intern-resume.pdf",
        status: "SUBMITTED",
        submittedAt: "2026-04-18T10:30:00",
      },
    ],
  });

  const wrapper = mount(AdminApplicationsView, {
    global: {
      stubs: {
        RouterLink: {
          props: ["to"],
          template: "<a :data-to='to'><slot /></a>",
        },
      },
    },
  });
  await flushPromises();

  await wrapper.find('[data-testid="download-application-resume-1001"]').trigger("click");

  expect(downloadAdminApplicationResume).toHaveBeenCalledWith(1001);
});
```

Extend `NavBar.spec.js`:

```javascript
test("navbar shows the admin applications link for admins", () => {
  const userStore = useUserStore();
  userStore.token = "demo-token";
  userStore.profile = {
    id: 1,
    userId: 1,
    nickname: "Admin",
    phone: "13800000000",
    role: "ADMIN",
    verificationStatus: "VERIFIED",
    unreadNotificationCount: 2,
  };

  const wrapper = mountNavBar();

  expect(wrapper.html()).toContain('data-to="/admin/applications"');
});
```

- [ ] **Step 2: Run the targeted apply/admin frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/JobDetailView.spec.js src/views/admin/AdminApplicationsView.spec.js src/components/NavBar.spec.js
```

Expected: FAIL because the apply API, resume selection UI, admin workbench view, and admin nav link do not exist yet.

- [ ] **Step 3: Implement job-detail apply and the admin workbench**

Before writing UI code, use `@frontend-design`, then review the finished surfaces with `@ui-ux-pro-max`.

Update `frontend/src/api/jobs.js`:

```javascript
export async function applyToJob(id, payload) {
  const { data } = await http.post(`/jobs/${id}/apply`, payload);
  return data.data;
}
```

Update `frontend/src/api/admin.js`:

```javascript
export async function getAdminApplications() {
  const { data } = await http.get("/admin/applications");
  return data.data;
}

export async function downloadAdminApplicationResume(id) {
  const response = await http.get(`/admin/applications/${id}/resume/download`, {
    responseType: "blob",
  });
  /* mirror the existing resource download helper */
}
```

Add the admin route:

```javascript
{
  path: "/admin/applications",
  name: "admin-applications",
  component: () => import("../views/admin/AdminApplicationsView.vue"),
  meta: { requiresAuth: true, roles: ["ADMIN"] },
},
```

Update `JobDetailView.vue` so it:

- keeps `Source Link`
- adds one apply toggle button:
  - `data-testid="apply-toggle"`
- lazily or eagerly loads resumes for authenticated users
- shows:
  - upload guidance when `resumes.length === 0`
  - inline radio selection when resumes exist
  - applied state when `detail.appliedByMe === true`
- submits with `applyToJob(detail.id, { resumeId: selectedResumeId })`
- updates local state after success:

```javascript
detail.value = {
  ...detail.value,
  appliedByMe: true,
  applicationId: response.id,
};
```

Implement `AdminApplicationsView.vue` so it:

- loads `getAdminApplications()` on entry
- shows one page-level loading state
- shows one page-level retry state on failure
- renders compact metrics and a read-only list/table
- renders applicant user id, applicant nickname, company name, resume snapshot file name, submitted time, `Download Resume`, and `Open Job`
- exposes only:
  - `Download Resume`
  - `Open Job`

Update `NavBar.vue` admin items:

```javascript
if (userStore.canReviewVerifications) {
  items.push({ to: "/admin/dashboard", label: "..." });
  items.push({ to: "/admin/applications", label: "..." });
  items.push({ to: "/admin/verifications", label: "..." });
  items.push({ to: "/admin/community", label: "..." });
}
```

- [ ] **Step 4: Re-run the targeted apply/admin frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/JobDetailView.spec.js src/views/admin/AdminApplicationsView.spec.js src/components/NavBar.spec.js
```

Expected: PASS with guest redirect, no-resume guidance, successful apply state, admin workbench rendering, and admin-only nav entry.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/admin/AdminApplicationsView.vue frontend/src/views/admin/AdminApplicationsView.spec.js frontend/src/api/jobs.js frontend/src/api/admin.js frontend/src/router/index.js frontend/src/views/JobDetailView.vue frontend/src/views/JobDetailView.spec.js frontend/src/components/NavBar.vue frontend/src/components/NavBar.spec.js
git commit -m "feat: add job application workbenches"
```

## Task 5: Update Docs And Run Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for Phase N**

Document all of the following:

- repo status now includes `Phase N job application and resume workflow first slice`
- implemented now includes:
  - resume library
  - in-platform job apply
  - my applications
  - admin applications workbench
- key routes:
  - `/profile/resumes`
  - `/profile/applications`
  - `/admin/applications`
- backend endpoints:
  - `GET /api/resumes/mine`
  - `POST /api/resumes`
  - `GET /api/resumes/{id}/download`
  - `DELETE /api/resumes/{id}`
  - `POST /api/jobs/{id}/apply`
  - `GET /api/applications/mine`
  - `GET /api/admin/applications`
  - `GET /api/admin/applications/{id}/resume/download`
- permissions:
  - authenticated users can manage their resume library and submit one application per job
  - admins can read applications and download snapshot resumes
- manual smoke steps for upload/apply/delete-original/admin-read path
- targeted verification section for Phase N

- [ ] **Step 2: Run the targeted backend verification set**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResumeControllerTests,JobApplicationControllerTests,AdminJobApplicationControllerTests,JobControllerTests" test
```

Expected: PASS.

- [ ] **Step 3: Run the targeted frontend verification set**

Run:

```powershell
cd frontend
npx vitest run src/views/ProfileResumesView.spec.js src/views/ProfileApplicationsView.spec.js src/views/JobDetailView.spec.js src/views/admin/AdminApplicationsView.spec.js src/views/ProfileView.spec.js src/components/NavBar.spec.js
```

Expected: PASS.

- [ ] **Step 4: Run full regression and build**

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

Expected: PASS across the full backend and frontend suites.

- [ ] **Step 5: Manual smoke and commit**

Manual smoke:

1. start backend with `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`
2. start frontend with `npm run dev -- --host 127.0.0.1`
3. log in as `13800000001`
4. open `/profile/resumes` and upload two resumes
5. open `/jobs/1` and apply with one selected resume
6. confirm `/profile/applications` shows the new record
7. delete the original resume from `/profile/resumes`
8. return to `/profile/applications` and confirm the same application record still renders after the original resume was deleted
9. log in as admin `13800000000`
10. open `/admin/applications`
11. confirm the record renders and the snapshot resume downloads successfully
12. confirm `/jobs/1` still shows applied state for the applicant and the page still keeps the external source link

Then commit:

```bash
git add README.md
git commit -m "docs: add phase n application workflow verification notes"
```

## Execution Notes

- Keep resume files and application snapshot files on the existing `ResourceFileStorage` abstraction; do not create a second storage stack.
- Do not route resumes through the public resource-review workflow.
- Do not silently allow duplicate apply when the unique key rejects insert; surface the explicit business error instead.
- If a file-copy failure occurs during apply, do not insert the application row.
- Keep the first UI slice intentionally narrow; do not add rename, replace, preview, modal-heavy flows, or status progression.
