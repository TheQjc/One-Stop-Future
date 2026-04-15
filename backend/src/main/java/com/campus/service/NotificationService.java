package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.common.BusinessException;
import com.campus.dto.HomeSummaryResponse;
import com.campus.dto.NotificationListResponse;
import com.campus.entity.Notification;
import com.campus.entity.User;
import com.campus.mapper.NotificationMapper;

@Service
public class NotificationService {

    private static final int DEFAULT_LIST_LIMIT = 50;

    private final NotificationMapper notificationMapper;
    private final UserService userService;

    public NotificationService(NotificationMapper notificationMapper, UserService userService) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
    }

    public NotificationListResponse listByIdentity(String identity) {
        User user = userService.requireByIdentity(identity);
        List<Notification> rows = notificationMapper.selectLatestByUserId(user.getId(), DEFAULT_LIST_LIMIT);
        List<NotificationListResponse.NotificationItem> notifications = rows.stream()
                .map(this::toNotificationItem)
                .toList();
        return new NotificationListResponse(notificationMapper.countUnreadByUserId(user.getId()), notifications.size(),
                notifications);
    }

    public int countUnreadByUserId(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    public List<HomeSummaryResponse.NotificationSnippet> listLatestSnippets(Long userId, int limit) {
        return notificationMapper.selectLatestByUserId(userId, limit).stream()
                .map(this::toNotificationSnippet)
                .toList();
    }

    @Transactional
    public void markRead(String identity, Long notificationId) {
        User user = userService.requireByIdentity(identity);
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !user.getId().equals(notification.getUserId())) {
            throw new BusinessException(404, "notification not found");
        }
        if (Integer.valueOf(1).equals(notification.getIsRead())) {
            return;
        }
        notificationMapper.markAsRead(notificationId, user.getId(), LocalDateTime.now());
    }

    @Transactional
    public int markAllRead(String identity) {
        User user = userService.requireByIdentity(identity);
        return notificationMapper.markAllAsRead(user.getId(), LocalDateTime.now());
    }

    public void createNotification(Long userId, String type, String title, String content, String sourceType, Long sourceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private HomeSummaryResponse.NotificationSnippet toNotificationSnippet(Notification row) {
        return new HomeSummaryResponse.NotificationSnippet(
                row.getId(),
                row.getType(),
                row.getTitle(),
                row.getContent(),
                Integer.valueOf(1).equals(row.getIsRead()),
                row.getCreatedAt());
    }

    private NotificationListResponse.NotificationItem toNotificationItem(Notification row) {
        return new NotificationListResponse.NotificationItem(
                row.getId(),
                row.getType(),
                row.getTitle(),
                row.getContent(),
                Integer.valueOf(1).equals(row.getIsRead()),
                row.getCreatedAt(),
                row.getReadAt());
    }
}
