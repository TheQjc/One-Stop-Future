package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.FavoriteTargetType;
import com.campus.common.ResourceCategory;
import com.campus.common.ResourceStatus;
import com.campus.dto.MyResourceListResponse;
import com.campus.dto.ResourceDetailResponse;
import com.campus.dto.ResourceListResponse;
import com.campus.dto.SearchResponse;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.campus.entity.UserFavorite;
import com.campus.mapper.ResourceItemMapper;
import com.campus.mapper.UserFavoriteMapper;
import com.campus.storage.ResourceFileStorage;

@Service
public class ResourceService {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "pptx", "zip");

    private final ResourceItemMapper resourceItemMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserService userService;
    private final ResourceFileStorage resourceFileStorage;

    public ResourceService(ResourceItemMapper resourceItemMapper, UserFavoriteMapper userFavoriteMapper,
            UserService userService, ResourceFileStorage resourceFileStorage) {
        this.resourceItemMapper = resourceItemMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userService = userService;
        this.resourceFileStorage = resourceFileStorage;
    }

    public ResourceListResponse listResources(String keyword, String category, String identity) {
        User viewer = findViewer(identity);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedCategory = normalizeCategory(category);

        LambdaQueryWrapper<ResourceItem> wrapper = new LambdaQueryWrapper<ResourceItem>()
                .eq(ResourceItem::getStatus, ResourceStatus.PUBLISHED.name())
                .orderByDesc(ResourceItem::getPublishedAt)
                .orderByDesc(ResourceItem::getId)
                .last("LIMIT " + DEFAULT_LIST_LIMIT);

        if (normalizedKeyword != null) {
            wrapper.and(query -> query.like(ResourceItem::getTitle, normalizedKeyword)
                    .or()
                    .like(ResourceItem::getSummary, normalizedKeyword)
                    .or()
                    .like(ResourceItem::getDescription, normalizedKeyword));
        }
        if (normalizedCategory != null) {
            wrapper.eq(ResourceItem::getCategory, normalizedCategory);
        }

        List<ResourceListResponse.ResourceSummary> resources = resourceItemMapper.selectList(wrapper).stream()
                .map(resource -> toResourceSummary(resource, viewer))
                .toList();

        return new ResourceListResponse(normalizedKeyword, normalizedCategory, resources.size(), resources);
    }

    public ResourceDetailResponse getResourceDetail(Long resourceId, String identity) {
        User viewer = findViewer(identity);
        return toResourceDetail(requireVisibleResourceForViewer(resourceId, viewer), viewer);
    }

    public List<SearchResponse.SearchResultItem> searchPublishedResources(String keyword) {
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }

        return resourceItemMapper.selectList(new LambdaQueryWrapper<ResourceItem>()
                .eq(ResourceItem::getStatus, ResourceStatus.PUBLISHED.name())
                .orderByDesc(ResourceItem::getPublishedAt)
                .orderByDesc(ResourceItem::getId))
                .stream()
                .filter(resource -> containsKeyword(resource.getTitle(), normalizedKeyword)
                        || containsKeyword(resource.getSummary(), normalizedKeyword)
                        || containsKeyword(resource.getDescription(), normalizedKeyword))
                .map(resource -> new SearchResponse.SearchResultItem(
                        resource.getId(),
                        "RESOURCE",
                        resource.getTitle(),
                        resource.getSummary(),
                        uploaderNicknameOf(resource.getUploaderId()),
                        resource.getCategory(),
                        "/resources/" + resource.getId(),
                        resource.getPublishedAt()))
                .toList();
    }

    public List<ResourceItem> listPublishedDiscoverResources() {
        return resourceItemMapper.selectList(new LambdaQueryWrapper<ResourceItem>()
                .eq(ResourceItem::getStatus, ResourceStatus.PUBLISHED.name())
                .orderByDesc(ResourceItem::getPublishedAt)
                .orderByDesc(ResourceItem::getId));
    }

    public MyResourceListResponse listMyResources(String identity) {
        User viewer = userService.requireByIdentity(identity);
        List<MyResourceListResponse.ResourceItem> resources = resourceItemMapper.selectList(
                new LambdaQueryWrapper<ResourceItem>()
                        .eq(ResourceItem::getUploaderId, viewer.getId())
                        .orderByDesc(ResourceItem::getCreatedAt)
                        .orderByDesc(ResourceItem::getId)
                        .last("LIMIT " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(this::toMyResourceItem)
                .toList();
        return new MyResourceListResponse(resources.size(), resources);
    }

    public ResourceListResponse listMyResourceFavorites(String identity) {
        User viewer = userService.requireByIdentity(identity);
        List<ResourceListResponse.ResourceSummary> resources = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, viewer.getId())
                        .eq(UserFavorite::getTargetType, FavoriteTargetType.RESOURCE.name())
                        .orderByDesc(UserFavorite::getCreatedAt)
                        .orderByDesc(UserFavorite::getId)
                        .last("LIMIT " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(favorite -> resourceItemMapper.selectById(favorite.getTargetId()))
                .filter(resource -> resource != null && ResourceStatus.PUBLISHED.name().equals(resource.getStatus()))
                .map(resource -> toResourceSummary(resource, viewer))
                .toList();
        return new ResourceListResponse(null, null, resources.size(), resources);
    }

    @Transactional
    public ResourceDetailResponse uploadResource(String identity, String title, String category, String summary,
            String description, MultipartFile file) {
        User uploader = userService.requireByIdentity(identity);
        String normalizedTitle = requireText(title, "title");
        String normalizedCategory = normalizeRequiredCategory(category);
        String normalizedSummary = requireText(summary, "summary");
        String normalizedDescription = normalizeOptional(description);
        ValidatedFile validatedFile = validateFile(file);

        String storageKey;
        try (InputStream inputStream = file.getInputStream()) {
            storageKey = resourceFileStorage.store(validatedFile.originalFilename(), inputStream);
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to store resource file");
        }

        ResourceItem resource = new ResourceItem();
        resource.setTitle(normalizedTitle);
        resource.setCategory(normalizedCategory);
        resource.setSummary(normalizedSummary);
        resource.setDescription(normalizedDescription);
        resource.setStatus(ResourceStatus.PENDING.name());
        resource.setUploaderId(uploader.getId());
        resource.setReviewedBy(null);
        resource.setRejectReason(null);
        resource.setFileName(validatedFile.originalFilename());
        resource.setFileExt(validatedFile.extension());
        resource.setContentType(validatedFile.contentType());
        resource.setFileSize(validatedFile.size());
        resource.setStorageKey(storageKey);
        resource.setDownloadCount(0);
        resource.setFavoriteCount(0);
        resource.setPublishedAt(null);
        resource.setReviewedAt(null);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceItemMapper.insert(resource);
        return toResourceDetail(resource, uploader);
    }

    @Transactional
    public ResourceDetailResponse favoriteResource(String identity, Long resourceId) {
        User viewer = userService.requireByIdentity(identity);
        ResourceItem resource = requirePublishedResource(resourceId);
        if (!hasFavorite(resource.getId(), viewer.getId())) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(viewer.getId());
            favorite.setTargetType(FavoriteTargetType.RESOURCE.name());
            favorite.setTargetId(resource.getId());
            favorite.setCreatedAt(LocalDateTime.now());
            userFavoriteMapper.insert(favorite);
            resource.setFavoriteCount(safeCount(resource.getFavoriteCount()) + 1);
            resource.setUpdatedAt(LocalDateTime.now());
            resourceItemMapper.updateById(resource);
        }
        return toResourceDetail(requirePublishedResource(resourceId), viewer);
    }

    @Transactional
    public ResourceDetailResponse unfavoriteResource(String identity, Long resourceId) {
        User viewer = userService.requireByIdentity(identity);
        ResourceItem resource = requirePublishedResource(resourceId);
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, viewer.getId())
                .eq(UserFavorite::getTargetType, FavoriteTargetType.RESOURCE.name())
                .eq(UserFavorite::getTargetId, resource.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
            resource.setFavoriteCount(Math.max(0, safeCount(resource.getFavoriteCount()) - 1));
            resource.setUpdatedAt(LocalDateTime.now());
            resourceItemMapper.updateById(resource);
        }
        return toResourceDetail(requirePublishedResource(resourceId), viewer);
    }

    public ResourceFileStream previewResource(Long resourceId, String identity) {
        User viewer = findViewer(identity);
        ResourceItem resource = requireVisibleResourceForViewer(resourceId, viewer);
        if (!isPdf(resource)) {
            throw new BusinessException(400, "resource preview only supports pdf");
        }
        return openResourceFile(resource);
    }

    @Transactional
    public DownloadedResource downloadResource(String identity, Long resourceId) {
        userService.requireByIdentity(identity);
        ResourceItem resource = requirePublishedResource(resourceId);

        resource.setDownloadCount(safeCount(resource.getDownloadCount()) + 1);
        resource.setUpdatedAt(LocalDateTime.now());
        resourceItemMapper.updateById(resource);

        ResourceFileStream openedResource = openResourceFile(resource);
        return new DownloadedResource(openedResource.fileName(), openedResource.contentType(), openedResource.inputStream());
    }

    private ResourceItem requireVisibleResourceForViewer(Long resourceId, User viewer) {
        ResourceItem resource = requireExistingResource(resourceId);
        if (canViewerSeeResource(resource, viewer)) {
            return resource;
        }
        throw new BusinessException(404, "resource not found");
    }

    private ResourceItem requirePublishedResource(Long resourceId) {
        ResourceItem resource = requireExistingResource(resourceId);
        if (!ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
            throw new BusinessException(404, "resource not found");
        }
        return resource;
    }

    private ResourceItem requireExistingResource(Long resourceId) {
        ResourceItem resource = resourceItemMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "resource not found");
        }
        return resource;
    }

    private ResourceListResponse.ResourceSummary toResourceSummary(ResourceItem resource, User viewer) {
        return new ResourceListResponse.ResourceSummary(
                resource.getId(),
                resource.getTitle(),
                resource.getCategory(),
                resource.getSummary(),
                resource.getStatus(),
                uploaderNicknameOf(resource.getUploaderId()),
                resource.getFileName(),
                resource.getFileSize(),
                safeCount(resource.getDownloadCount()),
                resource.getPublishedAt(),
                viewer != null && hasFavorite(resource.getId(), viewer.getId()));
    }

    private ResourceDetailResponse toResourceDetail(ResourceItem resource, User viewer) {
        return new ResourceDetailResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getCategory(),
                resource.getSummary(),
                resource.getDescription(),
                resource.getStatus(),
                resource.getUploaderId(),
                uploaderNicknameOf(resource.getUploaderId()),
                resource.getFileName(),
                resource.getFileExt(),
                resource.getContentType(),
                resource.getFileSize(),
                safeCount(resource.getDownloadCount()),
                safeCount(resource.getFavoriteCount()),
                resource.getPublishedAt(),
                resource.getReviewedAt(),
                resource.getCreatedAt(),
                resource.getUpdatedAt(),
                resource.getRejectReason(),
                viewer != null && hasFavorite(resource.getId(), viewer.getId()),
                isEditableByViewer(resource, viewer),
                canPreviewResource(resource, viewer));
    }

    private MyResourceListResponse.ResourceItem toMyResourceItem(ResourceItem resource) {
        return new MyResourceListResponse.ResourceItem(
                resource.getId(),
                resource.getTitle(),
                resource.getCategory(),
                resource.getSummary(),
                resource.getStatus(),
                resource.getFileName(),
                resource.getFileSize(),
                resource.getRejectReason(),
                resource.getCreatedAt(),
                resource.getPublishedAt(),
                resource.getUpdatedAt());
    }

    private ResourceFileStream openResourceFile(ResourceItem resource) {
        if (!resourceFileStorage.exists(resource.getStorageKey())) {
            throw new BusinessException(500, "resource file unavailable");
        }
        try {
            return new ResourceFileStream(resource.getFileName(), resource.getContentType(),
                    resourceFileStorage.open(resource.getStorageKey()));
        } catch (IOException exception) {
            throw new BusinessException(500, "resource file unavailable");
        }
    }

    private boolean canViewerSeeResource(ResourceItem resource, User viewer) {
        if (ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        if ("ADMIN".equals(viewer.getRole())) {
            return true;
        }
        return resource.getUploaderId() != null && resource.getUploaderId().equals(viewer.getId());
    }

    private boolean canPreviewResource(ResourceItem resource, User viewer) {
        return canViewerSeeResource(resource, viewer) && isPdf(resource);
    }

    private boolean isEditableByViewer(ResourceItem resource, User viewer) {
        return viewer != null
                && !"ADMIN".equals(viewer.getRole())
                && resource.getUploaderId() != null
                && resource.getUploaderId().equals(viewer.getId())
                && ResourceStatus.REJECTED.name().equals(resource.getStatus());
    }

    private boolean isPdf(ResourceItem resource) {
        return "pdf".equalsIgnoreCase(resource.getFileExt())
                || "application/pdf".equalsIgnoreCase(resource.getContentType());
    }

    private boolean hasFavorite(Long resourceId, Long userId) {
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, FavoriteTargetType.RESOURCE.name())
                .eq(UserFavorite::getTargetId, resourceId)) > 0;
    }

    private String uploaderNicknameOf(Long uploaderId) {
        User uploader = userService.findByUserId(uploaderId);
        return uploader != null ? uploader.getNickname() : "Unknown User";
    }

    public String findUploaderNickname(Long uploaderId) {
        return uploaderNicknameOf(uploaderId);
    }

    private User findViewer(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return null;
        }
        return userService.requireByIdentity(identity);
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

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return ResourceCategory.valueOf(category.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid resource category");
        }
    }

    private String normalizeRequiredCategory(String category) {
        String normalized = normalizeCategory(category);
        if (normalized == null) {
            throw new BusinessException(400, "category is required");
        }
        return normalized;
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(400, "file is too large");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(400, "file name is required");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "unsupported file type");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new ValidatedFile(originalFilename.trim(), extension, contentType, file.getSize());
    }

    private String extractExtension(String originalFilename) {
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == originalFilename.length() - 1) {
            throw new BusinessException(400, "unsupported file type");
        }
        return originalFilename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    public record DownloadedResource(String fileName, String contentType, InputStream inputStream) {
    }

    public record ResourceFileStream(String fileName, String contentType, InputStream inputStream) {
    }

    private record ValidatedFile(String originalFilename, String extension, String contentType, long size) {
    }
}
