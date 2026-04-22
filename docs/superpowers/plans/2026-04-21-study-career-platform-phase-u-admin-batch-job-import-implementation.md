# Phase U Admin Batch Job Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a synchronous admin-only UTF-8 CSV import flow on `/admin/jobs` that creates multiple new `JobPosting` records as `DRAFT`, rejects the whole batch on any validation failure, and returns row-level validation details without changing public jobs behavior.

**Architecture:** Keep the import slice isolated behind a dedicated admin multipart endpoint and a dedicated transactional batch-import service. Use a strict CSV parser for file-shape validation, a focused draft factory for row normalization and row-level validation, and a structured import-validation exception so the existing `Result` envelope can return `data.errors[]` without weakening current controller patterns.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, Apache Commons CSV, H2, JUnit 5, AssertJ, Vue 3, Vite, Vitest, Axios

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-design.md`
- Plan style reference:
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-implementation.md`
- Existing admin jobs backend:
  - `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
  - `backend/src/main/java/com/campus/service/AdminJobService.java`
  - `backend/src/main/java/com/campus/dto/CreateJobRequest.java`
  - `backend/src/main/java/com/campus/dto/UpdateJobRequest.java`
  - `backend/src/main/java/com/campus/entity/JobPosting.java`
  - `backend/src/main/java/com/campus/mapper/JobPostingMapper.java`
- Existing cross-cutting API envelope and exception handling:
  - `backend/src/main/java/com/campus/common/Result.java`
  - `backend/src/main/java/com/campus/common/BusinessException.java`
  - `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
- Existing admin jobs frontend:
  - `frontend/src/api/admin.js`
  - `frontend/src/api/http.js`
  - `frontend/src/views/admin/AdminJobManageView.vue`
  - `frontend/src/views/admin/AdminJobManageView.spec.js`
- Existing multipart and admin-controller test patterns to mirror:
  - `backend/src/test/java/com/campus/controller/ResumeControllerTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`

## Scope Lock

This plan covers only the approved Phase U slice:

- add `POST /api/admin/jobs/import`
- accept one `multipart/form-data` field named `file`
- support `.csv` only
- require `UTF-8`
- support the approved header names:
  - `title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt`
- require the approved required columns and allow the approved optional columns
- cap imports at `200` data rows
- create all imported jobs as `DRAFT`
- reject the entire batch on any validation failure
- reject duplicate `sourceUrl` values inside the file
- reject duplicate `sourceUrl` values against existing non-`DELETED` jobs
- keep the import surface inside the existing `/admin/jobs` page

This plan explicitly does not implement:

- `xlsx` or Excel parsing
- async import jobs, progress polling, or import history
- partial success or skip-invalid-row behavior
- upsert, merge, overwrite, or delete-during-import modes
- auto-publish after import
- public jobs-page changes
- third-party sync

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/common/JobImportValidationException.java`
  Carry structured validation payload for batch-import failures.
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobImportController.java`
  Admin-only multipart boundary for the new import endpoint.
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportResponse.java`
  Success summary payload for completed imports.
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportValidationError.java`
  One row-level validation error item.
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportValidationResponse.java`
  Validation-failure payload returned as `Result.data`.
- Create: `backend/src/main/java/com/campus/service/JobImportCsvParser.java`
  Strict UTF-8 CSV parser that owns file-shape validation.
- Create: `backend/src/main/java/com/campus/service/JobImportRow.java`
  Raw parsed CSV row plus the user-facing row number.
- Create: `backend/src/main/java/com/campus/service/JobBatchImportService.java`
  Transactional orchestration for parse, validate, duplicate check, and insert.
- Create: `backend/src/main/java/com/campus/service/JobPostingDraftFactory.java`
  Convert one parsed row into a validated `JobPosting` draft or row errors.
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`
  Endpoint coverage for success, validation failure, and auth boundaries.
- Create: `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`
  Transactional import coverage for draft defaults, duplicates, and rollback.
