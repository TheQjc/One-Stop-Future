package com.campus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.NotificationType;
import com.campus.common.VerificationStatus;
import com.campus.dto.AdminVerificationDashboardResponse;
import com.campus.dto.AdminVerificationReviewRequest;
import com.campus.entity.User;
import com.campus.entity.VerificationApplication;
import com.campus.mapper.UserMapper;
import com.campus.mapper.VerificationApplicationMapper;

@Service
public class AdminVerificationService {

    private static final int DASHBOARD_PENDING_LIMIT = 5;
    private static final int REVIEW_LIST_LIMIT = 50;

    private final VerificationApplicationMapper verificationApplicationMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    public AdminVerificationService(VerificationApplicationMapper verificationApplicationMapper, UserMapper userMapper,
            UserService userService, NotificationService notificationService) {
        this.verificationApplicationMapper = verificationApplicationMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    public AdminVerificationDashboardResponse getDashboard() {
        int pendingCount = verificationApplicationMapper
                .selectCount(new LambdaQueryWrapper<VerificationApplication>()
                        .eq(VerificationApplication::getStatus, VerificationStatus.PENDING.name()))
                .intValue();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        int reviewedToday = verificationApplicationMapper
                .selectCount(new LambdaQueryWrapper<VerificationApplication>()
                        .isNotNull(VerificationApplication::getReviewedAt)
                        .ge(VerificationApplication::getReviewedAt, startOfDay)
                        .lt(VerificationApplication::getReviewedAt, startOfDay.plusDays(1)))
                .intValue();

        List<AdminVerificationDashboardResponse.VerificationApplicationSummary> latestPendingApplications =
                verificationApplicationMapper.selectLatestPending(DASHBOARD_PENDING_LIMIT).stream()
                        .map(this::toSummary)
                        .toList();

        return new AdminVerificationDashboardResponse(pendingCount, reviewedToday, latestPendingApplications);
    }

    public List<AdminVerificationDashboardResponse.VerificationApplicationSummary> listApplications() {
        return verificationApplicationMapper
                .selectList(new LambdaQueryWrapper<VerificationApplication>()
                        .orderByDesc(VerificationApplication::getCreatedAt)
                        .last("LIMIT " + REVIEW_LIST_LIMIT))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public void review(Long applicationId, String reviewerIdentity, AdminVerificationReviewRequest request) {
        VerificationApplication application = verificationApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "认证申请不存在");
        }
        if (!VerificationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BusinessException(400, "该认证申请已处理，无法重复审核");
        }

        User reviewer = userService.requireByIdentity(reviewerIdentity);
        User applicant = userService.requireByUserId(application.getUserId());
        String action = normalizeAction(request.action());
        LocalDateTime now = LocalDateTime.now();

        if ("APPROVE".equals(action)) {
            application.setStatus("APPROVED");
            application.setRejectReason(null);
            applicant.setVerificationStatus(VerificationStatus.VERIFIED.name());
            applicant.setRealName(application.getRealName());
            applicant.setStudentId(application.getStudentId());
            notificationService.createNotification(applicant.getId(), NotificationType.VERIFICATION_APPROVED.name(),
                    "学生认证已通过", "您的学生身份认证申请已审核通过。",
                    "VERIFICATION_APPLICATION", application.getId());
        } else if ("REJECT".equals(action)) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new BusinessException(400, "驳回申请时必须填写原因");
            }
            application.setStatus("REJECTED");
            application.setRejectReason(request.reason().trim());
            applicant.setVerificationStatus(VerificationStatus.UNVERIFIED.name());
            applicant.setStudentId(null);
            notificationService.createNotification(applicant.getId(), NotificationType.VERIFICATION_REJECTED.name(),
                    "学生认证被驳回", "您的学生身份认证申请已被驳回：" + request.reason().trim(),
                    "VERIFICATION_APPLICATION", application.getId());
        } else {
            throw new BusinessException(400, "无效的审核操作");
        }

        application.setReviewerId(reviewer.getId());
        application.setReviewedAt(now);
        application.setUpdatedAt(now);
        applicant.setUpdatedAt(now);
        userMapper.updateById(applicant);
        verificationApplicationMapper.updateById(application);
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new BusinessException(400, "无效的审核操作");
        }
        return action.trim().toUpperCase(Locale.ROOT);
    }

    private AdminVerificationDashboardResponse.VerificationApplicationSummary toSummary(
            VerificationApplication application) {
        User applicant = userMapper.selectById(application.getUserId());
        return new AdminVerificationDashboardResponse.VerificationApplicationSummary(
                application.getId(),
                application.getUserId(),
                applicant == null ? null : applicant.getNickname(),
                application.getRealName(),
                application.getStudentId(),
                application.getStatus(),
                application.getCreatedAt());
    }
}
