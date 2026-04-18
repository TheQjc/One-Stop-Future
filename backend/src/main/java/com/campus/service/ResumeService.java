package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.dto.ResumeListResponse;
import com.campus.dto.ResumeRecordResponse;
import com.campus.entity.Resume;
import com.campus.entity.User;
import com.campus.mapper.ResumeMapper;
import com.campus.storage.ResourceFileStorage;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final ResumeMapper resumeMapper;
    private final UserService userService;
    private final ResourceFileStorage resourceFileStorage;
    private final MultipartProperties multipartProperties;

    public ResumeService(ResumeMapper resumeMapper, UserService userService, ResourceFileStorage resourceFileStorage,
            MultipartProperties multipartProperties) {
        this.resumeMapper = resumeMapper;
        this.userService = userService;
        this.resourceFileStorage = resourceFileStorage;
        this.multipartProperties = multipartProperties;
    }

    @Transactional
    public ResumeRecordResponse upload(String identity, String title, MultipartFile file) {
        User viewer = userService.requireByIdentity(identity);
        String normalizedTitle = requireText(title, "title");
        ValidatedResumeFile validatedFile = validateFile(file);
        String storageKey = storeValidatedFile(validatedFile, file);

        Resume resume = new Resume();
        resume.setUserId(viewer.getId());
        resume.setTitle(normalizedTitle);
        resume.setFileName(validatedFile.originalFilename());
        resume.setFileExt(validatedFile.extension());
        resume.setContentType(validatedFile.contentType());
        resume.setFileSize(validatedFile.size());
        resume.setStorageKey(storageKey);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);
        return toRecord(resume);
    }

    public ResumeListResponse listMine(String identity) {
        User viewer = userService.requireByIdentity(identity);
        List<ResumeRecordResponse> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, viewer.getId())
                .orderByDesc(Resume::getCreatedAt)
                .orderByDesc(Resume::getId))
                .stream()
                .map(this::toRecord)
                .toList();
        return new ResumeListResponse(resumes.size(), resumes);
    }

    public DownloadedResume download(String identity, Long resumeId) {
        User viewer = userService.requireByIdentity(identity);
        Resume resume = requireOwnedResume(viewer.getId(), resumeId);
        return new DownloadedResume(resume.getFileName(), resume.getContentType(), openResumeFile(resume));
    }

    @Transactional
    public void delete(String identity, Long resumeId) {
        User viewer = userService.requireByIdentity(identity);
        Resume resume = requireOwnedResume(viewer.getId(), resumeId);
        String storageKey = resume.getStorageKey();
        resumeMapper.deleteById(resumeId);
        tryDeleteStoredFile(storageKey);
    }

    private Resume requireOwnedResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getUserId() == null || !resume.getUserId().equals(userId)) {
            throw new BusinessException(404, "resume not found");
        }
        return resume;
    }

    private ResumeRecordResponse toRecord(Resume resume) {
        return new ResumeRecordResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getFileName(),
                resume.getFileExt(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
    }

    private InputStream openResumeFile(Resume resume) {
        try {
            if (!resourceFileStorage.exists(resume.getStorageKey())) {
                throw new BusinessException(500, "resume file unavailable");
            }
            return resourceFileStorage.open(resume.getStorageKey());
        } catch (IOException exception) {
            throw new BusinessException(500, "resume file unavailable");
        }
    }

    private String storeValidatedFile(ValidatedResumeFile validatedFile, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return resourceFileStorage.store(validatedFile.originalFilename(), inputStream);
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to store resume file");
        }
    }

    private void tryDeleteStoredFile(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            if (!resourceFileStorage.exists(storageKey)) {
                return;
            }
            resourceFileStorage.delete(storageKey);
        } catch (IOException exception) {
            log.warn("Failed to delete resume file: {}", storageKey, exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, fieldName + " is required");
        }
        return value.trim();
    }

    private ValidatedResumeFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "file is required");
        }

        validateFileSize(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(400, "resume file name is required");
        }

        String normalizedFilename = originalFilename.trim();
        if (normalizedFilename.isEmpty()) {
            throw new BusinessException(400, "resume file name is required");
        }

        String extension = extractExtension(normalizedFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "unsupported resume file type");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new ValidatedResumeFile(normalizedFilename, extension, contentType, file.getSize());
    }

    private void validateFileSize(MultipartFile file) {
        DataSize configuredLimit = multipartProperties.getMaxFileSize();
        long maxBytes = configuredLimit == null ? Long.MAX_VALUE : configuredLimit.toBytes();
        if (maxBytes < 0) {
            maxBytes = Long.MAX_VALUE;
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, "resume file is too large");
        }
    }

    private String extractExtension(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            throw new BusinessException(400, "unsupported resume file type");
        }
        return originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    public record DownloadedResume(String fileName, String contentType, InputStream inputStream) {
    }

    private record ValidatedResumeFile(String originalFilename, String extension, String contentType, long size) {
    }
}
