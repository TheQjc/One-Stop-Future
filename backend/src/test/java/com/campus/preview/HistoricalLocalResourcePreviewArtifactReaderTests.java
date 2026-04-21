package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void readerNormalizesBackslashesForHistoricalPreviewKeys() throws Exception {
        Path storedFile = tempDir.resolve("pptx/9/fingerprint.pdf");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "%PDF");

        HistoricalLocalResourcePreviewArtifactReader reader =
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString());

        assertThat(reader.exists("pptx\\9\\fingerprint.pdf")).isTrue();
        assertThat(new String(reader.open("pptx\\9\\fingerprint.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("%PDF");
    }

    @Test
    void readerRejectsEscapingKeys() {
        HistoricalLocalResourcePreviewArtifactReader reader =
                new HistoricalLocalResourcePreviewArtifactReader(tempDir.toString());

        assertThatThrownBy(() -> reader.exists("/escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
        assertThatThrownBy(() -> reader.open("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
    }
}
