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
import com.campus.entity.JobApplication;

@Service
public class ApplicationSnapshotPreviewService {

    private static final DateTimeFormatter FINGERPRINT_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ResourcePreviewArtifactStorage artifactStorage;
    private final DocxPreviewGenerator docxPreviewGenerator;

    public ApplicationSnapshotPreviewService(
            ResourcePreviewArtifactStorage artifactStorage,
            DocxPreviewGenerator docxPreviewGenerator) {
        this.artifactStorage = Objects.requireNonNull(artifactStorage, "artifactStorage");
        this.docxPreviewGenerator = Objects.requireNonNull(docxPreviewGenerator, "docxPreviewGenerator");
    }

    public ResourcePreviewKind previewKindOf(String fileExt, String contentType) {
        if (isPdf(fileExt, contentType) || isDocx(fileExt, contentType)) {
            return ResourcePreviewKind.FILE;
        }
        return ResourcePreviewKind.NONE;
    }

    public ResourcePreviewKind previewKindOf(JobApplication application) {
        if (application == null) {
            return ResourcePreviewKind.NONE;
        }
        return previewKindOf(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot());
    }

    public boolean isPreviewAvailable(String fileExt, String contentType) {
        return previewKindOf(fileExt, contentType) != ResourcePreviewKind.NONE;
    }

    public String docxArtifactKeyOf(JobApplication application) {
        return "application/snapshot/docx/" + application.getId() + "/" + fingerprintOf(application) + ".pdf";
    }

    public PreviewFile preview(JobApplication application, SnapshotSourceSupplier sourceSupplier) {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier");

        if (isPdf(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot())) {
            try {
                return new PreviewFile(
                        safeFileName(application.getResumeFileNameSnapshot()),
                        safeContentType(application.getResumeContentTypeSnapshot()),
                        sourceSupplier.open());
            } catch (IOException | RuntimeException exception) {
                throw new BusinessException(500, "application resume preview unavailable");
            }
        }

        if (!isDocx(application.getResumeFileExtSnapshot(), application.getResumeContentTypeSnapshot())) {
            throw new BusinessException(400, "application resume preview only supports pdf or docx");
        }

        String artifactKey = docxArtifactKeyOf(application);
        Optional<InputStream> cachedArtifact = openArtifactIfPresent(artifactKey);
        if (cachedArtifact.isPresent()) {
            return new PreviewFile(previewFileName(application.getResumeFileNameSnapshot()), "application/pdf",
                    cachedArtifact.get());
        }

        try (InputStream sourceInputStream = sourceSupplier.open()) {
            byte[] pdfBytes = docxPreviewGenerator.generate(sourceInputStream);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
            return new PreviewFile(previewFileName(application.getResumeFileNameSnapshot()), "application/pdf",
                    new ByteArrayInputStream(pdfBytes));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(500, "application resume preview unavailable");
        }
    }

    String fingerprintOf(JobApplication application) {
        String source = safe(application.getResumeStorageKeySnapshot())
                + "|"
                + formatDateTime(application.getSubmittedAt())
                + "|"
                + safeNumber(application.getResumeFileSizeSnapshot());
        return sha256Hex(source);
    }

    private Optional<InputStream> openArtifactIfPresent(String artifactKey) {
        try {
            return Optional.of(artifactStorage.open(artifactKey));
        } catch (FileNotFoundException exception) {
            return Optional.empty();
        } catch (IOException exception) {
            throw new BusinessException(500, "application resume preview unavailable");
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

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "application-resume";
        }
        return value;
    }

    private String safeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "application/octet-stream";
        }
        return value;
    }

    private boolean isPdf(String fileExt, String contentType) {
        return "pdf".equalsIgnoreCase(fileExt)
                || "application/pdf".equalsIgnoreCase(contentType);
    }

    private boolean isDocx(String fileExt, String contentType) {
        return "docx".equalsIgnoreCase(fileExt)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        .equalsIgnoreCase(contentType);
    }

    public record PreviewFile(String fileName, String contentType, InputStream inputStream) {
    }

    @FunctionalInterface
    public interface SnapshotSourceSupplier {
        InputStream open() throws IOException;
    }
}
