package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.campus.storage.MinioObjectOperations;

class MinioResourcePreviewArtifactStorageTests {

    @Test
    void constructorCreatesBucketWhenMissing() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();

        new MinioResourcePreviewArtifactStorage("campus-platform", "preview-artifacts", operations);

        assertThat(operations.createdBuckets).containsExactly("campus-platform");
    }

    @Test
    void writeOpenAndExistsRoundTripUnderConfiguredPrefix() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        MinioResourcePreviewArtifactStorage storage = new MinioResourcePreviewArtifactStorage(
                "campus-platform", "preview-artifacts", operations);

        storage.write("docx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.exists("docx/9/fingerprint.pdf")).isTrue();
        assertThat(new String(storage.open("docx/9/fingerprint.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("%PDF");
        assertThat(operations.objects).containsKey("campus-platform:preview-artifacts/docx/9/fingerprint.pdf");
    }

    @Test
    void openAndExistsNormalizeBackslashSeparatedArtifactKeys() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        MinioResourcePreviewArtifactStorage storage = new MinioResourcePreviewArtifactStorage(
                "campus-platform", "preview-artifacts/", operations);

        operations.putObject("campus-platform", "preview-artifacts/pptx/11/cached.pdf",
                new ByteArrayInputStream("cached".getBytes(StandardCharsets.UTF_8)));

        assertThat(storage.exists("pptx\\11\\cached.pdf")).isTrue();
        assertThat(new String(storage.open("pptx\\11\\cached.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("cached");
    }

    @Test
    void deleteRemovesObjectUnderConfiguredPrefix() throws Exception {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();
        MinioResourcePreviewArtifactStorage storage = new MinioResourcePreviewArtifactStorage(
                "campus-platform", "preview-artifacts", operations);
        storage.write("docx/9/fingerprint.pdf", new ByteArrayInputStream("%PDF".getBytes(StandardCharsets.UTF_8)));

        storage.delete("docx/9/fingerprint.pdf");

        assertThat(storage.exists("docx/9/fingerprint.pdf")).isFalse();
    }

    @Test
    void constructorRejectsBlankPrefix() {
        FakeMinioObjectOperations operations = new FakeMinioObjectOperations();

        assertThatThrownBy(() -> new MinioResourcePreviewArtifactStorage("campus-platform", "   ", operations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preview minio prefix is blank");
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
