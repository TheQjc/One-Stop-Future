# Study-Career Platform Phase J Historical Local Resource MinIO Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-only dry-run and execute path that migrates historical local raw resource files into MinIO without changing `storageKey`, deleting local files, or affecting existing resource download and preview flows.

**Architecture:** Keep the migration outside the user-facing `ResourceService` path and build it as an admin backend capability. Reuse the existing MinIO integration layer, add a safe local-file reader for historical storage keys, and expose MinIO object operations even when the active raw resource backend remains local so operators can migrate first and switch storage later.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MinIO Java SDK, JUnit 5, Mockito, AssertJ, Spring Boot test slices

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-17-study-career-platform-phase-j-historical-local-resource-minio-migration-design.md`
- Existing admin resource HTTP entry point:
  - `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Existing admin resource business service:
  - `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Existing raw-resource storage config:
  - `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
  - `backend/src/main/java/com/campus/config/ResourceStorageProperties.java`
  - `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
- Existing MinIO storage boundary:
  - `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
  - `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
  - `backend/src/main/java/com/campus/storage/MinioResourceFileStorage.java`
- Existing local path semantics reference:
  - `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
- Existing config and admin controller regression coverage:
  - `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Existing error-shape rule:
  - `backend/src/main/java/com/campus/config/GlobalExceptionHandler.java`
  - business and validation failures return HTTP `200` with body `code=400/500`, except Spring Security access-denied responses

## Scope Lock

This plan covers only the approved Phase J slice:

- add an admin-only migration endpoint under `/api/admin/resources`
- support `dryRun`, bounded `limit`, filter-by-status, filter-by-id, and filter-by-keyword
- migrate historical local raw files to MinIO using the existing `storageKey`
- keep local files after successful upload
- allow migration while `app.resource-storage.type=local` as long as MinIO integration is enabled
- return batch summaries with per-item `SUCCESS / SKIPPED / FAILED` outcomes

This plan explicitly does not implement:

- automatic runtime migration on read or preview
- local-plus-MinIO dual-read fallback
- deletion of local files after migration
- migration UI in the frontend
- database schema changes or per-row migration markers
- preview artifact migration

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/dto/AdminResourceMigrationRequest.java`
  Admin migration request DTO with validated batch controls.
- Create: `backend/src/main/java/com/campus/dto/AdminResourceMigrationResponse.java`
  Batch summary DTO with nested per-item result records.
- Create: `backend/src/main/java/com/campus/storage/LocalStoragePathResolver.java`
  Shared safe resolver for `storageKey -> local Path` semantics.
- Create: `backend/src/main/java/com/campus/storage/HistoricalLocalResourceReader.java`
  Historical local raw-file reader independent from the active `ResourceFileStorage` bean.
- Create: `backend/src/main/java/com/campus/service/AdminResourceMigrationService.java`
  Admin-only orchestration for dry-run and execution migration batches.
- Create: `backend/src/test/java/com/campus/storage/LocalStoragePathResolverTests.java`
  Path normalization and traversal-safety coverage.
- Create: `backend/src/test/java/com/campus/storage/HistoricalLocalResourceReaderTests.java`
  Reader behavior tests against temporary local files.
- Create: `backend/src/test/java/com/campus/service/AdminResourceMigrationServiceTests.java`
  Unit tests for batch filtering, dry-run semantics, MinIO writes, and partial failures.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
  Split MinIO infrastructure bean exposure from active storage backend selection.
- Modify: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
  Reuse `LocalStoragePathResolver` so local-storage and migration read paths share the same safety rules.
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
  Add the admin migration endpoint and wire the new service.
