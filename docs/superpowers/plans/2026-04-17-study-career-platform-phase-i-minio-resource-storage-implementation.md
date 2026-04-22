# Study-Career Platform Phase I MinIO Resource Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MinIO-backed raw resource storage for non-`local` environments without changing the existing resource HTTP contract, database schema, or Phase H preview behavior.

**Architecture:** Keep `ResourceFileStorage` as the single raw-file abstraction and add a MinIO implementation selected by Spring configuration. Preserve `local` profile filesystem storage, keep preview artifacts on local disk, and make MinIO startup fail fast when explicitly selected but misconfigured.

**Tech Stack:** Java 17, Spring Boot 3, Spring Security, MinIO Java SDK (`io.minio:minio:8.6.0` per official MinIO Java SDK docs as of 2026-04-17), H2 test profile, JUnit 5, AssertJ, Docker Compose

---

## Context Before Starting

- Spec baseline: `docs/superpowers/specs/2026-04-17-study-career-platform-phase-i-minio-resource-storage-design.md`
- Current resource storage baseline:
  - `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
  - `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
  - `backend/src/main/java/com/campus/service/ResourceService.java`
- Current runtime config baseline:
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/resources/application-local.yml`
  - `backend/src/test/resources/application.yml`
  - `docker-compose.yml`
- Current verification baseline:
  - `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
  - `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
  - `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`
- Important existing rule:
  - Phase H preview artifacts stay under `.local-storage/previews`
  - this plan must not move preview artifacts into MinIO
- Important migration boundary:
  - no automatic migration of old local resource files is in scope

## Scope Lock

This plan covers only the approved Phase I slice:

- add MinIO-backed raw resource storage
- keep `local` profile on local filesystem storage
- preserve `storageKey` as a logical object key
- fail fast when `type=minio` is selected but MinIO is disabled or misconfigured
- update Compose and README for MinIO-backed runtime

This plan explicitly does not implement:

- MinIO-backed preview artifact storage
- presigned URLs
- frontend direct-to-MinIO access
- historical resource migration
- dual local+MinIO fallback reads
- chunk upload or other resource feature changes

## Planned File Structure

### Backend: Create

- Create: `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
  MinIO config binding for `platform.integrations.minio`.
- Create: `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
  Conditional bean wiring for local vs MinIO raw storage.
- Create: `backend/src/main/java/com/campus/storage/StorageKeyFactory.java`
  Shared `yyyy/MM/dd/<uuid>.<ext>` key generator.
- Create: `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
  Small testable gateway interface around MinIO bucket/object operations.
- Create: `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
  MinIO SDK-backed implementation of `MinioObjectOperations`.
- Create: `backend/src/main/java/com/campus/storage/MinioResourceFileStorage.java`
  Raw resource storage implementation backed by MinIO.
- Create: `backend/src/test/java/com/campus/storage/StorageKeyFactoryTests.java`
  Key-format tests.
- Create: `backend/src/test/java/com/campus/storage/MinioResourceFileStorageTests.java`
  Repository-safe MinIO storage tests using a fake `MinioObjectOperations`.
- Create: `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
  Spring bean-selection and fail-fast configuration tests.

### Backend: Modify Existing

- Modify: `backend/pom.xml`
  Add MinIO Java SDK dependency.
