package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MinioResourceFileStorageTests {

    @Test
    void constructorCreatesBucketWhenMissing() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));

        new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        assertThat(operations.createdBuckets).containsExactlyInAnyOrder("campus-platform");
    }

    @Test
    void constructorSkipsBucketCreationWhenBucketAlreadyExists() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        operations.buckets.add("campus-platform");
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));

        new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        assertThat(operations.createdBuckets).isEmpty();
    }

    @Test
    void storeOpenExistsAndDeleteRoundTripAgainstMinioBucket() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));
        MinioResourceFileStorage storage = new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        String key = storage.store("resume.pdf",
                new ByteArrayInputStream("pdf-body".getBytes(StandardCharsets.UTF_8)));

        assertThat(key).matches("2026/04/17/[0-9a-f\\-]{36}\\.pdf");
        assertThat(storage.exists(key)).isTrue();
        assertThat(new String(storage.open(key).readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("pdf-body");

        storage.delete(key);

        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    void openAndDeleteNormalizeBackslashSeparatedKeys() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        StorageKeyFactory keyFactory = new StorageKeyFactory(
                Clock.fixed(Instant.parse("2026-04-17T08:00:00Z"), ZoneId.of("Asia/Shanghai")));
        MinioResourceFileStorage storage = new MinioResourceFileStorage("campus-platform", operations, keyFactory);

        operations.putObject("campus-platform", "2026/04/17/existing.pdf",
                new ByteArrayInputStream("existing".getBytes(StandardCharsets.UTF_8)));

        assertThat(new String(storage.open("2026\\04\\17\\existing.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("existing");

        storage.delete("2026\\04\\17\\existing.pdf");

        assertThat(storage.exists("2026/04/17/existing.pdf")).isFalse();
    }

    private static final class FakeMinioObjectOperations implements MinioObjectOperations {

        private final Set<String> buckets = new HashSet<>();
        private final Set<String> createdBuckets = new HashSet<>();
        private final Map<String, byte[]> objects = new HashMap<>();

        @Override
        public boolean bucketExists(String bucketName) {
            return buckets.contains(bucketName);
        }

        @Override
        public void createBucket(String bucketName) {
            buckets.add(bucketName);
            createdBuckets.add(bucketName);
        }

        @Override
        public void putObject(String bucketName, String objectKey, InputStream inputStream) throws IOException {
            requireBucket(bucketName);
            objects.put(objectKey(bucketName, objectKey), inputStream.readAllBytes());
        }

        @Override
        public InputStream getObject(String bucketName, String objectKey) throws IOException {
            requireBucket(bucketName);
            byte[] bytes = objects.get(objectKey(bucketName, objectKey));
            if (bytes == null) {
                throw new IOException("object not found");
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public boolean objectExists(String bucketName, String objectKey) throws IOException {
            requireBucket(bucketName);
            return objects.containsKey(objectKey(bucketName, objectKey));
        }

        @Override
        public void removeObject(String bucketName, String objectKey) throws IOException {
            requireBucket(bucketName);
            objects.remove(objectKey(bucketName, objectKey));
        }

        private void requireBucket(String bucketName) throws IOException {
            if (!buckets.contains(bucketName)) {
                throw new IOException("bucket not found");
            }
        }

        private String objectKey(String bucketName, String objectKey) {
            return bucketName + ":" + objectKey;
        }
    }
}
