package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.campus.common.BusinessException;
import com.campus.common.ResourceCategory;
import com.campus.config.ResourceUploadProperties;
import com.campus.dto.ResourceChunkUploadInitRequest;
import com.campus.dto.ResourceChunkUploadStatusResponse;
import com.campus.dto.ResourceDetailResponse;
import com.campus.entity.User;
import com.campus.service.ResourceService.ResourceUploadMetadata;
import com.campus.service.ResourceService.StoredResourceFile;
import com.campus.storage.ResourceFileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResourceChunkUploadService {

    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "pptx", "zip");

    private final UserService userService;
    private final ResourceService resourceService;
    private final ResourceFileStorage resourceFileStorage;
    private final ObjectMapper objectMapper;
    private final Path sessionRoot;
    private final int defaultChunkSizeBytes;
    private final int maxChunkSizeBytes;

    public ResourceChunkUploadService(UserService userService, ResourceService resourceService,
            ResourceFileStorage resourceFileStorage, ObjectMapper objectMapper, ResourceUploadProperties properties) {
        this.userService = userService;
        this.resourceService = resourceService;
        this.resourceFileStorage = resourceFileStorage;
        this.objectMapper = objectMapper;
        this.sessionRoot = Path.of(properties.getChunkRoot()).toAbsolutePath().normalize();
        this.defaultChunkSizeBytes = properties.getDefaultChunkSizeBytes();
        this.maxChunkSizeBytes = properties.getMaxChunkSizeBytes();
        try {
            Files.createDirectories(sessionRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize resource upload session storage", exception);
        }
    }

    public ResourceChunkUploadStatusResponse initiate(String identity, ResourceChunkUploadInitRequest request) {
        User uploader = userService.requireByIdentity(identity);
        if (request == null) {
            throw new BusinessException(400, "invalid request");
        }

        String title = requireText(request.title(), "title");
        String category = normalizeRequiredCategory(request.category());
        String summary = requireText(request.summary(), "summary");
        String description = normalizeOptional(request.description());
        ValidatedUploadFile file = validateFileMetadata(request.fileName(), request.contentType(), request.fileSize());
        int chunkSize = normalizeChunkSize(request.chunkSize());
        int totalChunks = totalChunks(file.size(), chunkSize);
        String uploadId = UUID.randomUUID().toString();

        UploadSessionMetadata metadata = new UploadSessionMetadata(
                uploadId,
                uploader.getId(),
                title,
                category,
                summary,
                description,
                file.originalFilename(),
                file.extension(),
                file.contentType(),
                file.size(),
                chunkSize,
                totalChunks,
                LocalDateTime.now());
        writeMetadata(metadata);
        return statusOf(metadata);
    }

    public ResourceChunkUploadStatusResponse status(String identity, String uploadId) {
        User uploader = userService.requireByIdentity(identity);
        UploadSessionMetadata metadata = requireOwnedSession(uploadId, uploader);
        return statusOf(metadata);
    }

    public ResourceChunkUploadStatusResponse uploadChunk(String identity, String uploadId, int chunkIndex,
            MultipartFile chunk) {
        User uploader = userService.requireByIdentity(identity);
        UploadSessionMetadata metadata = requireOwnedSession(uploadId, uploader);
        validateChunk(metadata, chunkIndex, chunk);

        Path chunkPath = chunkPath(metadata.uploadId(), chunkIndex);
        Path tempPath = chunkPath.resolveSibling(chunkPath.getFileName() + ".tmp");
        try (InputStream inputStream = chunk.getInputStream()) {
            Files.createDirectories(chunkPath.getParent());
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            moveReplacing(tempPath, chunkPath);
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to store resource chunk");
        }
        return statusOf(metadata);
    }

    public ResourceDetailResponse complete(String identity, String uploadId) {
        User uploader = userService.requireByIdentity(identity);
        UploadSessionMetadata metadata = requireOwnedSession(uploadId, uploader);
        List<Integer> missingChunks = missingChunks(metadata);
        if (!missingChunks.isEmpty()) {
            throw new BusinessException(400, "resource chunks are incomplete");
        }

        Path assembledPath = sessionPath(metadata.uploadId()).resolve("assembled.bin");
        try {
            assembleChunks(metadata, assembledPath);
            if (Files.size(assembledPath) != metadata.fileSize()) {
                throw new BusinessException(400, "assembled resource size mismatch");
            }
            String storageKey;
            try (InputStream inputStream = Files.newInputStream(assembledPath)) {
                storageKey = resourceFileStorage.store(metadata.fileName(), inputStream);
            }
            ResourceDetailResponse resource = resourceService.createStoredResource(
                    identity,
                    new ResourceUploadMetadata(metadata.title(), metadata.category(), metadata.summary(),
                            metadata.description()),
                    new StoredResourceFile(metadata.fileName(), metadata.fileExt(), metadata.contentType(),
                            metadata.fileSize(), storageKey));
            deleteSessionDirectory(metadata.uploadId());
            return resource;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to complete resource upload");
        }
    }

    public void abort(String identity, String uploadId) {
        User uploader = userService.requireByIdentity(identity);
        UploadSessionMetadata metadata = requireOwnedSession(uploadId, uploader);
        deleteSessionDirectory(metadata.uploadId());
    }

    private void assembleChunks(UploadSessionMetadata metadata, Path assembledPath) throws IOException {
        Files.createDirectories(assembledPath.getParent());
        Path tempPath = assembledPath.resolveSibling("assembled.tmp");
        try (OutputStream outputStream = Files.newOutputStream(tempPath)) {
            for (int chunkIndex = 0; chunkIndex < metadata.totalChunks(); chunkIndex++) {
                Files.copy(chunkPath(metadata.uploadId(), chunkIndex), outputStream);
            }
        }
        moveReplacing(tempPath, assembledPath);
    }

    private ResourceChunkUploadStatusResponse statusOf(UploadSessionMetadata metadata) {
        List<Integer> uploadedChunks = uploadedChunks(metadata);
        long uploadedBytes = uploadedChunks.stream()
                .mapToLong(chunkIndex -> sizeOfChunk(metadata.uploadId(), chunkIndex))
                .sum();
        return new ResourceChunkUploadStatusResponse(
                metadata.uploadId(),
                metadata.fileName(),
                metadata.fileSize(),
                metadata.chunkSize(),
                metadata.totalChunks(),
                uploadedChunks,
                uploadedBytes,
                uploadedChunks.size() == metadata.totalChunks());
    }

    private UploadSessionMetadata requireOwnedSession(String uploadId, User uploader) {
        UploadSessionMetadata metadata = readMetadata(uploadId);
        if (!metadata.uploaderId().equals(uploader.getId())) {
            throw new BusinessException(404, "upload session not found");
        }
        return metadata;
    }

    private void validateChunk(UploadSessionMetadata metadata, int chunkIndex, MultipartFile chunk) {
        if (chunkIndex < 0 || chunkIndex >= metadata.totalChunks()) {
            throw new BusinessException(400, "invalid chunk index");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new BusinessException(400, "chunk is required");
        }
        long expectedSize = expectedChunkSize(metadata, chunkIndex);
        if (chunk.getSize() != expectedSize) {
            throw new BusinessException(400, "invalid chunk size");
        }
    }

    private long expectedChunkSize(UploadSessionMetadata metadata, int chunkIndex) {
        if (chunkIndex == metadata.totalChunks() - 1) {
            return metadata.fileSize() - ((long) metadata.chunkSize() * chunkIndex);
        }
        return metadata.chunkSize();
    }

    private List<Integer> uploadedChunks(UploadSessionMetadata metadata) {
        return IntStream.range(0, metadata.totalChunks())
                .filter(chunkIndex -> Files.exists(chunkPath(metadata.uploadId(), chunkIndex)))
                .boxed()
                .toList();
    }

    private List<Integer> missingChunks(UploadSessionMetadata metadata) {
        return IntStream.range(0, metadata.totalChunks())
                .filter(chunkIndex -> !Files.exists(chunkPath(metadata.uploadId(), chunkIndex)))
                .boxed()
                .toList();
    }

    private long sizeOfChunk(String uploadId, int chunkIndex) {
        try {
            return Files.size(chunkPath(uploadId, chunkIndex));
        } catch (IOException exception) {
            return 0L;
        }
    }

    private void writeMetadata(UploadSessionMetadata metadata) {
        try {
            Path sessionPath = sessionPath(metadata.uploadId());
            Files.createDirectories(sessionPath);
            objectMapper.writeValue(sessionPath.resolve("session.json").toFile(), metadata);
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to create upload session");
        }
    }

    private UploadSessionMetadata readMetadata(String uploadId) {
        Path metadataPath = sessionPath(uploadId).resolve("session.json");
        if (!Files.exists(metadataPath)) {
            throw new BusinessException(404, "upload session not found");
        }
        try {
            return objectMapper.readValue(metadataPath.toFile(), UploadSessionMetadata.class);
        } catch (IOException exception) {
            throw new BusinessException(404, "upload session not found");
        }
    }

    private Path sessionPath(String uploadId) {
        String normalizedUploadId = normalizeUploadId(uploadId);
        return sessionRoot.resolve(normalizedUploadId).normalize();
    }

    private Path chunkPath(String uploadId, int chunkIndex) {
        return sessionPath(uploadId).resolve(chunkIndex + ".part");
    }

    private String normalizeUploadId(String uploadId) {
        try {
            return UUID.fromString(uploadId).toString();
        } catch (RuntimeException exception) {
            throw new BusinessException(404, "upload session not found");
        }
    }

    private void deleteSessionDirectory(String uploadId) {
        Path directory = sessionPath(uploadId);
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temporary upload chunks.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary upload chunks.
        }
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ValidatedUploadFile validateFileMetadata(String originalFilename, String contentType, Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(400, "file is required");
        }
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(400, "file is too large");
        }
        String normalizedFilename = requireText(originalFilename, "file name");
        String extension = extractExtension(normalizedFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "unsupported file type");
        }
        String normalizedContentType = normalizeOptional(contentType);
        if (normalizedContentType == null) {
            normalizedContentType = "application/octet-stream";
        }
        return new ValidatedUploadFile(normalizedFilename, extension, normalizedContentType, fileSize);
    }

    private int normalizeChunkSize(Integer requestedChunkSize) {
        int chunkSize = requestedChunkSize == null ? defaultChunkSizeBytes : requestedChunkSize;
        if (chunkSize <= 0) {
            throw new BusinessException(400, "chunk size is required");
        }
        if (chunkSize > maxChunkSizeBytes) {
            throw new BusinessException(400, "chunk size is too large");
        }
        return chunkSize;
    }

    private int totalChunks(long fileSize, int chunkSize) {
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(400, fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeRequiredCategory(String category) {
        String normalized = normalizeOptional(category);
        if (normalized == null) {
            throw new BusinessException(400, "category is required");
        }
        try {
            return ResourceCategory.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid resource category");
        }
    }

    private String extractExtension(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            throw new BusinessException(400, "unsupported file type");
        }
        return originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private record ValidatedUploadFile(String originalFilename, String extension, String contentType, long size) {
    }

    private record UploadSessionMetadata(
            String uploadId,
            Long uploaderId,
            String title,
            String category,
            String summary,
            String description,
            String fileName,
            String fileExt,
            String contentType,
            long fileSize,
            int chunkSize,
            int totalChunks,
            LocalDateTime createdAt) {
    }
}
