# Phase W Historical Preview Artifact MinIO Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an admin-only dry-run and execute path that migrates the current logical `PPTX`, `DOCX`, and `ZIP` preview artifacts from the historical local preview root into the Phase T MinIO preview namespace without changing runtime preview behavior, database schema, or local-profile defaults.

**Architecture:** Keep migration outside the normal preview read path by extending the existing admin resources surface with one focused `AdminResourcePreviewMigrationService`. Reuse `ResourcePreviewService` as the source of truth for current artifact-key derivation, add a shared local-preview path resolver plus a historical local preview reader for safe local artifact access, and reuse `MinioResourcePreviewArtifactStorage` as the MinIO target abstraction even when the active preview backend remains `local`.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MyBatis-Plus, MinIO Java SDK via `MinioObjectOperations`, H2, JUnit 5, AssertJ, Spring Boot Test

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md`
- Adjacent completed preview slices:
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-s-docx-resource-preview-implementation.md`
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-implementation.md`
- Existing admin-triggered migration reference flow:
  - `backend/src/main/java/com/campus/service/AdminResourceMigrationService.java`
  - `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
  - `backend/src/test/java/com/campus/service/AdminResourceMigrationServiceTests.java`
- Existing preview storage and key ownership:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  - `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Existing shared MinIO integration:
  - `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
  - `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
- Existing resource admin controller tests to mirror:
  - `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Existing preview test patterns to mirror:
  - `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
  - `backend/src/test/java/com/campus/storage/LocalStoragePathResolverTests.java`
  - `backend/src/test/java/com/campus/storage/HistoricalLocalResourceReaderTests.java`
- Existing docs that must be updated when the phase lands:
  - `README.md`
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md`

## Scope Lock

This plan covers only the approved Phase W slice:

- add `POST /api/admin/resources/migrate-preview-artifacts-to-minio`
- keep the migration entry inside the existing `/api/admin/resources` backend surface
- support `dryRun`, bounded `limit`, `statuses`, `resourceIds`, `keyword`, and `onlyMissingInMinio`
- select candidate rows from `t_resource_item`, not by recursively scanning the preview root
- migrate only the current logical preview artifact derived from current resource metadata
- support `PPTX`, `DOCX`, and `ZIP` derived artifacts
- report unsupported types such as `PDF` as skipped instead of failing
- allow migration while `app.resource-preview.type=local` as long as MinIO integration is enabled and the local preview root is readable
- preserve local preview artifacts after successful migration

This plan explicitly does not implement:

- runtime local-plus-MinIO dual-read fallback
- automatic regeneration of missing preview artifacts during migration
- migration of stale historical fingerprint artifacts
- automatic cleanup or garbage collection
- database schema changes
- frontend admin UI changes
- scheduler-driven or startup-time migration

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java`
  Resolve logical preview artifact keys under `app.resource-preview.local-root` with preview-root-specific escape protection.
- Create: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
  Check and open local preview artifacts for migration using the shared resolver.
- Create: `backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationRequest.java`
  Carry admin migration filters and execution mode for preview artifacts.
- Create: `backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationResponse.java`
  Return batch summary plus per-resource preview-artifact results.
- Create: `backend/src/main/java/com/campus/service/AdminResourcePreviewMigrationService.java`
  Orchestrate candidate selection, preview-type resolution, dry-run evaluation, execution, and summary building.
- Create: `backend/src/test/java/com/campus/preview/LocalPreviewArtifactPathResolverTests.java`
  Lock down preview-root path normalization and escape rejection.
- Create: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
  Cover local preview artifact existence and open behavior.
- Create: `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`
  Cover dry-run, skip rules, upload behavior, overwrite behavior, and partial failures.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
  Reuse the new preview-root path resolver instead of maintaining a private copy of the path rules.
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  Expose the current preview artifact target for migration while keeping preview-generation behavior unchanged.
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
  Add the new admin preview-artifact migration endpoint and request normalization.
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  Lock down artifact-target derivation for `PPTX`, `DOCX`, `ZIP`, and unsupported types.
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
  Cover the new endpoint’s authorization, validation, and response serialization.

### Docs: Modify Existing

- Modify: `README.md`
  Record historical preview-artifact migration as implemented and keep dual-read plus cleanup explicitly out of scope.
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md`
  Add the post-implementation validation note after rollout.

