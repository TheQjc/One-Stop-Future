# Phase T MinIO Preview Artifact Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add opt-in MinIO-backed preview artifact storage for `PPTX`, `DOCX`, and `ZIP` resource previews without changing preview HTTP contracts, frontend behavior, or local-profile defaults.

**Architecture:** Keep `ResourcePreviewService` as the single owner of preview artifact keys and cache semantics, but make the underlying `ResourcePreviewArtifactStorage` boundary safe for remote I/O and selectable by configuration. Reuse the existing shared MinIO integration layer from Phase I, add a dedicated `MinioResourcePreviewArtifactStorage` implementation plus preview-storage configuration, and keep local preview storage as the default closed loop for `application-local.yml`.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MinIO Java SDK, JUnit 5, AssertJ, Spring Boot `ApplicationContextRunner`, Vue 3, Vite, Vitest

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`
- Existing preview storage boundary:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Existing preview service and preview generators:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  - `backend/src/main/java/com/campus/preview/SofficeDocxPreviewGenerator.java`
- Existing preview configuration:
  - `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-local.yml`
- Existing shared MinIO integration:
  - `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
  - `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
  - `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
  - `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
  - `backend/src/main/java/com/campus/storage/MinioResourceFileStorage.java`
- Existing test patterns to mirror:
  - `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - `backend/src/test/java/com/campus/storage/MinioResourceFileStorageTests.java`
  - `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
- Existing resource-preview HTTP regression suites:
  - `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
  - `frontend/src/views/ResourceDetailView.spec.js`
  - `frontend/src/views/ProfileResourcesView.spec.js`
  - `frontend/src/views/admin/AdminResourceManageView.spec.js`
- Existing docs that must be updated when the phase lands:
  - `README.md`

## Scope Lock

This plan covers only the approved Phase T slice:

- make preview artifact existence checks safe for remote I/O
- add a `MinioResourcePreviewArtifactStorage` implementation
- add a dedicated preview-storage selector via `app.resource-preview.type`
- reuse the existing shared MinIO bucket with a dedicated preview prefix
- keep `application-local.yml` on local preview storage by default
- document the new runtime switches and remove preview-artifact MinIO storage from the README “not implemented yet” list

This plan explicitly does not implement:

- historical preview artifact migration
- local + MinIO dual-read fallback
- preview artifact garbage collection
- a new preview API
- frontend preview behavior changes
- Docker image changes to install LibreOffice
- compose-time guarantees for `DOCX` preview beyond the existing `soffice` prerequisite

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
  Select exactly one `ResourcePreviewArtifactStorage` bean and fail fast when MinIO preview storage is explicitly selected without MinIO integration.
- Create: `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
  Store preview artifacts in the existing MinIO bucket under a dedicated prefix.
- Create: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
  Configuration-safety coverage for preview storage selection.
- Create: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
  Focused unit tests for MinIO preview artifact read/write/exists behavior.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  Allow `exists(...)` to surface I/O failures so remote storage backends can preserve controlled error semantics.
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
  Remove unconditional component registration, keep it as a configuration-created bean, and align with the updated storage interface.
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  Catch preview-artifact lookup failures and map them back to the existing unavailable messages.
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
  Add preview storage type and MinIO prefix binding.
- Modify: `backend/src/main/resources/application.yml`
  Add `RESOURCE_PREVIEW_TYPE` and `RESOURCE_PREVIEW_MINIO_PREFIX` defaults.
- Modify: `backend/src/main/resources/application-local.yml`
  Keep preview storage explicitly `local`.
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  Cover controlled error mapping when preview artifact existence checks fail.

### Docs: Modify Existing

- Modify: `README.md`
  Document Phase T capability, config switches, and rollout limitations.
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`
  Add a post-implementation validation note once the phase is complete.

### Verify Existing Files Without Planned Logic Changes

- Verify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Verify: `frontend/src/views/ResourceDetailView.spec.js`
- Verify: `frontend/src/views/ProfileResourcesView.spec.js`
- Verify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

## Responsibility Notes

- `ResourcePreviewArtifactStorage` must remain the only abstraction that `ResourcePreviewService` depends on for preview artifact reads/writes.
- `ResourcePreviewService` must keep owning artifact keys and cache invalidation; MinIO support must not change key shape.
- `ResourcePreviewStorageConfiguration` owns only bean selection and fail-fast configuration rules.
- `MinioResourcePreviewArtifactStorage` owns only bucket readiness plus object-key mapping under the configured prefix.
- `README.md` should describe the feature as opt-in for non-local runtimes rather than implying the optional Compose stack now automatically supplies all `DOCX` preview dependencies.

