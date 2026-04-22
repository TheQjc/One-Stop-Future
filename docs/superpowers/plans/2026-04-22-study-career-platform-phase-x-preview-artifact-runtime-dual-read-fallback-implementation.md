# Phase X Preview Artifact Runtime Dual-Read Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add opt-in runtime `MinIO first -> historical local fallback` preview-artifact reads for `PPTX`, `DOCX`, and `ZIP` resources without changing preview HTTP contracts, local-profile defaults, or turning preview requests into migration side effects.

**Architecture:** Keep `ResourcePreviewArtifactStorage` as the only storage boundary that `ResourcePreviewService` depends on, but add a composed fallback implementation that wraps the existing MinIO preview storage with a read-only historical local reader. Tighten cache-miss semantics by teaching MinIO and local historical reads to surface explicit `FileNotFoundException`, then refactor `ResourcePreviewService` to use an open-first cache-read flow so dual-read fallback works without duplicate `exists(...)` round-trips.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MinIO Java SDK 8.6.0, MyBatis-Plus, H2, JUnit 5, AssertJ, Mockito, Spring Boot Test, ApplicationContextRunner

---

## Context Before Starting

- Spec baseline:
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`
- Adjacent completed preview-storage slices:
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-t-minio-preview-artifact-storage-implementation.md`
  - `docs/superpowers/plans/2026-04-21-study-career-platform-phase-w-historical-preview-artifact-minio-migration-implementation.md`
- Existing preview storage boundary and service:
  - `backend/src/main/java/com/campus/preview/ResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  - `backend/src/main/java/com/campus/preview/MinioResourcePreviewArtifactStorage.java`
  - `backend/src/main/java/com/campus/preview/LocalResourcePreviewArtifactStorage.java`
- Existing historical local preview utilities:
  - `backend/src/main/java/com/campus/preview/LocalPreviewArtifactPathResolver.java`
  - `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
- Existing preview configuration:
  - `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
  - `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-local.yml`
- Existing MinIO integration:
  - `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
  - `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
- Existing test suites to mirror:
  - `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  - `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
  - `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
  - `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
  - `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Existing docs that must be updated when the phase lands:
  - `README.md`
  - `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`

## Scope Lock

This plan covers only the approved Phase X slice:

- add one opt-in runtime flag: `app.resource-preview.read-fallback-local-enabled`
- allow local historical fallback only when `app.resource-preview.type=minio`
- keep read order `MinIO first -> historical local fallback second`
- keep runtime fallback read-only with no automatic write-back to MinIO
- preserve current preview-artifact key derivation and fingerprint semantics
- preserve local profile defaults and local-only preview behavior
- preserve existing preview endpoint URLs and payload formats
- preserve MinIO as the authoritative write target when preview storage type is `minio`
- preserve infrastructure-failure semantics so MinIO outages are not masked by local fallback

This plan explicitly does not implement:

- reverse lookup from local mode into MinIO
- automatic migration during preview requests
- automatic preview-artifact cleanup or garbage collection
- a second historical preview root
- preview lifecycle database fields
- new preview APIs or frontend behavior changes
- object-storage high availability beyond current MinIO integration

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java`
  Compose `MinioResourcePreviewArtifactStorage` with `HistoricalLocalResourcePreviewArtifactReader` behind the existing storage interface.
- Create: `backend/src/test/java/com/campus/storage/SdkMinioObjectOperationsTests.java`
  Lock down `NoSuchKey` / `NoSuchObject` to `FileNotFoundException` mapping.
- Create: `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
  Cover MinIO hit, local fallback hit, dual miss, failure propagation, and write delegation.
- Create: `backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java`
  Cover real HTTP preview behavior in MinIO mode with fallback enabled.

### Backend: Modify Existing

