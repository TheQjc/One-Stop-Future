package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPreviewArtifactPathResolverTests {

    @TempDir
    Path tempDir;

    @Test
    void resolveNormalizesBackslashesInsideConfiguredPreviewRoot() {
        LocalPreviewArtifactPathResolver resolver = new LocalPreviewArtifactPathResolver(tempDir);

        Path resolved = resolver.resolve("pptx\\9\\fingerprint.pdf");

        assertThat(resolved).isEqualTo(tempDir.resolve("pptx/9/fingerprint.pdf").normalize());
    }

    @Test
    void resolveRejectsBlankKey() {
        LocalPreviewArtifactPathResolver resolver = new LocalPreviewArtifactPathResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifact key is blank");
    }

    @Test
    void resolveRejectsLeadingSlashAndParentTraversal() {
        LocalPreviewArtifactPathResolver resolver = new LocalPreviewArtifactPathResolver(tempDir);

        assertThatThrownBy(() -> resolver.resolve("/escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
        assertThatThrownBy(() -> resolver.resolve("../escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
        assertThatThrownBy(() -> resolver.resolve("..\\escape.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes local preview root");
    }
}
