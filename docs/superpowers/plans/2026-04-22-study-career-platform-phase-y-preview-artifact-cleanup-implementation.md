# Study Career Platform Phase Y Preview Artifact Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add exact-key, best-effort preview-artifact cleanup for stale derived resource previews while preserving Phase T MinIO storage, Phase W migration, Phase X runtime fallback, and existing preview HTTP contracts.

**Architecture:** Extend the existing preview-artifact storage boundary with exact-key delete support, add a small historical-local cleaner plus a `PreviewArtifactCleanupService` orchestration layer, and wire that orchestration into rejected-resource resubmission after the row update succeeds. Keep the current preview fingerprint formula for compatibility, but stop unrelated `updatedAt` writes from passive resource actions so they no longer manufacture unnecessary preview keys.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security, MyBatis-Plus, MinIO Java SDK, H2, JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`
- Adjacent completed slices that must remain compatible:
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-design.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`
- Existing preview storage boundary and runtime:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Existing local preview path safety helpers:
  - `backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java`
  - `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
- Existing resource lifecycle services:
  - `backend/src/main/java/com/campus/service/ResourceService.java`
  - `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Existing tests to extend rather than duplicate:
  - `backend/src/test/java/com/campus/preview/LocalResourcePreviewArtifactStorageTests.java`
  - `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
  - `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
  - `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`
  - `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
  - `backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Docs that must be updated when the phase lands:
  - `README.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`

## Scope Lock

This plan covers only the approved Phase Y slice:

- add exact-key delete support to active preview-artifact storage
- add one exact-key historical local cleaner for stale fallback artifacts
- add one orchestration service for stale-key cleanup after resource mutation
- run cleanup only after rejected-resource resubmission invalidates the old preview target
- keep cleanup best-effort and non-blocking
- keep the current preview fingerprint formula unchanged
- stop unrelated `updatedAt` writes from `download`, `favorite`, `unfavorite`, `publish`, `reject`, and `offline`
- preserve Phase X runtime `MinIO first -> local fallback` read behavior

This plan explicitly does not implement:

- directory scans
- MinIO bucket scans
- scheduled cleanup jobs
- preview-read-side cleanup or write-back
- new admin cleanup endpoints
- preview lifecycle database schema changes
- a rewrite of preview fingerprint semantics

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleaner.java`
  - Exact-key local preview artifact deletion under the existing safe resolver.
- Create: `backend/src/main/java/com/campus/service/PreviewArtifactCleanupService.java`
  - Best-effort orchestration for stale active and historical-local cleanup.
- Create: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleanerTests.java`
  - Covers delete behavior and root-escape protection.
- Create: `backend/src/test/java/com/campus/service/PreviewArtifactCleanupServiceTests.java`
  - Covers old/new target comparison, local/minio routing, and swallowed delete failures.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - Add `delete(String artifactKey)`.
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
  - Implement exact-key delete on the local active preview root.
- Modify: `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
  - Implement exact-key delete under the configured preview prefix.
- Modify: `backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java`
  - Delegate delete only to the primary active backend.
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
  - Snapshot old preview target, invoke cleanup after rejected-resource resubmission, and stop interaction-driven `updatedAt` churn.
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
  - Stop status-transition-driven `updatedAt` churn.

### Backend Tests: Modify Existing

- Modify: `backend/src/test/java/com/campus/preview/LocalResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - Add no-op delete support to storage doubles so the suite still compiles after the interface change.
- Modify: `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`
  - Add no-op delete support to the anonymous storage double so the suite still compiles.
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
  - Cover stale preview cleanup after rejected-resource resubmission and interaction timestamp stability.
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
  - Cover admin status transitions without `updatedAt` churn.

### Docs: Modify Existing

