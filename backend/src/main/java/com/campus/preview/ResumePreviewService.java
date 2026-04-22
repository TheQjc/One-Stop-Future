package com.campus.preview;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.ResourcePreviewKind;
import com.campus.entity.Resume;

@Service
public class ResumePreviewService {

    private static final DateTimeFormatter FINGERPRINT_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ResourcePreviewArtifactStorage artifactStorage;
    private final DocxPreviewGenerator docxPreviewGenerator;

    public ResumePreviewService(
            ResourcePreviewArtifactStorage artifactStorage,
            DocxPreviewGenerator docxPreviewGenerator) {
        this.artifactStorage = Objects.requireNonNull(artifactStorage, "artifactStorage");
        this.docxPreviewGenerator = Objects.requireNonNull(docxPreviewGenerator, "docxPreviewGenerator");
    }

    public ResourcePreviewKind previewKindOf(Resume resume) {
        if (isPdf(resume) || isDocx(resume)) {
            return ResourcePreviewKind.FILE;
        }
        return ResourcePreviewKind.NONE;
    }

    public boolean isPreviewAvailable(Resume resume) {
        return previewKindOf(resume) != ResourcePreviewKind.NONE;
    }

    public String docxArtifactKeyOf(Resume resume) {
        return "resume/docx/" + resume.getId() + "/" + fingerprintOf(resume) + ".pdf";
    }

    public Optional<PreviewArtifactTarget> previewArtifactTargetOf(Resume resume) {
        if (resume == null || !isDocx(resume)) {
            return Optional.empty();
        }
        return Optional.of(new PreviewArtifactTarget("DOCX", docxArtifactKeyOf(resume)));
    }

    public PreviewFile previewDocx(Resume resume, DocxSourceSupplier sourceSupplier) {
        String artifactKey = docxArtifactKeyOf(resume);
        Optional<InputStream> cachedArtifact = openArtifactIfPresent(artifactKey);
        if (cachedArtifact.isPresent()) {
            return new PreviewFile(previewFileName(resume.getFileName()), "application/pdf", cachedArtifact.get());
        }

        try (InputStream sourceInputStream = sourceSupplier.open()) {
            byte[] pdfBytes = docxPreviewGenerator.generate(sourceInputStream);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
            return new PreviewFile(previewFileName(resume.getFileName()), "application/pdf",
                    new ByteArrayInputStream(pdfBytes));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(500, "resume preview unavailable");
        }
    }

    public void delete(String artifactKey) throws IOException {
        artifactStorage.delete(artifactKey);
    }

    String fingerprintOf(Resume resume) {
        String source = safe(resume.getStorageKey())
                + "|"
                + formatDateTime(resume.getUpdatedAt())
                + "|"
                + safeNumber(resume.getFileSize());
        return sha256Hex(source);
    }

    private Optional<InputStream> openArtifactIfPresent(String artifactKey) {
        try {
            return Optional.of(artifactStorage.open(artifactKey));
        } catch (FileNotFoundException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new BusinessException(500, "resume preview unavailable");
        }
    }

    private String previewFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "preview.pdf";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return fileName + ".pdf";
        }
        return fileName.substring(0, lastDot) + ".pdf";
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(FINGERPRINT_TIME_FORMATTER);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String safeNumber(Long value) {
        if (value == null) {
            return "0";
        }
        return Long.toString(value);
    }

    private boolean isPdf(Resume resume) {
        return resume != null
                && ("pdf".equalsIgnoreCase(resume.getFileExt())
                        || "application/pdf".equalsIgnoreCase(resume.getContentType()));
    }

    private boolean isDocx(Resume resume) {
        return resume != null
                && ("docx".equalsIgnoreCase(resume.getFileExt())
                        || "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                .equalsIgnoreCase(resume.getContentType()));
    }

    public record PreviewFile(String fileName, String contentType, InputStream inputStream) {
    }

    public record PreviewArtifactTarget(String previewType, String artifactKey) {
    }

    @FunctionalInterface
    public interface DocxSourceSupplier {
        InputStream open() throws IOException;
    }
}