- Modify: `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
  Cover local-active-storage plus MinIO-enabled migration wiring.
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
  Cover access control, request validation, and response-shape behavior for the new endpoint.

### Docs: Modify Existing

- Modify: `README.md`
  Document the new admin migration capability and remove it from the “not implemented yet” list.

## Responsibility Notes

- `ResourceStorageConfiguration` should expose MinIO client/object-operation beans when `platform.integrations.minio.enabled=true`, even if the active raw resource backend remains `local`.
- `ResourceFileStorage` remains the user-facing raw storage abstraction; the migration must not route through it because the active implementation may still be local.
- `LocalStoragePathResolver` is the single source of truth for safe historical local path resolution; it should reject blank or escaping keys.
- `HistoricalLocalResourceReader` owns only local existence/open checks for stored historical files and must always resolve from `app.resource-storage.local-root`, never from the preview-artifact root.
- `AdminResourceMigrationService` owns request normalization, filtering, MinIO preflight checks, per-item outcome mapping, and batch summary construction.
- `AdminResourceController` owns only request binding, admin endpoint exposure, and `Result.success(...)` wrapping.

## Task 1: Expose MinIO Infrastructure Beans For Migration

**Files:**
- Modify: `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
- Modify: `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`

- [ ] **Step 1: Write the failing configuration tests**

Extend `ResourceStorageConfigurationTests` so it covers the new migration wiring requirement:

```java
@Test
void localStorageTypeCanStillExposeMinioOperationsWhenMinioIntegrationIsEnabled() {
    contextRunner
            .withPropertyValues(
                    "app.resource-storage.type=local",
                    "platform.integrations.minio.enabled=true",
                    "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
                    "platform.integrations.minio.access-key=minioadmin",
                    "platform.integrations.minio.secret-key=minioadmin",
                    "platform.integrations.minio.bucket=campus-platform")
            .run(context -> {
                assertThat(context).hasSingleBean(LocalResourceFileStorage.class);
                assertThat(context).hasSingleBean(MinioObjectOperations.class);
            });
}

@Test
void localStorageTypeDoesNotCreateMinioOperationsWhenMinioIntegrationIsDisabled() {
    contextRunner
            .withPropertyValues(
                    "app.resource-storage.type=local",
                    "platform.integrations.minio.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MinioObjectOperations.class));
}
```

Keep the existing fail-fast assertion for `app.resource-storage.type=minio` plus `platform.integrations.minio.enabled=false`.

- [ ] **Step 2: Run the targeted configuration tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceStorageConfigurationTests" test
```

Expected: FAIL because the current configuration only exposes MinIO beans when `app.resource-storage.type=minio`.

- [ ] **Step 3: Refactor the configuration to separate active-storage selection from MinIO infrastructure exposure**

Update `ResourceStorageConfiguration` so it follows this shape:

```java
@Bean
@ConditionalOnProperty(prefix = "platform.integrations.minio", name = "enabled", havingValue = "true")
MinioClient minioClient(MinioIntegrationProperties properties) {
    return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();
}

@Bean
@ConditionalOnProperty(prefix = "platform.integrations.minio", name = "enabled", havingValue = "true")
MinioObjectOperations minioObjectOperations(MinioClient client) {
    return new SdkMinioObjectOperations(client);
}

@Bean
@ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
Object minioStorageSelectionGuard(MinioIntegrationProperties properties) {
    if (!properties.isEnabled()) {
        throw new IllegalStateException("minio storage is selected but disabled");
    }
    return new Object();
}
```

Keep the `ResourceFileStorage` bean selection unchanged:

- `local` -> `LocalResourceFileStorage`
- `minio` -> `MinioResourceFileStorage`

and make the `minio` storage bean depend on the new MinIO infrastructure beans plus the guard.

- [ ] **Step 4: Run the configuration tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceStorageConfigurationTests" test
```

Expected: PASS with both the old fail-fast rule and the new migration-ready MinIO bean exposure covered.

- [ ] **Step 5: Commit the MinIO wiring refactor**

```bash
git add backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java
git commit -m "refactor: expose minio beans for historical migration"
```

## Task 2: Add Shared Safe Local Path Resolution And Historical Reader

**Files:**
- Create: `backend/src/test/java/com/campus/storage/LocalStoragePathResolverTests.java`
- Create: `backend/src/test/java/com/campus/storage/HistoricalLocalResourceReaderTests.java`
- Create: `backend/src/main/java/com/campus/storage/LocalStoragePathResolver.java`
- Create: `backend/src/main/java/com/campus/storage/HistoricalLocalResourceReader.java`
- Modify: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`

- [ ] **Step 1: Write the failing path-safety and reader tests**

Create path resolver tests with `@TempDir` coverage:

```java
@TempDir
Path tempDir;