- Modify: `README.md`
  - Mark Phase Y implemented and document exact-key automatic cleanup semantics.
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`
  - Add the post-implementation validation note.

## Responsibility Notes

- `ResourcePreviewService` remains the source of truth for preview-artifact keys.
- `ResourcePreviewArtifactStorage.delete(...)` is for active storage only; it must not reach into historical fallback roots.
- `HistoricalLocalResourcePreviewArtifactCleaner` must reuse `LocalPreviewArtifactPathResolver` and remain exact-key-only.
- `PreviewArtifactCleanupService` must be best-effort: log failures, never throw new business errors.
- `ResourceService` should snapshot `oldTarget` before mutating the resource entity and compare it against `newTarget` after persistence.
- `AdminResourceService` and passive interaction flows must stop touching `updatedAt` unless the preview-relevant content really changed.

## Task 1: Add Exact-Key Delete Support To Preview Artifact Storage

**Files:**
- Create: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleaner.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java`
- Create: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleanerTests.java`
- Modify: `backend/src/test/java/com/campus/preview/LocalResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Modify: `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`

- [ ] **Step 1: Write the failing delete-support tests**

Extend `LocalResourcePreviewArtifactStorageTests`:

```java
@Test
void deleteExistingArtifactRemovesStoredFile() throws Exception {
    ResourcePreviewProperties properties = new ResourcePreviewProperties();
    properties.setLocalRoot(tempDir.toString());
    LocalResourcePreviewArtifactStorage storage = new LocalResourcePreviewArtifactStorage(properties);
    Path artifactPath = tempDir.resolve("pptx/9/fingerprint.pdf");
    Files.createDirectories(artifactPath.getParent());
    Files.writeString(artifactPath, "%PDF");

    storage.delete("pptx/9/fingerprint.pdf");

    assertThat(Files.exists(artifactPath)).isFalse();
}

@Test
void deleteMissingArtifactIsIdempotent() throws Exception {
    ResourcePreviewProperties properties = new ResourcePreviewProperties();
    properties.setLocalRoot(tempDir.toString());
    LocalResourcePreviewArtifactStorage storage = new LocalResourcePreviewArtifactStorage(properties);

    storage.delete("pptx/9/missing.pdf");

    assertThat(Files.exists(tempDir.resolve("pptx/9/missing.pdf"))).isFalse();
}
```

Extend `MinioResourcePreviewArtifactStorageTests`:

```java
@Test
void deleteRemovesObjectUnderConfiguredPrefix() throws Exception {
    FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
    MinioResourcePreviewArtifactStorage storage = new MinioResourcePreviewArtifactStorage(
            "campus-platform", "preview-artifacts", operations);
    storage.write("docx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

    storage.delete("docx/9/fingerprint.pdf");

    assertThat(storage.exists("docx/9/fingerprint.pdf")).isFalse();
}
```

Extend `FallbackResourcePreviewArtifactStorageTests`:

```java
@Test
void deleteDelegatesToPrimaryOnly() throws Exception {
    RecordingPrimaryStorage primary = new RecordingPrimaryStorage();
    Path localArtifact = tempDir.resolve("pptx/9/fingerprint.pdf");
    Files.createDirectories(localArtifact.getParent());
    Files.writeString(localArtifact, "%PDF-local");
    FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
            primary,
            new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

    storage.delete("pptx/9/fingerprint.pdf");

    assertThat(primary.deletedKeys()).containsExactly("pptx/9/fingerprint.pdf");
    assertThat(Files.exists(localArtifact)).isTrue();
}
```

Create `HistoricalLocalResourcePreviewArtifactCleanerTests`:

```java
class HistoricalLocalResourcePreviewArtifactCleanerTests {

    @TempDir
    Path tempDir;

    @Test
    void deleteRemovesHistoricalArtifact() throws Exception {
        Path artifactPath = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(artifactPath.getParent());
        Files.writeString(artifactPath, "%PDF");
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setLocalRoot(tempDir.toString());
        HistoricalLocalResourcePreviewArtifactCleaner cleaner =
                new HistoricalLocalResourcePreviewArtifactCleaner(properties);

        cleaner.delete("pptx/9/fingerprint.pdf");

        assertThat(Files.exists(artifactPath)).isFalse();
    }

    @Test
    void deleteRejectsRootEscapeAttempt() {
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setLocalRoot(tempDir.toString());
        HistoricalLocalResourcePreviewArtifactCleaner cleaner =
                new HistoricalLocalResourcePreviewArtifactCleaner(properties);

        assertThatThrownBy(() -> cleaner.delete("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run the targeted storage tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalResourcePreviewArtifactStorageTests,MinioResourcePreviewArtifactStorageTests,FallbackResourcePreviewArtifactStorageTests,HistoricalLocalResourcePreviewArtifactCleanerTests" test
```