- Create: `backend/src/test/java/com/campus/service/JobImportCsvParserTests.java`
  Focused parser coverage for UTF-8, headers, quoting, and row-limit rules.

### Backend: Modify Existing

- Modify: `backend/pom.xml`
  Add Apache Commons CSV so quoted commas and escaped values are parsed correctly.
- Modify: `backend/src/main/java/com/campus/common/Result.java`
  Add a typed `error(code, message, data)` helper for structured validation responses.
- Modify: `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
  Map `JobImportValidationException` into the approved `code=400` envelope with `data.errors`.

### Frontend: Modify Existing

- Modify: `frontend/src/api/admin.js`
  Add the multipart `importAdminJobs(...)` API call.
- Modify: `frontend/src/api/http.js`
  Preserve backend `data` on rejected requests so row-level import errors reach the view.
- Modify: `frontend/src/views/admin/AdminJobManageView.vue`
  Add the CSV upload surface, success summary, and row-error rendering.
- Modify: `frontend/src/views/admin/AdminJobManageView.spec.js`
  Cover successful import refresh and validation-error rendering.

### Docs: Modify Existing

- Modify: `README.md`
  Record Phase U as implemented once the code lands and document the new admin import capability.
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-design.md`
  Add a post-implementation validation note after the phase is shipped.

### Verify Existing Files Without Planned Logic Changes

- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`

## Responsibility Notes

- `JobImportCsvParser` owns file-level validation only:
  - file presence
  - `.csv` extension
  - UTF-8 decoding
  - required headers
  - row-count limit
- `JobPostingDraftFactory` owns row-level normalization and validation only:
  - trim text
  - enforce max lengths
  - normalize enums case-insensitively
  - validate `sourceUrl`
  - parse optional `deadlineAt`
- `JobBatchImportService` owns:
  - current-admin lookup
  - aggregate row errors
  - duplicate checks in-file and against existing non-`DELETED` jobs
  - transactional insert-all-or-rollback-all behavior
- `AdminJobImportController` owns request/response wiring only.
- `GlobalExceptionHandler` should be the only place that translates import validation exceptions into the final `Result` envelope.
- `http.js` should only preserve rejected `data`; `AdminJobManageView.vue` should own how row-level issues are presented.

## Task 1: Parse Strict UTF-8 CSV Files And Report File-Shape Errors

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/campus/common/JobImportValidationException.java`
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportValidationError.java`
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportValidationResponse.java`
- Create: `backend/src/main/java/com/campus/service/JobImportRow.java`
- Create: `backend/src/main/java/com/campus/service/JobImportCsvParser.java`
- Test: `backend/src/test/java/com/campus/service/JobImportCsvParserTests.java`

- [x] **Step 1: Write the failing CSV parser tests**

Create `JobImportCsvParserTests` to lock down the approved file contract:

```java
@Test
void parsesUtf8CsvWithQuotedCellsAndExpectedRowNumbers() {
    MockMultipartFile file = csvFile("jobs.csv", """
            title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
            "Data Intern","Campus, Inc",Hangzhou,internship,bachelor,Official Site,https://jobs.example.com/data-intern,"Build dashboards","quoted, content",2026-06-20 18:00:00
            """);

    List<JobImportRow> rows = parser.parse(file);

    assertThat(rows).singleElement().satisfies(row -> {
        assertThat(row.rowNumber()).isEqualTo(2);
        assertThat(row.companyName()).isEqualTo("Campus, Inc");
        assertThat(row.content()).isEqualTo("quoted, content");
        assertThat(row.deadlineAt()).isEqualTo("2026-06-20 18:00:00");
    });
}

@Test
void rejectsMissingRequiredHeader() {
    MockMultipartFile file = csvFile("jobs.csv", """
            title,companyName,city,jobType,educationRequirement,sourcePlatform,summary
            Data Intern,Campus Inc,Hangzhou,INTERNSHIP,BACHELOR,Official Site,Summary
            """);

    assertThatThrownBy(() -> parser.parse(file))
            .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                assertThat(exception.response().totalRows()).isEqualTo(0);
                assertThat(exception.response().errors()).extracting(AdminJobImportValidationError::column)
                        .contains("sourceUrl");
            });
}

@Test
void rejectsFilesAboveRowLimit() {
    MockMultipartFile file = csvFile("jobs.csv", buildCsvWithRows(201));

    assertThatThrownBy(() -> parser.parse(file))
            .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                assertThat(exception.response().totalRows()).isEqualTo(201);
                assertThat(exception.response().errors()).extracting(AdminJobImportValidationError::message)
                        .containsExactly("job import row limit exceeded");
            });
}

@Test
void rejectsMalformedUtf8Payload() {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "jobs.csv",
            "text/csv",
            new byte[] { (byte) 0xC3, (byte) 0x28 });

    assertThatThrownBy(() -> parser.parse(file))
            .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                assertThat(exception.response().errors()).extracting(AdminJobImportValidationError::message)
                        .containsExactly("csv file must be utf-8 encoded");
            });
}
```

- [x] **Step 2: Run the targeted parser tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobImportCsvParserTests" test
```

Expected: FAIL because the parser, DTOs, exception type, and CSV dependency do not exist yet.

- [x] **Step 3: Implement the strict CSV parser foundation**

Add Apache Commons CSV to `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>
```

Create the validation payload types:

```java
public record AdminJobImportValidationError(int rowNumber, String column, String message) {
}

public record AdminJobImportValidationResponse(
        String fileName,
        int totalRows,
        int importedCount,
        List<AdminJobImportValidationError> errors) {
}
```

Create the exception:

```java
public class JobImportValidationException extends RuntimeException {

    private final AdminJobImportValidationResponse response;

    public JobImportValidationException(AdminJobImportValidationResponse response) {
        super("job import validation failed");
        this.response = response;
    }

    public AdminJobImportValidationResponse response() {
        return response;
    }
}
```

Create `JobImportRow` as the raw parsed row shape:

```java
public record JobImportRow(
        int rowNumber,
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        String sourceUrl,
        String summary,
        String content,
        String deadlineAt) {
}
```

Create `JobImportCsvParser` with these rules:

```java
public List<JobImportRow> parse(MultipartFile file) {
    String fileName = requireCsvFile(file);
    try (Reader reader = utf8Reader(file);
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(false)
                    .build()
                    .parse(reader)) {
        validateHeaders(fileName, parser.getHeaderMap().keySet());
        List<CSVRecord> records = parser.getRecords();
        validateRowCount(fileName, records.size());
        return IntStream.range(0, records.size())
                .mapToObj(index -> toRow(index + 2, records.get(index)))
                .toList();
    } catch (MalformedInputException exception) {
        throw validationFailure(fileName, 0, error(1, "file", "csv file must be utf-8 encoded"));
    } catch (IOException exception) {
        throw new BusinessException(500, "job import file unavailable");
    }
}
```

Implementation rules:

- reject missing file, empty file, blank file name, and non-`.csv` file names as validation failures
- decode with a `CharsetDecoder` configured with `CodingErrorAction.REPORT`
- require the approved required header names exactly once each
- allow optional `content` and `deadlineAt` headers to be omitted and treat missing optional cells as blank
- keep raw cell values untouched here; trimming and domain validation happen later
- set row numbers as `2..N+1` so the first data row is always row `2`

- [x] **Step 4: Re-run the parser tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobImportCsvParserTests" test
```

Expected: PASS with UTF-8 decoding, quoted values, header enforcement, and the `200`-row limit covered.

- [x] **Step 5: Commit the parser foundation**