### Verify Existing Files Without Planned Logic Changes

- Verify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Verify: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`

## Responsibility Notes

- `ResourcePreviewService` must remain the source of truth for current preview artifact key derivation and fingerprint semantics.
- `LocalPreviewArtifactPathResolver` must own preview-root path normalization rules:
  - normalize backslashes
  - reject blank keys
  - reject leading-slash keys
  - reject `..` traversal
  - enforce that the resolved path stays inside the preview root
- `HistoricalLocalResourcePreviewArtifactReader` must own local artifact existence and open checks only.
- `AdminResourcePreviewMigrationService` must own:
  - admin identity lookup
  - request normalization
  - candidate row filtering
  - preview-type detection via `ResourcePreviewService`
  - per-item skip and failure decisions
  - batch summary counts
- `AdminResourceController` must own HTTP request and response wiring only.
- Do not expand this phase by changing public preview endpoints, adding preview-generation side effects to migration, or mutating `t_resource_item`.

## Task 1: Share Safe Local Preview Artifact Path Resolution

**Files:**
- Create: `backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java`
- Create: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
- Create: `backend/src/test/java/com/campus/preview/LocalPreviewArtifactPathResolverTests.java`
- Create: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`

- [ ] **Step 1: Write the failing resolver and reader tests**

Create `LocalPreviewArtifactPathResolverTests`:

```java
class LocalPreviewArtifactPathResolverTests {

    @TempDir
    Path tempDir;

    @Test
    void resolveNormalizesBackslashesInsideConfiguredPreviewRoot() {
        LocalPreviewArtifactPathResolver resolver = new LocalPreviewArtifactPathResolver(tempDir);

        Path resolved = resolver.resolve("pptx\\9\\fingerprint.pdf");

        assertThat(resolved).isEqualTo(tempDir.resolve("pptx/9/fingerprint.pdf").normalize());
    }

    @Test
    void resolveRejectsBlankAndEscapingArtifactKeys() {
        LocalPreviewArtifactPathResolver resolver = new LocalPreviewArtifactPathResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifact key is blank");
        assertThatThrownBy(() -> resolver.resolve("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
    }
}
```

Create `HistoricalLocalResourcePreviewArtifactReaderTests`:

```java
class HistoricalLocalResourcePreviewArtifactReaderTests {

    @TempDir
    Path tempDir;

    @Test
    void readerCanOpenExistingHistoricalPreviewArtifact() throws Exception {
        Path storedFile = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "%PDF");

        HistoricalLocalResourcePreviewArtifactReader reader =
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString());

        assertThat(reader.exists("pptx/9/fingerprint.pdf")).isTrue();
        assertThat(new String(reader.open("pptx/9/fingerprint.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("%PDF");
    }
}
```

- [ ] **Step 2: Run the targeted backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalPreviewArtifactPathResolverTests,HistoricalLocalResourcePreviewArtifactReaderTests" test
```

Expected: FAIL because the new preview resolver and historical preview reader do not exist yet, and `LocalResourcePreviewArtifactStorage` still owns its own private path logic.

- [ ] **Step 3: Implement the shared preview-root resolver and reader**

Create the resolver:

```java
public class LocalPreviewArtifactPathResolver {

    private final Path rootPath;

    public LocalPreviewArtifactPathResolver(Path rootPath) {
        this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
    }

    public Path resolve(String artifactKey) {
        String normalized = normalizeArtifactKey(artifactKey);
        Path resolved = rootPath.resolve(normalized).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IllegalArgumentException("artifact key escapes local preview root");
        }
        return resolved;
    }
}
```

Create the reader:

```java
public class HistoricalLocalResourcePreviewArtifactReader {

    private final LocalPreviewArtifactPathResolver pathResolver;

