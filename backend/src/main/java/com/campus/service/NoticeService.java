package com.campus.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.BusinessException;
import com.campus.common.NoticeStatus;
import com.campus.dto.NoticeCreateRequest;
import com.campus.dto.NoticeReviewRequest;
import com.campus.dto.NoticeUpdateRequest;
import com.campus.entity.Notice;
import com.campus.entity.User;
import com.campus.mapper.NoticeMapper;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final UserService userService;

    public NoticeService(NoticeMapper noticeMapper, UserService userService) {
        this.noticeMapper = noticeMapper;
        this.userService = userService;
    }

    public Map<String, Object> list(int page, int size, String category) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getCreatedAt);
        if (category != null && !category.isBlank()) {
            wrapper.eq(Notice::getCategory, category);
        }
        Page<Notice> result = noticeMapper.selectPage(Page.of(page, size), wrapper);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", result.getTotal());
        payload.put("records", result.getRecords());
        payload.put("page", page);
        payload.put("size", size);
        return payload;
    }

    public Notice detail(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "notice not found");
        }
        return notice;
    }

    public void create(String username, NoticeCreateRequest request) {
        User author = userService.requireByUsername(username);
        Notice notice = new Notice();
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setCategory(defaultCategory(request.category()));
        notice.setAuthorId(author.getId());
        notice.setIsTop(request.isTop() == null ? 0 : request.isTop());
        notice.setStatus(NoticeStatus.PENDING.name());
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    public void update(Long id, NoticeUpdateRequest request) {
        Notice notice = detail(id);
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setCategory(defaultCategory(request.category()));
        notice.setIsTop(request.isTop() == null ? 0 : request.isTop());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    public void delete(Long id) {
        if (noticeMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "notice not found");
        }
    }

    public void review(Long id, String reviewerUsername, NoticeReviewRequest request) {
        Notice notice = detail(id);
        NoticeStatus status = parseStatus(request.status());
        User reviewer = userService.requireByUsername(reviewerUsername);
        notice.setStatus(status.name());
        notice.setReviewedBy(reviewer.getId());
        notice.setReviewedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
    }

    public List<Notice> latestApproved(int limit) {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, NoticeStatus.APPROVED.name())
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getCreatedAt)
                .last("limit " + limit));
    }

    private String defaultCategory(String category) {
        return category == null || category.isBlank() ? "GENERAL" : category;
    }

    private NoticeStatus parseStatus(String value) {
        try {
            return NoticeStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(400, "invalid notice status");
        }
    }
}