- Modify: `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
  Surface missing MinIO objects as `FileNotFoundException` instead of generic `IOException`.
- Modify: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
  Surface missing local historical artifacts as `FileNotFoundException` for open-first fallback semantics.
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
  Replace `exists(...)`-then-`open(...)` cache reads with an open-first flow that distinguishes cache misses from infrastructure failures.
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
  Add `readFallbackLocalEnabled`.
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
  Select direct MinIO storage or the new fallback storage based on the new property.
- Modify: `backend/src/main/resources/application.yml`
  Add `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED` default.
- Modify: `backend/src/main/resources/application-local.yml`
  Pin the local profile to `read-fallback-local-enabled: false` for clarity.
- Modify: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
  Cover missing-artifact open behavior.
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
  Cover open-first cache hits, cache misses, and non-miss storage failures.
- Modify: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
  Cover fallback-enabled bean selection and local-mode ignore behavior.

### Docs: Modify Existing

- Modify: `README.md`
  Record Phase X as implemented, document the new fallback flag and runtime behavior, and remove runtime dual-read fallback from the “not implemented yet” list.
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`
  Add the post-implementation validation note once rollout is complete.

### Verify Existing Files Without Planned Logic Changes

- Verify: `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`
- Verify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Verify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

## Responsibility Notes

- `SdkMinioObjectOperations` must distinguish “object missing” from “MinIO failed” only for object reads. Do not broaden this into bucket-level HA behavior.
- `HistoricalLocalResourcePreviewArtifactReader` must remain a read-only utility and must not take on write or cleanup behavior.
- `FallbackResourcePreviewArtifactStorage` must own only runtime fallback order:
  - MinIO hit -> return MinIO artifact
  - MinIO object missing -> try local historical artifact
  - MinIO failure -> propagate failure
  - local historical miss -> surface cache miss
  - write -> delegate to MinIO only
- `ResourcePreviewService` must remain the source of truth for preview-artifact keys, generation, and user-facing preview-unavailable messages.
- `ResourcePreviewStorageConfiguration` must remain the only place that decides which preview storage implementation is active.
- `README.md` must describe fallback as opt-in and read-only, not as automatic migration.

## Task 1: Surface Explicit Cache Misses From MinIO And Historical Local Reads

**Files:**
- Create: `backend/src/test/java/com/campus/storage/SdkMinioObjectOperationsTests.java`
- Modify: `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
- Modify: `backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java`
- Modify: `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`

- [x] **Step 1: Write the failing MinIO and local-reader tests**

Create `SdkMinioObjectOperationsTests`:

```java
@ExtendWith(MockitoExtension.class)
class SdkMinioObjectOperationsTests {

    @Mock
    private MinioClient client;