Expected: FAIL because the storage interface does not expose delete yet and the historical cleaner does not exist.

- [ ] **Step 3: Implement delete support and compile fixes**

Update `ResourcePreviewArtifactStorage`:

```java
public interface ResourcePreviewArtifactStorage {

    boolean exists(String artifactKey) throws IOException;

    InputStream open(String artifactKey) throws IOException;

    void write(String artifactKey, InputStream inputStream) throws IOException;

    void delete(String artifactKey) throws IOException;
}
```

Update `LocalResourcePreviewArtifactStorage`:

```java
@Override
public void delete(String artifactKey) throws IOException {
    Files.deleteIfExists(pathResolver.resolve(artifactKey));
}
```

Update `MinioResourcePreviewArtifactStorage`:

```java
@Override
public void delete(String artifactKey) throws IOException {
    operations.removeObject(bucketName, objectKeyOf(artifactKey));
}
```

Update `FallbackResourcePreviewArtifactStorage`:

```java
@Override
public void delete(String artifactKey) throws IOException {
    primaryStorage.delete(artifactKey);
}
```

Create `HistoricalLocalResourcePreviewArtifactCleaner`:

```java
@Component
public class HistoricalLocalResourcePreviewArtifactCleaner {

    private final LocalPreviewArtifactPathResolver pathResolver;

    public HistoricalLocalResourcePreviewArtifactCleaner(ResourcePreviewProperties properties) {
        this.pathResolver = new LocalPreviewArtifactPathResolver(Path.of(properties.getLocalRoot()));
    }

    public void delete(String artifactKey) throws IOException {
        Files.deleteIfExists(pathResolver.resolve(artifactKey));
    }
}
```

Also update every in-test `ResourcePreviewArtifactStorage` double to add a no-op or recording `delete(...)`, especially in:

- `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`

- [ ] **Step 4: Re-run the targeted storage tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalResourcePreviewArtifactStorageTests,MinioResourcePreviewArtifactStorageTests,FallbackResourcePreviewArtifactStorageTests,HistoricalLocalResourcePreviewArtifactCleanerTests" test
```

Expected: PASS with exact-key deletion and no historical-root scanning.

- [ ] **Step 5: Commit the storage delete foundation**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleaner.java backend/src/test/java/com/campus/preview/LocalResourcePreviewArtifactStorageTests.java backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleanerTests.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java
git commit -m "refactor: add preview artifact delete support"
```

## Task 2: Add Preview Artifact Cleanup Orchestration

**Files:**
- Create: `backend/src/main/java/com/campus/service/PreviewArtifactCleanupService.java`
- Create: `backend/src/test/java/com/campus/service/PreviewArtifactCleanupServiceTests.java`

- [ ] **Step 1: Write the failing cleanup-orchestration tests**

Create `PreviewArtifactCleanupServiceTests`:

