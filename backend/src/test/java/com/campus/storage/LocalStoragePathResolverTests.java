package com.campus.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStoragePathResolverTests {

    @TempDir
    Path tempDir;

    @Test
    void resolveNormalizesBackslashesInsideConfiguredRoot() {
        LocalStoragePathResolver resolver = new LocalStoragePathResolver(tempDir);

        Path resolved = resolver.resolve("2026\\04\\17\\resume.pdf");

        assertThat(resolved).isEqualTo(tempDir.resolve("2026/04/17/resume.pdf").normalize());
    }

    @Test
    void resolveRejectsBlankKey() {
        LocalStoragePathResolver resolver = new LocalStoragePathResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storage key is blank");
    }

    @Test
    void resolveRejectsLeadingSlashAndParentTraversal() {
        LocalStoragePathResolver resolver = new LocalStoragePathResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("/escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local resource root");
        assertThatThrownBy(() -> resolver.resolve("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local resource root");
        assertThatThrownBy(() -> resolver.resolve("..\\escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local resource root");
    }
}