```bash
git add backend/pom.xml backend/src/main/java/com/campus/common/JobImportValidationException.java backend/src/main/java/com/campus/dto/AdminJobImportValidationError.java backend/src/main/java/com/campus/dto/AdminJobImportValidationResponse.java backend/src/main/java/com/campus/service/JobImportRow.java backend/src/main/java/com/campus/service/JobImportCsvParser.java backend/src/test/java/com/campus/service/JobImportCsvParserTests.java
git commit -m "feat: add job import csv parser foundation"
```

## Task 2: Validate Rows, Detect Duplicates, And Persist Drafts Atomically

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AdminJobImportResponse.java`
- Create: `backend/src/main/java/com/campus/service/JobPostingDraftFactory.java`
- Create: `backend/src/main/java/com/campus/service/JobBatchImportService.java`
- Test: `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`

- [x] **Step 1: Write the failing batch-import service tests**

Create `JobBatchImportServiceTests` as a transactional integration suite using `@SpringBootTest`, `@Sql`, `JdbcTemplate`, and `MockMultipartFile`:

```java
@Test
void importsMultipleValidRowsAsDrafts() {
    MockMultipartFile file = csvFile("""
            title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
            Data Intern,Campus Future,Hangzhou,internship,bachelor,Official Site,https://jobs.example.com/import/data-intern,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
            Growth PM,Campus Future,Shanghai,CAMPUS,ANY,Official Site,https://jobs.example.com/import/growth-pm,Support campus growth,, 
            """);

    AdminJobImportResponse response = service.importJobs("1", file);

    assertThat(response.importedCount()).isEqualTo(2);
    assertThat(response.defaultStatus()).isEqualTo("DRAFT");
    assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
            Integer.class)).isEqualTo(2);
    assertThat(jdbcTemplate.queryForList(
            "SELECT status FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
            String.class)).containsOnly("DRAFT");
}

@Test
void duplicateSourceUrlInsideFileFailsWholeImport() {
    MockMultipartFile file = csvFile("""
            title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
            Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/dup,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
            Growth PM,Campus Future,Shanghai,CAMPUS,ANY,Official Site,https://jobs.example.com/import/dup,Support campus growth,,
            """);

    assertThatThrownBy(() -> service.importJobs("1", file))
            .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                assertThat(exception.response().importedCount()).isEqualTo(0);
                assertThat(exception.response().errors()).extracting(AdminJobImportValidationError::message)
                        .contains("duplicate source url in file");
            });

    assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_job_posting WHERE source_url = 'https://jobs.example.com/import/dup'",
            Integer.class)).isEqualTo(0);
}

@Test
void duplicateSourceUrlAgainstExistingNonDeletedJobFailsWholeImport() {
    MockMultipartFile file = csvFile("""
            title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
            Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/future-campus-tech/backend-intern,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
            """);

    assertThatThrownBy(() -> service.importJobs("1", file))
            .isInstanceOfSatisfying(JobImportValidationException.class, exception -> {
                assertThat(exception.response().errors()).extracting(AdminJobImportValidationError::message)
                        .contains("duplicate source url already exists");
            });
}

@Test
void oneInvalidRowRollsBackTheWholeBatch() {
    MockMultipartFile file = csvFile("""
            title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
            Valid Row,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/valid-row,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
            Broken Row,Campus Future,Hangzhou,NOT_A_TYPE,BACHELOR,Official Site,https://jobs.example.com/import/broken-row,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
            """);

    assertThatThrownBy(() -> service.importJobs("1", file))
            .isInstanceOf(JobImportValidationException.class);

    assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_job_posting WHERE source_url LIKE 'https://jobs.example.com/import/%'",
            Integer.class)).isEqualTo(0);
}
```

- [x] **Step 2: Run the targeted batch-import service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobBatchImportServiceTests" test
```

Expected: FAIL because the draft factory, batch-import service, and success response DTO do not exist yet.

- [x] **Step 3: Implement row validation, duplicate checks, and transactional inserts**

Create the success response:

```java
public record AdminJobImportResponse(
        String fileName,
        int totalRows,
        int importedCount,
        String defaultStatus) {
}
```

