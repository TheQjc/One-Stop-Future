package com.campus.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class BackendDockerfileRuntimeDependencyTests {

    @Test
    void runtimeImageInstallsAwtFontDependenciesForPreviewRendering() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile")).toLowerCase(Locale.ROOT);

        assertThat(dockerfile).contains("libfreetype6");
        assertThat(dockerfile).contains("fontconfig");
        assertThat(dockerfile).contains("fonts-dejavu-core");
        assertThat(dockerfile).contains("java.awt.headless=true");
        assertThat(dockerfile).contains("rm -rf /var/lib/apt/lists/*");
    }
}
