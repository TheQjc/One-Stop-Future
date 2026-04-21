package com.campus.preview;

import java.io.ByteArrayInputStream;
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
import com.campus.dto.ResourceZipPreviewResponse;
import com.campus.entity.ResourceItem;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResourcePreviewService {

    private static final DateTimeFormatter FINGERPRINT_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ResourcePreviewArtifactStorage artifactStorage;
    private final ObjectMapper objectMapper;
    private final PptxPreviewGenerator pptxPreviewGenerator;
    private final DocxPreviewGenerator docxPreviewGenerator;
    private final ZipPreviewGenerator zipPreviewGenerator;

    public ResourcePreviewService(ResourcePreviewArtifactStorage artifactStorage, ObjectMapper objectMapper,
            PptxPreviewGenerator pptxPreviewGenerator, DocxPreviewGenerator docxPreviewGenerator,
            ZipPreviewGenerator zipPreviewGenerator) {
        this.artifactStorage = Objects.requireNonNull(artifactStorage, "artifactStorage");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.pptxPreviewGenerator = Objects.requireNonNull(pptxPreviewGenerator, "pptxPreviewGenerator");
        this.docxPreviewGenerator = Objects.requireNonNull(docxPreviewGenerator, "docxPreviewGenerator");
        this.zipPreviewGenerator = Objects.requireNonNull(zipPreviewGenerator, "zipPreviewGenerator");
    }

    public String pptxArtifactKeyOf(ResourceItem resource) {
        return generatedPdfArtifactKeyOf("pptx", resource);
    }

    public String docxArtifactKeyOf(ResourceItem resource) {
        return generatedPdfArtifactKeyOf("docx", resource);
    }

    public String zipArtifactKeyOf(ResourceItem resource) {
        return "zip/" + resource.getId() + "/" + fingerprintOf(resource) + ".json";
    }

    public Optional<PreviewArtifactTarget> previewArtifactTargetOf(ResourceItem resource) {
        if (resource == null) {
            return Optional.empty();
        }
        if (isPptx(resource)) {
            return Optional.of(new PreviewArtifactTarget("PPTX", pptxArtifactKeyOf(resource)));
        }
        if (isDocx(resource)) {
            return Optional.of(new PreviewArtifactTarget("DOCX", docxArtifactKeyOf(resource)));
        }
        if (isZip(resource)) {
            return Optional.of(new PreviewArtifactTarget("ZIP", zipArtifactKeyOf(resource)));
        }
        return Optional.empty();
    }

    public PreviewFile previewFile(ResourceItem resource, PptxSourceSupplier pptxSourceSupplier) {
        return previewGeneratedPdf(pptxArtifactKeyOf(resource), resource, pptxSourceSupplier::open,
                pptxPreviewGenerator::generate, "pptx preview unavailable");
    }

    public PreviewFile previewDocx(ResourceItem resource, DocxSourceSupplier docxSourceSupplier) {
        return previewGeneratedPdf(docxArtifactKeyOf(resource), resource, docxSourceSupplier::open,
                docxPreviewGenerator::generate, "docx preview unavailable");
    }

    public ResourceZipPreviewResponse previewZip(ResourceItem resource, ZipSourceSupplier zipSourceSupplier) {
        String artifactKey = zipArtifactKeyOf(resource);
        if (artifactExists(artifactKey, "zip preview unavailable")) {
            return readZipArtifact(artifactKey);
        }

        try (InputStream zipInputStream = zipSourceSupplier.open()) {
            ResourceZipPreviewResponse generated = zipPreviewGenerator.generate(resource.getId(), resource.getFileName(),
                    zipInputStream);
            byte[] serialized = objectMapper.writeValueAsBytes(generated);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(serialized));
            return generated;
        } catch (IOException exception) {
            throw new BusinessException(500, "zip preview unavailable");
        }
    }

    String fingerprintOf(ResourceItem resource) {
        String source = safe(resource.getStorageKey())
                + "|"
                + formatDateTime(resource.getUpdatedAt())
                + "|"
                + safeNumber(resource.getFileSize());
        return sha256Hex(source);
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

    private ResourceZipPreviewResponse readZipArtifact(String artifactKey) {
        try (InputStream artifactInputStream = artifactStorage.open(artifactKey)) {
            return objectMapper.readValue(artifactInputStream, ResourceZipPreviewResponse.class);
        } catch (IOException exception) {
            throw new BusinessException(500, "zip preview unavailable");
        }
    }

    private PreviewFile previewGeneratedPdf(String artifactKey, ResourceItem resource,
            GeneratedPdfSourceSupplier sourceSupplier, GeneratedPdfGenerator generator, String unavailableMessage) {
        if (artifactExists(artifactKey, unavailableMessage)) {
            return openGeneratedPdfArtifact(resource, artifactKey, unavailableMessage);
        }

        try (InputStream sourceInputStream = sourceSupplier.open()) {
            byte[] pdfBytes = generator.generate(sourceInputStream);
            artifactStorage.write(artifactKey, new ByteArrayInputStream(pdfBytes));
            return new PreviewFile(previewFileName(resource.getFileName()), "application/pdf",
                    new ByteArrayInputStream(pdfBytes));
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException(500, unavailableMessage);
        }
    }

    private String generatedPdfArtifactKeyOf(String previewType, ResourceItem resource) {
        return previewType + "/" + resource.getId() + "/" + fingerprintOf(resource) + ".pdf";
    }

    private boolean artifactExists(String artifactKey, String unavailableMessage) {
        try {
            return artifactStorage.exists(artifactKey);
        } catch (IOException exception) {
            throw new BusinessException(500, unavailableMessage);
        }
    }

    private PreviewFile openGeneratedPdfArtifact(ResourceItem resource, String artifactKey, String unavailableMessage) {
        try {
            return new PreviewFile(previewFileName(resource.getFileName()), "application/pdf",
                    artifactStorage.open(artifactKey));
        } catch (IOException exception) {
            throw new BusinessException(500, unavailableMessage);
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

    private boolean isPptx(ResourceItem resource) {
        return "pptx".equalsIgnoreCase(resource.getFileExt())
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                        .equalsIgnoreCase(resource.getContentType());
    }

    private boolean isDocx(ResourceItem resource) {
        return "docx".equalsIgnoreCase(resource.getFileExt())
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        .equalsIgnoreCase(resource.getContentType());
    }

    private boolean isZip(ResourceItem resource) {
        return "zip".equalsIgnoreCase(resource.getFileExt())
                || "application/zip".equalsIgnoreCase(resource.getContentType());
    }

    public record PreviewFile(String fileName, String contentType, InputStream inputStream) {
    }

    public record PreviewArtifactTarget(String previewType, String artifactKey) {
    }

    @FunctionalInterface
    public interface PptxSourceSupplier {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    public interface DocxSourceSupplier {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    public interface ZipSourceSupplier {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    private interface GeneratedPdfSourceSupplier {
        InputStream open() throws IOException;
    }

    @FunctionalInterface
    private interface GeneratedPdfGenerator {
        byte[] generate(InputStream inputStream) throws IOException;
    }
}
