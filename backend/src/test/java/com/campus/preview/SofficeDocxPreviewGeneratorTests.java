package com.campus.preview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

class SofficeDocxPreviewGeneratorTests {

    @Test
    void generatorReturnsPdfBytesWhenCommandProducesPdf() throws Exception {
        SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
                "fake-soffice",
                (command, workingDirectory) -> {
                    assertThat(command).containsExactly(
                            "fake-soffice",
                            "--headless",
                            "--convert-to",
                            "pdf",
                            "--outdir",
                            workingDirectory.toString(),
                            workingDirectory.resolve("preview.docx").toString());
                    Files.writeString(workingDirectory.resolve("preview.pdf"), "%PDF-1.7\n");
                    return 0;
                });

        byte[] pdf = generator.generate(new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8)));

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generatorFailsWhenCommandExitsWithoutPdfOutput() {
        SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
                "fake-soffice",
                (command, workingDirectory) -> 0);

        assertThatThrownBy(() -> generator.generate(new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("docx preview unavailable");
    }

    @Test
    void generatorFailsWhenCommandCannotBeStartedAndFallbackCannotReadInput() {
        SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
                "fake-soffice",
                (command, workingDirectory) -> {
                    throw new IOException("missing soffice");
                });

        assertThatThrownBy(() -> generator.generate(new ByteArrayInputStream("docx".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("docx preview unavailable");
    }

    @Test
    void generatorFallsBackToJavaPdfWhenCommandCannotBeStarted() throws Exception {
        SofficeDocxPreviewGenerator generator = new SofficeDocxPreviewGenerator(
                "fake-soffice",
                (command, workingDirectory) -> {
                    throw new IOException("missing soffice");
                });

        byte[] pdf = generator.generate(new ByteArrayInputStream(simpleDocxBytes("Resume Preview")));

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private byte[] simpleDocxBytes(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText(text);
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
