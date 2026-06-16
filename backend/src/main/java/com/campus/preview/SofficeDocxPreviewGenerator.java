package com.campus.preview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.campus.config.ResourcePreviewProperties;

@Component
public class SofficeDocxPreviewGenerator implements DocxPreviewGenerator {

    private static final String PREVIEW_UNAVAILABLE_MESSAGE = "docx preview unavailable";

    private final String sofficeCommand;
    private final CommandRunner commandRunner;

    @Autowired
    public SofficeDocxPreviewGenerator(ResourcePreviewProperties properties) {
        this(properties.getDocx().getSofficeCommand(), defaultCommandRunner());
    }

    SofficeDocxPreviewGenerator(String sofficeCommand, CommandRunner commandRunner) {
        this.sofficeCommand = Objects.requireNonNull(sofficeCommand, "sofficeCommand");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");
    }

    @Override
    public byte[] generate(InputStream docxInputStream) throws IOException {
        Objects.requireNonNull(docxInputStream, "docxInputStream");
        byte[] docxBytes = docxInputStream.readAllBytes();

        try {
            return generateWithSoffice(docxBytes);
        } catch (IOException sofficeException) {
            try {
                return generateWithJavaFallback(docxBytes);
            } catch (IOException | RuntimeException fallbackException) {
                IOException exception = new IOException(PREVIEW_UNAVAILABLE_MESSAGE, fallbackException);
                exception.addSuppressed(sofficeException);
                throw exception;
            }
        }
    }

    private byte[] generateWithSoffice(byte[] docxBytes) throws IOException {
        Path tempDirectory = Files.createTempDirectory("docx-preview-");
        try {
            Path inputFile = tempDirectory.resolve("preview.docx");
            Path outputFile = tempDirectory.resolve("preview.pdf");
            Files.copy(new ByteArrayInputStream(docxBytes), inputFile, StandardCopyOption.REPLACE_EXISTING);

            int exitCode = runConverter(inputFile, tempDirectory);
            if (exitCode != 0 || !Files.exists(outputFile)) {
                throw new IOException(PREVIEW_UNAVAILABLE_MESSAGE);
            }

            return Files.readAllBytes(outputFile);
        } finally {
            deleteRecursively(tempDirectory);
        }
    }

    private byte[] generateWithJavaFallback(byte[] docxBytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes));
                PDDocument pdfDocument = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            List<String> lines = extractTextLines(document);
            if (lines.isEmpty()) {
                lines = List.of("DOCX preview");
            }
            renderLinesToPdf(pdfDocument, lines);
            pdfDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<String> extractTextLines(XWPFDocument document) {
        List<String> lines = new ArrayList<>();
        document.getParagraphs().forEach(paragraph -> {
            String text = paragraph.getText();
            if (text != null && !text.isBlank()) {
                lines.add(text.trim());
            }
        });
        return lines;
    }

    private void renderLinesToPdf(PDDocument pdfDocument, List<String> lines) throws IOException {
        int pageWidth = 1240;
        int pageHeight = 1754;
        int margin = 90;
        int lineHeight = 40;
        int maxLinesPerPage = Math.max(1, (pageHeight - margin * 2) / lineHeight);

        for (int index = 0; index < lines.size(); index += maxLinesPerPage) {
            BufferedImage pageImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = pageImage.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, pageWidth, pageHeight);
                graphics.setColor(new Color(24, 38, 63));
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));

                int y = margin;
                int pageEnd = Math.min(lines.size(), index + maxLinesPerPage);
                for (String line : lines.subList(index, pageEnd)) {
                    graphics.drawString(line, margin, y);
                    y += lineHeight;
                }
            } finally {
                graphics.dispose();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            pdfDocument.addPage(page);
            PDImageXObject image = LosslessFactory.createFromImage(pdfDocument, pageImage);
            try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page)) {
                contentStream.drawImage(image, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
            }
        }
    }

    private int runConverter(Path inputFile, Path tempDirectory) throws IOException {
        try {
            return commandRunner.run(buildCommand(inputFile, tempDirectory), tempDirectory);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(PREVIEW_UNAVAILABLE_MESSAGE, exception);
        } catch (IOException exception) {
            throw new IOException(PREVIEW_UNAVAILABLE_MESSAGE, exception);
        }
    }

    private List<String> buildCommand(Path inputFile, Path tempDirectory) {
        return List.of(
                sofficeCommand,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                tempDirectory.toString(),
                inputFile.toString());
    }

    private void deleteRecursively(Path rootPath) {
        try (var paths = Files.walk(rootPath)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for preview temp files.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for preview temp files.
        }
    }

    private static CommandRunner defaultCommandRunner() {
        return (command, workingDirectory) -> {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor();
        };
    }

    @FunctionalInterface
    interface CommandRunner {

        int run(List<String> command, Path workingDirectory) throws IOException, InterruptedException;
    }
}