## Task 1: Make Preview Artifact Existence Checks Remote-Safe

**Files:**
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [x] **Step 1: Write the failing service tests for remote `exists(...)` failures**

Extend `ResourcePreviewServiceTests` with storage doubles that fail on artifact existence checks:

```java
@Test
void docxPreviewExistsFailureBecomesBusinessException() {
    ResourcePreviewService service = new ResourcePreviewService(
            new ThrowingExistsStorage(new IOException("boom")),
            new ObjectMapper(),
            new NoopPptxPreviewGenerator(),
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));

    assertThatThrownBy(() -> service.previewDocx(
            resource(9L, "writing-workbook.docx", "docx", "seed/workbook.docx", 1024L, LocalDateTime.now()),
            this::sampleDocxStream))
            .isInstanceOf(BusinessException.class)
            .hasMessage("docx preview unavailable");
}

@Test
void zipPreviewExistsFailureBecomesBusinessException() {
    ResourcePreviewService service = new ResourcePreviewService(
            new ThrowingExistsStorage(new IOException("boom")),
            new ObjectMapper(),
            new NoopPptxPreviewGenerator(),
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));

    assertThatThrownBy(() -> service.previewZip(
            resource(9L, "resume.zip", "zip", "seed/resume.zip", 1024L, LocalDateTime.now()),
            this::sampleZipStream))
            .isInstanceOf(BusinessException.class)
            .hasMessage("zip preview unavailable");
}
```

Add a helper storage double:

```java
private static class ThrowingExistsStorage implements ResourcePreviewArtifactStorage {

    private final IOException exception;

    private ThrowingExistsStorage(IOException exception) {
        this.exception = exception;
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        throw exception;
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        throw new IOException("not implemented");
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        throw new IOException("not implemented");
    }
}
```

- [x] **Step 2: Run the targeted preview-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: FAIL because `ResourcePreviewArtifactStorage.exists(...)` cannot currently throw `IOException`, and `ResourcePreviewService` does not map lookup failures to controlled business errors.

- [x] **Step 3: Implement the I/O-capable storage contract and controlled failure mapping**

Update the interface:

```java
public interface ResourcePreviewArtifactStorage {

    boolean exists(String artifactKey) throws IOException;

    InputStream open(String artifactKey) throws IOException;

    void write(String artifactKey, InputStream inputStream) throws IOException;
}
```

Refactor `ResourcePreviewService` so cache-lookups go through a helper that preserves the existing unavailable messages:

```java
private boolean artifactExists(String artifactKey, String unavailableMessage) {
    try {
        return artifactStorage.exists(artifactKey);
    } catch (IOException exception) {
        throw new BusinessException(500, unavailableMessage);
    }
}
```

Use that helper in both the generated-PDF path and the ZIP path before `open(...)` is attempted.

Update all test doubles in `ResourcePreviewServiceTests` and `LocalResourcePreviewArtifactStorage` to match the new signature.

- [x] **Step 4: Re-run the preview-service tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: PASS with both the old cache-reuse assertions and the new failure-mapping coverage.

- [x] **Step 5: Commit the remote-safe preview artifact contract**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "refactor: support io-aware preview artifact checks"
```

## Task 2: Add A MinIO Preview Artifact Storage Implementation

**Files:**
- Create: `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
- Create: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`

- [x] **Step 1: Write the failing MinIO preview storage tests**

Mirror the storage-level test style from `MinioResourceFileStorageTests`, but target the preview-artifact contract:

```java
@Test
void constructorCreatesBucketWhenMissing() throws Exception {
    FakeMinioObjectOperations operations = new FakeMinioObjectOperations();

    new MinioResourcePreviewArtifactStorage("campus-platform", "preview-artifacts", operations);

    assertThat(operations.createdBuckets).containsExactly("campus-platform");
}

