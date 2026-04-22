package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.campus.config.ResourcePreviewProperties;

class LocalResourcePreviewArtifactStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void openMissingArtifactBecomesFileNotFoundException() {
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setLocalRoot(tempDir.toString());
        LocalResourcePreviewArtifactStorage storage = new LocalResourcePreviewArtifactStorage(properties);

        assertThatThrownBy(() -> storage.open("pptx/9/missing.pdf"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void openExistingArtifactReturnsStoredBytes() throws Exception {
        ResourcePreviewProperties properties = new ResourcePreviewProperties();
        properties.setLocalRoot(tempDir.toString());
        LocalResourcePreviewArtifactStorage storage = new LocalResourcePreviewArtifactStorage(properties);
        Path artifactPath = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(artifactPath.getParent());
        Files.writeString(artifactPath, "%PDF");

        assertThat(new String(storage.open("pptx/9/fingerprint.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("%PDF");
    }

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
}