```java
class PreviewArtifactCleanupServiceTests {

    @Test
    void skipsWhenOldTargetIsMissing() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("local", storage, cleaner);

        service.cleanupAfterResourceMutation(Optional.empty(), Optional.empty());

        assertThat(storage.deletedKeys()).isEmpty();
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void skipsWhenOldAndNewTargetsMatch() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("minio", storage, cleaner);
        PreviewArtifactTarget target = new PreviewArtifactTarget("PPTX", "pptx/9/fingerprint.pdf");

        service.cleanupAfterResourceMutation(Optional.of(target), Optional.of(target));

        assertThat(storage.deletedKeys()).isEmpty();
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void minioModeDeletesActiveAndHistoricalCopiesWhenTargetChanges() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("minio", storage, cleaner);

        service.cleanupAfterResourceMutation(
                Optional.of(new PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.of(new PreviewArtifactTarget("PPTX", "pptx/9/new.pdf")));

        assertThat(storage.deletedKeys()).containsExactly("pptx/9/old.pdf");
        assertThat(cleaner.deletedKeys()).containsExactly("pptx/9/old.pdf");
    }

    @Test
    void localModeDeletesOnlyActiveCopyWhenPreviewSupportDisappears() {
        RecordingStorage storage = new RecordingStorage();
        RecordingHistoricalCleaner cleaner = new RecordingHistoricalCleaner();
        PreviewArtifactCleanupService service = service("local", storage, cleaner);

        service.cleanupAfterResourceMutation(
                Optional.of(new PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.empty());

        assertThat(storage.deletedKeys()).containsExactly("pptx/9/old.pdf");
        assertThat(cleaner.deletedKeys()).isEmpty();
    }

    @Test
    void deleteFailuresAreSwallowed() {
        PreviewArtifactCleanupService service = service(
                "minio",
                new BrokenStorage(new IOException("boom")),
                new BrokenHistoricalCleaner(new IOException("boom")));

        assertThatCode(() -> service.cleanupAfterResourceMutation(
                Optional.of(new PreviewArtifactTarget("PPTX", "pptx/9/old.pdf")),
                Optional.empty())).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run the targeted cleanup-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=PreviewArtifactCleanupServiceTests" test
```

Expected: FAIL because `PreviewArtifactCleanupService` does not exist yet.

- [ ] **Step 3: Implement the cleanup service**

Create `PreviewArtifactCleanupService`:

```java
@Service
public class PreviewArtifactCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PreviewArtifactCleanupService.class);

    private final ResourcePreviewArtifactStorage activeStorage;
    private final HistoricalLocalResourcePreviewArtifactCleaner historicalLocalCleaner;
    private final ResourcePreviewProperties previewProperties;

    public PreviewArtifactCleanupService(
            ResourcePreviewArtifactStorage activeStorage,
            HistoricalLocalResourcePreviewArtifactCleaner historicalLocalCleaner,
            ResourcePreviewProperties previewProperties) {
        this.activeStorage = Objects.requireNonNull(activeStorage, "activeStorage");
        this.historicalLocalCleaner = Objects.requireNonNull(historicalLocalCleaner, "historicalLocalCleaner");
        this.previewProperties = Objects.requireNonNull(previewProperties, "previewProperties");
    }

    public void cleanupAfterResourceMutation(
            Optional<ResourcePreviewService.PreviewArtifactTarget> oldTarget,
            Optional<ResourcePreviewService.PreviewArtifactTarget> newTarget) {
        if (oldTarget.isEmpty()) {
            return;
        }
        if (newTarget.isPresent() && oldTarget.get().equals(newTarget.get())) {
            return;
        }
        String artifactKey = oldTarget.get().artifactKey();
        deleteQuietly(() -> activeStorage.delete(artifactKey), "active preview artifact", artifactKey);
        if ("minio".equalsIgnoreCase(previewProperties.getType())) {
            deleteQuietly(() -> historicalLocalCleaner.delete(artifactKey), "historical local preview artifact",
                    artifactKey);
        }
    }

    private void deleteQuietly(IoAction action, String target, String artifactKey) {
        try {
            action.run();
        } catch (IOException | RuntimeException exception) {
            log.warn("Failed to delete {}: {}", target, artifactKey, exception);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
```

- [ ] **Step 4: Re-run the targeted cleanup-service tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=PreviewArtifactCleanupServiceTests" test
```

Expected: PASS with clean local/minio routing and swallowed delete failures.

- [ ] **Step 5: Commit the cleanup orchestration**

```bash
git add backend/src/main/java/com/campus/service/PreviewArtifactCleanupService.java backend/src/test/java/com/campus/service/PreviewArtifactCleanupServiceTests.java
git commit -m "feat: add preview artifact cleanup orchestration"
```

## Task 3: Clean Stale Preview Artifacts After Rejected-Resource Resubmission

**Files:**
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`

