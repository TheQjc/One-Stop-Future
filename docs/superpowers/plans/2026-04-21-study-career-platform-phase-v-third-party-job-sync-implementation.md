# Phase V Third-Party Job Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a synchronous admin-only third-party job sync on `/admin/jobs` that pulls one fixed HTTP JSON feed, creates missing rows as `DRAFT`, updates existing non-`DELETED` rows by `sourceUrl`, skips `DELETED` matches, and returns an immediate summary with item-level issues.

**Architecture:** Keep the sync slice inside the existing admin jobs surface by extending `AdminJobController`, adding one focused `ThirdPartyJobSyncService`, and binding the fixed feed through `JobSyncProperties`. Reuse current job field rules through one shared field normalizer so manual admin edits, CSV import, and feed sync stay aligned without turning this phase into a generic connector platform or scheduler.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, Jackson, JDK `java.net.http.HttpClient`, H2, JUnit 5, AssertJ, Vue 3, Vite, Vitest, Axios

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-v-third-party-job-sync-design.md`
- Adjacent completed slice:
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-u-admin-batch-job-import-implementation.md`
- Existing admin jobs backend:
  - `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
  - `backend/src/main/java/com/campus/service/AdminJobService.java`
  - `backend/src/main/java/com/campus/service/JobPostingDraftFactory.java`
  - `backend/src/main/java/com/campus/service/JobBatchImportService.java`
  - `backend/src/main/java/com/campus/entity/JobPosting.java`
  - `backend/src/main/java/com/campus/mapper/JobPostingMapper.java`
- Existing config and error handling patterns:
  - `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
  - `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-local.yml`
  - `backend/src/test/resources/application.yml`
- Existing admin-triggered reference flow:
  - `backend/src/main/java/com/campus/service/AdminResourceMigrationService.java`
  - `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Existing admin jobs frontend:
  - `frontend/src/api/admin.js`
  - `frontend/src/api/http.js`
  - `frontend/src/views/admin/AdminJobManageView.vue`
  - `frontend/src/views/admin/AdminJobManageView.spec.js`
- Existing backend tests to mirror:
  - `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`
  - `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`
  - `backend/src/test/java/com/campus/service/AdminResourceMigrationServiceTests.java`
  - `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`

## Scope Lock

This plan covers only the approved Phase V slice:

- add `POST /api/admin/jobs/sync`
- keep sync entry inside the existing `/admin/jobs` route
- fetch one server-configured `GET` JSON feed
- require the top-level payload shape `{ "jobs": [ ... ] }`
- create missing jobs as `DRAFT`
- update existing non-`DELETED` jobs in place by matching `sourceUrl`
- skip existing `DELETED` matches and report them as issues
- continue processing when one remote item is invalid
- fail the whole sync on feed-level problems
- keep new rows unpublished until an admin publishes them manually

This plan explicitly does not implement:

- multiple feeds
- scheduler-driven sync
- arbitrary admin-entered URLs
- sync history tables
- webhook callbacks or retries
- auto-offline or auto-delete when remote jobs disappear
- publish automation
- public jobs-page redesign
- a generic integration framework

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/config/JobSyncProperties.java`
  Bind the fixed feed configuration under `platform.integrations.job-sync`.
- Create: `backend/src/main/java/com/campus/dto/AdminJobSyncResponse.java`
  Return the sync summary payload with nested `issues`.
- Create: `backend/src/main/java/com/campus/service/JobPostingFieldNormalizer.java`
  Share trimming, enum normalization, URL validation, and deadline parsing rules.
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobFeedItem.java`
  Hold the backend-owned remote item shape returned by the feed client.
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobFeedClient.java`
  Call the fixed external feed and parse the top-level JSON contract.
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobSyncService.java`
  Orchestrate fetch, feed validation, upsert, skip rules, and summary building.
- Create: `backend/src/test/java/com/campus/service/JobPostingFieldNormalizerTests.java`
  Lock down shared normalization and validation rules.
- Create: `backend/src/test/java/com/campus/service/ThirdPartyJobSyncServiceTests.java`
  Cover end-to-end sync semantics with a local HTTP test server.
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobSyncControllerTests.java`
  Cover the sync endpoint boundary and error envelope.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
  Add the `POST /sync` action to the existing admin jobs controller.