Create `JobPostingDraftFactory` with a focused build result:

```java
public BuildResult build(JobImportRow row, Long adminId, LocalDateTime now) {
    List<AdminJobImportValidationError> errors = new ArrayList<>();

    String title = requiredText(row.rowNumber(), "title", row.title(), 120, errors);
    String companyName = requiredText(row.rowNumber(), "companyName", row.companyName(), 80, errors);
    String city = requiredText(row.rowNumber(), "city", row.city(), 80, errors);
    String jobType = normalizeEnum(row.rowNumber(), "jobType", row.jobType(), JobType.class, "invalid job type", errors);
    String educationRequirement = normalizeEnum(
            row.rowNumber(), "educationRequirement", row.educationRequirement(),
            JobEducationRequirement.class, "invalid education requirement", errors);
    String sourcePlatform = requiredText(row.rowNumber(), "sourcePlatform", row.sourcePlatform(), 50, errors);
    String sourceUrl = normalizeSourceUrl(row.rowNumber(), row.sourceUrl(), errors);
    String summary = requiredText(row.rowNumber(), "summary", row.summary(), 300, errors);
    String content = optionalText(row.rowNumber(), "content", row.content(), 10000, errors);
    LocalDateTime deadlineAt = parseDeadline(row.rowNumber(), row.deadlineAt(), errors);

    if (!errors.isEmpty()) {
        return BuildResult.invalid(errors);
    }

    JobPosting job = new JobPosting();
    job.setTitle(title);
    job.setCompanyName(companyName);
    job.setCity(city);
    job.setJobType(jobType);
    job.setEducationRequirement(educationRequirement);
    job.setSourcePlatform(sourcePlatform);
    job.setSourceUrl(sourceUrl);
    job.setSummary(summary);
    job.setContent(content);
    job.setDeadlineAt(deadlineAt);
    job.setStatus(JobPostingStatus.DRAFT.name());
    job.setPublishedAt(null);
    job.setCreatedBy(adminId);
    job.setUpdatedBy(adminId);
    job.setCreatedAt(now);
    job.setUpdatedAt(now);
    return BuildResult.valid(job);
}
```

Create `JobBatchImportService`:

```java
@Transactional
public AdminJobImportResponse importJobs(String identity, MultipartFile file) {
    User admin = userService.requireByIdentity(identity);
    String fileName = requireFileName(file);
    List<JobImportRow> rows = parser.parse(file);
    LocalDateTime now = LocalDateTime.now();

    List<JobPosting> drafts = new ArrayList<>();
    List<AdminJobImportValidationError> errors = new ArrayList<>();
    Map<String, Integer> firstSeenRowBySourceUrl = new HashMap<>();

    for (JobImportRow row : rows) {
        JobPostingDraftFactory.BuildResult result = draftFactory.build(row, admin.getId(), now);
        errors.addAll(result.errors());
        if (!result.isValid()) {
            continue;
        }

        JobPosting draft = result.job();
        String duplicateKey = draft.getSourceUrl().toLowerCase(Locale.ROOT);
        if (firstSeenRowBySourceUrl.putIfAbsent(duplicateKey, row.rowNumber()) != null) {
            errors.add(new AdminJobImportValidationError(row.rowNumber(), "sourceUrl", "duplicate source url in file"));
            continue;
        }
        drafts.add(draft);
    }

    errors.addAll(findExistingSourceUrlConflicts(drafts));
    if (!errors.isEmpty()) {
        throw new JobImportValidationException(new AdminJobImportValidationResponse(
                fileName,
                rows.size(),
                0,
                sortErrors(errors)));
    }

    drafts.forEach(jobPostingMapper::insert);
    return new AdminJobImportResponse(fileName, rows.size(), drafts.size(), JobPostingStatus.DRAFT.name());
}
```

Implementation rules:

- treat blank required cells as missing values
- keep `jobType` and `educationRequirement` case-insensitive before enum normalization
- validate `sourceUrl` using `URI.create(...)` and only allow `http` or `https`
- parse non-blank `deadlineAt` with `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")`
- query existing conflicts with one `IN (...)` lookup on normalized source URLs and exclude `DELETED`
- sort aggregated errors by `rowNumber`, then `column`, before throwing
- never insert any rows when `errors` is not empty

- [x] **Step 4: Re-run the batch-import service tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobBatchImportServiceTests" test
```

Expected: PASS with draft defaults, duplicate detection, and rollback behavior covered.

- [x] **Step 5: Commit the transactional batch-import service**

```bash
git add backend/src/main/java/com/campus/dto/AdminJobImportResponse.java backend/src/main/java/com/campus/service/JobPostingDraftFactory.java backend/src/main/java/com/campus/service/JobBatchImportService.java backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java
git commit -m "feat: add transactional admin job batch import"
```

## Task 3: Expose The Admin Import Endpoint And Structured Error Envelope

**Files:**
- Modify: `backend/src/main/java/com/campus/common/Result.java`
- Modify: `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/campus/controller/admin/AdminJobImportController.java`
- Test: `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`

- [x] **Step 1: Write the failing controller tests**

Create `AdminJobImportControllerTests` by mirroring `AdminJobControllerTests` and `ResumeControllerTests`:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanImportValidCsv() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "jobs.csv",
            "text/csv",
            """
                    title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                    Data Intern,Campus Future,Hangzhou,INTERNSHIP,BACHELOR,Official Site,https://jobs.example.com/import/controller-success,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                    """.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.fileName").value("jobs.csv"))
            .andExpect(jsonPath("$.data.importedCount").value(1))
            .andExpect(jsonPath("$.data.defaultStatus").value("DRAFT"));
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void validationFailureReturnsStructuredErrorPayload() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "jobs.csv",
            "text/csv",
            """
                    title,companyName,city,jobType,educationRequirement,sourcePlatform,sourceUrl,summary,content,deadlineAt
                    Broken Row,Campus Future,Hangzhou,NOPE,BACHELOR,Official Site,https://jobs.example.com/import/controller-broken,Build dashboards,Own weekly analytics,2026-06-20 18:00:00
                    """.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("job import validation failed"))
            .andExpect(jsonPath("$.data.importedCount").value(0))
            .andExpect(jsonPath("$.data.errors[0].column").value("jobType"));
}

@Test
@WithMockUser(username = "2", roles = "USER")
void normalUserCannotImportJobs() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "jobs.csv", "text/csv", "x".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/admin/jobs/import").file(file))
            .andExpect(status().isForbidden());
}
```

- [x] **Step 2: Run the targeted controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobImportControllerTests" test
```

Expected: FAIL because the import controller does not exist, and the current result helpers cannot return `code=400` with a typed `data` payload.

- [x] **Step 3: Implement the endpoint and structured error mapping**

Extend `Result` with an overloaded error factory:

```java
public static <T> Result<T> error(int code, String message, T data) {
    return new Result<>(code, message, data);
}
```

Add a dedicated exception handler before the generic handlers:

```java
@ExceptionHandler(JobImportValidationException.class)
public Result<AdminJobImportValidationResponse> handleJobImportValidationException(
        JobImportValidationException exception) {
    return Result.error(400, "job import validation failed", exception.response());
}
```

Create `AdminJobImportController`:

```java
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/jobs")
public class AdminJobImportController {

    private final JobBatchImportService jobBatchImportService;

    public AdminJobImportController(JobBatchImportService jobBatchImportService) {
        this.jobBatchImportService = jobBatchImportService;
    }

