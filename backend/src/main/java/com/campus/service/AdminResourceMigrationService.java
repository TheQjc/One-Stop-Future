package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.ResourceStatus;
import com.campus.config.MinioIntegrationProperties;
import com.campus.config.ResourceStorageProperties;
import com.campus.dto.AdminResourceMigrationRequest;
import com.campus.dto.AdminResourceMigrationResponse;
import com.campus.entity.ResourceItem;
import com.campus.mapper.ResourceItemMapper;
import com.campus.storage.HistoricalLocalResourceReader;
import com.campus.storage.MinioObjectOperations;

@Service
public class AdminResourceMigrationService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;
    private static final String OUTCOME_SUCCESS = "SUCCESS";
    private static final String OUTCOME_SKIPPED = "SKIPPED";
    private static final String OUTCOME_FAILED = "FAILED";
    private static final String MESSAGE_READY = "ready to migrate";
    private static final String MESSAGE_UPLOADED = "uploaded to minio";
    private static final String MESSAGE_LOCAL_NOT_FOUND = "local file not found";
    private static final String MESSAGE_OBJECT_EXISTS = "object already exists in minio";
    private static final String MESSAGE_OPEN_FAILED = "failed to open local file";
    private static final String MESSAGE_UPLOAD_FAILED = "failed to upload to minio";
    private static final String MESSAGE_MINIO_UNAVAILABLE = "minio migration unavailable";

    private final ResourceItemMapper resourceItemMapper;
    private final UserService userService;
    private final HistoricalLocalResourceReader historicalLocalResourceReader;
    private final MinioIntegrationProperties minioIntegrationProperties;
    private final Optional<MinioObjectOperations> minioObjectOperations;

    public AdminResourceMigrationService(ResourceItemMapper resourceItemMapper, UserService userService,
            ResourceStorageProperties resourceStorageProperties,
            MinioIntegrationProperties minioIntegrationProperties,
            Optional<MinioObjectOperations> minioObjectOperations) {
        this.resourceItemMapper = resourceItemMapper;
        this.userService = userService;
        this.historicalLocalResourceReader = new HistoricalLocalResourceReader(resourceStorageProperties.getLocalRoot());
        this.minioIntegrationProperties = minioIntegrationProperties;
        this.minioObjectOperations = minioObjectOperations;
    }

    public AdminResourceMigrationResponse migrateResources(String identity, AdminResourceMigrationRequest request) {
        userService.requireByIdentity(identity);
        NormalizedRequest normalized = normalize(request);
        MinioObjectOperations operations = requireMinioOperations();
        ensureBucketReady(operations);

        List<ResourceItem> candidates = loadCandidates(normalized);
        List<AdminResourceMigrationResponse.Item> items = candidates.stream()
                .map(resource -> migrateOne(resource, normalized, operations))
                .toList();

        return summarize(normalized, items);
    }

    private NormalizedRequest normalize(AdminResourceMigrationRequest request) {
        AdminResourceMigrationRequest safeRequest = request == null
                ? new AdminResourceMigrationRequest(null, null, null, null, null, null)
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

    private MinioObjectOperations requireMinioOperations() {
        if (!minioIntegrationProperties.isEnabled() || minioObjectOperations.isEmpty()
                || minioIntegrationProperties.getBucket() == null || minioIntegrationProperties.getBucket().isBlank()) {
            throw new BusinessException(500, MESSAGE_MINIO_UNAVAILABLE);
        }
        return minioObjectOperations.get();
    }

    private void ensureBucketReady(MinioObjectOperations operations) {
        try {
            if (!operations.bucketExists(minioIntegrationProperties.getBucket())) {
                operations.createBucket(minioIntegrationProperties.getBucket());
            }
        } catch (IOException exception) {
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

    private AdminResourceMigrationResponse.Item migrateOne(ResourceItem resource, NormalizedRequest normalized,
            MinioObjectOperations operations) {
        try {
            if (!historicalLocalResourceReader.exists(resource.getStorageKey())) {
                return item(resource, OUTCOME_SKIPPED, MESSAGE_LOCAL_NOT_FOUND);
            }
        } catch (IllegalArgumentException exception) {
            return item(resource, OUTCOME_SKIPPED, exception.getMessage());
        }

        if (normalized.onlyMissingInMinio()) {
            try {
                if (operations.objectExists(minioIntegrationProperties.getBucket(), resource.getStorageKey())) {
                    return item(resource, OUTCOME_SKIPPED, MESSAGE_OBJECT_EXISTS);
                }
            } catch (IOException exception) {
                return item(resource, OUTCOME_FAILED, MESSAGE_UPLOAD_FAILED);
            }
        }

        if (normalized.dryRun()) {
            return item(resource, OUTCOME_SUCCESS, MESSAGE_READY);
        }

        InputStream inputStream;
        try {
            inputStream = historicalLocalResourceReader.open(resource.getStorageKey());
        } catch (IllegalArgumentException exception) {
            return item(resource, OUTCOME_SKIPPED, exception.getMessage());
        } catch (IOException exception) {
            return item(resource, OUTCOME_FAILED, MESSAGE_OPEN_FAILED);
        }

        try (InputStream closable = inputStream) {
            operations.putObject(minioIntegrationProperties.getBucket(), resource.getStorageKey(), closable);
            return item(resource, OUTCOME_SUCCESS, MESSAGE_UPLOADED);
        } catch (IOException exception) {
            return item(resource, OUTCOME_FAILED, MESSAGE_UPLOAD_FAILED);
        }
    }

    private AdminResourceMigrationResponse.Item item(ResourceItem resource, String outcome, String message) {
        return new AdminResourceMigrationResponse.Item(
                resource.getId(),
                resource.getTitle(),
                resource.getStatus(),
                resource.getStorageKey(),
                outcome,
                message);
    }

    private AdminResourceMigrationResponse summarize(NormalizedRequest normalized,
            List<AdminResourceMigrationResponse.Item> items) {
        int successCount = (int) items.stream()
                .filter(item -> OUTCOME_SUCCESS.equals(item.outcome()))
                .count();
        int skippedCount = (int) items.stream()
                .filter(item -> OUTCOME_SKIPPED.equals(item.outcome()))
                .count();
        int failureCount = (int) items.stream()
                .filter(item -> OUTCOME_FAILED.equals(item.outcome()))
                .count();

        return new AdminResourceMigrationResponse(
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