- Modify: `backend/src/main/java/com/campus/service/AdminJobService.java`
  Reuse the shared field normalizer without changing manual job lifecycle behavior.
- Modify: `backend/src/main/java/com/campus/service/JobPostingDraftFactory.java`
  Reuse the shared field normalizer for CSV import row validation.
- Modify: `backend/src/main/resources/application.yml`
  Add default `job-sync` config mapping from environment variables.
- Modify: `backend/src/main/resources/application-local.yml`
  Keep sync disabled locally without pinning any feed URL.
- Modify: `backend/src/test/resources/application.yml`
  Keep sync disabled by default in tests.
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
  Verify `job-sync` defaults stay safe.

### Frontend: Modify Existing

- Modify: `frontend/src/api/admin.js`
  Add the thin `syncAdminJobs()` API helper.
- Modify: `frontend/src/views/admin/AdminJobManageView.vue`
  Add the sync panel, loading state, summary, issue list, and failure message.
- Modify: `frontend/src/views/admin/AdminJobManageView.spec.js`
  Cover successful sync refresh, issue rendering, and feed-failure rendering.

### Docs: Modify Existing

- Modify: `README.md`
  Record Phase V as implemented after the code lands.
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-v-third-party-job-sync-design.md`
  Add a post-implementation validation note after rollout.

### Verify Existing Files Without Planned Logic Changes

- Verify: `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`
- Verify: `frontend/src/views/admin/AdminJobManageView.spec.js`

## Responsibility Notes

- `JobSyncProperties` owns configuration only:
  - enabled flag
  - feed URL
  - source name
  - bearer token
  - connect and read timeouts
- `JobPostingFieldNormalizer` owns field rules only:
  - trim required and optional text
  - enforce max lengths
  - normalize `JobType`
  - normalize `JobEducationRequirement`
  - validate `sourceUrl`
  - parse `deadlineAt`
- `ThirdPartyJobFeedClient` owns transport and top-level feed parsing only:
  - send the request
  - add optional bearer auth
  - require valid JSON
  - require the top-level `jobs` array
  - convert each remote row into `ThirdPartyJobFeedItem`
- `ThirdPartyJobSyncService` owns:
  - admin lookup
  - feed-level duplicate detection
  - existing-row lookup by `sourceUrl`
  - create vs update vs skip behavior
  - item-level issue collection
  - summary counts
- `AdminJobController` should own request and response wiring only.
- `AdminJobManageView.vue` should own presentation and local UI state only.
- `frontend/src/api/http.js` already preserves backend `data` on rejected requests; do not expand this phase by rewriting the shared HTTP client unless a failing test proves it is necessary.

## Task 1: Add Safe Config Defaults And Shared Job Field Normalization

**Files:**
- Create: `backend/src/main/java/com/campus/config/JobSyncProperties.java`
- Create: `backend/src/main/java/com/campus/service/JobPostingFieldNormalizer.java`
- Create: `backend/src/test/java/com/campus/service/JobPostingFieldNormalizerTests.java`
- Modify: `backend/src/main/java/com/campus/service/AdminJobService.java`
- Modify: `backend/src/main/java/com/campus/service/JobPostingDraftFactory.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/resources/application.yml`
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`

- [x] **Step 1: Write the failing config and normalizer tests**

Add `JobPostingFieldNormalizerTests` to lock down the shared rules:

```java
@SpringBootTest
class JobPostingFieldNormalizerTests {

    @Autowired
    private JobPostingFieldNormalizer normalizer;

    @Test
    void normalizesEnumsAndDeadlineUsingSharedRules() {
        assertThat(normalizer.normalizeJobType(" full_time ")).isEqualTo("FULL_TIME");
        assertThat(normalizer.normalizeEducationRequirement(" master ")).isEqualTo("MASTER");
        assertThat(normalizer.parseDeadline("2026-06-20 18:00:00"))
                .isEqualTo(LocalDateTime.of(2026, 6, 20, 18, 0));
    }

    @Test
    void rejectsUnsupportedSourceUrlSchemes() {
        assertThatThrownBy(() -> normalizer.normalizeSourceUrl("ftp://partner.example/jobs/1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid source url");
    }

    @Test
    void trimsOptionalTextAndReturnsNullForBlankValues() {
        assertThat(normalizer.optionalText("  summary  ", 300, "summary")).isEqualTo("summary");
        assertThat(normalizer.optionalText("   ", 300, "summary")).isNull();
    }
}
```