- Modify: `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
  Tighten remote-safe existence semantics.
- Modify: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
  Remove direct component wiring and reuse `StorageKeyFactory`.
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
  Handle storage existence/open/delete I/O failures correctly.
- Modify: `backend/src/main/resources/application.yml`
  Keep MinIO properties explicit and runtime-safe.
- Modify: `backend/src/main/resources/application-local.yml`
  Keep local profile pinned to filesystem raw storage.
- Modify: `backend/src/test/resources/application.yml`
  Keep MinIO disabled in default test runs.
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
  Extend config assertions for MinIO and local profile behavior.

### Deployment And Docs: Modify Existing

- Modify: `docker-compose.yml`
  Add MinIO service, data volume, and backend env wiring.
- Modify: `README.md`
  Document local-vs-MinIO storage behavior, compose usage, and migration boundary.

## Responsibility Notes

- `StorageKeyFactory` is the single source of truth for raw object-key generation.
- `ResourceFileStorage` remains the only abstraction the service layer uses for raw file reads and writes.
- `MinioObjectOperations` exists only to keep MinIO storage behavior unit-testable without mandatory Docker or live object storage during `mvn test`.
- `MinioResourceFileStorage` owns bucket initialization and raw object operations; it does not own controller behavior, permissions, or preview cache logic.
- `ResourceStorageConfiguration` owns backend selection between local and MinIO raw storage; no other class should guess which storage backend is active.
- `ResourceService` remains responsible for mapping infrastructure failures into existing business error semantics.

## Task 1: Add Shared Storage-Key Generation And MinIO Config Baseline

**Files:**
- Modify: `backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java`
- Create: `backend/src/test/java/com/campus/storage/StorageKeyFactoryTests.java`
- Create: `backend/src/main/java/com/campus/config/MinioIntegrationProperties.java`
- Create: `backend/src/main/java/com/campus/storage/StorageKeyFactory.java`
- Modify: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/resources/application.yml`

- [x] **Step 1: Write the failing config and key-format tests**

Add a focused key-format test:

```java
package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class StorageKeyFactoryTests {

    @Test
    void generatedKeyKeepsDatePrefixAndOriginalExtension() {
        StorageKeyFactory factory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));

        String key = factory.newStorageKey("resume-template-pack.pdf");

        assertThat(key).matches("2026/04/17/[0-9a-f\\-]{36}\\.pdf");
    }
}
```

Extend config safety coverage so these expectations are explicit:

```java
@Test
void defaultApplicationConfigKeepsMinioDisabledUntilExplicitlyEnabled() {
    Properties properties = loadYaml(Path.of("src", "main", "resources", "application.yml"));

    assertThat(properties.getProperty("app.resource-storage.type")).isEqualTo("local");
    assertThat(properties.getProperty("platform.integrations.minio.enabled")).isEqualTo("false");
    assertThat(properties.getProperty("platform.integrations.minio.bucket")).isEqualTo("campus-platform");
}

@Test
void testConfigKeepsMinioDisabledByDefault() {
    Properties properties = loadYaml(Path.of("src", "test", "resources", "application.yml"));

    assertThat(properties.getProperty("platform.integrations.minio.enabled")).isEqualTo("false");
    assertThat(properties.getProperty("platform.integrations.minio.bucket")).isEqualTo("campus-platform-test");
}
```

- [x] **Step 2: Run the targeted tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=StorageKeyFactoryTests,ApplicationConfigSafetyTests" test
```

Expected: FAIL because `StorageKeyFactory` and `MinioIntegrationProperties` do not exist yet and the config assertions are not fully pinned.

- [x] **Step 3: Implement the shared key generator and config baseline**

Create `MinioIntegrationProperties`:

```java
package com.campus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "platform.integrations.minio")
public class MinioIntegrationProperties {

    private boolean enabled = false;
    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "campus-platform";
}
```

Create a shared key generator:

```java
package com.campus.storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class StorageKeyFactory {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final Clock clock;

    public StorageKeyFactory() {
        this(Clock.systemDefaultZone());
    }

    StorageKeyFactory(Clock clock) {
        this.clock = clock;
    }

    public String newStorageKey(String originalFilename) {
        String extension = extractExtension(originalFilename);
        return DATE_PATH.format(LocalDate.now(clock)) + "/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(lastDot);
    }
}
```

Refactor `LocalResourceFileStorage` to accept `StorageKeyFactory` and use it instead of generating keys inline.

Adjust the config safety helper so it can load either main-resource or test-resource YAML files directly from an absolute `Path`, for example:

```java
private Properties loadYaml(Path resourcePath) {
    YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
    factoryBean.setResources(new FileSystemResource(resourcePath));
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
}
```

Pin the YAMLs explicitly:

```yaml
# backend/src/main/resources/application.yml
app:
  resource-storage:
    type: ${RESOURCE_STORAGE_TYPE:local}
    local-root: ${RESOURCE_STORAGE_LOCAL_ROOT:.local-storage/resources}

