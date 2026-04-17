package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoricalLocalResourceReaderTests {

    @TempDir
    Path tempDir;

    @Test
    void readerCanOpenExistingHistoricalFile() throws Exception {
        Path storedFile = tempDir.resolve("2026/04/17/resume.pdf");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "legacy-pdf");

        HistoricalLocalResourceReader reader = new HistoricalLocalResourceReader(tempDir.toString());

        assertThat(reader.exists("2026/04/17/resume.pdf")).isTrue();
        assertThat(new String(reader.open("2026/04/17/resume.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("legacy-pdf");
    }

    @Test
    void readerNormalizesBackslashesForHistoricalKeys() throws Exception {
        Path storedFile = tempDir.resolve("2026/04/17/resume.pdf");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "legacy-pdf");

        HistoricalLocalResourceReader reader = new HistoricalLocalResourceReader(tempDir.toString());

        assertThat(reader.exists("2026\\04\\17\\resume.pdf")).isTrue();
        assertThat(new String(reader.open("2026\\04\\17\\resume.pdf").readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("legacy-pdf");
    }

    @Test
    void readerRejectsEscapingKeys() {
        HistoricalLocalResourceReader reader = new HistoricalLocalResourceReader(tempDir.toString());

        assertThatThrownBy(() -> reader.exists("/escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local resource root");
        assertThatThrownBy(() -> reader.open("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local resource root");
    }
}
