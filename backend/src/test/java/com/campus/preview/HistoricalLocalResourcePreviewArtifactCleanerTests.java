package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.campus.config.ResourcePreviewProperties;

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