platform:
  integrations:
    minio:
      enabled: ${MINIO_ENABLED:false}
      endpoint: ${MINIO_ENDPOINT:http://127.0.0.1:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: ${MINIO_BUCKET:campus-platform}
```

```yaml
# backend/src/main/resources/application-local.yml
app:
  resource-storage:
    type: local

platform:
  integrations:
    minio:
      enabled: false
```

```yaml
# backend/src/test/resources/application.yml
app:
  resource-storage:
    type: local

platform:
  integrations:
    minio:
      enabled: false
      bucket: campus-platform-test
```

- [x] **Step 4: Run the targeted tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=StorageKeyFactoryTests,ApplicationConfigSafetyTests" test
```

Expected: PASS with deterministic key-shape coverage and explicit MinIO config defaults.

- [x] **Step 5: Commit the shared storage baseline**

```bash
git add backend/src/test/java/com/campus/config/ApplicationConfigSafetyTests.java backend/src/test/java/com/campus/storage/StorageKeyFactoryTests.java backend/src/main/java/com/campus/config/MinioIntegrationProperties.java backend/src/main/java/com/campus/storage/StorageKeyFactory.java backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java backend/src/main/resources/application.yml backend/src/main/resources/application-local.yml backend/src/test/resources/application.yml
git commit -m "refactor: prepare shared resource storage key generation"
```

## Task 2: Implement The MinIO Raw Storage Backend

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/campus/storage/MinioObjectOperations.java`
- Create: `backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java`
- Create: `backend/src/main/java/com/campus/storage/MinioResourceFileStorage.java`
- Create: `backend/src/test/java/com/campus/storage/MinioResourceFileStorageTests.java`

- [x] **Step 1: Write the failing MinIO storage tests**

Create repository-safe tests around a fake `MinioObjectOperations` implementation:

```java
package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class MinioResourceFileStorageTests {

    @Test
    void constructorCreatesBucketWhenMissing() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));

        new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        assertThat(operations.bucketCreated).isTrue();
    }

    @Test
    void storeOpenExistsAndDeleteRoundTripAgainstMinioBucket() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));
        MinioResourceFileStorage storage = new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        String key = storage.store("resume.pdf",
                new ByteArrayInputStream("pdf-body".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.exists(key)).isTrue();
        assertThat(new String(storage.open(key).readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("pdf-body");

        storage.delete(key);

        assertThat(storage.exists(key)).isFalse();
    }
}
```

- [x] **Step 2: Run the MinIO storage tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=MinioResourceFileStorageTests" test
```

Expected: FAIL because the MinIO storage classes and dependency do not exist yet.

- [x] **Step 3: Add the MinIO dependency and implement the storage backend**

Add the MinIO Java SDK dependency to `backend/pom.xml`:

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.6.0</version>
</dependency>
```

Create a narrow MinIO operations interface:

```java
package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

public interface MinioObjectOperations {

    boolean bucketExists(String bucketName) throws IOException;

    void createBucket(String bucketName) throws IOException;

    void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException;

    InputStream getObject(String bucketName, String objectKey) throws IOException;

    boolean objectExists(String bucketName, String objectKey) throws IOException;

    void removeObject(String bucketName, String objectKey) throws IOException;
}
```

Create the SDK-backed adapter:

```java
package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;

public class SdkMinioObjectOperations implements MinioObjectOperations {

    private final MinioClient client;

    public SdkMinioObjectOperations(MinioClient client) {
        this.client = client;
    }

    @Override
    public boolean bucketExists(String bucketName) throws IOException { /* wrap SDK exceptions */ }

    @Override
    public void createBucket(String bucketName) throws IOException { /* wrap SDK exceptions */ }

    @Override
    public void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        client.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(inputStream, -1, 10 * 1024 * 1024)
                .contentType("application/octet-stream")
                .build());
    }

    @Override
    public InputStream getObject(String bucketName, String objectKey) throws IOException { /* wrap SDK exceptions */ }

    @Override
    public boolean objectExists(String bucketName, String objectKey) throws IOException { /* statObject */ }

    @Override
    public void removeObject(String bucketName, String objectKey) throws IOException { /* wrap SDK exceptions */ }
}
```

Create the MinIO storage implementation:

```java
package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