    @Test
    void getObjectMapsNoSuchObjectToFileNotFoundException() throws Exception {
        when(client.getObject(any(GetObjectArgs.class)))
                .thenThrow(noSuchObject("preview-artifacts/pptx/9/fingerprint.pdf"));

        SdkMinioObjectOperations operations = new SdkMinioObjectOperations(client);

        assertThatThrownBy(() -> operations.getObject(
                "campus-platform",
                "preview-artifacts/pptx/9/fingerprint.pdf"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void getObjectKeepsNonMissingMinioFailuresAsIoException() throws Exception {
        when(client.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IOException("boom"));

        SdkMinioObjectOperations operations = new SdkMinioObjectOperations(client);

        assertThatThrownBy(() -> operations.getObject(
                "campus-platform",
                "preview-artifacts/pptx/9/fingerprint.pdf"))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("failed to get minio object");
    }

    private ErrorResponseException noSuchObject(String objectKey) {
        ErrorResponse error = new ErrorResponse(
                "NoSuchObject",
                "Object does not exist",
                "campus-platform",
                objectKey,
                "/" + objectKey,
                "request-id",
                "host-id");
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://127.0.0.1:9000/" + objectKey).build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
        return new ErrorResponseException(error, response, null);
    }
}
```

Extend `HistoricalLocalResourcePreviewArtifactReaderTests`:

```java
@Test
void readerOpenMissingArtifactBecomesFileNotFoundException() {
    HistoricalLocalResourcePreviewArtifactReader reader =
            new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString());

    assertThatThrownBy(() -> reader.open("pptx/9/missing.pdf"))
            .isInstanceOf(FileNotFoundException.class);
}
```

- [x] **Step 2: Run the targeted tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=SdkMinioObjectOperationsTests,HistoricalLocalResourcePreviewArtifactReaderTests" test
```

Expected: FAIL because `SdkMinioObjectOperations.getObject(...)` currently wraps missing objects as generic `IOException`, and the historical local reader currently leaks `NoSuchFileException`.

- [x] **Step 3: Implement explicit cache-miss mapping**

Update `SdkMinioObjectOperations.getObject(...)`:

```java
@Override
public InputStream getObject(String bucketName, String objectKey) throws IOException {
    try {
        return client.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
    } catch (ErrorResponseException exception) {
        String code = exception.errorResponse() == null ? null : exception.errorResponse().code();
        if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
            FileNotFoundException notFound =
                    new FileNotFoundException("failed to get minio object");
            notFound.initCause(exception);
            throw notFound;
        }
        throw asIoException("failed to get minio object", exception);
    } catch (Exception exception) {
        throw asIoException("failed to get minio object", exception);
    }
}
```

Update `HistoricalLocalResourcePreviewArtifactReader.open(...)`:

```java
public InputStream open(String artifactKey) throws IOException {
    try {
        return Files.newInputStream(pathResolver.resolve(artifactKey));
    } catch (NoSuchFileException exception) {
        FileNotFoundException notFound = new FileNotFoundException("local preview artifact not found");
        notFound.initCause(exception);
        throw notFound;
    }
}
```

- [x] **Step 4: Re-run the targeted tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=SdkMinioObjectOperationsTests,HistoricalLocalResourcePreviewArtifactReaderTests" test
```

Expected: PASS with explicit cache-miss signaling in both MinIO and historical local reads.

- [x] **Step 5: Commit the cache-miss signaling foundation**

```bash
git add backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java backend/src/main/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReader.java backend/src/test/java/com/campus/storage/SdkMinioObjectOperationsTests.java backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java
git commit -m "refactor: surface explicit preview cache misses"
```

## Task 2: Add The Composed Preview Fallback Storage

**Files:**
- Create: `backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java`
- Create: `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`

- [x] **Step 1: Write the failing fallback-storage tests**

Create `FallbackResourcePreviewArtifactStorageTests`:

```java
class FallbackResourcePreviewArtifactStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void openReturnsPrimaryArtifactWhenMinioHitExists() throws Exception {
        RecordingPrimaryStorage primary = new RecordingPrimaryStorage();
        primary.put("pptx/9/fingerprint.pdf", "%PDF-primary");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                primary,
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThat(new String(
                storage.open("pptx/9/fingerprint.pdf").readAllBytes(),
                StandardCharsets.UTF_8)).isEqualTo("%PDF-primary");
    }

    @Test
    void openFallsBackToHistoricalLocalWhenPrimaryReportsFileNotFound() throws Exception {
        Path localArtifact = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(localArtifact.getParent());
        Files.writeString(localArtifact, "%PDF-local");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                new MissingPrimaryStorage(),
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThat(new String(
                storage.open("pptx/9/fingerprint.pdf").readAllBytes(),
                StandardCharsets.UTF_8)).isEqualTo("%PDF-local");
    }

    @Test
    void openDoesNotFallbackWhenPrimaryFailsWithIoError() throws Exception {
        Path localArtifact = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(localArtifact.getParent());
        Files.writeString(localArtifact, "%PDF-local");

        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                new BrokenPrimaryStorage(new IOException("boom")),
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        assertThatThrownBy(() -> storage.open("pptx/9/fingerprint.pdf"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void writeDelegatesToPrimaryOnly() throws Exception {
        RecordingPrimaryStorage primary = new RecordingPrimaryStorage();
        FallbackResourcePreviewArtifactStorage storage = new FallbackResourcePreviewArtifactStorage(
                primary,
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString()));

        storage.write("pptx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(primary.stored("pptx/9/fingerprint.pdf")).isEqualTo("%PDF");
    }
}
```

- [x] **Step 2: Run the targeted storage tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=FallbackResourcePreviewArtifactStorageTests" test
```

Expected: FAIL because the composed fallback storage does not exist yet.

- [x] **Step 3: Implement the composed storage**

Create `FallbackResourcePreviewArtifactStorage`:

```java
public class FallbackResourcePreviewArtifactStorage implements ResourcePreviewArtifactStorage {

    private final ResourcePreviewArtifactStorage primaryStorage;
    private final HistoricalLocalResourcePreviewArtifactReader localFallbackReader;

    public FallbackResourcePreviewArtifactStorage(
            ResourcePreviewArtifactStorage primaryStorage,
            HistoricalLocalResourcePreviewArtifactReader localFallbackReader) {
        this.primaryStorage = Objects.requireNonNull(primaryStorage, "primaryStorage");
        this.localFallbackReader = Objects.requireNonNull(localFallbackReader, "localFallbackReader");
    }

    @Override
    public boolean exists(String artifactKey) throws IOException {
        if (primaryStorage.exists(artifactKey)) {
            return true;
        }
        return localFallbackReader.exists(artifactKey);
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        try {
            return primaryStorage.open(artifactKey);
        } catch (FileNotFoundException exception) {
            return localFallbackReader.open(artifactKey);
        }
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        primaryStorage.write(artifactKey, inputStream);
    }
}
```

Keep the implementation intentionally narrow:

- no write-back to MinIO on local hit
- no local deletion
- no background sync

- [x] **Step 4: Re-run the targeted storage tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=FallbackResourcePreviewArtifactStorageTests" test
```

Expected: PASS with clear primary-hit, local-fallback-hit, and failure-propagation behavior.

- [x] **Step 5: Commit the composed fallback storage**

```bash
git add backend/src/main/java/com/campus/preview/FallbackResourcePreviewArtifactStorage.java backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java
git commit -m "feat: add preview artifact fallback storage"
```

## Task 3: Wire The Fallback Property And Preview Storage Selection

**Files:**
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewProperties.java`
- Modify: `backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`

- [x] **Step 1: Write the failing configuration tests**

Extend `ResourcePreviewStorageConfigurationTests`:

```java
@Test
void minioPreviewTypeWithFallbackEnabledCreatesComposedFallbackStorage() {
    contextRunner
            .withBean(MinioObjectOperations.class, FakeMinioObjectOperations::new)
            .withPropertyValues(
                    "app.resource-preview.type=minio",
                    "app.resource-preview.read-fallback-local-enabled=true",
                    "app.resource-preview.minio-prefix=preview-artifacts",
                    "platform.integrations.minio.enabled=true",
                    "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
                    "platform.integrations.minio.access-key=minioadmin",
                    "platform.integrations.minio.secret-key=minioadmin",
                    "platform.integrations.minio.bucket=campus-platform")
            .run(context -> {
                assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                assertThat(context).hasSingleBean(FallbackResourcePreviewArtifactStorage.class);
            });
}

@Test
void localPreviewTypeIgnoresFallbackFlagAndStaysLocalOnly() {
    contextRunner
            .withPropertyValues(
                    "app.resource-preview.type=local",
                    "app.resource-preview.read-fallback-local-enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(ResourcePreviewArtifactStorage.class);
                assertThat(context).hasSingleBean(LocalResourcePreviewArtifactStorage.class);
                assertThat(context).doesNotHaveBean(FallbackResourcePreviewArtifactStorage.class);
            });
}
```

- [x] **Step 2: Run the targeted configuration tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewStorageConfigurationTests" test
```

Expected: FAIL because the new property does not exist yet and preview storage configuration cannot build the composed fallback storage.

- [x] **Step 3: Implement the new property and bean selection**

Update `ResourcePreviewProperties`:

```java
@Data
@Component
@ConfigurationProperties(prefix = "app.resource-preview")
public class ResourcePreviewProperties {

    private String type = "local";
    private String localRoot = ".local-storage/previews";
    private String minioPrefix = "preview-artifacts";
    private boolean readFallbackLocalEnabled = false;
    private Docx docx = new Docx();
}
```

Update `ResourcePreviewStorageConfiguration`:

```java
@Bean
@DependsOn("minioPreviewStorageSelectionGuard")
@ConditionalOnProperty(prefix = "app.resource-preview", name = "type", havingValue = "minio")
ResourcePreviewArtifactStorage minioResourcePreviewArtifactStorage(
        ResourcePreviewProperties previewProperties,
        MinioIntegrationProperties minioProperties,
        MinioObjectOperations operations) throws IOException {
    MinioResourcePreviewArtifactStorage primary = new MinioResourcePreviewArtifactStorage(
            minioProperties.getBucket(),
            previewProperties.getMinioPrefix(),
            operations);
    if (!previewProperties.isReadFallbackLocalEnabled()) {
        return primary;
    }
    return new FallbackResourcePreviewArtifactStorage(
            primary,
            new HistoricalLocalResourcePreviewArtifactReader(previewProperties.getLocalRoot()));
}
```

Update config defaults:

```yaml
app:
  resource-preview:
    type: ${RESOURCE_PREVIEW_TYPE:local}
    local-root: ${RESOURCE_PREVIEW_LOCAL_ROOT:.local-storage/previews}
    minio-prefix: ${RESOURCE_PREVIEW_MINIO_PREFIX:preview-artifacts}
    read-fallback-local-enabled: ${RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED:false}
```

Pin `application-local.yml` explicitly:

```yaml
app:
  resource-preview:
    type: local
    local-root: .local-storage/previews
    minio-prefix: preview-artifacts
    read-fallback-local-enabled: false
```

- [x] **Step 4: Re-run the targeted configuration tests**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewStorageConfigurationTests" test
```

Expected: PASS with all three modes covered:

- local only
- MinIO only
- MinIO plus local fallback

- [x] **Step 5: Commit the fallback configuration wiring**

```bash
git add backend/src/main/java/com/campus/config/ResourcePreviewProperties.java backend/src/main/java/com/campus/config/ResourcePreviewStorageConfiguration.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java
git commit -m "feat: add preview fallback configuration"
```

## Task 4: Refactor `ResourcePreviewService` To Use Open-First Cache Reads

**Files:**
- Modify: `backend/src/main/java/com/campus/preview/ResourcePreviewService.java`
- Modify: `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`

- [x] **Step 1: Write the failing preview-service tests**

Extend `ResourcePreviewServiceTests`:

```java
@Test
void pptxPreviewCacheMissOnOpenRegeneratesAndWritesArtifact() throws IOException {
    MissingOnOpenStorage storage = new MissingOnOpenStorage();
    CountingPptxPreviewGenerator generator = new CountingPptxPreviewGenerator();
    ResourcePreviewService service = new ResourcePreviewService(
            storage,
            new ObjectMapper(),
            generator,
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));
    ResourceItem resource = resource(9L, "career-deck.pptx", "pptx", "seed/career-deck.pptx", 1024L, LocalDateTime.now());

    service.previewFile(resource, this::samplePptxStream);

    assertThat(generator.invocationCount()).isEqualTo(1);
    assertThat(storage.writtenKeys()).containsExactly(service.pptxArtifactKeyOf(resource));
}

@Test
void pptxPreviewOpenFailureBecomesBusinessException() {
    ResourcePreviewService service = new ResourcePreviewService(
            new ThrowingOpenStorage(new IOException("boom")),
            new ObjectMapper(),
            new NoopPptxPreviewGenerator(),
            new NoopDocxPreviewGenerator(),
            new CountingZipPreviewGenerator(payload("resume/", "resume/a.md")));

    assertThatThrownBy(() -> service.previewFile(
            resource(9L, "career-deck.pptx", "pptx", "seed/career-deck.pptx", 1024L, LocalDateTime.now()),
            this::samplePptxStream))
            .isInstanceOf(BusinessException.class)
            .hasMessage("pptx preview unavailable");
}

@Test
void zipPreviewOpenFailureBecomesBusinessException() {
    ResourcePreviewService service = new ResourcePreviewService(
            new ThrowingOpenStorage(new IOException("boom")),
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

Add storage doubles:

```java
private static class MissingOnOpenStorage implements ResourcePreviewArtifactStorage {

    private final List<String> writtenKeys = new ArrayList<>();

    @Override
    public boolean exists(String artifactKey) {
        return false;
    }

    @Override
    public InputStream open(String artifactKey) throws IOException {
        throw new FileNotFoundException(artifactKey);
    }

    @Override
    public void write(String artifactKey, InputStream inputStream) throws IOException {
        writtenKeys.add(artifactKey);
        inputStream.readAllBytes();
    }
}
```

- [x] **Step 2: Run the targeted preview-service tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: FAIL because `ResourcePreviewService` still depends on `exists(...)` for cache checks and does not treat `FileNotFoundException` from `open(...)` as a cache miss.

- [x] **Step 3: Implement the open-first cache-read flow**

Refactor `ResourcePreviewService` around a helper:

```java
private Optional<InputStream> openArtifactIfPresent(String artifactKey, String unavailableMessage) {
    try {
        return Optional.of(artifactStorage.open(artifactKey));
    } catch (FileNotFoundException exception) {
        return Optional.empty();
    } catch (IOException exception) {
        throw new BusinessException(500, unavailableMessage);
    }
}
```

Use it in generated-PDF previews:

```java
private PreviewFile previewGeneratedPdf(
        String artifactKey,
        ResourceItem resource,
        GeneratedPdfSourceSupplier sourceSupplier,
        GeneratedPdfGenerator generator,
        String unavailableMessage) {
    Optional<InputStream> cachedArtifact = openArtifactIfPresent(artifactKey, unavailableMessage);
    if (cachedArtifact.isPresent()) {
        return new PreviewFile(previewFileName(resource.getFileName()), "application/pdf", cachedArtifact.get());
    }

    try (InputStream sourceInputStream = sourceSupplier.open()) {
        byte[] pdfBytes = generator.generate(sourceInputStream);
        artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
        return new PreviewFile(
                previewFileName(resource.getFileName()),
                "application/pdf",
                new ByteArrayInputStream(pdfBytes));
    } catch (IOException | RuntimeException exception) {
        throw new BusinessException(500, unavailableMessage);
    }
}
```

Use the same pattern in `previewZip(...)` before generation.

- [x] **Step 4: Re-run the preview-service tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourcePreviewServiceTests" test
```

Expected: PASS with open-first cache-hit behavior, explicit cache-miss regeneration, and unchanged user-facing unavailable messages.

- [x] **Step 5: Commit the open-first preview read path**

```bash
git add backend/src/main/java/com/campus/preview/ResourcePreviewService.java backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java
git commit -m "refactor: use open-first preview cache reads"
```

## Task 5: Add Runtime HTTP Regressions For Fallback Hits And MinIO Failures

**Files:**
- Create: `backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java`

- [x] **Step 1: Write the failing controller regression tests**

Create `ResourceControllerPreviewFallbackTests`:

```java
@SpringBootTest(properties = {
        "app.resource-preview.type=minio",
        "app.resource-preview.read-fallback-local-enabled=true",
        "platform.integrations.minio.enabled=true",
        "platform.integrations.minio.endpoint=http://127.0.0.1:9000",
        "platform.integrations.minio.access-key=minioadmin",
        "platform.integrations.minio.secret-key=minioadmin",
        "platform.integrations.minio.bucket=campus-platform"
})
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResourceControllerPreviewFallbackTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceItemMapper resourceItemMapper;

    @Autowired
    private ResourcePreviewService resourcePreviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MinioObjectOperations minioObjectOperations;

    @MockBean
    private DocxPreviewGenerator docxPreviewGenerator;

    @Test
    void guestCanPreviewPublishedPptxFromHistoricalLocalFallbackWhenMinioObjectIsMissing() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");
        writeStoredBinaryFile("seed/2026/04/career-deck.pptx", simplePptxBytes("Career Deck"));

        ResourceItem resource = resourceItemMapper.selectById(4L);
        String artifactKey = resourcePreviewService.pptxArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, samplePdfBytes());

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new FileNotFoundException("missing"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    void guestCanPreviewPublishedZipTreeFromHistoricalLocalFallbackWhenMinioObjectIsMissing() throws Exception {
        ResourceItem resource = resourceItemMapper.selectById(2L);
        String artifactKey = resourcePreviewService.zipArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, objectMapper.writeValueAsBytes(
                new ResourceZipPreviewResponse(2L, "interview-experience-notes.zip", 0, List.of())));

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new FileNotFoundException("missing"));

        mockMvc.perform(get("/api/resources/2/preview-zip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("interview-experience-notes.zip"));
    }

    @Test
    void minioFailureIsNotMaskedByHistoricalLocalFallback() throws Exception {
        insertResource(4L, 2L, "PUBLISHED", null, "career-deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/career-deck.pptx");
        ResourceItem resource = resourceItemMapper.selectById(4L);
        String artifactKey = resourcePreviewService.pptxArtifactKeyOf(resource);
        writePreviewArtifact(artifactKey, samplePdfBytes());

        when(minioObjectOperations.getObject("campus-platform", "preview-artifacts/" + artifactKey))
                .thenThrow(new IOException("boom"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("pptx preview unavailable"));
    }
}
```

Copy and adapt the local-storage helpers from `ResourceControllerTests` so this new suite stays self-contained:

- `insertResource(...)`
- `writeStoredBinaryFile(...)`
- `writePreviewArtifact(...)`
- `simplePptxBytes(...)`
- `samplePdfBytes()`
- preview/resource root cleanup in `@AfterEach`

- [x] **Step 2: Run the targeted controller regression tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerPreviewFallbackTests" test
```

Expected: FAIL because runtime fallback is not yet wired, so MinIO-mode preview requests either regenerate or fail instead of reading the historical local preview root.

- [x] **Step 3: Implement any remaining glue required by the regressions**

At this point the only remaining code changes should be small correctness fixes surfaced by the controller tests, such as:

- making sure the composed storage is actually selected in MinIO mode with fallback enabled
- making sure preview requests use the open-first cache path
- making sure MinIO `IOException` still surfaces as the existing `preview unavailable` response

Do not add ad-hoc controller logic. Keep fixes in the storage or preview service layer.

- [x] **Step 4: Re-run the controller regression tests and verify success**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceControllerPreviewFallbackTests" test
```

Expected: PASS with:

- `PPTX` fallback hit returning inline `PDF`
- `ZIP` fallback hit returning cached JSON
- MinIO failure still returning the existing `500` business error

- [x] **Step 5: Commit the HTTP regression coverage**

```bash
git add backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java
git commit -m "test: add preview fallback runtime regressions"
```

## Task 6: Update Docs And Record Validation

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md`

- [x] **Step 1: Update the README**

Apply the following documentation changes:

- add `Phase X preview artifact runtime dual-read fallback first slice` to the repository status line
- remove runtime preview dual-read fallback from the “Explicitly not implemented yet” list
- keep automatic preview-artifact cleanup explicitly out of scope
- in the preview-artifact storage section, document:
  - `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=false`
  - fallback only works when `RESOURCE_PREVIEW_TYPE=minio`
  - read order is `MinIO first -> historical local fallback`
  - fallback hits do not automatically write back to MinIO
  - the Phase W admin migration endpoint still exists as the formal copy path

Suggested README wording:

```markdown
- `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=false`
- when `RESOURCE_PREVIEW_TYPE=minio` and `RESOURCE_PREVIEW_READ_FALLBACK_LOCAL_ENABLED=true`, runtime preview reads first check MinIO and then fall back to the existing local preview root for historical `PPTX`, `DOCX`, and `ZIP` artifacts
- fallback hits remain read-only and do not automatically copy artifacts into MinIO
- MinIO infrastructure failures are not masked by local fallback
```

- [x] **Step 2: Add the Phase X validation note to the spec**

Add a validation note near the top of the Phase X spec similar to the Phase T and Phase W docs:

```markdown
> **Validation note:** This design was implemented and validated on 2026-04-22 using the approved execution record at `docs/superpowers/plans/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-implementation.md`. Local verification suites now present for this slice are `SdkMinioObjectOperationsTests`, `HistoricalLocalResourcePreviewArtifactReaderTests`, `FallbackResourcePreviewArtifactStorageTests`, `ResourcePreviewServiceTests`, `ResourcePreviewStorageConfigurationTests`, `ResourceControllerPreviewFallbackTests`, `ResourceControllerTests`, and `MinioResourcePreviewArtifactStorageTests`.
```

- [x] **Step 3: Run the targeted verification suite**

Run:

```powershell
cd backend
mvn -q "-Dtest=SdkMinioObjectOperationsTests,HistoricalLocalResourcePreviewArtifactReaderTests,FallbackResourcePreviewArtifactStorageTests,ResourcePreviewServiceTests,ResourcePreviewStorageConfigurationTests,ResourceControllerPreviewFallbackTests,ResourceControllerTests,MinioResourcePreviewArtifactStorageTests" test
```

Expected: PASS with:

- explicit cache-miss mapping covered
- fallback storage behavior covered
- preview-service regression coverage green
- configuration selection coverage green
- MinIO runtime fallback HTTP behavior green
- existing local preview HTTP behavior preserved

- [x] **Step 4: Commit the rollout notes**

```bash
git add README.md docs/superpowers/specs/2026-04-22-study-career-platform-phase-x-preview-artifact-runtime-dual-read-fallback-design.md
git commit -m "docs: add phase x preview fallback rollout notes"
```

## Final Verification Checklist

- [x] `backend/src/test/java/com/campus/storage/SdkMinioObjectOperationsTests.java`
- [x] `backend/src/test/java/com/campus/preview/HistoricalLocalResourcePreviewArtifactReaderTests.java`
- [x] `backend/src/test/java/com/campus/preview/FallbackResourcePreviewArtifactStorageTests.java`
- [x] `backend/src/test/java/com/campus/preview/ResourcePreviewServiceTests.java`
- [x] `backend/src/test/java/com/campus/config/ResourcePreviewStorageConfigurationTests.java`
- [x] `backend/src/test/java/com/campus/controller/ResourceControllerPreviewFallbackTests.java`
- [x] `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- [x] `backend/src/test/java/com/campus/preview/MinioResourcePreviewArtifactStorageTests.java`

## Execution Notes

- Keep the phase strictly backend-only plus docs.
- Prefer `apply_patch` for manual edits.
- Do not revert unrelated work if the worktree becomes dirty mid-implementation.
- If the controller regressions expose a bug in the plan order, fix the lower-level storage or preview-service layer rather than patching controller code.
- Do not add automatic MinIO write-back on local fallback hits, even if it feels convenient during implementation.
