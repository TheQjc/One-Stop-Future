package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.ResourceStatus;
import com.campus.dto.AdminResourceListResponse;
import com.campus.dto.ResourceDetailResponse;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.campus.mapper.ResourceItemMapper;

@Service
public class AdminResourceService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final ResourceItemMapper resourceItemMapper;
    private final UserService userService;
    private final ResourceService resourceService;

    public AdminResourceService(ResourceItemMapper resourceItemMapper, UserService userService,
            ResourceService resourceService) {
        this.resourceItemMapper = resourceItemMapper;
        this.userService = userService;
        this.resourceService = resourceService;
    }

    public AdminResourceListResponse listResources() {
        List<AdminResourceListResponse.ResourceItem> resources = resourceItemMapper.selectList(
                new LambdaQueryWrapper<ResourceItem>()
                        .orderByDesc(ResourceItem::getCreatedAt)
                        .orderByDesc(ResourceItem::getId)
                        .last("LIMIT " + ADMIN_LIST_LIMIT))
                .stream()
                .map(this::toAdminItem)
                .toList();
        return new AdminResourceListResponse(resources.size(), resources);
    }

    @Transactional
    public ResourceDetailResponse publishResource(String identity, Long resourceId) {
        User admin = userService.requireByIdentity(identity);
        ResourceItem resource = requireResource(resourceId);
        if (ResourceStatus.REJECTED.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "rejected resource cannot be published");
        }
        if (ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
            return resourceService.getResourceDetail(resource.getId(), identity);
        }
        if (!ResourceStatus.PENDING.name().equals(resource.getStatus())
                && !ResourceStatus.OFFLINE.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "resource is not ready for publish");
        }
        validatePublishable(resource);
        LocalDateTime now = LocalDateTime.now();
        resource.setStatus(ResourceStatus.PUBLISHED.name());
        resource.setReviewedBy(admin.getId());
        resource.setRejectReason(null);
        resource.setPublishedAt(now);
        resource.setReviewedAt(now);
        resource.setUpdatedAt(now);
        resourceItemMapper.updateById(resource);
        return resourceService.getResourceDetail(resource.getId(), identity);
    }

    @Transactional
    public ResourceDetailResponse rejectResource(String identity, Long resourceId, String reason) {
        User admin = userService.requireByIdentity(identity);
        ResourceItem resource = requireResource(resourceId);
        if (!ResourceStatus.PENDING.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "only pending resource can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "reason is required when rejecting resource");
        }
        LocalDateTime now = LocalDateTime.now();
        resource.setStatus(ResourceStatus.REJECTED.name());
        resource.setReviewedBy(admin.getId());
        resource.setRejectReason(reason.trim());
        resource.setReviewedAt(now);
        resource.setUpdatedAt(now);
        resourceItemMapper.updateById(resource);
        return resourceService.getResourceDetail(resource.getId(), identity);
    }

    @Transactional
    public ResourceDetailResponse offlineResource(String identity, Long resourceId) {
        User admin = userService.requireByIdentity(identity);
        ResourceItem resource = requireResource(resourceId);
        if (ResourceStatus.OFFLINE.name().equals(resource.getStatus())) {
            return resourceService.getResourceDetail(resource.getId(), identity);
        }
        if (!ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "only published resource can be offlined");
        }
        resource.setStatus(ResourceStatus.OFFLINE.name());
        resource.setReviewedBy(admin.getId());
        resource.setReviewedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceItemMapper.updateById(resource);
        return resourceService.getResourceDetail(resource.getId(), identity);
    }

    private ResourceItem requireResource(Long resourceId) {
        ResourceItem resource = resourceItemMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "resource not found");
        }
        return resource;
    }

    private void validatePublishable(ResourceItem resource) {
        if (isBlank(resource.getTitle()) || isBlank(resource.getCategory()) || isBlank(resource.getSummary())
                || isBlank(resource.getFileName()) || isBlank(resource.getStorageKey())) {
            throw new BusinessException(400, "resource is not ready for publish");
        }
    }

    private AdminResourceListResponse.ResourceItem toAdminItem(ResourceItem resource) {
        User uploader = userService.findByUserId(resource.getUploaderId());
        return new AdminResourceListResponse.ResourceItem(
                resource.getId(),
                resource.getTitle(),
                resource.getCategory(),
                uploader != null ? uploader.getNickname() : "Unknown User",
                resource.getFileName(),
                resource.getFileSize(),
                resource.getDownloadCount(),
                resource.getStatus(),
                resource.getRejectReason(),
                resource.getCreatedAt(),
                resource.getReviewedAt(),
                resource.getPublishedAt(),
                resourceService.isPreviewAvailableForAdmin(resource),
                resourceService.previewKindForAdmin(resource));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