public class MinioResourceFileStorage implements ResourceFileStorage {

    private final String bucketName;
    private final MinioObjectOperations operations;
    private final StorageKeyFactory keyFactory;

    public MinioResourceFileStorage(String bucketName, MinioObjectOperations operations, StorageKeyFactory keyFactory)
            throws IOException {
        this.bucketName = bucketName;
        this.operations = operations;
        this.keyFactory = keyFactory;
        ensureBucket();
    }

    @Override
    public String store(String originalFilename, InputStream inputStream) throws IOException {
        String key = keyFactory.newStorageKey(originalFilename);
        operations.putObject(bucketName, key, inputStream);
        return key;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return operations.getObject(bucketName, normalizeKey(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        operations.removeObject(bucketName, normalizeKey(storageKey));
    }

    @Override
    public boolean exists(String storageKey) throws IOException {
        return operations.objectExists(bucketName, normalizeKey(storageKey));
    }

    private void ensureBucket() throws IOException {
        if (!operations.bucketExists(bucketName)) {
            operations.createBucket(bucketName);
        }
    }
}
```

- [x] **Step 4: Run the MinIO storage tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=MinioResourceFileStorageTests" test
```

Expected: PASS with bucket initialization and object round-trip behavior covered without requiring a live MinIO server.

- [x] **Step 5: Commit the MinIO storage backend**

```bash
git add backend/pom.xml backend/src/main/java/com/campus/storage/MinioObjectOperations.java backend/src/main/java/com/campus/storage/SdkMinioObjectOperations.java backend/src/main/java/com/campus/storage/MinioResourceFileStorage.java backend/src/test/java/com/campus/storage/MinioResourceFileStorageTests.java
git commit -m "feat: add minio resource file storage backend"
```

## Task 3: Wire Storage Selection Into Spring And Preserve Resource-Service Semantics

**Files:**
- Create: `backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java`
- Modify: `backend/src/main/java/com/campus/storage/ResourceFileStorage.java`
- Modify: `backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java`
- Modify: `backend/src/main/java/com/campus/service/ResourceService.java`
- Create: `backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java`
- Modify: `backend/src/test/java/com/campus/controller/ResourceControllerTests.java`
- Modify: `backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java`

- [x] **Step 1: Write the failing Spring wiring and regression tests**

Create focused storage-configuration tests:

```java
package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.campus.storage.LocalResourceFileStorage;
import com.campus.storage.StorageKeyFactory;

class ResourceStorageConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ResourceStorageProperties.class,
                    MinioIntegrationProperties.class,
                    ResourceStorageConfiguration.class,
                    StorageKeyFactory.class);

    @Test
    void localStorageTypeCreatesLocalResourceFileStorage() {
        contextRunner
                .withPropertyValues("app.resource-storage.type=local")
                .run(context -> assertThat(context).hasSingleBean(LocalResourceFileStorage.class));
    }

    @Test
    void minioStorageTypeFailsFastWhenMinioIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.resource-storage.type=minio",
                        "platform.integrations.minio.enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure()).hasMessageContaining("minio storage is selected but disabled");
                });
    }
}
```

Add one narrow regression assertion to `ResourceControllerTests` so storage-I/O failures do not silently become `404`:

```java
@Test
@WithMockUser(username = "2", roles = "USER")
void missingVisibleFileStillReturnsInfrastructureFailure() throws Exception {
    insertResource(6L, 2L, "PENDING", null, "owner-only.pdf", "pdf", "application/pdf",
            "seed/2026/04/owner-only.pdf");

    mockMvc.perform(get("/api/resources/6/preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("resource file unavailable"));
}
```

- [x] **Step 2: Run the targeted Spring wiring and controller tests and verify failure**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceStorageConfigurationTests,ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: FAIL because Spring storage selection is not wired yet and `ResourceFileStorage` still assumes a local-only `exists(...)` contract.

- [x] **Step 3: Implement conditional bean selection and remote-safe storage handling**

Tighten the storage interface:

```java
package com.campus.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceFileStorage {

    String store(String originalFilename, InputStream inputStream) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey) throws IOException;
}
```

Add Spring configuration for storage selection:

```java
package com.campus.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.campus.storage.LocalResourceFileStorage;
import com.campus.storage.MinioObjectOperations;
import com.campus.storage.MinioResourceFileStorage;
import com.campus.storage.ResourceFileStorage;
import com.campus.storage.SdkMinioObjectOperations;
import com.campus.storage.StorageKeyFactory;

@Configuration
public class ResourceStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "local", matchIfMissing = true)
    ResourceFileStorage localResourceFileStorage(ResourceStorageProperties properties, StorageKeyFactory keyFactory) {
        return new LocalResourceFileStorage(properties, keyFactory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
    MinioClient minioClient(MinioIntegrationProperties properties) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("minio storage is selected but disabled");
        }
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
    MinioObjectOperations minioObjectOperations(MinioClient client) {
        return new SdkMinioObjectOperations(client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.resource-storage", name = "type", havingValue = "minio")
    ResourceFileStorage minioResourceFileStorage(
            MinioIntegrationProperties properties,
            MinioObjectOperations operations,
            StorageKeyFactory keyFactory) throws java.io.IOException {
        return new MinioResourceFileStorage(properties.getBucket(), operations, keyFactory);
    }
}
```

Update `ResourceService` so storage failures stay infrastructure errors:

```java
private ResourceFileStream openResourceFile(ResourceItem resource) {
    try {
        if (!resourceFileStorage.exists(resource.getStorageKey())) {
            throw new BusinessException(500, "resource file unavailable");
        }
        return new ResourceFileStream(resource.getFileName(), resource.getContentType(),
                resourceFileStorage.open(resource.getStorageKey()));
    } catch (IOException exception) {
        throw new BusinessException(500, "resource file unavailable");
    }
}

private void tryDeleteReplacedFile(String previousStorageKey, String currentStorageKey) {
    if (previousStorageKey == null || previousStorageKey.isBlank() || previousStorageKey.equals(currentStorageKey)) {
        return;
    }
    try {
        if (!resourceFileStorage.exists(previousStorageKey)) {
            return;
        }
        resourceFileStorage.delete(previousStorageKey);
    } catch (IOException exception) {
        log.warn("Failed to delete replaced resource file: {}", previousStorageKey, exception);
    }
}
```

Also remove direct `@Component` wiring from `LocalResourceFileStorage` so `ResourceStorageConfiguration` becomes the only selector.

- [x] **Step 4: Run the targeted Spring wiring and controller tests again and make them pass**

Run:

```powershell
cd backend
mvn -q "-Dtest=ResourceStorageConfigurationTests,ResourceControllerTests,AdminResourceControllerTests" test
```

Expected: PASS with local-vs-MinIO selection covered and resource controller behavior preserved.

- [x] **Step 5: Commit the Spring storage wiring**

```bash
git add backend/src/main/java/com/campus/config/ResourceStorageConfiguration.java backend/src/main/java/com/campus/storage/ResourceFileStorage.java backend/src/main/java/com/campus/storage/LocalResourceFileStorage.java backend/src/main/java/com/campus/service/ResourceService.java backend/src/test/java/com/campus/config/ResourceStorageConfigurationTests.java backend/src/test/java/com/campus/controller/ResourceControllerTests.java backend/src/test/java/com/campus/controller/admin/AdminResourceControllerTests.java
git commit -m "feat: wire selectable raw resource storage backends"
```

## Task 4: Update Docker Compose For MinIO-Backed Runtime

**Files:**
- Modify: `docker-compose.yml`

- [x] **Step 1: Update the Compose scaffold to include MinIO**

Extend `docker-compose.yml` so the stack becomes `mysql + minio + backend + frontend`.

Add a MinIO service like:

```yaml
  minio:
    image: minio/minio:latest
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
      TZ: Asia/Shanghai
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data
```

Update backend env so raw resource storage explicitly switches to MinIO while preview cache stays local:

```yaml
  backend:
    environment:
      DB_URL: jdbc:mysql://mysql:3306/campus_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      DB_USERNAME: root
      DB_PASSWORD: Qiao@2541Pass
      RESOURCE_STORAGE_TYPE: minio
      MINIO_ENABLED: "true"
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
      MINIO_BUCKET: campus-platform
```

Keep the existing backend local-storage volume because preview artifacts are still local in this phase.
Use backend restart policy plus MinIO startup order instead of a container-internal `curl` healthcheck, since recent MinIO images no longer reliably ship curl.

- [x] **Step 2: Validate the Compose file**

Run:

```powershell
docker compose config
```

Expected: PASS with merged config output that includes `minio`, MinIO env vars on `backend`, and both `mysql-data`, `minio-data`, and `backend-data` volumes.

- [x] **Step 3: Commit the Compose update**

```bash
git add docker-compose.yml
git commit -m "chore(deploy): add minio-backed resource storage runtime"
```

## Task 5: Update README And Run Verification

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update README for Phase I MinIO raw storage**

Document all of the following:

- `local` profile still uses local filesystem resource storage
- non-`local` or Compose runtime can enable MinIO-backed raw resource storage
- preview artifacts remain under backend local storage in this phase
- historical local resource files are not migrated automatically
- Compose stack now includes MinIO and the MinIO console port

Recommended README additions:

```md
- `backend/.local-storage/resources/`: default local raw resource storage in the `local` profile
- `backend/.local-storage/previews/`: preview artifact cache; still local even when raw resource storage uses MinIO
```

```md
Explicitly not implemented yet:

- historical local-resource migration into MinIO
- MinIO-backed preview artifact storage
```

```md
Optional Docker Stack notes:

- MinIO API is exposed on `http://127.0.0.1:9000`
- MinIO console is exposed on `http://127.0.0.1:9001`
- backend stores raw resource files in MinIO and preview artifacts in the `backend-data` volume
```

- [x] **Step 2: Run targeted backend verification**

Run:

```powershell
cd backend
mvn -q "-Dtest=StorageKeyFactoryTests,MinioResourceFileStorageTests,ResourceStorageConfigurationTests,ApplicationConfigSafetyTests,ResourceControllerTests,AdminResourceControllerTests,ResourcePreviewServiceTests,ApachePoiPptxPreviewGeneratorTests" test
```

Expected: PASS.

- [x] **Step 3: Run full backend regression**

Run:

```powershell
cd backend
mvn test
```

Expected: PASS.

- [x] **Step 4: Run frontend regression and build checks**

Run:

```powershell
cd frontend
npm run test
npm run build
```

Expected: PASS. No frontend code changes are expected, so any failure here is a regression signal.

- [x] **Step 5: Run compose-level smoke when Docker is available**

Run:

```powershell
docker compose up --build -d
docker compose ps
```

Manual smoke checklist:

1. Open the app through the frontend port.
2. Log in as `13800000001` and upload a new PDF resource.
3. Confirm the upload appears in `/profile/resources`.
4. Log in as admin and publish the resource.
5. Download and preview the published resource through the app.
6. Replace a rejected resource file and confirm the resubmission still succeeds.
7. Confirm preview artifacts continue to appear in the backend local-storage volume rather than in MinIO.

If Docker is unavailable in the execution environment, record that `docker compose config` passed but live compose smoke could not be executed.

- [x] **Step 6: Commit the docs and verification updates**

```bash
git add README.md
git commit -m "docs: add minio raw storage usage notes"
```

## Execution Notes

- Follow TDD in order. Do not skip the failing-test step even if the storage wiring looks straightforward.
- Keep preview artifact storage untouched in this phase.
- Do not change resource controller routes or frontend API calls.
- Keep `storageKey` bucket-agnostic and URL-agnostic.
- Do not add automatic migration logic or dual-read fallback.
- If `docker compose` is unavailable during execution, do not block the code changes; finish implementation, validate backend/frontend regressions, and record the compose limitation in docs or execution notes.