- [ ] **Step 1: Write the failing rejected-resource cleanup regressions**

Extend `ResourceControllerTests` with Spring-managed collaborators:

```java
@Autowired
private ResourceItemMapper resourceItemMapper;

@Autowired
private ResourcePreviewService resourcePreviewService;
```

Add a metadata-only resubmission cleanup regression:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void resubmittingRejectedPptxWithoutFileReplacementDeletesOldPreviewArtifact() throws Exception {
    insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
            "career-deck.pptx", "pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "seed/2026/04/career-deck.pptx");
    writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));
    ResourceItem original = resourceItemMapper.selectById(4L);
    String oldArtifactKey = resourcePreviewService.pptxArtifactKeyOf(original);
    Path oldPreviewPath = PREVIEW_ROOT.resolve(oldArtifactKey);
    Files.createDirectories(oldPreviewPath.getParent());
    Files.writeString(oldPreviewPath, "%PDF-old");

    mockMvc.perform(multipart("/api/resources/4")
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    })
                    .param("title", "Revised Career Deck")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Revised summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

    assertThat(Files.exists(oldPreviewPath)).isFalse();
}
```

Add a preview-support-loss cleanup regression:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void replacingRejectedPptxWithPdfDeletesOldDerivedPreviewArtifact() throws Exception {
    insertResource(4L, 2L, "REJECTED", "Please simplify the intro section",
            "career-deck.pptx", "pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "seed/2026/04/career-deck.pptx");
    writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));
    ResourceItem original = resourceItemMapper.selectById(4L);
    String oldArtifactKey = resourcePreviewService.pptxArtifactKeyOf(original);
    Path oldPreviewPath = PREVIEW_ROOT.resolve(oldArtifactKey);
    Files.createDirectories(oldPreviewPath.getParent());
    Files.writeString(oldPreviewPath, "%PDF-old");
    MockMultipartFile file = new MockMultipartFile(
            "file", "revised-pack.pdf", "application/pdf", "new-pdf".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/resources/4")
                    .file(file)
                    .with(request -> {
                        request.setMethod("PUT");
                        return request;
                    })
                    .param("title", "Revised Resume Pack")
                    .param("category", "RESUME_TEMPLATE")
                    .param("summary", "Revised summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fileName").value("revised-pack.pdf"));

    assertThat(Files.exists(oldPreviewPath)).isFalse();
}
```

- [ ] **Step 2: Run the targeted controller regressions and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: FAIL because `updateRejectedResource(...)` does not clean stale preview artifacts yet.

- [ ] **Step 3: Wire cleanup into `ResourceService.updateRejectedResource(...)`**

Inject `PreviewArtifactCleanupService`:

```java
private final PreviewArtifactCleanupService previewArtifactCleanupService;

public ResourceService(..., ResourcePreviewService resourcePreviewService,
        PreviewArtifactCleanupService previewArtifactCleanupService) {
    ...
    this.previewArtifactCleanupService = Objects.requireNonNull(
            previewArtifactCleanupService, "previewArtifactCleanupService");
}
```

Snapshot the old target before mutation and clean after persistence:

```java
@Transactional
public ResourceDetailResponse updateRejectedResource(...) {
    User viewer = userService.requireByIdentity(identity);
    ResourceItem resource = requireEditableRejectedResource(resourceId, viewer);
    Optional<ResourcePreviewService.PreviewArtifactTarget> oldTarget =
            resourcePreviewService.previewArtifactTargetOf(resource);

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
    previewArtifactCleanupService.cleanupAfterResourceMutation(
            oldTarget,
            resourcePreviewService.previewArtifactTargetOf(resource));
    return toResourceDetail(resource, viewer);
}
```