Extend `ApplicationConfigSafetyTests` with sync-specific assertions:

```java
@Test
void defaultApplicationConfigKeepsJobSyncDisabledUntilExplicitlyEnabled() {
    Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

    assertThat(properties.getProperty("platform.integrations.job-sync.enabled"))
            .isEqualTo("${JOB_SYNC_ENABLED:false}");
    assertThat(properties.getProperty("platform.integrations.job-sync.source-name"))
            .isEqualTo("${JOB_SYNC_SOURCE_NAME:Partner Feed}");
}

@Test
void localAndTestConfigsKeepJobSyncDisabledWithoutPinnedFeedUrl() {
    Properties localProperties = loadYaml(Path.of("src", "main", "resources", "application-local.yml"));
    Properties testProperties = loadYaml(Path.of("src", "test", "resources", "application.yml"));

    assertThat(localProperties.getProperty("platform.integrations.job-sync.enabled")).isEqualTo("false");
    assertThat(localProperties.getProperty("platform.integrations.job-sync.feed-url")).isNull();
    assertThat(testProperties.getProperty("platform.integrations.job-sync.enabled")).isEqualTo("false");
}
```

- [x] **Step 2: Run the targeted backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobPostingFieldNormalizerTests,ApplicationConfigSafetyTests" test
```

Expected: FAIL because the new config properties bean and shared field normalizer do not exist yet.

- [x] **Step 3: Implement safe config defaults and the shared normalizer**

Create `JobSyncProperties`:

```java
@Data
@Component
@ConfigurationProperties(prefix = "platform.integrations.job-sync")
public class JobSyncProperties {

    private boolean enabled = false;
    private String feedUrl = "";
    private String sourceName = "Partner Feed";
    private String bearerToken = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
```

Create `JobPostingFieldNormalizer` with reusable rule methods:

```java
@Component
public class JobPostingFieldNormalizer {

    private static final DateTimeFormatter DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String requiredText(String value, int maxLength, String fieldName) { ... }

    public String optionalText(String value, int maxLength, String fieldName) { ... }

    public String normalizeJobType(String value) { ... }

    public String normalizeEducationRequirement(String value) { ... }

    public String normalizeSourceUrl(String value) { ... }

