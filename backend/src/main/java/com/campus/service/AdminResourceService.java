package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.NotificationType;
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
    private final NotificationService notificationService;

    public AdminResourceService(ResourceItemMapper resourceItemMapper, UserService userService,
            ResourceService resourceService, NotificationService notificationService) {
        this.resourceItemMapper = resourceItemMapper;
        this.userService = userService;
        this.resourceService = resourceService;
        this.notificationService = notificationService;
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
            throw new BusinessException(400, "已驳回的资料不能直接发布");
        }
        if (ResourceStatus.PUBLISHED.name().equals(resource.getStatus())) {
            return resourceService.getResourceDetail(resource.getId(), identity);
        }
        if (!ResourceStatus.PENDING.name().equals(resource.getStatus())
                && !ResourceStatus.OFFLINE.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "资料未准备好，无法发布");
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
        notifyResourceOwner(resource, NotificationType.RESOURCE_APPROVED,
                "您的资料已发布",
                "您的资料 \"" + resource.getTitle() + "\" 已通过审核，现已对其他同学可见。");
        return resourceService.getResourceDetail(resource.getId(), identity);
    }

    @Transactional
    public ResourceDetailResponse rejectResource(String identity, Long resourceId, String reason) {
        User admin = userService.requireByIdentity(identity);
        ResourceItem resource = requireResource(resourceId);
        if (!ResourceStatus.PENDING.name().equals(resource.getStatus())) {
            throw new BusinessException(400, "只有待审核的资料才能被驳回");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "驳回资料时必须填写原因");
        }
        LocalDateTime now = LocalDateTime.now();
        resource.setStatus(ResourceStatus.REJECTED.name());
        resource.setReviewedBy(admin.getId());
        resource.setRejectReason(reason.trim());
        resource.setReviewedAt(now);
        resource.setUpdatedAt(now);
        resourceItemMapper.updateById(resource);
        notifyResourceOwner(resource, NotificationType.RESOURCE_REJECTED,
                "您的资料需要修改",
                "您的资料 \"" + resource.getTitle() + "\" 未通过审核，原因：" + reason.trim());
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
            throw new BusinessException(400, "只有已发布的资料才能下线");
        }
        resource.setStatus(ResourceStatus.OFFLINE.name());
        resource.setReviewedBy(admin.getId());
        LocalDateTime now = LocalDateTime.now();
        resource.setReviewedAt(now);
        resource.setUpdatedAt(now);
        resourceItemMapper.updateById(resource);
        notifyResourceOwner(resource, NotificationType.RESOURCE_OFFLINED,
                "您的资料已下线",
                "您的资料 \"" + resource.getTitle() + "\" 已被下线，不再对其他同学可见。");
        return resourceService.getResourceDetail(resource.getId(), identity);
    }

    private void notifyResourceOwner(ResourceItem resource, NotificationType type, String title, String content) {
        if (resource.getUploaderId() == null) {
            return;
        }
        notificationService.createNotification(
                resource.getUploaderId(),
                type.name(),
                title,
                content,
                "RESOURCE",
                resource.getId());
    }

    private ResourceItem requireResource(Long resourceId) {
        ResourceItem resource = resourceItemMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "资料不存在");
        }
        return resource;
    }

    private void validatePublishable(ResourceItem resource) {
        if (isBlank(resource.getTitle()) || isBlank(resource.getCategory()) || isBlank(resource.getSummary())
                || isBlank(resource.getFileName()) || isBlank(resource.getStorageKey())) {
            throw new BusinessException(400, "资料未准备好，无法发布");
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
