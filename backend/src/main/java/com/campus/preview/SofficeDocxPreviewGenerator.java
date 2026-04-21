package com.campus.preview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

        Path tempDirectory = Files.createTempDirectory("docx-preview-");
        try {
            Path inputFile = tempDirectory.resolve("preview.docx");
            Path outputFile = tempDirectory.resolve("preview.pdf");
            Files.copy(docxInputStream, inputFile, StandardCopyOption.REPLACE_EXISTING);

            int exitCode = runConverter(inputFile, tempDirectory);
            if (exitCode != 0 || !Files.exists(outputFile)) {
                throw new IOException(PREVIEW_UNAVAILABLE_MESSAGE);
            }

            return Files.readAllBytes(outputFile);
        } finally {
            deleteRecursively(tempDirectory);
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