    public LocalDateTime parseDeadline(String value) { ... }
}
```

Update `AdminJobService` to reuse the new helper without changing current manual behavior:

```java
job.setJobType(fieldNormalizer.normalizeJobType(jobType));
job.setEducationRequirement(fieldNormalizer.normalizeEducationRequirement(educationRequirement));

private void validatePublishable(JobPosting job) {
    ...
    fieldNormalizer.normalizeSourceUrl(job.getSourceUrl());
}
```

Update `JobPostingDraftFactory` to convert `IllegalArgumentException` messages into existing row-level validation errors instead of duplicating the parsing logic.

Add safe defaults to config files:

```yaml
platform:
  integrations:
    job-sync:
      enabled: ${JOB_SYNC_ENABLED:false}
      feed-url: ${JOB_SYNC_FEED_URL:}
      source-name: ${JOB_SYNC_SOURCE_NAME:Partner Feed}
      bearer-token: ${JOB_SYNC_BEARER_TOKEN:}
      connect-timeout-ms: ${JOB_SYNC_CONNECT_TIMEOUT_MS:5000}
      read-timeout-ms: ${JOB_SYNC_READ_TIMEOUT_MS:10000}
```

Implementation rules:

- keep local and test configs explicitly disabled
- do not pin any feed URL in `application-local.yml`
- preserve current manual admin create/update behavior
- preserve current CSV import error messages where possible

- [x] **Step 4: Re-run the targeted backend tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobPostingFieldNormalizerTests,ApplicationConfigSafetyTests" test
```

Expected: PASS with shared normalization rules and safe `job-sync` defaults covered.

- [x] **Step 5: Commit the shared sync foundation**

```bash
git add backend/src/main/java/com/campus/config/JobSyncProperties.java backend/src/main/java/com/campus/service/JobPostingFieldNormalizer.java backend/src/test/java/com/campus/service/JobPostingFieldNormalizerTests.java backend/src/main/java/com/campus/service/AdminJobService.java backend/src/main/java/com/campus/service/JobPostingDraftFactory.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml backend/src/test/resources/application.yml backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java
git commit -m "feat: add job sync config and field normalizer foundation"
```

## Task 2: Implement The Feed Client And Sync Service Semantics

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AdminJobSyncResponse.java`
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobFeedItem.java`
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobFeedClient.java`
- Create: `backend/src/main/java/com/campus/service/ThirdPartyJobSyncService.java`
- Create: `backend/src/test/java/com/campus/service/ThirdPartyJobSyncServiceTests.java`

- [x] **Step 1: Write the failing sync service tests**

Create `ThirdPartyJobSyncServiceTests` as a `@SpringBootTest` integration suite with `@Sql`, `JdbcTemplate`, and a local JDK `HttpServer`:

```java
@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ThirdPartyJobSyncServiceTests {

    @Autowired
    private ThirdPartyJobSyncService service;

    @Autowired
    private JobSyncProperties jobSyncProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void syncCreatesUpdatesSkipsDeletedAndReportsInvalidItems() {
        jdbcTemplate.update("""
                INSERT INTO t_job_posting (
                  id, title, company_name, city, job_type, education_requirement, source_platform, source_url,
                  summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at
                ) VALUES (
                  99, 'Old Deleted Job', 'Legacy Partner', 'Hangzhou', 'FULL_TIME', 'BACHELOR', 'Partner Feed',
                  'https://partner.example/jobs/deleted-role', 'deleted', 'deleted', NULL, NULL, 'DELETED', 1, 1,
                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """);

        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "Java Backend Intern Updated",
                      "companyName": "Future Campus Tech",
                      "city": "Shanghai",
                      "jobType": "INTERNSHIP",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://jobs.example.com/future-campus-tech/backend-intern",
                      "summary": "Updated summary",
                      "content": "Updated content",
                      "deadlineAt": "2026-06-20 18:00:00"
                    },
                    {
                      "title": "Partner Data Analyst",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/data-analyst",
                      "summary": "New draft job",
                      "content": "Partner content",
                      "deadlineAt": "2026-06-30 18:00:00"
                    },
                    {
                      "title": "Deleted Match",
                      "companyName": "Legacy Partner",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/deleted-role",
                      "summary": "Should skip"
                    },
                    {
                      "title": "Broken Item",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "NOT_A_TYPE",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/broken",
                      "summary": "Broken"
                    }
                  ]
                }
                """);

        AdminJobSyncResponse response = service.syncJobs("1");

        assertThat(response.fetchedCount()).isEqualTo(4);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.issues()).extracting(AdminJobSyncResponse.Issue::type)
                .containsExactly("SKIPPED", "INVALID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_job_posting WHERE source_url = 'https://partner.example/jobs/data-analyst'",
                String.class)).isEqualTo("DRAFT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT city FROM t_job_posting WHERE source_url = 'https://jobs.example.com/future-campus-tech/backend-intern'",
                String.class)).isEqualTo("Shanghai");
    }

    @Test
    void duplicateSourceUrlInsideFeedFailsWholeSyncWithoutWrites() {
        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "One",
                      "companyName": "Partner",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/dup",
                      "summary": "first"
                    },
                    {
                      "title": "Two",
                      "companyName": "Partner",
                      "city": "Shanghai",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/dup",
                      "summary": "second"
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("invalid job sync feed");
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_posting WHERE source_url = 'https://partner.example/jobs/dup'",
                Integer.class)).isEqualTo(0);
    }

    @Test
    void malformedJsonFailsWholeSyncWithoutWrites() {
        serveRaw(200, "{ not-json");

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("invalid job sync feed");
                });
    }

    @Test
    void disabledIntegrationFailsBeforeCallingTheFeed() {
        jobSyncProperties.setEnabled(false);

        assertThatThrownBy(() -> service.syncJobs("1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("job sync unavailable");
                });
    }
}
```

- [x] **Step 2: Run the targeted sync service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ThirdPartyJobSyncServiceTests" test
```

Expected: FAIL because the sync response DTO, feed client, and sync service do not exist yet.

- [x] **Step 3: Implement the feed client and sync service**

Create `AdminJobSyncResponse` with nested issues:

```java
public record AdminJobSyncResponse(
        String sourceName,
        int fetchedCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int invalidCount,
        String defaultCreatedStatus,
        List<Issue> issues) {

    public record Issue(
            int itemIndex,
            String sourceUrl,
            String type,
            String message) {
    }
}
```

Create `ThirdPartyJobFeedItem`:

```java
public record ThirdPartyJobFeedItem(
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourceUrl,
        String summary,
        String content,
        String deadlineAt) {
}
```

Create `ThirdPartyJobFeedClient`:

```java
@Service
public class ThirdPartyJobFeedClient {

    private final JobSyncProperties jobSyncProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public List<ThirdPartyJobFeedItem> fetchJobs() {
        requireConfiguredFeed();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(jobSyncProperties.getFeedUrl()))
                .timeout(Duration.ofMillis(jobSyncProperties.getReadTimeoutMs()))
                .header("Accept", "application/json")
                .GET();

        if (!jobSyncProperties.getBearerToken().isBlank()) {
            builder.header("Authorization", "Bearer " + jobSyncProperties.getBearerToken().trim());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode jobsNode = root.get("jobs");
        if (jobsNode == null || !jobsNode.isArray()) {
            throw new BusinessException(500, "invalid job sync feed");
        }
        return StreamSupport.stream(jobsNode.spliterator(), false)
                .map(this::toItem)
                .toList();
    }
}
```

Create `ThirdPartyJobSyncService`:

```java
@Service
public class ThirdPartyJobSyncService {

    @Transactional
    public AdminJobSyncResponse syncJobs(String identity) {
        User admin = userService.requireByIdentity(identity);
        List<ThirdPartyJobFeedItem> feedItems = feedClient.fetchJobs();
        validateFeedDuplicates(feedItems);

        Map<String, JobPosting> existingBySourceUrl = loadExistingJobs(feedItems);
        LocalDateTime now = LocalDateTime.now();
        List<AdminJobSyncResponse.Issue> issues = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (int index = 0; index < feedItems.size(); index++) {
            int itemIndex = index + 1;
            ThirdPartyJobFeedItem item = feedItems.get(index);

            NormalizedJob normalized;
            try {
                normalized = normalize(item);
            } catch (IllegalArgumentException exception) {
                issues.add(new AdminJobSyncResponse.Issue(itemIndex, safeSourceUrl(item.sourceUrl()), "INVALID",
                        exception.getMessage()));
                continue;
            }

            JobPosting existing = existingBySourceUrl.get(normalized.sourceUrl());
            if (existing != null && JobPostingStatus.DELETED.name().equals(existing.getStatus())) {
                issues.add(new AdminJobSyncResponse.Issue(itemIndex, normalized.sourceUrl(), "SKIPPED",
                        "job is deleted locally"));
                continue;
            }

            if (existing == null) {
                jobPostingMapper.insert(newDraft(normalized, admin.getId(), now));
                createdCount++;
            } else {
                applySyncUpdate(existing, normalized, admin.getId(), now);
                jobPostingMapper.updateById(existing);
                updatedCount++;
            }
        }

        return new AdminJobSyncResponse(
                jobSyncProperties.getSourceName().trim(),
                feedItems.size(),
                createdCount,
                updatedCount,
                countIssues(issues, "SKIPPED"),
                countIssues(issues, "INVALID"),
                JobPostingStatus.DRAFT.name(),
                issues.stream()
                        .sorted(Comparator.comparingInt(AdminJobSyncResponse.Issue::itemIndex))
                        .toList());
    }
}
```

Implementation rules:

- treat disabled integration, blank `feedUrl`, blank `sourceName`, request failure, invalid JSON, and missing `jobs` as `BusinessException(500, ...)`
- detect duplicate `sourceUrl` values after trimming and before any database writes
- keep feed-level failures all-or-nothing
- preserve `status`, `publishedAt`, `createdBy`, and `createdAt` on updates
- set `sourcePlatform` from `jobSyncProperties.getSourceName()`
- do nothing for local jobs missing from the latest feed
- keep item-level invalid issues non-fatal

- [x] **Step 4: Re-run the targeted sync service tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ThirdPartyJobSyncServiceTests" test
```

Expected: PASS with create, update, deleted-skip, invalid-item, malformed-feed, and duplicate-feed coverage in place.

- [x] **Step 5: Commit the backend sync engine**

```bash
git add backend/src/main/java/com/campus/dto/AdminJobSyncResponse.java backend/src/main/java/com/campus/service/ThirdPartyJobFeedItem.java backend/src/main/java/com/campus/service/ThirdPartyJobFeedClient.java backend/src/main/java/com/campus/service/ThirdPartyJobSyncService.java backend/src/test/java/com/campus/service/ThirdPartyJobSyncServiceTests.java
git commit -m "feat: add third-party job sync service"
```

## Task 3: Expose The Admin Sync Endpoint On The Existing Jobs Controller

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminJobController.java`
- Create: `backend/src/test/java/com/campus/controller/admin/AdminJobSyncControllerTests.java`

- [x] **Step 1: Write the failing controller tests**

Create `AdminJobSyncControllerTests`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminJobSyncControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobSyncProperties jobSyncProperties;

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanTriggerJobSync() throws Exception {
        serveJson("""
                {
                  "jobs": [
                    {
                      "title": "Partner Data Analyst",
                      "companyName": "North Lake Studio",
                      "city": "Hangzhou",
                      "jobType": "FULL_TIME",
                      "educationRequirement": "BACHELOR",
                      "sourceUrl": "https://partner.example/jobs/data-analyst",
                      "summary": "New draft job"
                    }
                  ]
                }
                """);

        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceName").value("Partner Feed"))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.defaultCreatedStatus").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotTriggerJobSync() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestCannotTriggerJobSync() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/sync").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void feedFailureReturnsBusinessErrorEnvelope() throws Exception {
        serveRaw(200, "{ not-json");

        mockMvc.perform(post("/api/admin/jobs/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("invalid job sync feed"));
    }
}
```

- [x] **Step 2: Run the targeted controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobSyncControllerTests" test
```

Expected: FAIL because the sync endpoint does not exist on `AdminJobController` yet.

- [x] **Step 3: Implement the controller action**

Extend `AdminJobController`:

```java
@PostMapping("/sync")
public Result<AdminJobSyncResponse> sync(Authentication authentication) {
    return Result.success(thirdPartyJobSyncService.syncJobs(authentication.getName()));
}
```

Implementation rules:

- keep the controller-level `@PreAuthorize("hasRole('ADMIN')")`
- do not create a new controller class for this phase
- keep the request body empty
- keep the existing `/api/admin/jobs` route family unchanged

- [x] **Step 4: Re-run the targeted controller tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminJobSyncControllerTests" test
```

Expected: PASS with admin success, guest `401`, user `403`, and feed-failure envelope coverage.

- [x] **Step 5: Commit the sync endpoint**

```bash
git add backend/src/main/java/com/campus/controller/admin/AdminJobController.java backend/src/test/java/com/campus/controller/admin/AdminJobSyncControllerTests.java
git commit -m "feat: add admin job sync endpoint"
```

## Task 4: Add The `/admin/jobs` Sync Surface And Frontend Result Rendering

**Files:**
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/admin/AdminJobManageView.vue`
- Modify: `frontend/src/views/admin/AdminJobManageView.spec.js`

- [x] **Step 1: Write the failing frontend tests**

Extend `AdminJobManageView.spec.js`:

```javascript
import { getAdminJobs, syncAdminJobs } from "../../api/admin.js";

vi.mock("../../api/admin.js", () => ({
  createAdminJob: vi.fn(),
  deleteAdminJob: vi.fn(),
  getAdminJobs: vi.fn(),
  importAdminJobs: vi.fn(),
  offlineAdminJob: vi.fn(),
  publishAdminJob: vi.fn(),
  syncAdminJobs: vi.fn(),
  updateAdminJob: vi.fn(),
}));

test("syncs the configured feed and reloads the board", async () => {
  getAdminJobs
    .mockResolvedValueOnce({ total: 1, jobs: [{ id: 1, title: "Java Backend Intern", companyName: "Future Campus Tech", status: "PUBLISHED" }] })
    .mockResolvedValueOnce({ total: 2, jobs: [
      { id: 1, title: "Java Backend Intern Updated", companyName: "Future Campus Tech", status: "PUBLISHED" },
      { id: 99, title: "Partner Data Analyst", companyName: "North Lake Studio", status: "DRAFT" },
    ] });
  syncAdminJobs.mockResolvedValue({
    sourceName: "Partner Feed",
    fetchedCount: 4,
    createdCount: 1,
    updatedCount: 1,
    skippedCount: 1,
    invalidCount: 1,
    defaultCreatedStatus: "DRAFT",
    issues: [{
      itemIndex: 3,
      sourceUrl: "https://partner.example/jobs/deleted-role",
      type: "SKIPPED",
      message: "job is deleted locally",
    }],
  });

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="job-sync-button"]').trigger("click");
  await flushPromises();

  expect(syncAdminJobs).toHaveBeenCalledTimes(1);
  expect(getAdminJobs).toHaveBeenCalledTimes(2);
  expect(wrapper.text()).toContain("Partner Feed");
  expect(wrapper.text()).toContain("Created 1");
  expect(wrapper.text()).toContain("job is deleted locally");
});

test("renders sync failure without reloading the jobs board", async () => {
  getAdminJobs.mockResolvedValue({
    total: 1,
    jobs: [{ id: 1, title: "Java Backend Intern", companyName: "Future Campus Tech", status: "PUBLISHED" }],
  });
  syncAdminJobs.mockRejectedValue(new Error("invalid job sync feed"));

  const wrapper = mount(AdminJobManageView);
  await flushPromises();

  await wrapper.find('[data-testid="job-sync-button"]').trigger("click");
  await flushPromises();

  expect(getAdminJobs).toHaveBeenCalledTimes(1);
  expect(wrapper.text()).toContain("invalid job sync feed");
});
```

- [x] **Step 2: Run the targeted frontend tests and verify failure**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js
```

Expected: FAIL because there is no `syncAdminJobs()` API helper and no sync UI in the admin jobs view.

- [x] **Step 3: Implement the frontend sync flow**

Add the API helper:

```javascript
export async function syncAdminJobs() {
  const { data } = await http.post("/admin/jobs/sync");
  return data.data;
}
```

Add sync state and action handling to `AdminJobManageView.vue`:

```javascript
const syncLoading = ref(false);
const syncSummary = ref(null);
const syncIssues = ref([]);
const syncErrorMessage = ref("");

async function handleSyncJobs() {
  syncLoading.value = true;
  syncErrorMessage.value = "";
  syncSummary.value = null;
  syncIssues.value = [];

  try {
    const result = await syncAdminJobs();
    syncSummary.value = result;
    syncIssues.value = Array.isArray(result.issues) ? result.issues : [];
    await loadJobs();
  } catch (error) {
    syncErrorMessage.value = error.message || "Job sync failed. Please try again.";
  } finally {
    syncLoading.value = false;
  }
}
```

Render a sync panel inside the existing `/admin/jobs` page:

```vue
<article class="section-card">
  <div class="section-header">
    <div>
      <span class="section-eyebrow">Sync</span>
      <h2 class="page-title" style="margin-top: 16px;">Pull jobs from the configured partner feed</h2>
      <p class="page-subtitle" style="margin-top: 16px;">
        The backend fetches one fixed HTTP JSON feed and creates new rows as drafts.
      </p>
    </div>
  </div>

  <button
    type="button"
    class="app-btn"
    data-testid="job-sync-button"
    :disabled="syncLoading"
    @click="handleSyncJobs"
  >
    {{ syncLoading ? "Syncing..." : "Sync Feed" }}
  </button>

  <p v-if="syncSummary" class="field-hint">
    {{ syncSummary.sourceName }}: Created {{ syncSummary.createdCount }}, updated {{ syncSummary.updatedCount }},
    skipped {{ syncSummary.skippedCount }}, invalid {{ syncSummary.invalidCount }}.
  </p>
  <p v-if="syncErrorMessage" class="field-error" role="alert">{{ syncErrorMessage }}</p>
  <ul v-if="syncIssues.length" class="import-error-list">
    <li v-for="item in syncIssues" :key="`${item.itemIndex}-${item.type}-${item.sourceUrl}`">
      Item {{ item.itemIndex }} / {{ item.type }} / {{ item.sourceUrl || "no sourceUrl" }}: {{ item.message }}
    </li>
  </ul>
</article>
```

Implementation rules:

- refresh the jobs list only after successful sync
- keep the admin on `/admin/jobs`
- render both `SKIPPED` and `INVALID` issues
- do not remove or redesign the existing CSV import surface
- do not modify `frontend/src/api/http.js` unless a test proves the current error-data propagation is insufficient

- [x] **Step 4: Re-run the targeted frontend tests**

Run:

```powershell
cd frontend
npx vitest run src/views/admin/AdminJobManageView.spec.js
```

Expected: PASS with sync success refresh, issue rendering, and feed-failure rendering covered.

- [x] **Step 5: Commit the admin sync UI**

```bash
git add frontend/src/api/admin.js frontend/src/views/admin/AdminJobManageView.vue frontend/src/views/admin/AdminJobManageView.spec.js
git commit -m "feat: add admin job sync workflow"
```

## Task 5: Update Docs And Run Cross-Surface Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-v-third-party-job-sync-design.md`
- Verify: `backend/src/test/java/com/campus/service/JobPostingFieldNormalizerTests.java`
- Verify: `backend/src/test/java/com/campus/service/ThirdPartyJobSyncServiceTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobSyncControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java`
- Verify: `backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java`
- Verify: `backend/src/test/java/com/campus/controller/JobControllerTests.java`
- Verify: `frontend/src/views/admin/AdminJobManageView.spec.js`

- [x] **Step 1: Update README and add the Phase V validation note**

Update `README.md`:

```md
- admin third-party job sync from one fixed HTTP JSON feed on `/admin/jobs`, with new rows created as `DRAFT`, existing non-`DELETED` rows updated by `sourceUrl`, and skipped / invalid items reported in the same sync summary
```

Add a validation note near the top of the Phase V spec:

```md
> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-v-third-party-job-sync-implementation.md`.
```

- [x] **Step 2: Run the targeted backend verification suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=JobPostingFieldNormalizerTests,ThirdPartyJobSyncServiceTests,AdminJobSyncControllerTests,AdminJobControllerTests,AdminJobImportControllerTests,JobBatchImportServiceTests,JobControllerTests,ApplicationConfigSafetyTests" test
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

- only Phase V sync files changed
- no scheduler or history tables slipped in
- Phase U CSV import still works
- existing public jobs behavior still reads from the same published job data
- no generic connector abstraction or multi-feed UI slipped into this slice

- [x] **Step 5: Commit docs and rollout notes**

```bash
git add README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-v-third-party-job-sync-design.md backend/src/test/java/com/campus/service/JobPostingFieldNormalizerTests.java backend/src/test/java/com/campus/service/ThirdPartyJobSyncServiceTests.java backend/src/test/java/com/campus/controller/admin/AdminJobSyncControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminJobControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminJobImportControllerTests.java backend/src/test/java/com/campus/service/JobBatchImportServiceTests.java backend/src/test/java/com/campus/controller/JobControllerTests.java frontend/src/views/admin/AdminJobManageView.spec.js
git commit -m "docs: add phase v job sync rollout notes"
```

## Execution Notes

- Do not add scheduler support in this phase.
- Do not auto-publish synced jobs.
- Do not revive `DELETED` rows.
- Do not delete or offline rows that disappear from the feed.
- Do not change the CSV import contract while extracting shared normalization logic.
- Do not create a separate admin route for sync.
- Do not expand `frontend/src/api/http.js` unless an actual failing test proves the current rejected-payload handling is insufficient.