- [ ] **Step 4: Re-run the targeted controller regressions and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests" test
```

Expected: PASS with stale local preview artifacts removed after rejected-resource resubmission.

- [ ] **Step 5: Commit the rejected-resource cleanup wiring**

```bash
git add backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java
git commit -m "feat: clean stale preview artifacts after resource resubmission"
```

## Task 4: Stop Passive `updatedAt` Writes From Creating Preview Churn

**Files:**
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Modify: `backend/src/main/java/com/campus/service/AdminResourceService.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [ ] **Step 1: Write the failing timestamp-stability regressions**

Extend `ResourceControllerTests`:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void favoriteAndUnfavoriteDoNotChangeResourceUpdatedAt() throws Exception {
    LocalDateTime before = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 1", LocalDateTime.class);

    mockMvc.perform(post("/api/resources/1/favorite"))
            .andExpect(status().isOk());
    mockMvc.perform(delete("/api/resources/1/favorite"))
            .andExpect(status().isOk());

    LocalDateTime after = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 1", LocalDateTime.class);
    assertThat(after).isEqualTo(before);
}

@Test
@WithMockUser(username = "2", roles = "USER")
void downloadDoesNotChangeResourceUpdatedAt() throws Exception {
    writeStoredFile("seed/2026/04/resume-template-pack.pdf", "resource-file");
    LocalDateTime before = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 1", LocalDateTime.class);

    mockMvc.perform(get("/api/resources/1/download"))
            .andExpect(status().isOk());

    LocalDateTime after = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 1", LocalDateTime.class);
    assertThat(after).isEqualTo(before);
}
```

Extend `AdminResourceControllerTests`:

```java
@Test
@WithMockUser(username = "1", roles = "ADMIN")
void publishRejectAndOfflineDoNotChangeUpdatedAt() throws Exception {
    jdbcTemplate.update(
            """
                    INSERT INTO t_resource_item (
                      id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                      file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                      published_at, reviewed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?)
                    """,
            4L, "Mock Exam Paper", "EXAM_PAPER", "Pending exam paper", "Pending exam paper details",
            "PENDING", 2L, "mock-exam-paper.pdf", "pdf", "application/pdf", 2048L,
            "seed/2026/04/mock-exam-paper.pdf", 0, 0,
            LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));
    LocalDateTime publishBefore = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 4", LocalDateTime.class);

    mockMvc.perform(post("/api/admin/resources/4/publish"))
            .andExpect(status().isOk());

    LocalDateTime publishAfter = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM t_resource_item WHERE id = 4", LocalDateTime.class);
    assertThat(publishAfter).isEqualTo(publishBefore);
}
```

Use the same pattern in the same test for seeded resource `3` on reject and seeded resource `1` on offline so all three admin transitions prove `updated_at` stability.

- [ ] **Step 2: Run the targeted controller regressions and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: FAIL because these flows currently call `setUpdatedAt(...)`.

- [ ] **Step 3: Remove passive `updatedAt` churn**

Update `ResourceService.favoriteResource(...)` and `unfavoriteResource(...)` by removing:

```java
resource.setUpdatedAt(LocalDateTime.now());
```

Update `ResourceService.downloadResource(...)` by removing:

```java
resource.setUpdatedAt(LocalDateTime.now());
```

Update `AdminResourceService.publishResource(...)`, `rejectResource(...)`, and `offlineResource(...)` by removing:

```java
resource.setUpdatedAt(now);
resource.setUpdatedAt(LocalDateTime.now());
```

Do not remove legitimate `reviewedAt` or `publishedAt` writes. Only stop the unrelated `updatedAt` churn.

- [ ] **Step 4: Re-run the targeted controller regressions and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS with unchanged API behavior, but stable `updated_at` values for passive operations.

- [ ] **Step 5: Commit the timestamp-stability refactor**

```bash
git add backend/src/main/java/com/campus/service/ResourceService.java backend/src/main/java/com/campus/service/AdminResourceService.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "refactor: stop passive resource preview churn"
```

## Task 5: Update Docs And Record Validation

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md`