    public HistoricalLocalResourcePreviewArtifactReader(String localRoot) {
        this.pathResolver = new LocalPreviewArtifactPathResolver(Path.of(localRoot));
    }

    public boolean exists(String artifactKey) {
        return Files.exists(pathResolver.resolve(artifactKey));
    }

    public InputStream open(String artifactKey) throws IOException {
        return Files.newInputStream(pathResolver.resolve(artifactKey));
    }
}
```

Refactor `LocalResourcePreviewArtifactStorage` to depend on `LocalPreviewArtifactPathResolver` instead of its private `resolve(...)` and `normalizeKey(...)` helpers.

- [ ] **Step 4: Re-run the targeted tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalPreviewArtifactPathResolverTests,HistoricalLocalResourcePreviewArtifactReaderTests" test
```

Expected: PASS with backslash normalization, root-escape rejection, and local preview artifact reads covered.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/test/java/com/campus/preview/LocalPreviewArtifactPathResolverTests.java backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java
git commit -m "refactor: share local preview artifact path resolution"
```

## Task 2: Expose Current Preview Artifact Targets For Migration

**Files:**
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [ ] **Step 1: Write the failing artifact-target tests**

Extend `ResourcePreviewServiceTests`:

```java
@Test
void previewArtifactTargetOfReturnsPptxLogicalArtifactKey() {
    ResourcePreviewService service = new ResourcePreviewService(
            new NoopStorage(),
            new ObjectMapper(),
            new NoopPptxPreviewGenerator(),
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
    ResourceItem resource = resource(9L, "career-deck.pptx", "pptx", "seed/career-deck.pptx", 1024L, LocalDateTime.now());

    assertThat(service.previewArtifactTargetOf(resource))
            .contains(new ResourcePreviewService.PreviewArtifactTarget(
                    "PPTX",
                    service.pptxArtifactKeyOf(resource)));
}

@Test
void previewArtifactTargetOfReturnsEmptyForPdfResources() {
    ResourcePreviewService service = new ResourcePreviewService(
            new NoopStorage(),
            new ObjectMapper(),
            new NoopPptxPreviewGenerator(),
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
    ResourceItem resource = resource(9L, "resume.pdf", "pdf", "seed/resume.pdf", 1024L, LocalDateTime.now());

    assertThat(service.previewArtifactTargetOf(resource)).isEmpty();
}
```

Add one `DOCX` and one `ZIP` assertion in the same style so the three supported artifact types are locked down explicitly.

- [ ] **Step 2: Run the targeted preview-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: FAIL because `ResourcePreviewService` does not yet expose a migration-facing artifact target method or record.

- [ ] **Step 3: Implement the migration-facing artifact target helper**

Add a new public helper:

```java
public Optional<PreviewArtifactTarget> previewArtifactTargetOf(ResourceItem resource) {
    if (resource == null) {
        return Optional.empty();
    }
    if (isPptx(resource)) {
        return Optional.of(new PreviewArtifactTarget("PPTX", pptxArtifactKeyOf(resource)));
    }
    if (isDocx(resource)) {
        return Optional.of(new PreviewArtifactTarget("DOCX", docxArtifactKeyOf(resource)));
    }
    if (isZip(resource)) {
        return Optional.of(new PreviewArtifactTarget("ZIP", zipArtifactKeyOf(resource)));
    }
    return Optional.empty();
}

public record PreviewArtifactTarget(String previewType, String artifactKey) {
}
```

Keep the existing preview generation and cache lookup flows unchanged.

- [ ] **Step 4: Re-run the targeted tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: PASS with the new target-derivation contract covered alongside the existing cache and error-mapping tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "refactor: expose preview artifact migration targets"
```

## Task 3: Add The Admin Preview Artifact Migration Service

**Files:**
- Create: `backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationRequest.java`
- Create: `backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationResponse.java`
- Create: `backend/src/main/java/com/campus/service/AdminResourcePreviewMigrationService.java`
- Create: `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`

- [ ] **Step 1: Write the failing service tests**

Create `AdminResourcePreviewMigrationServiceTests` with focused cases:

```java
@Test
void dryRunMarksCurrentPptxArtifactReadyWhenLocalArtifactExists() throws Exception {
    ResourceItem resource = resource(7L, "Career Deck", "PUBLISHED", "career-deck.pptx", "pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "seed/career-deck.pptx");
    when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
    when(userService.requireByIdentity("1")).thenReturn(adminUser());
    when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);

    ResourcePreviewService previewService = previewService();
    String artifactKey = previewService.pptxArtifactKeyOf(resource);
    writeLegacyArtifact(artifactKey, "%PDF");

    AdminResourcePreviewMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations), previewService)
            .migratePreviewArtifacts("1",
                    new AdminResourcePreviewMigrationRequest(true, List.of("PUBLISHED"), List.of(7L), " deck ", true, 100));

    assertThat(response.items().get(0).previewType()).isEqualTo("PPTX");
    assertThat(response.items().get(0).artifactKey()).isEqualTo(artifactKey);
    assertThat(response.items().get(0).message()).isEqualTo("ready to migrate");
}

@Test
void unsupportedPreviewTypeIsReportedAsSkipped() {
    ResourceItem resource = resource(8L, "Resume PDF", "PUBLISHED", "resume.pdf", "pdf", "application/pdf", "seed/resume.pdf");
    when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
    when(userService.requireByIdentity("1")).thenReturn(adminUser());
    when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);

    AdminResourcePreviewMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations), previewService())
            .migratePreviewArtifacts("1", new AdminResourcePreviewMigrationRequest(null, null, null, null, null, 100));

    assertThat(response.items().get(0).previewType()).isEqualTo("NONE");
    assertThat(response.items().get(0).artifactKey()).isNull();
    assertThat(response.items().get(0).message()).isEqualTo("preview not supported");
}

@Test
void executeContinuesAfterOneArtifactFailsToUpload() throws Exception {
    // one supported artifact upload throws IOException, the second succeeds
}

@Test
void migrationFailsWhenMinioIntegrationIsDisabled() {
    // expect BusinessException(500, "minio preview migration unavailable")
}
```

Mirror the query-wrapper assertions from `AdminResourceMigrationServiceTests` so `statuses`, `resourceIds`, `keyword`, and bounded `limit` stay deterministic.

- [ ] **Step 2: Run the targeted backend tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourcePreviewMigrationServiceTests" test
```

Expected: FAIL because the new request/response DTOs and preview migration service do not exist yet.

- [ ] **Step 3: Implement the DTOs and migration service**

Create the request DTO:

```java
public record AdminResourcePreviewMigrationRequest(
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
public record AdminResourcePreviewMigrationResponse(
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
            String previewType,
            String artifactKey,
            String outcome,
            String message) {
    }
}
```

Implement `AdminResourcePreviewMigrationService` in the same structural style as `AdminResourceMigrationService`:

```java
public AdminResourcePreviewMigrationResponse migratePreviewArtifacts(
        String identity,
        AdminResourcePreviewMigrationRequest request) {
    userService.requireByIdentity(identity);
    NormalizedRequest normalized = normalize(request);
    ResourcePreviewArtifactStorage targetStorage = requireMinioPreviewTargetStorage();

    List<ResourceItem> candidates = loadCandidates(normalized);
    List<AdminResourcePreviewMigrationResponse.Item> items = candidates.stream()
            .map(resource -> migrateOne(resource, normalized, targetStorage))
            .toList();

    return summarize(normalized, items);
}
```

Key implementation rules:

- build `HistoricalLocalResourcePreviewArtifactReader` from `ResourcePreviewProperties.getLocalRoot()`
- build a dedicated MinIO target storage with:

```java
new MinioResourcePreviewArtifactStorage(
        minioIntegrationProperties.getBucket(),
        resourcePreviewProperties.getMinioPrefix(),
        minioObjectOperations.get())
```

- use `resourcePreviewService.previewArtifactTargetOf(resource)` to resolve `previewType` and `artifactKey`
- return `SKIPPED` with `previewType=NONE` when the current resource type has no derived artifact
- return `SKIPPED` when the local artifact is missing
- when `onlyMissingInMinio=true`, use `targetStorage.exists(artifactKey)` before writing
- when `dryRun=true`, return `SUCCESS` with `ready to migrate`
- when executing, open the local artifact through `HistoricalLocalResourcePreviewArtifactReader` and upload it through `targetStorage.write(artifactKey, inputStream)`
- keep local artifacts and database rows untouched

- [ ] **Step 4: Re-run the targeted tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourcePreviewMigrationServiceTests" test
```

Expected: PASS with dry-run, skip rules, overwrite behavior, MinIO failure handling, and bounded query behavior covered.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationRequest.java backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationResponse.java backend/src/main/java/com/campus/service/AdminResourcePreviewMigrationService.java backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java
git commit -m "feat: add admin preview artifact migration service"
```

## Task 4: Wire The Admin Endpoint And Controller Coverage

**Files:**
- Modify: `backend/src/main/java/com/campus/controller/admin/AdminResourceController.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write the failing controller tests**

Extend `AdminResourceControllerTests`:

```java
@MockBean
private AdminResourcePreviewMigrationService adminResourcePreviewMigrationService;

@Test
@WithMockUser(username = "2", roles = "USER")
void normalUserCannotTriggerHistoricalPreviewArtifactMigration() throws Exception {
    mockMvc.perform(post("/api/admin/resources/migrate-preview-artifacts-to-minio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isForbidden());
}

@Test
@WithMockUser(username = "1", roles = "ADMIN")
void adminCanDryRunHistoricalPreviewArtifactMigration() throws Exception {
    when(adminResourcePreviewMigrationService.migratePreviewArtifacts(eq("1"), any()))
            .thenReturn(new AdminResourcePreviewMigrationResponse(
                    true,
                    100,
                    1,
                    1,
                    1,
                    0,
                    0,
                    List.of(new AdminResourcePreviewMigrationResponse.Item(
                            7L,
                            "Career Deck",
                            "PUBLISHED",
                            "PPTX",
                            "pptx/7/fingerprint.pdf",
                            "SUCCESS",
                            "ready to migrate"))));

    mockMvc.perform(post("/api/admin/resources/migrate-preview-artifacts-to-minio")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"dryRun":true,"statuses":["PUBLISHED"],"keyword":"deck","limit":100}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.items[0].previewType").value("PPTX"))
            .andExpect(jsonPath("$.data.items[0].artifactKey").value("pptx/7/fingerprint.pdf"));
}
```

Also add an invalid-limit test for the new request DTO so `{"limit":201}` still returns body `code=400` through the existing exception envelope.

- [ ] **Step 2: Run the targeted controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceControllerTests" test
```

Expected: FAIL because the controller does not yet inject the new service or expose the preview-artifact migration endpoint.

- [ ] **Step 3: Implement the new controller endpoint**

Inject the new service and add:

```java
@PostMapping("/migrate-preview-artifacts-to-minio")
public Result<AdminResourcePreviewMigrationResponse> migratePreviewArtifactsToMinio(
        Authentication authentication,
        @Validated @RequestBody(required = false) AdminResourcePreviewMigrationRequest request) {
    AdminResourcePreviewMigrationRequest normalizedRequest = request == null
            ? new AdminResourcePreviewMigrationRequest(null, null, null, null, null, null)
            : request;
    return Result.success(
            adminResourcePreviewMigrationService.migratePreviewArtifacts(authentication.getName(), normalizedRequest));
}
```

Keep the existing raw-resource migration endpoint untouched.

- [ ] **Step 4: Re-run the targeted controller tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=AdminResourceControllerTests" test
```

Expected: PASS with the new admin-only preview migration endpoint covered alongside the existing admin resource tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/campus/controller/admin/AdminResourceController.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: add admin preview artifact migration endpoint"
```

## Task 5: Update Rollout Docs And Validation Notes

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md`

- [ ] **Step 1: Update README for the new admin preview migration capability**

Update `README.md` so it:

- adds historical preview-artifact MinIO migration to the implemented feature list
- removes historical preview-artifact migration from the “Explicitly not implemented yet” list
- keeps `local+MinIO dual-read fallback` and `automatic preview-artifact cleanup` in the “not implemented yet” list
- documents the new admin backend endpoint under the preview-storage section
- documents that migration only covers the current logical preview artifact for each eligible resource
- documents that local source preview artifacts are preserved after migration

Recommended wording to add under preview storage notes:

```markdown
- admins can call `POST /api/admin/resources/migrate-preview-artifacts-to-minio` to dry-run or execute historical preview-artifact migration into MinIO
- migration targets the current logical `PPTX`, `DOCX`, or `ZIP` preview artifact derived from the current resource row
- migration keeps local source preview artifacts in place after successful upload
- dual-read fallback and preview-artifact garbage collection remain out of scope in this phase
```

- [ ] **Step 2: Add the validation note to the Phase W spec**

Prepend a validation note after implementation is complete:

```markdown
> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-implementation.md`. Local verification covered `LocalPreviewArtifactPathResolverTests`, `HistoricalLocalResourcePreviewArtifactReaderTests`, `ResourcePreviewServiceTests`, `AdminResourcePreviewMigrationServiceTests`, `AdminResourceControllerTests`, `ResourceControllerTests`, and the MinIO preview storage regression suites. Repository-safe MinIO verification used in-memory test doubles; no live MinIO smoke was executed in this environment.
```

- [ ] **Step 3: Run the doc-specific sanity checks**

Run:

```powershell
rg -n "historical preview-artifact migration|migrate-preview-artifacts-to-minio|dual-read|automatic preview-artifact cleanup" README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md
```

Expected: README shows the feature as implemented, the remaining gaps are still explicit, and the spec contains the new validation note.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md
git commit -m "docs: add phase w preview migration rollout notes"
```

## Task 6: Run Final Backend Verification

**Files:**
- Verify: `backend/src/test/java/com/campus/preview/LocalPreviewArtifactPathResolverTests.java`
- Verify: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
- Verify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Verify: `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Verify: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`

- [ ] **Step 1: Run the focused backend regression suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalPreviewArtifactPathResolverTests,HistoricalLocalResourcePreviewArtifactReaderTests,ResourcePreviewServiceTests,AdminResourcePreviewMigrationServiceTests,AdminResourceControllerTests,ResourceControllerTests,MinioResourcePreviewArtifactStorageTests,ResourcePreviewStorageConfigurationTests" test
```

Expected: PASS with the new migration slice covered and the existing preview runtime behavior still stable.

- [ ] **Step 2: Run the optional live smoke only if a real MinIO environment is available**

Suggested smoke flow:

1. Start the backend with access to the historical local preview root and `platform.integrations.minio.enabled=true`.
2. Call `POST /api/admin/resources/migrate-preview-artifacts-to-minio` with `{"dryRun":true,"limit":10}` as an admin.
3. Execute one bounded batch with `{"dryRun":false,"limit":10}`.
4. Verify MinIO contains objects under `preview-artifacts/pptx/...`, `preview-artifacts/docx/...`, or `preview-artifacts/zip/...`.

If no live MinIO runtime is available, explicitly skip this smoke and rely on the repository-safe test doubles.

- [ ] **Step 3: Commit the final verification state**

```bash
git add backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationRequest.java backend/src/main/java/com/campus/dto/AdminResourcePreviewMigrationResponse.java backend/src/main/java/com/campus/service/AdminResourcePreviewMigrationService.java backend/src/main/java/com/campus/controller/admin/AdminResourceController.java backend/src/test/java/com/campus/preview/LocalPreviewArtifactPathResolverTests.java backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md
git commit -m "chore: finalize phase w preview migration verification"
```

## Notes

- Do not route preview-artifact migration through the active `ResourcePreviewArtifactStorage` bean; when the active preview backend remains `local`, the migration still needs a dedicated MinIO target storage.
- Do not add recursive directory scanning for stale fingerprints.
- Do not modify `ResourceService`, `ResourceController`, or frontend files unless a failing regression test proves the migration helper changes leaked into runtime behavior.
- Keep the implementation backend-only in this phase.