@Test
void resolveNormalizesBackslashesInsideConfiguredRoot() {
    LocalStoragePathResolver resolver = new LocalStoragePathResolver(tempDir);

    Path resolved = resolver.resolve("2026\\04\\17\\resume.pdf");

    assertThat(resolved).isEqualTo(tempDir.resolve("2026/04/17/resume.pdf").normalize());
}

@Test
void resolveRejectsBlankKeysAndParentTraversal() {
    LocalStoragePathResolver resolver = new LocalStoragePathResolver(tempDir);

    assertThatThrownBy(() -> resolver.resolve("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("storage key is blank");
    assertThatThrownBy(() -> resolver.resolve("../escape.pdf"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("escapes local resource root");
}
```

Create reader tests:

```java
@Test
void readerCanOpenExistingHistoricalFile() throws Exception {
    Path storedFile = tempDir.resolve("2026/04/17/resume.pdf");
    Files.createDirectories(storedFile.getParent());
    Files.writeString(storedFile, "legacy-pdf");

    HistoricalLocalResourceReader reader = new HistoricalLocalResourceReader(tempDir.toString());

    assertThat(reader.exists("2026/04/17/resume.pdf")).isTrue();
    assertThat(new String(reader.open("2026/04/17/resume.pdf").readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("legacy-pdf");
}
```

- [ ] **Step 2: Run the targeted storage tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalStoragePathResolverTests,HistoricalLocalResourceReaderTests" test
```

Expected: FAIL because the resolver and historical reader classes do not exist yet.

- [ ] **Step 3: Implement the shared resolver, the historical reader, and the local-storage reuse**

Create the shared resolver:

```java
public class LocalStoragePathResolver {

    private final Path rootPath;

    public LocalStoragePathResolver(Path rootPath) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
    }

    public Path resolve(String storageKey) {
        String normalizedKey = normalizeStorageKey(storageKey);
        Path resolvedPath = rootPath.resolve(normalizedKey).normalize();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }
        return resolvedPath;
    }

    private String normalizeStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storage key is blank");
        }
        String normalized = storageKey.trim().replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")) {
            throw new IllegalArgumentException("storage key escapes local resource root");
        }
        return normalized;
    }
}
```

Create the historical reader:

```java
public class HistoricalLocalResourceReader {

    private final LocalStoragePathResolver pathResolver;

    public HistoricalLocalResourceReader(String localRoot) {
        this.pathResolver = new LocalStoragePathResolver(Path.of(localRoot));
    }

    public boolean exists(String storageKey) {
        return Files.exists(pathResolver.resolve(storageKey));
    }

    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(pathResolver.resolve(storageKey));
    }
}
```

Refactor `LocalResourceFileStorage` to replace its private `resolve(...)` logic with the shared resolver so both active local storage and historical migration use the same normalization rules.

- [ ] **Step 4: Run the storage tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalStoragePathResolverTests,HistoricalLocalResourceReaderTests" test
```

Expected: PASS with path traversal protection, slash normalization, and historical open/existence behavior covered.

- [ ] **Step 5: Commit the safe historical reader layer**

```bash
git add backend/src/test/java/com/campus/storage/LocalStoragePathResolverTests.java backend/src/test/java/com/campus/storage/HistoricalLocalResourceReaderTests.java backend/src/main/java/com/campus/storage/LocalStoragePathResolver.java backend/src/main/java/com/campus/storage/HistoricalLocalResourceReader.java backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java
git commit -m "feat: add safe historical local resource reader"
```

## Task 3: Implement Migration DTOs And Batch Service

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AdminResourceMigrationRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourceMigrationResponse.java`
- Create: `backend/src/test/java/com/campus/service/AdminResourceMigrationServiceTests.java`
- Create: `backend/src/main/java/com/campus/service/AdminResourceMigrationService.java`

- [ ] **Step 1: Write the failing service tests**

Create `AdminResourceMigrationServiceTests` with Mockito and temporary local files:

```java
@ExtendWith(MockitoExtension.class)
class AdminResourceMigrationServiceTests {

    @Mock
    private ResourceItemMapper resourceItemMapper;

    @Mock
    private UserService userService;

    @Mock
    private MinioObjectOperations minioObjectOperations;

    @TempDir
    Path tempDir;

    @Test
    void dryRunMarksResourceReadyWhenLocalFileExistsAndObjectIsMissing() throws Exception {
        ResourceItem resource = resource(7L, "Campus Resume", "PUBLISHED", "2026/04/17/resume.pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists("campus-platform", "2026/04/17/resume.pdf")).thenReturn(false);

        Path storedFile = tempDir.resolve("2026/04/17/resume.pdf");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "legacy");

        AdminResourceMigrationService service = service(tempDir, true, Optional.of(minioObjectOperations));

        AdminResourceMigrationResponse response = service.migrateResources("1",
                new AdminResourceMigrationRequest(true, List.of("PUBLISHED"), null, "resume", true, 100));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.items().get(0).outcome()).isEqualTo("SUCCESS");
        assertThat(response.items().get(0).message()).isEqualTo("ready to migrate");
    }

    @Test
    void executeContinuesAfterOneResourceFailsToUpload() throws Exception {
        ResourceItem first = resource(7L, "First Resume", "PUBLISHED", "2026/04/17/first.pdf");
        ResourceItem second = resource(8L, "Second Resume", "PUBLISHED", "2026/04/17/second.pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(anyString(), anyString())).thenReturn(false);
        doThrow(new IOException("boom")).when(minioObjectOperations)
                .putObject(eq("campus-platform"), eq("2026/04/17/first.pdf"), any(InputStream.class));

        writeLegacyFile("2026/04/17/first.pdf", "first");
        writeLegacyFile("2026/04/17/second.pdf", "second");

        AdminResourceMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations))
                .migrateResources("1", new AdminResourceMigrationRequest(false, null, null, null, true, 100));

        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
    }

    @Test
    void requestWithoutKeywordStillUsesDeterministicBoundedBatch() {
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(
                resource(7L, "First", "PUBLISHED", "2026/04/17/first.pdf"),
                resource(8L, "Second", "PUBLISHED", "2026/04/17/second.pdf"),
                resource(9L, "Third", "PUBLISHED", "2026/04/17/third.pdf")));

        AdminResourceMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations))
                .migrateResources("1", new AdminResourceMigrationRequest(true, null, null, null, true, 2));

        assertThat(response.requestedLimit()).isEqualTo(2);
        assertThat(response.matchedCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
    }
}
```

Also add one preflight test:

```java
@Test
void migrationFailsWhenMinioIntegrationIsDisabled() {
    AdminResourceMigrationService service = service(tempDir, false, Optional.empty());

    assertThatThrownBy(() -> service.migrateResources("1",
            new AdminResourceMigrationRequest(true, null, null, null, true, 100)))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(500);
}
```

- [ ] **Step 2: Run the targeted service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceMigrationServiceTests" test
```

Expected: FAIL because the request/response DTOs and migration service do not exist yet.

- [ ] **Step 3: Implement the DTOs and the batch migration service**

Create the request DTO with validation for `limit`:

```java
public record AdminResourceMigrationRequest(
        Boolean dryRun,
        List<String> statuses,
        List<@NotNull @Positive Long> resourceIds,
        String keyword,
        Boolean onlyMissingInMinio,
        @Min(1) @Max(200) Integer limit) {
}
```

Create the response DTO:

```java
public record AdminResourceMigrationResponse(
        boolean dryRun,
        int requestedLimit,
        int matchedCount,
        int processedCount,
        int successCount,
        int skippedCount,
        int failureCount,
        List<Item> items) {

    public record Item(
            Long resourceId,
            String title,
            String status,
            String storageKey,
            String outcome,
            String message) {
    }
}
```

Implement the service around a normalized request model:

```java
@Service
public class AdminResourceMigrationService {

    public AdminResourceMigrationResponse migrateResources(String identity, AdminResourceMigrationRequest request) {
        userService.requireByIdentity(identity);
        NormalizedRequest normalized = normalize(request);
        MinioObjectOperations operations = requireMinioOperations();
        ensureBucketReady(operations);

        List<ResourceItem> candidates = loadCandidates(normalized);
        List<AdminResourceMigrationResponse.Item> items = candidates.stream()
                .map(resource -> migrateOne(resource, normalized, operations))
                .toList();

        return summarize(normalized, candidates.size(), items);
    }
}
```

Implementation rules to follow:

- use `Optional<MinioObjectOperations>` in the constructor so the app can still start when MinIO integration is disabled
- require `platform.integrations.minio.enabled=true` at call time and throw `new BusinessException(500, "minio migration unavailable")` when preflight cannot start
- query `ResourceItemMapper.selectList(...)` with deterministic `orderByAsc(ResourceItem::getId)`
- apply `statuses` and `resourceIds` in the query wrapper
- apply case-insensitive keyword filtering in the query wrapper against `title`, `summary`, and `file_name`
- implement the case-insensitive keyword filter with a concrete MyBatis-Plus pattern such as:

```java
String likeKeyword = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
wrapper.and(query -> query
        .apply("LOWER(title) LIKE {0}", likeKeyword)
        .or()
        .apply("LOWER(summary) LIKE {0}", likeKeyword)
        .or()
        .apply("LOWER(file_name) LIKE {0}", likeKeyword));
```

so `ORDER BY id ASC` plus `LIMIT` can stay in SQL
- apply the batch `limit` in SQL only after all active filters have been added to the wrapper
- keep `dryRun=true`, `onlyMissingInMinio=true`, and `limit=100` as service-side defaults
- map blank or invalid `storageKey` to `SKIPPED`
- map missing local file to `SKIPPED`
- map MinIO object already existing with `onlyMissingInMinio=true` to `SKIPPED`
- map local I/O or MinIO I/O failures during per-item processing to `FAILED`
- when `onlyMissingInMinio=false`, upload without existence pre-check and allow overwrite by key
- define `matchedCount` as the number of resources returned for the current batch after all active filters and the final SQL `LIMIT`
- define `processedCount` as the number of per-item results produced for the current batch; in this phase it should always equal `items.size()` and normally match `matchedCount`

Recommended user-facing per-item messages:

- `ready to migrate`
- `uploaded to minio`
- `storage key is blank`
- `storage key escapes local resource root`
- `local file not found`
- `object already exists in minio`
- `failed to open local file`
- `failed to upload to minio`

- [ ] **Step 4: Run the service tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceMigrationServiceTests" test
```

Expected: PASS with dry-run, execute, overwrite policy, skip reasons, and partial success behavior covered.

- [ ] **Step 5: Commit the migration service layer**

```bash
git add backend/src/main/java/com/campus/dto/AdminResourceMigrationRequest.java backend/src/main/java/com/campus/dto/AdminResourceMigrationResponse.java backend/src/test/java/com/campus/service/AdminResourceMigrationServiceTests.java backend/src/main/java/com/campus/service/AdminResourceMigrationService.java
git commit -m "feat: add admin resource minio migration service"
```

## Task 4: Wire The Admin Endpoint And Controller Regression Tests

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write the failing controller tests**

Add a `@MockBean` for the new migration service to keep controller tests focused:

```java
@MockBean
private AdminResourceMigrationService adminResourceMigrationService;
```

Add access-control and response-shape coverage:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void normalUserCannotTriggerHistoricalMinioMigration() throws Exception {
    mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isForbidden());
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanDryRunHistoricalMinioMigration() throws Exception {
    when(adminResourceMigrationService.migrateResources(eq("1"), any()))
            .thenReturn(new AdminResourceMigrationResponse(
                    true,
                    100,
                    1,
                    1,
                    1,
                    0,
                    0,
                    List.of(new AdminResourceMigrationResponse.Item(
                            7L,
                            "Campus Resume",
                            "PUBLISHED",
                            "2026/04/17/resume.pdf",
                            "SUCCESS",
                            "ready to migrate"))));

    mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"dryRun":true,"statuses":["PUBLISHED"],"keyword":"resume","limit":100}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.successCount").value(1))
            .andExpect(jsonPath("$.data.items[0].message").value("ready to migrate"));
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void invalidMigrationLimitReturnsBodyCode400() throws Exception {
    mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"limit":201}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
}
```

- [ ] **Step 2: Run the targeted controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceControllerTests" test
```

Expected: FAIL because the controller does not expose the migration endpoint or inject the new service yet.

- [ ] **Step 3: Implement the endpoint**

Update the controller constructor and add the new endpoint:

```java
@PostMapping("/migrate-to-minio")
public Result<AdminResourceMigrationResponse> migrateToMinio(
        Authentication authentication,
        @Validated @RequestBody(required = false) AdminResourceMigrationRequest request) {
    AdminResourceMigrationRequest normalizedRequest = request == null
            ? new AdminResourceMigrationRequest(null, null, null, null, null, null)
            : request;
    return Result.success(adminResourceMigrationService.migrateResources(authentication.getName(), normalizedRequest));
}
```

Keep the existing `publish`, `reject`, and `offline` endpoints unchanged.

- [ ] **Step 4: Run the controller tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceControllerTests" test
```

Expected: PASS with forbidden access, validated request shape, and `Result.success(...)` wrapping preserved.

- [ ] **Step 5: Commit the admin endpoint**

```bash
git add backend/src/main/java/com/campus/controller/admin/AdminResourceController.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: add admin minio migration endpoint"
```

## Task 5: Update README And Run Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README for the new admin migration capability**

Make all of these documentation updates:

- move historical local-resource MinIO migration from “not implemented yet” into the implemented feature list
- document that migration is admin-triggered and backend-only
- document that local files are preserved after successful migration
- document that `platform.integrations.minio.enabled=true` is required even if active raw storage remains local, and that this is provided through the existing runtime env mapping `MINIO_ENABLED=true`
- document that preview artifacts are still outside migration scope

Recommended README additions:

```md
- admin historical local-resource MinIO migration with dry-run and bounded batch execution
```

```md
- MinIO-backed preview artifact storage
- DOCX online preview
```

```md
Migration note:

- admins can call `POST /api/admin/resources/migrate-to-minio` to dry-run or execute historical raw-file migration into MinIO
- migration keeps the existing `storageKey` and leaves local files in place
- migration reads source files from `app.resource-storage.local-root`
- migration requires `platform.integrations.minio.enabled=true` and access to the existing local raw resource root
- in environment-variable-based deployments, that MinIO enablement is supplied by `MINIO_ENABLED=true`
```

- [ ] **Step 2: Run focused backend tests for the new migration slice**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceStorageConfigurationTests,LocalStoragePathResolverTests,HistoricalLocalResourceReaderTests,AdminResourceMigrationServiceTests,AdminResourceControllerTests" test
```

Expected: PASS.

- [ ] **Step 3: Run full backend regression**

Run:

```powershell
cd backend
mvn test
```

Expected: PASS.

- [ ] **Step 4: Run optional live migration smoke when MinIO is available**

If a reachable MinIO instance is available locally, run a manual smoke covering the key acceptance path:

1. Start the backend with `app.resource-storage.type=local` and `platform.integrations.minio.enabled=true`.
2. Seed or reuse at least one resource row whose `storageKey` points to an existing file under the local raw-resource root.
3. Call `POST /api/admin/resources/migrate-to-minio` with `{"dryRun":true,"limit":10}` as an admin.
4. Call the same endpoint with `{"dryRun":false,"limit":10}`.
5. Verify the MinIO bucket now contains the object under the same `storageKey`.
6. Verify the original local file still exists.

If live MinIO is unavailable in the execution environment, record that automated and repository-safe tests passed but live smoke was not executed.

- [ ] **Step 5: Commit the docs and verification updates**

```bash
git add README.md
git commit -m "docs: add historical minio migration usage notes"
```

## Execution Notes

- Keep the migration serial in this phase; do not add async workers or parallel uploads.
- Keep top-level response behavior aligned with the existing repository convention: request validation and preflight failures return HTTP `200` with body `code=400/500`, while forbidden access remains HTTP `403`.
- Do not mutate `t_resource_item` rows during migration.
- Do not route migration through `ResourceFileStorage`; the active storage backend may still be local.
- Do not delete local files after successful upload.
- Do not add frontend code unless a later approved phase explicitly asks for an admin UI.