@Test
void writeOpenAndExistsRoundTripUnderConfiguredPrefix() throws Exception {
    FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
    MinioResourcePreviewArtifactStorage storage =
            new MinioResourcePreviewArtifactStorage("campus-platform", "preview-artifacts", operations);

    storage.write("docx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

    assertThat(storage.exists("docx/9/fingerprint.pdf")).isTrue();
    assertThat(new String(storage.open("docx/9/fingerprint.pdf").readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("%PDF");
    assertThat(operations.objects).containsKey("campus-platform:preview-artifacts/docx/9/fingerprint.pdf");
}

@Test
void openAndExistsNormalizeBackslashSeparatedArtifactKeys() throws Exception {
    FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
    MinioResourcePreviewArtifactStorage storage =
            new MinioResourcePreviewArtifactStorage("campus-platform", "preview-artifacts/", operations);

    operations.putObject("campus-platform", "preview-artifacts/pptx/11/cached.pdf",
            new ByteArrayInputStream("cached".getBytes(StandardCharsets.UTF_8)));

    assertThat(storage.exists("pptx\\11\\cached.pdf")).isTrue();
    assertThat(new String(storage.open("pptx\\11\\cached.pdf").readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("cached");
}
```

Also add one invalid-config guard:

```java
@Test
void constructorRejectsBlankPrefix() {
    FakeMinioObjectOperations operations = new FakeMinioObjectOperations();

    assertThatThrownBy(() -> new MinioResourcePreviewArtifactStorage("campus-platform", "   ", operations))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("preview minio prefix is blank");
}
```

- [x] **Step 2: Run the targeted MinIO preview storage tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=MinioResourcePreviewArtifactStorageTests" test
```

Expected: FAIL because the MinIO preview artifact storage class does not exist yet.

- [x] **Step 3: Implement the minimal MinIO preview artifact storage**

Create `MinioResourcePreviewArtifactStorage` in the `preview` package:

```java
public class MinioResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final String bucketName;
    private final String minioPrefix;
    private final MinioObjectOperations operations;

    public MinioResourcePreviewArtifactStorage(String bucketName, String minioPrefix, MinioObjectOperations operations)
            throws IOException {
        this.bucketName = Objects.requireNonNull(bucketName, "bucketName");
        this.minioPrefix = normalizePrefix(minioPrefix);
        this.operations = Objects.requireNonNull(operations, "operations");
        ensureBucket();
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        return operations.objectExists(bucketName, objectKeyOf(artifactKey));
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        return operations.getObject(bucketName, objectKeyOf(artifactKey));
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        operations.putObject(bucketName, objectKeyOf(artifactKey), inputStream);
    }
}
```

Implementation rules:

- reuse the existing shared bucket from `platform.integrations.minio.bucket`
- map logical artifact keys under the configured prefix
- normalize backslashes to forward slashes
- trim leading and trailing slashes from the configured prefix
- reject a blank prefix
- keep bucket creation behavior aligned with `MinioResourceFileStorage`
- do not add delete support or historical-migration logic in this phase

- [x] **Step 4: Re-run the MinIO preview storage tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=MinioResourcePreviewArtifactStorageTests" test
```

Expected: PASS with prefix mapping, bucket readiness, and key normalization covered.

- [x] **Step 5: Commit the MinIO preview storage implementation**

```bash
git add backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java
git commit -m "feat: add minio preview artifact storage"
```

## Task 3: Add Preview Storage Selection And Runtime Configuration

**Files:**
- Create: `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
- Create: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Modify: `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`

- [x] **Step 1: Write the failing configuration tests**

Create `ResourcePreviewStorageConfigurationTests` with `ApplicationContextRunner` and a fake `MinioObjectOperations` bean:

```java
class ResourcePreviewStorageConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ResourcePreviewProperties.class,
                    MinioIntegrationProperties.class,
                    ResourcePreviewStorageConfiguration.class);

    @Test
    void localPreviewTypeCreatesLocalPreviewArtifactStorage() {
        contextRunner
                .withPropertyValues("app.resource-preview.type=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                    assertThat(context).hasSingleBean(LocalResourcePreviewArtifactStorage.class);
                    assertThat(context).doesNotHaveBean(MinioResourcePreviewArtifactStorage.class);
                });
    }

    @Test
    void minioPreviewTypeCreatesMinioPreviewArtifactStorageWhenMinioIsEnabled() {
        contextRunner
                .withBean(MinioObjectOperations.class, FakeMinioObjectOperations::new)
                .withPropertyValues(
                        "app.resource-preview.type=minio",
                        "app.resource-preview.minio-prefix=preview-artifacts",
                        "platform.integrations.minio.enabled=true",
                        "platform.integrations.minio.bucket=campus-platform")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                    assertThat(context).hasSingleBean(MinioResourcePreviewArtifactStorage.class);
                    assertThat(context).doesNotHaveBean(LocalResourcePreviewArtifactStorage.class);
                });
    }

    @Test
    void minioPreviewTypeFailsFastWhenMinioIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-preview.type=minio",
                        "platform.integrations.minio.enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("minio preview storage is selected but disabled");
                });
    }
}
```

- [x] **Step 2: Run the preview-storage configuration tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewStorageConfigurationTests" test
```

Expected: FAIL because the preview storage configuration class does not exist yet, `LocalResourcePreviewArtifactStorage` is still an unconditional component, and `ResourcePreviewProperties` does not bind preview storage type or MinIO prefix.

- [x] **Step 3: Implement preview storage selection and config defaults**

Extend `ResourcePreviewProperties`:

```java
@Data
@Component
@ConfigurationProperties(prefix = "app.resource-preview")
public class ResourcePreviewProperties {

    private String type = "local";
    private String localRoot = ".local-storage/previews";
    private String minioPrefix = "preview-artifacts";
    private Docx docx = new Docx();
}
```

Create `ResourcePreviewStorageConfiguration`:

```java
@Configuration
public class ResourcePreviewStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "local", matchIfMissing = true)
    ResourcePreviewArtifactStorage localResourcePreviewArtifactStorage(ResourcePreviewProperties properties) {
        return new LocalResourcePreviewArtifactStorage(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "minio")
    Object minioPreviewStorageSelectionGuard(MinioIntegrationProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("minio preview storage is selected but disabled");
        }
        return new Object();
    }

    @Bean
    @DependsOn("minioPreviewStorageSelectionGuard")
    @ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "minio")
    ResourcePreviewArtifactStorage minioResourcePreviewArtifactStorage(
            ResourcePreviewProperties previewProperties,
            MinioIntegrationProperties minioProperties,
            MinioObjectOperations operations) throws IOException {
        return new MinioResourcePreviewArtifactStorage(
                minioProperties.getBucket(),
                previewProperties.getMinioPrefix(),
                operations);
    }
}
```

Refactor `LocalResourcePreviewArtifactStorage` so it is no longer annotated with `@Component`; it should now be created only through the configuration above.

Update config files:

```yaml
app:
  resource-preview:
    type: ${RESOURCE_PREVIEW_TYPE:local}
    local-root: ${RESOURCE_PREVIEW_LOCAL_ROOT:.local-storage/previews}
    minio-prefix: ${RESOURCE_PREVIEW_MINIO_PREFIX:preview-artifacts}
    docx:
      soffice-command: ${RESOURCE_PREVIEW_DOCX_SOFFICE_COMMAND:soffice}
```

Keep `application-local.yml` explicit:

```yaml
app:
  resource-preview:
    type: local
    local-root: .local-storage/previews
    minio-prefix: preview-artifacts
```

- [x] **Step 4: Re-run the preview-storage configuration tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewStorageConfigurationTests" test
```

Expected: PASS with local default selection, MinIO selection, and fail-fast behavior covered.

- [x] **Step 5: Commit the preview storage selection layer**

```bash
git add backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java backend/src/main/java/com/campus/config/ResourcePreviewProperties.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml
git commit -m "feat: add preview artifact storage selection"
```

## Task 4: Update README And Validation Notes

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md`

- [x] **Step 1: Confirm the current docs still describe preview artifacts as local-only**

Run:

```powershell
rg -n "MinIO-backed preview artifact storage|preview artifacts remain local|RESOURCE_PREVIEW_TYPE|RESOURCE_PREVIEW_MINIO_PREFIX" README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md
```

Expected:

- README still lists MinIO-backed preview artifact storage as not implemented
- README does not yet document `RESOURCE_PREVIEW_TYPE` or `RESOURCE_PREVIEW_MINIO_PREFIX`
- the spec file does not yet carry a post-implementation validation note

- [x] **Step 2: Update README for the new Phase T behavior**

Make all of these documentation updates:

- add Phase T to the current repo status line
- move MinIO-backed preview artifact storage from “not implemented yet” into the implemented feature list
- replace statements that preview artifacts always remain local with conditional wording:
  - local profile stays local
  - non-local runtimes can opt into MinIO preview artifact storage with `RESOURCE_PREVIEW_TYPE=minio`
- document `RESOURCE_PREVIEW_MINIO_PREFIX`
- keep the no-migration / no-GC limitations explicit
- keep the existing `DOCX` preview `soffice` runtime note explicit
- avoid claiming that the optional Compose stack now automatically provides LibreOffice

Recommended README additions:

```md
- MinIO-backed preview artifact storage for opt-in non-`local` runtimes while local development keeps the preview cache on disk
```

```md
- `RESOURCE_PREVIEW_TYPE=minio` switches newly generated preview artifacts to the shared MinIO bucket
- `RESOURCE_PREVIEW_MINIO_PREFIX` controls the preview artifact namespace inside that bucket
- historical local preview artifacts are not migrated and are not dual-read after the switch
```

- [x] **Step 3: Add a validation note to the Phase T spec**

Prepend a validation note, following the Phase S pattern:

```md
> **Validation note:** This design was implemented and validated on 2026-04-21. Execution record: `docs/superpowers/plans/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-implementation.md`.
```

If live MinIO smoke is not executed in the implementation environment, phrase the note to mention repository-safe verification plus any skipped live smoke explicitly.

- [x] **Step 4: Re-run the doc checks**

Run:

```powershell
rg -n "Phase T|RESOURCE_PREVIEW_TYPE|RESOURCE_PREVIEW_MINIO_PREFIX|historical local preview artifacts are not migrated" README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md
```

Expected: PASS with the new runtime flags and scope limitations visible in the docs.

- [x] **Step 5: Commit the docs update**

```bash
git add README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md
git commit -m "docs: add minio preview artifact rollout notes"
```

## Task 5: Run Cross-Surface Verification

**Files:**
- Verify: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- Verify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Verify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- Verify: `backend/src/test/java/com/campus/preview/SofficeDocxPreviewGeneratorTests.java`
- Verify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Verify: `frontend/src/views/ResourceDetailView.spec.js`
- Verify: `frontend/src/views/ProfileResourcesView.spec.js`
- Verify: `frontend/src/views/admin/AdminResourceManageView.spec.js`

- [x] **Step 1: Run the targeted backend verification suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewStorageConfigurationTests,MinioResourcePreviewArtifactStorageTests,ResourcePreviewServiceTests,SofficeDocxPreviewGeneratorTests,ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS.

- [x] **Step 2: Run the targeted frontend verification suite and production build**

Run:

```powershell
cd frontend
npx vitest run src/views/ResourceDetailView.spec.js src/views/ProfileResourcesView.spec.js src/views/admin/AdminResourceManageView.spec.js
npm run build
```

Expected: PASS with no frontend code changes required for the new backend storage option.

- [x] **Step 3: Review the final diff for scope discipline**

Run:

```powershell
git diff --stat
git status --short
```

Confirm:

- only Phase T preview-artifact storage files changed
- no raw-resource storage migration logic was mixed into this slice
- no preview API contract changed
- no frontend runtime logic changes slipped in unless a red regression test forced a minimal fix
- no Dockerfile or Compose LibreOffice work was accidentally pulled into this phase

- [x] **Step 4: Run optional live MinIO smoke if the environment supports it**

If a reachable MinIO instance is available and the backend can run with the required preview dependencies:

1. Start the backend with `MINIO_ENABLED=true` and `RESOURCE_PREVIEW_TYPE=minio`.
2. Preview one published `PPTX` resource twice.
3. Preview one published `ZIP` resource twice.
4. If `soffice` is available in that runtime, preview one published `DOCX` resource twice.
5. Verify MinIO contains objects under `preview-artifacts/pptx/...`, `preview-artifacts/zip/...`, and optionally `preview-artifacts/docx/...`.
6. Confirm the second request reuses the cached artifact instead of changing the object key.

If live MinIO or `soffice` is unavailable, record that repository-safe tests passed and live smoke was skipped.

- [x] **Step 5: Commit final polish only if verification required code changes**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/main/java/com/campus/config/ResourcePreviewProperties.java backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml README.md docs/superpowers/specs/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-design.md
git commit -m "chore: finalize phase t preview storage verification"
```

## Execution Notes

- Do not add historical preview artifact migration in this phase.
- Do not add dual-read fallback from MinIO preview storage back to local disk.
- Do not change `ResourcePreviewService` artifact key shapes.
- Do not add a new preview endpoint or a preview database table.
- Do not change frontend preview behavior unless a regression test proves a minimal fix is required.
- Do not expand this phase into Docker image changes for `soffice`; keep containerized `DOCX` preview dependency handling explicitly documented instead.