- [ ] **Step 1: Update the README**

Apply the following documentation changes:

- add `Phase Y preview artifact cleanup first slice` to the repository status line
- remove `automatic preview-artifact cleanup` from the `Explicitly not implemented yet` list
- keep scheduled or scanning-style garbage collection out of scope
- in the preview-artifact storage section, document:
  - stale derived preview artifacts are cleaned up automatically and best-effort after rejected-resource resubmission when the old logical key can be derived exactly
  - `download`, `favorite`, `unfavorite`, `publish`, `reject`, and `offline` no longer rotate preview keys through unrelated `updatedAt` writes
  - no recursive preview-root scan or scheduled cleanup job exists in this phase

Suggested README wording:

```markdown
- stale derived preview artifacts are cleaned up best-effort after rejected-resource resubmission when the old logical preview key is known exactly
- passive resource interactions and admin status transitions no longer rotate preview artifact keys through unrelated `updatedAt` writes
- this phase still does not introduce recursive preview-root scanning or scheduled preview garbage collection
```

- [ ] **Step 2: Add the Phase Y validation note to the spec**

Add a validation note near the top of the Phase Y spec:

```markdown
> **Validation note:** This design was implemented and validated on 2026-04-22 using the approved execution record at `docs/superpowers/plans/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-implementation.md`. Local verification suites now present for this slice are `LocalResourcePreviewArtifactStorageTests`, `MinioResourcePreviewArtifactStorageTests`, `FallbackResourcePreviewArtifactStorageTests`, `HistoricalLocalResourcePreviewArtifactCleanerTests`, `PreviewArtifactCleanupServiceTests`, `ResourcePreviewServiceTests`, `AdminResourcePreviewMigrationServiceTests`, `ResourceControllerPreviewFallbackTests`, `ResourceControllerTests`, `AdminResourceControllerTests`, and `ResourcePreviewStorageConfigurationTests`.
```

- [ ] **Step 3: Run the targeted verification suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=LocalResourcePreviewArtifactStorageTests,MinioResourcePreviewArtifactStorageTests,FallbackResourcePreviewArtifactStorageTests,HistoricalLocalResourcePreviewArtifactCleanerTests,PreviewArtifactCleanupServiceTests,ResourcePreviewServiceTests,AdminResourcePreviewMigrationServiceTests,ResourcePreviewStorageConfigurationTests,ResourceControllerPreviewFallbackTests,ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS with:

- delete support green across local, MinIO, and fallback storage
- cleanup orchestration green in both local and minio modes
- rejected-resource resubmission cleanup green
- Phase X preview fallback regressions still green
- passive-operation timestamp stability green

- [ ] **Step 4: Commit the rollout notes**

```bash
git add README.md docs/superpowers/specs/2026-04-22-study-career-platform-phase-y-preview-artifact-cleanup-design.md
git commit -m "docs: add phase y preview cleanup rollout notes"
```

## Final Verification Checklist

- [ ] `backend/src/test/java/com/campus/preview/LocalResourcePreviewArtifactStorageTests.java`
- [ ] `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- [ ] `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
- [ ] `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactCleanerTests.java`
- [ ] `backend/src/test/java/com/campus/service/PreviewArtifactCleanupServiceTests.java`
- [ ] `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- [ ] `backend/src/test/java/com/campus/service/AdminResourcePreviewMigrationServiceTests.java`
- [ ] `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- [ ] `backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java`
- [ ] `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- [ ] `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

## Execution Notes

- Keep Phase Y backend-only plus docs; do not add new frontend routes or controls.
- Use `apply_patch` for manual edits.
- Because the storage interface changes, update all test doubles immediately after adding `delete(...)` to avoid misleading compile noise.
- Do not rewrite the preview fingerprint formula in this phase, even if it looks tempting while touching cleanup logic.
- Keep cleanup exact-key-only and best-effort; if you find yourself writing directory scans, stop and realign with the spec.