    @PostMapping("/import")
    public Result<AdminJobImportResponse> importJobs(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return Result.success(jobBatchImportService.importJobs(authentication.getName(), file));
    }
}
```

Implementation rules:

- keep `@PreAuthorize("hasRole('ADMIN')")` aligned with the existing admin controller pattern
- keep the endpoint under the same `/api/admin/jobs` route family
- do not move single-job create/update/publish/offline/delete into the new controller

- [x] **Step 4: Re-run the controller tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobImportControllerTests" test
```

Expected: PASS with admin success, structured validation failure, and `403` coverage in place.

- [x] **Step 5: Commit the import HTTP boundary**

```bash
git add backend/src/main/java/com/campus/common/Result.java backend/src/main/java/com/campus/config/GlobalExceptionHandler.java backend/src/main/java/com/campus/controller/admin/AdminJobImportController.java backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java
git commit -m "feat: add admin job import endpoint"
```

## Task 4: Add The `/admin/jobs` Import Surface And Frontend Error Rendering

**Files:**
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/api/http.js`
- Modify: `frontend/src/views/admin/AdminJobManageView.vue`
- Test: `frontend/src/views/admin/AdminJobManageView.spec.js`

- [x] **Step 1: Write the failing frontend tests**

Extend `AdminJobManageView.spec.js` with one success flow and one validation-error flow:

```javascript
import { getAdminJobs, importAdminJobs } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  createAdminJob: vi.fn(),
  deleteAdminJob: vi.fn(),
  getAdminJobs: vi.fn(),
  importAdminJobs: vi.fn(),
  offlineAdminJob: vi.fn(),
  publishAdminJob: vi.fn(),
  updateAdminJob: vi.fn(),
}));

