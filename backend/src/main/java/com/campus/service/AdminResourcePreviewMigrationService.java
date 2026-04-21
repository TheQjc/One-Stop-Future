package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.ResourceStatus;
import com.campus.config.MinioIntegrationProperties;
import com.campus.config.ResourcePreviewProperties;
import com.campus.dto.AdminResourcePreviewMigrationRequest;
import com.campus.dto.AdminResourcePreviewMigrationResponse;
import com.campus.entity.ResourceItem;
import com.campus.mapper.ResourceItemMapper;
import com.campus.preview.HistoricalLocalResourcePreviewArtifactReader;
import com.campus.preview.MinioResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewService;
import com.campus.storage.MinioObjectOperations;

@Service
public class AdminResourcePreviewMigrationService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private static final String PREVIEW_TYPE_NONE = "NONE";

    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_SKIPPED = "SKIPPED";
    private static final String OUTCOME_FAILED = "FAILED";

    private static final String MESSAGE_READY = "ready to migrate";
    private static final String MESSAGE_UPLOADED = "uploaded to minio";
    private static final String MESSAGE_PREVIEW_NOT_SUPPORTED = "preview not supported";
    private static final String MESSAGE_LOCAL_NOT_FOUND = "local preview artifact not found";
    private static final String MESSAGE_OBJECT_EXISTS = "preview artifact already exists in minio";
    private static final String MESSAGE_OPEN_FAILED = "failed to open local preview artifact";
    private static final String MESSAGE_UPLOAD_FAILED = "failed to upload preview artifact";
    private static final String MESSAGE_MINIO_UNAVAILABLE = "minio preview migration unavailable";

    private final ResourceItemMapper resourceItemMapper;
    private final UserService userService;
    private final ResourcePreviewService resourcePreviewService;
    private final HistoricalLocalResourcePreviewArtifactReader localArtifactReader;
    private final MinioIntegrationProperties minioIntegrationProperties;
    private final ResourcePreviewProperties resourcePreviewProperties;
    private final Optional<MinioObjectOperations> minioObjectOperations;

    public AdminResourcePreviewMigrationService(ResourceItemMapper resourceItemMapper, UserService userService,
            ResourcePreviewService resourcePreviewService, ResourcePreviewProperties resourcePreviewProperties,
            MinioIntegrationProperties minioIntegrationProperties, Optional<MinioObjectOperations> minioObjectOperations) {
        this.resourceItemMapper = Objects.requireNonNull(resourceItemMapper, "resourceItemMapper");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.resourcePreviewService = Objects.requireNonNull(resourcePreviewService, "resourcePreviewService");
        this.resourcePreviewProperties = Objects.requireNonNull(resourcePreviewProperties, "resourcePreviewProperties");
        this.localArtifactReader = new HistoricalLocalResourcePreviewArtifactReader(resourcePreviewProperties.getLocalRoot());
        this.minioIntegrationProperties = Objects.requireNonNull(minioIntegrationProperties, "minioIntegrationProperties");
        this.minioObjectOperations = Objects.requireNonNull(minioObjectOperations, "minioObjectOperations");
    }

    public AdminResourcePreviewMigrationResponse migratePreviewArtifacts(String identity,
            AdminResourcePreviewMigrationRequest request) {
        userService.requireByIdentity(identity);
        NormalizedRequest normalized = normalize(request);
        MinioResourcePreviewArtifactStorage targetStorage = requireTargetStorage();

        List<ResourceItem> candidates = loadCandidates(normalized);
        List<AdminResourcePreviewMigrationResponse.Item> items = candidates.stream()
                .map(resource -> migrateOne(resource, normalized, targetStorage))
                .toList();

        return summarize(normalized, items);
    }

    private NormalizedRequest normalize(AdminResourcePreviewMigrationRequest request) {
        AdminResourcePreviewMigrationRequest safeRequest = request == null
                ? new AdminResourcePreviewMigrationRequest(null, null, null, null, null, null)
                : request;

        boolean dryRun = safeRequest.dryRun() == null ? true : safeRequest.dryRun();
        boolean onlyMissingInMinio = safeRequest.onlyMissingInMinio() == null ? true : safeRequest.onlyMissingInMinio();
        int limit = normalizeLimit(safeRequest.limit());
        List<String> statuses = normalizeStatuses(safeRequest.statuses());
        List<Long> resourceIds = normalizeResourceIds(safeRequest.resourceIds());
        String keyword = normalizeKeyword(safeRequest.keyword());

        return new NormalizedRequest(dryRun, statuses, resourceIds, keyword, onlyMissingInMinio, limit);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        if (requestedLimit < 1 || requestedLimit > MAX_LIMIT) {
            throw new BusinessException(400, "invalid limit");
        }
        return requestedLimit;
    }

    private List<String> normalizeStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return statuses.stream()
                .map(this::normalizeStatus)
                .distinct()
                .toList();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(400, "invalid resource status");
        }
        try {
            return ResourceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid resource status");
        }
    }

    private List<Long> normalizeResourceIds(List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        return resourceIds.stream()
                .map(this::normalizeResourceId)
                .distinct()
                .toList();
    }

    private Long normalizeResourceId(Long resourceId) {
        if (resourceId == null || resourceId <= 0) {
            throw new BusinessException(400, "invalid resource id");
        }
        return resourceId;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private MinioResourcePreviewArtifactStorage requireTargetStorage() {
        if (!minioIntegrationProperties.isEnabled() || minioObjectOperations.isEmpty()
                || minioIntegrationProperties.getBucket() == null || minioIntegrationProperties.getBucket().isBlank()) {
            throw new BusinessException(500, MESSAGE_MINIO_UNAVAILABLE);
        }

        try {
            return new MinioResourcePreviewArtifactStorage(
                    minioIntegrationProperties.getBucket(),
                    resourcePreviewProperties.getMinioPrefix(),
                    minioObjectOperations.get());
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(500, MESSAGE_MINIO_UNAVAILABLE);
        }
    }

    private List<ResourceItem> loadCandidates(NormalizedRequest normalized) {
        LambdaQueryWrapper<ResourceItem> wrapper = new LambdaQueryWrapper<>();

        if (!normalized.statuses().isEmpty()) {
            wrapper.in(ResourceItem::getStatus, normalized.statuses());
        }
        if (!normalized.resourceIds().isEmpty()) {
            wrapper.in(ResourceItem::getId, normalized.resourceIds());
        }
        if (normalized.keyword() != null) {
            String likeKeyword = "%" + normalized.keyword().toLowerCase(Locale.ROOT) + "%";
            wrapper.and(query -> query
                    .apply("LOWER(title) LIKE {0}", likeKeyword)
                    .or()
                    .apply("LOWER(summary) LIKE {0}", likeKeyword)
                    .or()
                    .apply("LOWER(file_name) LIKE {0}", likeKeyword));
        }

        wrapper.orderByAsc(ResourceItem::getId)
                .last("LIMIT " + normalized.limit());

        return resourceItemMapper.selectList(wrapper);
    }

    private AdminResourcePreviewMigrationResponse.Item migrateOne(ResourceItem resource, NormalizedRequest normalized,
            MinioResourcePreviewArtifactStorage targetStorage) {
        Optional<ResourcePreviewService.PreviewArtifactTarget> optionalTarget = resourcePreviewService.previewArtifactTargetOf(resource);
        if (optionalTarget.isEmpty()) {
            return item(resource, PREVIEW_TYPE_NONE, null, OUTCOME_SKIPPED, MESSAGE_PREVIEW_NOT_SUPPORTED);
        }

        ResourcePreviewService.PreviewArtifactTarget target = optionalTarget.get();

        try {
            if (!localArtifactReader.exists(target.artifactKey())) {
                return item(resource, target.previewType(), target.artifactKey(), OUTCOME_SKIPPED, MESSAGE_LOCAL_NOT_FOUND);
            }
        } catch (IllegalArgumentException exception) {
            return item(resource, target.previewType(), target.artifactKey(), OUTCOME_FAILED, MESSAGE_OPEN_FAILED);
        }

        if (normalized.onlyMissingInMinio()) {
            try {
                if (targetStorage.exists(target.artifactKey())) {
                    return item(resource, target.previewType(), target.artifactKey(), OUTCOME_SKIPPED, MESSAGE_OBJECT_EXISTS);
                }
            } catch (IOException | IllegalArgumentException exception) {
                return item(resource, target.previewType(), target.artifactKey(), OUTCOME_FAILED, MESSAGE_UPLOAD_FAILED);
            }
        }

        if (normalized.dryRun()) {
            return item(resource, target.previewType(), target.artifactKey(), OUTCOME_SUCCESS, MESSAGE_READY);
        }

        try (InputStream inputStream = localArtifactReader.open(target.artifactKey())) {
            try {
                targetStorage.write(target.artifactKey(), inputStream);
                return item(resource, target.previewType(), target.artifactKey(), OUTCOME_SUCCESS, MESSAGE_UPLOADED);
            } catch (IOException | IllegalArgumentException exception) {
                return item(resource, target.previewType(), target.artifactKey(), OUTCOME_FAILED, MESSAGE_UPLOAD_FAILED);
            }
        } catch (IOException | IllegalArgumentException exception) {
            return item(resource, target.previewType(), target.artifactKey(), OUTCOME_FAILED, MESSAGE_OPEN_FAILED);
        }
    }

    private AdminResourcePreviewMigrationResponse.Item item(ResourceItem resource, String previewType, String artifactKey,
            String outcome, String message) {
        return new AdminResourcePreviewMigrationResponse.Item(
                resource.getId(),
                resource.getTitle(),
                resource.getStatus(),
                previewType,
                artifactKey,
                outcome,
                message);
    }

    private AdminResourcePreviewMigrationResponse summarize(NormalizedRequest normalized,
            List<AdminResourcePreviewMigrationResponse.Item> items) {
        int successCount = (int) items.stream()
                .filter(item -> OUTCOME_SUCCESS.equals(item.outcome()))
                .count();
        int skippedCount = (int) items.stream()
                .filter(item -> OUTCOME_SKIPPED.equals(item.outcome()))
                .count();
        int failureCount = (int) items.stream()
                .filter(item -> OUTCOME_FAILED.equals(item.outcome()))
                .count();

        return new AdminResourcePreviewMigrationResponse(
                normalized.dryRun(),
                normalized.limit(),
                items.size(),
                items.size(),
                successCount,
                skippedCount,
                failureCount,
                items);
    }

    private record NormalizedRequest(
            boolean dryRun,
            List<String> statuses,
            List<Long> resourceIds,
            String keyword,
            boolean onlyMissingInMinio,
            int limit) {
    }
}