test("imports a csv file and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({ total: 0, jobs: [] })
    .mockResolvedValueOnce({ total: 1, jobs: [{ id: 88, title: "Data Intern", companyName: "Campus Future", status: "DRAFT" }] });
  importAdminJobs.mockResolvedValue({
    fileName: "jobs.csv",
    totalRows: 1,
    importedCount: 1,
    defaultStatus: "DRAFT",
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  const file = new File(["csv"], "jobs.csv", { type: "text/csv" });
  const input = wrapper.find('input[name="jobImportFile"]');
  Object.defineProperty(input.element, "files", { value: [file] });
  await input.trigger("change");
  await wrapper.find('[data-testid="job-import-form"]').trigger("submit.prevent");
  await flushPromises();

  expect(importAdminJobs).toHaveBeenCalledTimes(1);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Imported 1 jobs as DRAFT");
});

test("renders row-level import errors returned by the backend", async () => {
  getAdminJobs.mockResolvedValue({ total: 0, jobs: [] });
  const requestError = new Error("job import validation failed");
  requestError.code = 400;
  requestError.data = {
    fileName: "jobs.csv",
    totalRows: 1,
    importedCount: 0,
    errors: [{ rowNumber: 2, column: "jobType", message: "invalid job type" }],
  };
  importAdminJobs.mockRejectedValue(requestError);

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  const file = new File(["csv"], "jobs.csv", { type: "text/csv" });
  const input = wrapper.find('input[name="jobImportFile"]');
  Object.defineProperty(input.element, "files", { value: [file] });
  await input.trigger("change");
  await wrapper.find('[data-testid="job-import-form"]').trigger("submit.prevent");
  await flushPromises();

  expect(getAdminJobs).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("job import validation failed");
  expect(wrapper.text()).toContain("Row 2");
  expect(wrapper.text()).toContain("invalid job type");
});
```

- [x] **Step 2: Run the targeted frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js
```

Expected: FAIL because the admin API does not expose `importAdminJobs`, the view has no CSV import surface, and rejected Axios requests do not preserve `data.errors`.

- [x] **Step 3: Implement the frontend import flow**

Add the admin API call:

```javascript
export async function importAdminJobs(formData) {
  const { data } = await http.post("/admin/jobs/import", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}
```

Preserve backend error data in `http.js`:

```javascript
const requestError = new Error(payload.message || "request failed");
requestError.code = payload.code;
requestError.data = payload.data ?? null;
```

Add import state to `AdminJobManageView.vue`:

```javascript
const importFile = ref(null);
const importLoading = ref(false);
const importSummary = ref(null);
const importErrors = ref([]);
const importErrorMessage = ref("");
const importInputRef = ref(null);

function handleImportFileChange(event) {
  importFile.value = event.target.files?.[0] || null;
}

async function handleImportJobs() {
  importErrorMessage.value = "";
  importErrors.value = [];
  importSummary.value = null;

  if (!importFile.value) {
    importErrorMessage.value = "Choose one CSV file first.";
    return;
  }

  const formData = new FormData();
  formData.append("file", importFile.value);
  importLoading.value = true;

  try {
    importSummary.value = await importAdminJobs(formData);
    await loadJobs();
  } catch (error) {
    importErrorMessage.value = error.message || "Job import failed. Please try again.";
    importErrors.value = error.data?.errors || [];
  } finally {
    importLoading.value = false;
  }
}
```

Render the new import panel inside the existing admin jobs page with:

- one file input accepting `.csv`
- short header guidance showing the required column order
- a submit button labeled `Import CSV`
- success summary text such as `Imported 12 jobs as DRAFT`
- a readable row-error list such as `Row 4 / sourceUrl: duplicate source url in file`

Implementation rules:

- keep the admin on `/admin/jobs`
- refresh the jobs list only after successful import
- leave the current jobs list untouched on validation failure
- keep the existing create/edit/publish/offline/delete flows intact

- [x] **Step 4: Re-run the frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js
```

Expected: PASS with import success refresh and row-error rendering covered.

- [x] **Step 5: Commit the admin jobs import UI**

```bash
git add frontend/src/api/admin.js frontend/src/api/http.js frontend/src/views/admin/AdminJobManageView.vue frontend/src/views/admin/AdminJobManageView.spec.js
git commit -m "feat: add admin job import workflow"
```

## Task 5: Update Docs And Run Cross-Surface Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-design.md`
- Verify: `backend/src/test/java/com/campus/service/JobImportCsvParserTests.java`
- Verify: `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`
- Verify: `frontend/src/views/admin/AdminJobManageView.spec.js`

- [x] **Step 1: Update README and add the Phase U validation note**

Update `README.md` to mention the new admin capability in the implemented feature set, for example:

```md
- Admin batch job import from UTF-8 CSV on `/admin/jobs`, with imported rows created as `DRAFT` and whole-file rollback on validation failure
```

Add a validation note near the top of the Phase U spec:

```md
> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-implementation.md`.
```

- [x] **Step 2: Run the targeted backend verification suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobImportCsvParserTests,JobBatchImportServiceTests,AdminJobImportControllerTests,AdminJobControllerTests,JobControllerTests" test
```

Expected: PASS.

- [x] **Step 3: Run the targeted frontend verification suite and production build**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js
npm run build
```

Expected: PASS.

- [x] **Step 4: Review the final diff for scope discipline**

Run:

```powershell
git diff --stat
git status --short
```

Confirm:

- only Phase U batch-import files changed
- no public jobs behavior changed
- no async import framework or import history slipped in
- no `xlsx` parser or third-party sync logic slipped in
- no partial-success behavior was mixed into this slice

- [x] **Step 5: Commit the docs and verification polish**

```bash
git add README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-design.md backend/src/test/java/com/campus/service/JobImportCsvParserTests.java backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java backend/src/test/java/com/campus/controller/JobControllerTests.java frontend/src/views/admin/AdminJobManageView.spec.js
git commit -m "docs: add phase u job import rollout notes"
```

## Execution Notes

- Do not add `xlsx` support in this phase.
- Do not add async status polling or import history.
- Do not auto-publish imported rows.
- Do not weaken the all-or-nothing import rule.
- Do not add database uniqueness constraints that would block re-import after `DELETED` jobs unless the spec is explicitly revised.
- Do not expand the frontend into a separate admin import route.
